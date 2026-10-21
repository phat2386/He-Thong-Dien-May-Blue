package bus;

import dao.GiaoDich;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import dao.AuditLogDAO;
import dao.NhatKyDangNhap_DAO;
import dao.NhanVien_DAO;
import dao.TaiKhoan_DAO;
import dao.VaiTro_DAO;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.VaiTro;

/** Nghiệp vụ quản lý tài khoản nhân viên (UC005). */
public class TaiKhoan_BUS {

	private final TaiKhoan_DAO taiKhoanDAO = new TaiKhoan_DAO();
	private final NhanVien_DAO nhanVienDAO = new NhanVien_DAO();
	private final VaiTro_DAO vaiTroDAO = new VaiTro_DAO();
	private final NhatKyDangNhap_DAO nhatKyDangNhapDAO = new NhatKyDangNhap_DAO();
	private final AuditLogDAO auditDAO = new AuditLogDAO();

	// ===================== TRA CỨU =====================

	public List<TaiKhoan> timKiem(String tuKhoa, String codeVaiTro, String trangThai) throws Exception {
		return taiKhoanDAO.timKiem(tuKhoa == null ? "" : tuKhoa.trim(), codeVaiTro, trangThai);
	}

	public TaiKhoan timTheoMa(int maTaiKhoan) throws Exception {
		return taiKhoanDAO.timTheoMa(maTaiKhoan);
	}

	public List<NhanVien> layNhanVienChuaCoTaiKhoan() throws Exception {
		return nhanVienDAO.layNhanVienChuaCoTaiKhoan();
	}

	public List<VaiTro> layVaiTroGanDuoc() throws Exception {
		return vaiTroDAO.layVaiTroGanDuoc();
	}

	public boolean dangCoPhienDangNhap(int maTaiKhoan) throws Exception {
		return nhatKyDangNhapDAO.dangCoPhien(maTaiKhoan);
	}

	// ===================== THÊM / SỬA =====================

	public void capTaiKhoan(String maNhanVien, String tenDangNhap, String matKhau, int maVaiTro) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("ACCOUNT_MANAGE");
		if (maNhanVien == null || maNhanVien.trim().isEmpty()) {
			throw new IllegalArgumentException("Chưa chọn nhân viên.");
		}
		if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
			throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
		}
		if (!PasswordUtil.isStrongEnough(matKhau)) {
			throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số.");
		}
		tenDangNhap = tenDangNhap.trim();

		Connection con = null;
		try {
			con = GiaoDich.mo();

			NhanVien nv = nhanVienDAO.timTheoMa(con, maNhanVien);
			if (nv == null || !"ACTIVE".equals(nv.getTrangThai())) {
				throw new IllegalArgumentException("Chỉ có thể cấp tài khoản cho nhân viên đang hoạt động.");
			}
			VaiTro vaiTro = vaiTroDAO.timTheoMa(con, maVaiTro);
			if (vaiTro == null) {
				throw new IllegalArgumentException("Vai trò không hợp lệ.");
			}
			if (!Arrays.asList("MANAGER", "SALES", "CUSTOMER_SERVICE").contains(vaiTro.getCodeVaiTro())) {
				throw new IllegalArgumentException("Tài khoản Admin hệ thống không được tạo tại UC005.");
			}
			if (taiKhoanDAO.daTonTai(con, maNhanVien, tenDangNhap)) {
				throw new IllegalArgumentException("Nhân viên đã có tài khoản hoặc tên đăng nhập đã tồn tại.");
			}

			int maTaiKhoan = taiKhoanDAO.them(con, maNhanVien, tenDangNhap, PasswordUtil.sha256(matKhau), maVaiTro);
			auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "ACCOUNT_CREATED", "accounts",
					String.valueOf(maTaiKhoan), "Cấp tài khoản cho " + maNhanVien);
			con.commit();
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}

	public void capNhatTaiKhoan(int maTaiKhoan, String tenDangNhap, int maVaiTroMoi) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("ACCOUNT_MANAGE");
		if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
			throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
		}
		tenDangNhap = tenDangNhap.trim();

		Connection con = null;
		try {
			con = GiaoDich.mo();

			TaiKhoan tk = taiKhoanDAO.timTheoMa(con, maTaiKhoan);
			if (tk == null) {
				throw new IllegalArgumentException("Không tìm thấy tài khoản.");
			}
			String codeCu = tk.getVaiTro().getCodeVaiTro();
			if ("ADMIN".equals(codeCu)) {
				throw new IllegalStateException("Tài khoản Admin hệ thống không được chỉnh sửa tại UC005.");
			}
			VaiTro vaiTroMoi = vaiTroDAO.timTheoMa(con, maVaiTroMoi);
			if (vaiTroMoi == null) {
				throw new IllegalArgumentException("Vai trò không hợp lệ.");
			}
			if ("ADMIN".equals(vaiTroMoi.getCodeVaiTro())) {
				throw new IllegalArgumentException("Không thể gán vai trò Admin tại UC005.");
			}
			if (taiKhoanDAO.trungTenDangNhap(con, tenDangNhap, maTaiKhoan)) {
				throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
			}
			if (maTaiKhoan == nguoiThucHien.getMaTaiKhoan() && tk.getVaiTro().getMaVaiTro() != maVaiTroMoi) {
				throw new IllegalArgumentException("Không thể tự thay đổi vai trò của tài khoản đang đăng nhập.");
			}
			// Không được đổi vai trò của quản lý đang hoạt động cuối cùng
			if ("MANAGER".equals(codeCu) && !"MANAGER".equals(vaiTroMoi.getCodeVaiTro())
					&& "ACTIVE".equals(tk.getTrangThai()) && taiKhoanDAO.demQuanLyDangHoatDong(con) <= 1) {
				throw new IllegalStateException("Không thể đổi vai trò của quản lý đang hoạt động cuối cùng.");
			}

			taiKhoanDAO.capNhatTenVaVaiTro(con, maTaiKhoan, tenDangNhap, maVaiTroMoi);
			auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "ACCOUNT_UPDATED", "accounts",
					String.valueOf(maTaiKhoan), "Cập nhật tên đăng nhập/vai trò");
			con.commit();
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}

	// ===================== ĐỔI TRẠNG THÁI / XÓA =====================

	/** trangThaiMoi: ACTIVE, LOCKED hoặc DISABLED. */
	public void doiTrangThai(int maTaiKhoan, String trangThaiMoi) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("ACCOUNT_MANAGE");
		if (!Arrays.asList("ACTIVE", "LOCKED", "DISABLED").contains(trangThaiMoi)) {
			throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
		}
		if (maTaiKhoan == nguoiThucHien.getMaTaiKhoan() && !"ACTIVE".equals(trangThaiMoi)) {
			throw new IllegalArgumentException("Không thể tự khóa/vô hiệu hóa tài khoản đang sử dụng.");
		}

		Connection con = null;
		try {
			con = GiaoDich.mo();

			TaiKhoan tk = taiKhoanDAO.timTheoMa(con, maTaiKhoan);
			if (tk == null) {
				throw new IllegalArgumentException("Không tìm thấy tài khoản.");
			}
			String code = tk.getVaiTro().getCodeVaiTro();
			if ("ADMIN".equals(code)) {
				throw new IllegalStateException("Tài khoản Admin hệ thống không được khóa/vô hiệu hóa tại UC005.");
			}
			if ("ACTIVE".equals(trangThaiMoi) && !"ACTIVE".equals(tk.getNhanVien().getTrangThai())) {
				throw new IllegalArgumentException("Không thể kích hoạt tài khoản khi hồ sơ nhân viên chưa hoạt động.");
			}
			if (!"ACTIVE".equals(trangThaiMoi) && "MANAGER".equals(code) && "ACTIVE".equals(tk.getTrangThai())
					&& taiKhoanDAO.demQuanLyDangHoatDong(con) <= 1) {
				throw new IllegalStateException("Không thể khóa/vô hiệu hóa quản lý đang hoạt động cuối cùng.");
			}

			taiKhoanDAO.doiTrangThai(con, maTaiKhoan, trangThaiMoi);
			if (!"ACTIVE".equals(trangThaiMoi)) {
				nhatKyDangNhapDAO.ketThucPhienCuaTaiKhoan(con, maTaiKhoan);
			}
			auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "ACCOUNT_STATUS_CHANGED", "accounts",
					String.valueOf(maTaiKhoan), "Trạng thái -> " + trangThaiMoi);
			con.commit();
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}

	/**
	 * Xóa tài khoản. Nếu tài khoản đã có lịch sử thì chỉ vô hiệu hóa để giữ dữ liệu.
	 * 
	 * @return true nếu đã xóa hẳn; false nếu chỉ vô hiệu hóa.
	 */
	public boolean xoaHoacVoHieuHoa(int maTaiKhoan) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("ACCOUNT_MANAGE");
		if (maTaiKhoan == nguoiThucHien.getMaTaiKhoan()) {
			throw new IllegalArgumentException("Không thể tự xóa tài khoản đang sử dụng.");
		}

		Connection con = null;
		try {
			con = GiaoDich.mo();

			TaiKhoan tk = taiKhoanDAO.timTheoMa(con, maTaiKhoan);
			if (tk == null) {
				throw new IllegalArgumentException("Không tìm thấy tài khoản.");
			}
			String code = tk.getVaiTro().getCodeVaiTro();
			if ("ADMIN".equals(code)) {
				throw new IllegalStateException("Tài khoản Admin hệ thống không được xóa/vô hiệu hóa tại UC005.");
			}
			if ("MANAGER".equals(code) && "ACTIVE".equals(tk.getTrangThai())
					&& taiKhoanDAO.demQuanLyDangHoatDong(con) <= 1) {
				throw new IllegalStateException("Không thể xóa quản lý đang hoạt động cuối cùng.");
			}

			boolean daXoaHan;
			if (taiKhoanDAO.demLichSu(con, maTaiKhoan) > 0) {
				// Đã có lịch sử -> chỉ vô hiệu hóa
				taiKhoanDAO.voHieuHoa(con, maTaiKhoan);
				nhatKyDangNhapDAO.ketThucPhienCuaTaiKhoan(con, maTaiKhoan);
				auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "ACCOUNT_DISABLED_PRESERVE_HISTORY", "accounts",
						String.valueOf(maTaiKhoan), "Không xóa vật lý vì tài khoản đã có lịch sử");
				daXoaHan = false;
			} else {
				taiKhoanDAO.xoa(con, maTaiKhoan);
				auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "ACCOUNT_DELETED", "accounts",
						String.valueOf(maTaiKhoan), "Xóa tài khoản chưa phát sinh lịch sử");
				daXoaHan = true;
			}
			con.commit();
			return daXoaHan;
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}
}

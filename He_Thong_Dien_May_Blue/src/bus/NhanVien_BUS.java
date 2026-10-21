package bus;

import dao.GiaoDich;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import dao.AuditLogDAO;
import dao.NhatKyDangNhap_DAO;
import dao.NhanVien_DAO;
import dao.TaiKhoan_DAO;
import entity.NhanVien;
import entity.TaiKhoan;

/** Nghiệp vụ quản lý hồ sơ nhân viên (UC032). */
public class NhanVien_BUS {

	private final NhanVien_DAO nhanVienDAO = new NhanVien_DAO();
	private final TaiKhoan_DAO taiKhoanDAO = new TaiKhoan_DAO();
	private final NhatKyDangNhap_DAO nhatKyDangNhapDAO = new NhatKyDangNhap_DAO();
	private final AuditLogDAO auditDAO = new AuditLogDAO();

	public List<NhanVien> timKiem(String tuKhoa, String trangThai) throws Exception {
		return nhanVienDAO.timKiem(tuKhoa == null ? "" : tuKhoa.trim(), trangThai);
	}

	public NhanVien timTheoMa(String maNhanVien) throws Exception {
		return nhanVienDAO.timTheoMa(maNhanVien);
	}

	public void themNhanVien(NhanVien nv) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("EMPLOYEE_MANAGE");
		kiemTraDuLieu(nv);
		chuanHoa(nv);

		Connection con = null;
		try {
			con = GiaoDich.mo();
			if (nhanVienDAO.trungMaHoacSdt(con, nv.getMaNhanVien(), nv.getSoDienThoai())) {
				throw new IllegalArgumentException("Mã nhân viên hoặc số điện thoại đã tồn tại.");
			}
			nhanVienDAO.them(con, nv);
			auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "EMPLOYEE_CREATED", "employees", nv.getMaNhanVien(),
					"Tạo hồ sơ nhân viên: " + nv.getHoTen());
			con.commit();
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}

	public void capNhatNhanVien(NhanVien nv) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("EMPLOYEE_MANAGE");
		kiemTraDuLieu(nv);
		chuanHoa(nv);

		Connection con = null;
		try {
			con = GiaoDich.mo();
			if (nhanVienDAO.timTheoMa(con, nv.getMaNhanVien()) == null) {
				throw new IllegalArgumentException("Không tìm thấy nhân viên.");
			}
			if (laAdmin(con, nv.getMaNhanVien())) {
				throw new IllegalStateException("Hồ sơ Admin hệ thống không được chỉnh sửa tại UC032.");
			}
			if (nhanVienDAO.sdtThuocNguoiKhac(con, nv.getSoDienThoai(), nv.getMaNhanVien())) {
				throw new IllegalArgumentException("Số điện thoại đã thuộc nhân viên khác.");
			}
			nhanVienDAO.capNhat(con, nv);
			auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "EMPLOYEE_UPDATED", "employees", nv.getMaNhanVien(),
					"Cập nhật hồ sơ nhân viên");
			con.commit();
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}

	/**
	 * Ngừng hoạt động hoặc kích hoạt lại nhân viên. Khi ngừng: tài khoản bị vô hiệu
	 * hóa, phiên đăng nhập bị kết thúc và các ca từ hôm nay bị hủy.
	 */
	public void doiTrangThaiHoatDong(String maNhanVien, boolean kichHoat) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("EMPLOYEE_MANAGE");
		if (!kichHoat && nguoiThucHien.getNhanVien().getMaNhanVien().equals(maNhanVien)) {
			throw new IllegalArgumentException("Không thể tự ngừng hoạt động hồ sơ đang đăng nhập.");
		}

		Connection con = null;
		try {
			con = GiaoDich.mo();
			if (nhanVienDAO.timTheoMa(con, maNhanVien) == null) {
				throw new IllegalArgumentException("Không tìm thấy nhân viên.");
			}
			TaiKhoan tk = taiKhoanDAO.timTheoMaNhanVien(con, maNhanVien);
			String codeVaiTro = tk == null ? "" : tk.getVaiTro().getCodeVaiTro();

			if ("ADMIN".equals(codeVaiTro)) {
				throw new IllegalStateException("Hồ sơ Admin hệ thống không được ngừng/kích hoạt tại UC032.");
			}
			// Không được ngừng quản lý đang hoạt động cuối cùng
			if (!kichHoat && "MANAGER".equals(codeVaiTro) && "ACTIVE".equals(tk.getTrangThai())
					&& taiKhoanDAO.demQuanLyDangHoatDong(con) <= 1) {
				throw new IllegalStateException(
						"Không thể ngừng hoạt động quản lý đang hoạt động cuối cùng của hệ thống.");
			}

			nhanVienDAO.doiTrangThai(con, maNhanVien, kichHoat ? "ACTIVE" : "INACTIVE");
			if (!kichHoat) {
				nhanVienDAO.voHieuHoaTaiKhoan(con, maNhanVien);
				nhatKyDangNhapDAO.ketThucPhienCuaNhanVien(con, maNhanVien);
				nhanVienDAO.huyCaTuongLai(con, maNhanVien);
			}
			auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(),
					kichHoat ? "EMPLOYEE_REACTIVATED" : "EMPLOYEE_DEACTIVATED", "employees", maNhanVien,
					kichHoat ? "Kích hoạt lại hồ sơ nhân viên"
							: "Ngừng hoạt động; vô hiệu hóa tài khoản và hủy ca tương lai");
			con.commit();
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}

	// ===================== HÀM PHỤ =====================

	private boolean laAdmin(Connection con, String maNhanVien) throws Exception {
		TaiKhoan tk = taiKhoanDAO.timTheoMaNhanVien(con, maNhanVien);
		return tk != null && "ADMIN".equals(tk.getVaiTro().getCodeVaiTro());
	}

	private void kiemTraDuLieu(NhanVien nv) {
		if (nv.getMaNhanVien() == null || nv.getMaNhanVien().trim().isEmpty() || nv.getHoTen() == null
				|| nv.getHoTen().trim().isEmpty() || nv.getSoDienThoai() == null
				|| nv.getSoDienThoai().trim().isEmpty()) {
			throw new IllegalArgumentException("Mã nhân viên, họ tên và số điện thoại không được để trống.");
		}
		if (!nv.getSoDienThoai().trim().matches("[0-9+]{9,15}")) {
			throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
		}
		String email = nv.getEmail();
		if (email != null && !email.trim().isEmpty() && !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
			throw new IllegalArgumentException("Email không hợp lệ.");
		}
		if (nv.getNgayVaoLam() == null || nv.getNgayVaoLam().isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Ngày vào làm không được để trống hoặc nằm trong tương lai.");
		}
	}

	/** Cắt khoảng trắng; email / phòng ban để trống thì lưu null. */
	private void chuanHoa(NhanVien nv) {
		nv.setMaNhanVien(nv.getMaNhanVien().trim());
		nv.setHoTen(nv.getHoTen().trim());
		nv.setSoDienThoai(nv.getSoDienThoai().trim());
		nv.setEmail(rongThanhNull(nv.getEmail()));
		nv.setPhongBan(rongThanhNull(nv.getPhongBan()));
	}

	private String rongThanhNull(String s) {
		return s == null || s.trim().isEmpty() ? null : s.trim();
	}
}

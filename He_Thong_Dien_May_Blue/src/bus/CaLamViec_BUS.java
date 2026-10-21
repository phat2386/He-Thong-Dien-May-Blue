package bus;

import dao.GiaoDich;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import dao.AuditLogDAO;
import dao.CaLamViec_DAO;
import dao.TaiKhoan_DAO;
import entity.CaLamViec;
import entity.PhanCa;
import entity.TaiKhoan;

/** Nghiệp vụ phân ca làm việc (UC006). */
public class CaLamViec_BUS {

	private final CaLamViec_DAO caDAO = new CaLamViec_DAO();
	private final TaiKhoan_DAO taiKhoanDAO = new TaiKhoan_DAO();
	private final AuditLogDAO auditDAO = new AuditLogDAO();

	// ===================== TRA CỨU =====================

	public List<PhanCa> layPhanCaTheoNgay(LocalDate ngay) throws Exception {
		return caDAO.layPhanCaTheoNgay(ngay);
	}

	public List<CaLamViec> layCaDangDung() throws Exception {
		return caDAO.layCaDangDung();
	}

	/** Nhân viên bán hàng / CSKH đang hoạt động, có thể được phân ca. */
	public List<TaiKhoan> layNhanVienCoTheXepCa() throws Exception {
		return taiKhoanDAO.layTaiKhoanCoTheXepCa();
	}

	/** Ca này (trong ngày) đã có nhân viên bán hàng chưa? */
	public boolean caDaCoNhanVienBanHang(LocalDate ngay, int maCa) throws Exception {
		return caDAO.demNhanVienBanHang(ngay, maCa) > 0;
	}

	// ===================== PHÂN CA =====================

	public void phanCa(String maNhanVien, LocalDate ngay, int maCa) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("SHIFT_MANAGE");
		if (ngay == null || ngay.isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("Không thể phân ca cho ngày đã qua.");
		}

		Connection con = null;
		try {
			con = GiaoDich.mo();

			// Nhân viên phải có tài khoản + vai trò, đang hoạt động
			TaiKhoan tk = taiKhoanDAO.timTheoMaNhanVien(con, maNhanVien);
			if (tk == null) {
				throw new IllegalArgumentException("Nhân viên chưa có tài khoản/vai trò.");
			}
			if (!"ACTIVE".equals(tk.getNhanVien().getTrangThai())) {
				throw new IllegalArgumentException("Nhân viên không ở trạng thái làm việc.");
			}
			if (!"ACTIVE".equals(tk.getTrangThai())) {
				throw new IllegalArgumentException("Tài khoản nhân viên không hoạt động.");
			}
			String code = tk.getVaiTro().getCodeVaiTro();
			if (!"SALES".equals(code) && !"CUSTOMER_SERVICE".equals(code)) {
				throw new IllegalArgumentException("UC006 chỉ phân ca cho Nhân viên bán hàng và Nhân viên CSKH.");
			}

			CaLamViec ca = caDAO.timCaTheoMa(con, maCa);
			if (ca == null || !ca.isHoatDong()) {
				throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc đã ngừng sử dụng.");
			}
			if (caDAO.daDuyetNghi(con, maNhanVien, ngay)) {
				throw new IllegalArgumentException("Nhân viên đã được duyệt nghỉ trong ngày đã chọn.");
			}
			if (caDAO.daCoCaTrongNgay(con, maNhanVien, ngay)) {
				throw new IllegalArgumentException("Nhân viên đã có ca làm việc trong ngày này.");
			}

			String nguoiPhan = nguoiThucHien.getNhanVien().getMaNhanVien();
			Long maPhanCaCu = caDAO.timPhanCaDaHuy(con, maNhanVien, ngay, maCa);
			if (maPhanCaCu != null) {
				caDAO.kichHoatLai(con, maPhanCaCu.longValue(), nguoiPhan); // dùng lại dòng đã hủy
			} else {
				caDAO.them(con, maNhanVien, ngay, maCa, nguoiPhan);
			}

			caDAO.themThongBao(con, maNhanVien, "Lịch ca làm việc mới",
					"Ngày " + ngay + " - " + ca.getTenCa() + " (" + ca.getGioBatDau() + " - " + ca.getGioKetThuc() + ")");
			auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "SHIFT_ASSIGNED", "shift_assignments", maNhanVien,
					"Phân ca ngày " + ngay + ", shift_id=" + maCa);
			con.commit();
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}

	public void huyPhanCa(long maPhanCa) throws Exception {
		TaiKhoan nguoiThucHien = PhienDangNhap.yeuCauQuyen("SHIFT_MANAGE");

		Connection con = null;
		try {
			con = GiaoDich.mo();

			PhanCa pc = caDAO.timPhanCaTheoMa(con, maPhanCa);
			if (pc == null) {
				throw new IllegalArgumentException("Không tìm thấy phân ca.");
			}
			if (!"ASSIGNED".equals(pc.getTrangThai())) {
				throw new IllegalArgumentException("Phân ca này không còn ở trạng thái đang phân công.");
			}

			// Không được hủy nhân viên bán hàng cuối cùng khi ca vẫn còn người khác
			if ("SALES".equals(pc.getCodeVaiTro())) {
				int nguoiKhac = caDAO.demNhanVienKhac(con, pc.getNgayLam(), pc.getMaCa(), maPhanCa);
				int banHangKhac = caDAO.demNhanVienBanHangKhac(con, pc.getNgayLam(), pc.getMaCa(), maPhanCa);
				if (nguoiKhac > 0 && banHangKhac == 0) {
					throw new IllegalStateException(
							"Không thể hủy nhân viên bán hàng cuối cùng khi ca vẫn còn nhân viên khác. "
									+ "Hãy phân công nhân viên bán hàng thay thế trước.");
				}
			}

			caDAO.huy(con, maPhanCa);
			caDAO.themThongBao(con, pc.getMaNhanVien(), "Lịch ca đã hủy",
					"Ca " + pc.getTenCa() + " ngày " + pc.getNgayLam() + " đã được hủy.");
			auditDAO.insert(con, nguoiThucHien.getMaTaiKhoan(), "SHIFT_CANCELLED", "shift_assignments",
					String.valueOf(maPhanCa), "Hủy phân ca của " + pc.getMaNhanVien() + " ngày " + pc.getNgayLam());
			con.commit();
		} catch (Exception e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}
}

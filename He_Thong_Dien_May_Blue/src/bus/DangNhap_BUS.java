package bus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Set;

import dao.AuditLogDAO;
import dao.NhatKyDangNhap_DAO;
import dao.PhanQuyen_DAO;
import dao.TaiKhoan_DAO;
import entity.KetQuaDangNhap;
import entity.KetQuaXuLy;
import entity.TaiKhoan;

/** Xử lý đăng nhập, đăng xuất, đổi mật khẩu. */
public class DangNhap_BUS {

	private final TaiKhoan_DAO taiKhoanDAO = new TaiKhoan_DAO();
	private final PhanQuyen_DAO phanQuyenDAO = new PhanQuyen_DAO();
	private final NhatKyDangNhap_DAO nhatKyDAO = new NhatKyDangNhap_DAO();
	private final AuditLogDAO auditLogDAO = new AuditLogDAO();

	// ===================== ĐĂNG NHẬP =====================

	public KetQuaDangNhap dangNhap(String tenDangNhap, String matKhau) {
		tenDangNhap = tenDangNhap == null ? "" : tenDangNhap.trim();
		matKhau = matKhau == null ? "" : matKhau;

		if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
			return KetQuaDangNhap.thatBai("Tên đăng nhập và mật khẩu không được để trống.");
		}

		try {
			TaiKhoan tk = taiKhoanDAO.timTheoTenDangNhap(tenDangNhap);

			// 1. Không có tài khoản
			if (tk == null) {
				nhatKyDAO.them(null, tenDangNhap, false, "ACCOUNT_NOT_FOUND", layThongTinMay());
				return KetQuaDangNhap.thatBai("Sai tài khoản hoặc mật khẩu.");
			}

			// 2. Tài khoản không ở trạng thái ACTIVE
			String trangThai = tk.getTrangThai() == null ? "" : tk.getTrangThai().toUpperCase();
			if (!"ACTIVE".equals(trangThai)) {
				nhatKyDAO.them(tk.getMaTaiKhoan(), tenDangNhap, false, "ACCOUNT_STATUS_" + trangThai,
						layThongTinMay());
				if ("LOCKED".equals(trangThai)) {
					return KetQuaDangNhap.thatBai("Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên.");
				}
				if ("DISABLED".equals(trangThai)) {
					return KetQuaDangNhap.thatBai("Tài khoản đã ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
				}
				if ("PENDING".equals(trangThai)) {
					return KetQuaDangNhap.thatBai("Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
				}
				return KetQuaDangNhap.thatBai("Tài khoản hiện không thể đăng nhập. Vui lòng liên hệ quản trị viên.");
			}

			// 3. Hồ sơ nhân viên phải đang làm việc
			if (tk.getNhanVien() == null || !"ACTIVE".equalsIgnoreCase(tk.getNhanVien().getTrangThai())) {
				String lyDo = tk.getNhanVien() == null ? "MISSING" : tk.getNhanVien().getTrangThai();
				nhatKyDAO.them(tk.getMaTaiKhoan(), tenDangNhap, false, "EMPLOYEE_STATUS_" + lyDo, layThongTinMay());
				return KetQuaDangNhap
						.thatBai("Hồ sơ nhân viên hiện không ở trạng thái làm việc. Vui lòng liên hệ quản lý.");
			}

			// 4. Đang bị khóa tạm (nhập sai nhiều lần)
			if (tk.getKhoaDen() != null) {
				if (tk.getKhoaDen().isAfter(LocalDateTime.now())) {
					nhatKyDAO.them(tk.getMaTaiKhoan(), tenDangNhap, false, "TEMPORARY_LOCK", layThongTinMay());
					return KetQuaDangNhap.thatBai("Tài khoản đang bị khóa tạm thời đến "
							+ tk.getKhoaDen().toLocalTime().withSecond(0).withNano(0) + ". Vui lòng thử lại sau.");
				}
				// hết hạn khóa tạm -> mở lại
				taiKhoanDAO.xoaKhoaTamHetHan(tk.getMaTaiKhoan());
				tk.setSoLanSai(0);
				tk.setKhoaDen(null);
			}

			// 5. Kiểm tra mật khẩu
			if (!PasswordUtil.matches(matKhau, tk.getMatKhauHash())) {
				int soLanSai = taiKhoanDAO.tangSoLanSai(tk.getMaTaiKhoan());
				nhatKyDAO.them(tk.getMaTaiKhoan(), tenDangNhap, false, "WRONG_PASSWORD_ATTEMPT_" + soLanSai,
						layThongTinMay());

				if (soLanSai > 5) {
					return KetQuaDangNhap
							.thatBai("Bạn đã nhập sai quá 5 lần liên tiếp. Tài khoản bị khóa tạm thời 15 phút.");
				}
				if (soLanSai == 5) {
					return KetQuaDangNhap.thatBai("Sai tài khoản hoặc mật khẩu. Đây là lần sai thứ 5; "
							+ "nếu nhập sai thêm một lần, tài khoản sẽ bị khóa tạm thời 15 phút.");
				}
				return KetQuaDangNhap
						.thatBai("Sai tài khoản hoặc mật khẩu. Lần nhập sai liên tiếp: " + soLanSai + "/5.");
			}

			// 6. Đăng nhập thành công: đọc quyền trước rồi mới ghi nhận
			Set<String> quyen = phanQuyenDAO.layDanhSachQuyenTheoVaiTro(tk.getVaiTro().getMaVaiTro());
			taiKhoanDAO.ghiNhanDangNhapThanhCong(tk.getMaTaiKhoan());
			long maNhatKy = nhatKyDAO.them(tk.getMaTaiKhoan(), tenDangNhap, true, null, layThongTinMay());

			PhienDangNhap.dangNhap(tk, maNhatKy, quyen);
			return KetQuaDangNhap.thanhCong(tk, tk.isDangNhapLanDau());

		} catch (SQLException e) {
			e.printStackTrace();
			return KetQuaDangNhap.thatBai("Không thể kết nối hoặc truy vấn cơ sở dữ liệu.");
		}
	}

	// ===================== ĐỔI MẬT KHẨU =====================

	public KetQuaXuLy doiMatKhau(String matKhauHienTai, String matKhauMoi, String xacNhan) {
		if (!PhienDangNhap.daDangNhap()) {
			return KetQuaXuLy.thatBai("Phiên đăng nhập không còn hiệu lực.");
		}

		matKhauHienTai = matKhauHienTai == null ? "" : matKhauHienTai;
		matKhauMoi = matKhauMoi == null ? "" : matKhauMoi;
		xacNhan = xacNhan == null ? "" : xacNhan;

		if (matKhauHienTai.isEmpty() || matKhauMoi.isEmpty() || xacNhan.isEmpty()) {
			return KetQuaXuLy.thatBai(
					"Vui lòng nhập đầy đủ mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu.");
		}

		try {
			TaiKhoan tkPhien = PhienDangNhap.getTaiKhoanHienTai();
			// Lấy dữ liệu mới nhất trong CSDL, không chỉ dựa vào bản sao trong phiên
			TaiKhoan tk = taiKhoanDAO.timTheoMa(tkPhien.getMaTaiKhoan());

			if (tk == null || !"ACTIVE".equalsIgnoreCase(tk.getTrangThai()) || tk.getNhanVien() == null
					|| !"ACTIVE".equalsIgnoreCase(tk.getNhanVien().getTrangThai())) {
				return KetQuaXuLy.thatBai(
						"Tài khoản hoặc hồ sơ nhân viên không còn hoạt động. Vui lòng đăng nhập lại.");
			}
			if (!PasswordUtil.matches(matKhauHienTai, tk.getMatKhauHash())) {
				return KetQuaXuLy.thatBai("Mật khẩu hiện tại không đúng.");
			}
			if (!PasswordUtil.isStrongEnough(matKhauMoi)) {
				return KetQuaXuLy.thatBai("Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ và số.");
			}
			if (!matKhauMoi.equals(xacNhan)) {
				return KetQuaXuLy.thatBai("Mật khẩu xác nhận không khớp.");
			}
			if (PasswordUtil.matches(matKhauMoi, tk.getMatKhauHash())) {
				return KetQuaXuLy.thatBai("Mật khẩu mới không được trùng mật khẩu hiện tại.");
			}

			String hashMoi = PasswordUtil.sha256(matKhauMoi);
			taiKhoanDAO.capNhatMatKhau(tk.getMaTaiKhoan(), hashMoi);

			auditLogDAO.insert(tk.getMaTaiKhoan(), "PASSWORD_CHANGED", "accounts",
					String.valueOf(tk.getMaTaiKhoan()),
					tkPhien.isDangNhapLanDau() ? "Đổi mật khẩu lần đầu" : "Người dùng tự đổi mật khẩu");

			// Cập nhật bản sao trong phiên để lần đổi sau không cần đăng nhập lại
			tkPhien.setMatKhauHash(hashMoi);
			tkPhien.setDangNhapLanDau(false);

			return KetQuaXuLy.thanhCong("Đổi mật khẩu thành công.");

		} catch (SQLException e) {
			e.printStackTrace();
			return KetQuaXuLy.thatBai("Không thể cập nhật mật khẩu.");
		}
	}

	/** Hồ sơ mới nhất của tài khoản đang đăng nhập (đọc lại từ CSDL). */
	public TaiKhoan layHoSoHienTai() throws SQLException {
		if (!PhienDangNhap.daDangNhap()) {
			return null;
		}
		return taiKhoanDAO.timTheoMa(PhienDangNhap.getTaiKhoanHienTai().getMaTaiKhoan());
	}

	// ===================== ĐĂNG XUẤT =====================

	public void dangXuat() {
		Long maNhatKy = PhienDangNhap.getMaNhatKy();
		if (maNhatKy != null) {
			try {
				nhatKyDAO.ghiDangXuat(maNhatKy.longValue());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		PhienDangNhap.xoaPhien();
	}

	/** Thông tin máy đăng nhập, chỉ để ghi nhật ký. */
	private String layThongTinMay() {
		String tenMay = "unknown-host";
		try {
			tenMay = java.net.InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			// không lấy được tên máy thì bỏ qua, không làm hỏng đăng nhập
		}
		return tenMay + " | " + System.getProperty("os.name") + " | Java " + System.getProperty("java.version")
				+ " | " + System.getProperty("user.name");
	}
}

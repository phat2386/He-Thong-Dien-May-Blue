package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import connectDB.ConnectDB;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.VaiTro;

/** Bảng accounts (kèm thông tin nhân viên và vai trò). */
public class TaiKhoan_DAO {

	// Phần SELECT dùng chung: tài khoản + nhân viên + vai trò
	private static final String SELECT_TAI_KHOAN = "SELECT a.account_id, a.username, a.password_hash, a.status AS account_status, "
			+ "a.first_login, a.failed_attempts, a.locked_until, a.last_login_at, "
			+ "e.employee_id, e.full_name, e.phone, e.email, e.department, e.join_date, e.status AS employee_status, "
			+ "r.role_id, r.role_code, r.role_name, r.description "
			+ "FROM accounts a JOIN employees e ON e.employee_id = a.employee_id "
			+ "JOIN roles r ON r.role_id = a.role_id ";

	// ===================== ĐĂNG NHẬP =====================

	public TaiKhoan timTheoTenDangNhap(String tenDangNhap) throws SQLException {
		String sql = SELECT_TAI_KHOAN + "WHERE a.username = ?";
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, tenDangNhap);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? docTaiKhoan(rs) : null;
			}
		}
	}

	public TaiKhoan timTheoMa(int maTaiKhoan) throws SQLException {
		try (Connection con = ConnectDB.getConnection()) {
			return timTheoMa(con, maTaiKhoan);
		}
	}

	public TaiKhoan timTheoMa(Connection con, int maTaiKhoan) throws SQLException {
		String sql = SELECT_TAI_KHOAN + "WHERE a.account_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maTaiKhoan);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? docTaiKhoan(rs) : null;
			}
		}
	}

	/**
	 * Tăng số lần nhập sai. Sai lần thứ 6 trở đi thì khóa tạm 15 phút. Trả về số lần
	 * sai hiện tại.
	 */
	public int tangSoLanSai(int maTaiKhoan) throws SQLException {
		String capNhat = "UPDATE accounts SET failed_attempts = failed_attempts + 1, "
				+ "locked_until = CASE WHEN failed_attempts + 1 > 5 THEN DATEADD(MINUTE, 15, SYSDATETIME()) "
				+ "ELSE locked_until END, updated_at = SYSDATETIME() WHERE account_id = ?";
		String truyVan = "SELECT failed_attempts FROM accounts WHERE account_id = ?";
		Connection con = null;
		try {
			con = GiaoDich.mo();
			try (PreparedStatement ps = con.prepareStatement(capNhat)) {
				ps.setInt(1, maTaiKhoan);
				ps.executeUpdate();
			}
			int soLan;
			try (PreparedStatement ps = con.prepareStatement(truyVan)) {
				ps.setInt(1, maTaiKhoan);
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						throw new SQLException("Không tìm thấy tài khoản.");
					}
					soLan = rs.getInt("failed_attempts");
				}
			}
			con.commit();
			return soLan;
		} catch (SQLException e) {
			GiaoDich.hoanTac(con);
			throw e;
		} finally {
			GiaoDich.dong(con);
		}
	}

	public void xoaKhoaTamHetHan(int maTaiKhoan) throws SQLException {
		String sql = "UPDATE accounts SET failed_attempts = 0, locked_until = NULL, updated_at = SYSDATETIME() "
				+ "WHERE account_id = ? AND locked_until IS NOT NULL AND locked_until <= SYSDATETIME()";
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maTaiKhoan);
			ps.executeUpdate();
		}
	}

	public void ghiNhanDangNhapThanhCong(int maTaiKhoan) throws SQLException {
		String sql = "UPDATE accounts SET failed_attempts = 0, locked_until = NULL, "
				+ "last_login_at = SYSDATETIME(), updated_at = SYSDATETIME() WHERE account_id = ?";
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maTaiKhoan);
			ps.executeUpdate();
		}
	}

	public void capNhatMatKhau(int maTaiKhoan, String matKhauHash) throws SQLException {
		String sql = "UPDATE accounts SET password_hash = ?, first_login = 0, updated_at = SYSDATETIME() WHERE account_id = ?";
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, matKhauHash);
			ps.setInt(2, maTaiKhoan);
			ps.executeUpdate();
		}
	}

	// ===================== QUẢN LÝ TÀI KHOẢN =====================

	/** Tìm tài khoản (trừ ADMIN) theo từ khóa, vai trò, trạng thái. Để null nếu không lọc. */
	public List<TaiKhoan> timKiem(String tuKhoa, String codeVaiTro, String trangThai) throws SQLException {
		String sql = SELECT_TAI_KHOAN + "WHERE r.role_code <> 'ADMIN' "
				+ "AND (e.full_name LIKE ? OR a.username LIKE ? OR e.phone LIKE ? OR e.employee_id LIKE ?) "
				+ (codeVaiTro == null ? "" : "AND r.role_code = ? ")
				+ (trangThai == null ? "" : "AND a.status = ? ") + "ORDER BY e.full_name";
		String kw = "%" + tuKhoa + "%";
		List<TaiKhoan> ds = new ArrayList<TaiKhoan>();
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			int i = 1;
			ps.setString(i++, kw);
			ps.setString(i++, kw);
			ps.setString(i++, kw);
			ps.setString(i++, kw);
			if (codeVaiTro != null) {
				ps.setString(i++, codeVaiTro);
			}
			if (trangThai != null) {
				ps.setString(i++, trangThai);
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					ds.add(docTaiKhoan(rs));
				}
			}
		}
		return ds;
	}

	/** Tài khoản của một nhân viên (null nếu chưa cấp). Dùng trong giao dịch. */
	public TaiKhoan timTheoMaNhanVien(Connection con, String maNhanVien) throws SQLException {
		String sql = SELECT_TAI_KHOAN + "WHERE a.employee_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? docTaiKhoan(rs) : null;
			}
		}
	}

	/** Nhân viên đã có tài khoản hoặc tên đăng nhập đã bị dùng? */
	public boolean daTonTai(Connection con, String maNhanVien, String tenDangNhap) throws SQLException {
		String sql = "SELECT COUNT(*) FROM accounts WHERE employee_id = ? OR username = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.setString(2, tenDangNhap);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		}
	}

	/** Tên đăng nhập đã thuộc tài khoản khác? */
	public boolean trungTenDangNhap(Connection con, String tenDangNhap, int maTaiKhoan) throws SQLException {
		String sql = "SELECT COUNT(*) FROM accounts WHERE username = ? AND account_id <> ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, tenDangNhap);
			ps.setInt(2, maTaiKhoan);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		}
	}

	/** Thêm tài khoản mới (trạng thái ACTIVE, bắt buộc đổi mật khẩu lần đầu). Trả về mã tài khoản. */
	public int them(Connection con, String maNhanVien, String tenDangNhap, String matKhauHash, int maVaiTro)
			throws SQLException {
		String sql = "INSERT INTO accounts(employee_id, username, password_hash, role_id, status, first_login) "
				+ "VALUES(?, ?, ?, ?, 'ACTIVE', 1)";
		try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, maNhanVien);
			ps.setString(2, tenDangNhap);
			ps.setString(3, matKhauHash);
			ps.setInt(4, maVaiTro);
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
			}
		}
		throw new SQLException("Không lấy được mã tài khoản vừa tạo.");
	}

	public void capNhatTenVaVaiTro(Connection con, int maTaiKhoan, String tenDangNhap, int maVaiTro)
			throws SQLException {
		String sql = "UPDATE accounts SET username = ?, role_id = ?, updated_at = SYSDATETIME() WHERE account_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, tenDangNhap);
			ps.setInt(2, maVaiTro);
			ps.setInt(3, maTaiKhoan);
			ps.executeUpdate();
		}
	}

	/** Đổi trạng thái; nếu chuyển về ACTIVE thì xóa luôn số lần sai và thời gian khóa tạm. */
	public void doiTrangThai(Connection con, int maTaiKhoan, String trangThaiMoi) throws SQLException {
		String sql = "UPDATE accounts SET status = ?, "
				+ "failed_attempts = CASE WHEN ? = 'ACTIVE' THEN 0 ELSE failed_attempts END, "
				+ "locked_until = CASE WHEN ? = 'ACTIVE' THEN NULL ELSE locked_until END, "
				+ "updated_at = SYSDATETIME() WHERE account_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, trangThaiMoi);
			ps.setString(2, trangThaiMoi);
			ps.setString(3, trangThaiMoi);
			ps.setInt(4, maTaiKhoan);
			ps.executeUpdate();
		}
	}

	/** Vô hiệu hóa tài khoản (giữ lại lịch sử). */
	public void voHieuHoa(Connection con, int maTaiKhoan) throws SQLException {
		String sql = "UPDATE accounts SET status = 'DISABLED', updated_at = SYSDATETIME() WHERE account_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maTaiKhoan);
			ps.executeUpdate();
		}
	}

	public void xoa(Connection con, int maTaiKhoan) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement("DELETE FROM accounts WHERE account_id = ?")) {
			ps.setInt(1, maTaiKhoan);
			ps.executeUpdate();
		}
	}

	/** Số quản lý đang hoạt động (cả tài khoản lẫn hồ sơ nhân viên) - để không khóa nhầm quản lý cuối cùng. */
	public int demQuanLyDangHoatDong(Connection con) throws SQLException {
		String sql = "SELECT COUNT(*) FROM accounts a JOIN employees e ON e.employee_id = a.employee_id "
				+ "JOIN roles r ON r.role_id = a.role_id "
				+ "WHERE r.role_code = 'MANAGER' AND a.status = 'ACTIVE' AND e.status = 'ACTIVE'";
		try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			return rs.next() ? rs.getInt(1) : 0;
		}
	}

	/** Số dòng lịch sử (đăng nhập + nhật ký hệ thống) gắn với tài khoản. */
	public int demLichSu(Connection con, int maTaiKhoan) throws SQLException {
		String sql = "SELECT (SELECT COUNT(*) FROM login_logs WHERE account_id = ?) "
				+ "+ (SELECT COUNT(*) FROM audit_logs WHERE account_id = ?)";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maTaiKhoan);
			ps.setInt(2, maTaiKhoan);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	/** Tài khoản đang hoạt động của nhân viên bán hàng / CSKH (để phân ca). */
	public List<TaiKhoan> layTaiKhoanCoTheXepCa() throws SQLException {
		String sql = SELECT_TAI_KHOAN + "WHERE e.status = 'ACTIVE' AND a.status = 'ACTIVE' "
				+ "AND r.role_code IN ('SALES','CUSTOMER_SERVICE') ORDER BY e.full_name";
		List<TaiKhoan> ds = new ArrayList<TaiKhoan>();
		try (Connection con = ConnectDB.getConnection();
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				ds.add(docTaiKhoan(rs));
			}
		}
		return ds;
	}

	// ===================== ĐỌC DỮ LIỆU =====================

	private TaiKhoan docTaiKhoan(ResultSet rs) throws SQLException {
		NhanVien nv = new NhanVien();
		nv.setMaNhanVien(rs.getString("employee_id"));
		nv.setHoTen(rs.getString("full_name"));
		nv.setSoDienThoai(rs.getString("phone"));
		nv.setEmail(rs.getString("email"));
		nv.setPhongBan(rs.getString("department"));
		if (rs.getDate("join_date") != null) {
			nv.setNgayVaoLam(rs.getDate("join_date").toLocalDate());
		}
		nv.setTrangThai(rs.getString("employee_status"));

		VaiTro vt = new VaiTro(rs.getInt("role_id"), rs.getString("role_code"), rs.getString("role_name"),
				rs.getString("description"));

		TaiKhoan tk = new TaiKhoan();
		tk.setMaTaiKhoan(rs.getInt("account_id"));
		tk.setTenDangNhap(rs.getString("username"));
		tk.setMatKhauHash(rs.getString("password_hash"));
		tk.setTrangThai(rs.getString("account_status"));
		tk.setDangNhapLanDau(rs.getBoolean("first_login"));
		tk.setSoLanSai(rs.getInt("failed_attempts"));
		Timestamp khoa = rs.getTimestamp("locked_until");
		if (khoa != null) {
			tk.setKhoaDen(khoa.toLocalDateTime());
		}
		Timestamp cuoi = rs.getTimestamp("last_login_at");
		if (cuoi != null) {
			tk.setDangNhapCuoi(cuoi.toLocalDateTime());
		}
		tk.setNhanVien(nv);
		tk.setVaiTro(vt);
		return tk;
	}
}

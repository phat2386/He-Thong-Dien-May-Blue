package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import connectDB.ConnectDB;

/** Bảng login_logs: ghi lại mỗi lần đăng nhập (thành công / thất bại) và đăng xuất. */
public class NhatKyDangNhap_DAO {

	/** Thêm một dòng nhật ký, trả về mã nhật ký vừa tạo. */
	public long them(Integer maTaiKhoan, String tenDaNhap, boolean thanhCong, String lyDoThatBai, String thongTinMay)
			throws SQLException {
		String sql = "INSERT INTO login_logs (account_id, username_entered, success, failure_reason, device_info) "
				+ "VALUES (?, ?, ?, ?, ?)";
		try (Connection con = ConnectDB.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			if (maTaiKhoan == null) {
				ps.setNull(1, Types.INTEGER);
			} else {
				ps.setInt(1, maTaiKhoan.intValue());
			}
			ps.setString(2, tenDaNhap);
			ps.setBoolean(3, thanhCong);
			ps.setString(4, lyDoThatBai);
			ps.setString(5, thongTinMay);
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getLong(1);
				}
			}
		}
		throw new SQLException("Không lấy được mã nhật ký đăng nhập.");
	}

	public void ghiDangXuat(long maNhatKy) throws SQLException {
		String sql = "UPDATE login_logs SET logout_at = SYSDATETIME() WHERE login_log_id = ? AND logout_at IS NULL";
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setLong(1, maNhatKy);
			ps.executeUpdate();
		}
	}

	/** Kết thúc mọi phiên đang mở của tài khoản (khi khóa / vô hiệu hóa / xóa tài khoản). */
	public void ketThucPhienCuaTaiKhoan(Connection con, int maTaiKhoan) throws SQLException {
		String sql = "UPDATE login_logs SET logout_at = COALESCE(logout_at, SYSDATETIME()) "
				+ "WHERE account_id = ? AND success = 1 AND logout_at IS NULL";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maTaiKhoan);
			ps.executeUpdate();
		}
	}

	/** Kết thúc mọi phiên đang mở của tài khoản thuộc một nhân viên. */
	public void ketThucPhienCuaNhanVien(Connection con, String maNhanVien) throws SQLException {
		String sql = "UPDATE login_logs SET logout_at = COALESCE(logout_at, SYSDATETIME()) "
				+ "WHERE account_id = (SELECT account_id FROM accounts WHERE employee_id = ?) "
				+ "AND success = 1 AND logout_at IS NULL";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.executeUpdate();
		}
	}

	/** Tài khoản có đang đăng nhập ở đâu đó không? */
	public boolean dangCoPhien(int maTaiKhoan) throws SQLException {
		String sql = "SELECT COUNT(*) FROM login_logs WHERE account_id = ? AND logout_at IS NULL AND success = 1";
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maTaiKhoan);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		}
	}

	/** Đếm số dòng nhật ký của tài khoản (để biết có xóa hẳn được không). */
	public int demTheoTaiKhoan(Connection con, int maTaiKhoan) throws SQLException {
		String sql = "SELECT COUNT(*) FROM login_logs WHERE account_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maTaiKhoan);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}
}

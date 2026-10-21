package dao;

import java.sql.Connection;
import java.sql.SQLException;

import connectDB.ConnectDB;

/** Hỗ trợ chạy nhiều câu lệnh SQL trong cùng một giao dịch (mở, hoàn tác, đóng). */
public final class GiaoDich {
	private GiaoDich() {
	}

	/** Mở kết nối và tắt tự động commit. */
	public static Connection mo() throws SQLException {
		Connection con = ConnectDB.getConnection();
		con.setAutoCommit(false);
		return con;
	}

	/** Hoàn tác giao dịch (bỏ qua lỗi nếu có). */
	public static void hoanTac(Connection con) {
		if (con != null) {
			try {
				con.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void dong(Connection con) {
		if (con != null) {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}

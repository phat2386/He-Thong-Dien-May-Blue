package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import connectDB.ConnectDB;

/** Bảng role_permissions / permissions: vai trò nào được dùng chức năng nào. */
public class PhanQuyen_DAO {

	public Set<String> layDanhSachQuyenTheoVaiTro(int maVaiTro) throws SQLException {
		String sql = "SELECT p.permission_code FROM role_permissions rp "
				+ "JOIN permissions p ON p.permission_id = rp.permission_id "
				+ "WHERE rp.role_id = ? AND rp.granted = 1";
		Set<String> ketQua = new HashSet<String>();
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maVaiTro);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					ketQua.add(rs.getString("permission_code"));
				}
			}
		}
		return ketQua;
	}
}

package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import connectDB.ConnectDB;
import entity.VaiTro;

/** Bảng roles. */
public class VaiTro_DAO {

	private VaiTro docVaiTro(ResultSet rs) throws SQLException {
		return new VaiTro(rs.getInt("role_id"), rs.getString("role_code"), rs.getString("role_name"),
				rs.getString("description"));
	}

	public VaiTro timTheoMa(Connection con, int maVaiTro) throws SQLException {
		String sql = "SELECT role_id, role_code, role_name, description FROM roles WHERE role_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maVaiTro);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? docVaiTro(rs) : null;
			}
		}
	}

	/** Các vai trò có thể gán cho nhân viên (không gồm ADMIN). */
	public List<VaiTro> layVaiTroGanDuoc() throws SQLException {
		String sql = "SELECT role_id, role_code, role_name, description FROM roles "
				+ "WHERE role_code IN ('MANAGER','SALES','CUSTOMER_SERVICE') ORDER BY role_id";
		List<VaiTro> ds = new ArrayList<VaiTro>();
		try (Connection con = ConnectDB.getConnection();
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				ds.add(docVaiTro(rs));
			}
		}
		return ds;
	}
}

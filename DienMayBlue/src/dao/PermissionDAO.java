package truycapdulieuDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import cauhinhConfig.DatabaseConnection;

public class PermissionDAO {

    public Set<String> findPermissionCodesByRoleId(int roleId)
            throws SQLException {

        String sql =
                "SELECT p.permission_code "
              + "FROM role_permissions rp "
              + "JOIN permissions p "
              + "  ON p.permission_id = rp.permission_id "
              + "WHERE rp.role_id = ? AND rp.granted = 1";

        Set<String> result = new HashSet<String>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roleId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("permission_code"));
                }
            }
        }

        return result;
    }
}

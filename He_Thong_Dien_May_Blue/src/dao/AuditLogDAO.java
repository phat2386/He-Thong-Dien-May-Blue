package dao;

import connectDB.ConnectDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class AuditLogDAO {

    public void insert(Integer accountId, String actionCode,
                       String entityName, String entityId,
                       String detail) throws SQLException {

        String sql =
                "INSERT INTO audit_logs "
              + "(account_id, action_code, entity_name, entity_id, detail) "
              + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (accountId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, accountId.intValue());
            }

            ps.setString(2, actionCode);
            ps.setString(3, entityName);
            ps.setString(4, entityId);
            ps.setString(5, detail);
            ps.executeUpdate();
        }
    }

    /** Ghi nhật ký hệ thống trong cùng giao dịch với các thao tác khác. */
    public void insert(Connection con, Integer accountId, String actionCode,
                       String entityName, String entityId,
                       String detail) throws SQLException {

        String sql =
                "INSERT INTO audit_logs "
              + "(account_id, action_code, entity_name, entity_id, detail) "
              + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (accountId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, accountId.intValue());
            }
            ps.setString(2, actionCode);
            ps.setString(3, entityName);
            ps.setString(4, entityId);
            ps.setString(5, detail);
            ps.executeUpdate();
        }
    }
}

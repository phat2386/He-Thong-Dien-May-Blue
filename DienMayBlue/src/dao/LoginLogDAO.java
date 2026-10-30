package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import database.DatabaseConnection;

public class LoginLogDAO {

    public long insert(Integer accountId, String usernameEntered,
                       boolean success, String failureReason,
                       String deviceInfo) throws SQLException {

        String sql =
                "INSERT INTO login_logs "
              + "(account_id, username_entered, success, failure_reason, device_info) "
              + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            if (accountId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, accountId.intValue());
            }

            ps.setString(2, usernameEntered);
            ps.setBoolean(3, success);
            ps.setString(4, failureReason);
            ps.setString(5, deviceInfo);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Không lấy được mã nhật ký đăng nhập.");
    }

    public void markLogout(long loginLogId) throws SQLException {
        String sql =
                "UPDATE login_logs "
              + "SET logout_at = SYSDATETIME() "
              + "WHERE login_log_id = ? AND logout_at IS NULL";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, loginLogId);
            ps.executeUpdate();
        }
    }
}

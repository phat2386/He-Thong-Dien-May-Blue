package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import connectDB.ConnectDB;
import entity.Account;
import entity.Employee;
import entity.Role;

/** Truy cập dữ liệu tài khoản, nhật ký đăng nhập, nhật ký thao tác và quyền. */
public class AccountDAO {

    private static final String FIND_BY_USERNAME =
            "SELECT "
          + "a.account_id, a.username, a.password_hash, a.status AS account_status, "
          + "a.first_login, a.failed_attempts, a.locked_until, a.last_login_at, "
          + "e.employee_id, e.full_name, e.phone, e.email, e.department, "
          + "e.join_date, e.status AS employee_status, "
          + "r.role_id, r.role_code, r.role_name, r.description "
          + "FROM accounts a "
          + "JOIN employees e ON e.employee_id = a.employee_id "
          + "JOIN roles r ON r.role_id = a.role_id "
          + "WHERE a.username = ?";

    public Account findByUsername(String username) throws SQLException {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(FIND_BY_USERNAME)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapAccount(rs);
            }
        }
    }

    public Account findById(int accountId) throws SQLException {
        String sql =
                "SELECT "
              + "a.account_id, a.username, a.password_hash, a.status AS account_status, "
              + "a.first_login, a.failed_attempts, a.locked_until, a.last_login_at, "
              + "e.employee_id, e.full_name, e.phone, e.email, e.department, "
              + "e.join_date, e.status AS employee_status, "
              + "r.role_id, r.role_code, r.role_name, r.description "
              + "FROM accounts a "
              + "JOIN employees e ON e.employee_id = a.employee_id "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE a.account_id = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapAccount(rs) : null;
            }
        }
    }

    /**
     * Tăng số lần nhập sai. Theo SRS, chỉ khóa tạm khi số lần sai > 5,
     * tức lần sai thứ 6 sẽ khóa 15 phút.
     */
    public int registerFailedAttempt(int accountId) throws SQLException {
        String update =
                "UPDATE accounts "
              + "SET failed_attempts = failed_attempts + 1, "
              + "locked_until = CASE "
              + "  WHEN failed_attempts + 1 > 5 "
              + "  THEN DATEADD(MINUTE, 15, SYSDATETIME()) "
              + "  ELSE locked_until END, "
              + "updated_at = SYSDATETIME() "
              + "WHERE account_id = ?";

        String query =
                "SELECT failed_attempts FROM accounts WHERE account_id = ?";

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(update)) {
                ps.setInt(1, accountId);
                ps.executeUpdate();
            }

            int attempts;
            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setInt(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Không tìm thấy tài khoản.");
                    }
                    attempts = rs.getInt("failed_attempts");
                }
            }

            con.commit();
            return attempts;
        }
    }

    public void clearExpiredTemporaryLock(int accountId) throws SQLException {
        String sql =
                "UPDATE accounts "
              + "SET failed_attempts = 0, locked_until = NULL, "
              + "updated_at = SYSDATETIME() "
              + "WHERE account_id = ? "
              + "AND locked_until IS NOT NULL "
              + "AND locked_until <= SYSDATETIME()";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
        }
    }

    public void markLoginSuccess(int accountId) throws SQLException {
        String sql =
                "UPDATE accounts "
              + "SET failed_attempts = 0, locked_until = NULL, "
              + "last_login_at = SYSDATETIME(), updated_at = SYSDATETIME() "
              + "WHERE account_id = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
        }
    }

    public void updatePassword(int accountId, String passwordHash)
            throws SQLException {
        String sql =
                "UPDATE accounts "
              + "SET password_hash = ?, first_login = 0, "
              + "updated_at = SYSDATETIME() "
              + "WHERE account_id = ?";

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    // ===== Danh sách và tra cứu cho màn hình quản lý tài khoản =====

    public List<Map<String, Object>> searchAccounts(
            String keyword, String roleCode, String status) throws SQLException {

        String sql =
                "SELECT a.account_id, e.employee_id, e.full_name, a.username, "
              + "r.role_code, e.phone, a.status, a.last_login_at "
              + "FROM accounts a "
              + "JOIN employees e ON e.employee_id = a.employee_id "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE r.role_code <> 'ADMIN' "
              + "AND (e.full_name LIKE ? OR a.username LIKE ? "
              + "OR e.phone LIKE ? OR e.employee_id LIKE ?)";

        List<Object> params = new ArrayList<Object>();
        params.add(keyword);
        params.add(keyword);
        params.add(keyword);
        params.add(keyword);

        if (roleCode != null) {
            sql += " AND r.role_code = ?";
            params.add(roleCode);
        }
        if (status != null) {
            sql += " AND a.status = ?";
            params.add(status);
        }
        sql += " ORDER BY e.full_name";

        return queryList(sql, params.toArray());
    }

    public List<Map<String, Object>> findAvailableEmployees()
            throws SQLException {

        return queryList(
                "SELECT e.employee_id, e.full_name, e.phone "
              + "FROM employees e "
              + "WHERE e.status = 'ACTIVE' "
              + "AND NOT EXISTS (SELECT 1 FROM accounts a "
              + "                WHERE a.employee_id = e.employee_id) "
              + "ORDER BY e.full_name");
    }

    public List<Map<String, Object>> findManageableRoles() throws SQLException {
        return queryList(
                "SELECT role_id, role_name, role_code FROM roles "
              + "WHERE role_code IN ('MANAGER', 'SALES', 'CUSTOMER_SERVICE') "
              + "ORDER BY role_id");
    }

    public Map<String, Object> findAccountInfo(int accountId)
            throws SQLException {

        return queryOne(
                "SELECT a.account_id, a.employee_id, a.username, a.role_id, e.full_name "
              + "FROM accounts a "
              + "JOIN employees e ON e.employee_id = a.employee_id "
              + "WHERE a.account_id = ?",
                accountId);
    }

    public String findStatus(int accountId) throws SQLException {
        Object status = queryValue(
                "SELECT status FROM accounts WHERE account_id = ?", accountId);
        return status == null ? null : String.valueOf(status);
    }

    /** Trạng thái tài khoản, nhân viên và vai trò hiện tại trong CSDL (dùng kiểm tra phiên). */
    public Map<String, Object> findSessionState(int accountId)
            throws SQLException {

        return queryOne(
                "SELECT a.status AS account_status, e.status AS employee_status, a.role_id "
              + "FROM accounts a "
              + "JOIN employees e ON e.employee_id = a.employee_id "
              + "WHERE a.account_id = ?",
                accountId);
    }

    /** Gồm role_id, role_code, status, employee_status của tài khoản. */
    public Map<String, Object> findBrief(int accountId) throws SQLException {
        return queryOne(
                "SELECT a.role_id, r.role_code, a.status, e.status AS employee_status "
              + "FROM accounts a "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "JOIN employees e ON e.employee_id = a.employee_id "
              + "WHERE a.account_id = ?",
                accountId);
    }

    public String findRoleCode(int roleId) throws SQLException {
        Object code = queryValue(
                "SELECT role_code FROM roles WHERE role_id = ?", roleId);
        return code == null ? null : String.valueOf(code);
    }

    public boolean existsByEmployeeOrUsername(String employeeId, String username)
            throws SQLException {

        return queryExists(
                "SELECT COUNT(*) FROM accounts WHERE employee_id = ? OR username = ?",
                employeeId, username);
    }

    public boolean existsUsernameExcept(String username, int accountId)
            throws SQLException {

        return queryExists(
                "SELECT COUNT(*) FROM accounts WHERE username = ? AND account_id <> ?",
                username, accountId);
    }

    /** Tài khoản đã có nhật ký đăng nhập hoặc nhật ký thao tác thì không được xóa vật lý. */
    public boolean hasHistory(int accountId) throws SQLException {
        return queryExists(
                "SELECT (SELECT COUNT(*) FROM login_logs WHERE account_id = ?) "
              + "     + (SELECT COUNT(*) FROM audit_logs WHERE account_id = ?)",
                accountId, accountId);
    }

    // ===== Thêm / sửa / xóa (chạy trong transaction do service quản lý) =====

    public long insert(Connection con, String employeeId, String username,
                       String passwordHash, int roleId) throws SQLException {

        String sql =
                "INSERT INTO accounts "
              + "(employee_id, username, password_hash, role_id, status, first_login) "
              + "VALUES (?, ?, ?, ?, 'ACTIVE', 1)";

        try (PreparedStatement ps = con.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, employeeId);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            ps.setInt(4, roleId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Không lấy được mã tài khoản vừa tạo.");
    }

    public void updateInfo(Connection con, int accountId,
                           String username, int roleId) throws SQLException {

        runUpdate(con,
                "UPDATE accounts SET username = ?, role_id = ?, "
              + "updated_at = SYSDATETIME() WHERE account_id = ?",
                username, roleId, accountId);
    }

    public void updateStatus(Connection con, int accountId, String newStatus)
            throws SQLException {

        runUpdate(con,
                "UPDATE accounts SET status = ?, "
              + "failed_attempts = CASE WHEN ? = 'ACTIVE' THEN 0 ELSE failed_attempts END, "
              + "locked_until = CASE WHEN ? = 'ACTIVE' THEN NULL ELSE locked_until END, "
              + "updated_at = SYSDATETIME() WHERE account_id = ?",
                newStatus, newStatus, newStatus, accountId);
    }

    public void disable(Connection con, int accountId) throws SQLException {
        runUpdate(con,
                "UPDATE accounts SET status = 'DISABLED', "
              + "updated_at = SYSDATETIME() WHERE account_id = ?",
                accountId);
    }

    public void delete(Connection con, int accountId) throws SQLException {
        runUpdate(con,
                "DELETE FROM accounts WHERE account_id = ?", accountId);
    }

    public int countActiveManagers(Connection con) throws SQLException {
        return queryCount(con,
                "SELECT COUNT(*) FROM accounts a "
              + "JOIN employees e ON e.employee_id = a.employee_id "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE r.role_code = 'MANAGER' "
              + "AND a.status = 'ACTIVE' AND e.status = 'ACTIVE'");
    }

    /** Dùng cho các kiểm tra "quản lý cuối cùng" khi chưa mở transaction. */
    public int countActiveManagers() throws SQLException {
        try (Connection con = ConnectDB.getConnection()) {
            return countActiveManagers(con);
        }
    }

    // ===== Nhật ký đăng nhập =====

    /** Ghi một lần đăng nhập (thành công hoặc thất bại), trả về mã dòng nhật ký. */
    public long insertLoginLog(Integer accountId, String usernameEntered,
                               boolean success, String failureReason,
                               String deviceInfo) throws SQLException {

        String sql =
                "INSERT INTO login_logs "
              + "(account_id, username_entered, success, failure_reason, device_info) "
              + "VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConnectDB.getConnection();
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
        try (Connection con = ConnectDB.getConnection()) {
            runUpdate(con,
                    "UPDATE login_logs SET logout_at = SYSDATETIME() "
                  + "WHERE login_log_id = ? AND logout_at IS NULL",
                    loginLogId);
        }
    }

    public boolean hasActiveSession(int accountId) throws SQLException {
        return queryExists(
                "SELECT COUNT(*) FROM login_logs "
              + "WHERE account_id = ? AND success = 1 AND logout_at IS NULL",
                accountId);
    }

    /** Kết thúc mọi phiên đăng nhập còn mở của một tài khoản. */
    public void closeLoginSessions(Connection con, int accountId) throws SQLException {
        runUpdate(con,
                "UPDATE login_logs "
              + "SET logout_at = COALESCE(logout_at, SYSDATETIME()) "
              + "WHERE account_id = ? AND success = 1 AND logout_at IS NULL",
                accountId);
    }

    // ===== Quyền của vai trò =====

    public Set<String> findPermissionCodesByRoleId(int roleId) throws SQLException {
        String sql =
                "SELECT p.permission_code "
              + "FROM role_permissions rp "
              + "JOIN permissions p ON p.permission_id = rp.permission_id "
              + "WHERE rp.role_id = ? AND rp.granted = 1";

        Set<String> result = new HashSet<String>();

        for (Map<String, Object> row : queryList(sql, roleId)) {
            result.add(String.valueOf(row.get("permission_code")));
        }
        return result;
    }

    // ===== Nhật ký thao tác =====

    /** Ghi nhật ký cùng transaction với thao tác nghiệp vụ. */
    public void insertAuditLog(Connection con, int accountId, String actionCode,
                               String entityName, String entityId, String detail)
            throws SQLException {

        runUpdate(con,
                "INSERT INTO audit_logs "
              + "(account_id, action_code, entity_name, entity_id, detail) "
              + "VALUES (?, ?, ?, ?, ?)",
                accountId, actionCode, entityName, entityId, detail);
    }

    /** Ghi nhật ký riêng, không nằm trong transaction nào. */
    public void insertAuditLog(int accountId, String actionCode,
                               String entityName, String entityId, String detail)
            throws SQLException {

        try (Connection con = ConnectDB.getConnection()) {
            insertAuditLog(con, accountId, actionCode, entityName, entityId, detail);
        }
    }

    // ===== Hàm dùng chung trong lớp này (gom phần JDBC lặp lại) =====

    /** Lấy nhiều dòng; mỗi dòng là một Map: tên cột -> giá trị. */
    private static List<Map<String, Object>> queryList(String sql, Object... params)
            throws SQLException {

        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            setParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                return readRows(rs);
            }
        }
    }

    /** Lấy 1 dòng, không có thì trả về null. */
    private static Map<String, Object> queryOne(String sql, Object... params)
            throws SQLException {

        List<Map<String, Object>> rows = queryList(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Lấy 1 dòng, dùng chung connection của transaction đang chạy. */
    private static Map<String, Object> queryOne(Connection con, String sql, Object... params)
            throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = readRows(rs);
                return rows.isEmpty() ? null : rows.get(0);
            }
        }
    }

    /** Lấy 1 giá trị (cột đầu tiên của dòng đầu tiên). */
    private static Object queryValue(String sql, Object... params) throws SQLException {
        try (Connection con = ConnectDB.getConnection()) {
            return queryValue(con, sql, params);
        }
    }

    private static Object queryValue(Connection con, String sql, Object... params)
            throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    /** Câu lệnh COUNT(*) trả về > 0 thì là có. */
    private static boolean queryExists(String sql, Object... params) throws SQLException {
        Object count = queryValue(sql, params);
        return count != null && ((Number) count).intValue() > 0;
    }

    private static boolean queryExists(Connection con, String sql, Object... params)
            throws SQLException {
        Object count = queryValue(con, sql, params);
        return count != null && ((Number) count).intValue() > 0;
    }

    private static int queryCount(Connection con, String sql, Object... params)
            throws SQLException {
        Object count = queryValue(con, sql, params);
        return count == null ? 0 : ((Number) count).intValue();
    }

    /** Chạy câu lệnh INSERT / UPDATE / DELETE. */
    private static int runUpdate(Connection con, String sql, Object... params)
            throws SQLException {

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        }
    }

    private static List<Map<String, Object>> readRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        ResultSetMetaData md = rs.getMetaData();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                row.put(md.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }

    private static void setParams(PreparedStatement ps, Object... params)
            throws SQLException {

        for (int i = 0; i < params.length; i++) {
            Object p = params[i];

            if (p instanceof java.time.LocalDate) {
                ps.setDate(i + 1, java.sql.Date.valueOf((java.time.LocalDate) p));
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        Employee employee = new Employee();
        employee.setEmployeeId(rs.getString("employee_id"));
        employee.setFullName(rs.getString("full_name"));
        employee.setPhone(rs.getString("phone"));
        employee.setEmail(rs.getString("email"));
        employee.setDepartment(rs.getString("department"));

        if (rs.getDate("join_date") != null) {
            employee.setJoinDate(rs.getDate("join_date").toLocalDate());
        }
        employee.setStatus(rs.getString("employee_status"));

        Role role = new Role(
                rs.getInt("role_id"),
                rs.getString("role_code"),
                rs.getString("role_name"),
                rs.getString("description"));

        Account account = new Account();
        account.setAccountId(rs.getInt("account_id"));
        account.setUsername(rs.getString("username"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setStatus(rs.getString("account_status"));
        account.setFirstLogin(rs.getBoolean("first_login"));
        account.setFailedAttempts(rs.getInt("failed_attempts"));

        Timestamp locked = rs.getTimestamp("locked_until");
        if (locked != null) {
            account.setLockedUntil(locked.toLocalDateTime());
        }

        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        if (lastLogin != null) {
            account.setLastLoginAt(lastLogin.toLocalDateTime());
        }

        account.setEmployee(employee);
        account.setRole(role);
        return account;
    }
}

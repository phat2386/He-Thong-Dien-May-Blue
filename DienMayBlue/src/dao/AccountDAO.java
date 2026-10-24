package dao;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import database.Db;
import entity.Account;
import entity.Employee;
import entity.Role;

public class AccountDAO extends BaseDAO {

    private static final String ACCOUNT_SELECT =
            "SELECT a.account_id,a.username,a.password_hash,a.status account_status,a.first_login,a.failed_attempts," +
            "a.locked_until,a.last_login_at,e.employee_id,e.full_name,e.phone,e.email,e.department,e.join_date," +
            "e.status employee_status,r.role_id,r.role_code,r.role_name,r.description " +
            "FROM accounts a JOIN employees e ON e.employee_id=a.employee_id " + "JOIN roles r ON r.role_id=a.role_id ";

    public Account findByUsername(String username) throws SQLException {
        Map<String, Object> row = Db.one( ACCOUNT_SELECT + "WHERE a.username=?", username);
        return map(row);
    }

    public Account findById(int id) throws SQLException {
        Map<String, Object> row = Db.one( ACCOUNT_SELECT + "WHERE a.account_id=?", id);
        return map(row);
    }

    public int registerFailedAttempt(int id) throws Exception {
        return Db.transaction(connection -> {
            Db.update(connection, "UPDATE accounts SET failed_attempts=failed_attempts+1," +
                    "locked_until=CASE WHEN failed_attempts+1>5 " + "THEN DATEADD(MINUTE,15,SYSDATETIME()) ELSE locked_until END," +
                    "updated_at=SYSDATETIME() WHERE account_id=?", id);
            Object attempts = Db.scalar( connection, "SELECT failed_attempts FROM accounts WHERE account_id=?", id);
            if (attempts == null) {
                throw new SQLException("Không tìm thấy tài khoản.");
            }
            return ((Number) attempts).intValue();
        });
    }

    public void clearExpiredTemporaryLock(int id) throws SQLException {
        Db.update(
                "UPDATE accounts SET failed_attempts=0,locked_until=NULL,updated_at=SYSDATETIME() " +
                "WHERE account_id=? AND locked_until<=SYSDATETIME()", id);
    }

    public void markLoginSuccess(int id) throws SQLException {
        Db.update(
                "UPDATE accounts SET failed_attempts=0,locked_until=NULL," +
                "last_login_at=SYSDATETIME(),updated_at=SYSDATETIME() WHERE account_id=?", id);
    }

    public void updatePassword(int id, String hash) throws SQLException {
        Db.update( "UPDATE accounts SET password_hash=?,first_login=0,updated_at=SYSDATETIME() WHERE account_id=?", hash, id);
    }

    public Set<String> findPermissionCodesByRoleId(int roleId) throws SQLException {
        Set<String> permissions = new HashSet<>();
        List<Map<String, Object>> rows = Db.query(
                "SELECT p.permission_code FROM role_permissions rp " + "JOIN permissions p ON p.permission_id=rp.permission_id " +
                "WHERE rp.role_id=? AND rp.granted=1", roleId);
        for (Map<String, Object> row : rows) {
            permissions.add(String.valueOf(row.get("permission_code")));
        }
        return permissions;
    }

    public long insertLoginLog(
            Integer accountId, String username, boolean success, String reason, String device) throws SQLException {
        return Db.insertAndKey(
                "INSERT INTO login_logs(account_id,username_entered,success,failure_reason,device_info) " + "VALUES(?,?,?,?,?)",
                accountId, username, success, reason, device);
    }

    public void markLogout(long id) throws SQLException {
        Db.update( "UPDATE login_logs SET logout_at=SYSDATETIME() " + "WHERE login_log_id=? AND logout_at IS NULL", id);
    }

    public void insertAuditLog(
            Integer accountId, String action, String entity, String entityId, String detail) throws SQLException {
        Db.update(
                "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)", accountId,
                action, entity, entityId, detail);
    }

    public List<Map<String, Object>> searchAccounts(
            String keyword, String roleCode, String status) throws SQLException {
        String sql = "SELECT a.account_id,e.employee_id,e.full_name,a.username,r.role_code,e.phone,a.status,a.last_login_at " +
                "FROM accounts a JOIN employees e ON e.employee_id=a.employee_id " + "JOIN roles r ON r.role_id=a.role_id " +
                "WHERE r.role_code<>'ADMIN' " + "AND (e.full_name LIKE ? OR a.username LIKE ? OR e.phone LIKE ? OR e.employee_id LIKE ?)";
        List<Object> params = new ArrayList<>( Arrays.asList(keyword, keyword, keyword, keyword));
        if (roleCode != null) {
            sql += " AND r.role_code=?";
            params.add(roleCode);
        }
        if (status != null) {
            sql += " AND a.status=?";
            params.add(status);
        }
        return Db.query(sql + " ORDER BY e.full_name", params.toArray());
    }

    public List<Map<String, Object>> availableEmployees() throws SQLException {
        return Db.query(
                "SELECT e.employee_id,e.full_name,e.phone FROM employees e " + "WHERE e.status='ACTIVE' " +
                "AND NOT EXISTS(SELECT 1 FROM accounts a WHERE a.employee_id=e.employee_id) " + "ORDER BY e.full_name");
    }

    public Map<String, Object> accountInfo(int id) throws SQLException {
        return Db.one(
                "SELECT a.account_id,a.employee_id,a.username,a.role_id,e.full_name " +
                "FROM accounts a JOIN employees e ON e.employee_id=a.employee_id " + "WHERE a.account_id=?", id);
    }

    public String accountStatus(int id) throws SQLException {
        Object value = Db.scalar( "SELECT status FROM accounts WHERE account_id=?", id);
        return value == null ? null : String.valueOf(value);
    }

    public boolean hasActiveSession(int id) throws SQLException {
        Object count = Db.scalar( "SELECT COUNT(*) FROM login_logs " + "WHERE account_id=? AND logout_at IS NULL AND success=1", id);
        return ((Number) count).intValue() > 0;
    }

    public List<Map<String, Object>> manageableRoles() throws SQLException {
        return Db.query(
                "SELECT role_id,role_name,role_code FROM roles " +
                "WHERE role_code IN ('MANAGER','SALES','CUSTOMER_SERVICE') ORDER BY role_id");
    }

    public Map<String, Object> sessionState(int id) throws SQLException {
        return Db.one(
                "SELECT a.status account_status,e.status employee_status,a.role_id " +
                "FROM accounts a JOIN employees e ON e.employee_id=a.employee_id " + "WHERE a.account_id=?", id);
    }

    private static Account map(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Employee employee = new Employee();
        employee.setEmployeeId(text(row, "employee_id"));
        employee.setFullName(text(row, "full_name"));
        employee.setPhone(text(row, "phone"));
        employee.setEmail(text(row, "email"));
        employee.setDepartment(text(row, "department"));
        employee.setStatus(text(row, "employee_status"));
        Object joinDate = row.get("join_date");
        if (joinDate instanceof java.sql.Date date) {
            employee.setJoinDate(date.toLocalDate());
        }
        Role role = new Role( number(row, "role_id"), text(row, "role_code"), text(row, "role_name"), text(row, "description"));
        Account account = new Account();
        account.setAccountId(number(row, "account_id"));
        account.setUsername(text(row, "username"));
        account.setPasswordHash(text(row, "password_hash"));
        account.setStatus(text(row, "account_status"));
        account.setFirstLogin(Boolean.TRUE.equals(row.get("first_login")));
        account.setFailedAttempts(number(row, "failed_attempts"));
        Object lockedUntil = row.get("locked_until");
        if (lockedUntil instanceof Timestamp timestamp) {
            account.setLockedUntil(timestamp.toLocalDateTime());
        }
        Object lastLoginAt = row.get("last_login_at");
        if (lastLoginAt instanceof Timestamp timestamp) {
            account.setLastLoginAt(timestamp.toLocalDateTime());
        }
        account.setEmployee(employee);
        account.setRole(role);
        return account;
    }

    private static String text(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static int number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }
}

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

public class AccountDAO {

	private static final String ACCOUNT_SELECT =
			"SELECT a.account_id,a.username,a.password_hash,a.status account_status,a.first_login,a.failed_attempts,"
			+ "a.locked_until,a.last_login_at,e.employee_id,e.full_name,e.phone,e.email,e.department,e.join_date,"
			+ "e.status employee_status,r.role_id,r.role_code,r.role_name,r.description "
			+ "FROM accounts a JOIN employees e ON e.employee_id=a.employee_id "
			+ "JOIN roles r ON r.role_id=a.role_id ";

	// ===== Tìm tài khoản =====

	public Account findByUsername(String username) {
		Account acc = null;
		try (Connection con = ConnectDB.getConnection();
				PreparedStatement stmt = con.prepareStatement(ACCOUNT_SELECT + "WHERE a.username=?")) {
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				acc = mapAccount(rs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return acc;
	}

	public Account findById(int id) {
		Account acc = null;
		try (Connection con = ConnectDB.getConnection();
				PreparedStatement stmt = con.prepareStatement(ACCOUNT_SELECT + "WHERE a.account_id=?")) {
			stmt.setInt(1, id);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				acc = mapAccount(rs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return acc;
	}

	// ===== Đăng nhập =====

	/** Tăng số lần sai, quá 5 lần thì khóa tạm 15 phút. Trả về số lần sai hiện tại, -1 nếu lỗi. */
	public int registerFailedAttempt(int id) {
		String sqlUpdate = "UPDATE accounts SET failed_attempts=failed_attempts+1,"
				+ "locked_until=CASE WHEN failed_attempts+1>5 THEN DATEADD(MINUTE,15,SYSDATETIME()) ELSE locked_until END,"
				+ "updated_at=SYSDATETIME() WHERE account_id=?";
		String sqlSelect = "SELECT failed_attempts FROM accounts WHERE account_id=?";
		try (Connection con = ConnectDB.getConnection()) {
			con.setAutoCommit(false);
			try (PreparedStatement up = con.prepareStatement(sqlUpdate);
					PreparedStatement sel = con.prepareStatement(sqlSelect)) {
				up.setInt(1, id);
				up.executeUpdate();
				sel.setInt(1, id);
				ResultSet rs = sel.executeQuery();
				int attempts = rs.next() ? rs.getInt(1) : -1;
				con.commit();
				return attempts;
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public boolean clearExpiredTemporaryLock(int id) {
		return execute("UPDATE accounts SET failed_attempts=0,locked_until=NULL,updated_at=SYSDATETIME() "
				+ "WHERE account_id=? AND locked_until<=SYSDATETIME()", id);
	}

	public boolean markLoginSuccess(int id) {
		return execute("UPDATE accounts SET failed_attempts=0,locked_until=NULL,"
				+ "last_login_at=SYSDATETIME(),updated_at=SYSDATETIME() WHERE account_id=?", id);
	}

	public boolean updatePassword(int id, String hash) {
		return execute("UPDATE accounts SET password_hash=?,first_login=0,updated_at=SYSDATETIME() WHERE account_id=?",
				hash, id);
	}

	public Set<String> findPermissionCodesByRoleId(int roleId) {
		Set<String> permissions = new HashSet<>();
		String sql = "SELECT p.permission_code FROM role_permissions rp "
				+ "JOIN permissions p ON p.permission_id=rp.permission_id WHERE rp.role_id=? AND rp.granted=1";
		try (Connection con = ConnectDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
			stmt.setInt(1, roleId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				permissions.add(rs.getString("permission_code"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return permissions;
	}

	// ===== Nhật ký =====

	/** Trả về id của dòng log vừa thêm, -1 nếu lỗi. */
	public long insertLoginLog(Integer accountId, String username, boolean success, String reason, String device) {
		String sql = "INSERT INTO login_logs(account_id,username_entered,success,failure_reason,device_info) VALUES(?,?,?,?,?)";
		try (Connection con = ConnectDB.getConnection();
				PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setObject(1, accountId);
			stmt.setString(2, username);
			stmt.setBoolean(3, success);
			stmt.setString(4, reason);
			stmt.setString(5, device);
			stmt.executeUpdate();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
				return rs.getLong(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public boolean markLogout(long logId) {
		return execute("UPDATE login_logs SET logout_at=SYSDATETIME() WHERE login_log_id=? AND logout_at IS NULL", logId);
	}

	public boolean insertAuditLog(Integer accountId, String action, String entity, String entityId, String detail) {
		return execute("INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
				accountId, action, entity, entityId, detail);
	}

	// ===== Danh sách / tra cứu (trả về Map để màn hình hiện tại dùng được) =====

	public List<Map<String, Object>> searchAccounts(String keyword, String roleCode, String status) {
		String sql = "SELECT a.account_id,e.employee_id,e.full_name,a.username,r.role_code,e.phone,a.status,a.last_login_at "
				+ "FROM accounts a JOIN employees e ON e.employee_id=a.employee_id JOIN roles r ON r.role_id=a.role_id "
				+ "WHERE r.role_code<>'ADMIN' "
				+ "AND (e.full_name LIKE ? OR a.username LIKE ? OR e.phone LIKE ? OR e.employee_id LIKE ?)";
		List<Object> params = new ArrayList<>();
		params.add(keyword);
		params.add(keyword);
		params.add(keyword);
		params.add(keyword);
		if (roleCode != null) {
			sql += " AND r.role_code=?";
			params.add(roleCode);
		}
		if (status != null) {
			sql += " AND a.status=?";
			params.add(status);
		}
		sql += " ORDER BY e.full_name";
		return queryRows(sql, params.toArray());
	}

	public List<Map<String, Object>> availableEmployees() {
		return queryRows("SELECT e.employee_id,e.full_name,e.phone FROM employees e WHERE e.status='ACTIVE' "
				+ "AND NOT EXISTS(SELECT 1 FROM accounts a WHERE a.employee_id=e.employee_id) ORDER BY e.full_name");
	}

	public Map<String, Object> accountInfo(int id) {
		return queryOne("SELECT a.account_id,a.employee_id,a.username,a.role_id,e.full_name "
				+ "FROM accounts a JOIN employees e ON e.employee_id=a.employee_id WHERE a.account_id=?", id);
	}

	public String accountStatus(int id) {
		Object value = queryScalar("SELECT status FROM accounts WHERE account_id=?", id);
		return value == null ? null : String.valueOf(value);
	}

	public boolean hasActiveSession(int id) {
		Object count = queryScalar("SELECT COUNT(*) FROM login_logs WHERE account_id=? AND logout_at IS NULL AND success=1", id);
		return count != null && ((Number) count).intValue() > 0;
	}

	public List<Map<String, Object>> manageableRoles() {
		return queryRows("SELECT role_id,role_name,role_code FROM roles "
				+ "WHERE role_code IN ('MANAGER','SALES','CUSTOMER_SERVICE') ORDER BY role_id");
	}

	public Map<String, Object> sessionState(int id) {
		return queryOne("SELECT a.status account_status,e.status employee_status,a.role_id "
				+ "FROM accounts a JOIN employees e ON e.employee_id=a.employee_id WHERE a.account_id=?", id);
	}

	// ===== Kiểm tra điều kiện cho nghiệp vụ quản lý tài khoản =====

	public boolean isActiveEmployee(String employeeId) {
		Object status = queryScalar("SELECT status FROM employees WHERE employee_id=?", employeeId);
		return status != null && "ACTIVE".equals(String.valueOf(status));
	}

	public String findRoleCode(int roleId) {
		Object code = queryScalar("SELECT role_code FROM roles WHERE role_id=?", roleId);
		return code == null ? null : String.valueOf(code);
	}

	public boolean existsByEmployeeOrUsername(String employeeId, String username) {
		Object n = queryScalar("SELECT COUNT(*) FROM accounts WHERE employee_id=? OR username=?", employeeId, username);
		return n != null && ((Number) n).intValue() > 0;
	}

	public boolean existsUsernameExcept(String username, int accountId) {
		Object n = queryScalar("SELECT COUNT(*) FROM accounts WHERE username=? AND account_id<>?", username, accountId);
		return n != null && ((Number) n).intValue() > 0;
	}

	/** Trả về: role_id, role_code, status (tài khoản), employee_status. */
	public Map<String, Object> accountBrief(int id) {
		return queryOne("SELECT a.role_id,r.role_code,a.status,e.status employee_status FROM accounts a "
				+ "JOIN roles r ON r.role_id=a.role_id JOIN employees e ON e.employee_id=a.employee_id "
				+ "WHERE a.account_id=?", id);
	}

	public int countActiveManagers() {
		Object n = queryScalar("SELECT COUNT(*) FROM accounts a JOIN employees e ON e.employee_id=a.employee_id "
				+ "JOIN roles r ON r.role_id=a.role_id "
				+ "WHERE r.role_code='MANAGER' AND a.status='ACTIVE' AND e.status='ACTIVE'");
		return n == null ? 0 : ((Number) n).intValue();
	}

	// ===== Thêm / sửa / xóa (ghi tài khoản + audit log trong cùng một transaction) =====

	public boolean createAccount(int actorId, String employeeId, String username, String hash, int roleId) {
		try (Connection con = ConnectDB.getConnection()) {
			con.setAutoCommit(false);
			try {
				long id;
				try (PreparedStatement stmt = con.prepareStatement(
						"INSERT INTO accounts(employee_id,username,password_hash,role_id,status,first_login) VALUES(?,?,?,?,'ACTIVE',1)",
						Statement.RETURN_GENERATED_KEYS)) {
					stmt.setString(1, employeeId);
					stmt.setString(2, username);
					stmt.setString(3, hash);
					stmt.setInt(4, roleId);
					stmt.executeUpdate();
					ResultSet rs = stmt.getGeneratedKeys();
					if (!rs.next()) {
						throw new SQLException("Không lấy được khóa tự tăng.");
					}
					id = rs.getLong(1);
				}
				audit(con, actorId, "ACCOUNT_CREATED", id, "Cấp tài khoản cho " + employeeId);
				con.commit();
				return true;
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean updateAccount(int actorId, int accountId, String username, int roleId) {
		try (Connection con = ConnectDB.getConnection()) {
			con.setAutoCommit(false);
			try {
				run(con, "UPDATE accounts SET username=?,role_id=?,updated_at=SYSDATETIME() WHERE account_id=?",
						username, roleId, accountId);
				audit(con, actorId, "ACCOUNT_UPDATED", accountId, "Cập nhật tên đăng nhập/vai trò");
				con.commit();
				return true;
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean changeStatus(int actorId, int accountId, String newStatus) {
		try (Connection con = ConnectDB.getConnection()) {
			con.setAutoCommit(false);
			try {
				run(con, "UPDATE accounts SET status=?,"
						+ "failed_attempts=CASE WHEN ?='ACTIVE' THEN 0 ELSE failed_attempts END,"
						+ "locked_until=CASE WHEN ?='ACTIVE' THEN NULL ELSE locked_until END,"
						+ "updated_at=SYSDATETIME() WHERE account_id=?", newStatus, newStatus, newStatus, accountId);
				if (!"ACTIVE".equals(newStatus)) {
					closeSessions(con, accountId);
				}
				audit(con, actorId, "ACCOUNT_STATUS_CHANGED", accountId, "Trạng thái -> " + newStatus);
				con.commit();
				return true;
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/** Đã có lịch sử thì chỉ vô hiệu hóa, chưa có thì xóa hẳn. */
	public boolean hasHistory(int accountId) {
		Object n = queryScalar("SELECT (SELECT COUNT(*) FROM login_logs WHERE account_id=?)"
				+ "+(SELECT COUNT(*) FROM audit_logs WHERE account_id=?)", accountId, accountId);
		return n != null && ((Number) n).intValue() > 0;
	}

	public boolean disablePreserveHistory(int actorId, int accountId) {
		try (Connection con = ConnectDB.getConnection()) {
			con.setAutoCommit(false);
			try {
				run(con, "UPDATE accounts SET status='DISABLED',updated_at=SYSDATETIME() WHERE account_id=?", accountId);
				closeSessions(con, accountId);
				audit(con, actorId, "ACCOUNT_DISABLED_PRESERVE_HISTORY", accountId,
						"Không xóa vật lý vì tài khoản đã có lịch sử");
				con.commit();
				return true;
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean deleteAccount(int actorId, int accountId) {
		try (Connection con = ConnectDB.getConnection()) {
			con.setAutoCommit(false);
			try {
				run(con, "DELETE FROM accounts WHERE account_id=?", accountId);
				audit(con, actorId, "ACCOUNT_DELETED", accountId, "Xóa tài khoản chưa phát sinh lịch sử");
				con.commit();
				return true;
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// ===== Hàm phụ =====

	private void closeSessions(Connection con, int accountId) throws SQLException {
		run(con, "UPDATE login_logs SET logout_at=COALESCE(logout_at,SYSDATETIME()) "
				+ "WHERE account_id=? AND success=1 AND logout_at IS NULL", accountId);
	}

	private void audit(Connection con, int actorId, String action, long entityId, String detail) throws SQLException {
		run(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
				actorId, action, "accounts", String.valueOf(entityId), detail);
	}

	/** Chạy 1 câu lệnh trên connection có sẵn (dùng trong transaction). */
	private void run(Connection con, String sql, Object... params) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(sql)) {
			bind(stmt, params);
			stmt.executeUpdate();
		}
	}

	/** Chạy 1 câu lệnh độc lập, true nếu không lỗi. */
	private boolean execute(String sql, Object... params) {
		try (Connection con = ConnectDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
			bind(stmt, params);
			stmt.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	private List<Map<String, Object>> queryRows(String sql, Object... params) {
		List<Map<String, Object>> list = new ArrayList<>();
		try (Connection con = ConnectDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
			bind(stmt, params);
			ResultSet rs = stmt.executeQuery();
			ResultSetMetaData md = rs.getMetaData();
			while (rs.next()) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (int i = 1; i <= md.getColumnCount(); i++) {
					row.put(md.getColumnLabel(i), rs.getObject(i));
				}
				list.add(row);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	private Map<String, Object> queryOne(String sql, Object... params) {
		List<Map<String, Object>> rows = queryRows(sql, params);
		return rows.isEmpty() ? null : rows.get(0);
	}

	private Object queryScalar(String sql, Object... params) {
		try (Connection con = ConnectDB.getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) {
			bind(stmt, params);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getObject(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void bind(PreparedStatement stmt, Object... params) throws SQLException {
		for (int i = 0; i < params.length; i++) {
			stmt.setObject(i + 1, params[i]);
		}
	}

	private Account mapAccount(ResultSet rs) throws SQLException {
		Employee employee = new Employee();
		employee.setEmployeeId(rs.getString("employee_id"));
		employee.setFullName(rs.getString("full_name"));
		employee.setPhone(rs.getString("phone"));
		employee.setEmail(rs.getString("email"));
		employee.setDepartment(rs.getString("department"));
		employee.setStatus(rs.getString("employee_status"));
		java.sql.Date joinDate = rs.getDate("join_date");
		if (joinDate != null) {
			employee.setJoinDate(joinDate.toLocalDate());
		}

		Role role = new Role(rs.getInt("role_id"), rs.getString("role_code"), rs.getString("role_name"),
				rs.getString("description"));

		Account account = new Account();
		account.setAccountId(rs.getInt("account_id"));
		account.setUsername(rs.getString("username"));
		account.setPasswordHash(rs.getString("password_hash"));
		account.setStatus(rs.getString("account_status"));
		account.setFirstLogin(rs.getBoolean("first_login"));
		account.setFailedAttempts(rs.getInt("failed_attempts"));
		Timestamp lockedUntil = rs.getTimestamp("locked_until");
		if (lockedUntil != null) {
			account.setLockedUntil(lockedUntil.toLocalDateTime());
		}
		Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
		if (lastLoginAt != null) {
			account.setLastLoginAt(lastLoginAt.toLocalDateTime());
		}
		account.setEmployee(employee);
		account.setRole(role);
		return account;
	}
}
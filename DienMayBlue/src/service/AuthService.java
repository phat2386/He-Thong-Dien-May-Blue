package service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import connectDB.ConnectDB;
import dao.AccountDAO;
import dao.EmployeeDAO;
import entity.Account;
import util.PasswordUtil;
import util.SessionManager;

public class AuthService {

    private final AccountDAO accountDAO = new AccountDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    /** Kết quả đăng nhập trả về cho màn hình đăng nhập. */
    public static final class LoginResult {
        private final boolean success;
        private final String message;
        private final Account account;
        private final boolean requiresPasswordChange;

        private LoginResult(boolean success, String message,
                            Account account, boolean requiresPasswordChange) {
            this.success = success;
            this.message = message;
            this.account = account;
            this.requiresPasswordChange = requiresPasswordChange;
        }

        public static LoginResult success(Account account, boolean requiresPasswordChange) {
            return new LoginResult(true, "Đăng nhập thành công.",
                    account, requiresPasswordChange);
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, message, null, false);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Account getAccount() {
            return account;
        }

        public boolean isRequiresPasswordChange() {
            return requiresPasswordChange;
        }
    }

    /** Kết quả chung cho các thao tác như đổi mật khẩu. */
    public static final class Result {
        private final boolean success;
        private final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static Result success(String message) {
            return new Result(true, message);
        }

        public static Result failure(String message) {
            return new Result(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public LoginResult login(String username, String rawPassword) {
        username = username == null ? "" : username.trim();
        rawPassword = rawPassword == null ? "" : rawPassword;

        if (username.isEmpty() || rawPassword.isEmpty()) {
            return LoginResult.failure(
                    "Tên đăng nhập và mật khẩu không được để trống.");
        }

        try {
            Account account = accountDAO.findByUsername(username);

            if (account == null) {
                accountDAO.insertLoginLog(null, username, false,
                        "ACCOUNT_NOT_FOUND", getDeviceInfo());
                return LoginResult.failure(
                        "Sai tài khoản hoặc mật khẩu.");
            }

            String accountStatus = account.getStatus() == null
                    ? "" : account.getStatus().toUpperCase();

            if (!"ACTIVE".equals(accountStatus)) {
                accountDAO.insertLoginLog(account.getAccountId(), username, false,
                        "ACCOUNT_STATUS_" + accountStatus, getDeviceInfo());

                if ("LOCKED".equals(accountStatus)) {
                    return LoginResult.failure(
                            "Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên.");
                }
                if ("DISABLED".equals(accountStatus)) {
                    return LoginResult.failure(
                            "Tài khoản đã ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
                }
                if ("PENDING".equals(accountStatus)) {
                    return LoginResult.failure(
                            "Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
                }
                return LoginResult.failure(
                        "Tài khoản hiện không thể đăng nhập. Vui lòng liên hệ quản trị viên.");
            }

            if (account.getEmployee() == null
                    || !"ACTIVE".equalsIgnoreCase(account.getEmployee().getStatus())) {
                accountDAO.insertLoginLog(account.getAccountId(), username, false,
                        "EMPLOYEE_STATUS_" + (account.getEmployee() == null
                                ? "MISSING" : account.getEmployee().getStatus()),
                        getDeviceInfo());
                return LoginResult.failure(
                        "Hồ sơ nhân viên hiện không ở trạng thái làm việc. Vui lòng liên hệ quản lý.");
            }

            if (account.getLockedUntil() != null) {
                LocalDateTime now = LocalDateTime.now();

                if (account.getLockedUntil().isAfter(now)) {
                    accountDAO.insertLoginLog(account.getAccountId(), username, false,
                            "TEMPORARY_LOCK", getDeviceInfo());
                    return LoginResult.failure(
                            "Tài khoản đang bị khóa tạm thời đến "
                          + account.getLockedUntil().toLocalTime().withSecond(0).withNano(0)
                          + ". Vui lòng thử lại sau.");
                }

                accountDAO.clearExpiredTemporaryLock(account.getAccountId());
                account.setFailedAttempts(0);
                account.setLockedUntil(null);
            }

            if (!PasswordUtil.matches(rawPassword, account.getPasswordHash())) {
                int attempts = accountDAO.registerFailedAttempt(account.getAccountId());

                accountDAO.insertLoginLog(account.getAccountId(), username, false,
                        "WRONG_PASSWORD_ATTEMPT_" + attempts,
                        getDeviceInfo());

                if (attempts > 5) {
                    return LoginResult.failure(
                            "Bạn đã nhập sai quá 5 lần liên tiếp. "
                          + "Tài khoản bị khóa tạm thời 15 phút.");
                }

                if (attempts == 5) {
                    return LoginResult.failure(
                            "Sai tài khoản hoặc mật khẩu. Đây là lần sai thứ 5; "
                          + "nếu nhập sai thêm một lần, tài khoản sẽ bị khóa tạm thời 15 phút.");
                }

                return LoginResult.failure(
                        "Sai tài khoản hoặc mật khẩu. Lần nhập sai liên tiếp: "
                      + attempts + "/5.");
            }

            // Đọc quyền trước khi ghi nhận đăng nhập thành công để tránh tạo
            // một phiên thành công nhưng không nạp được quyền sử dụng.
            Set<String> permissions =
                    accountDAO.findPermissionCodesByRoleId(
                            account.getRole().getRoleId());

            accountDAO.markLoginSuccess(account.getAccountId());

            long loginLogId = accountDAO.insertLoginLog(
                    account.getAccountId(), username, true,
                    null, getDeviceInfo());

            SessionManager.login(account, loginLogId, permissions);

            return LoginResult.success(
                    account, account.isFirstLogin());

        } catch (SQLException e) {
            e.printStackTrace();
            return LoginResult.failure(
                    "Không thể kết nối hoặc truy vấn cơ sở dữ liệu.");
        }
    }

    public Result changePassword(
            String currentPassword,
            String newPassword,
            String confirmPassword) {

        if (!SessionManager.isLoggedIn()) {
            return Result.failure(
                    "Phiên đăng nhập không còn hiệu lực.");
        }

        currentPassword = currentPassword == null ? "" : currentPassword;
        newPassword = newPassword == null ? "" : newPassword;
        confirmPassword = confirmPassword == null ? "" : confirmPassword;

        if (currentPassword.isEmpty()
                || newPassword.isEmpty()
                || confirmPassword.isEmpty()) {
            return Result.failure(
                    "Vui lòng nhập đầy đủ mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu.");
        }

        try {
            Account sessionAccount = SessionManager.getCurrentAccount();
            Account account = accountDAO.findById(sessionAccount.getAccountId());

            if (account == null
                    || !"ACTIVE".equalsIgnoreCase(account.getStatus())
                    || account.getEmployee() == null
                    || !"ACTIVE".equalsIgnoreCase(account.getEmployee().getStatus())) {
                return Result.failure(
                        "Tài khoản hoặc hồ sơ nhân viên không còn hoạt động. Vui lòng đăng nhập lại.");
            }

            // So sánh với dữ liệu mới nhất trong DB, không chỉ dựa vào bản sao trong session.
            if (!PasswordUtil.matches(currentPassword, account.getPasswordHash())) {
                return Result.failure(
                        "Mật khẩu hiện tại không đúng.");
            }

            if (!PasswordUtil.isStrongEnough(newPassword)) {
                return Result.failure(
                        "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ và số.");
            }

            if (!newPassword.equals(confirmPassword)) {
                return Result.failure(
                        "Mật khẩu xác nhận không khớp.");
            }

            if (PasswordUtil.matches(newPassword, account.getPasswordHash())) {
                return Result.failure(
                        "Mật khẩu mới không được trùng mật khẩu hiện tại.");
            }

            String newHash = PasswordUtil.sha256(newPassword);
            accountDAO.updatePassword(account.getAccountId(), newHash);

            accountDAO.insertAuditLog(
                    account.getAccountId(),
                    "PASSWORD_CHANGED",
                    "accounts",
                    String.valueOf(account.getAccountId()),
                    sessionAccount.isFirstLogin()
                            ? "Đổi mật khẩu lần đầu"
                            : "Người dùng tự đổi mật khẩu");

            // Đồng bộ lại bản sao đang giữ trong session để các lần đổi tiếp theo
            // không phải đăng xuất/đăng nhập chỉ vì mật khẩu vừa thay đổi.
            sessionAccount.setPasswordHash(newHash);
            sessionAccount.setFirstLogin(false);

            return Result.success(
                    "Đổi mật khẩu thành công.");

        } catch (SQLException e) {
            e.printStackTrace();
            return Result.failure(
                    "Không thể cập nhật mật khẩu.");
        }
    }

    /**
     * Lấy hồ sơ mới nhất của tài khoản đang đăng nhập để UC004 luôn hiển thị
     * dữ liệu hiện tại trong CSDL thay vì chỉ dùng dữ liệu được nạp lúc login.
     */
    public Account getCurrentAccountSnapshot() throws SQLException {
        if (!SessionManager.isLoggedIn()) return null;
        return accountDAO.findById(
                SessionManager.getCurrentAccount().getAccountId());
    }

    public void logout() {
        Long logId = SessionManager.getLoginLogId();

        if (logId != null) {
            try {
                accountDAO.markLogout(logId.longValue());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        SessionManager.clear();
    }

    /** Kiểm tra đã đăng nhập và có quyền; trả về tài khoản đang thao tác. */
    private Account requirePermission(String permissionCode) {
        Account account = SessionManager.getCurrentAccount();

        if (account == null || account.getEmployee() == null || account.getRole() == null) {
            throw new IllegalStateException("Phiên đăng nhập không hợp lệ.");
        }
        if (!SessionManager.hasPermission(permissionCode)) {
            throw new IllegalStateException("Tài khoản chưa được cấp quyền thực hiện chức năng này.");
        }
        return account;
    }

    private String getDeviceInfo() {
        String host = "unknown-host";
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            // Tên máy chỉ phục vụ nhật ký; không làm thất bại đăng nhập.
        }

        return host
                + " | " + System.getProperty("os.name")
                + " | Java " + System.getProperty("java.version")
                + " | " + System.getProperty("user.name");
    }

    // ===== Truy vấn cho màn hình quản lý tài khoản và kiểm tra phiên =====

    public List<Map<String, Object>> searchAccounts(
            String keyword, String role, String status) throws SQLException {
        return accountDAO.searchAccounts(keyword, role, status);
    }

    public List<Map<String, Object>> availableEmployees() throws SQLException {
        return accountDAO.findAvailableEmployees();
    }

    public List<Map<String, Object>> manageableRoles() throws SQLException {
        return accountDAO.findManageableRoles();
    }

    public Map<String, Object> accountInfo(int accountId) throws SQLException {
        return accountDAO.findAccountInfo(accountId);
    }

    public String accountStatus(int accountId) throws SQLException {
        return accountDAO.findStatus(accountId);
    }

    public boolean hasActiveSession(int accountId) throws SQLException {
        return accountDAO.hasActiveSession(accountId);
    }

    public Map<String, Object> sessionState(int accountId) throws SQLException {
        return accountDAO.findSessionState(accountId);
    }

    public Set<String> permissionsForRole(int roleId) throws SQLException {
        return accountDAO.findPermissionCodesByRoleId(roleId);
    }

    // ===== UC005 - Quản lý tài khoản =====

    public void createAccountForEmployee(String employeeId, String username,
                                         String rawPassword, int roleId)
            throws Exception {

        Account actor = requirePermission("ACCOUNT_MANAGE");

        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn nhân viên.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        if (!PasswordUtil.isStrongEnough(rawPassword)) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số.");
        }
        username = username.trim();

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                if (!"ACTIVE".equals(employeeDAO.findStatus(employeeId))) {
                    throw new IllegalArgumentException(
                            "Chỉ có thể cấp tài khoản cho nhân viên đang hoạt động.");
                }

                String roleCode = accountDAO.findRoleCode(roleId);
                if (roleCode == null) {
                    throw new IllegalArgumentException("Vai trò không hợp lệ.");
                }
                if (!Arrays.asList("MANAGER", "SALES", "CUSTOMER_SERVICE")
                        .contains(roleCode)) {
                    throw new IllegalArgumentException(
                            "Tài khoản Admin hệ thống không được tạo tại UC005.");
                }
                if (accountDAO.existsByEmployeeOrUsername(employeeId, username)) {
                    throw new IllegalArgumentException(
                            "Nhân viên đã có tài khoản hoặc tên đăng nhập đã tồn tại.");
                }

                long accountId = accountDAO.insert(con, employeeId, username,
                        PasswordUtil.sha256(rawPassword), roleId);

                accountDAO.insertAuditLog(con, actor.getAccountId(), "ACCOUNT_CREATED",
                        "accounts", String.valueOf(accountId),
                        "Cấp tài khoản cho " + employeeId);

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void updateAccount(int accountId, String username, int roleId)
            throws Exception {

        Account actor = requirePermission("ACCOUNT_MANAGE");

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        username = username.trim();

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                Map<String, Object> target = accountDAO.findBrief(accountId);
                if (target == null) {
                    throw new IllegalArgumentException("Không tìm thấy tài khoản.");
                }

                String targetRole = String.valueOf(target.get("role_code"));
                if ("ADMIN".equals(targetRole)) {
                    throw new IllegalStateException(
                            "Tài khoản Admin hệ thống không được chỉnh sửa tại UC005.");
                }

                String newRole = accountDAO.findRoleCode(roleId);
                if (newRole == null) {
                    throw new IllegalArgumentException("Vai trò không hợp lệ.");
                }
                if ("ADMIN".equals(newRole)) {
                    throw new IllegalArgumentException(
                            "Không thể gán vai trò Admin tại UC005.");
                }
                if (accountDAO.existsUsernameExcept(username, accountId)) {
                    throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
                }
                if (accountId == actor.getAccountId()
                        && ((Number) target.get("role_id")).intValue() != roleId) {
                    throw new IllegalArgumentException(
                            "Không thể tự thay đổi vai trò của tài khoản đang đăng nhập.");
                }
                if ("MANAGER".equals(targetRole) && !"MANAGER".equals(newRole)
                        && "ACTIVE".equals(String.valueOf(target.get("status")))
                        && accountDAO.countActiveManagers(con) <= 1) {
                    throw new IllegalStateException(
                            "Không thể đổi vai trò của quản lý đang hoạt động cuối cùng.");
                }

                accountDAO.updateInfo(con, accountId, username, roleId);

                accountDAO.insertAuditLog(con, actor.getAccountId(), "ACCOUNT_UPDATED",
                        "accounts", String.valueOf(accountId),
                        "Cập nhật tên đăng nhập/vai trò");

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void changeAccountStatus(int accountId, String newStatus)
            throws Exception {

        Account actor = requirePermission("ACCOUNT_MANAGE");

        if (!Arrays.asList("ACTIVE", "LOCKED", "DISABLED").contains(newStatus)) {
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        }
        if (accountId == actor.getAccountId() && !"ACTIVE".equals(newStatus)) {
            throw new IllegalArgumentException(
                    "Không thể tự khóa/vô hiệu hóa tài khoản đang sử dụng.");
        }

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                Map<String, Object> target = accountDAO.findBrief(accountId);
                if (target == null) {
                    throw new IllegalArgumentException("Không tìm thấy tài khoản.");
                }

                String targetRole = String.valueOf(target.get("role_code"));
                if ("ADMIN".equals(targetRole)) {
                    throw new IllegalStateException(
                            "Tài khoản Admin hệ thống không được khóa/vô hiệu hóa tại UC005.");
                }
                if ("ACTIVE".equals(newStatus)
                        && !"ACTIVE".equals(String.valueOf(target.get("employee_status")))) {
                    throw new IllegalArgumentException(
                            "Không thể kích hoạt tài khoản khi hồ sơ nhân viên chưa hoạt động.");
                }
                if (!"ACTIVE".equals(newStatus) && "MANAGER".equals(targetRole)
                        && "ACTIVE".equals(String.valueOf(target.get("status")))
                        && accountDAO.countActiveManagers(con) <= 1) {
                    throw new IllegalStateException(
                            "Không thể khóa/vô hiệu hóa quản lý đang hoạt động cuối cùng.");
                }

                accountDAO.updateStatus(con, accountId, newStatus);

                // Tài khoản không còn ACTIVE thì kết thúc các phiên đang mở.
                if (!"ACTIVE".equals(newStatus)) {
                    accountDAO.closeLoginSessions(con, accountId);
                }

                accountDAO.insertAuditLog(con, actor.getAccountId(),
                        "ACCOUNT_STATUS_CHANGED", "accounts",
                        String.valueOf(accountId), "Trạng thái -> " + newStatus);

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Xóa tài khoản chưa có lịch sử; tài khoản đã có lịch sử thì chỉ vô hiệu hóa.
     *
     * @return true nếu đã xóa hẳn, false nếu chỉ vô hiệu hóa
     */
    public boolean deleteAccountOrDisable(int accountId) throws Exception {
        Account actor = requirePermission("ACCOUNT_MANAGE");

        if (accountId == actor.getAccountId()) {
            throw new IllegalArgumentException(
                    "Không thể tự xóa tài khoản đang sử dụng.");
        }

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                Map<String, Object> target = accountDAO.findBrief(accountId);
                if (target == null) {
                    throw new IllegalArgumentException("Không tìm thấy tài khoản.");
                }

                String targetRole = String.valueOf(target.get("role_code"));
                if ("ADMIN".equals(targetRole)) {
                    throw new IllegalStateException(
                            "Tài khoản Admin hệ thống không được xóa/vô hiệu hóa tại UC005.");
                }
                if ("MANAGER".equals(targetRole)
                        && "ACTIVE".equals(String.valueOf(target.get("status")))
                        && accountDAO.countActiveManagers(con) <= 1) {
                    throw new IllegalStateException(
                            "Không thể xóa quản lý đang hoạt động cuối cùng.");
                }

                boolean deleted;
                if (accountDAO.hasHistory(accountId)) {
                    accountDAO.disable(con, accountId);
                    accountDAO.closeLoginSessions(con, accountId);
                    accountDAO.insertAuditLog(con, actor.getAccountId(),
                            "ACCOUNT_DISABLED_PRESERVE_HISTORY", "accounts",
                            String.valueOf(accountId),
                            "Không xóa vật lý vì tài khoản đã có lịch sử");
                    deleted = false;
                } else {
                    accountDAO.delete(con, accountId);
                    accountDAO.insertAuditLog(con, actor.getAccountId(),
                            "ACCOUNT_DELETED", "accounts",
                            String.valueOf(accountId),
                            "Xóa tài khoản chưa phát sinh lịch sử");
                    deleted = true;
                }

                con.commit();
                return deleted;
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }
}

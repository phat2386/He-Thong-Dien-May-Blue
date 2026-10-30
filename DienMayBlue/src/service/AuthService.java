package service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dao.AccountDAO;
import entity.Account;
import util.PasswordUtil;
import util.SessionManager;

/**
 * Nghiệp vụ đăng nhập và quản lý tài khoản.
 * Service chỉ kiểm tra điều kiện, mọi câu SQL nằm trong AccountDAO.
 */
public class AuthService {

    private final AccountDAO accountDAO = new AccountDAO();

    // ===== Kết quả trả về cho UI =====

    public static final class LoginResult {
        private final boolean success;
        private final String message;
        private final Account account;
        private final boolean requiresPasswordChange;

        private LoginResult(boolean success, String message, Account account, boolean change) {
            this.success = success;
            this.message = message;
            this.account = account;
            this.requiresPasswordChange = change;
        }

        public static LoginResult success(Account a, boolean change) {
            return new LoginResult(true, "Đăng nhập thành công.", a, change);
        }

        public static LoginResult failure(String m) {
            return new LoginResult(false, m, null, false);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Account getAccount() { return account; }
        public boolean isRequiresPasswordChange() { return requiresPasswordChange; }
    }

    public static final class Result {
        private final boolean success;
        private final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static Result success(String m) { return new Result(true, m); }
        public static Result failure(String m) { return new Result(false, m); }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    // ===== Truy vấn (chuyển thẳng xuống DAO) =====

    public List<Map<String, Object>> searchAccounts(String keyword, String role, String status) throws SQLException {
        return accountDAO.searchAccounts(keyword, role, status);
    }

    public List<Map<String, Object>> availableEmployees() throws SQLException {
        return accountDAO.availableEmployees();
    }

    public Map<String, Object> accountInfo(int id) throws SQLException {
        return accountDAO.accountInfo(id);
    }

    public String accountStatus(int id) throws SQLException {
        return accountDAO.accountStatus(id);
    }

    public boolean hasActiveSession(int id) throws SQLException {
        return accountDAO.hasActiveSession(id);
    }

    public List<Map<String, Object>> manageableRoles() throws SQLException {
        return accountDAO.manageableRoles();
    }

    public Map<String, Object> sessionState(int id) throws SQLException {
        return accountDAO.sessionState(id);
    }

    public Set<String> permissionsForRole(int roleId) throws SQLException {
        return accountDAO.findPermissionCodesByRoleId(roleId);
    }

    // ===== Đăng nhập / đăng xuất =====

    public LoginResult login(String username, String rawPassword) {
        username = username == null ? "" : username.trim();
        rawPassword = rawPassword == null ? "" : rawPassword;
        if (username.isEmpty() || rawPassword.isEmpty()) {
            return LoginResult.failure("Tên đăng nhập và mật khẩu không được để trống.");
        }
        try {
            Account account = accountDAO.findByUsername(username);
            if (account == null) {
                logFail(null, username, "ACCOUNT_NOT_FOUND");
                return LoginResult.failure("Sai tài khoản hoặc mật khẩu.");
            }
            int id = account.getAccountId();

            // 1. Trạng thái tài khoản
            String status = account.getStatus() == null ? "" : account.getStatus().toUpperCase();
            if (!"ACTIVE".equals(status)) {
                logFail(id, username, "ACCOUNT_STATUS_" + status);
                return LoginResult.failure(statusMessage(status));
            }

            // 2. Trạng thái nhân viên
            if (account.getEmployee() == null || !"ACTIVE".equalsIgnoreCase(account.getEmployee().getStatus())) {
                String empStatus = account.getEmployee() == null ? "MISSING" : account.getEmployee().getStatus();
                logFail(id, username, "EMPLOYEE_STATUS_" + empStatus);
                return LoginResult.failure("Hồ sơ nhân viên hiện không ở trạng thái làm việc. Vui lòng liên hệ quản lý.");
            }

            // 3. Khóa tạm thời
            if (account.getLockedUntil() != null) {
                if (account.getLockedUntil().isAfter(LocalDateTime.now())) {
                    logFail(id, username, "TEMPORARY_LOCK");
                    return LoginResult.failure("Tài khoản đang bị khóa tạm thời đến "
                            + account.getLockedUntil().toLocalTime().withSecond(0).withNano(0)
                            + ". Vui lòng thử lại sau.");
                }
                accountDAO.clearExpiredTemporaryLock(id);
                account.setFailedAttempts(0);
                account.setLockedUntil(null);
            }

            // 4. Sai mật khẩu
            if (!PasswordUtil.matches(rawPassword, account.getPasswordHash())) {
                int attempts = accountDAO.registerFailedAttempt(id);
                logFail(id, username, "WRONG_PASSWORD_ATTEMPT_" + attempts);
                if (attempts > 5) {
                    return LoginResult.failure("Bạn đã nhập sai quá 5 lần liên tiếp. Tài khoản bị khóa tạm thời 15 phút.");
                }
                if (attempts == 5) {
                    return LoginResult.failure("Sai tài khoản hoặc mật khẩu. Đây là lần sai thứ 5; "
                            + "nếu nhập sai thêm một lần, tài khoản sẽ bị khóa tạm thời 15 phút.");
                }
                return LoginResult.failure("Sai tài khoản hoặc mật khẩu. Lần nhập sai liên tiếp: " + attempts + "/5.");
            }

            // 5. Thành công
            Set<String> permissions = accountDAO.findPermissionCodesByRoleId(account.getRole().getRoleId());
            accountDAO.markLoginSuccess(id);
            long loginLogId = accountDAO.insertLoginLog(id, username, true, null, getDeviceInfo());
            SessionManager.login(account, loginLogId, permissions);
            return LoginResult.success(account, account.isFirstLogin());
        } catch (Exception e) {
            e.printStackTrace();
            return LoginResult.failure("Không thể kết nối hoặc truy vấn cơ sở dữ liệu.");
        }
    }

    public void logout() {
        Long logId = SessionManager.getLoginLogId();
        if (logId != null) {
            accountDAO.markLogout(logId.longValue());
        }
        SessionManager.clear();
    }

    public Account getCurrentAccountSnapshot() throws SQLException {
        if (!SessionManager.isLoggedIn()) {
            return null;
        }
        return accountDAO.findById(SessionManager.getCurrentAccount().getAccountId());
    }

    // ===== Đổi mật khẩu =====

    public Result changePassword(String currentPassword, String newPassword, String confirmPassword) {
        if (!SessionManager.isLoggedIn()) {
            return Result.failure("Phiên đăng nhập không còn hiệu lực.");
        }
        currentPassword = currentPassword == null ? "" : currentPassword;
        newPassword = newPassword == null ? "" : newPassword;
        confirmPassword = confirmPassword == null ? "" : confirmPassword;
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return Result.failure("Vui lòng nhập đầy đủ mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu.");
        }
        try {
            Account session = SessionManager.getCurrentAccount();
            Account account = accountDAO.findById(session.getAccountId());
            if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())
                    || account.getEmployee() == null || !"ACTIVE".equalsIgnoreCase(account.getEmployee().getStatus())) {
                return Result.failure("Tài khoản hoặc hồ sơ nhân viên không còn hoạt động. Vui lòng đăng nhập lại.");
            }
            if (!PasswordUtil.matches(currentPassword, account.getPasswordHash())) {
                return Result.failure("Mật khẩu hiện tại không đúng.");
            }
            if (!PasswordUtil.isStrongEnough(newPassword)) {
                return Result.failure("Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ và số.");
            }
            if (!newPassword.equals(confirmPassword)) {
                return Result.failure("Mật khẩu xác nhận không khớp.");
            }
            if (PasswordUtil.matches(newPassword, account.getPasswordHash())) {
                return Result.failure("Mật khẩu mới không được trùng mật khẩu hiện tại.");
            }
            String newHash = PasswordUtil.sha256(newPassword);
            if (!accountDAO.updatePassword(account.getAccountId(), newHash)) {
                return Result.failure("Không thể cập nhật mật khẩu.");
            }
            accountDAO.insertAuditLog(account.getAccountId(), "PASSWORD_CHANGED", "accounts",
                    String.valueOf(account.getAccountId()),
                    session.isFirstLogin() ? "Đổi mật khẩu lần đầu" : "Người dùng tự đổi mật khẩu");
            session.setPasswordHash(newHash);
            session.setFirstLogin(false);
            return Result.success("Đổi mật khẩu thành công.");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("Không thể cập nhật mật khẩu.");
        }
    }

    // ===== Quản lý tài khoản (UC005) =====

    public void createAccountForEmployee(String employeeId, String username, String rawPassword, int roleId) throws Exception {
        Account actor = requirePermission("ACCOUNT_MANAGE");
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn nhân viên.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        if (!PasswordUtil.isStrongEnough(rawPassword)) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số.");
        }
        username = username.trim();

        if (!accountDAO.isActiveEmployee(employeeId)) {
            throw new IllegalArgumentException("Chỉ có thể cấp tài khoản cho nhân viên đang hoạt động.");
        }
        String roleCode = accountDAO.findRoleCode(roleId);
        if (roleCode == null) {
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }
        if (!Arrays.asList("MANAGER", "SALES", "CUSTOMER_SERVICE").contains(roleCode)) {
            throw new IllegalArgumentException("Tài khoản Admin hệ thống không được tạo tại UC005.");
        }
        if (accountDAO.existsByEmployeeOrUsername(employeeId, username)) {
            throw new IllegalArgumentException("Nhân viên đã có tài khoản hoặc tên đăng nhập đã tồn tại.");
        }
        if (!accountDAO.createAccount(actor.getAccountId(), employeeId, username, PasswordUtil.sha256(rawPassword), roleId)) {
            throw new IllegalStateException("Không thể tạo tài khoản. Vui lòng thử lại.");
        }
    }

    public void updateAccount(int accountId, String username, int roleId) throws Exception {
        Account actor = requirePermission("ACCOUNT_MANAGE");
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        username = username.trim();

        Map<String, Object> target = accountDAO.accountBrief(accountId);
        if (target == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản.");
        }
        String targetRole = String.valueOf(target.get("role_code"));
        if ("ADMIN".equals(targetRole)) {
            throw new IllegalStateException("Tài khoản Admin hệ thống không được chỉnh sửa tại UC005.");
        }
        String newRole = accountDAO.findRoleCode(roleId);
        if (newRole == null) {
            throw new IllegalArgumentException("Vai trò không hợp lệ.");
        }
        if ("ADMIN".equals(newRole)) {
            throw new IllegalArgumentException("Không thể gán vai trò Admin tại UC005.");
        }
        if (accountDAO.existsUsernameExcept(username, accountId)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
        }
        if (accountId == actor.getAccountId() && ((Number) target.get("role_id")).intValue() != roleId) {
            throw new IllegalArgumentException("Không thể tự thay đổi vai trò của tài khoản đang đăng nhập.");
        }
        if ("MANAGER".equals(targetRole) && !"MANAGER".equals(newRole)
                && "ACTIVE".equals(String.valueOf(target.get("status")))
                && accountDAO.countActiveManagers() <= 1) {
            throw new IllegalStateException("Không thể đổi vai trò của quản lý đang hoạt động cuối cùng.");
        }
        if (!accountDAO.updateAccount(actor.getAccountId(), accountId, username, roleId)) {
            throw new IllegalStateException("Không thể cập nhật tài khoản. Vui lòng thử lại.");
        }
    }

    public void changeAccountStatus(int accountId, String newStatus) throws Exception {
        Account actor = requirePermission("ACCOUNT_MANAGE");
        if (!Arrays.asList("ACTIVE", "LOCKED", "DISABLED").contains(newStatus)) {
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        }
        if (accountId == actor.getAccountId() && !"ACTIVE".equals(newStatus)) {
            throw new IllegalArgumentException("Không thể tự khóa/vô hiệu hóa tài khoản đang sử dụng.");
        }
        Map<String, Object> target = accountDAO.accountBrief(accountId);
        if (target == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản.");
        }
        String targetRole = String.valueOf(target.get("role_code"));
        if ("ADMIN".equals(targetRole)) {
            throw new IllegalStateException("Tài khoản Admin hệ thống không được khóa/vô hiệu hóa tại UC005.");
        }
        if ("ACTIVE".equals(newStatus) && !"ACTIVE".equals(String.valueOf(target.get("employee_status")))) {
            throw new IllegalArgumentException("Không thể kích hoạt tài khoản khi hồ sơ nhân viên chưa hoạt động.");
        }
        if (!"ACTIVE".equals(newStatus) && "MANAGER".equals(targetRole)
                && "ACTIVE".equals(String.valueOf(target.get("status")))
                && accountDAO.countActiveManagers() <= 1) {
            throw new IllegalStateException("Không thể khóa/vô hiệu hóa quản lý đang hoạt động cuối cùng.");
        }
        if (!accountDAO.changeStatus(actor.getAccountId(), accountId, newStatus)) {
            throw new IllegalStateException("Không thể đổi trạng thái tài khoản. Vui lòng thử lại.");
        }
    }

    /** @return true nếu xóa hẳn, false nếu chỉ vô hiệu hóa để giữ lịch sử. */
    public boolean deleteAccountOrDisable(int accountId) throws Exception {
        Account actor = requirePermission("ACCOUNT_MANAGE");
        if (accountId == actor.getAccountId()) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản đang sử dụng.");
        }
        Map<String, Object> target = accountDAO.accountBrief(accountId);
        if (target == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản.");
        }
        String targetRole = String.valueOf(target.get("role_code"));
        if ("ADMIN".equals(targetRole)) {
            throw new IllegalStateException("Tài khoản Admin hệ thống không được xóa/vô hiệu hóa tại UC005.");
        }
        if ("MANAGER".equals(targetRole) && "ACTIVE".equals(String.valueOf(target.get("status")))
                && accountDAO.countActiveManagers() <= 1) {
            throw new IllegalStateException("Không thể xóa quản lý đang hoạt động cuối cùng.");
        }
        if (accountDAO.hasHistory(accountId)) {
            if (!accountDAO.disablePreserveHistory(actor.getAccountId(), accountId)) {
                throw new IllegalStateException("Không thể vô hiệu hóa tài khoản. Vui lòng thử lại.");
            }
            return false;
        }
        if (!accountDAO.deleteAccount(actor.getAccountId(), accountId)) {
            throw new IllegalStateException("Không thể xóa tài khoản. Vui lòng thử lại.");
        }
        return true;
    }

    // ===== Hàm phụ =====

    private Account requirePermission(String permission) {
        Account a = SessionManager.getCurrentAccount();
        if (a == null || !SessionManager.hasPermission(permission)) {
            throw new IllegalStateException("Tài khoản chưa được cấp quyền thực hiện chức năng này.");
        }
        return a;
    }

    private void logFail(Integer accountId, String username, String reason) {
        accountDAO.insertLoginLog(accountId, username, false, reason, getDeviceInfo());
    }

    private String statusMessage(String status) {
        if ("LOCKED".equals(status)) {
            return "Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên.";
        }
        if ("DISABLED".equals(status)) {
            return "Tài khoản đã ngừng hoạt động. Vui lòng liên hệ quản trị viên.";
        }
        if ("PENDING".equals(status)) {
            return "Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.";
        }
        return "Tài khoản hiện không thể đăng nhập. Vui lòng liên hệ quản trị viên.";
    }

    private String getDeviceInfo() {
        String host = "unknown-host";
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        return host + " | " + System.getProperty("os.name")
                + " | Java " + System.getProperty("java.version") + " | " + System.getProperty("user.name");
    }
}
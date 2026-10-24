package service;
import java.sql.*;
import java.time.*;
import java.util.*;
import dao.AccountDAO;
import entity.Account;
import util.PasswordUtil;
import util.SessionManager;
public class AuthService {
    private final AccountDAO accountDAO = new AccountDAO();
    public static final class LoginResult {
        private final boolean success; private final String message; private final Account account; private final boolean requiresPasswordChange;
        private LoginResult(boolean success,String message,Account account,boolean change){this.success=success;this.message=message;this.account=account;this.requiresPasswordChange=change;}
        public static LoginResult success(Account a,boolean change){return new LoginResult(true,"Đăng nhập thành công.",a,change);}
        public static LoginResult failure(String m){return new LoginResult(false,m,null,false);}
        public boolean isSuccess(){return success;} public String getMessage(){return message;} public Account getAccount(){return account;} public boolean isRequiresPasswordChange(){return requiresPasswordChange;}
    }
    public static final class Result {
        private final boolean success; private final String message;
        private Result(boolean success,String message){this.success=success;this.message=message;}
        public static Result success(String m){return new Result(true,m);} public static Result failure(String m){return new Result(false,m);}
        public boolean isSuccess(){return success;} public String getMessage(){return message;}
    }
    public List<Map<String,Object>> searchAccounts(String keyword,String role,String status) throws SQLException { return accountDAO.searchAccounts(keyword,role,status); }
    public List<Map<String,Object>> availableEmployees() throws SQLException { return accountDAO.availableEmployees(); }
    public Map<String,Object> accountInfo(int id) throws SQLException { return accountDAO.accountInfo(id); }
    public String accountStatus(int id) throws SQLException { return accountDAO.accountStatus(id); }
    public boolean hasActiveSession(int id) throws SQLException { return accountDAO.hasActiveSession(id); }
    public List<Map<String,Object>> manageableRoles() throws SQLException { return accountDAO.manageableRoles(); }
    public Map<String,Object> sessionState(int id) throws SQLException { return accountDAO.sessionState(id); }
    public Set<String> permissionsForRole(int roleId) throws SQLException { return accountDAO.findPermissionCodesByRoleId(roleId); }
    private Account requireManagerPermission(String permission) {
        Account a=SessionManager.getCurrentAccount();
        if(a==null || !SessionManager.hasPermission(permission)) throw new IllegalStateException("Tài khoản chưa được cấp quyền thực hiện chức năng này.");
        return a;
    }
    private static Object emptyToNull(String s){return s==null||s.trim().isEmpty()?null:s.trim();}
public LoginResult login(String username, String rawPassword) {
        username = username == null ? "" : username.trim();
        rawPassword = rawPassword == null ? "" : rawPassword;
        if (username.isEmpty() || rawPassword.isEmpty()) {
            return LoginResult.failure( "Tên đăng nhập và mật khẩu không được để trống.");
        }
        try {
            Account account = accountDAO.findByUsername(username);
            if (account == null) {
                accountDAO.insertLoginLog(null, username, false, "ACCOUNT_NOT_FOUND", getDeviceInfo());
                return LoginResult.failure( "Sai tài khoản hoặc mật khẩu.");
            }
            String accountStatus = account.getStatus() == null ? "" : account.getStatus().toUpperCase();
            if (!"ACTIVE".equals(accountStatus)) {
                accountDAO.insertLoginLog(account.getAccountId(), username, false, "ACCOUNT_STATUS_" + accountStatus, getDeviceInfo());
                if ("LOCKED".equals(accountStatus)) {
                    return LoginResult.failure( "Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên.");
                }
                if ("DISABLED".equals(accountStatus)) {
                    return LoginResult.failure( "Tài khoản đã ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
                }
                if ("PENDING".equals(accountStatus)) {
                    return LoginResult.failure( "Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
                }
                return LoginResult.failure( "Tài khoản hiện không thể đăng nhập. Vui lòng liên hệ quản trị viên.");
            }
            if (account.getEmployee() == null || !"ACTIVE".equalsIgnoreCase(account.getEmployee().getStatus())) {
                accountDAO.insertLoginLog(account.getAccountId(), username, false,
                        "EMPLOYEE_STATUS_" + (account.getEmployee() == null ? "MISSING" : account.getEmployee().getStatus()),
                        getDeviceInfo());
                return LoginResult.failure( "Hồ sơ nhân viên hiện không ở trạng thái làm việc. Vui lòng liên hệ quản lý.");
            }
            if (account.getLockedUntil() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (account.getLockedUntil().isAfter(now)) {
                    accountDAO.insertLoginLog(account.getAccountId(), username, false, "TEMPORARY_LOCK", getDeviceInfo());
                    return LoginResult.failure(
                            "Tài khoản đang bị khóa tạm thời đến " + account.getLockedUntil().toLocalTime().withSecond(0).withNano(0)
                          + ". Vui lòng thử lại sau.");
                }
                accountDAO.clearExpiredTemporaryLock(account.getAccountId());
                account.setFailedAttempts(0);
                account.setLockedUntil(null);
            }
            if (!PasswordUtil.matches(rawPassword, account.getPasswordHash())) {
                int attempts = accountDAO.registerFailedAttempt(account.getAccountId());
                accountDAO.insertLoginLog(account.getAccountId(), username, false, "WRONG_PASSWORD_ATTEMPT_" + attempts, getDeviceInfo());
                if (attempts > 5) {
                    return LoginResult.failure( "Bạn đã nhập sai quá 5 lần liên tiếp. " + "Tài khoản bị khóa tạm thời 15 phút.");
                }
                if (attempts == 5) {
                    return LoginResult.failure(
                            "Sai tài khoản hoặc mật khẩu. Đây là lần sai thứ 5; "
                          + "nếu nhập sai thêm một lần, tài khoản sẽ bị khóa tạm thời 15 phút.");
                }
                return LoginResult.failure( "Sai tài khoản hoặc mật khẩu. Lần nhập sai liên tiếp: " + attempts + "/5.");
            }
            Set<String> permissions = accountDAO.findPermissionCodesByRoleId( account.getRole().getRoleId());
            accountDAO.markLoginSuccess(account.getAccountId());
            long loginLogId = accountDAO.insertLoginLog( account.getAccountId(), username, true, null, getDeviceInfo());
            SessionManager.login(account, loginLogId, permissions);
            return LoginResult.success( account, account.isFirstLogin());
        } catch (Exception e) {
            e.printStackTrace();
            return LoginResult.failure( "Không thể kết nối hoặc truy vấn cơ sở dữ liệu.");
        }
    }
    public Result changePassword(
            String currentPassword, String newPassword, String confirmPassword) {
        if (!SessionManager.isLoggedIn()) {
            return Result.failure( "Phiên đăng nhập không còn hiệu lực.");
        }
        currentPassword = currentPassword == null ? "" : currentPassword;
        newPassword = newPassword == null ? "" : newPassword;
        confirmPassword = confirmPassword == null ? "" : confirmPassword;
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return Result.failure( "Vui lòng nhập đầy đủ mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu.");
        }
        try {
            Account sessionAccount = SessionManager.getCurrentAccount();
            Account account = accountDAO.findById(sessionAccount.getAccountId());
            if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())
                    || account.getEmployee() == null || !"ACTIVE".equalsIgnoreCase(account.getEmployee().getStatus())) {
                return Result.failure( "Tài khoản hoặc hồ sơ nhân viên không còn hoạt động. Vui lòng đăng nhập lại.");
            }
            if (!PasswordUtil.matches(currentPassword, account.getPasswordHash())) {
                return Result.failure( "Mật khẩu hiện tại không đúng.");
            }
            if (!PasswordUtil.isStrongEnough(newPassword)) {
                return Result.failure( "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ và số.");
            }
            if (!newPassword.equals(confirmPassword)) {
                return Result.failure( "Mật khẩu xác nhận không khớp.");
            }
            if (PasswordUtil.matches(newPassword, account.getPasswordHash())) {
                return Result.failure( "Mật khẩu mới không được trùng mật khẩu hiện tại.");
            }
            String newHash = PasswordUtil.sha256(newPassword);
            accountDAO.updatePassword(account.getAccountId(), newHash);
            accountDAO.insertAuditLog(
                    account.getAccountId(), "PASSWORD_CHANGED", "accounts", String.valueOf(account.getAccountId()),
                    sessionAccount.isFirstLogin() ? "Đổi mật khẩu lần đầu" : "Người dùng tự đổi mật khẩu");
            sessionAccount.setPasswordHash(newHash);
            sessionAccount.setFirstLogin(false);
            return Result.success( "Đổi mật khẩu thành công.");
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.failure( "Không thể cập nhật mật khẩu.");
        }
    }
    public Account getCurrentAccountSnapshot() throws SQLException {
        if (!SessionManager.isLoggedIn()) return null;
        return accountDAO.findById( SessionManager.getCurrentAccount().getAccountId());
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
    private String getDeviceInfo() {
        String host = "unknown-host";
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        return host + " | " + System.getProperty("os.name")
                + " | Java " + System.getProperty("java.version") + " | " + System.getProperty("user.name");
    }
public void createAccountForEmployee(String employeeId, String username, String rawPassword, int roleId) throws Exception {
        Account actor = requireManagerPermission("ACCOUNT_MANAGE");
        if(employeeId==null || employeeId.trim().isEmpty()) throw new IllegalArgumentException("Chưa chọn nhân viên.");
        if(username==null || username.trim().isEmpty()) throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        if(!PasswordUtil.isStrongEnough(rawPassword)) throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số.");
        accountDAO.transaction(con -> {
            Map<String,Object> emp=accountDAO.one(con,"SELECT status,full_name FROM employees WHERE employee_id=?",employeeId);
            if(emp==null || !"ACTIVE".equals(String.valueOf(emp.get("status"))))
                throw new IllegalArgumentException("Chỉ có thể cấp tài khoản cho nhân viên đang hoạt động.");
            Map<String,Object> role=accountDAO.one(con,"SELECT role_code FROM roles WHERE role_id=?",roleId);
            if(role==null) throw new IllegalArgumentException("Vai trò không hợp lệ.");
            String roleCode=String.valueOf(role.get("role_code"));
            if(!Arrays.asList("MANAGER","SALES","CUSTOMER_SERVICE").contains(roleCode))
                throw new IllegalArgumentException("Tài khoản Admin hệ thống không được tạo tại UC005.");
            Object exists=accountDAO.scalar(con,"SELECT COUNT(*) FROM accounts WHERE employee_id=? OR username=?",employeeId,username.trim());
            if(((Number)exists).intValue()>0) throw new IllegalArgumentException("Nhân viên đã có tài khoản hoặc tên đăng nhập đã tồn tại.");
            long accountId=accountDAO.insertAndKey(con,
                    "INSERT INTO accounts(employee_id,username,password_hash,role_id,status,first_login) VALUES(?,?,?,?,'ACTIVE',1)",
                    employeeId,username.trim(),PasswordUtil.sha256(rawPassword),roleId);
            accountDAO.update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    actor.getAccountId(),"ACCOUNT_CREATED","accounts",String.valueOf(accountId),"Cấp tài khoản cho "+employeeId);
            return null;
        });
    }
    public void updateAccount(int accountId, String username, int roleId) throws Exception {
        Account actor=requireManagerPermission("ACCOUNT_MANAGE");
        if(username==null || username.trim().isEmpty()) throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        accountDAO.transaction(con -> {
            Map<String,Object> target=accountDAO.one(con,
                    "SELECT a.role_id,r.role_code,a.status FROM accounts a JOIN roles r ON r.role_id=a.role_id WHERE a.account_id=?",accountId);
            if(target==null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            if("ADMIN".equals(String.valueOf(target.get("role_code"))))
                throw new IllegalStateException("Tài khoản Admin hệ thống không được chỉnh sửa tại UC005.");
            Map<String,Object> newRole=accountDAO.one(con,"SELECT role_code FROM roles WHERE role_id=?",roleId);
            if(newRole==null) throw new IllegalArgumentException("Vai trò không hợp lệ.");
            if("ADMIN".equals(String.valueOf(newRole.get("role_code"))))
                throw new IllegalArgumentException("Không thể gán vai trò Admin tại UC005.");
            Object dup=accountDAO.scalar(con,"SELECT COUNT(*) FROM accounts WHERE username=? AND account_id<>?",username.trim(),accountId);
            if(((Number)dup).intValue()>0) throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
            if(accountId==actor.getAccountId() && ((Number)target.get("role_id")).intValue()!=roleId)
                throw new IllegalArgumentException("Không thể tự thay đổi vai trò của tài khoản đang đăng nhập.");
            if("MANAGER".equals(String.valueOf(target.get("role_code"))) && !"MANAGER".equals(String.valueOf(newRole.get("role_code")))
                    && "ACTIVE".equals(String.valueOf(target.get("status")))) {
                Object managers=accountDAO.scalar(con,
                        "SELECT COUNT(*) FROM accounts a JOIN employees e ON e.employee_id=a.employee_id JOIN roles r ON r.role_id=a.role_id "
                      + "WHERE r.role_code='MANAGER' AND a.status='ACTIVE' AND e.status='ACTIVE'");
                if(((Number)managers).intValue()<=1)
                    throw new IllegalStateException("Không thể đổi vai trò của quản lý đang hoạt động cuối cùng.");
            }
            accountDAO.update(con,"UPDATE accounts SET username=?,role_id=?,updated_at=SYSDATETIME() WHERE account_id=?",username.trim(),roleId,accountId);
            accountDAO.update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    actor.getAccountId(),"ACCOUNT_UPDATED","accounts",String.valueOf(accountId),"Cập nhật tên đăng nhập/vai trò");
            return null;
        });
    }
    public void changeAccountStatus(int accountId, String newStatus) throws Exception {
        Account actor=requireManagerPermission("ACCOUNT_MANAGE");
        if(!Arrays.asList("ACTIVE","LOCKED","DISABLED").contains(newStatus))
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        if(accountId==actor.getAccountId() && !"ACTIVE".equals(newStatus))
            throw new IllegalArgumentException("Không thể tự khóa/vô hiệu hóa tài khoản đang sử dụng.");
        accountDAO.transaction(con -> {
            Map<String,Object> target=accountDAO.one(con,
                    "SELECT a.status,r.role_code,e.status employee_status FROM accounts a JOIN roles r ON r.role_id=a.role_id "
                  + "JOIN employees e ON e.employee_id=a.employee_id WHERE a.account_id=?",accountId);
            if(target==null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            if("ADMIN".equals(String.valueOf(target.get("role_code"))))
                throw new IllegalStateException("Tài khoản Admin hệ thống không được khóa/vô hiệu hóa tại UC005.");
            if("ACTIVE".equals(newStatus) && !"ACTIVE".equals(String.valueOf(target.get("employee_status"))))
                throw new IllegalArgumentException("Không thể kích hoạt tài khoản khi hồ sơ nhân viên chưa hoạt động.");
            if(!"ACTIVE".equals(newStatus) && "MANAGER".equals(String.valueOf(target.get("role_code")))
                    && "ACTIVE".equals(String.valueOf(target.get("status")))) {
                Object managers=accountDAO.scalar(con,
                        "SELECT COUNT(*) FROM accounts a JOIN employees e ON e.employee_id=a.employee_id JOIN roles r ON r.role_id=a.role_id "
                      + "WHERE r.role_code='MANAGER' AND a.status='ACTIVE' AND e.status='ACTIVE'");
                if(((Number)managers).intValue()<=1)
                    throw new IllegalStateException("Không thể khóa/vô hiệu hóa quản lý đang hoạt động cuối cùng.");
            }
            accountDAO.update(con, "UPDATE accounts SET status=?,failed_attempts=CASE WHEN ?='ACTIVE' THEN 0 ELSE failed_attempts END,"
                  + "locked_until=CASE WHEN ?='ACTIVE' THEN NULL ELSE locked_until END,updated_at=SYSDATETIME() WHERE account_id=?",
                    newStatus,newStatus,newStatus,accountId);
            if(!"ACTIVE".equals(newStatus))
                accountDAO.update(con,"UPDATE login_logs SET logout_at=COALESCE(logout_at,SYSDATETIME()) WHERE account_id=? AND success=1 AND logout_at IS NULL",accountId);
            accountDAO.update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    actor.getAccountId(),"ACCOUNT_STATUS_CHANGED","accounts",String.valueOf(accountId),"Trạng thái -> "+newStatus);
            return null;
        });
    }
    public boolean deleteAccountOrDisable(int accountId) throws Exception {
        Account actor=requireManagerPermission("ACCOUNT_MANAGE");
        if(accountId==actor.getAccountId()) throw new IllegalArgumentException("Không thể tự xóa tài khoản đang sử dụng.");
        return accountDAO.transaction(con -> {
            Map<String,Object> target=accountDAO.one(con,
                    "SELECT a.status,r.role_code FROM accounts a JOIN roles r ON r.role_id=a.role_id WHERE a.account_id=?",accountId);
            if(target==null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            if("ADMIN".equals(String.valueOf(target.get("role_code"))))
                throw new IllegalStateException("Tài khoản Admin hệ thống không được xóa/vô hiệu hóa tại UC005.");
            if("MANAGER".equals(String.valueOf(target.get("role_code"))) && "ACTIVE".equals(String.valueOf(target.get("status")))) {
                Object managers=accountDAO.scalar(con,
                        "SELECT COUNT(*) FROM accounts a JOIN employees e ON e.employee_id=a.employee_id JOIN roles r ON r.role_id=a.role_id "
                      + "WHERE r.role_code='MANAGER' AND a.status='ACTIVE' AND e.status='ACTIVE'");
                if(((Number)managers).intValue()<=1)
                    throw new IllegalStateException("Không thể xóa quản lý đang hoạt động cuối cùng.");
            }
            Object refs=accountDAO.scalar(con,
                    "SELECT (SELECT COUNT(*) FROM login_logs WHERE account_id=?)+(SELECT COUNT(*) FROM audit_logs WHERE account_id=?)",
                    accountId,accountId);
            if(((Number)refs).intValue()>0){
                accountDAO.update(con,"UPDATE accounts SET status='DISABLED',updated_at=SYSDATETIME() WHERE account_id=?",accountId);
                accountDAO.update(con,"UPDATE login_logs SET logout_at=COALESCE(logout_at,SYSDATETIME()) WHERE account_id=? AND success=1 AND logout_at IS NULL",accountId);
                accountDAO.update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                        actor.getAccountId(),"ACCOUNT_DISABLED_PRESERVE_HISTORY","accounts",String.valueOf(accountId),"Không xóa vật lý vì tài khoản đã có lịch sử");
                return false;
            }
            accountDAO.update(con,"DELETE FROM accounts WHERE account_id=?",accountId);
            accountDAO.update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    actor.getAccountId(),"ACCOUNT_DELETED","accounts",String.valueOf(accountId),"Xóa tài khoản chưa phát sinh lịch sử");
            return true;
        });
    }
}

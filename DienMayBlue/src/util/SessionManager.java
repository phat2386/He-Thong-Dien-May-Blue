package util;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import entity.Account;
public final class SessionManager {
    private static Account currentAccount;
    private static Long loginLogId;
    private static Set<String> permissions = Collections.emptySet();
    private SessionManager() {}
    public static void login(Account account, long logId, Set<String> permissionCodes) {
        currentAccount = account;
        loginLogId = logId;
        permissions = Collections.unmodifiableSet( new HashSet<String>(permissionCodes));
    }
    public static Account getCurrentAccount() {
        return currentAccount;
    }
    public static Long getLoginLogId() {
        return loginLogId;
    }
    public static Set<String> getPermissions() {
        return permissions;
    }
    public static boolean hasPermission(String permissionCode) {
        if (currentAccount != null && currentAccount.getRole() != null && "ADMIN".equals(currentAccount.getRole().getRoleCode())) {
            return true;
        }
        return permissions.contains(permissionCode);
    }
    public static boolean isLoggedIn() {
        return currentAccount != null;
    }
    public static void clear() {
        currentAccount = null;
        loginLogId = null;
        permissions = Collections.emptySet();
    }
}

package entity;
import java.time.LocalDateTime;
public class Account {
    private int accountId;
    private String username, passwordHash, status;
    private boolean firstLogin;
    private int failedAttempts;
    private LocalDateTime lockedUntil, lastLoginAt;
    private Employee employee;
    private Role role;
    public int getAccountId(){return accountId;} public void setAccountId(int v){accountId=v;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public boolean isFirstLogin(){return firstLogin;} public void setFirstLogin(boolean v){firstLogin=v;}
    public int getFailedAttempts(){return failedAttempts;} public void setFailedAttempts(int v){failedAttempts=v;}
    public LocalDateTime getLockedUntil(){return lockedUntil;} public void setLockedUntil(LocalDateTime v){lockedUntil=v;}
    public LocalDateTime getLastLoginAt(){return lastLoginAt;} public void setLastLoginAt(LocalDateTime v){lastLoginAt=v;}
    public Employee getEmployee(){return employee;} public void setEmployee(Employee v){employee=v;}
    public Role getRole(){return role;} public void setRole(Role v){role=v;}
}

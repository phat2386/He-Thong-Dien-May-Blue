package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Truy cập dữ liệu cho nhân viên và phân ca.
 */
public class EmployeeDAO extends BaseDAO {

    public boolean employeeIdOrPhoneExists(Connection con, String employeeId, String phone)
            throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM employees WHERE employee_id=? OR phone=?", employeeId, phone);
        return count != null && ((Number) count).intValue() > 0;
    }

    public void insertEmployee(
            Connection con, String employeeId, String fullName, String phone,
            Object email, Object department, LocalDate joinDate) throws SQLException {
        update(con, "INSERT INTO employees(employee_id,full_name,phone,email,department,join_date,status) " +
                "VALUES(?,?,?,?,?,?,'ACTIVE')", employeeId, fullName, phone, email, department, joinDate);
    }

    public boolean employeeExists(Connection con, String employeeId) throws SQLException {
        return scalar(con, "SELECT employee_id FROM employees WHERE employee_id=?", employeeId) != null;
    }

    public boolean isAdminEmployee(Connection con, String employeeId) throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM accounts a JOIN roles r ON r.role_id=a.role_id " +
                "WHERE a.employee_id=? AND r.role_code='ADMIN'", employeeId);
        return count != null && ((Number) count).intValue() > 0;
    }

    public boolean phoneUsedByAnotherEmployee(Connection con, String phone, String employeeId)
            throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM employees WHERE phone=? AND employee_id<>?", phone, employeeId);
        return count != null && ((Number) count).intValue() > 0;
    }

    public void updateEmployee(
            Connection con, String employeeId, String fullName, String phone,
            Object email, Object department, LocalDate joinDate) throws SQLException {
        update(con, "UPDATE employees SET full_name=?,phone=?,email=?,department=?,join_date=?," +
                "updated_at=SYSDATETIME() WHERE employee_id=?", fullName, phone, email, department, joinDate, employeeId);
    }

    public Map<String, Object> findEmployeeAccountState(Connection con, String employeeId)
            throws SQLException {
        return one(con, "SELECT e.status,r.role_code,a.account_id,a.status account_status FROM employees e " +
                "LEFT JOIN accounts a ON a.employee_id=e.employee_id " + "LEFT JOIN roles r ON r.role_id=a.role_id WHERE e.employee_id=?",
                employeeId);
    }

    public int activeManagerCount(Connection con) throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM accounts a " +
                "JOIN employees e ON e.employee_id=a.employee_id " + "JOIN roles r ON r.role_id=a.role_id " +
                "WHERE r.role_code='MANAGER' AND a.status='ACTIVE' AND e.status='ACTIVE'");
        return count == null ? 0 : ((Number) count).intValue();
    }

    public void updateEmployeeStatus(Connection con, String employeeId, String status) throws SQLException {
        update(con, "UPDATE employees SET status=?,updated_at=SYSDATETIME() WHERE employee_id=?", status, employeeId);
    }

    public void disableEmployeeAccount(Connection con, String employeeId) throws SQLException {
        update(con, "UPDATE accounts SET status='DISABLED',locked_until=NULL,updated_at=SYSDATETIME() " +
                "WHERE employee_id=?", employeeId);
    }

    public void closeEmployeeLoginSessions(Connection con, String employeeId) throws SQLException {
        update(con, "UPDATE login_logs SET logout_at=COALESCE(logout_at,SYSDATETIME()) " +
                "WHERE account_id=(SELECT account_id FROM accounts WHERE employee_id=?) " + "AND success=1 AND logout_at IS NULL",
                employeeId);
    }

    public void cancelFutureAssignments(Connection con, String employeeId) throws SQLException {
        update(con, "UPDATE shift_assignments SET status='CANCELLED' " +
                "WHERE employee_id=? AND status='ASSIGNED' AND work_date>=CAST(GETDATE() AS DATE)", employeeId);
    }

    public Map<String, Object> findEmployeeForShift(Connection con, String employeeId)
            throws SQLException {
        return one(con, "SELECT e.full_name,e.status employee_status,a.status account_status,r.role_code " +
                "FROM employees e JOIN accounts a ON a.employee_id=e.employee_id " +
                "JOIN roles r ON r.role_id=a.role_id WHERE e.employee_id=?", employeeId);
    }

    public Map<String, Object> findShift(Connection con, int shiftId) throws SQLException {
        return one(con, "SELECT shift_name,start_time,end_time,active FROM shifts WHERE shift_id=?", shiftId);
    }

    public boolean hasApprovedLeave(Connection con, String employeeId, LocalDate date)
            throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM leave_requests " +
                "WHERE employee_id=? AND leave_date=? AND status='APPROVED'", employeeId, date);
        return count != null && ((Number) count).intValue() > 0;
    }

    public boolean hasAssignedShift(Connection con, String employeeId, LocalDate date)
            throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM shift_assignments " +
                "WHERE employee_id=? AND work_date=? AND status='ASSIGNED'", employeeId, date);
        return count != null && ((Number) count).intValue() > 0;
    }

    public Object findCancelledAssignmentId(
            Connection con, String employeeId, LocalDate date, int shiftId) throws SQLException {
        return scalar(con, "SELECT TOP 1 assignment_id FROM shift_assignments " +
                "WHERE employee_id=? AND work_date=? AND shift_id=? AND status='CANCELLED' " + "ORDER BY assignment_id DESC",
                employeeId, date, shiftId);
    }

    public void reactivateAssignment(Connection con, Object assignmentId, String assignedBy)
            throws SQLException {
        update(con, "UPDATE shift_assignments SET assigned_by=?,status='ASSIGNED',created_at=SYSDATETIME() " +
                "WHERE assignment_id=?", assignedBy, assignmentId);
    }

    public void insertShiftAssignment(
            Connection con, String employeeId, LocalDate date, int shiftId, String assignedBy) throws SQLException {
        update(con, "INSERT INTO shift_assignments(employee_id,work_date,shift_id,assigned_by,status) " +
                "VALUES(?,?,?,?,'ASSIGNED')", employeeId, date, shiftId, assignedBy);
    }

    public Map<String, Object> findAssignment(Connection con, long assignmentId) throws SQLException {
        return one(con, "SELECT sa.employee_id,sa.work_date,sa.shift_id,sa.status,e.full_name,r.role_code,s.shift_name " +
                "FROM shift_assignments sa " + "JOIN employees e ON e.employee_id=sa.employee_id " +
                "JOIN shifts s ON s.shift_id=sa.shift_id " + "LEFT JOIN accounts a ON a.employee_id=e.employee_id " +
                "LEFT JOIN roles r ON r.role_id=a.role_id WHERE sa.assignment_id=?", assignmentId);
    }

    public int otherAssignedEmployees(
            Connection con, Object workDate, Object shiftId, long assignmentId) throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM shift_assignments " +
                "WHERE work_date=? AND shift_id=? AND status='ASSIGNED' AND assignment_id<>?", workDate, shiftId, assignmentId);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public int otherAssignedSalesEmployees(
            Connection con, Object workDate, Object shiftId, long assignmentId) throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM shift_assignments sa " +
                "JOIN accounts a ON a.employee_id=sa.employee_id " + "JOIN roles r ON r.role_id=a.role_id " +
                "WHERE sa.work_date=? AND sa.shift_id=? AND sa.status='ASSIGNED' " + "AND sa.assignment_id<>? AND r.role_code='SALES'",
                workDate, shiftId, assignmentId);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public void cancelAssignment(Connection con, long assignmentId) throws SQLException {
        update(con, "UPDATE shift_assignments SET status='CANCELLED' WHERE assignment_id=?", assignmentId);
    }

    public void insertNotification(Connection con, Object employeeId, String title, String content)
            throws SQLException {
        update(con, "INSERT INTO notifications(employee_id,title,content) VALUES(?,?,?)", employeeId, title, content);
    }

    public void insertAuditLog(
            Connection con, int accountId, String action, String entity, String entityId, String detail) throws SQLException {
        update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)", accountId, action,
                entity, entityId, detail);
    }
}

package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import connectDB.ConnectDB;

/** Truy cập dữ liệu hồ sơ nhân viên, ca làm việc, phân ca và thông báo. */
public class EmployeeDAO {

    // ===== Danh sách và tra cứu cho màn hình =====

    public List<Map<String, Object>> search(String keyword, String statusCode)
            throws SQLException {

        String sql =
                "SELECT e.employee_id, e.full_name, e.phone, e.email, e.department, "
              + "e.join_date, e.status, a.status AS account_status "
              + "FROM employees e "
              + "LEFT JOIN accounts a ON a.employee_id = e.employee_id "
              + "LEFT JOIN roles r ON r.role_id = a.role_id "
              + "WHERE ISNULL(r.role_code, '') <> 'ADMIN' "
              + "AND (e.employee_id LIKE ? OR e.full_name LIKE ? "
              + "OR e.phone LIKE ? OR ISNULL(e.email, '') LIKE ?) ";

        if (statusCode == null) {
            return queryList(sql + "ORDER BY e.full_name",
                    keyword, keyword, keyword, keyword);
        }
        return queryList(sql + "AND e.status = ? ORDER BY e.full_name",
                keyword, keyword, keyword, keyword, statusCode);
    }

    public Map<String, Object> findById(String employeeId) throws SQLException {
        return queryOne(
                "SELECT * FROM employees WHERE employee_id = ?", employeeId);
    }

    public String findStatus(String employeeId) throws SQLException {
        Object status = queryValue(
                "SELECT status FROM employees WHERE employee_id = ?", employeeId);
        return status == null ? null : String.valueOf(status);
    }

    // ===== Kiểm tra điều kiện (chạy trong transaction của service) =====

    public boolean idOrPhoneExists(Connection con, String employeeId, String phone)
            throws SQLException {

        return queryExists(con,
                "SELECT COUNT(*) FROM employees WHERE employee_id = ? OR phone = ?",
                employeeId, phone);
    }

    public boolean exists(Connection con, String employeeId) throws SQLException {
        return queryValue(con,
                "SELECT employee_id FROM employees WHERE employee_id = ?",
                employeeId) != null;
    }

    public boolean isAdmin(Connection con, String employeeId) throws SQLException {
        return queryExists(con,
                "SELECT COUNT(*) FROM accounts a "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE a.employee_id = ? AND r.role_code = 'ADMIN'",
                employeeId);
    }

    public boolean phoneUsedByAnother(Connection con, String phone, String employeeId)
            throws SQLException {

        return queryExists(con,
                "SELECT COUNT(*) FROM employees WHERE phone = ? AND employee_id <> ?",
                phone, employeeId);
    }

    /** Gồm status, role_code, account_id, account_status của nhân viên. */
    public Map<String, Object> findAccountState(Connection con, String employeeId)
            throws SQLException {

        return queryOne(con,
                "SELECT e.status, r.role_code, a.account_id, a.status AS account_status "
              + "FROM employees e "
              + "LEFT JOIN accounts a ON a.employee_id = e.employee_id "
              + "LEFT JOIN roles r ON r.role_id = a.role_id "
              + "WHERE e.employee_id = ?",
                employeeId);
    }

    public int countActiveManagers(Connection con) throws SQLException {
        return queryCount(con,
                "SELECT COUNT(*) FROM accounts a "
              + "JOIN employees e ON e.employee_id = a.employee_id "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE r.role_code = 'MANAGER' "
              + "AND a.status = 'ACTIVE' AND e.status = 'ACTIVE'");
    }

    // ===== Thêm / sửa =====

    public void insert(Connection con, String employeeId, String fullName,
                       String phone, Object email, Object department,
                       LocalDate joinDate) throws SQLException {

        runUpdate(con,
                "INSERT INTO employees "
              + "(employee_id, full_name, phone, email, department, join_date, status) "
              + "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')",
                employeeId, fullName, phone, email, department, joinDate);
    }

    public void update(Connection con, String employeeId, String fullName,
                       String phone, Object email, Object department,
                       LocalDate joinDate) throws SQLException {

        runUpdate(con,
                "UPDATE employees SET full_name = ?, phone = ?, email = ?, "
              + "department = ?, join_date = ?, updated_at = SYSDATETIME() "
              + "WHERE employee_id = ?",
                fullName, phone, email, department, joinDate, employeeId);
    }

    public void updateStatus(Connection con, String employeeId, String status)
            throws SQLException {

        runUpdate(con,
                "UPDATE employees SET status = ?, updated_at = SYSDATETIME() "
              + "WHERE employee_id = ?",
                status, employeeId);
    }

    /** Khi nhân viên ngừng làm việc thì tài khoản của họ cũng bị vô hiệu hóa. */
    public void disableAccountOf(Connection con, String employeeId)
            throws SQLException {

        runUpdate(con,
                "UPDATE accounts SET status = 'DISABLED', locked_until = NULL, "
              + "updated_at = SYSDATETIME() WHERE employee_id = ?",
                employeeId);
    }

    // ===== Ca làm việc và phân ca =====



    public List<Map<String, Object>> findAssignmentsByDate(LocalDate date)
            throws SQLException {

        return queryList(
                "SELECT sa.assignment_id, sa.work_date, s.shift_id, s.shift_name, "
              + "s.start_time, s.end_time, e.employee_id, e.full_name, "
              + "ro.role_code, sa.status, "
              + "CASE WHEN EXISTS (SELECT 1 FROM shift_assignments sx "
              + "      JOIN accounts ax ON ax.employee_id = sx.employee_id "
              + "      JOIN roles rx ON rx.role_id = ax.role_id "
              + "      WHERE sx.work_date = sa.work_date AND sx.shift_id = sa.shift_id "
              + "      AND sx.status = 'ASSIGNED' AND rx.role_code = 'SALES') "
              + "     THEN N'Đủ' ELSE N'Thiếu NV bán hàng' END AS coverage "
              + "FROM shift_assignments sa "
              + "JOIN shifts s ON s.shift_id = sa.shift_id "
              + "JOIN employees e ON e.employee_id = sa.employee_id "
              + "LEFT JOIN accounts a ON a.employee_id = e.employee_id "
              + "LEFT JOIN roles ro ON ro.role_id = a.role_id "
              + "WHERE sa.work_date = ? AND sa.status = 'ASSIGNED' "
              + "ORDER BY s.start_time, e.full_name",
                date);
    }

    /** Nhân viên bán hàng / CSKH đang hoạt động, có thể được phân ca. */
    public List<Map<String, Object>> findAssignableStaff() throws SQLException {
        return queryList(
                "SELECT e.employee_id, e.full_name, r.role_name, r.role_code "
              + "FROM employees e "
              + "JOIN accounts a ON a.employee_id = e.employee_id "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE e.status = 'ACTIVE' AND a.status = 'ACTIVE' "
              + "AND r.role_code IN ('SALES', 'CUSTOMER_SERVICE') "
              + "ORDER BY e.full_name");
    }

    public List<Map<String, Object>> findActiveShifts() throws SQLException {
        return queryList(
                "SELECT shift_id, shift_name, start_time, end_time "
              + "FROM shifts WHERE active = 1 ORDER BY start_time");
    }

    public int countAssignedSales(LocalDate date, int shiftId) throws SQLException {
        Object count = queryValue(
                "SELECT COUNT(*) FROM shift_assignments sa "
              + "JOIN accounts a ON a.employee_id = sa.employee_id "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE sa.work_date = ? AND sa.shift_id = ? "
              + "AND sa.status = 'ASSIGNED' AND r.role_code = 'SALES'",
                date, shiftId);
        return count == null ? 0 : ((Number) count).intValue();
    }

    // ===== Kiểm tra điều kiện (chạy trong transaction của service) =====

    public Map<String, Object> findEmployeeForShift(Connection con, String employeeId)
            throws SQLException {

        return queryOne(con,
                "SELECT e.full_name, e.status AS employee_status, "
              + "a.status AS account_status, r.role_code "
              + "FROM employees e "
              + "JOIN accounts a ON a.employee_id = e.employee_id "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE e.employee_id = ?",
                employeeId);
    }

    public Map<String, Object> findShift(Connection con, int shiftId)
            throws SQLException {

        return queryOne(con,
                "SELECT shift_name, start_time, end_time, active "
              + "FROM shifts WHERE shift_id = ?",
                shiftId);
    }

    public boolean hasApprovedLeave(Connection con, String employeeId, LocalDate date)
            throws SQLException {

        return queryExists(con,
                "SELECT COUNT(*) FROM leave_requests "
              + "WHERE employee_id = ? AND leave_date = ? AND status = 'APPROVED'",
                employeeId, date);
    }

    public boolean hasAssignedShift(Connection con, String employeeId, LocalDate date)
            throws SQLException {

        return queryExists(con,
                "SELECT COUNT(*) FROM shift_assignments "
              + "WHERE employee_id = ? AND work_date = ? AND status = 'ASSIGNED'",
                employeeId, date);
    }

    public Object findCancelledAssignmentId(Connection con, String employeeId,
                                            LocalDate date, int shiftId)
            throws SQLException {

        return queryValue(con,
                "SELECT TOP 1 assignment_id FROM shift_assignments "
              + "WHERE employee_id = ? AND work_date = ? AND shift_id = ? "
              + "AND status = 'CANCELLED' ORDER BY assignment_id DESC",
                employeeId, date, shiftId);
    }

    public Map<String, Object> findAssignment(Connection con, long assignmentId)
            throws SQLException {

        return queryOne(con,
                "SELECT sa.employee_id, sa.work_date, sa.shift_id, sa.status, "
              + "e.full_name, r.role_code, s.shift_name "
              + "FROM shift_assignments sa "
              + "JOIN employees e ON e.employee_id = sa.employee_id "
              + "JOIN shifts s ON s.shift_id = sa.shift_id "
              + "LEFT JOIN accounts a ON a.employee_id = e.employee_id "
              + "LEFT JOIN roles r ON r.role_id = a.role_id "
              + "WHERE sa.assignment_id = ?",
                assignmentId);
    }

    public int countOtherAssigned(Connection con, Object workDate,
                                  Object shiftId, long assignmentId)
            throws SQLException {

        return queryCount(con,
                "SELECT COUNT(*) FROM shift_assignments "
              + "WHERE work_date = ? AND shift_id = ? "
              + "AND status = 'ASSIGNED' AND assignment_id <> ?",
                workDate, shiftId, assignmentId);
    }

    public int countOtherAssignedSales(Connection con, Object workDate,
                                       Object shiftId, long assignmentId)
            throws SQLException {

        return queryCount(con,
                "SELECT COUNT(*) FROM shift_assignments sa "
              + "JOIN accounts a ON a.employee_id = sa.employee_id "
              + "JOIN roles r ON r.role_id = a.role_id "
              + "WHERE sa.work_date = ? AND sa.shift_id = ? "
              + "AND sa.status = 'ASSIGNED' AND sa.assignment_id <> ? "
              + "AND r.role_code = 'SALES'",
                workDate, shiftId, assignmentId);
    }

    // ===== Thêm / sửa =====

    public void insertAssignment(Connection con, String employeeId, LocalDate date,
                                 int shiftId, String assignedBy)
            throws SQLException {

        runUpdate(con,
                "INSERT INTO shift_assignments "
              + "(employee_id, work_date, shift_id, assigned_by, status) "
              + "VALUES (?, ?, ?, ?, 'ASSIGNED')",
                employeeId, date, shiftId, assignedBy);
    }

    public void reactivateAssignment(Connection con, Object assignmentId,
                                     String assignedBy) throws SQLException {

        runUpdate(con,
                "UPDATE shift_assignments SET assigned_by = ?, status = 'ASSIGNED', "
              + "created_at = SYSDATETIME() WHERE assignment_id = ?",
                assignedBy, assignmentId);
    }

    public void cancelAssignment(Connection con, long assignmentId)
            throws SQLException {

        runUpdate(con,
                "UPDATE shift_assignments SET status = 'CANCELLED' "
              + "WHERE assignment_id = ?",
                assignmentId);
    }

    /** Hủy mọi ca từ hôm nay trở đi của nhân viên (khi ngừng làm việc). */
    public void cancelFutureAssignments(Connection con, String employeeId)
            throws SQLException {

        runUpdate(con,
                "UPDATE shift_assignments SET status = 'CANCELLED' "
              + "WHERE employee_id = ? AND status = 'ASSIGNED' "
              + "AND work_date >= CAST(GETDATE() AS DATE)",
                employeeId);
    }

    // ===== Thông báo, nhật ký =====

    public void insertNotification(Connection con, Object employeeId,
                                   String title, String content) throws SQLException {

        runUpdate(con,
                "INSERT INTO notifications (employee_id, title, content) VALUES (?, ?, ?)",
                employeeId, title, content);
    }

    public void insertAuditLog(Connection con, int accountId, String actionCode,
                               String entityName, String entityId, String detail)
            throws SQLException {

        runUpdate(con,
                "INSERT INTO audit_logs "
              + "(account_id, action_code, entity_name, entity_id, detail) "
              + "VALUES (?, ?, ?, ?, ?)",
                accountId, actionCode, entityName, entityId, detail);
    }

    /** Kết thúc mọi phiên đăng nhập còn mở của tài khoản thuộc một nhân viên. */
    public void closeLoginSessions(Connection con, String employeeId) throws SQLException {
        runUpdate(con,
                "UPDATE login_logs "
              + "SET logout_at = COALESCE(logout_at, SYSDATETIME()) "
              + "WHERE account_id = (SELECT account_id FROM accounts WHERE employee_id = ?) "
              + "AND success = 1 AND logout_at IS NULL",
                employeeId);
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
}

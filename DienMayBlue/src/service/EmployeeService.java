package service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import connectDB.ConnectDB;
import entity.Account;
import util.SessionManager;
import dao.EmployeeDAO;

/** UC032 - Quản lý hồ sơ nhân viên và UC006 - Phân ca làm việc. */
public class EmployeeService {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    // ===== Truy vấn cho màn hình =====

    public List<Map<String, Object>> searchEmployees(String keyword, String statusCode)
            throws SQLException {
        return employeeDAO.search(keyword, statusCode);
    }

    public Map<String, Object> findEmployee(String employeeId) throws SQLException {
        return employeeDAO.findById(employeeId);
    }

    public String employeeStatus(String employeeId) throws SQLException {
        return employeeDAO.findStatus(employeeId);
    }

    public List<Map<String, Object>> assignmentsByDate(LocalDate date)
            throws SQLException {
        return employeeDAO.findAssignmentsByDate(date);
    }

    public List<Map<String, Object>> assignableStaff() throws SQLException {
        return employeeDAO.findAssignableStaff();
    }

    public List<Map<String, Object>> activeShifts() throws SQLException {
        return employeeDAO.findActiveShifts();
    }

    public int countAssignedSales(LocalDate date, int shiftId) throws SQLException {
        return employeeDAO.countAssignedSales(date, shiftId);
    }

    // ===== UC032 - Hồ sơ nhân viên =====

    public void createEmployee(String employeeId, String fullName, String phone,
                               String email, String department, LocalDate joinDate)
            throws Exception {

        Account actor = requirePermission("EMPLOYEE_MANAGE");
        validateEmployeeInput(employeeId, fullName, phone, email, joinDate);

        String id = employeeId.trim();
        String tel = phone.trim();

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                if (employeeDAO.idOrPhoneExists(con, id, tel)) {
                    throw new IllegalArgumentException(
                            "Mã nhân viên hoặc số điện thoại đã tồn tại.");
                }

                employeeDAO.insert(con, id, fullName.trim(), tel,
                        emptyToNull(email), emptyToNull(department), joinDate);

                employeeDAO.insertAuditLog(con, actor.getAccountId(), "EMPLOYEE_CREATED",
                        "employees", id, "Tạo hồ sơ nhân viên: " + fullName.trim());

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void updateEmployee(String employeeId, String fullName, String phone,
                               String email, String department, LocalDate joinDate)
            throws Exception {

        Account actor = requirePermission("EMPLOYEE_MANAGE");
        validateEmployeeInput(employeeId, fullName, phone, email, joinDate);

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                if (!employeeDAO.exists(con, employeeId)) {
                    throw new IllegalArgumentException("Không tìm thấy nhân viên.");
                }
                if (employeeDAO.isAdmin(con, employeeId)) {
                    throw new IllegalStateException(
                            "Hồ sơ Admin hệ thống không được chỉnh sửa tại UC032.");
                }
                if (employeeDAO.phoneUsedByAnother(con, phone.trim(), employeeId)) {
                    throw new IllegalArgumentException(
                            "Số điện thoại đã thuộc nhân viên khác.");
                }

                employeeDAO.update(con, employeeId, fullName.trim(), phone.trim(),
                        emptyToNull(email), emptyToNull(department), joinDate);

                employeeDAO.insertAuditLog(con, actor.getAccountId(), "EMPLOYEE_UPDATED",
                        "employees", employeeId, "Cập nhật hồ sơ nhân viên");

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void setEmployeeActive(String employeeId, boolean active)
            throws Exception {

        Account actor = requirePermission("EMPLOYEE_MANAGE");

        if (!active && actor.getEmployee().getEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException(
                    "Không thể tự ngừng hoạt động hồ sơ đang đăng nhập.");
        }

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                Map<String, Object> employee =
                        employeeDAO.findAccountState(con, employeeId);
                if (employee == null) {
                    throw new IllegalArgumentException("Không tìm thấy nhân viên.");
                }

                String roleCode = String.valueOf(employee.get("role_code"));
                if ("ADMIN".equals(roleCode)) {
                    throw new IllegalStateException(
                            "Hồ sơ Admin hệ thống không được ngừng/kích hoạt tại UC032.");
                }

                boolean activeManager = !active && "MANAGER".equals(roleCode)
                        && "ACTIVE".equals(String.valueOf(employee.get("account_status")));
                if (activeManager && employeeDAO.countActiveManagers(con) <= 1) {
                    throw new IllegalStateException(
                            "Không thể ngừng hoạt động quản lý đang hoạt động cuối cùng của hệ thống.");
                }

                employeeDAO.updateStatus(con, employeeId, active ? "ACTIVE" : "INACTIVE");

                // Ngừng làm việc: khóa tài khoản, đóng phiên và hủy ca tương lai.
                if (!active) {
                    employeeDAO.disableAccountOf(con, employeeId);
                    employeeDAO.closeLoginSessions(con, employeeId);
                    employeeDAO.cancelFutureAssignments(con, employeeId);
                }

                employeeDAO.insertAuditLog(con, actor.getAccountId(),
                        active ? "EMPLOYEE_REACTIVATED" : "EMPLOYEE_DEACTIVATED",
                        "employees", employeeId,
                        active ? "Kích hoạt lại hồ sơ nhân viên"
                               : "Ngừng hoạt động; vô hiệu hóa tài khoản và hủy ca tương lai");

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ===== UC006 - Phân ca =====

    public void assignShift(String employeeId, LocalDate date, int shiftId)
            throws Exception {

        Account actor = requirePermission("SHIFT_MANAGE");

        if (date == null || date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể phân ca cho ngày đã qua.");
        }

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                Map<String, Object> employee =
                        employeeDAO.findEmployeeForShift(con, employeeId);
                if (employee == null) {
                    throw new IllegalArgumentException(
                            "Nhân viên chưa có tài khoản/vai trò.");
                }
                if (!"ACTIVE".equals(String.valueOf(employee.get("employee_status")))) {
                    throw new IllegalArgumentException(
                            "Nhân viên không ở trạng thái làm việc.");
                }
                if (!"ACTIVE".equals(String.valueOf(employee.get("account_status")))) {
                    throw new IllegalArgumentException(
                            "Tài khoản nhân viên không hoạt động.");
                }

                String role = String.valueOf(employee.get("role_code"));
                if (!"SALES".equals(role) && !"CUSTOMER_SERVICE".equals(role)) {
                    throw new IllegalArgumentException(
                            "UC006 chỉ phân ca cho Nhân viên bán hàng và Nhân viên CSKH.");
                }

                Map<String, Object> shift = employeeDAO.findShift(con, shiftId);
                if (shift == null || !Boolean.TRUE.equals(shift.get("active"))) {
                    throw new IllegalArgumentException(
                            "Ca làm việc không tồn tại hoặc đã ngừng sử dụng.");
                }
                if (employeeDAO.hasApprovedLeave(con, employeeId, date)) {
                    throw new IllegalArgumentException(
                            "Nhân viên đã được duyệt nghỉ trong ngày đã chọn.");
                }
                if (employeeDAO.hasAssignedShift(con, employeeId, date)) {
                    throw new IllegalArgumentException(
                            "Nhân viên đã có ca làm việc trong ngày này.");
                }

                String assignedBy = actor.getEmployee().getEmployeeId();
                Object cancelledId = employeeDAO.findCancelledAssignmentId(
                        con, employeeId, date, shiftId);

                if (cancelledId != null) {
                    employeeDAO.reactivateAssignment(con, cancelledId, assignedBy);
                } else {
                    employeeDAO.insertAssignment(con, employeeId, date, shiftId, assignedBy);
                }

                employeeDAO.insertNotification(con, employeeId, "Lịch ca làm việc mới",
                        "Ngày " + date + " - " + shift.get("shift_name")
                      + " (" + shift.get("start_time") + " - " + shift.get("end_time") + ")");

                employeeDAO.insertAuditLog(con, actor.getAccountId(), "SHIFT_ASSIGNED",
                        "shift_assignments", employeeId,
                        "Phân ca ngày " + date + ", shift_id=" + shiftId);

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void cancelShiftAssignment(long assignmentId) throws Exception {
        Account actor = requirePermission("SHIFT_MANAGE");

        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);

            try {
                Map<String, Object> assignment =
                        employeeDAO.findAssignment(con, assignmentId);
                if (assignment == null) {
                    throw new IllegalArgumentException("Không tìm thấy phân ca.");
                }
                if (!"ASSIGNED".equals(String.valueOf(assignment.get("status")))) {
                    throw new IllegalArgumentException(
                            "Phân ca này không còn ở trạng thái đang phân công.");
                }

                // Không được hủy nhân viên bán hàng cuối cùng khi ca vẫn còn người khác.
                if ("SALES".equals(String.valueOf(assignment.get("role_code")))) {
                    int others = employeeDAO.countOtherAssigned(con,
                            assignment.get("work_date"), assignment.get("shift_id"),
                            assignmentId);
                    int otherSales = employeeDAO.countOtherAssignedSales(con,
                            assignment.get("work_date"), assignment.get("shift_id"),
                            assignmentId);

                    if (others > 0 && otherSales == 0) {
                        throw new IllegalStateException(
                                "Không thể hủy nhân viên bán hàng cuối cùng khi ca vẫn còn nhân viên khác. "
                              + "Hãy phân công nhân viên bán hàng thay thế trước.");
                    }
                }

                employeeDAO.cancelAssignment(con, assignmentId);

                employeeDAO.insertNotification(con, assignment.get("employee_id"),
                        "Lịch ca đã hủy",
                        "Ca " + assignment.get("shift_name") + " ngày "
                      + assignment.get("work_date") + " đã được hủy.");

                employeeDAO.insertAuditLog(con, actor.getAccountId(), "SHIFT_CANCELLED",
                        "shift_assignments", String.valueOf(assignmentId),
                        "Hủy phân ca của " + assignment.get("employee_id")
                      + " ngày " + assignment.get("work_date"));

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ===== Hàm phụ =====

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

    private void validateEmployeeInput(String employeeId, String fullName,
                                       String phone, String email,
                                       LocalDate joinDate) {

        if (employeeId == null || employeeId.trim().isEmpty()
                || fullName == null || fullName.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Mã nhân viên, họ tên và số điện thoại không được để trống.");
        }
        if (!phone.trim().matches("[0-9+]{9,15}")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (email != null && !email.trim().isEmpty()
                && !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (joinDate == null || joinDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Ngày vào làm không được để trống hoặc nằm trong tương lai.");
        }
    }

    private static Object emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

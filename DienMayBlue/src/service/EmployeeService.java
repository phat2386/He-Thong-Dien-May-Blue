package service;

import java.time.LocalDate;
import java.util.Map;

import dao.EmployeeDAO;
import entity.Account;

public class EmployeeService extends BaseService<EmployeeDAO> {

    public EmployeeService() {
        super(new EmployeeDAO());
    }

    public void createEmployee(
            String employeeId, String fullName, String phone, String email, String department, LocalDate joinDate) throws Exception {
        Account actor = requireManagerPermission("EMPLOYEE_MANAGE");
        validateEmployeeInput(employeeId, fullName, phone, email, joinDate);
        dao.transaction(con -> {
            String id = employeeId.trim();
            String tel = phone.trim();
            if (dao.employeeIdOrPhoneExists(con, id, tel)) {
                throw new IllegalArgumentException("Mã nhân viên hoặc số điện thoại đã tồn tại.");
            }
            dao.insertEmployee( con, id, fullName.trim(), tel, emptyToNull(email), emptyToNull(department), joinDate);
            dao.insertAuditLog(
                    con, actor.getAccountId(), "EMPLOYEE_CREATED", "employees", id, "Tạo hồ sơ nhân viên: " + fullName.trim());
            return null;
        });
    }

    public void updateEmployee(
            String employeeId, String fullName, String phone, String email, String department, LocalDate joinDate) throws Exception {
        Account actor = requireManagerPermission("EMPLOYEE_MANAGE");
        validateEmployeeInput(employeeId, fullName, phone, email, joinDate);
        dao.transaction(con -> {
            if (!dao.employeeExists(con, employeeId)) {
                throw new IllegalArgumentException("Không tìm thấy nhân viên.");
            }
            if (dao.isAdminEmployee(con, employeeId)) {
                throw new IllegalStateException("Hồ sơ Admin hệ thống không được chỉnh sửa tại UC032.");
            }
            if (dao.phoneUsedByAnotherEmployee(con, phone.trim(), employeeId)) {
                throw new IllegalArgumentException("Số điện thoại đã thuộc nhân viên khác.");
            }
            dao.updateEmployee( con, employeeId, fullName.trim(), phone.trim(), emptyToNull(email), emptyToNull(department), joinDate);
            dao.insertAuditLog( con, actor.getAccountId(), "EMPLOYEE_UPDATED", "employees", employeeId, "Cập nhật hồ sơ nhân viên");
            return null;
        });
    }

    public void setEmployeeActive(String employeeId, boolean active) throws Exception {
        Account actor = requireManagerPermission("EMPLOYEE_MANAGE");
        if (!active && actor.getEmployee().getEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException("Không thể tự ngừng hoạt động hồ sơ đang đăng nhập.");
        }
        dao.transaction(con -> {
            Map<String, Object> employee = dao.findEmployeeAccountState(con, employeeId);
            if (employee == null) {
                throw new IllegalArgumentException("Không tìm thấy nhân viên.");
            }
            if ("ADMIN".equals(String.valueOf(employee.get("role_code")))) {
                throw new IllegalStateException("Hồ sơ Admin hệ thống không được ngừng/kích hoạt tại UC032.");
            }
            boolean activeManager = !active && "MANAGER".equals(String.valueOf(employee.get("role_code")))
                    && "ACTIVE".equals(String.valueOf(employee.get("account_status")));
            if (activeManager && dao.activeManagerCount(con) <= 1) {
                throw new IllegalStateException( "Không thể ngừng hoạt động quản lý đang hoạt động cuối cùng của hệ thống.");
            }
            String nextStatus = active ? "ACTIVE" : "INACTIVE";
            dao.updateEmployeeStatus(con, employeeId, nextStatus);
            if (!active) {
                dao.disableEmployeeAccount(con, employeeId);
                dao.closeEmployeeLoginSessions(con, employeeId);
                dao.cancelFutureAssignments(con, employeeId);
            }
            dao.insertAuditLog(
                    con, actor.getAccountId(), active ? "EMPLOYEE_REACTIVATED" : "EMPLOYEE_DEACTIVATED", "employees",
                    employeeId, active ? "Kích hoạt lại hồ sơ nhân viên" : "Ngừng hoạt động; vô hiệu hóa tài khoản và hủy ca tương lai");
            return null;
        });
    }

    private void validateEmployeeInput(
            String employeeId, String fullName, String phone, String email, LocalDate joinDate) {
        if (employeeId == null || employeeId.trim().isEmpty() || fullName == null || fullName.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException( "Mã nhân viên, họ tên và số điện thoại không được để trống.");
        }
        if (!phone.trim().matches("[0-9+]{9,15}")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (email != null && !email.trim().isEmpty() && !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (joinDate == null || joinDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException( "Ngày vào làm không được để trống hoặc nằm trong tương lai.");
        }
    }

    public void assignShift(String employeeId, LocalDate date, int shiftId) throws Exception {
        Account actor = requireManagerPermission("SHIFT_MANAGE");
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể phân ca cho ngày đã qua.");
        }
        dao.transaction(con -> {
            Map<String, Object> employee = dao.findEmployeeForShift(con, employeeId);
            if (employee == null) {
                throw new IllegalArgumentException("Nhân viên chưa có tài khoản/vai trò.");
            }
            if (!"ACTIVE".equals(String.valueOf(employee.get("employee_status")))) {
                throw new IllegalArgumentException("Nhân viên không ở trạng thái làm việc.");
            }
            if (!"ACTIVE".equals(String.valueOf(employee.get("account_status")))) {
                throw new IllegalArgumentException("Tài khoản nhân viên không hoạt động.");
            }
            String role = String.valueOf(employee.get("role_code"));
            if (!"SALES".equals(role) && !"CUSTOMER_SERVICE".equals(role)) {
                throw new IllegalArgumentException( "UC006 chỉ phân ca cho Nhân viên bán hàng và Nhân viên CSKH.");
            }
            Map<String, Object> shift = dao.findShift(con, shiftId);
            if (shift == null || !Boolean.TRUE.equals(shift.get("active"))) {
                throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc đã ngừng sử dụng.");
            }
            if (dao.hasApprovedLeave(con, employeeId, date)) {
                throw new IllegalArgumentException("Nhân viên đã được duyệt nghỉ trong ngày đã chọn.");
            }
            if (dao.hasAssignedShift(con, employeeId, date)) {
                throw new IllegalArgumentException("Nhân viên đã có ca làm việc trong ngày này.");
            }
            Object cancelledId = dao.findCancelledAssignmentId(con, employeeId, date, shiftId);
            if (cancelledId != null) {
                dao.reactivateAssignment( con, cancelledId, actor.getEmployee().getEmployeeId());
            } else {
                dao.insertShiftAssignment( con, employeeId, date, shiftId, actor.getEmployee().getEmployeeId());
            }
            dao.insertNotification(
                    con, employeeId, "Lịch ca làm việc mới", "Ngày " + date
                            + " - " + shift.get("shift_name") + " (" + shift.get("start_time") + " - " + shift.get("end_time") + ")");
            dao.insertAuditLog(
                    con, actor.getAccountId(), "SHIFT_ASSIGNED", "shift_assignments",
                    employeeId, "Phân ca ngày " + date + ", shift_id=" + shiftId);
            return null;
        });
    }

    public void cancelShiftAssignment(long assignmentId) throws Exception {
        Account actor = requireManagerPermission("SHIFT_MANAGE");
        dao.transaction(con -> {
            Map<String, Object> assignment = dao.findAssignment(con, assignmentId);
            if (assignment == null) {
                throw new IllegalArgumentException("Không tìm thấy phân ca.");
            }
            if (!"ASSIGNED".equals(String.valueOf(assignment.get("status")))) {
                throw new IllegalArgumentException( "Phân ca này không còn ở trạng thái đang phân công.");
            }
            if ("SALES".equals(String.valueOf(assignment.get("role_code")))) {
                int others = dao.otherAssignedEmployees( con, assignment.get("work_date"), assignment.get("shift_id"), assignmentId);
                int otherSales = dao.otherAssignedSalesEmployees(
                        con, assignment.get("work_date"), assignment.get("shift_id"), assignmentId);
                if (others > 0 && otherSales == 0) {
                    throw new IllegalStateException(
                            "Không thể hủy nhân viên bán hàng cuối cùng khi ca vẫn còn nhân viên khác. " +
                            "Hãy phân công nhân viên bán hàng thay thế trước.");
                }
            }
            dao.cancelAssignment(con, assignmentId);
            dao.insertNotification(
                    con, assignment.get("employee_id"), "Lịch ca đã hủy", "Ca " + assignment.get("shift_name")
                            + " ngày " + assignment.get("work_date") + " đã được hủy.");
            dao.insertAuditLog(
                    con, actor.getAccountId(), "SHIFT_CANCELLED", "shift_assignments",
                    String.valueOf(assignmentId), "Hủy phân ca của " + assignment.get("employee_id")
                            + " ngày " + assignment.get("work_date"));
            return null;
        });
    }
}

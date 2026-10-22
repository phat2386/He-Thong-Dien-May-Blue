package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dao.OperationDAO;
import entity.Account;
import util.SessionManager;

public class OperationService extends BaseService<OperationDAO> {

    public static final BigDecimal DELIVERY_FEE_INNER_CITY = BigDecimal.ZERO;
    public static final BigDecimal DELIVERY_FEE_OTHER_PROVINCE = new BigDecimal("50000");
    public static final BigDecimal INSTALLATION_FEE = new BigDecimal("100000");
    public static final BigDecimal DEFAULT_BONUS_RATE = new BigDecimal("0.03");
    public static final BigDecimal REFUND_APPROVAL_LIMIT = new BigDecimal("5000000");

    public OperationService() {
        super(new OperationDAO());
    }

    protected void ensureCanSell(Connection con, Account account) throws Exception {
        Map<String, Object> state = dao.findAccountOperationState(con, account.getAccountId());
        if (state == null) {
            throw new IllegalStateException("Tài khoản không còn tồn tại trong hệ thống.");
        }
        if (!"ACTIVE".equals(String.valueOf(state.get("account_status")))) {
            throw new IllegalStateException("Tài khoản hiện không ở trạng thái hoạt động.");
        }
        if (!"ACTIVE".equals(String.valueOf(state.get("employee_status")))) {
            throw new IllegalStateException("Nhân viên hiện không ở trạng thái làm việc.");
        }
        if (!"SALES".equals(String.valueOf(state.get("role_code")))) {
            return;
        }
        if (!dao.hasCurrentShift(con, account.getEmployee().getEmployeeId())) {
            throw new IllegalStateException( "Bạn chưa có ca làm việc hợp lệ tại thời điểm hiện tại.");
        }
    }

    public BigDecimal calculateDeliveryFee(String region, boolean install) {
        BigDecimal fee;
        if ("INNER_CITY".equals(region)) {
            fee = DELIVERY_FEE_INNER_CITY;
        } else if ("OTHER_PROVINCE".equals(region)) {
            fee = DELIVERY_FEE_OTHER_PROVINCE;
        } else {
            throw new IllegalArgumentException("Khu vực giao hàng không hợp lệ.");
        }
        return install ? fee.add(INSTALLATION_FEE) : fee;
    }

    public String createDelivery(
            String invoiceId, String address, String region, LocalDate date,
            LocalTime from, LocalTime to, boolean install, String note) throws Exception {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn hóa đơn.");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không được rỗng.");
        }
        if (date == null || from == null || to == null) {
            throw new IllegalArgumentException("Ngày/khung giờ giao hàng không hợp lệ.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày giao hàng không được ở trong quá khứ.");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("Khung giờ giao không hợp lệ.");
        }
        if (!Arrays.asList("INNER_CITY", "OTHER_PROVINCE").contains(region)) {
            throw new IllegalArgumentException("Khu vực giao hàng không hợp lệ.");
        }
        LocalTime open = LocalTime.of(9, 0);
        LocalTime close = "INNER_CITY".equals(region) ? LocalTime.of(21, 0) : LocalTime.of(20, 0);
        if (from.isBefore(open) || to.isAfter(close)) {
            throw new IllegalArgumentException("Khung giờ không đúng chính sách giao hàng.");
        }
        Account account = requirePermission("DELIVERY_CREATE");
        BigDecimal fee = calculateDeliveryFee(region, install);
        String cleanNote = note == null ? "" : note.trim();
        String normalizedInvoiceId = invoiceId.trim();
        return dao.transaction(con -> {
            ensureCanSell(con, account);
            Map<String, Object> invoice = dao.findInvoiceStatus(con, normalizedInvoiceId);
            if (invoice == null) {
                throw new IllegalArgumentException("Không tìm thấy hóa đơn.");
            }
            if (!"PAID".equals(String.valueOf(invoice.get("status")))) {
                throw new IllegalArgumentException( "Chỉ hóa đơn đã thanh toán đầy đủ mới được tạo phiếu giao hàng.");
            }
            if (dao.deliveryExistsForInvoice(con, normalizedInvoiceId)) {
                throw new IllegalArgumentException( "Hóa đơn này đã có phiếu giao hàng; hãy xử lý trên phiếu hiện có.");
            }
            String deliveryId = nextDeliveryId(con);
            dao.insertDelivery( con, deliveryId, normalizedInvoiceId, address.trim(), region, date, from, to, install, fee);
            dao.insertDeliveryHistory(
                    con, deliveryId, null, "PREPARING",
                    account.getEmployee().getEmployeeId(), cleanNote.isEmpty() ? "Tạo phiếu giao hàng" : cleanNote);
            return deliveryId;
        });
    }

    private String nextDeliveryId(Connection con) throws SQLException {
        int next = Math.max(dao.maxShortDeliveryNumber(con), dao.deliveryCount(con)) + 1;
        while (next <= 99999) {
            String candidate = String.format("GH%05d", next);
            if (!dao.deliveryIdExists(con, candidate)) {
                return candidate;
            }
            next++;
        }
        throw new IllegalStateException("Đã vượt quá giới hạn mã phiếu giao hàng GH99999.");
    }

    public List<Map<String, Object>> getAvailableDeliveryEmployees(String deliveryId)
            throws Exception {
        if (deliveryId == null || deliveryId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return dao.findAvailableDeliveryEmployees(deliveryId.trim());
    }

    public void assignDelivery(String deliveryId, String employeeId, boolean confirmReassign)
            throws Exception {
        if (deliveryId == null || deliveryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn phiếu giao hàng.");
        }
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn nhân viên phụ trách.");
        }
        Account account = requirePermission("DELIVERY_ASSIGN");
        String normalizedDeliveryId = deliveryId.trim();
        String normalizedEmployeeId = employeeId.trim();
        dao.transaction(con -> {
            ensureCanSell(con, account);
            Map<String, Object> delivery = dao.findDeliveryAssignment(con, normalizedDeliveryId);
            if (delivery == null) {
                throw new IllegalArgumentException("Không tìm thấy phiếu giao hàng.");
            }
            if (!"PREPARING".equals(String.valueOf(delivery.get("status")))) {
                throw new IllegalArgumentException( "Chỉ phiếu đang chuẩn bị mới được phân công hoặc phân công lại.");
            }
            String oldAssignee = delivery.get("assigned_employee_id") == null ? null
                    : String.valueOf(delivery.get("assigned_employee_id"));
            if (normalizedEmployeeId.equals(oldAssignee)) {
                throw new IllegalArgumentException("Phiếu đã được phân công cho nhân viên này.");
            }
            if (oldAssignee != null && !confirmReassign) {
                throw new IllegalArgumentException( "Phiếu đã có người phụ trách. Cần xác nhận trước khi phân công lại.");
            }
            if (!dao.isEligibleDeliveryEmployee(con, normalizedDeliveryId, normalizedEmployeeId)) {
                throw new IllegalArgumentException( "Nhân viên được chọn không có ca phù hợp với ngày/khung giờ giao hàng.");
            }
            if (dao.assignDelivery(con, normalizedDeliveryId, normalizedEmployeeId) != 1) {
                throw new IllegalStateException( "Phiếu vừa được thay đổi, vui lòng tải lại danh sách.");
            }
            String action = oldAssignee == null ? "Phân công giao hàng" : "Phân công lại giao hàng";
            dao.insertDeliveryHistory(
                    con, normalizedDeliveryId, "PREPARING", "PREPARING",
                    account.getEmployee().getEmployeeId(), action + " cho " + normalizedEmployeeId);
            return null;
        });
    }

    public void updateDeliveryStatus(String deliveryId, String newStatus, String note)
            throws Exception {
        Account account = requirePermission("DELIVERY_MANAGE");
        dao.transaction(con -> {
            ensureCanSell(con, account);
            Map<String, Object> delivery = deliveryForUpdate( con, deliveryId, account.getEmployee().getEmployeeId());
            String oldStatus = String.valueOf(delivery.get("status"));
            boolean install = Boolean.TRUE.equals(delivery.get("install_required"));
            if (!validDeliveryTransition(oldStatus, newStatus, install)) {
                throw new IllegalArgumentException( "Không thể chuyển trạng thái từ " + oldStatus + " sang " + newStatus + ".");
            }
            dao.updateDeliveryStatus(con, deliveryId, newStatus);
            dao.insertDeliveryHistory( con, deliveryId, oldStatus, newStatus, account.getEmployee().getEmployeeId(), emptyToNull(note));
            return null;
        });
    }

    public void completeDelivery(
            String deliveryId, String proofPath, String signaturePath, String note) throws Exception {
        Account account = requirePermission("DELIVERY_MANAGE");
        String proof = proofPath == null ? "" : proofPath.trim();
        String signature = signaturePath == null ? "" : signaturePath.trim();
        if (proof.isEmpty() && signature.isEmpty()) {
            throw new IllegalArgumentException( "Cần ít nhất ảnh bằng chứng hoặc chữ ký xác nhận của khách hàng.");
        }
        dao.transaction(con -> {
            ensureCanSell(con, account);
            Map<String, Object> delivery = deliveryForUpdate( con, deliveryId, account.getEmployee().getEmployeeId());
            String oldStatus = String.valueOf(delivery.get("status"));
            if (!"DELIVERING".equals(oldStatus)) {
                throw new IllegalArgumentException( "Chỉ phiếu đang giao mới được xác nhận hoàn tất.");
            }
            boolean install = Boolean.TRUE.equals(delivery.get("install_required"));
            String nextStatus = install ? "INSTALLED" : "DELIVERED";
            dao.completeDelivery( con, deliveryId, nextStatus, emptyToNull(proof), emptyToNull(signature));
            dao.insertDeliveryHistory( con, deliveryId, oldStatus, nextStatus, account.getEmployee().getEmployeeId(), emptyToNull(note));
            return null;
        });
    }

    public LocalDate failDelivery(String deliveryId, String reason) throws Exception {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cần nhập lý do giao không thành công.");
        }
        Account account = requirePermission("DELIVERY_MANAGE");
        return dao.transaction(con -> {
            ensureCanSell(con, account);
            Map<String, Object> delivery = deliveryForUpdate( con, deliveryId, account.getEmployee().getEmployeeId());
            if (!"DELIVERING".equals(String.valueOf(delivery.get("status")))) {
                throw new IllegalArgumentException( "Chỉ phiếu đang giao mới được ghi nhận giao không thành công.");
            }
            LocalDate deliveryDate = ((java.sql.Date) delivery.get("delivery_date")).toLocalDate();
            LocalDate proposed = deliveryDate.plusDays(1);
            if (!proposed.isAfter(LocalDate.now())) {
                proposed = LocalDate.now().plusDays(1);
            }
            dao.failDelivery(con, deliveryId, reason.trim(), proposed);
            dao.insertDeliveryHistory( con, deliveryId, "DELIVERING", "FAILED", account.getEmployee().getEmployeeId(), reason.trim());
            return proposed;
        });
    }

    public LocalDate retryDelivery(String deliveryId) throws Exception {
        Account account = requirePermission("DELIVERY_MANAGE");
        return dao.transaction(con -> {
            ensureCanSell(con, account);
            Map<String, Object> delivery = deliveryForUpdate( con, deliveryId, account.getEmployee().getEmployeeId());
            if (!"FAILED".equals(String.valueOf(delivery.get("status")))) {
                throw new IllegalArgumentException( "Chỉ phiếu giao không thành công mới được lập lịch giao lại.");
            }
            LocalDate next = delivery.get("reschedule_date") == null ? LocalDate.now().plusDays(1)
                    : ((java.sql.Date) delivery.get("reschedule_date")).toLocalDate();
            if (!next.isAfter(LocalDate.now())) {
                next = LocalDate.now().plusDays(1);
            }
            dao.retryDelivery(con, deliveryId, next);
            dao.insertDeliveryHistory(
                    con, deliveryId, "FAILED", "PREPARING",
                    account.getEmployee().getEmployeeId(), "Lập lịch giao lại ngày " + next + "; chờ phân công lại");
            return next;
        });
    }

    private Map<String, Object> deliveryForUpdate(
            Connection con, String deliveryId, String employeeId) throws Exception {
        if (deliveryId == null || deliveryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn phiếu giao hàng.");
        }
        Map<String, Object> delivery = dao.findDeliveryForUpdate(con, deliveryId.trim());
        if (delivery == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu giao hàng.");
        }
        String assigned = delivery.get("assigned_employee_id") == null ? null : String.valueOf(delivery.get("assigned_employee_id"));
        boolean admin = SessionManager.getCurrentAccount() != null && SessionManager.getCurrentAccount().getRole() != null
                && "ADMIN".equals(SessionManager.getCurrentAccount().getRole().getRoleCode());
        if (!admin) {
            if (assigned == null) {
                throw new IllegalArgumentException( "Phiếu chưa được phân công. Hãy thực hiện UC033 trước.");
            }
            if (!employeeId.equals(assigned)) {
                throw new IllegalStateException( "Chỉ nhân viên được phân công mới được cập nhật phiếu giao hàng này.");
            }
        }
        return delivery;
    }

    private boolean validDeliveryTransition(String oldStatus, String nextStatus, boolean install) {
        return "PREPARING".equals(oldStatus) && "DELIVERING".equals(nextStatus);
    }

    public BigDecimal calculateRefundAmount(long invoiceDetailId, int quantity) throws Exception {
        Map<String, Object> detail = dao.findInvoiceDetailForRefund(invoiceDetailId);
        if (detail == null) {
            throw new IllegalArgumentException("Không tìm thấy chi tiết hóa đơn.");
        }
        int soldQuantity = ((Number) detail.get("quantity")).intValue();
        if (quantity <= 0 || quantity > soldQuantity) {
            throw new IllegalArgumentException("Số lượng đổi trả không hợp lệ.");
        }
        return proportionalRefund( (BigDecimal) detail.get("line_total"), soldQuantity, quantity);
    }

    private BigDecimal proportionalRefund(BigDecimal lineTotal, int soldQty, int returnQty) {
        return lineTotal .multiply(BigDecimal.valueOf(returnQty)) .divide(BigDecimal.valueOf(soldQty), 2, RoundingMode.HALF_UP);
    }

    public String createReturn(
            String invoiceId, long invoiceDetailId, int quantity, String reason,
            String condition, boolean seal, boolean accessories, String evidencePath) throws Exception {
        if (invoiceId == null || invoiceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn hóa đơn.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cần nhập lý do đổi trả/hoàn tiền.");
        }
        if (condition == null || condition.trim().isEmpty()) {
            throw new IllegalArgumentException("Cần ghi nhận tình trạng thực tế của sản phẩm.");
        }
        if (!seal || !accessories) {
            throw new IllegalArgumentException( "Không đủ điều kiện đổi trả: sản phẩm phải còn tem/mác và đầy đủ phụ kiện.");
        }
        Account account = requirePermission("RETURN_CREATE");
        String normalizedInvoiceId = invoiceId.trim();
        return dao.transaction(con -> {
            String returnId = nextReturnId(con);
            ensureCanSell(con, account);
            String employeeId = account.getEmployee().getEmployeeId();
            Map<String, Object> invoice = dao.findInvoiceForReturn(con, normalizedInvoiceId);
            if (invoice == null) {
                throw new IllegalArgumentException("Không tìm thấy hóa đơn.");
            }
            if (!"PAID".equals(String.valueOf(invoice.get("status")))) {
                throw new IllegalArgumentException(
                        "Hóa đơn này đã phát sinh hoàn tiền hoặc không còn ở trạng thái đủ điều kiện đổi trả.");
            }
            LocalDate purchaseDate = ((java.sql.Timestamp) invoice.get("created_at")) .toLocalDateTime() .toLocalDate();
            if (purchaseDate.plusDays(7).isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Hóa đơn đã quá thời hạn đổi trả 7 ngày.");
            }
            if (dao.returnExistsForInvoice(con, normalizedInvoiceId)) {
                throw new IllegalArgumentException(
                        "Hóa đơn này đã có phiếu đổi trả/hoàn tiền. " + "Theo quy định mỗi hóa đơn chỉ lập tối đa một phiếu; " +
                        "trường hợp phát sinh thêm cần chuyển Quản lý xử lý riêng.");
            }
            Map<String, Object> detail = dao.findInvoiceDetailForReturn( con, invoiceDetailId, normalizedInvoiceId);
            if (detail == null) {
                throw new IllegalArgumentException("Chi tiết hóa đơn không hợp lệ.");
            }
            int soldQuantity = ((Number) detail.get("quantity")).intValue();
            if (quantity <= 0 || quantity > soldQuantity) {
                throw new IllegalArgumentException("Số lượng đổi trả không hợp lệ.");
            }
            BigDecimal refund = proportionalRefund( (BigDecimal) detail.get("line_total"), soldQuantity, quantity);
            if (refund.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá trị hoàn tiền không hợp lệ.");
            }
            boolean requiresApproval = refund.compareTo(REFUND_APPROVAL_LIMIT) > 0;
            String status = requiresApproval ? "WAITING_APPROVAL" : "PENDING";
            dao.insertReturnRequest(
                    con, returnId, normalizedInvoiceId, employeeId, reason.trim(), condition.trim(), emptyToNull(evidencePath), refund,
                    status);
            dao.insertReturnDetail(
                    con, returnId, invoiceDetailId, String.valueOf(detail.get("product_id")), quantity, refund, seal, accessories);
            if (requiresApproval) {
                dao.insertPendingRefundApproval(con, returnId);
            } else {
                completeRefund(con, returnId);
            }
            dao.insertAuditLog(
                    con, account.getAccountId(), "RETURN_CREATED", "return_requests", returnId, requiresApproval
                            ? "Lập phiếu đổi trả; chờ duyệt do vượt hạn mức" : "Lập phiếu đổi trả; xử lý trong hạn mức");
            return returnId;
        });
    }

    private String nextReturnId(Connection con) throws SQLException {
        int next = Math.max(dao.maxShortReturnNumber(con), dao.returnCount(con)) + 1;
        while (next <= 99999) {
            String candidate = String.format("HT%05d", next);
            if (!dao.returnIdExists(con, candidate)) {
                return candidate;
            }
            next++;
        }
        throw new IllegalStateException("Đã vượt quá giới hạn mã phiếu HT99999.");
    }

    private void completeRefund(Connection con, String returnId) throws Exception {
        Map<String, Object> request = dao.findReturnRequest(con, returnId);
        if (request == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu đổi trả.");
        }
        if ("COMPLETED".equals(String.valueOf(request.get("status")))) {
            throw new IllegalStateException("Phiếu này đã được hoàn tiền trước đó.");
        }
        if ("REJECTED".equals(String.valueOf(request.get("status")))) {
            throw new IllegalStateException("Phiếu đã bị từ chối.");
        }
        String invoiceId = String.valueOf(request.get("invoice_id"));
        BigDecimal refund = (BigDecimal) request.get("refund_amount");
        Map<String, Object> invoice = dao.findInvoiceRefundData(con, invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn gốc.");
        }
        BigDecimal invoiceTotal = (BigDecimal) invoice.get("total_amount");
        List<Map<String, Object>> details = dao.findReturnDetails(con, returnId);
        if (details.isEmpty()) {
            throw new IllegalArgumentException("Phiếu đổi trả chưa có sản phẩm.");
        }
        for (Map<String, Object> detail : details) {
            int quantity = ((Number) detail.get("quantity")).intValue();
            String productId = String.valueOf(detail.get("product_id"));
            dao.restoreProductStock(con, productId, quantity);
        }
        BigDecimal priorRefunds = dao.priorCompletedRefunds(con, invoiceId, returnId);
        BigDecimal totalRefunded = priorRefunds.add(refund);
        String invoiceStatus = totalRefunded.compareTo(invoiceTotal) >= 0 ? "REFUNDED" : "PARTIAL_REFUND";
        dao.updateInvoiceStatus(con, invoiceId, invoiceStatus);
        Map<String, Object> payment = dao.findSuccessfulPayment(con, invoiceId);
        if (payment != null) {
            dao.insertRefundPayment( con, invoiceId, String.valueOf(payment.get("payment_method")), refund, "REFUND:" + returnId);
        }
        Object customerId = invoice.get("customer_id");
        if (customerId != null) {
            int earned = dao.earnedPoints(con, invoiceId);
            int targetReverse = totalRefunded.compareTo(invoiceTotal) >= 0 ? earned : BigDecimal.valueOf(earned) .multiply(refund)
                            .divide(invoiceTotal, 0, RoundingMode.DOWN) .intValue();
            int alreadyReversed = dao.reversedPoints(con, invoiceId);
            int reverse = Math.max(0, targetReverse - alreadyReversed);
            if (reverse > 0) {
                int currentPoints = dao.currentCustomerPoints(con, customerId);
                int actualReverse = Math.min(reverse, currentPoints);
                if (actualReverse > 0) {
                    dao.insertPointReversal(con, customerId, invoiceId, actualReverse);
                    dao.subtractCustomerPoints(con, customerId, actualReverse);
                    dao.refreshCustomerTier(con, customerId);
                }
            }
        }
        BigDecimal rate = dao.findBonusRate( con, invoiceId, invoice.get("employee_id"), DEFAULT_BONUS_RATE);
        BigDecimal bonusReverse = refund .multiply(rate) .setScale(2, RoundingMode.HALF_UP);
        if (bonusReverse.compareTo(BigDecimal.ZERO) > 0) {
            dao.insertBonusReversal( con, invoice.get("employee_id"), invoiceId, refund, rate, bonusReverse);
        }
        dao.markReturnCompleted(con, returnId);
    }

    public void approveRefund(String returnId, boolean approve, String reason) throws Exception {
        Account account = requirePermission("REFUND_APPROVE");
        if (!approve && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException("Cần nhập lý do từ chối.");
        }
        String employeeId = account.getEmployee().getEmployeeId();
        dao.transaction(con -> {
            Map<String, Object> request = dao.findRefundApprovalRequest(con, returnId);
            if (request == null) {
                throw new IllegalArgumentException("Không tìm thấy phiếu.");
            }
            if (!"WAITING_APPROVAL".equals(String.valueOf(request.get("status")))) {
                throw new IllegalStateException("Phiếu không còn ở trạng thái chờ duyệt.");
            }
            if (((BigDecimal) request.get("refund_amount")).compareTo(REFUND_APPROVAL_LIMIT) <= 0) {
                throw new IllegalStateException( "Phiếu này không thuộc nhóm vượt hạn mức cần Quản lý duyệt.");
            }
            int changed;
            if (approve) {
                changed = dao.decideRefundApproval( con, returnId, "APPROVED", employeeId, emptyToNull(reason));
                if (changed != 1) {
                    throw new IllegalStateException("Phiếu đã được Quản lý khác xử lý.");
                }
                dao.markReturnApproved(con, returnId);
                completeRefund(con, returnId);
            } else {
                changed = dao.decideRefundApproval( con, returnId, "REJECTED", employeeId, reason.trim());
                if (changed != 1) {
                    throw new IllegalStateException("Phiếu đã được Quản lý khác xử lý.");
                }
                dao.markReturnRejected(con, returnId, reason.trim());
            }
            dao.insertAuditLog(
                    con, account.getAccountId(), approve ? "REFUND_APPROVED" : "REFUND_REJECTED", "return_requests",
                    returnId, approve ? "Quản lý duyệt và thực hiện hoàn tiền" : "Quản lý từ chối: " + reason.trim());
            return null;
        });
    }
}

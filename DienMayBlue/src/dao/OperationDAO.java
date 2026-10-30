package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Truy cập dữ liệu cho giao hàng, đổi trả và hoàn tiền.
 */
public class OperationDAO extends BaseDAO {

    public Map<String, Object> findAccountOperationState(Connection con, int accountId)
            throws SQLException {
        return one(con, "SELECT ac.status account_status,e.status employee_status,r.role_code " +
                "FROM accounts ac JOIN employees e ON e.employee_id=ac.employee_id " +
                "JOIN roles r ON r.role_id=ac.role_id WHERE ac.account_id=?", accountId);
    }

    public boolean hasCurrentShift(Connection con, String employeeId) throws SQLException {
        return scalar(con, "SELECT TOP 1 sa.assignment_id FROM shift_assignments sa " +
                "JOIN shifts s ON s.shift_id=sa.shift_id " + "WHERE sa.employee_id=? AND sa.work_date=CAST(GETDATE() AS DATE) " +
                "AND sa.status='ASSIGNED' AND s.active=1 " +
                "AND CAST(GETDATE() AS TIME)>=s.start_time AND CAST(GETDATE() AS TIME)<s.end_time", employeeId) != null;
    }

    public Map<String, Object> findInvoiceStatus(Connection con, String invoiceId) throws SQLException {
        return one(con, "SELECT invoice_id,status FROM invoices WHERE invoice_id=?", invoiceId);
    }

    public boolean deliveryExistsForInvoice(Connection con, String invoiceId) throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM deliveries WHERE invoice_id=?", invoiceId);
        return count != null && ((Number) count).intValue() > 0;
    }

    public void insertDelivery(
            Connection con, String deliveryId, String invoiceId, String address,
            String region, LocalDate date, LocalTime from, LocalTime to, boolean install, BigDecimal fee) throws SQLException {
        update(con, "INSERT INTO deliveries(" + "delivery_id,invoice_id,assigned_employee_id,delivery_address,region_type," +
                "delivery_date,time_from,time_to,install_required,delivery_fee,status) " + "VALUES(?,?,NULL,?,?,?,?,?,?,?,'PREPARING')",
                deliveryId, invoiceId, address, region, date, from, to, install, fee);
    }

    public void insertDeliveryHistory(
            Connection con, String deliveryId, Object oldStatus, String newStatus, String changedBy, Object note) throws SQLException {
        update(con, "INSERT INTO delivery_history(delivery_id,old_status,new_status,changed_by,note) " + "VALUES(?,?,?,?,?)", deliveryId,
                oldStatus, newStatus, changedBy, note);
    }

    public int maxShortDeliveryNumber(Connection con) throws SQLException {
        Object value = scalar(con, "SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(delivery_id,3,5))),0) " +
                "FROM deliveries WHERE LEN(delivery_id)=7 " + "AND delivery_id LIKE 'GH[0-9][0-9][0-9][0-9][0-9]'");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int deliveryCount(Connection con) throws SQLException {
        Object value = scalar(con, "SELECT COUNT(*) FROM deliveries");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public boolean deliveryIdExists(Connection con, String deliveryId) throws SQLException {
        Object value = scalar(con, "SELECT COUNT(*) FROM deliveries WHERE delivery_id=?", deliveryId);
        return value != null && ((Number) value).intValue() > 0;
    }

    public List<Map<String, Object>> findAvailableDeliveryEmployees(String deliveryId)
            throws SQLException {
        return query(
                "SELECT DISTINCT e.employee_id,e.full_name " + "FROM deliveries d " +
                "JOIN shift_assignments sa ON sa.work_date=d.delivery_date AND sa.status='ASSIGNED' " +
                "JOIN shifts s ON s.shift_id=sa.shift_id AND s.active=1 " +
                "JOIN employees e ON e.employee_id=sa.employee_id AND e.status='ACTIVE' " +
                "JOIN accounts ac ON ac.employee_id=e.employee_id AND ac.status='ACTIVE' " +
                "JOIN roles r ON r.role_id=ac.role_id AND r.role_code='SALES' " +
                "WHERE d.delivery_id=? AND s.start_time<=d.time_from AND s.end_time>=d.time_to " +
                "AND NOT EXISTS(SELECT 1 FROM leave_requests lr WHERE lr.employee_id=e.employee_id " +
                "AND lr.leave_date=d.delivery_date AND lr.status='APPROVED') " + "ORDER BY e.full_name", deliveryId);
    }

    public Map<String, Object> findDeliveryAssignment(Connection con, String deliveryId)
            throws SQLException {
        return one(con, "SELECT status,assigned_employee_id FROM deliveries WHERE delivery_id=?", deliveryId);
    }

    public boolean isEligibleDeliveryEmployee(
            Connection con, String deliveryId, String employeeId) throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM deliveries d " +
                "JOIN shift_assignments sa ON sa.work_date=d.delivery_date " + "AND sa.employee_id=? AND sa.status='ASSIGNED' " +
                "JOIN shifts s ON s.shift_id=sa.shift_id AND s.active=1 " +
                "JOIN employees e ON e.employee_id=sa.employee_id AND e.status='ACTIVE' " +
                "JOIN accounts ac ON ac.employee_id=e.employee_id AND ac.status='ACTIVE' " +
                "JOIN roles r ON r.role_id=ac.role_id AND r.role_code='SALES' " +
                "WHERE d.delivery_id=? AND s.start_time<=d.time_from AND s.end_time>=d.time_to " +
                "AND NOT EXISTS(SELECT 1 FROM leave_requests lr WHERE lr.employee_id=e.employee_id " +
                "AND lr.leave_date=d.delivery_date AND lr.status='APPROVED')", employeeId, deliveryId);
        return count != null && ((Number) count).intValue() > 0;
    }

    public int assignDelivery(Connection con, String deliveryId, String employeeId) throws SQLException {
        return update(con, "UPDATE deliveries SET assigned_employee_id=?,updated_at=SYSDATETIME() " +
                "WHERE delivery_id=? AND status='PREPARING'", employeeId, deliveryId);
    }

    public Map<String, Object> findDeliveryForUpdate(Connection con, String deliveryId)
            throws SQLException {
        return one(con, "SELECT status,install_required,assigned_employee_id,delivery_date,reschedule_date " +
                "FROM deliveries WHERE delivery_id=?", deliveryId);
    }

    public void updateDeliveryStatus(Connection con, String deliveryId, String status)
            throws SQLException {
        update(con, "UPDATE deliveries SET status=?,updated_at=SYSDATETIME() WHERE delivery_id=?", status, deliveryId);
    }

    public void completeDelivery(
            Connection con, String deliveryId, String status, Object proofPath, Object signaturePath) throws SQLException {
        update(con, "UPDATE deliveries SET status=?,proof_image_path=?,signature_path=?," +
                "failure_reason=NULL,updated_at=SYSDATETIME() WHERE delivery_id=?", status, proofPath, signaturePath, deliveryId);
    }

    public void failDelivery(
            Connection con, String deliveryId, String reason, LocalDate rescheduleDate) throws SQLException {
        update(con, "UPDATE deliveries SET status='FAILED',failure_reason=?,reschedule_date=?," +
                "updated_at=SYSDATETIME() WHERE delivery_id=?", reason, rescheduleDate, deliveryId);
    }

    public void retryDelivery(Connection con, String deliveryId, LocalDate nextDate)
            throws SQLException {
        update(con, "UPDATE deliveries SET status='PREPARING',delivery_date=?,assigned_employee_id=NULL," +
                "reschedule_date=NULL,updated_at=SYSDATETIME() WHERE delivery_id=?", nextDate, deliveryId);
    }

    public Map<String, Object> findInvoiceDetailForRefund(long invoiceDetailId) throws SQLException {
        return one( "SELECT quantity,line_total FROM invoice_details WHERE invoice_detail_id=?", invoiceDetailId);
    }

    public Map<String, Object> findInvoiceForReturn(Connection con, String invoiceId)
            throws SQLException {
        return one(con, "SELECT invoice_id,created_at,status,total_amount FROM invoices WHERE invoice_id=?", invoiceId);
    }

    public boolean returnExistsForInvoice(Connection con, String invoiceId) throws SQLException {
        return scalar(con, "SELECT TOP 1 return_id FROM return_requests WHERE invoice_id=?", invoiceId) != null;
    }

    public Map<String, Object> findInvoiceDetailForReturn(
            Connection con, long invoiceDetailId, String invoiceId) throws SQLException {
        return one(con, "SELECT product_id,quantity,line_total FROM invoice_details " +
                "WHERE invoice_detail_id=? AND invoice_id=?", invoiceDetailId, invoiceId);
    }

    public void insertReturnRequest(
            Connection con, String returnId, String invoiceId, String createdBy,
            String reason, String condition, Object evidencePath, BigDecimal refundAmount, String status) throws SQLException {
        update(con, "INSERT INTO return_requests(" +
                "return_id,invoice_id,created_by,reason,physical_condition,evidence_image_path,refund_amount,status) " +
                "VALUES(?,?,?,?,?,?,?,?)", returnId, invoiceId, createdBy, reason, condition, evidencePath, refundAmount, status);
    }

    public void insertReturnDetail(
            Connection con, String returnId, long invoiceDetailId, String productId,
            int quantity, BigDecimal refundAmount, boolean sealIntact, boolean accessoriesComplete) throws SQLException {
        update(con, "INSERT INTO return_details(" +
                "return_id,invoice_detail_id,product_id,quantity,refund_amount,seal_intact,accessories_complete) " +
                "VALUES(?,?,?,?,?,?,?)", returnId, invoiceDetailId, productId, quantity, refundAmount, sealIntact, accessoriesComplete);
    }

    public void insertPendingRefundApproval(Connection con, String returnId) throws SQLException {
        update(con, "INSERT INTO refund_approvals(return_id,decision) VALUES(?,'PENDING')", returnId);
    }

    public int maxShortReturnNumber(Connection con) throws SQLException {
        Object value = scalar(con, "SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(return_id,3,5))),0) " +
                "FROM return_requests WHERE LEN(return_id)=7 " + "AND return_id LIKE 'HT[0-9][0-9][0-9][0-9][0-9]'");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int returnCount(Connection con) throws SQLException {
        Object value = scalar(con, "SELECT COUNT(*) FROM return_requests");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public boolean returnIdExists(Connection con, String returnId) throws SQLException {
        Object value = scalar(con, "SELECT COUNT(*) FROM return_requests WHERE return_id=?", returnId);
        return value != null && ((Number) value).intValue() > 0;
    }

    public Map<String, Object> findReturnRequest(Connection con, String returnId) throws SQLException {
        return one(con, "SELECT return_id,invoice_id,refund_amount,status FROM return_requests WHERE return_id=?", returnId);
    }

    public Map<String, Object> findInvoiceRefundData(Connection con, String invoiceId)
            throws SQLException {
        return one(con, "SELECT customer_id,employee_id,total_amount FROM invoices WHERE invoice_id=?", invoiceId);
    }

    public List<Map<String, Object>> findReturnDetails(Connection con, String returnId)
            throws SQLException {
        return query(con, "SELECT product_id,quantity FROM return_details WHERE return_id=?", returnId);
    }

    public void restoreProductStock(Connection con, String productId, int quantity) throws SQLException {
        update(con, "UPDATE products SET stock_quantity=stock_quantity+?," +
                "status=CASE WHEN status='OUT_OF_STOCK' THEN 'ACTIVE' ELSE status END," + "updated_at=SYSDATETIME() WHERE product_id=?",
                quantity, productId);
    }

    public BigDecimal priorCompletedRefunds(
            Connection con, String invoiceId, String returnId) throws SQLException {
        Object value = scalar(con, "SELECT ISNULL(SUM(refund_amount),0) FROM return_requests " +
                "WHERE invoice_id=? AND status='COMPLETED' AND return_id<>?", invoiceId, returnId);
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    public void updateInvoiceStatus(Connection con, String invoiceId, String status) throws SQLException {
        update(con, "UPDATE invoices SET status=? WHERE invoice_id=?", status, invoiceId);
    }

    public Map<String, Object> findSuccessfulPayment(Connection con, String invoiceId)
            throws SQLException {
        return one(con, "SELECT TOP 1 payment_method FROM payments " +
                "WHERE invoice_id=? AND status='SUCCESS' ORDER BY paid_at", invoiceId);
    }

    public void insertRefundPayment(
            Connection con, String invoiceId, String method, BigDecimal amount, String reference) throws SQLException {
        update(con, "INSERT INTO payments(invoice_id,payment_method,amount,status,transaction_ref) " +
                "VALUES(?,?,?,'REFUNDED',?)", invoiceId, method, amount, reference);
    }

    public int earnedPoints(Connection con, String invoiceId) throws SQLException {
        Object value = scalar(con, "SELECT ISNULL(SUM(points_change),0) FROM point_transactions " +
                "WHERE invoice_id=? AND transaction_type='EARN'", invoiceId);
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int reversedPoints(Connection con, String invoiceId) throws SQLException {
        Object value = scalar(con, "SELECT ISNULL(-SUM(points_change),0) FROM point_transactions " +
                "WHERE invoice_id=? AND transaction_type='REVERSAL'", invoiceId);
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int currentCustomerPoints(Connection con, Object customerId) throws SQLException {
        Object value = scalar(con, "SELECT current_points FROM customers WHERE customer_id=?", customerId);
        return value == null ? 0 : ((Number) value).intValue();
    }

    public void insertPointReversal(
            Connection con, Object customerId, String invoiceId, int points) throws SQLException {
        update(con, "INSERT INTO point_transactions(" + "customer_id,invoice_id,transaction_type,points_change,note) " +
                "VALUES(?,?,'REVERSAL',?,N'Khấu trừ điểm do đổi trả/hoàn tiền')", customerId, invoiceId, -points);
    }

    public void subtractCustomerPoints(Connection con, Object customerId, int points)
            throws SQLException {
        update(con, "UPDATE customers SET current_points=current_points-?,updated_at=SYSDATETIME() " + "WHERE customer_id=?", points,
                customerId);
    }

    public void refreshCustomerTier(Connection con, Object customerId) throws SQLException {
        update(con, "UPDATE c SET tier_id=x.tier_id FROM customers c OUTER APPLY " +
                "(SELECT TOP 1 tier_id FROM membership_tiers " + " WHERE min_points<=c.current_points ORDER BY min_points DESC) x " +
                "WHERE c.customer_id=?", customerId);
    }

    public BigDecimal findBonusRate(
            Connection con, String invoiceId, Object employeeId, BigDecimal defaultRate) throws SQLException {
        Object value = scalar(con, "SELECT TOP 1 bonus_rate FROM sales_bonuses " +
                "WHERE invoice_id=? AND employee_id=? AND sales_amount>0 ORDER BY created_at", invoiceId, employeeId);
        return value == null ? defaultRate : new BigDecimal(String.valueOf(value));
    }

    public void insertBonusReversal(
            Connection con, Object employeeId, String invoiceId, BigDecimal refundAmount,
            BigDecimal rate, BigDecimal bonusAmount) throws SQLException {
        update(con, "INSERT INTO sales_bonuses(" +
                "employee_id,invoice_id,sales_amount,bonus_rate,bonus_amount,status) " + "VALUES(?,?,?,?,?,'OPEN')",
                employeeId, invoiceId, refundAmount.negate(), rate, bonusAmount.negate());
    }

    public void markReturnCompleted(Connection con, String returnId) throws SQLException {
        update(con, "UPDATE return_requests SET status='COMPLETED',completed_at=SYSDATETIME() WHERE return_id=?", returnId);
    }

    public Map<String, Object> findRefundApprovalRequest(Connection con, String returnId)
            throws SQLException {
        return one(con, "SELECT return_id,status,refund_amount FROM return_requests WHERE return_id=?", returnId);
    }

    public int decideRefundApproval(
            Connection con, String returnId, String decision, String employeeId, Object reason) throws SQLException {
        return update(con, "UPDATE refund_approvals SET decision=?,approver_employee_id=?,reason=?,decided_at=SYSDATETIME() " +
                "WHERE return_id=? AND decision='PENDING'", decision, employeeId, reason, returnId);
    }

    public void markReturnApproved(Connection con, String returnId) throws SQLException {
        update(con, "UPDATE return_requests SET status='APPROVED' WHERE return_id=?", returnId);
    }

    public void markReturnRejected(Connection con, String returnId, String reason) throws SQLException {
        update(con, "UPDATE return_requests SET status='REJECTED',rejection_reason=? WHERE return_id=?", reason, returnId);
    }

    public void insertAuditLog(
            Connection con, int accountId, String action, String entity, String entityId, String detail) throws SQLException {
        update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)", accountId, action,
                entity, entityId, detail);
    }
}

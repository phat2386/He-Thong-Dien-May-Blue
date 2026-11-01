package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import connectDB.ConnectDB;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Truy cập dữ liệu cho giao hàng, đổi trả và hoàn tiền.
 */
public class OperationDAO {

    public Map<String, Object> findAccountOperationState(int accountId)
            throws SQLException {
        return one("SELECT ac.status account_status,e.status employee_status,r.role_code " +
                "FROM accounts ac JOIN employees e ON e.employee_id=ac.employee_id " +
                "JOIN roles r ON r.role_id=ac.role_id WHERE ac.account_id=?", accountId);
    }

    public boolean hasCurrentShift(String employeeId) throws SQLException {
        return scalar("SELECT TOP 1 sa.assignment_id FROM shift_assignments sa " +
                "JOIN shifts s ON s.shift_id=sa.shift_id " + "WHERE sa.employee_id=? AND sa.work_date=CAST(GETDATE() AS DATE) " +
                "AND sa.status='ASSIGNED' AND s.active=1 " +
                "AND CAST(GETDATE() AS TIME)>=s.start_time AND CAST(GETDATE() AS TIME)<s.end_time", employeeId) != null;
    }

    public Map<String, Object> findInvoiceStatus(String invoiceId) throws SQLException {
        return one("SELECT invoice_id,status FROM invoices WHERE invoice_id=?", invoiceId);
    }

    public boolean deliveryExistsForInvoice(String invoiceId) throws SQLException {
        Object count = scalar("SELECT COUNT(*) FROM deliveries WHERE invoice_id=?", invoiceId);
        return count != null && ((Number) count).intValue() > 0;
    }

    public void insertDelivery(
            String deliveryId, String invoiceId, String address,
            String region, LocalDate date, LocalTime from, LocalTime to, boolean install, BigDecimal fee) throws SQLException {
        update("INSERT INTO deliveries(" + "delivery_id,invoice_id,assigned_employee_id,delivery_address,region_type," +
                "delivery_date,time_from,time_to,install_required,delivery_fee,status) " + "VALUES(?,?,NULL,?,?,?,?,?,?,?,'PREPARING')",
                deliveryId, invoiceId, address, region, date, from, to, install, fee);
    }

    public void insertDeliveryHistory(
            String deliveryId, Object oldStatus, String newStatus, String changedBy, Object note) throws SQLException {
        update("INSERT INTO delivery_history(delivery_id,old_status,new_status,changed_by,note) " + "VALUES(?,?,?,?,?)", deliveryId,
                oldStatus, newStatus, changedBy, note);
    }

    public int maxShortDeliveryNumber() throws SQLException {
        Object value = scalar("SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(delivery_id,3,5))),0) " +
                "FROM deliveries WHERE LEN(delivery_id)=7 " + "AND delivery_id LIKE 'GH[0-9][0-9][0-9][0-9][0-9]'");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int deliveryCount() throws SQLException {
        Object value = scalar("SELECT COUNT(*) FROM deliveries");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public boolean deliveryIdExists(String deliveryId) throws SQLException {
        Object value = scalar("SELECT COUNT(*) FROM deliveries WHERE delivery_id=?", deliveryId);
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

    public Map<String, Object> findDeliveryAssignment(String deliveryId)
            throws SQLException {
        return one("SELECT status,assigned_employee_id FROM deliveries WHERE delivery_id=?", deliveryId);
    }

    public boolean isEligibleDeliveryEmployee(
            String deliveryId, String employeeId) throws SQLException {
        Object count = scalar("SELECT COUNT(*) FROM deliveries d " +
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

    public int assignDelivery(String deliveryId, String employeeId) throws SQLException {
        return update("UPDATE deliveries SET assigned_employee_id=?,updated_at=SYSDATETIME() " +
                "WHERE delivery_id=? AND status='PREPARING'", employeeId, deliveryId);
    }

    public Map<String, Object> findDeliveryForUpdate(String deliveryId)
            throws SQLException {
        return one("SELECT status,install_required,assigned_employee_id,delivery_date,reschedule_date " +
                "FROM deliveries WHERE delivery_id=?", deliveryId);
    }

    public void updateDeliveryStatus(String deliveryId, String status)
            throws SQLException {
        update("UPDATE deliveries SET status=?,updated_at=SYSDATETIME() WHERE delivery_id=?", status, deliveryId);
    }

    public void completeDelivery(
            String deliveryId, String status, Object proofPath, Object signaturePath) throws SQLException {
        update("UPDATE deliveries SET status=?,proof_image_path=?,signature_path=?," +
                "failure_reason=NULL,updated_at=SYSDATETIME() WHERE delivery_id=?", status, proofPath, signaturePath, deliveryId);
    }

    public void failDelivery(
            String deliveryId, String reason, LocalDate rescheduleDate) throws SQLException {
        update("UPDATE deliveries SET status='FAILED',failure_reason=?,reschedule_date=?," +
                "updated_at=SYSDATETIME() WHERE delivery_id=?", reason, rescheduleDate, deliveryId);
    }

    public void retryDelivery(String deliveryId, LocalDate nextDate)
            throws SQLException {
        update("UPDATE deliveries SET status='PREPARING',delivery_date=?,assigned_employee_id=NULL," +
                "reschedule_date=NULL,updated_at=SYSDATETIME() WHERE delivery_id=?", nextDate, deliveryId);
    }

    public Map<String, Object> findInvoiceDetailForRefund(long invoiceDetailId) throws SQLException {
        return one( "SELECT quantity,line_total FROM invoice_details WHERE invoice_detail_id=?", invoiceDetailId);
    }

    public Map<String, Object> findInvoiceForReturn(String invoiceId)
            throws SQLException {
        return one("SELECT invoice_id,created_at,status,total_amount FROM invoices WHERE invoice_id=?", invoiceId);
    }

    public boolean returnExistsForInvoice(String invoiceId) throws SQLException {
        return scalar("SELECT TOP 1 return_id FROM return_requests WHERE invoice_id=?", invoiceId) != null;
    }

    public Map<String, Object> findInvoiceDetailForReturn(
            long invoiceDetailId, String invoiceId) throws SQLException {
        return one("SELECT product_id,quantity,line_total FROM invoice_details " +
                "WHERE invoice_detail_id=? AND invoice_id=?", invoiceDetailId, invoiceId);
    }

    public void insertReturnRequest(
            String returnId, String invoiceId, String createdBy,
            String reason, String condition, Object evidencePath, BigDecimal refundAmount, String status) throws SQLException {
        update("INSERT INTO return_requests(" +
                "return_id,invoice_id,created_by,reason,physical_condition,evidence_image_path,refund_amount,status) " +
                "VALUES(?,?,?,?,?,?,?,?)", returnId, invoiceId, createdBy, reason, condition, evidencePath, refundAmount, status);
    }

    public void insertReturnDetail(
            String returnId, long invoiceDetailId, String productId,
            int quantity, BigDecimal refundAmount, boolean sealIntact, boolean accessoriesComplete) throws SQLException {
        update("INSERT INTO return_details(" +
                "return_id,invoice_detail_id,product_id,quantity,refund_amount,seal_intact,accessories_complete) " +
                "VALUES(?,?,?,?,?,?,?)", returnId, invoiceDetailId, productId, quantity, refundAmount, sealIntact, accessoriesComplete);
    }

    public void insertPendingRefundApproval(String returnId) throws SQLException {
        update("INSERT INTO refund_approvals(return_id,decision) VALUES(?,'PENDING')", returnId);
    }

    public int maxShortReturnNumber() throws SQLException {
        Object value = scalar("SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(return_id,3,5))),0) " +
                "FROM return_requests WHERE LEN(return_id)=7 " + "AND return_id LIKE 'HT[0-9][0-9][0-9][0-9][0-9]'");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int returnCount() throws SQLException {
        Object value = scalar("SELECT COUNT(*) FROM return_requests");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public boolean returnIdExists(String returnId) throws SQLException {
        Object value = scalar("SELECT COUNT(*) FROM return_requests WHERE return_id=?", returnId);
        return value != null && ((Number) value).intValue() > 0;
    }

    public Map<String, Object> findReturnRequest(String returnId) throws SQLException {
        return one("SELECT return_id,invoice_id,refund_amount,status FROM return_requests WHERE return_id=?", returnId);
    }

    public Map<String, Object> findInvoiceRefundData(String invoiceId)
            throws SQLException {
        return one("SELECT customer_id,employee_id,total_amount FROM invoices WHERE invoice_id=?", invoiceId);
    }

    public List<Map<String, Object>> findReturnDetails(String returnId)
            throws SQLException {
        return query("SELECT product_id,quantity FROM return_details WHERE return_id=?", returnId);
    }

    public void restoreProductStock(String productId, int quantity) throws SQLException {
        update("UPDATE products SET stock_quantity=stock_quantity+?," +
                "status=CASE WHEN status='OUT_OF_STOCK' THEN 'ACTIVE' ELSE status END," + "updated_at=SYSDATETIME() WHERE product_id=?",
                quantity, productId);
    }

    public BigDecimal priorCompletedRefunds(
            String invoiceId, String returnId) throws SQLException {
        Object value = scalar("SELECT ISNULL(SUM(refund_amount),0) FROM return_requests " +
                "WHERE invoice_id=? AND status='COMPLETED' AND return_id<>?", invoiceId, returnId);
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }

    public void updateInvoiceStatus(String invoiceId, String status) throws SQLException {
        update("UPDATE invoices SET status=? WHERE invoice_id=?", status, invoiceId);
    }

    public Map<String, Object> findSuccessfulPayment(String invoiceId)
            throws SQLException {
        return one("SELECT TOP 1 payment_method FROM payments " +
                "WHERE invoice_id=? AND status='SUCCESS' ORDER BY paid_at", invoiceId);
    }

    public void insertRefundPayment(
            String invoiceId, String method, BigDecimal amount, String reference) throws SQLException {
        update("INSERT INTO payments(invoice_id,payment_method,amount,status,transaction_ref) " +
                "VALUES(?,?,?,'REFUNDED',?)", invoiceId, method, amount, reference);
    }

    public int earnedPoints(String invoiceId) throws SQLException {
        Object value = scalar("SELECT ISNULL(SUM(points_change),0) FROM point_transactions " +
                "WHERE invoice_id=? AND transaction_type='EARN'", invoiceId);
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int reversedPoints(String invoiceId) throws SQLException {
        Object value = scalar("SELECT ISNULL(-SUM(points_change),0) FROM point_transactions " +
                "WHERE invoice_id=? AND transaction_type='REVERSAL'", invoiceId);
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int currentCustomerPoints(Object customerId) throws SQLException {
        Object value = scalar("SELECT current_points FROM customers WHERE customer_id=?", customerId);
        return value == null ? 0 : ((Number) value).intValue();
    }

    public void insertPointReversal(
            Object customerId, String invoiceId, int points) throws SQLException {
        update("INSERT INTO point_transactions(" + "customer_id,invoice_id,transaction_type,points_change,note) " +
                "VALUES(?,?,'REVERSAL',?,N'Khấu trừ điểm do đổi trả/hoàn tiền')", customerId, invoiceId, -points);
    }

    public void subtractCustomerPoints(Object customerId, int points)
            throws SQLException {
        update("UPDATE customers SET current_points=current_points-?,updated_at=SYSDATETIME() " + "WHERE customer_id=?", points,
                customerId);
    }

    public void refreshCustomerTier(Object customerId) throws SQLException {
        update("UPDATE c SET tier_id=x.tier_id FROM customers c OUTER APPLY " +
                "(SELECT TOP 1 tier_id FROM membership_tiers " + " WHERE min_points<=c.current_points ORDER BY min_points DESC) x " +
                "WHERE c.customer_id=?", customerId);
    }

    public BigDecimal findBonusRate(
            String invoiceId, Object employeeId, BigDecimal defaultRate) throws SQLException {
        Object value = scalar("SELECT TOP 1 bonus_rate FROM sales_bonuses " +
                "WHERE invoice_id=? AND employee_id=? AND sales_amount>0 ORDER BY created_at", invoiceId, employeeId);
        return value == null ? defaultRate : new BigDecimal(String.valueOf(value));
    }

    public void insertBonusReversal(
            Object employeeId, String invoiceId, BigDecimal refundAmount,
            BigDecimal rate, BigDecimal bonusAmount) throws SQLException {
        update("INSERT INTO sales_bonuses(" +
                "employee_id,invoice_id,sales_amount,bonus_rate,bonus_amount,status) " + "VALUES(?,?,?,?,?,'OPEN')",
                employeeId, invoiceId, refundAmount.negate(), rate, bonusAmount.negate());
    }

    public void markReturnCompleted(String returnId) throws SQLException {
        update("UPDATE return_requests SET status='COMPLETED',completed_at=SYSDATETIME() WHERE return_id=?", returnId);
    }

    public Map<String, Object> findRefundApprovalRequest(String returnId)
            throws SQLException {
        return one("SELECT return_id,status,refund_amount FROM return_requests WHERE return_id=?", returnId);
    }

    public int decideRefundApproval(
            String returnId, String decision, String employeeId, Object reason) throws SQLException {
        return update("UPDATE refund_approvals SET decision=?,approver_employee_id=?,reason=?,decided_at=SYSDATETIME() " +
                "WHERE return_id=? AND decision='PENDING'", decision, employeeId, reason, returnId);
    }

    public void markReturnApproved(String returnId) throws SQLException {
        update("UPDATE return_requests SET status='APPROVED' WHERE return_id=?", returnId);
    }

    public void markReturnRejected(String returnId, String reason) throws SQLException {
        update("UPDATE return_requests SET status='REJECTED',rejection_reason=? WHERE return_id=?", reason, returnId);
    }

    public void insertAuditLog(
            int accountId, String action, String entity, String entityId, String detail) throws SQLException {
        update("INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)", accountId, action,
                entity, entityId, detail);
    }

    public List<Map<String, Object>> query(String sql, Object... thamSo) throws SQLException {
        List<Map<String, Object>> ketQua = new ArrayList<>();
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            guiThamSo(ps, thamSo);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int soCot = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> dong = new LinkedHashMap<>();
                    for (int i = 1; i <= soCot; i++) {
                        dong.put(md.getColumnLabel(i), rs.getObject(i));
                    }
                    ketQua.add(dong);
                }
            }
        }
        return ketQua;
    }

    public Map<String, Object> one(String sql, Object... thamSo) throws SQLException {
        List<Map<String, Object>> ds = query(sql, thamSo);
        if (ds.isEmpty()) {
            return null;
        }
        return ds.get(0);
    }

    public Object scalar(String sql, Object... thamSo) throws SQLException {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            guiThamSo(ps, thamSo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1);
                }
                return null;
            }
        }
    }

    public int update(String sql, Object... thamSo) throws SQLException {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            guiThamSo(ps, thamSo);
            return ps.executeUpdate();
        }
    }

    private void guiThamSo(PreparedStatement ps, Object[] thamSo) throws SQLException {
        for (int i = 0; i < thamSo.length; i++) {
            ps.setObject(i + 1, thamSo[i]);
        }
    }
}

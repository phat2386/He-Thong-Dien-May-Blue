package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Truy cập dữ liệu cho khách hàng, thành viên, bảo hành và khiếu nại.
 */
public class CustomerDAO extends BaseDAO {

    public List<Map<String, Object>> searchCustomers(String keyword, String tierCode)
            throws SQLException {
        String sql = "SELECT c.customer_id,c.full_name,c.phone,c.email,mt.tier_code,c.current_points,c.status," +
                "ISNULL(sp.spent,0) spent," + "(SELECT TOP 1 card_code FROM membership_cards mc " +
                " WHERE mc.customer_id=c.customer_id AND mc.status='ACTIVE' ORDER BY issued_at DESC) card " + "FROM customers c " +
                "LEFT JOIN membership_tiers mt ON mt.tier_id=c.tier_id " + "OUTER APPLY (" +
                " SELECT SUM(i.total_amount-ISNULL(r.refunded,0)) spent FROM invoices i " +
                " LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests " +
                "            WHERE status='COMPLETED' GROUP BY invoice_id) r ON r.invoice_id=i.invoice_id " +
                " WHERE i.customer_id=c.customer_id AND i.status IN ('PAID','PARTIAL_REFUND','REFUNDED')" + ") sp " +
                "WHERE (c.customer_id LIKE ? OR c.full_name LIKE ? OR ISNULL(c.phone,'') LIKE ?)";
        if (tierCode == null) {
            return query(sql + " ORDER BY c.full_name", keyword, keyword, keyword);
        }
        return query( sql + " AND mt.tier_code=? ORDER BY c.full_name", keyword, keyword, keyword, tierCode);
    }

    public Map<String, Object> findCustomer(String customerId) throws SQLException {
        return one("SELECT * FROM customers WHERE customer_id=?", customerId);
    }

    public int maxShortCustomerNumber() throws SQLException {
        Object value = scalar(
                "SELECT ISNULL(MAX(TRY_CONVERT(int,SUBSTRING(customer_id,3,10))),0) " +
                "FROM customers WHERE customer_id LIKE 'KH%' AND LEN(customer_id)=5 " +
                "AND TRY_CONVERT(int,SUBSTRING(customer_id,3,10)) IS NOT NULL");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public boolean customerExists(String customerId) throws SQLException {
        Object value = scalar("SELECT COUNT(*) FROM customers WHERE customer_id=?", customerId);
        return value != null && ((Number) value).intValue() > 0;
    }

    public Object standardTierId() throws SQLException {
        return scalar("SELECT tier_id FROM membership_tiers WHERE tier_code='STANDARD'");
    }

    public void insertCustomer(
            String customerId, String fullName, Object phone, Object email, Object address, Object tierId) throws SQLException {
        update(
                "INSERT INTO customers(" + "customer_id,full_name,phone,email,address,tier_id,current_points,status) " +
                "VALUES(?,?,?,?,?,?,0,'ACTIVE')", customerId, fullName, phone, email, address, tierId);
    }

    public void updateCustomer(
            String customerId, String fullName, Object phone, Object email, Object address) throws SQLException {
        update(
                "UPDATE customers SET full_name=?,phone=?,email=?,address=?,updated_at=SYSDATETIME() " + "WHERE customer_id=?",
                fullName, phone, email, address, customerId);
    }

    public List<Map<String, Object>> findMembershipCustomers(String keyword) throws SQLException {
        return query(
                "SELECT c.customer_id,c.full_name,c.phone,mt.tier_name,c.current_points," +
                "(SELECT TOP 1 mc.card_code FROM membership_cards mc " +
                " WHERE mc.customer_id=c.customer_id AND mc.status='ACTIVE' ORDER BY mc.issued_at DESC) card_code " +
                "FROM customers c LEFT JOIN membership_tiers mt ON mt.tier_id=c.tier_id " + "WHERE c.status='ACTIVE' " +
                "AND (c.customer_id LIKE ? OR c.full_name LIKE ? OR ISNULL(c.phone,'') LIKE ?) " + "ORDER BY c.full_name",
                keyword, keyword, keyword);
    }

    public List<Map<String, Object>> findMembershipTiers() throws SQLException {
        return query(
                "SELECT tier_id,tier_code,tier_name,min_points,discount_rate,point_multiplier " +
                "FROM membership_tiers ORDER BY min_points");
    }

    public List<Map<String, Object>> findWarranties() throws SQLException {
        return query(
                "SELECT w.warranty_id,c.full_name,p.product_name,w.received_at,w.warranty_end_date," +
                "w.issue_description,w.status,w.replacement_charge " +
                "FROM warranties w JOIN customers c ON c.customer_id=w.customer_id " +
                "JOIN products p ON p.product_id=w.product_id ORDER BY w.received_at DESC");
    }

    public List<Map<String, Object>> findComplaints() throws SQLException {
        return query(
                "SELECT cp.complaint_id,COALESCE(c.full_name,cp.customer_phone,N'Không rõ') customer," +
                "cp.content,cp.priority,cp.status,e.full_name assigned_name,cp.due_at,cp.escalated " +
                "FROM complaints cp LEFT JOIN customers c ON c.customer_id=cp.customer_id " +
                "LEFT JOIN employees e ON e.employee_id=cp.assigned_to ORDER BY cp.created_at DESC");
    }

    public List<Map<String, Object>> findWarrantyOptions(String phone) throws SQLException {
        return query(
                "SELECT c.customer_id,c.full_name,d.invoice_detail_id,p.product_name,i.created_at," +
                "p.warranty_months,DATEADD(MONTH,p.warranty_months,CAST(i.created_at AS DATE)) warranty_end_date," +
                "d.quantity-COALESCE(ret.returned_qty,0) remaining_qty " +
                "FROM customers c JOIN invoices i ON i.customer_id=c.customer_id " +
                "JOIN invoice_details d ON d.invoice_id=i.invoice_id " + "JOIN products p ON p.product_id=d.product_id " +
                "LEFT JOIN (SELECT rd.invoice_detail_id,SUM(rd.quantity) returned_qty " +
                "           FROM return_details rd JOIN return_requests rr ON rr.return_id=rd.return_id " +
                "           WHERE rr.status='COMPLETED' GROUP BY rd.invoice_detail_id) ret " +
                "ON ret.invoice_detail_id=d.invoice_detail_id " + "WHERE c.phone=? AND i.status IN ('PAID','PARTIAL_REFUND') " +
                "AND d.quantity-COALESCE(ret.returned_qty,0)>0 " +
                "AND DATEADD(MONTH,p.warranty_months,CAST(i.created_at AS DATE))>=CAST(GETDATE() AS DATE) " +
                "ORDER BY i.created_at DESC", phone);
    }

    public List<Map<String, Object>> findCustomerIdsByPhone(String phone) throws SQLException {
        return query("SELECT customer_id FROM customers WHERE phone=?", phone);
    }

    public Map<String, Object> findCustomerStatus(Connection con, String customerId) throws SQLException {
        return one(con, "SELECT customer_id,status FROM customers WHERE customer_id=?", customerId);
    }

    public void invalidateActiveMembershipCards(Connection con, String customerId) throws SQLException {
        update(con, "UPDATE membership_cards SET status='INVALIDATED',invalidated_at=SYSDATETIME() " +
                "WHERE customer_id=? AND status='ACTIVE'", customerId);
    }

    public List<Map<String, Object>> findMembershipCardCodes(Connection con, String customerId)
            throws SQLException {
        return query(con, "SELECT card_code FROM membership_cards WHERE customer_id=?", customerId);
    }

    public void insertMembershipCard(Connection con, String customerId, String cardCode)
            throws SQLException {
        update(con, "INSERT INTO membership_cards(customer_id,card_code,status) VALUES(?,?,'ACTIVE')", customerId, cardCode);
    }

    public Map<String, Object> findTier(Connection con, int tierId) throws SQLException {
        return one(con, "SELECT tier_code,tier_name FROM membership_tiers WHERE tier_id=?", tierId);
    }

    public boolean tierThresholdUsedByAnother(Connection con, int tierId, int minPoints)
            throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM membership_tiers WHERE tier_id<>? AND min_points=?", tierId, minPoints);
        return count != null && ((Number) count).intValue() > 0;
    }

    public List<Map<String, Object>> findTierThresholds(Connection con) throws SQLException {
        return query(con, "SELECT tier_id,tier_code,min_points FROM membership_tiers");
    }

    public void updateTierPolicy(
            Connection con, int tierId, int minPoints, BigDecimal discountRate, BigDecimal pointMultiplier) throws SQLException {
        update(con, "UPDATE membership_tiers SET min_points=?,discount_rate=?,point_multiplier=? WHERE tier_id=?",
                minPoints, discountRate, pointMultiplier, tierId);
    }

    public void recalculateAllCustomerTiers(Connection con) throws SQLException {
        update(con, "UPDATE c SET tier_id=x.tier_id,updated_at=SYSDATETIME() " +
                "FROM customers c CROSS APPLY (" + " SELECT TOP 1 tier_id FROM membership_tiers " +
                " WHERE min_points<=c.current_points ORDER BY min_points DESC) x");
    }

    public Map<String, Object> findWarrantyPurchase(Connection con, long invoiceDetailId)
            throws SQLException {
        return one(con, "SELECT d.product_id,d.quantity,i.customer_id,i.created_at,i.status,p.warranty_months," +
                "d.quantity-COALESCE((SELECT SUM(rd.quantity) FROM return_details rd " +
                "JOIN return_requests rr ON rr.return_id=rd.return_id " +
                "WHERE rd.invoice_detail_id=d.invoice_detail_id AND rr.status='COMPLETED'),0) remaining_qty " +
                "FROM invoice_details d JOIN invoices i ON i.invoice_id=d.invoice_id " +
                "JOIN products p ON p.product_id=d.product_id WHERE d.invoice_detail_id=?", invoiceDetailId);
    }

    public int activeWarrantyCount(Connection con, long invoiceDetailId) throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM warranties WHERE invoice_detail_id=? " +
                "AND status IN ('REPAIRING','COMPLETED','REPLACEMENT')", invoiceDetailId);
        return count == null ? 0 : ((Number) count).intValue();
    }

    public void insertWarranty(
            Connection con, String warrantyId, String customerId, long invoiceDetailId,
            Object productId, String issue, LocalDate warrantyEnd, String createdBy) throws SQLException {
        update(con, "INSERT INTO warranties(" +
                "warranty_id,customer_id,invoice_detail_id,product_id,issue_description,warranty_end_date,status,created_by) " +
                "VALUES(?,?,?,?,?,?,'REPAIRING',?)", warrantyId, customerId, invoiceDetailId, productId, issue, warrantyEnd, createdBy);
    }

    public Map<String, Object> findWarrantyChargeData(String warrantyId) throws SQLException {
        return one(
                "SELECT d.unit_price,i.created_at,w.status FROM warranties w " +
                "JOIN invoice_details d ON d.invoice_detail_id=w.invoice_detail_id " +
                "JOIN invoices i ON i.invoice_id=d.invoice_id WHERE w.warranty_id=?", warrantyId);
    }

    public Map<String, Object> findWarrantyStatus(Connection con, String warrantyId) throws SQLException {
        return one(con, "SELECT status FROM warranties WHERE warranty_id=?", warrantyId);
    }

    public Map<String, Object> findWarrantyChargeData(Connection con, String warrantyId)
            throws SQLException {
        return one(con, "SELECT d.unit_price,i.created_at FROM warranties w " +
                "JOIN invoice_details d ON d.invoice_detail_id=w.invoice_detail_id " +
                "JOIN invoices i ON i.invoice_id=d.invoice_id WHERE w.warranty_id=?", warrantyId);
    }

    public void updateWarrantyState(
            Connection con, String warrantyId, String status, BigDecimal replacementCharge) throws SQLException {
        update(con, "UPDATE warranties SET status=?," +
                "replacement_charge=CASE WHEN ?='REPLACEMENT' THEN ? ELSE replacement_charge END," +
                "completed_at=CASE WHEN ?='COMPLETED' THEN SYSDATETIME() ELSE completed_at END," +
                "returned_at=CASE WHEN ?='RETURNED' THEN SYSDATETIME() ELSE returned_at END " + "WHERE warranty_id=?", status, status,
                replacementCharge, status, status, warrantyId);
    }

    public void insertWarrantyHistory(
            Connection con, String warrantyId, Object oldStatus, String newStatus, String changedBy, Object note) throws SQLException {
        update(con, "INSERT INTO warranty_history(warranty_id,old_status,new_status,changed_by,note) " + "VALUES(?,?,?,?,?)", warrantyId,
                oldStatus, newStatus, changedBy, note);
    }

    public void insertComplaint(
            Connection con, String complaintId, Object customerId, String phone,
            String content, String priority, String employeeId) throws SQLException {
        update(con, "INSERT INTO complaints(" +
                "complaint_id,customer_id,customer_phone,content,priority,status,created_by,assigned_to) " +
                "VALUES(?,?,?,?,?,'NEW',?,?)", complaintId, customerId, phone, content, priority, employeeId, employeeId);
    }

    public void insertComplaintHistory(
            Connection con, String complaintId, Object oldStatus, String newStatus, String changedBy, Object note) throws SQLException {
        update(con, "INSERT INTO complaint_history(complaint_id,old_status,new_status,changed_by,note) " +
                "VALUES(?,?,?,?,?)", complaintId, oldStatus, newStatus, changedBy, note);
    }

    public List<Map<String, Object>> findOverdueComplaints(Connection con) throws SQLException {
        return query(con, "SELECT complaint_id,customer_phone,content FROM complaints WITH (UPDLOCK,ROWLOCK) " +
                "WHERE escalated=0 AND due_at<SYSDATETIME() AND status NOT IN ('RESOLVED','CLOSED')");
    }

    public void markComplaintEscalated(Connection con, String complaintId) throws SQLException {
        update(con, "UPDATE complaints SET escalated=1,updated_at=SYSDATETIME() " + "WHERE complaint_id=? AND escalated=0", complaintId);
    }

    public List<Map<String, Object>> findActiveManagers(Connection con) throws SQLException {
        return query(con, "SELECT DISTINCT e.employee_id FROM accounts a " +
                "JOIN employees e ON e.employee_id=a.employee_id " + "JOIN roles r ON r.role_id=a.role_id " +
                "WHERE r.role_code='MANAGER' AND a.status='ACTIVE' AND e.status='ACTIVE'");
    }

    public void insertNotification(Connection con, Object employeeId, String title, String content)
            throws SQLException {
        update(con, "INSERT INTO notifications(employee_id,title,content) VALUES(?,?,?)", employeeId, title, content);
    }

    public Map<String, Object> findComplaintStatus(Connection con, String complaintId)
            throws SQLException {
        return one(con, "SELECT status,assigned_to FROM complaints WHERE complaint_id=?", complaintId);
    }

    public void updateComplaintStatus(
            Connection con, String complaintId, String status, String assignedTo) throws SQLException {
        update(con, "UPDATE complaints SET status=?,assigned_to=COALESCE(assigned_to,?),updated_at=SYSDATETIME()," +
                "resolved_at=CASE WHEN ? IN ('RESOLVED','CLOSED') " + "THEN COALESCE(resolved_at,SYSDATETIME()) ELSE resolved_at END " +
                "WHERE complaint_id=?", status, assignedTo, status, complaintId);
    }

    public void insertAuditLog(
            Connection con, Integer accountId, String action, String entity, String entityId, String detail) throws SQLException {
        update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)", accountId, action,
                entity, entityId, detail);
    }
}

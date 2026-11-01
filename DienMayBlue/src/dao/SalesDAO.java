package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import connectDB.ConnectDB;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Truy cập dữ liệu cho bán hàng, hóa đơn, thanh toán, điểm thành viên và thưởng doanh số.
 */
public class SalesDAO {

// ===== Kiểm tra quyền bán: trạng thái tài khoản và ca làm việc =====
    public Map<String, Object> findAccountSaleState(int accountId) throws SQLException {
        return one("SELECT ac.status account_status,e.status employee_status,r.role_code " +
                "FROM accounts ac JOIN employees e ON e.employee_id=ac.employee_id " +
                "JOIN roles r ON r.role_id=ac.role_id WHERE ac.account_id=?", accountId);
    }

    public boolean hasCurrentShift(String employeeId) throws SQLException {
        Object id = scalar("SELECT TOP 1 sa.assignment_id FROM shift_assignments sa " +
                "JOIN shifts s ON s.shift_id=sa.shift_id " + "WHERE sa.employee_id=? AND sa.work_date=CAST(GETDATE() AS DATE) " +
                "AND sa.status='ASSIGNED' AND s.active=1 " +
                "AND CAST(GETDATE() AS TIME)>=s.start_time AND CAST(GETDATE() AS TIME)<s.end_time", employeeId);
        return id != null;
    }

// ===== Tra cứu khách hàng, sản phẩm, khuyến mãi =====
    public Map<String, Object> findCustomerMembership(String customerId)
            throws SQLException {
        return one("SELECT c.customer_id,c.status,ISNULL(t.discount_rate,0) discount_rate," +
                "ISNULL(t.point_multiplier,1) point_multiplier " +
                "FROM customers c LEFT JOIN membership_tiers t ON t.tier_id=c.tier_id " + "WHERE c.customer_id=?", customerId);
    }

    public Map<String, Object> findProductForInvoice(String productId)
            throws SQLException {
        return one("SELECT product_id,sale_price,cost_price,stock_quantity,category_id,status " +
                "FROM products WHERE product_id=?", productId);
    }

// ===== Sinh mã hóa đơn HD00001 =====
    public int maxShortInvoiceNumber() throws SQLException {
        Object value = scalar("SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(invoice_id,3,5))),0) " +
                "FROM invoices WHERE LEN(invoice_id)=7 " + "AND invoice_id LIKE 'HD[0-9][0-9][0-9][0-9][0-9]'");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public int invoiceCount() throws SQLException {
        Object value = scalar("SELECT COUNT(*) FROM invoices");
        return value == null ? 0 : ((Number) value).intValue();
    }

    public boolean invoiceIdExists(String invoiceId) throws SQLException {
        Object value = scalar("SELECT COUNT(*) FROM invoices WHERE invoice_id=?", invoiceId);
        return value != null && ((Number) value).intValue() > 0;
    }

    public List<Map<String, Object>> findActivePromotions(
            String productId, int categoryId) throws SQLException {
        return query("SELECT DISTINCT p.discount_type,p.discount_value " +
                "FROM promotions p " + "LEFT JOIN promotion_products pp ON pp.promotion_id=p.promotion_id " +
                "LEFT JOIN promotion_categories pc ON pc.promotion_id=p.promotion_id " +
                "WHERE p.status='ACTIVE' AND SYSDATETIME() BETWEEN p.start_at AND p.end_at " +
                "AND (pp.product_id=? OR pc.category_id=?)", productId, categoryId);
    }

// ===== Ghi hóa đơn, trừ tồn kho, chi tiết hóa đơn, thanh toán =====
    public void insertInvoice(
            String invoiceId, Object customerId, String employeeId,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total, Object note) throws SQLException {
        update("INSERT INTO invoices(" +
                "invoice_id,customer_id,employee_id,subtotal,discount_amount,total_amount,status,note) " + "VALUES(?,?,?,?,?,?,'PAID',?)",
                invoiceId, customerId, employeeId, subtotal, discount, total, note);
    }

    public int decreaseProductStock(String productId, int quantity) throws SQLException {
        return update("UPDATE products SET stock_quantity=stock_quantity-?, " +
                "status=CASE WHEN stock_quantity-?=0 THEN 'OUT_OF_STOCK' ELSE status END, " + "updated_at=SYSDATETIME() " +
                "WHERE product_id=? AND status='ACTIVE' AND stock_quantity>=?", quantity, quantity, productId, quantity);
    }

    public void insertInvoiceDetail(
            String invoiceId, String productId, int quantity,
            BigDecimal unitPrice, BigDecimal costPrice, BigDecimal discount, BigDecimal lineTotal) throws SQLException {
        update("INSERT INTO invoice_details(" +
                "invoice_id,product_id,quantity,unit_price,cost_price_snapshot,discount_amount,line_total) " + "VALUES(?,?,?,?,?,?,?)",
                invoiceId, productId, quantity, unitPrice, costPrice, discount, lineTotal);
    }

    public void insertPayment(
            String invoiceId, String method, BigDecimal amount, Object transactionRef) throws SQLException {
        update("INSERT INTO payments(invoice_id,payment_method,amount,status,transaction_ref) " +
                "VALUES(?,?,?,'SUCCESS',?)", invoiceId, method, amount, transactionRef);
    }

// ===== Tích điểm và cập nhật hạng khách hàng =====
    public void insertEarnedPoints(
            String customerId, String invoiceId, int points) throws SQLException {
        update("INSERT INTO point_transactions(" + "customer_id,invoice_id,transaction_type,points_change,note) " +
                "VALUES(?,?,'EARN',?,N'Tích điểm từ hóa đơn đã thanh toán')", customerId, invoiceId, points);
    }

    public void addCustomerPoints(String customerId, int points) throws SQLException {
        update("UPDATE customers SET current_points=current_points+?,updated_at=SYSDATETIME() " + "WHERE customer_id=?", points,
                customerId);
    }

    public void refreshCustomerTier(String customerId) throws SQLException {
        update("UPDATE c SET tier_id=x.tier_id FROM customers c " + "CROSS APPLY (SELECT TOP 1 tier_id FROM membership_tiers " +
                "WHERE min_points<=c.current_points ORDER BY min_points DESC) x " + "WHERE c.customer_id=?", customerId);
    }

// ===== Thưởng bán hàng, thông báo, nhật ký hệ thống =====
    public void insertSalesBonus(
            String employeeId, String invoiceId, BigDecimal salesAmount,
            BigDecimal bonusRate, BigDecimal bonusAmount) throws SQLException {
        update("INSERT INTO sales_bonuses(" +
                "employee_id,invoice_id,sales_amount,bonus_rate,bonus_amount,status) " + "VALUES(?,?,?,?,?,'OPEN')",
                employeeId, invoiceId, salesAmount, bonusRate, bonusAmount);
    }

    public int overlappingFinalizedStatementCount(LocalDate from, LocalDate to)
            throws SQLException {
        Object value = scalar("SELECT COUNT(*) FROM bonus_statements " +
                "WHERE status='FINALIZED' AND period_start<=? AND period_end>=?", to, from);
        return value == null ? 0 : ((Number) value).intValue();
    }

    public List<Map<String, Object>> openBonusesByEmployee(
            LocalDate from, LocalDate to) throws SQLException {
        return query("SELECT employee_id,SUM(sales_amount) total_sales,SUM(bonus_amount) total_bonus " +
                "FROM sales_bonuses WHERE CAST(created_at AS DATE) BETWEEN ? AND ? AND status='OPEN' " + "GROUP BY employee_id",
                from, to);
    }

    public long insertBonusStatement(
            LocalDate from, LocalDate to, String finalizedBy) throws SQLException {
        return insertAndKey("INSERT INTO bonus_statements(period_start,period_end,status,finalized_by,finalized_at) " +
                "VALUES(?,?,'FINALIZED',?,SYSDATETIME())", from, to, finalizedBy);
    }

    public void insertBonusStatementDetail(
            long statementId, Object employeeId, Object totalSales, Object totalBonus) throws SQLException {
        update("INSERT INTO bonus_statement_details(statement_id,employee_id,total_sales,total_bonus) " +
                "VALUES(?,?,?,?)", statementId, employeeId, totalSales, totalBonus);
    }

    public void markBonusesFinalized(LocalDate from, LocalDate to) throws SQLException {
        update("UPDATE sales_bonuses SET status='FINALIZED' " +
                "WHERE CAST(created_at AS DATE) BETWEEN ? AND ? AND status='OPEN'", from, to);
    }

    public void insertNotification(Object employeeId, String title, String content)
            throws SQLException {
        update("INSERT INTO notifications(employee_id,title,content) VALUES(?,?,?)", employeeId, title, content);
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

    public long insertAndKey(String sql, Object... thamSo) throws SQLException {
        try (Connection con = ConnectDB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            guiThamSo(ps, thamSo);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được mã vừa thêm.");
    }

    private void guiThamSo(PreparedStatement ps, Object[] thamSo) throws SQLException {
        for (int i = 0; i < thamSo.length; i++) {
            ps.setObject(i + 1, thamSo[i]);
        }
    }
}

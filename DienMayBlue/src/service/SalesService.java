package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import dao.SalesDAO;
import database.Db;
import entity.Account;

/**
 * Nghiệp vụ bán hàng / lập hóa đơn (UC019) và cầu nối đơn tạm từ màn tìm sản phẩm (UC018).
 */
public class SalesService extends BaseService<SalesDAO> {

    public SalesService() {
        super(new SalesDAO());
    }

    public static final BigDecimal POINT_VND_PER_POINT = new BigDecimal("10000");
    public static final BigDecimal DEFAULT_BONUS_RATE = new BigDecimal("0.03");

    public static class InvoiceItem {
        public final String productId;
        public final int quantity;
        public InvoiceItem(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }


    /**
     * Kiểm tra điều kiện tối thiểu để thực hiện UC019.
     * Quyền INVOICE_CREATE do Admin cấp quyết định khả năng truy cập.
     * Riêng tài khoản có role SALES vẫn phải có ca ASSIGNED đúng ngày/giờ.
     */
    public void ensureCanSell() throws Exception {
        final Account a = requirePermission("INVOICE_CREATE");
        try (Connection con = database.DatabaseConnection.getConnection()) {
            ensureCanSell(con, a);
        }
    }

    private void ensureCanSell(Connection con, Account a) throws Exception {
        Map<String,Object> user = Db.one(con,
                "SELECT ac.status account_status,e.status employee_status,r.role_code "
              + "FROM accounts ac JOIN employees e ON e.employee_id=ac.employee_id "
              + "JOIN roles r ON r.role_id=ac.role_id WHERE ac.account_id=?",
                a.getAccountId());
        if (user == null) throw new IllegalStateException("Tài khoản không còn tồn tại trong hệ thống.");
        if (!"ACTIVE".equals(String.valueOf(user.get("account_status"))))
            throw new IllegalStateException("Tài khoản hiện không ở trạng thái hoạt động.");
        if (!"ACTIVE".equals(String.valueOf(user.get("employee_status"))))
            throw new IllegalStateException("Nhân viên hiện không ở trạng thái làm việc.");
        String roleCode = String.valueOf(user.get("role_code"));
        if (!"SALES".equals(roleCode)) {
            // Chức năng hiện được Admin phân quyền linh hoạt cho mọi actor.
            // Chỉ nhân viên SALES phải tuân thủ điều kiện ca làm việc; các actor
            // khác được cấp quyền (đặc biệt ADMIN) có thể dùng để quản lý/kiểm thử.
            return;
        }

        Object shift = Db.scalar(con,
                "SELECT TOP 1 sa.assignment_id FROM shift_assignments sa "
              + "JOIN shifts s ON s.shift_id=sa.shift_id "
              + "WHERE sa.employee_id=? AND sa.work_date=CAST(GETDATE() AS DATE) "
              + "AND sa.status='ASSIGNED' AND s.active=1 "
              + "AND CAST(GETDATE() AS TIME)>=s.start_time AND CAST(GETDATE() AS TIME)<s.end_time",
                a.getEmployee().getEmployeeId());
        if (shift == null)
            throw new IllegalStateException("Bạn chưa có ca làm việc hợp lệ tại thời điểm hiện tại.");
    }
    public String createInvoice(final String customerId,
                                final List<InvoiceItem> items,
                                final String paymentMethod,
                                final String transactionRef,
                                final String note) throws Exception {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Hóa đơn chưa có sản phẩm.");

        final Account a = requirePermission("INVOICE_CREATE");

        final String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if ("INSTALLMENT".equals(method))
            throw new IllegalArgumentException("Đơn trả góp phải được xét duyệt trước; chưa thể ghi nhận là đã thanh toán tại UC019.");
        if (!Arrays.asList("CASH", "CARD", "BANK_TRANSFER").contains(method))
            throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ.");
        if (("CARD".equals(method) || "BANK_TRANSFER".equals(method))
                && (transactionRef == null || transactionRef.trim().isEmpty()))
            throw new IllegalArgumentException("Cần nhập mã giao dịch cho thanh toán thẻ/chuyển khoản.");

        // Gộp sản phẩm trùng mã để mỗi sản phẩm chỉ có một dòng chi tiết.
        final LinkedHashMap<String,Integer> quantities = new LinkedHashMap<String,Integer>();
        for (InvoiceItem item : items) {
            if (item == null || item.productId == null || item.productId.trim().isEmpty() || item.quantity <= 0)
                throw new IllegalArgumentException("Danh sách sản phẩm không hợp lệ.");
            quantities.merge(item.productId.trim(), item.quantity, Integer::sum);
        }

        return Db.transaction(con -> {
            ensureCanSell(con, a);
            final String employeeId = a.getEmployee().getEmployeeId();

            final BigDecimal memberDiscountRate;
            final BigDecimal pointMultiplier;
            if (customerId != null && !customerId.trim().isEmpty()) {
                Map<String,Object> customer = Db.one(con,
                        "SELECT c.customer_id,c.status,ISNULL(t.discount_rate,0) discount_rate,"
                      + "ISNULL(t.point_multiplier,1) point_multiplier "
                      + "FROM customers c LEFT JOIN membership_tiers t ON t.tier_id=c.tier_id WHERE c.customer_id=?",
                        customerId.trim());
                if (customer == null || !"ACTIVE".equals(String.valueOf(customer.get("status"))))
                    throw new IllegalArgumentException("Khách hàng không tồn tại hoặc đã ngừng hoạt động.");
                memberDiscountRate = (BigDecimal)customer.get("discount_rate");
                pointMultiplier = (BigDecimal)customer.get("point_multiplier");
            } else {
                memberDiscountRate = BigDecimal.ZERO;
                pointMultiplier = BigDecimal.ONE;
            }

            String invoiceId = nextInvoiceId(con);
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal discountTotal = BigDecimal.ZERO;

            class Line {
                String id; int qty; BigDecimal sale; BigDecimal cost; BigDecimal disc; BigDecimal total;
            }
            List<Line> lines = new ArrayList<Line>();

            for (Map.Entry<String,Integer> entry : quantities.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();
                Map<String,Object> p = Db.one(con,
                        "SELECT product_id,sale_price,cost_price,stock_quantity,category_id,status "
                      + "FROM products WHERE product_id=?", productId);
                if (p == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm " + productId);
                if (!"ACTIVE".equals(String.valueOf(p.get("status"))))
                    throw new IllegalArgumentException("Sản phẩm " + productId + " không còn kinh doanh hoặc đã hết hàng.");

                int stock = ((Number)p.get("stock_quantity")).intValue();
                if (quantity > stock)
                    throw new IllegalArgumentException("Sản phẩm " + productId + " chỉ còn " + stock + " sản phẩm.");

                BigDecimal sale = (BigDecimal)p.get("sale_price");
                BigDecimal cost = (BigDecimal)p.get("cost_price");
                BigDecimal gross = sale.multiply(BigDecimal.valueOf(quantity));
                BigDecimal promotionDiscount = bestPromotionDiscount(con, productId,
                        ((Number)p.get("category_id")).intValue(), gross, sale, quantity);
                BigDecimal memberDiscount = gross.multiply(memberDiscountRate)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                // SRS có ưu đãi theo hạng nhưng không quy định cách cộng dồn với khuyến mãi.
                // Quy tắc triển khai tối giản: không cộng dồn, lấy mức giảm có lợi hơn cho khách.
                BigDecimal disc = promotionDiscount.max(memberDiscount).min(gross);
                BigDecimal lineTotal = gross.subtract(disc).max(BigDecimal.ZERO);

                Line l = new Line();
                l.id=productId; l.qty=quantity; l.sale=sale; l.cost=cost; l.disc=disc; l.total=lineTotal;
                lines.add(l);
                subtotal = subtotal.add(gross);
                discountTotal = discountTotal.add(disc);
            }

            BigDecimal total = subtotal.subtract(discountTotal);
            if (total.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Tổng thanh toán phải lớn hơn 0.");

            Db.update(con,
                    "INSERT INTO invoices(invoice_id,customer_id,employee_id,subtotal,discount_amount,total_amount,status,note) "
                  + "VALUES(?,?,?,?,?,?,'PAID',?)",
                    invoiceId, emptyToNull(customerId), employeeId, subtotal, discountTotal, total, emptyToNull(note));

            for (Line l: lines) {
                // Trừ kho bằng điều kiện stock_quantity >= qty để tránh hai quầy cùng bán vượt tồn.
                int updated = Db.update(con,
                        "UPDATE products SET stock_quantity=stock_quantity-?, "
                      + "status=CASE WHEN stock_quantity-?=0 THEN 'OUT_OF_STOCK' ELSE status END, "
                      + "updated_at=SYSDATETIME() "
                      + "WHERE product_id=? AND status='ACTIVE' AND stock_quantity>=?",
                        l.qty, l.qty, l.id, l.qty);
                if (updated != 1)
                    throw new IllegalArgumentException("Tồn kho của sản phẩm " + l.id + " vừa thay đổi. Vui lòng kiểm tra lại hóa đơn.");

                Db.update(con,
                        "INSERT INTO invoice_details(invoice_id,product_id,quantity,unit_price,cost_price_snapshot,discount_amount,line_total) "
                      + "VALUES(?,?,?,?,?,?,?)",
                        invoiceId,l.id,l.qty,l.sale,l.cost,l.disc,l.total);
            }

            Db.update(con,
                    "INSERT INTO payments(invoice_id,payment_method,amount,status,transaction_ref) VALUES(?,?,?,'SUCCESS',?)",
                    invoiceId,method,total,emptyToNull(transactionRef));

            if (customerId != null && !customerId.trim().isEmpty()) {
                BigDecimal basePoints = total.divide(POINT_VND_PER_POINT, 0, RoundingMode.DOWN);
                int points = basePoints.multiply(pointMultiplier).setScale(0, RoundingMode.DOWN).intValue();
                if (points > 0) {
                    Db.update(con,
                            "INSERT INTO point_transactions(customer_id,invoice_id,transaction_type,points_change,note) "
                          + "VALUES(?,?,'EARN',?,N'Tích điểm từ hóa đơn đã thanh toán')",
                            customerId.trim(),invoiceId,points);
                    Db.update(con,
                            "UPDATE customers SET current_points=current_points+?,updated_at=SYSDATETIME() WHERE customer_id=?",
                            points,customerId.trim());
                    Db.update(con,
                            "UPDATE c SET tier_id=x.tier_id "
                          + "FROM customers c CROSS APPLY (SELECT TOP 1 tier_id FROM membership_tiers "
                          + "WHERE min_points<=c.current_points ORDER BY min_points DESC) x WHERE c.customer_id=?",
                            customerId.trim());
                }
            }

            BigDecimal bonus = total.multiply(DEFAULT_BONUS_RATE).setScale(2,RoundingMode.HALF_UP);
            Db.update(con,
                    "INSERT INTO sales_bonuses(employee_id,invoice_id,sales_amount,bonus_rate,bonus_amount,status) "
                  + "VALUES(?,?,?,?,?,'OPEN')",
                    employeeId,invoiceId,total,DEFAULT_BONUS_RATE,bonus);

            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getAccountId(),"INVOICE_CREATED","invoices",invoiceId,
                    "Lập hóa đơn bán hàng; thanh toán=" + method);

            return invoiceId;
        });
    }

    /** Tạo mã hóa đơn ngắn, tuần tự: HD00001, HD00002, ... */
    private String nextInvoiceId(Connection con) throws SQLException {
        Object maxShortObj = Db.scalar(con,
                "SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(invoice_id,3,5))),0) "
              + "FROM invoices WHERE LEN(invoice_id)=7 "
              + "AND invoice_id LIKE 'HD[0-9][0-9][0-9][0-9][0-9]'");
        Object totalObj = Db.scalar(con, "SELECT COUNT(*) FROM invoices");
        int maxShort = maxShortObj == null ? 0 : ((Number) maxShortObj).intValue();
        int total = totalObj == null ? 0 : ((Number) totalObj).intValue();
        int next = Math.max(maxShort, total) + 1;

        while (next <= 99999) {
            String candidate = String.format("HD%05d", next);
            Object exists = Db.scalar(con, "SELECT COUNT(*) FROM invoices WHERE invoice_id=?", candidate);
            if (exists == null || ((Number) exists).intValue() == 0) return candidate;
            next++;
        }
        throw new IllegalStateException("Đã vượt quá giới hạn mã hóa đơn HD99999.");
    }

    private BigDecimal bestPromotionDiscount(Connection con, String productId, int categoryId,
                                             BigDecimal gross, BigDecimal unitPrice, int qty) throws SQLException {
        List<Map<String,Object>> rows = Db.query(con,
                "SELECT DISTINCT p.discount_type,p.discount_value "
              + "FROM promotions p "
              + "LEFT JOIN promotion_products pp ON pp.promotion_id=p.promotion_id "
              + "LEFT JOIN promotion_categories pc ON pc.promotion_id=p.promotion_id "
              + "WHERE p.status='ACTIVE' AND SYSDATETIME() BETWEEN p.start_at AND p.end_at "
              + "AND (pp.product_id=? OR pc.category_id=?)",
                productId, categoryId);
        BigDecimal best = BigDecimal.ZERO;
        for (Map<String,Object> r: rows) {
            String type=String.valueOf(r.get("discount_type"));
            BigDecimal v=(BigDecimal)r.get("discount_value");
            BigDecimal d = "PERCENT".equals(type)
                    ? gross.multiply(v).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP)
                    : v.multiply(BigDecimal.valueOf(qty));
            if (d.compareTo(best)>0) best=d;
        }
        return best.min(gross);
    }


    // ===== Đơn tạm chuyển từ màn Tìm sản phẩm sang Lập hóa đơn =====
    private static final LinkedHashMap<String, Integer> pendingInvoiceItems = new LinkedHashMap<String, Integer>();

    public static synchronized void setDraftItems(Map<String, Integer> items) {
        pendingInvoiceItems.clear();
        if (items == null) return;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue().intValue() > 0) {
                pendingInvoiceItems.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static synchronized Map<String, Integer> consumePendingInvoiceItems() {
        LinkedHashMap<String, Integer> copy = new LinkedHashMap<String, Integer>(pendingInvoiceItems);
        pendingInvoiceItems.clear();
        return copy;
    }
}

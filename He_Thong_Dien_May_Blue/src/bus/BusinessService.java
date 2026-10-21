package bus;

import dao.Db;
import entity.TaiKhoan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.util.*;


/**
 * Nghiệp vụ chung UC005-UC033.
 *
 * Ghi chú triển khai:
 * - SRS không quy định chính xác tỷ lệ quy đổi điểm và tỷ lệ thưởng.
 * - Bản đồ án dùng cấu hình mặc định:
 *   1 điểm / 10.000 VND và thưởng 3% doanh số.
 *   Có thể đổi hai hằng số này mà không phải sửa database.
 */
public class BusinessService {
    public static final BigDecimal POINT_VND_PER_POINT = new BigDecimal("10000");
    public static final BigDecimal DEFAULT_BONUS_RATE = new BigDecimal("0.03");
    public static final BigDecimal REFUND_APPROVAL_LIMIT = new BigDecimal("5000000");

    // SRS yêu cầu hệ thống tự tính phí giao/lắp nhưng không quy định mức cụ thể.
    // Dùng chính sách đồ án đơn giản, có thể đổi tại đây mà không sửa database.
    public static final BigDecimal DELIVERY_FEE_INNER_CITY = BigDecimal.ZERO;
    public static final BigDecimal DELIVERY_FEE_OTHER_PROVINCE = new BigDecimal("50000");
    public static final BigDecimal INSTALLATION_FEE = new BigDecimal("100000");

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
        final TaiKhoan a = requirePermission("INVOICE_CREATE");
        try (Connection con = connectDB.ConnectDB.getConnection()) {
            ensureCanSell(con, a);
        }
    }

    private void ensureCanSell(Connection con, TaiKhoan a) throws Exception {
        Map<String,Object> user = Db.one(con,
                "SELECT ac.status account_status,e.status employee_status,r.role_code "
              + "FROM accounts ac JOIN employees e ON e.employee_id=ac.employee_id "
              + "JOIN roles r ON r.role_id=ac.role_id WHERE ac.account_id=?",
                a.getMaTaiKhoan());
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
                a.getNhanVien().getMaNhanVien());
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

        final TaiKhoan a = requirePermission("INVOICE_CREATE");

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
            final String employeeId = a.getNhanVien().getMaNhanVien();

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
                    a.getMaTaiKhoan(),"INVOICE_CREATED","invoices",invoiceId,
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

    public BigDecimal calculateDeliveryFee(String region, boolean install) {
        BigDecimal fee;
        if ("INNER_CITY".equals(region)) fee = DELIVERY_FEE_INNER_CITY;
        else if ("OTHER_PROVINCE".equals(region)) fee = DELIVERY_FEE_OTHER_PROVINCE;
        else throw new IllegalArgumentException("Khu vực giao hàng không hợp lệ.");
        if (install) fee = fee.add(INSTALLATION_FEE);
        return fee;
    }

    /** UC020 - tạo phiếu giao hàng. Việc phân công nhân viên được tách sang UC033. */
    public String createDelivery(String invoiceId, String address, String region,
                                 LocalDate date, LocalTime from, LocalTime to,
                                 boolean install, String note) throws Exception {
        if (invoiceId == null || invoiceId.trim().isEmpty())
            throw new IllegalArgumentException("Chưa chọn hóa đơn.");
        if (address == null || address.trim().isEmpty())
            throw new IllegalArgumentException("Địa chỉ giao hàng không được rỗng.");
        if (date == null || from == null || to == null)
            throw new IllegalArgumentException("Ngày/khung giờ giao hàng không hợp lệ.");
        if (date.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ngày giao hàng không được ở trong quá khứ.");
        if (!from.isBefore(to))
            throw new IllegalArgumentException("Khung giờ giao không hợp lệ.");
        if (!Arrays.asList("INNER_CITY", "OTHER_PROVINCE").contains(region))
            throw new IllegalArgumentException("Khu vực giao hàng không hợp lệ.");

        LocalTime open = LocalTime.of(9, 0);
        LocalTime close = "INNER_CITY".equals(region) ? LocalTime.of(21, 0) : LocalTime.of(20, 0);
        if (from.isBefore(open) || to.isAfter(close))
            throw new IllegalArgumentException("Khung giờ không đúng chính sách giao hàng.");

        final TaiKhoan a = requirePermission("DELIVERY_CREATE");
        final BigDecimal fee = calculateDeliveryFee(region, install);
        final String cleanNote = note == null ? "" : note.trim();

        return Db.transaction(con -> {
            ensureCanSell(con, a);
            Map<String,Object> inv = Db.one(con,
                    "SELECT invoice_id,status FROM invoices WHERE invoice_id=?", invoiceId.trim());
            if (inv == null) throw new IllegalArgumentException("Không tìm thấy hóa đơn.");
            if (!"PAID".equals(String.valueOf(inv.get("status"))))
                throw new IllegalArgumentException("Chỉ hóa đơn đã thanh toán đầy đủ mới được tạo phiếu giao hàng.");

            Object duplicated = Db.scalar(con,
                    "SELECT COUNT(*) FROM deliveries WHERE invoice_id=?", invoiceId.trim());
            if (duplicated != null && ((Number)duplicated).intValue() > 0)
                throw new IllegalArgumentException("Hóa đơn này đã có phiếu giao hàng; hãy xử lý trên phiếu hiện có.");

            String id = nextDeliveryId(con);
            Db.update(con,
                    "INSERT INTO deliveries(delivery_id,invoice_id,assigned_employee_id,delivery_address,region_type,"
                  + "delivery_date,time_from,time_to,install_required,delivery_fee,status) "
                  + "VALUES(?,?,NULL,?,?,?,?,?,?,?,'PREPARING')",
                    id, invoiceId.trim(), address.trim(), region, date, from, to, install, fee);
            Db.update(con,
                    "INSERT INTO delivery_history(delivery_id,old_status,new_status,changed_by,note) VALUES(?,NULL,'PREPARING',?,?)",
                    id, a.getNhanVien().getMaNhanVien(),
                    cleanNote.isEmpty() ? "Tạo phiếu giao hàng" : cleanNote);
            return id;
        });
    }

    /** Tạo mã phiếu giao hàng ngắn, tuần tự: GH00001, GH00002, ... */
    private String nextDeliveryId(Connection con) throws SQLException {
        Object maxShortObj = Db.scalar(con,
                "SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(delivery_id,3,5))),0) "
              + "FROM deliveries WHERE LEN(delivery_id)=7 "
              + "AND delivery_id LIKE 'GH[0-9][0-9][0-9][0-9][0-9]'");
        Object totalObj = Db.scalar(con, "SELECT COUNT(*) FROM deliveries");
        int maxShort = maxShortObj == null ? 0 : ((Number) maxShortObj).intValue();
        int total = totalObj == null ? 0 : ((Number) totalObj).intValue();
        int next = Math.max(maxShort, total) + 1;

        while (next <= 99999) {
            String candidate = String.format("GH%05d", next);
            Object exists = Db.scalar(con, "SELECT COUNT(*) FROM deliveries WHERE delivery_id=?", candidate);
            if (exists == null || ((Number) exists).intValue() == 0) return candidate;
            next++;
        }
        throw new IllegalStateException("Đã vượt quá giới hạn mã phiếu giao hàng GH99999.");
    }

    /** Danh sách nhân viên bán hàng đang hoạt động và có ca bao phủ khung giờ giao. */
    public List<Map<String,Object>> getAvailableDeliveryEmployees(String deliveryId) throws Exception {
        if (deliveryId == null || deliveryId.trim().isEmpty()) return Collections.emptyList();
        return Db.query(
                "SELECT DISTINCT e.employee_id,e.full_name "
              + "FROM deliveries d "
              + "JOIN shift_assignments sa ON sa.work_date=d.delivery_date AND sa.status='ASSIGNED' "
              + "JOIN shifts s ON s.shift_id=sa.shift_id AND s.active=1 "
              + "JOIN employees e ON e.employee_id=sa.employee_id AND e.status='ACTIVE' "
              + "JOIN accounts ac ON ac.employee_id=e.employee_id AND ac.status='ACTIVE' "
              + "JOIN roles r ON r.role_id=ac.role_id AND r.role_code='SALES' "
              + "WHERE d.delivery_id=? AND s.start_time<=d.time_from AND s.end_time>=d.time_to "
              + "AND NOT EXISTS(SELECT 1 FROM leave_requests lr WHERE lr.employee_id=e.employee_id "
              + "AND lr.leave_date=d.delivery_date AND lr.status='APPROVED') "
              + "ORDER BY e.full_name",
                deliveryId.trim());
    }

    /** UC033 - phân công hoặc phân công lại khi phiếu vẫn còn Đang chuẩn bị. */
    public void assignDelivery(String deliveryId, String employeeId, boolean confirmReassign) throws Exception {
        if (deliveryId == null || deliveryId.trim().isEmpty())
            throw new IllegalArgumentException("Chưa chọn phiếu giao hàng.");
        if (employeeId == null || employeeId.trim().isEmpty())
            throw new IllegalArgumentException("Chưa chọn nhân viên phụ trách.");
        final TaiKhoan a = requirePermission("DELIVERY_ASSIGN");

        Db.transaction(con -> {
            ensureCanSell(con, a);
            Map<String,Object> d = Db.one(con,
                    "SELECT status,assigned_employee_id FROM deliveries WHERE delivery_id=?", deliveryId.trim());
            if (d == null) throw new IllegalArgumentException("Không tìm thấy phiếu giao hàng.");
            if (!"PREPARING".equals(String.valueOf(d.get("status"))))
                throw new IllegalArgumentException("Chỉ phiếu đang chuẩn bị mới được phân công hoặc phân công lại.");

            String oldAssignee = d.get("assigned_employee_id") == null ? null : String.valueOf(d.get("assigned_employee_id"));
            if (employeeId.equals(oldAssignee))
                throw new IllegalArgumentException("Phiếu đã được phân công cho nhân viên này.");
            if (oldAssignee != null && !confirmReassign)
                throw new IllegalArgumentException("Phiếu đã có người phụ trách. Cần xác nhận trước khi phân công lại.");

            Object eligible = Db.scalar(con,
                    "SELECT COUNT(*) FROM deliveries d "
                  + "JOIN shift_assignments sa ON sa.work_date=d.delivery_date AND sa.employee_id=? AND sa.status='ASSIGNED' "
                  + "JOIN shifts s ON s.shift_id=sa.shift_id AND s.active=1 "
                  + "JOIN employees e ON e.employee_id=sa.employee_id AND e.status='ACTIVE' "
                  + "JOIN accounts ac ON ac.employee_id=e.employee_id AND ac.status='ACTIVE' "
                  + "JOIN roles r ON r.role_id=ac.role_id AND r.role_code='SALES' "
                  + "WHERE d.delivery_id=? AND s.start_time<=d.time_from AND s.end_time>=d.time_to "
                  + "AND NOT EXISTS(SELECT 1 FROM leave_requests lr WHERE lr.employee_id=e.employee_id "
                  + "AND lr.leave_date=d.delivery_date AND lr.status='APPROVED')",
                    employeeId.trim(), deliveryId.trim());
            if (eligible == null || ((Number)eligible).intValue() == 0)
                throw new IllegalArgumentException("Nhân viên được chọn không có ca phù hợp với ngày/khung giờ giao hàng.");

            int changed = Db.update(con,
                    "UPDATE deliveries SET assigned_employee_id=?,updated_at=SYSDATETIME() "
                  + "WHERE delivery_id=? AND status='PREPARING'",
                    employeeId.trim(), deliveryId.trim());
            if (changed != 1) throw new IllegalStateException("Phiếu vừa được thay đổi, vui lòng tải lại danh sách.");

            String action = oldAssignee == null ? "Phân công giao hàng" : "Phân công lại giao hàng";
            Db.update(con,
                    "INSERT INTO delivery_history(delivery_id,old_status,new_status,changed_by,note) VALUES(?,'PREPARING','PREPARING',?,?)",
                    deliveryId.trim(), a.getNhanVien().getMaNhanVien(),
                    action + " cho " + employeeId.trim());
            return null;
        });
    }

    public BigDecimal calculateRefundAmount(long invoiceDetailId, int qty) throws Exception {
        Map<String,Object> d = Db.one(
                "SELECT quantity,line_total FROM invoice_details WHERE invoice_detail_id=?",
                invoiceDetailId);
        if (d == null) throw new IllegalArgumentException("Không tìm thấy chi tiết hóa đơn.");
        int soldQty = ((Number)d.get("quantity")).intValue();
        if (qty <= 0 || qty > soldQty)
            throw new IllegalArgumentException("Số lượng đổi trả không hợp lệ.");
        return proportionalRefund((BigDecimal)d.get("line_total"), soldQty, qty);
    }

    private BigDecimal proportionalRefund(BigDecimal lineTotal, int soldQty, int returnQty) {
        return lineTotal.multiply(BigDecimal.valueOf(returnQty))
                .divide(BigDecimal.valueOf(soldQty), 2, RoundingMode.HALF_UP);
    }

    /** UC021 - lập phiếu đổi trả/hoàn tiền. Giá trị hoàn luôn do hệ thống tính. */
    public String createReturn(String invoiceId, long invoiceDetailId, int qty,
                               String reason, String condition, boolean seal,
                               boolean accessories, String evidencePath) throws Exception {
        if (invoiceId == null || invoiceId.trim().isEmpty())
            throw new IllegalArgumentException("Chưa chọn hóa đơn.");
        if (reason == null || reason.trim().isEmpty())
            throw new IllegalArgumentException("Cần nhập lý do đổi trả/hoàn tiền.");
        if (condition == null || condition.trim().isEmpty())
            throw new IllegalArgumentException("Cần ghi nhận tình trạng thực tế của sản phẩm.");
        if (!seal || !accessories)
            throw new IllegalArgumentException("Không đủ điều kiện đổi trả: sản phẩm phải còn tem/mác và đầy đủ phụ kiện.");

        final TaiKhoan a = requirePermission("RETURN_CREATE");

        return Db.transaction(con -> {
            final String id = nextReturnId(con);
            // UC021 cần đúng quyền; nếu tài khoản là SALES thì vẫn kiểm tra ca hiện tại.
            ensureCanSell(con, a);
            final String emp = a.getNhanVien().getMaNhanVien();

            Map<String,Object> inv = Db.one(con,
                    "SELECT invoice_id,created_at,status,total_amount FROM invoices WHERE invoice_id=?",
                    invoiceId.trim());
            if (inv == null) throw new IllegalArgumentException("Không tìm thấy hóa đơn.");
            if (!"PAID".equals(String.valueOf(inv.get("status"))))
                throw new IllegalArgumentException("Hóa đơn này đã phát sinh hoàn tiền hoặc không còn ở trạng thái đủ điều kiện đổi trả.");

            java.sql.Timestamp created = (java.sql.Timestamp)inv.get("created_at");
            LocalDate purchaseDate = created.toLocalDateTime().toLocalDate();
            if (purchaseDate.plusDays(7).isBefore(LocalDate.now()))
                throw new IllegalArgumentException("Hóa đơn đã quá thời hạn đổi trả 7 ngày.");

            Object existed = Db.scalar(con,
                    "SELECT TOP 1 return_id FROM return_requests WHERE invoice_id=?",
                    invoiceId.trim());
            if (existed != null)
                throw new IllegalArgumentException("Hóa đơn này đã có phiếu đổi trả/hoàn tiền. Theo quy định mỗi hóa đơn chỉ lập tối đa một phiếu; trường hợp phát sinh thêm cần chuyển Quản lý xử lý riêng.");

            Map<String,Object> d = Db.one(con,
                    "SELECT product_id,quantity,line_total FROM invoice_details WHERE invoice_detail_id=? AND invoice_id=?",
                    invoiceDetailId, invoiceId.trim());
            if (d == null) throw new IllegalArgumentException("Chi tiết hóa đơn không hợp lệ.");
            int soldQty = ((Number)d.get("quantity")).intValue();
            if (qty <= 0 || qty > soldQty)
                throw new IllegalArgumentException("Số lượng đổi trả không hợp lệ.");

            BigDecimal refund = proportionalRefund((BigDecimal)d.get("line_total"), soldQty, qty);
            if (refund.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Giá trị hoàn tiền không hợp lệ.");

            boolean approval = refund.compareTo(REFUND_APPROVAL_LIMIT) > 0;
            String status = approval ? "WAITING_APPROVAL" : "PENDING";

            Db.update(con,
                    "INSERT INTO return_requests(return_id,invoice_id,created_by,reason,physical_condition,evidence_image_path,refund_amount,status) "
                  + "VALUES(?,?,?,?,?,?,?,?)",
                    id,invoiceId.trim(),emp,reason.trim(),condition.trim(),emptyToNull(evidencePath),refund,status);
            Db.update(con,
                    "INSERT INTO return_details(return_id,invoice_detail_id,product_id,quantity,refund_amount,seal_intact,accessories_complete) "
                  + "VALUES(?,?,?,?,?,?,?)",
                    id,invoiceDetailId,String.valueOf(d.get("product_id")),qty,refund,seal,accessories);

            if (approval) {
                Db.update(con,"INSERT INTO refund_approvals(return_id,decision) VALUES(?,'PENDING')",id);
            } else {
                completeRefund(con,id);
            }

            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),"RETURN_CREATED","return_requests",id,
                    approval ? "Lập phiếu đổi trả; chờ duyệt do vượt hạn mức" : "Lập phiếu đổi trả; xử lý trong hạn mức");
            return id;
        });
    }

    /** Tạo mã phiếu hoàn tiền ngắn, tuần tự: HT00001, HT00002, ... */
    private String nextReturnId(Connection con) throws SQLException {
        Object maxShortObj = Db.scalar(con,
                "SELECT ISNULL(MAX(TRY_CONVERT(INT,SUBSTRING(return_id,3,5))),0) "
              + "FROM return_requests WHERE LEN(return_id)=7 "
              + "AND return_id LIKE 'HT[0-9][0-9][0-9][0-9][0-9]'");
        Object totalObj = Db.scalar(con, "SELECT COUNT(*) FROM return_requests");
        int maxShort = maxShortObj == null ? 0 : ((Number) maxShortObj).intValue();
        int total = totalObj == null ? 0 : ((Number) totalObj).intValue();
        int next = Math.max(maxShort, total) + 1;

        while (next <= 99999) {
            String candidate = String.format("HT%05d", next);
            Object exists = Db.scalar(con, "SELECT COUNT(*) FROM return_requests WHERE return_id=?", candidate);
            if (exists == null || ((Number) exists).intValue() == 0) return candidate;
            next++;
        }
        throw new IllegalStateException("Đã vượt quá giới hạn mã phiếu HT99999.");
    }

    /** Thực hiện tác động tài chính/kho sau khi phiếu đã đủ điều kiện xử lý. */
    private void completeRefund(Connection con, String returnId) throws Exception {
        Map<String,Object> rr = Db.one(con,
                "SELECT return_id,invoice_id,refund_amount,status FROM return_requests WHERE return_id=?",
                returnId);
        if (rr == null) throw new IllegalArgumentException("Không tìm thấy phiếu đổi trả.");
        if ("COMPLETED".equals(String.valueOf(rr.get("status"))))
            throw new IllegalStateException("Phiếu này đã được hoàn tiền trước đó.");
        if ("REJECTED".equals(String.valueOf(rr.get("status"))))
            throw new IllegalStateException("Phiếu đã bị từ chối.");

        String invoiceId = String.valueOf(rr.get("invoice_id"));
        BigDecimal refund = (BigDecimal)rr.get("refund_amount");
        Map<String,Object> inv = Db.one(con,
                "SELECT customer_id,employee_id,total_amount FROM invoices WHERE invoice_id=?",
                invoiceId);
        if (inv == null) throw new IllegalArgumentException("Không tìm thấy hóa đơn gốc.");
        BigDecimal invoiceTotal = (BigDecimal)inv.get("total_amount");

        // UC021 hiện lập một phiếu cho hóa đơn; vẫn xử lý theo danh sách chi tiết để không phụ thuộc TOP 1.
        List<Map<String,Object>> details = Db.query(con,
                "SELECT product_id,quantity FROM return_details WHERE return_id=?", returnId);
        if (details.isEmpty()) throw new IllegalArgumentException("Phiếu đổi trả chưa có sản phẩm.");
        for (Map<String,Object> d : details) {
            int q = ((Number)d.get("quantity")).intValue();
            String productId = String.valueOf(d.get("product_id"));
            Db.update(con,
                    "UPDATE products SET stock_quantity=stock_quantity+?, "
                  + "status=CASE WHEN status='OUT_OF_STOCK' THEN 'ACTIVE' ELSE status END, updated_at=SYSDATETIME() "
                  + "WHERE product_id=?",
                    q, productId);
        }

        Object priorObj = Db.scalar(con,
                "SELECT ISNULL(SUM(refund_amount),0) FROM return_requests WHERE invoice_id=? AND status='COMPLETED' AND return_id<>?",
                invoiceId, returnId);
        BigDecimal prior = priorObj == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(priorObj));
        BigDecimal totalRefunded = prior.add(refund);
        String invoiceStatus = totalRefunded.compareTo(invoiceTotal) >= 0 ? "REFUNDED" : "PARTIAL_REFUND";
        Db.update(con,"UPDATE invoices SET status=? WHERE invoice_id=?",invoiceStatus,invoiceId);

        // Ghi một giao dịch hoàn tiền riêng để lịch sử thanh toán có thể đối soát.
        Map<String,Object> payment = Db.one(con,
                "SELECT TOP 1 payment_method FROM payments WHERE invoice_id=? AND status='SUCCESS' ORDER BY paid_at",
                invoiceId);
        if (payment != null) {
            Db.update(con,
                    "INSERT INTO payments(invoice_id,payment_method,amount,status,transaction_ref) VALUES(?,?,?,'REFUNDED',?)",
                    invoiceId,String.valueOf(payment.get("payment_method")),refund,"REFUND:"+returnId);
        }

        // Trừ điểm theo đúng tỷ lệ điểm thực tế đã cộng cho hóa đơn (kể cả hệ số hạng thành viên).
        if (inv.get("customer_id") != null) {
            Object earnObj = Db.scalar(con,
                    "SELECT ISNULL(SUM(points_change),0) FROM point_transactions WHERE invoice_id=? AND transaction_type='EARN'",
                    invoiceId);
            int earned = earnObj == null ? 0 : ((Number)earnObj).intValue();
            int targetReverse = totalRefunded.compareTo(invoiceTotal) >= 0
                    ? earned
                    : BigDecimal.valueOf(earned).multiply(refund).divide(invoiceTotal,0,RoundingMode.DOWN).intValue();
            Object reversedObj = Db.scalar(con,
                    "SELECT ISNULL(-SUM(points_change),0) FROM point_transactions WHERE invoice_id=? AND transaction_type='REVERSAL'",
                    invoiceId);
            int alreadyReversed = reversedObj == null ? 0 : ((Number)reversedObj).intValue();
            int reverse = Math.max(0, targetReverse - alreadyReversed);
            if (reverse > 0) {
                Object currentObj = Db.scalar(con,"SELECT current_points FROM customers WHERE customer_id=?",inv.get("customer_id"));
                int current = currentObj == null ? 0 : ((Number)currentObj).intValue();
                int actual = Math.min(reverse,current);
                if (actual > 0) {
                    Db.update(con,
                            "INSERT INTO point_transactions(customer_id,invoice_id,transaction_type,points_change,note) "
                          + "VALUES(?,?,'REVERSAL',?,N'Khấu trừ điểm do đổi trả/hoàn tiền')",
                            inv.get("customer_id"),invoiceId,-actual);
                    Db.update(con,
                            "UPDATE customers SET current_points=current_points-?,updated_at=SYSDATETIME() WHERE customer_id=?",
                            actual,inv.get("customer_id"));
                    Db.update(con,
                            "UPDATE c SET tier_id=x.tier_id FROM customers c OUTER APPLY "
                          + "(SELECT TOP 1 tier_id FROM membership_tiers WHERE min_points<=c.current_points ORDER BY min_points DESC) x "
                          + "WHERE c.customer_id=?",
                            inv.get("customer_id"));
                }
            }
        }

        // Ghi một dòng điều chỉnh âm. Nếu thưởng kỳ cũ đã chốt, dòng âm sẽ được tính vào kỳ kế tiếp.
        Object rateObj = Db.scalar(con,
                "SELECT TOP 1 bonus_rate FROM sales_bonuses WHERE invoice_id=? AND employee_id=? AND sales_amount>0 ORDER BY created_at",
                invoiceId,inv.get("employee_id"));
        BigDecimal rate = rateObj == null ? DEFAULT_BONUS_RATE : new BigDecimal(String.valueOf(rateObj));
        BigDecimal bonusReverse = refund.multiply(rate).setScale(2,RoundingMode.HALF_UP);
        if (bonusReverse.compareTo(BigDecimal.ZERO) > 0) {
            Db.update(con,
                    "INSERT INTO sales_bonuses(employee_id,invoice_id,sales_amount,bonus_rate,bonus_amount,status) "
                  + "VALUES(?,?,?,?,?,'OPEN')",
                    inv.get("employee_id"),invoiceId,refund.negate(),rate,bonusReverse.negate());
        }

        Db.update(con,
                "UPDATE return_requests SET status='COMPLETED',completed_at=SYSDATETIME() WHERE return_id=?",
                returnId);
    }

    /** UC007 - Quản lý duyệt/từ chối phiếu vượt hạn mức. */
    public void approveRefund(String returnId, boolean approve, String reason) throws Exception {
        final TaiKhoan a = requirePermission("REFUND_APPROVE");
        if (!approve && (reason == null || reason.trim().isEmpty()))
            throw new IllegalArgumentException("Cần nhập lý do từ chối.");

        final String emp = a.getNhanVien().getMaNhanVien();
        Db.transaction(con -> {
            Map<String,Object> rr = Db.one(con,
                    "SELECT return_id,status,refund_amount FROM return_requests WHERE return_id=?",
                    returnId);
            if (rr == null) throw new IllegalArgumentException("Không tìm thấy phiếu.");
            if (!"WAITING_APPROVAL".equals(String.valueOf(rr.get("status"))))
                throw new IllegalStateException("Phiếu không còn ở trạng thái chờ duyệt.");
            if (((BigDecimal)rr.get("refund_amount")).compareTo(REFUND_APPROVAL_LIMIT) <= 0)
                throw new IllegalStateException("Phiếu này không thuộc nhóm vượt hạn mức cần Quản lý duyệt.");

            int changed;
            if (approve) {
                changed = Db.update(con,
                        "UPDATE refund_approvals SET decision='APPROVED',approver_employee_id=?,reason=?,decided_at=SYSDATETIME() "
                      + "WHERE return_id=? AND decision='PENDING'",
                        emp,emptyToNull(reason),returnId);
                if (changed != 1) throw new IllegalStateException("Phiếu đã được Quản lý khác xử lý.");
                Db.update(con,"UPDATE return_requests SET status='APPROVED' WHERE return_id=?",returnId);
                completeRefund(con,returnId);
            } else {
                changed = Db.update(con,
                        "UPDATE refund_approvals SET decision='REJECTED',approver_employee_id=?,reason=?,decided_at=SYSDATETIME() "
                      + "WHERE return_id=? AND decision='PENDING'",
                        emp,reason.trim(),returnId);
                if (changed != 1) throw new IllegalStateException("Phiếu đã được Quản lý khác xử lý.");
                Db.update(con,
                        "UPDATE return_requests SET status='REJECTED',rejection_reason=? WHERE return_id=?",
                        reason.trim(),returnId);
            }

            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),approve?"REFUND_APPROVED":"REFUND_REJECTED","return_requests",returnId,
                    approve?"Quản lý duyệt và thực hiện hoàn tiền":"Quản lý từ chối: "+reason.trim());
            return null;
        });
    }

    /** UC028 - chỉ chính nhân viên được phân công mới được cập nhật tiến trình giao hàng. */
    public void updateDeliveryStatus(String deliveryId, String newStatus, String note) throws Exception {
        final TaiKhoan a = requirePermission("DELIVERY_MANAGE");
        Db.transaction(con -> {
            ensureCanSell(con, a);
            Map<String,Object> row = deliveryForUpdate(con, deliveryId, a.getNhanVien().getMaNhanVien());
            String old = String.valueOf(row.get("status"));
            boolean install = Boolean.TRUE.equals(row.get("install_required"));
            if (!validDeliveryTransition(old, newStatus, install))
                throw new IllegalArgumentException("Không thể chuyển trạng thái từ " + old + " sang " + newStatus + ".");
            Db.update(con,
                    "UPDATE deliveries SET status=?,updated_at=SYSDATETIME() WHERE delivery_id=?",
                    newStatus, deliveryId);
            Db.update(con,
                    "INSERT INTO delivery_history(delivery_id,old_status,new_status,changed_by,note) VALUES(?,?,?,?,?)",
                    deliveryId, old, newStatus, a.getNhanVien().getMaNhanVien(), emptyToNull(note));
            return null;
        });
    }

    public void completeDelivery(String deliveryId, String proofPath, String signaturePath, String note) throws Exception {
        final TaiKhoan a = requirePermission("DELIVERY_MANAGE");
        final String proof = proofPath == null ? "" : proofPath.trim();
        final String signature = signaturePath == null ? "" : signaturePath.trim();
        if (proof.isEmpty() && signature.isEmpty())
            throw new IllegalArgumentException("Cần ít nhất ảnh bằng chứng hoặc chữ ký xác nhận của khách hàng.");

        Db.transaction(con -> {
            ensureCanSell(con, a);
            Map<String,Object> row = deliveryForUpdate(con, deliveryId, a.getNhanVien().getMaNhanVien());
            String old = String.valueOf(row.get("status"));
            if (!"DELIVERING".equals(old))
                throw new IllegalArgumentException("Chỉ phiếu đang giao mới được xác nhận hoàn tất.");
            boolean install = Boolean.TRUE.equals(row.get("install_required"));
            String next = install ? "INSTALLED" : "DELIVERED";
            Db.update(con,
                    "UPDATE deliveries SET status=?,proof_image_path=?,signature_path=?,failure_reason=NULL,updated_at=SYSDATETIME() "
                  + "WHERE delivery_id=?",
                    next, emptyToNull(proof), emptyToNull(signature), deliveryId);
            Db.update(con,
                    "INSERT INTO delivery_history(delivery_id,old_status,new_status,changed_by,note) VALUES(?,?,?,?,?)",
                    deliveryId, old, next, a.getNhanVien().getMaNhanVien(), emptyToNull(note));
            return null;
        });
    }

    /** Ghi nhận giao thất bại và đề xuất lịch giao lại vào ngày kế tiếp. */
    public LocalDate failDelivery(String deliveryId, String reason) throws Exception {
        if (reason == null || reason.trim().isEmpty())
            throw new IllegalArgumentException("Cần nhập lý do giao không thành công.");
        final TaiKhoan a = requirePermission("DELIVERY_MANAGE");

        return Db.transaction(con -> {
            ensureCanSell(con, a);
            Map<String,Object> row = deliveryForUpdate(con, deliveryId, a.getNhanVien().getMaNhanVien());
            String old = String.valueOf(row.get("status"));
            if (!"DELIVERING".equals(old))
                throw new IllegalArgumentException("Chỉ phiếu đang giao mới được ghi nhận giao không thành công.");
            LocalDate deliveryDate = ((java.sql.Date)row.get("delivery_date")).toLocalDate();
            LocalDate proposed = deliveryDate.plusDays(1);
            if (!proposed.isAfter(LocalDate.now())) proposed = LocalDate.now().plusDays(1);
            Db.update(con,
                    "UPDATE deliveries SET status='FAILED',failure_reason=?,reschedule_date=?,updated_at=SYSDATETIME() WHERE delivery_id=?",
                    reason.trim(), proposed, deliveryId);
            Db.update(con,
                    "INSERT INTO delivery_history(delivery_id,old_status,new_status,changed_by,note) VALUES(?,'DELIVERING','FAILED',?,?)",
                    deliveryId, a.getNhanVien().getMaNhanVien(), reason.trim());
            return proposed;
        });
    }

    /** Dùng lịch đề xuất sau lần giao thất bại và đưa phiếu về UC033 để phân công lại. */
    public LocalDate retryDelivery(String deliveryId) throws Exception {
        final TaiKhoan a = requirePermission("DELIVERY_MANAGE");
        return Db.transaction(con -> {
            ensureCanSell(con, a);
            Map<String,Object> row = deliveryForUpdate(con, deliveryId, a.getNhanVien().getMaNhanVien());
            if (!"FAILED".equals(String.valueOf(row.get("status"))))
                throw new IllegalArgumentException("Chỉ phiếu giao không thành công mới được lập lịch giao lại.");
            LocalDate next = row.get("reschedule_date") == null
                    ? LocalDate.now().plusDays(1)
                    : ((java.sql.Date)row.get("reschedule_date")).toLocalDate();
            if (!next.isAfter(LocalDate.now())) next = LocalDate.now().plusDays(1);
            // Ngày giao đã đổi nên bỏ phân công cũ; UC033 sẽ chọn lại người có ca phù hợp cho ngày mới.
            Db.update(con,
                    "UPDATE deliveries SET status='PREPARING',delivery_date=?,assigned_employee_id=NULL,reschedule_date=NULL,updated_at=SYSDATETIME() WHERE delivery_id=?",
                    next, deliveryId);
            Db.update(con,
                    "INSERT INTO delivery_history(delivery_id,old_status,new_status,changed_by,note) VALUES(?,'FAILED','PREPARING',?,?)",
                    deliveryId, a.getNhanVien().getMaNhanVien(), "Lập lịch giao lại ngày " + next + "; chờ phân công lại");
            return next;
        });
    }

    private Map<String,Object> deliveryForUpdate(Connection con, String deliveryId, String employeeId) throws Exception {
        if (deliveryId == null || deliveryId.trim().isEmpty())
            throw new IllegalArgumentException("Chưa chọn phiếu giao hàng.");
        Map<String,Object> row = Db.one(con,
                "SELECT status,install_required,assigned_employee_id,delivery_date,reschedule_date "
              + "FROM deliveries WHERE delivery_id=?", deliveryId.trim());
        if (row == null) throw new IllegalArgumentException("Không tìm thấy phiếu giao hàng.");
        String assigned = row.get("assigned_employee_id") == null ? null : String.valueOf(row.get("assigned_employee_id"));
        boolean admin = PhienDangNhap.getTaiKhoanHienTai() != null
                && PhienDangNhap.getTaiKhoanHienTai().getVaiTro() != null
                && "ADMIN".equals(PhienDangNhap.getTaiKhoanHienTai().getVaiTro().getCodeVaiTro());
        if (!admin) {
            if (assigned == null)
                throw new IllegalArgumentException("Phiếu chưa được phân công. Hãy thực hiện UC033 trước.");
            if (!employeeId.equals(assigned))
                throw new IllegalStateException("Chỉ nhân viên được phân công mới được cập nhật phiếu giao hàng này.");
        }
        return row;
    }

    private boolean validDeliveryTransition(String old, String next, boolean install) {
        // Hoàn tất/thất bại dùng các hàm chuyên biệt để bắt buộc bằng chứng/lý do.
        return "PREPARING".equals(old) && "DELIVERING".equals(next);
    }

    /** UC023 - Cấp/cấp lại thẻ thành viên. Yêu cầu quyền MEMBERSHIP_MANAGE. */
    public String reissueMembershipCard(String customerId) throws Exception {
        final TaiKhoan a=requirePermission("MEMBERSHIP_MANAGE");
        if(customerId==null || customerId.trim().isEmpty())
            throw new IllegalArgumentException("Chưa chọn khách hàng.");
        final String cid=customerId.trim();
        return Db.transaction(con -> {
            Map<String,Object> customer=Db.one(con,"SELECT customer_id,status FROM customers WHERE customer_id=?",cid);
            if(customer==null)throw new IllegalArgumentException("Không tìm thấy khách hàng.");
            if(!"ACTIVE".equals(String.valueOf(customer.get("status"))))
                throw new IllegalStateException("Khách hàng đang ngừng hoạt động nên không thể cấp thẻ mới.");

            Db.update(con,"UPDATE membership_cards SET status='INVALIDATED',invalidated_at=SYSDATETIME() WHERE customer_id=? AND status='ACTIVE'",cid);

            int nextSeq = 1;
            for (Map<String,Object> row : Db.query(con,
                    "SELECT card_code FROM membership_cards WHERE customer_id=?", cid)) {
                String code = row.get("card_code") == null ? "" : String.valueOf(row.get("card_code")).trim();
                String prefix = "BLUE-" + cid + "-";
                if (!code.startsWith(prefix)) continue;
                String suffix = code.substring(prefix.length());
                if (!suffix.matches("\\d+")) continue;
                try {
                    nextSeq = Math.max(nextSeq, Integer.parseInt(suffix) + 1);
                } catch (NumberFormatException ignored) {
                    // Bỏ qua các mã thẻ cũ không theo chuẩn đánh số.
                }
            }
            String cardCode = String.format("BLUE-%s-%02d", cid, nextSeq);
            Db.update(con,"INSERT INTO membership_cards(customer_id,card_code,status) VALUES(?,?,'ACTIVE')",cid,cardCode);
            Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),"MEMBERSHIP_CARD_REISSUED","customers",cid,"Cấp/cấp lại thẻ thành viên: "+cardCode);
            return cardCode;
        });
    }

    /**
     * UC024 - Quản lý chính sách hạng/điểm.
     * Không cho chỉnh điểm khách hàng thủ công; hệ thống vẫn cộng/trừ điểm tự động theo UC019/UC021.
     */
    public void updateMembershipTierPolicy(int tierId,int minPoints,BigDecimal discountRate,BigDecimal pointMultiplier) throws Exception {
        final TaiKhoan a=requirePermission("POINT_MANAGE");
        if(minPoints<0)throw new IllegalArgumentException("Mốc điểm không được âm.");
        if(discountRate==null || discountRate.compareTo(BigDecimal.ZERO)<0 || discountRate.compareTo(new BigDecimal("100"))>0)
            throw new IllegalArgumentException("Mức ưu đãi phải từ 0 đến 100%.");
        if(pointMultiplier==null || pointMultiplier.compareTo(BigDecimal.ZERO)<=0)
            throw new IllegalArgumentException("Hệ số tích điểm phải lớn hơn 0.");

        Db.transaction(con -> {
            Map<String,Object> tier=Db.one(con,"SELECT tier_code,tier_name FROM membership_tiers WHERE tier_id=?",tierId);
            if(tier==null)throw new IllegalArgumentException("Không tìm thấy hạng thành viên.");
            String code=String.valueOf(tier.get("tier_code"));
            if("STANDARD".equals(code) && minPoints!=0)
                throw new IllegalArgumentException("Hạng Tiêu chuẩn phải bắt đầu từ 0 điểm.");
            Object dup=Db.scalar(con,"SELECT COUNT(*) FROM membership_tiers WHERE tier_id<>? AND min_points=?",tierId,minPoints);
            if(((Number)dup).intValue()>0)
                throw new IllegalArgumentException("Mốc điểm này đã được dùng cho hạng khác.");

            // Giữ thứ tự nghiệp vụ cố định: Tiêu chuẩn < Bạc < Vàng < Bạch kim.
            Map<String,Integer> proposed=new LinkedHashMap<String,Integer>();
            for(Map<String,Object> row:Db.query(con,"SELECT tier_id,tier_code,min_points FROM membership_tiers")){
                int id=((Number)row.get("tier_id")).intValue();
                proposed.put(String.valueOf(row.get("tier_code")),id==tierId?minPoints:((Number)row.get("min_points")).intValue());
            }
            String[] order={"STANDARD","SILVER","GOLD","PLATINUM"};
            Integer previous=null;
            for(String c:order){
                Integer v=proposed.get(c);
                if(v==null)continue;
                if(previous!=null && v<=previous)
                    throw new IllegalArgumentException("Mốc điểm phải tăng dần theo thứ tự Tiêu chuẩn → Bạc → Vàng → Bạch kim.");
                previous=v;
            }

            Db.update(con,"UPDATE membership_tiers SET min_points=?,discount_rate=?,point_multiplier=? WHERE tier_id=?",
                    minPoints,discountRate,pointMultiplier,tierId);
            Db.update(con,
                    "UPDATE c SET tier_id=x.tier_id,updated_at=SYSDATETIME() "
                  + "FROM customers c CROSS APPLY (SELECT TOP 1 tier_id FROM membership_tiers WHERE min_points<=c.current_points ORDER BY min_points DESC) x");
            Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),"MEMBERSHIP_POLICY_UPDATED","membership_tiers",String.valueOf(tierId),
                    "Cập nhật chính sách "+tier.get("tier_name")+": mốc="+minPoints+", ưu đãi="+discountRate+"%, hệ số điểm="+pointMultiplier);
            return null;
        });
    }

    /* ========================= UC029-UC031 - HẬU MÃI ========================= */
    private TaiKhoan requireCustomerServicePermission(String permission) {
        return requirePermission(permission);
    }

    /**
     * UC030 - Hạn bảo hành được lấy từ ngày mua + số tháng bảo hành của sản phẩm.
     * Nhân viên CSKH không nhập tay hạn bảo hành để tránh sai lệch.
     */
    public String createWarranty(String customerId,long invoiceDetailId,String issue) throws Exception {
        TaiKhoan a=requireCustomerServicePermission("WARRANTY_MANAGE");
        if(issue==null || issue.trim().isEmpty())
            throw new IllegalArgumentException("Mô tả lỗi không được để trống.");

        return Db.transaction(con -> {
            Map<String,Object> d=Db.one(con,
                    "SELECT d.product_id,d.quantity,i.customer_id,i.created_at,i.status,p.warranty_months, "
                  + "d.quantity-COALESCE((SELECT SUM(rd.quantity) FROM return_details rd "
                  + "JOIN return_requests rr ON rr.return_id=rd.return_id "
                  + "WHERE rd.invoice_detail_id=d.invoice_detail_id AND rr.status='COMPLETED'),0) remaining_qty "
                  + "FROM invoice_details d JOIN invoices i ON i.invoice_id=d.invoice_id "
                  + "JOIN products p ON p.product_id=d.product_id WHERE d.invoice_detail_id=?", invoiceDetailId);
            if(d==null) throw new IllegalArgumentException("Không tìm thấy sản phẩm đã mua.");
            if(!String.valueOf(d.get("customer_id")).equals(customerId))
                throw new IllegalArgumentException("Sản phẩm không thuộc khách hàng đã chọn.");
            String invoiceStatus=String.valueOf(d.get("status"));
            if(!"PAID".equals(invoiceStatus) && !"PARTIAL_REFUND".equals(invoiceStatus))
                throw new IllegalArgumentException("Hóa đơn không còn đủ điều kiện tiếp nhận bảo hành.");
            if(((Number)d.get("remaining_qty")).intValue()<=0)
                throw new IllegalArgumentException("Sản phẩm này đã được trả/hoàn toàn bộ, không thể tiếp nhận bảo hành.");

            LocalDate purchase=((java.sql.Timestamp)d.get("created_at")).toLocalDateTime().toLocalDate();
            int months=((Number)d.get("warranty_months")).intValue();
            LocalDate warrantyEnd=purchase.plusMonths(months);
            if(warrantyEnd.isBefore(LocalDate.now()))
                throw new IllegalArgumentException("Sản phẩm đã hết hạn bảo hành từ "+warrantyEnd+".");

            // Không tạo trùng một phiếu đang xử lý cho cùng dòng hàng.
            int activeTickets=((Number)Db.scalar(con,
                    "SELECT COUNT(*) FROM warranties WHERE invoice_detail_id=? AND status IN ('REPAIRING','COMPLETED','REPLACEMENT')",
                    invoiceDetailId)).intValue();
            int remaining=((Number)d.get("remaining_qty")).intValue();
            if(activeTickets>=remaining)
                throw new IllegalArgumentException("Sản phẩm này đang có phiếu bảo hành chưa kết thúc.");

            String id=Ids.next("BH");
            Db.update(con,
                    "INSERT INTO warranties(warranty_id,customer_id,invoice_detail_id,product_id,issue_description,warranty_end_date,status,created_by) "
                  + "VALUES(?,?,?,?,?,?,'REPAIRING',?)",
                    id,customerId,invoiceDetailId,d.get("product_id"),issue.trim(),warrantyEnd,a.getNhanVien().getMaNhanVien());
            Db.update(con,
                    "INSERT INTO warranty_history(warranty_id,old_status,new_status,changed_by,note) VALUES(?,NULL,'REPAIRING',?,?)",
                    id,a.getNhanVien().getMaNhanVien(),"Tiếp nhận bảo hành");
            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),"WARRANTY_CREATED","warranties",id,"Tiếp nhận bảo hành cho invoice_detail="+invoiceDetailId);
            return id;
        });
    }

    public BigDecimal calculateReplacementCharge(String warrantyId,BigDecimal accessoryFee) throws Exception {
        requireCustomerServicePermission("WARRANTY_MANAGE");
        if(accessoryFee==null) accessoryFee=BigDecimal.ZERO;
        if(accessoryFee.compareTo(BigDecimal.ZERO)<0)
            throw new IllegalArgumentException("Phí phụ kiện không được âm.");
        Map<String,Object> w=Db.one(
                "SELECT d.unit_price,i.created_at,w.status FROM warranties w "
              + "JOIN invoice_details d ON d.invoice_detail_id=w.invoice_detail_id "
              + "JOIN invoices i ON i.invoice_id=d.invoice_id WHERE w.warranty_id=?", warrantyId);
        if(w==null) throw new IllegalArgumentException("Không tìm thấy phiếu bảo hành.");
        if(!"REPAIRING".equals(String.valueOf(w.get("status"))))
            throw new IllegalArgumentException("Chỉ phiếu đang sửa chữa mới được chuyển sang đổi máy.");
        LocalDate purchase=((java.sql.Timestamp)w.get("created_at")).toLocalDateTime().toLocalDate();
        long months=Math.max(0,java.time.temporal.ChronoUnit.MONTHS.between(purchase,LocalDate.now()));
        int percent=(int)Math.min(60,months*10);
        BigDecimal rate=new BigDecimal(percent).divide(new BigDecimal("100"));
        return ((BigDecimal)w.get("unit_price")).multiply(rate).add(accessoryFee).setScale(2,RoundingMode.HALF_UP);
    }

    public void updateWarrantyStatus(String id,String newStatus,String note) throws Exception {
        updateWarrantyStatus(id,newStatus,note,BigDecimal.ZERO);
    }

    public void updateWarrantyStatus(String id,String newStatus,String note,BigDecimal accessoryFee) throws Exception {
        TaiKhoan a=requireCustomerServicePermission("WARRANTY_MANAGE");
        final String ns=newStatus==null?"":newStatus.trim();
        if(accessoryFee==null) accessoryFee=BigDecimal.ZERO;
        final BigDecimal fee=accessoryFee;
        Db.transaction(con -> {
            Map<String,Object> row=Db.one(con,"SELECT status FROM warranties WHERE warranty_id=?",id);
            if(row==null) throw new IllegalArgumentException("Không tìm thấy phiếu bảo hành.");
            String old=String.valueOf(row.get("status"));
            if(old.equals(ns)) throw new IllegalArgumentException("Phiếu đã ở trạng thái này.");
            if(!isValidWarrantyTransition(old,ns))
                throw new IllegalArgumentException("Không thể chuyển bảo hành từ '"+old+"' sang '"+ns+"'.");
            if(("REJECTED".equals(ns) || "REPLACEMENT".equals(ns)) && (note==null || note.trim().isEmpty()))
                throw new IllegalArgumentException("Vui lòng nhập ghi chú/lý do cho trạng thái này.");

            BigDecimal replacementCharge=null;
            if("REPLACEMENT".equals(ns)) {
                if(fee.compareTo(BigDecimal.ZERO)<0) throw new IllegalArgumentException("Phí phụ kiện không được âm.");
                Map<String,Object> w=Db.one(con,
                        "SELECT d.unit_price,i.created_at FROM warranties w "
                      + "JOIN invoice_details d ON d.invoice_detail_id=w.invoice_detail_id "
                      + "JOIN invoices i ON i.invoice_id=d.invoice_id WHERE w.warranty_id=?",id);
                LocalDate purchase=((java.sql.Timestamp)w.get("created_at")).toLocalDateTime().toLocalDate();
                long months=Math.max(0,java.time.temporal.ChronoUnit.MONTHS.between(purchase,LocalDate.now()));
                int percent=(int)Math.min(60,months*10);
                replacementCharge=((BigDecimal)w.get("unit_price"))
                        .multiply(new BigDecimal(percent).divide(new BigDecimal("100")))
                        .add(fee).setScale(2,RoundingMode.HALF_UP);
            }

            Db.update(con,
                    "UPDATE warranties SET status=?,replacement_charge=CASE WHEN ?='REPLACEMENT' THEN ? ELSE replacement_charge END,"
                  + "completed_at=CASE WHEN ?='COMPLETED' THEN SYSDATETIME() ELSE completed_at END,"
                  + "returned_at=CASE WHEN ?='RETURNED' THEN SYSDATETIME() ELSE returned_at END WHERE warranty_id=?",
                    ns,ns,replacementCharge,ns,ns,id);
            Db.update(con,"INSERT INTO warranty_history(warranty_id,old_status,new_status,changed_by,note) VALUES(?,?,?,?,?)",
                    id,old,ns,a.getNhanVien().getMaNhanVien(),emptyToNull(note));
            Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),"WARRANTY_STATUS_CHANGED","warranties",id,old+" -> "+ns);
            return null;
        });
    }

    private boolean isValidWarrantyTransition(String oldStatus,String newStatus) {
        if("REPAIRING".equals(oldStatus))
            return "COMPLETED".equals(newStatus) || "REPLACEMENT".equals(newStatus) || "REJECTED".equals(newStatus);
        if("REPLACEMENT".equals(oldStatus)) return "COMPLETED".equals(newStatus);
        if("COMPLETED".equals(oldStatus)) return "RETURNED".equals(newStatus);
        return false;
    }

    public String createComplaint(String customerId,String phone,String content) throws Exception {
        TaiKhoan a=requireCustomerServicePermission("COMPLAINT_MANAGE");
        String p=phone==null?"":phone.trim();
        String body=content==null?"":content.trim();
        if(p.isEmpty()) throw new IllegalArgumentException("Số điện thoại khách hàng không được để trống.");
        if(!p.matches("[0-9+]{9,15}")) throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        if(body.length()<5) throw new IllegalArgumentException("Nội dung khiếu nại quá ngắn.");
        String priority=classifyComplaint(body);
        return Db.transaction(con -> {
            String id=Ids.next("KN");
            Db.update(con,
                    "INSERT INTO complaints(complaint_id,customer_id,customer_phone,content,priority,status,created_by,assigned_to) "
                  + "VALUES(?,?,?,?,?,'NEW',?,?)",
                    id,emptyToNull(customerId),p,body,priority,a.getNhanVien().getMaNhanVien(),a.getNhanVien().getMaNhanVien());
            Db.update(con,"INSERT INTO complaint_history(complaint_id,old_status,new_status,changed_by,note) VALUES(?,NULL,'NEW',?,?)",
                    id,a.getNhanVien().getMaNhanVien(),"Tiếp nhận khiếu nại");
            Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),"COMPLAINT_CREATED","complaints",id,"Tiếp nhận khiếu nại - ưu tiên "+priority);
            return id;
        });
    }

    /**
     * Đánh dấu các khiếu nại quá 48 giờ và gửi thông báo cho Manager đúng một lần.
     * Có thể được gọi định kỳ; cờ escalated ngăn tạo thông báo lặp.
     */
    public void refreshComplaintEscalation() throws Exception {
        Db.transaction(con -> {
            java.util.List<Map<String,Object>> overdue=Db.query(con,
                    "SELECT complaint_id,customer_phone,content FROM complaints WITH (UPDLOCK,ROWLOCK) "
                  + "WHERE escalated=0 AND due_at<SYSDATETIME() AND status NOT IN ('RESOLVED','CLOSED')");
            if(overdue.isEmpty()) return null;
            for(Map<String,Object> cp:overdue) {
                String id=String.valueOf(cp.get("complaint_id"));
                Db.update(con,"UPDATE complaints SET escalated=1,updated_at=SYSDATETIME() WHERE complaint_id=? AND escalated=0",id);
                for(Map<String,Object> manager:Db.query(con,
                        "SELECT DISTINCT e.employee_id FROM accounts a JOIN employees e ON e.employee_id=a.employee_id "
                      + "JOIN roles r ON r.role_id=a.role_id WHERE r.role_code='MANAGER' AND a.status='ACTIVE' AND e.status='ACTIVE'")) {
                    Db.update(con,"INSERT INTO notifications(employee_id,title,content) VALUES(?,N'Khiếu nại quá 48 giờ',?)",
                            manager.get("employee_id"),"Khiếu nại "+id+" ("+Formats.safe(cp.get("customer_phone"))+") chưa được xử lý quá 48 giờ.");
                }
                Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(NULL,?,?,?,?)",
                        "COMPLAINT_ESCALATED","complaints",id,"Tự động leo thang do quá hạn 48 giờ");
            }
            return null;
        });
    }

    private String classifyComplaint(String text){
        String s=text==null?"":text.toLowerCase(Locale.ROOT);
        String[] urgent={"cháy","nổ","điện giật","nguy hiểm","thương tích","khẩn cấp","rò điện"};
        for(String k:urgent) if(s.contains(k)) return "URGENT";
        return "NORMAL";
    }

    public void updateComplaintStatus(String id,String newStatus,String note) throws Exception {
        TaiKhoan a=requireCustomerServicePermission("COMPLAINT_MANAGE");
        final String ns=newStatus==null?"":newStatus.trim();
        Db.transaction(con -> {
            Map<String,Object> row=Db.one(con,"SELECT status,assigned_to FROM complaints WHERE complaint_id=?",id);
            if(row==null) throw new IllegalArgumentException("Không tìm thấy khiếu nại.");
            String old=String.valueOf(row.get("status"));
            if(old.equals(ns)) throw new IllegalArgumentException("Khiếu nại đã ở trạng thái này.");
            if(!isValidComplaintTransition(old,ns))
                throw new IllegalArgumentException("Không thể chuyển khiếu nại từ '"+old+"' sang '"+ns+"'.");
            if(("RESOLVED".equals(ns) || "CLOSED".equals(ns)) && (note==null || note.trim().isEmpty()))
                throw new IllegalArgumentException("Vui lòng ghi rõ kết quả xử lý trước khi hoàn tất/đóng khiếu nại.");

            Db.update(con,
                    "UPDATE complaints SET status=?,assigned_to=COALESCE(assigned_to,?),updated_at=SYSDATETIME(),"
                  + "resolved_at=CASE WHEN ? IN ('RESOLVED','CLOSED') THEN COALESCE(resolved_at,SYSDATETIME()) ELSE resolved_at END WHERE complaint_id=?",
                    ns,a.getNhanVien().getMaNhanVien(),ns,id);
            Db.update(con,"INSERT INTO complaint_history(complaint_id,old_status,new_status,changed_by,note) VALUES(?,?,?,?,?)",
                    id,old,ns,a.getNhanVien().getMaNhanVien(),emptyToNull(note));
            Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),"COMPLAINT_STATUS_CHANGED","complaints",id,old+" -> "+ns);
            return null;
        });
    }

    private boolean isValidComplaintTransition(String oldStatus,String newStatus) {
        if("NEW".equals(oldStatus)) return "IN_PROGRESS".equals(newStatus);
        if("IN_PROGRESS".equals(oldStatus)) return "WAITING_CUSTOMER".equals(newStatus) || "RESOLVED".equals(newStatus);
        if("WAITING_CUSTOMER".equals(oldStatus)) return "IN_PROGRESS".equals(newStatus) || "RESOLVED".equals(newStatus);
        if("RESOLVED".equals(oldStatus)) return "CLOSED".equals(newStatus);
        return false;
    }

    private TaiKhoan requireManagerPermission(String permission) {
        return requirePermission(permission);
    }

    private TaiKhoan requirePermission(String permission) {
        TaiKhoan a = PhienDangNhap.getTaiKhoanHienTai();
        if (a == null || a.getNhanVien() == null || a.getVaiTro() == null)
            throw new IllegalStateException("Phiên đăng nhập không hợp lệ.");
        if (!PhienDangNhap.coQuyen(permission))
            throw new IllegalStateException("Tài khoản chưa được Admin cấp quyền thực hiện chức năng này.");
        return a;
    }

    /* ========================= UC032 - NHÂN VIÊN ========================= */




    /* ========================= UC006 - CA LÀM VIỆC ========================= */


    /* ========================= UC013 - TỒN KHO ========================= */
    public void setProductDiscontinued(String productId, boolean discontinued) throws Exception {
        TaiKhoan actor=requireManagerPermission("PRODUCT_MANAGE");
        Db.transaction(con -> {
            Map<String,Object> p=Db.one(con,
                    "SELECT stock_quantity,status FROM products WHERE product_id=?",productId);
            if(p==null) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
            String oldStatus=String.valueOf(p.get("status"));
            String newStatus=discontinued ? "DISCONTINUED"
                    : (((Number)p.get("stock_quantity")).intValue()>0 ? "ACTIVE" : "OUT_OF_STOCK");
            if(oldStatus.equals(newStatus)) return null;
            Db.update(con,"UPDATE products SET status=?,updated_at=SYSDATETIME() WHERE product_id=?",newStatus,productId);
            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    actor.getMaTaiKhoan(),"PRODUCT_STATUS_CHANGED","products",productId,oldStatus+" -> "+newStatus);
            return null;
        });
    }

    /**
     * Nhập kho: hệ thống lấy đơn giá từ giá vốn của sản phẩm, tăng tồn và tạo công nợ NCC.
     * Tồn kho không chỉnh tay; các luồng nhập hàng, bán hàng và đổi trả tự cập nhật số lượng.
     */
    public void receiveProductStock(String productId,int supplierId,int quantity,String referenceCode) throws Exception {
        TaiKhoan actor=requireManagerPermission("PRODUCT_MANAGE");
        if(quantity<=0) throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0.");
        String ref=referenceCode==null?"":referenceCode.trim().toUpperCase(Locale.ROOT);
        if(!ref.matches("PN\\d{3,}"))
            throw new IllegalArgumentException("Mã phiếu nhập phải theo dạng PN001, PN002, ...");

        Db.transaction(con -> {
            Map<String,Object> product=Db.one(con,
                    "SELECT product_name,cost_price,status FROM products WHERE product_id=?",productId);
            if(product==null) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
            if("DISCONTINUED".equals(String.valueOf(product.get("status"))))
                throw new IllegalStateException("Sản phẩm đã ngừng kinh doanh. Hãy khôi phục trước khi nhập hàng.");

            BigDecimal unitCost=(BigDecimal)product.get("cost_price");
            if(unitCost==null || unitCost.compareTo(BigDecimal.ZERO)<=0)
                throw new IllegalStateException("Sản phẩm chưa có giá vốn hợp lệ.");

            Map<String,Object> supplier=Db.one(con,
                    "SELECT supplier_name,status FROM suppliers WHERE supplier_id=?",supplierId);
            if(supplier==null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp.");
            if(!"ACTIVE".equals(String.valueOf(supplier.get("status"))))
                throw new IllegalStateException("Nhà cung cấp đang ngừng hợp tác.");

            Object dup=Db.scalar(con,"SELECT COUNT(*) FROM supplier_debts WHERE reference_code=?",ref);
            if(((Number)dup).intValue()>0)
                throw new IllegalArgumentException("Mã phiếu nhập đã tồn tại.");

            BigDecimal total=unitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2,RoundingMode.HALF_UP);
            String desc="Nhập "+quantity+" x "+product.get("product_name")+" @ "+unitCost.toPlainString();

            Db.update(con,
                    "UPDATE products SET stock_quantity=stock_quantity+?,supplier_id=COALESCE(supplier_id,?),"
                  + "status=CASE WHEN status='OUT_OF_STOCK' THEN 'ACTIVE' ELSE status END,updated_at=SYSDATETIME() "
                  + "WHERE product_id=?",
                    quantity,supplierId,productId);
            Db.update(con,
                    "INSERT INTO supplier_debts(supplier_id,reference_code,description,original_amount,remaining_amount,due_date,status) "
                  + "VALUES(?,?,?,?,?,NULL,'UNPAID')",
                    supplierId,ref,desc,total,total);
            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    actor.getMaTaiKhoan(),"STOCK_RECEIVED","products",productId,
                    "Nhập +"+quantity+"; phiếu "+ref+"; NCC "+supplier.get("supplier_name")+"; công nợ "+total.toPlainString());
            return null;
        });
    }

    /* ========================= UC014 - NCC / CÔNG NỢ ========================= */
    public void setSupplierActive(int supplierId,boolean active) throws Exception {
        TaiKhoan actor=requireManagerPermission("SUPPLIER_MANAGE");
        Db.transaction(con -> {
            Map<String,Object> s=Db.one(con,"SELECT status FROM suppliers WHERE supplier_id=?",supplierId);
            if(s==null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp.");
            String next=active?"ACTIVE":"INACTIVE";
            String oldStatus=String.valueOf(s.get("status"));
            if(next.equals(oldStatus)) return null;
            Db.update(con,"UPDATE suppliers SET status=?,updated_at=SYSDATETIME() WHERE supplier_id=?",next,supplierId);
            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    actor.getMaTaiKhoan(),"SUPPLIER_STATUS_CHANGED","suppliers",String.valueOf(supplierId),oldStatus+" -> "+next);
            return null;
        });
    }

    /** Thanh toán tổng công nợ của một NCC, tự phân bổ vào các khoản nợ cũ nhất trước. */
    public void paySupplierDebts(int supplierId, BigDecimal amount) throws Exception {
        TaiKhoan actor=requireManagerPermission("SUPPLIER_MANAGE");
        if(amount==null || amount.compareTo(BigDecimal.ZERO)<=0)
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        Db.transaction(con -> {
            Map<String,Object> supplier=Db.one(con,
                    "SELECT supplier_code,supplier_name FROM suppliers WHERE supplier_id=?",supplierId);
            if(supplier==null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp.");

            java.util.List<Map<String,Object>> debts=Db.query(con,
                    "SELECT debt_id,remaining_amount,reference_code FROM supplier_debts WITH (UPDLOCK,ROWLOCK) "
                  + "WHERE supplier_id=? AND remaining_amount>0 ORDER BY created_at,debt_id",supplierId);
            BigDecimal total=BigDecimal.ZERO;
            for(Map<String,Object> d:debts) total=total.add((BigDecimal)d.get("remaining_amount"));
            if(total.compareTo(BigDecimal.ZERO)<=0)
                throw new IllegalStateException("Nhà cung cấp này hiện không còn công nợ.");
            if(amount.compareTo(total)>0)
                throw new IllegalArgumentException("Số tiền thanh toán vượt quá công nợ còn lại.");

            BigDecimal left=amount;
            for(Map<String,Object> d:debts){
                if(left.compareTo(BigDecimal.ZERO)<=0) break;
                long debtId=((Number)d.get("debt_id")).longValue();
                BigDecimal rem=(BigDecimal)d.get("remaining_amount");
                BigDecimal paid=left.min(rem);

                Db.update(con,
                        "INSERT INTO supplier_payments(supplier_id,debt_id,amount,paid_by,note) VALUES(?,?,?,?,?)",
                        supplierId,debtId,paid,actor.getNhanVien().getMaNhanVien(),"Thanh toán công nợ tự động");
                Db.update(con,
                        "UPDATE supplier_debts SET remaining_amount=remaining_amount-?,"
                      + "status=CASE WHEN remaining_amount-?=0 THEN 'PAID' "
                      + "WHEN due_date IS NOT NULL AND due_date<CAST(GETDATE() AS DATE) THEN 'OVERDUE' ELSE 'PARTIAL' END "
                      + "WHERE debt_id=?",
                        paid,paid,debtId);
                left=left.subtract(paid);
            }

            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    actor.getMaTaiKhoan(),"SUPPLIER_DEBT_PAID","suppliers",String.valueOf(supplierId),
                    "Thanh toán "+amount.toPlainString()+"; còn lại "+total.subtract(amount).toPlainString());
            return null;
        });
    }

    public void finalizeBonusStatement(LocalDate from,LocalDate to) throws Exception {
        TaiKhoan a=requirePermission("BONUS_FINALIZE");
        if(from==null || to==null || from.isAfter(to))
            throw new IllegalArgumentException("Kỳ chốt thưởng không hợp lệ.");

        Db.transaction(con -> {
            Object overlap=Db.scalar(con,
                    "SELECT COUNT(*) FROM bonus_statements WHERE status='FINALIZED' AND period_start<=? AND period_end>=?",
                    to,from);
            if(((Number)overlap).intValue()>0)
                throw new IllegalStateException("Khoảng thời gian này bị trùng với một bảng kê thưởng đã chốt.");

            List<Map<String,Object>> rows=Db.query(con,
                    "SELECT employee_id,SUM(sales_amount) total_sales,SUM(bonus_amount) total_bonus "
                  + "FROM sales_bonuses WHERE CAST(created_at AS DATE) BETWEEN ? AND ? AND status='OPEN' "
                  + "GROUP BY employee_id",
                    from,to);
            if(rows.isEmpty())
                throw new IllegalStateException("Không có dữ liệu thưởng chưa chốt trong kỳ đã chọn.");

            long id=Db.insertAndKey(con,
                    "INSERT INTO bonus_statements(period_start,period_end,status,finalized_by,finalized_at) "
                  + "VALUES(?,?,'FINALIZED',?,SYSDATETIME())",
                    from,to,a.getNhanVien().getMaNhanVien());

            for(Map<String,Object> r:rows){
                Db.update(con,
                        "INSERT INTO bonus_statement_details(statement_id,employee_id,total_sales,total_bonus) VALUES(?,?,?,?)",
                        id,r.get("employee_id"),r.get("total_sales"),r.get("total_bonus"));
                Db.update(con,
                        "INSERT INTO notifications(employee_id,title,content) VALUES(?,N'Bảng kê thưởng đã chốt',?)",
                        r.get("employee_id"),"Kỳ "+from+" đến "+to+" - thưởng: "+Formats.money(r.get("total_bonus")));
            }

            Db.update(con,
                    "UPDATE sales_bonuses SET status='FINALIZED' WHERE CAST(created_at AS DATE) BETWEEN ? AND ? AND status='OPEN'",
                    from,to);
            Db.update(con,
                    "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    a.getMaTaiKhoan(),"BONUS_FINALIZED","bonus_statements",String.valueOf(id),
                    "Chốt bảng kê thưởng kỳ "+from+" đến "+to);
            return null;
        });
    }





    private static Object emptyToNull(String s){ return s==null || s.trim().isEmpty()?null:s.trim(); }
}

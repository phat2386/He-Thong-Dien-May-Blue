package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dao.SalesDAO;
import entity.Account;
import util.Formats;

public class SalesService extends BaseService<SalesDAO> {

    private static final LinkedHashMap<String, Integer> DRAFT_ITEMS = new LinkedHashMap<>();

    public static final BigDecimal POINT_VND_PER_POINT = new BigDecimal("10000");
    public static final BigDecimal DEFAULT_BONUS_RATE = new BigDecimal("0.03");

    public SalesService() {
        super(new SalesDAO());
    }

    public static synchronized void setDraftItems(Map<String, Integer> items) {
        DRAFT_ITEMS.clear();
        if (items == null) {
            return;
        }
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                DRAFT_ITEMS.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static synchronized Map<String, Integer> consumePendingInvoiceItems() {
        Map<String, Integer> copy = new LinkedHashMap<>(DRAFT_ITEMS);
        DRAFT_ITEMS.clear();
        return copy;
    }

    public static class InvoiceItem {
        public final String productId;
        public final int quantity;
        public InvoiceItem(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }

    public void ensureCanSell() throws Exception {
        Account account = requirePermission("INVOICE_CREATE");
        dao.transaction(con -> {
            ensureCanSell(con, account);
            return null;
        });
    }

    protected void ensureCanSell(Connection con, Account account) throws Exception {
        Map<String, Object> state = dao.findAccountSaleState(con, account.getAccountId());
        if (state == null) {
            throw new IllegalStateException("Tài khoản không còn tồn tại trong hệ thống.");
        }
        if (!"ACTIVE".equals(String.valueOf(state.get("account_status")))) {
            throw new IllegalStateException("Tài khoản hiện không ở trạng thái hoạt động.");
        }
        if (!"ACTIVE".equals(String.valueOf(state.get("employee_status")))) {
            throw new IllegalStateException("Nhân viên hiện không ở trạng thái làm việc.");
        }
        String roleCode = String.valueOf(state.get("role_code"));
        if (!"SALES".equals(roleCode)) {
            return;
        }
        if (!dao.hasCurrentShift(con, account.getEmployee().getEmployeeId())) {
            throw new IllegalStateException( "Bạn chưa có ca làm việc hợp lệ tại thời điểm hiện tại.");
        }
    }

    public String createInvoice(
            String customerId, List<InvoiceItem> items, String paymentMethod, String transactionRef, String note) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn chưa có sản phẩm.");
        }
        Account account = requirePermission("INVOICE_CREATE");
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if ("INSTALLMENT".equals(method)) {
            throw new IllegalArgumentException( "Đơn trả góp phải được xét duyệt trước; chưa thể ghi nhận là đã thanh toán tại UC019.");
        }
        if (!Arrays.asList("CASH", "CARD", "BANK_TRANSFER").contains(method)) {
            throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ.");
        }
        if (("CARD".equals(method) || "BANK_TRANSFER".equals(method)) && (transactionRef == null || transactionRef.trim().isEmpty())) {
            throw new IllegalArgumentException( "Cần nhập mã giao dịch cho thanh toán thẻ/chuyển khoản.");
        }
        LinkedHashMap<String, Integer> quantities = new LinkedHashMap<>();
        for (InvoiceItem item : items) {
            if (item == null || item.productId == null || item.productId.trim().isEmpty() || item.quantity <= 0) {
                throw new IllegalArgumentException("Danh sách sản phẩm không hợp lệ.");
            }
            quantities.merge(item.productId.trim(), item.quantity, Integer::sum);
        }
        return dao.transaction(con -> {
            ensureCanSell(con, account);
            String employeeId = account.getEmployee().getEmployeeId();
            BigDecimal memberDiscountRate = BigDecimal.ZERO;
            BigDecimal pointMultiplier = BigDecimal.ONE;
            String normalizedCustomerId = customerId == null ? "" : customerId.trim();
            if (!normalizedCustomerId.isEmpty()) {
                Map<String, Object> customer = dao.findCustomerMembership(con, normalizedCustomerId);
                if (customer == null || !"ACTIVE".equals(String.valueOf(customer.get("status")))) {
                    throw new IllegalArgumentException( "Khách hàng không tồn tại hoặc đã ngừng hoạt động.");
                }
                memberDiscountRate = (BigDecimal) customer.get("discount_rate");
                pointMultiplier = (BigDecimal) customer.get("point_multiplier");
            }
            String invoiceId = nextInvoiceId(con);
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal discountTotal = BigDecimal.ZERO;
            List<InvoiceLine> lines = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();
                Map<String, Object> product = dao.findProductForInvoice(con, productId);
                if (product == null) {
                    throw new IllegalArgumentException("Không tìm thấy sản phẩm " + productId);
                }
                if (!"ACTIVE".equals(String.valueOf(product.get("status")))) {
                    throw new IllegalArgumentException( "Sản phẩm " + productId + " không còn kinh doanh hoặc đã hết hàng.");
                }
                int stock = ((Number) product.get("stock_quantity")).intValue();
                if (quantity > stock) {
                    throw new IllegalArgumentException( "Sản phẩm " + productId + " chỉ còn " + stock + " sản phẩm.");
                }
                BigDecimal salePrice = (BigDecimal) product.get("sale_price");
                BigDecimal costPrice = (BigDecimal) product.get("cost_price");
                BigDecimal gross = salePrice.multiply(BigDecimal.valueOf(quantity));
                int categoryId = ((Number) product.get("category_id")).intValue();
                BigDecimal promotionDiscount = bestPromotionDiscount( con, productId, categoryId, gross, quantity);
                BigDecimal memberDiscount = gross .multiply(memberDiscountRate) .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal discount = promotionDiscount.max(memberDiscount).min(gross);
                BigDecimal lineTotal = gross.subtract(discount).max(BigDecimal.ZERO);
                lines.add(new InvoiceLine( productId, quantity, salePrice, costPrice, discount, lineTotal));
                subtotal = subtotal.add(gross);
                discountTotal = discountTotal.add(discount);
            }
            BigDecimal total = subtotal.subtract(discountTotal);
            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Tổng thanh toán phải lớn hơn 0.");
            }
            dao.insertInvoice(
                    con, invoiceId, emptyToNull(normalizedCustomerId), employeeId, subtotal, discountTotal, total, emptyToNull(note));
            for (InvoiceLine line : lines) {
                int updated = dao.decreaseProductStock(con, line.productId, line.quantity);
                if (updated != 1) {
                    throw new IllegalArgumentException(
                            "Tồn kho của sản phẩm " + line.productId + " vừa thay đổi. Vui lòng kiểm tra lại hóa đơn.");
                }
                dao.insertInvoiceDetail(
                        con, invoiceId, line.productId, line.quantity, line.salePrice, line.costPrice, line.discount, line.lineTotal);
            }
            dao.insertPayment( con, invoiceId, method, total, emptyToNull(transactionRef));
            if (!normalizedCustomerId.isEmpty()) {
                BigDecimal basePoints = total.divide( POINT_VND_PER_POINT, 0, RoundingMode.DOWN);
                int points = basePoints .multiply(pointMultiplier) .setScale(0, RoundingMode.DOWN) .intValue();
                if (points > 0) {
                    dao.insertEarnedPoints(con, normalizedCustomerId, invoiceId, points);
                    dao.addCustomerPoints(con, normalizedCustomerId, points);
                    dao.refreshCustomerTier(con, normalizedCustomerId);
                }
            }
            BigDecimal bonus = total .multiply(DEFAULT_BONUS_RATE) .setScale(2, RoundingMode.HALF_UP);
            dao.insertSalesBonus( con, employeeId, invoiceId, total, DEFAULT_BONUS_RATE, bonus);
            dao.insertAuditLog(
                    con, account.getAccountId(), "INVOICE_CREATED", "invoices", invoiceId, "Lập hóa đơn bán hàng; thanh toán=" + method);
            return invoiceId;
        });
    }

    private String nextInvoiceId(Connection con) throws SQLException {
        int next = Math.max(dao.maxShortInvoiceNumber(con), dao.invoiceCount(con)) + 1;
        while (next <= 99999) {
            String candidate = String.format("HD%05d", next);
            if (!dao.invoiceIdExists(con, candidate)) {
                return candidate;
            }
            next++;
        }
        throw new IllegalStateException("Đã vượt quá giới hạn mã hóa đơn HD99999.");
    }

    private BigDecimal bestPromotionDiscount(
            Connection con, String productId, int categoryId, BigDecimal gross, int quantity) throws SQLException {
        List<Map<String, Object>> rows = dao.findActivePromotions(con, productId, categoryId);
        BigDecimal best = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            String type = String.valueOf(row.get("discount_type"));
            BigDecimal value = (BigDecimal) row.get("discount_value");
            BigDecimal discount = "PERCENT".equals(type) ? gross.multiply(value)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : value.multiply(BigDecimal.valueOf(quantity));
            if (discount.compareTo(best) > 0) {
                best = discount;
            }
        }
        return best.min(gross);
    }

    public void finalizeBonusStatement(LocalDate from, LocalDate to) throws Exception {
        Account account = requirePermission("BONUS_FINALIZE");
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Kỳ chốt thưởng không hợp lệ.");
        }
        dao.transaction(con -> {
            if (dao.overlappingFinalizedStatementCount(con, from, to) > 0) {
                throw new IllegalStateException( "Khoảng thời gian này bị trùng với một bảng kê thưởng đã chốt.");
            }
            List<Map<String, Object>> rows = dao.openBonusesByEmployee(con, from, to);
            if (rows.isEmpty()) {
                throw new IllegalStateException( "Không có dữ liệu thưởng chưa chốt trong kỳ đã chọn.");
            }
            long statementId = dao.insertBonusStatement( con, from, to, account.getEmployee().getEmployeeId());
            for (Map<String, Object> row : rows) {
                dao.insertBonusStatementDetail( con, statementId, row.get("employee_id"), row.get("total_sales"), row.get("total_bonus"));
                dao.insertNotification(
                        con, row.get("employee_id"), "Bảng kê thưởng đã chốt", "Kỳ " + from + " đến " + to
                                + " - thưởng: " + Formats.money(row.get("total_bonus")));
            }
            dao.markBonusesFinalized(con, from, to);
            dao.insertAuditLog(
                    con, account.getAccountId(), "BONUS_FINALIZED", "bonus_statements",
                    String.valueOf(statementId), "Chốt bảng kê thưởng kỳ " + from + " đến " + to);
            return null;
        });
    }

    private static final class InvoiceLine {
        private final String productId;
        private final int quantity;
        private final BigDecimal salePrice;
        private final BigDecimal costPrice;
        private final BigDecimal discount;
        private final BigDecimal lineTotal;
        private InvoiceLine(
                String productId, int quantity, BigDecimal salePrice, BigDecimal costPrice, BigDecimal discount, BigDecimal lineTotal) {
            this.productId = productId;
            this.quantity = quantity;
            this.salePrice = salePrice;
            this.costPrice = costPrice;
            this.discount = discount;
            this.lineTotal = lineTotal;
        }
    }
}

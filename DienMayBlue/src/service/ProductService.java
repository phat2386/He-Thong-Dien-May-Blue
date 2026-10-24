package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dao.ProductDAO;
import entity.Account;

public class ProductService extends BaseService<ProductDAO> {

    public ProductService() {
        super(new ProductDAO());
    }

    public void setProductDiscontinued(String productId, boolean discontinued) throws Exception {
        Account actor = requireManagerPermission("PRODUCT_MANAGE");
        dao.transaction(con -> {
            Map<String, Object> product = dao.findProductStatus(con, productId);
            if (product == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
            }
            String oldStatus = String.valueOf(product.get("status"));
            int stock = ((Number) product.get("stock_quantity")).intValue();
            String newStatus = discontinued ? "DISCONTINUED" : (stock > 0 ? "ACTIVE" : "OUT_OF_STOCK");
            if (oldStatus.equals(newStatus)) {
                return null;
            }
            dao.updateProductStatus(con, productId, newStatus);
            dao.insertAuditLog(
                    con, actor.getAccountId(), "PRODUCT_STATUS_CHANGED", "products", productId, oldStatus + " -> " + newStatus);
            return null;
        });
    }

    public void receiveProductStock(
            String productId, int supplierId, int quantity, String referenceCode) throws Exception {
        Account actor = requireManagerPermission("PRODUCT_MANAGE");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0.");
        }
        String ref = referenceCode == null ? "" : referenceCode.trim().toUpperCase(Locale.ROOT);
        if (!ref.matches("PN\\d{3,}")) {
            throw new IllegalArgumentException("Mã phiếu nhập phải theo dạng PN001, PN002, ...");
        }
        dao.transaction(con -> {
            Map<String, Object> product = dao.findProductForReceiving(con, productId);
            if (product == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
            }
            if ("DISCONTINUED".equals(String.valueOf(product.get("status")))) {
                throw new IllegalStateException( "Sản phẩm đã ngừng kinh doanh. Hãy khôi phục trước khi nhập hàng.");
            }
            BigDecimal unitCost = (BigDecimal) product.get("cost_price");
            if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Sản phẩm chưa có giá vốn hợp lệ.");
            }
            Map<String, Object> supplier = dao.findSupplierStatus(con, supplierId);
            if (supplier == null) {
                throw new IllegalArgumentException("Không tìm thấy nhà cung cấp.");
            }
            if (!"ACTIVE".equals(String.valueOf(supplier.get("status")))) {
                throw new IllegalStateException("Nhà cung cấp đang ngừng hợp tác.");
            }
            if (dao.supplierDebtReferenceExists(con, ref)) {
                throw new IllegalArgumentException("Mã phiếu nhập đã tồn tại.");
            }
            BigDecimal total = unitCost .multiply(BigDecimal.valueOf(quantity)) .setScale(2, RoundingMode.HALF_UP);
            String description = "Nhập " + quantity + " x " + product.get("product_name") + " @ " + unitCost.toPlainString();
            dao.addStock(con, productId, supplierId, quantity);
            dao.insertSupplierDebt(con, supplierId, ref, description, total);
            dao.insertAuditLog(
                    con, actor.getAccountId(), "STOCK_RECEIVED", "products",
                    productId, "Nhập +" + quantity + "; phiếu " + ref + "; NCC " + supplier.get("supplier_name")
                            + "; công nợ " + total.toPlainString());
            return null;
        });
    }

    public void setSupplierActive(int supplierId, boolean active) throws Exception {
        Account actor = requireManagerPermission("SUPPLIER_MANAGE");
        dao.transaction(con -> {
            Map<String, Object> supplier = dao.findSupplierStatus(con, supplierId);
            if (supplier == null) {
                throw new IllegalArgumentException("Không tìm thấy nhà cung cấp.");
            }
            String nextStatus = active ? "ACTIVE" : "INACTIVE";
            String oldStatus = String.valueOf(supplier.get("status"));
            if (nextStatus.equals(oldStatus)) {
                return null;
            }
            dao.updateSupplierStatus(con, supplierId, nextStatus);
            dao.insertAuditLog(
                    con, actor.getAccountId(), "SUPPLIER_STATUS_CHANGED", "suppliers",
                    String.valueOf(supplierId), oldStatus + " -> " + nextStatus);
            return null;
        });
    }

    public void paySupplierDebts(int supplierId, BigDecimal amount) throws Exception {
        Account actor = requireManagerPermission("SUPPLIER_MANAGE");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }
        dao.transaction(con -> {
            Map<String, Object> supplier = dao.findSupplierForDebtPayment(con, supplierId);
            if (supplier == null) {
                throw new IllegalArgumentException("Không tìm thấy nhà cung cấp.");
            }
            List<Map<String, Object>> debts = dao.findOpenSupplierDebts(con, supplierId);
            BigDecimal totalDebt = BigDecimal.ZERO;
            for (Map<String, Object> debt : debts) {
                totalDebt = totalDebt.add((BigDecimal) debt.get("remaining_amount"));
            }
            if (totalDebt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Nhà cung cấp này hiện không còn công nợ.");
            }
            if (amount.compareTo(totalDebt) > 0) {
                throw new IllegalArgumentException("Số tiền thanh toán vượt quá công nợ còn lại.");
            }
            BigDecimal remainingPayment = amount;
            for (Map<String, Object> debt : debts) {
                if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                long debtId = ((Number) debt.get("debt_id")).longValue();
                BigDecimal remainingDebt = (BigDecimal) debt.get("remaining_amount");
                BigDecimal paid = remainingPayment.min(remainingDebt);
                dao.insertSupplierPayment(
                        con, supplierId, debtId, paid, actor.getEmployee().getEmployeeId(), "Thanh toán công nợ tự động");
                dao.reduceSupplierDebt(con, debtId, paid);
                remainingPayment = remainingPayment.subtract(paid);
            }
            dao.insertAuditLog(
                    con, actor.getAccountId(), "SUPPLIER_DEBT_PAID", "suppliers",
                    String.valueOf(supplierId), "Thanh toán " + amount.toPlainString()
                            + "; còn lại " + totalDebt.subtract(amount).toPlainString());
            return null;
        });
    }
}

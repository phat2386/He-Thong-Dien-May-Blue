package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Truy cập dữ liệu cho sản phẩm, nhà cung cấp và công nợ nhập hàng.
 */
public class ProductDAO extends BaseDAO {

    public Map<String, Object> findProductStatus(Connection con, String productId) throws SQLException {
        return one(con, "SELECT stock_quantity,status FROM products WHERE product_id=?", productId);
    }

    public void updateProductStatus(Connection con, String productId, String status) throws SQLException {
        update(con, "UPDATE products SET status=?,updated_at=SYSDATETIME() WHERE product_id=?", status, productId);
    }

    public Map<String, Object> findProductForReceiving(Connection con, String productId) throws SQLException {
        return one(con, "SELECT product_name,cost_price,status FROM products WHERE product_id=?", productId);
    }

    public Map<String, Object> findSupplierStatus(Connection con, int supplierId) throws SQLException {
        return one(con, "SELECT supplier_name,status FROM suppliers WHERE supplier_id=?", supplierId);
    }

    public boolean supplierDebtReferenceExists(Connection con, String referenceCode) throws SQLException {
        Object count = scalar(con, "SELECT COUNT(*) FROM supplier_debts WHERE reference_code=?", referenceCode);
        return count != null && ((Number) count).intValue() > 0;
    }

    public void addStock(Connection con, String productId, int supplierId, int quantity) throws SQLException {
        update(con, "UPDATE products SET stock_quantity=stock_quantity+?,supplier_id=COALESCE(supplier_id,?)," +
                "status=CASE WHEN status='OUT_OF_STOCK' THEN 'ACTIVE' ELSE status END," + "updated_at=SYSDATETIME() WHERE product_id=?",
                quantity, supplierId, productId);
    }

    public void insertSupplierDebt(
            Connection con, int supplierId, String referenceCode, String description, BigDecimal amount) throws SQLException {
        update(con, "INSERT INTO supplier_debts(" +
                "supplier_id,reference_code,description,original_amount,remaining_amount,due_date,status) " +
                "VALUES(?,?,?,?,?,NULL,'UNPAID')", supplierId, referenceCode, description, amount, amount);
    }

    public Map<String, Object> findSupplierForDebtPayment(Connection con, int supplierId) throws SQLException {
        return one(con, "SELECT supplier_code,supplier_name FROM suppliers WHERE supplier_id=?", supplierId);
    }

    public List<Map<String, Object>> findOpenSupplierDebts(Connection con, int supplierId) throws SQLException {
        return query(con, "SELECT debt_id,remaining_amount,reference_code FROM supplier_debts WITH (UPDLOCK,ROWLOCK) " +
                "WHERE supplier_id=? AND remaining_amount>0 ORDER BY created_at,debt_id", supplierId);
    }

    public void insertSupplierPayment(
            Connection con, int supplierId, long debtId, BigDecimal amount, String paidBy, String note) throws SQLException {
        update(con, "INSERT INTO supplier_payments(supplier_id,debt_id,amount,paid_by,note) VALUES(?,?,?,?,?)", supplierId, debtId,
                amount, paidBy, note);
    }

    public void reduceSupplierDebt(Connection con, long debtId, BigDecimal amount) throws SQLException {
        update(con, "UPDATE supplier_debts SET remaining_amount=remaining_amount-?," +
                "status=CASE WHEN remaining_amount-?=0 THEN 'PAID' " +
                "WHEN due_date IS NOT NULL AND due_date<CAST(GETDATE() AS DATE) THEN 'OVERDUE' " + "ELSE 'PARTIAL' END WHERE debt_id=?",
                amount, amount, debtId);
    }

    public void updateSupplierStatus(Connection con, int supplierId, String status) throws SQLException {
        update(con, "UPDATE suppliers SET status=?,updated_at=SYSDATETIME() WHERE supplier_id=?", status, supplierId);
    }

    public void insertAuditLog(
            Connection con, int accountId, String action, String entity, String entityId, String detail) throws SQLException {
        update(con, "INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)", accountId, action,
                entity, entityId, detail);
    }
}

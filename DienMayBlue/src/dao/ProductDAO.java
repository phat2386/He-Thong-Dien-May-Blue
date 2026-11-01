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

/**
 * Truy cập dữ liệu cho sản phẩm, nhà cung cấp và công nợ nhập hàng.
 */
public class ProductDAO {

    public Map<String, Object> findProductStatus(String productId) throws SQLException {
        return one("SELECT stock_quantity,status FROM products WHERE product_id=?", productId);
    }

    public void updateProductStatus(String productId, String status) throws SQLException {
        update("UPDATE products SET status=?,updated_at=SYSDATETIME() WHERE product_id=?", status, productId);
    }

    public Map<String, Object> findProductForReceiving(String productId) throws SQLException {
        return one("SELECT product_name,cost_price,status FROM products WHERE product_id=?", productId);
    }

    public Map<String, Object> findSupplierStatus(int supplierId) throws SQLException {
        return one("SELECT supplier_name,status FROM suppliers WHERE supplier_id=?", supplierId);
    }

    public boolean supplierDebtReferenceExists(String referenceCode) throws SQLException {
        Object count = scalar("SELECT COUNT(*) FROM supplier_debts WHERE reference_code=?", referenceCode);
        return count != null && ((Number) count).intValue() > 0;
    }

    public void addStock(String productId, int supplierId, int quantity) throws SQLException {
        update("UPDATE products SET stock_quantity=stock_quantity+?,supplier_id=COALESCE(supplier_id,?)," +
                "status=CASE WHEN status='OUT_OF_STOCK' THEN 'ACTIVE' ELSE status END," + "updated_at=SYSDATETIME() WHERE product_id=?",
                quantity, supplierId, productId);
    }

    public void insertSupplierDebt(
            int supplierId, String referenceCode, String description, BigDecimal amount) throws SQLException {
        update("INSERT INTO supplier_debts(" +
                "supplier_id,reference_code,description,original_amount,remaining_amount,due_date,status) " +
                "VALUES(?,?,?,?,?,NULL,'UNPAID')", supplierId, referenceCode, description, amount, amount);
    }

    public Map<String, Object> findSupplierForDebtPayment(int supplierId) throws SQLException {
        return one("SELECT supplier_code,supplier_name FROM suppliers WHERE supplier_id=?", supplierId);
    }

    public List<Map<String, Object>> findOpenSupplierDebts(int supplierId) throws SQLException {
        return query("SELECT debt_id,remaining_amount,reference_code FROM supplier_debts WITH (UPDLOCK,ROWLOCK) " +
                "WHERE supplier_id=? AND remaining_amount>0 ORDER BY created_at,debt_id", supplierId);
    }

    public void insertSupplierPayment(
            int supplierId, long debtId, BigDecimal amount, String paidBy, String note) throws SQLException {
        update("INSERT INTO supplier_payments(supplier_id,debt_id,amount,paid_by,note) VALUES(?,?,?,?,?)", supplierId, debtId,
                amount, paidBy, note);
    }

    public void reduceSupplierDebt(long debtId, BigDecimal amount) throws SQLException {
        update("UPDATE supplier_debts SET remaining_amount=remaining_amount-?," +
                "status=CASE WHEN remaining_amount-?=0 THEN 'PAID' " +
                "WHEN due_date IS NOT NULL AND due_date<CAST(GETDATE() AS DATE) THEN 'OVERDUE' " + "ELSE 'PARTIAL' END WHERE debt_id=?",
                amount, amount, debtId);
    }

    public void updateSupplierStatus(int supplierId, String status) throws SQLException {
        update("UPDATE suppliers SET status=?,updated_at=SYSDATETIME() WHERE supplier_id=?", status, supplierId);
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

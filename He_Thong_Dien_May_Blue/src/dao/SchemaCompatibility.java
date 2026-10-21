package dao;

import connectDB.ConnectDB;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Đồng bộ các thay đổi schema nhỏ cần thiết khi mở project trên database cũ.
 * Chỉ thực hiện migration idempotent, không xóa dữ liệu nghiệp vụ.
 */
public final class SchemaCompatibility {
    private SchemaCompatibility() {}

    public static void ensureCompatible() throws SQLException {
        try (Connection con = ConnectDB.getConnection()) {
            ensureWarrantyMonths(con);
            ensureDeliveryIds(con);
            ensureRefundBonusAdjustments(con);
            ensureAdminActor(con);
            ensureFlexiblePermissionModel(con);
            ensureDefaultActorPermissions(con);
        }
    }

    private static void ensureWarrantyMonths(Connection con) throws SQLException {
        boolean productTableExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT CASE WHEN OBJECT_ID(N'dbo.products',N'U') IS NULL THEN 0 ELSE 1 END")) {
            productTableExists = rs.next() && rs.getInt(1) == 1;
        }
        if (!productTableExists) {
            throw new SQLException("Chưa có bảng dbo.products. Hãy chạy bộ SQL 01 -> 04 trước khi mở ứng dụng.");
        }

        boolean exists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT CASE WHEN COL_LENGTH('dbo.products','warranty_months') IS NULL THEN 0 ELSE 1 END")) {
            exists = rs.next() && rs.getInt(1) == 1;
        }

        if (!exists) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate(
                    "ALTER TABLE dbo.products ADD warranty_months INT NOT NULL " +
                    "CONSTRAINT DF_products_warranty_months DEFAULT (12) WITH VALUES"
                );
            }
        }

        boolean constraintExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM sys.check_constraints " +
                 "WHERE name=N'CK_products_warranty_months' " +
                 "AND parent_object_id=OBJECT_ID(N'dbo.products')")) {
            constraintExists = rs.next() && rs.getInt(1) > 0;
        }

        if (!constraintExists) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate(
                    "ALTER TABLE dbo.products ADD CONSTRAINT CK_products_warranty_months " +
                    "CHECK (warranty_months >= 12)"
                );
            }
        }
    }

    /**
     * Chuẩn hóa mã phiếu giao hàng về GH00001, GH00002, ... cho database cũ.
     * Chỉ đổi khóa nghiệp vụ và các tham chiếu liên quan; không xóa dữ liệu giao hàng.
     */
    private static void ensureDeliveryIds(Connection con) throws SQLException {
        boolean tableExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT CASE WHEN OBJECT_ID(N'dbo.deliveries',N'U') IS NULL THEN 0 ELSE 1 END")) {
            tableExists = rs.next() && rs.getInt(1) == 1;
        }
        if (!tableExists) return;

        int count;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM dbo.deliveries")) {
            count = rs.next() ? rs.getInt(1) : 0;
        }
        if (count > 99999) throw new SQLException("Số lượng phiếu giao hàng vượt quá 99.999, không thể dùng định dạng GH00001.");

        boolean needsMigration;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM dbo.deliveries WHERE delivery_id NOT LIKE 'GH[0-9][0-9][0-9][0-9][0-9]' OR LEN(delivery_id)<>7")) {
            needsMigration = rs.next() && rs.getInt(1) > 0;
        }

        if (needsMigration) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {
                st.executeUpdate("IF OBJECT_ID(N'dbo.CK_deliveries_id_format',N'C') IS NOT NULL ALTER TABLE dbo.deliveries DROP CONSTRAINT CK_deliveries_id_format");
                st.executeUpdate("IF OBJECT_ID(N'dbo.FK_delivery_history_delivery',N'F') IS NOT NULL ALTER TABLE dbo.delivery_history DROP CONSTRAINT FK_delivery_history_delivery");
                st.executeUpdate("IF OBJECT_ID('tempdb..#DeliveryMap') IS NOT NULL DROP TABLE #DeliveryMap; " +
                        "CREATE TABLE #DeliveryMap(old_id VARCHAR(30) NOT NULL PRIMARY KEY,temp_id VARCHAR(7) NOT NULL UNIQUE,new_id VARCHAR(7) NOT NULL UNIQUE); " +
                        ";WITH x AS (SELECT delivery_id,ROW_NUMBER() OVER (ORDER BY created_at,delivery_id) rn FROM dbo.deliveries) " +
                        "INSERT INTO #DeliveryMap(old_id,temp_id,new_id) SELECT delivery_id,'ZG'+RIGHT('00000'+CONVERT(VARCHAR(5),rn),5),'GH'+RIGHT('00000'+CONVERT(VARCHAR(5),rn),5) FROM x; " +
                        "UPDATE d SET d.delivery_id=m.temp_id FROM dbo.deliveries d JOIN #DeliveryMap m ON m.old_id=d.delivery_id; " +
                        "UPDATE h SET h.delivery_id=m.temp_id FROM dbo.delivery_history h JOIN #DeliveryMap m ON m.old_id=h.delivery_id; " +
                        "UPDATE d SET d.delivery_id=m.new_id FROM dbo.deliveries d JOIN #DeliveryMap m ON m.temp_id=d.delivery_id; " +
                        "UPDATE h SET h.delivery_id=m.new_id FROM dbo.delivery_history h JOIN #DeliveryMap m ON m.temp_id=h.delivery_id; " +
                        "UPDATE a SET a.entity_id=m.new_id FROM dbo.audit_logs a JOIN #DeliveryMap m ON m.old_id=a.entity_id WHERE a.entity_name='deliveries'; " +
                        "ALTER TABLE dbo.delivery_history WITH CHECK ADD CONSTRAINT FK_delivery_history_delivery FOREIGN KEY(delivery_id) REFERENCES dbo.deliveries(delivery_id)");
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(oldAutoCommit);
            }
        }

        boolean checkExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM sys.check_constraints WHERE name=N'CK_deliveries_id_format' AND parent_object_id=OBJECT_ID(N'dbo.deliveries')")) {
            checkExists = rs.next() && rs.getInt(1) > 0;
        }
        if (!checkExists) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("ALTER TABLE dbo.deliveries ADD CONSTRAINT CK_deliveries_id_format CHECK (delivery_id LIKE 'GH[0-9][0-9][0-9][0-9][0-9]' AND LEN(delivery_id)=7)");
            }
        }
    }

    /**
     * UC021 ghi một dòng thưởng âm khi hoàn tiền. Database cũ từng chặn số âm
     * và đặt UNIQUE(invoice_id, employee_id), vì vậy cần chuyển sang unique
     * có lọc chỉ dành cho dòng thưởng bán hàng gốc.
     */
    private static void ensureRefundBonusAdjustments(Connection con) throws SQLException {
        boolean bonusTableExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT CASE WHEN OBJECT_ID(N'dbo.sales_bonuses',N'U') IS NULL THEN 0 ELSE 1 END")) {
            bonusTableExists = rs.next() && rs.getInt(1) == 1;
        }
        if (!bonusTableExists) {
            throw new SQLException("Chưa có bảng dbo.sales_bonuses. Hãy chạy bộ SQL 01 -> 04 trước khi mở ứng dụng.");
        }

        boolean oldUniqueConstraint;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM sys.key_constraints " +
                 "WHERE name=N'UQ_sales_bonuses_invoice_employee' " +
                 "AND parent_object_id=OBJECT_ID(N'dbo.sales_bonuses')")) {
            oldUniqueConstraint = rs.next() && rs.getInt(1) > 0;
        }
        if (oldUniqueConstraint) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("ALTER TABLE dbo.sales_bonuses DROP CONSTRAINT UQ_sales_bonuses_invoice_employee");
            }
        }

        boolean oldStatusIndexExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM sys.indexes " +
                 "WHERE name=N'UX_sales_bonuses_invoice_employee_status' " +
                 "AND object_id=OBJECT_ID(N'dbo.sales_bonuses')")) {
            oldStatusIndexExists = rs.next() && rs.getInt(1) > 0;
        }
        if (oldStatusIndexExists) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("DROP INDEX UX_sales_bonuses_invoice_employee_status ON dbo.sales_bonuses");
            }
        }

        boolean filteredIndexExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT COUNT(*) FROM sys.indexes " +
                 "WHERE name=N'UX_sales_bonuses_base_invoice_employee' " +
                 "AND object_id=OBJECT_ID(N'dbo.sales_bonuses')")) {
            filteredIndexExists = rs.next() && rs.getInt(1) > 0;
        }
        if (!filteredIndexExists) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate(
                    "CREATE UNIQUE INDEX UX_sales_bonuses_base_invoice_employee " +
                    "ON dbo.sales_bonuses(invoice_id, employee_id) WHERE sales_amount > 0"
                );
            }
        }

        String definition = null;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT definition FROM sys.check_constraints " +
                 "WHERE name=N'CK_sales_bonuses_values' " +
                 "AND parent_object_id=OBJECT_ID(N'dbo.sales_bonuses')")) {
            if (rs.next()) definition = rs.getString(1);
        }

        boolean supportsNegativeAdjustments = definition != null
                && definition.toLowerCase().contains("sales_amount")
                && definition.toLowerCase().contains("<=")
                && definition.toLowerCase().contains("bonus_amount");

        if (!supportsNegativeAdjustments) {
            if (definition != null) {
                try (Statement st = con.createStatement()) {
                    st.executeUpdate("ALTER TABLE dbo.sales_bonuses DROP CONSTRAINT CK_sales_bonuses_values");
                }
            }
            try (Statement st = con.createStatement()) {
                st.executeUpdate(
                    "ALTER TABLE dbo.sales_bonuses ADD CONSTRAINT CK_sales_bonuses_values CHECK (" +
                    "bonus_rate >= 0 AND ((sales_amount >= 0 AND bonus_amount >= 0) " +
                    "OR (sales_amount <= 0 AND bonus_amount <= 0)))"
                );
            }
        }
    }
    /**
     * Bổ sung actor ADMIN và tài khoản Admin cho database đang dùng từ phiên bản cũ.
     * Đây là migration dữ liệu idempotent: không reset mật khẩu hay quyền đã tồn tại.
     */
    private static void ensureAdminActor(Connection con) throws SQLException {
        boolean baseTablesExist;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT CASE WHEN OBJECT_ID(N'dbo.roles',N'U') IS NOT NULL " +
                 "AND OBJECT_ID(N'dbo.permissions',N'U') IS NOT NULL " +
                 "AND OBJECT_ID(N'dbo.role_permissions',N'U') IS NOT NULL " +
                 "AND OBJECT_ID(N'dbo.employees',N'U') IS NOT NULL " +
                 "AND OBJECT_ID(N'dbo.accounts',N'U') IS NOT NULL THEN 1 ELSE 0 END")) {
            baseTablesExist = rs.next() && rs.getInt(1) == 1;
        }
        if (!baseTablesExist)
            throw new SQLException("Thiếu bảng tài khoản/phân quyền. Hãy chạy bộ SQL 01 -> 04 trước khi mở ứng dụng.");

        try (Statement st = con.createStatement()) {
            st.executeUpdate(
                "IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE role_code='ADMIN') " +
                "INSERT INTO dbo.roles(role_code,role_name,description) " +
                "VALUES('ADMIN',N'Admin',N'Quản trị viên phát triển - toàn quyền kiểm thử và phân quyền chức năng')"
            );
            st.executeUpdate(
                "UPDATE dbo.roles SET role_name=N'Admin', " +
                "description=N'Quản trị viên phát triển - toàn quyền kiểm thử và phân quyền chức năng' " +
                "WHERE role_code='ADMIN'"
            );
            st.executeUpdate(
                "IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE permission_code='ROLE_PERMISSION_MANAGE') " +
                "INSERT INTO dbo.permissions(permission_code,permission_name,module_code,description) " +
                "VALUES('ROLE_PERMISSION_MANAGE',N'Phân quyền chức năng theo actor','ADMIN',N'Chỉ dành cho actor Admin')"
            );
            st.executeUpdate(
                "UPDATE dbo.permissions SET permission_name=N'Quản lý tài khoản' " +
                "WHERE permission_code='ACCOUNT_MANAGE'"
            );

            st.executeUpdate(
                "IF NOT EXISTS (SELECT 1 FROM dbo.employees WHERE employee_id='ADM-001') BEGIN " +
                "DECLARE @AdminPhone VARCHAR(20); " +
                "SELECT TOP 1 @AdminPhone=v.phone FROM (VALUES " +
                "('0999999999'),('0999999998'),('0999999997'),('0999999996'),('0999999995')) v(phone) " +
                "WHERE NOT EXISTS (SELECT 1 FROM dbo.employees e WHERE e.phone=v.phone); " +
                "IF @AdminPhone IS NULL THROW 51000, 'Không tìm được số điện thoại hệ thống trống cho ADM-001.', 1; " +
                "INSERT INTO dbo.employees(employee_id,full_name,phone,email,department,join_date,status) " +
                "VALUES('ADM-001',N'Quản trị hệ thống',@AdminPhone,'admin@dienmayblue.local',N'Hệ thống',CAST(GETDATE() AS DATE),'ACTIVE'); END"
            );

            st.executeUpdate(
                "DECLARE @AdminRole INT=(SELECT role_id FROM dbo.roles WHERE role_code='ADMIN'); " +
                "DELETE rp FROM dbo.role_permissions rp JOIN dbo.permissions p ON p.permission_id=rp.permission_id " +
                "WHERE rp.role_id=@AdminRole AND p.permission_code NOT IN ('ROLE_PERMISSION_MANAGE','PROFILE_VIEW','PASSWORD_CHANGE'); " +
                "INSERT INTO dbo.role_permissions(role_id,permission_id,granted) " +
                "SELECT @AdminRole,p.permission_id,1 FROM dbo.permissions p " +
                "WHERE p.permission_code IN ('ROLE_PERMISSION_MANAGE','PROFILE_VIEW','PASSWORD_CHANGE') " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.role_permissions rp " +
                "WHERE rp.role_id=@AdminRole AND rp.permission_id=p.permission_id); " +
                "UPDATE dbo.role_permissions SET granted=1 WHERE role_id=@AdminRole AND permission_id IN " +
                "(SELECT permission_id FROM dbo.permissions WHERE permission_code IN ('ROLE_PERMISSION_MANAGE','PROFILE_VIEW','PASSWORD_CHANGE'))"
            );

            st.executeUpdate(
                "DECLARE @AdminRole2 INT=(SELECT role_id FROM dbo.roles WHERE role_code='ADMIN'); " +
                "IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE employee_id='ADM-001') BEGIN " +
                "IF EXISTS (SELECT 1 FROM dbo.accounts WHERE username='admin') " +
                "THROW 51001, 'Tên đăng nhập admin đã tồn tại ở tài khoản khác. Hãy đổi username bị trùng trong bảng accounts rồi mở lại ứng dụng.', 1; " +
                "INSERT INTO dbo.accounts(employee_id,username,password_hash,role_id,status,first_login) " +
                "VALUES('ADM-001','admin',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Admin@1234'),2),@AdminRole2,'ACTIVE',0); END; " +
                "UPDATE dbo.accounts SET first_login=0, updated_at=SYSDATETIME() " +
                "WHERE employee_id='ADM-001' OR username='admin'"
            );
        }
    }

    /**
     * Permission model v2:
     * - ADMIN là superuser ở tầng ứng dụng.
     * - MANAGER / SALES / CUSTOMER_SERVICE không còn whitelist cứng theo actor.
     * - Lần nâng cấp đầu tiên xóa các quyền nghiệp vụ đã seed sẵn để Admin tự phân quyền từ đầu.
     * - PROFILE_VIEW và PASSWORD_CHANGE vẫn là quyền tài khoản bắt buộc.
     */
    private static void ensureFlexiblePermissionModel(Connection con) throws SQLException {
        boolean settingsExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT CASE WHEN OBJECT_ID(N'dbo.system_settings',N'U') IS NULL THEN 0 ELSE 1 END")) {
            settingsExists = rs.next() && rs.getInt(1) == 1;
        }
        if (!settingsExists) return;

        boolean initialized;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM dbo.system_settings WHERE setting_key='PERMISSION_MODEL_V2_INITIALIZED'")) {
            initialized = rs.next() && rs.getInt(1) > 0;
        }
        if (initialized) return;

        try (Statement st = con.createStatement()) {
            st.executeUpdate(
                "DELETE rp FROM dbo.role_permissions rp " +
                "JOIN dbo.roles r ON r.role_id=rp.role_id " +
                "JOIN dbo.permissions p ON p.permission_id=rp.permission_id " +
                "WHERE r.role_code IN ('MANAGER','SALES','CUSTOMER_SERVICE') " +
                "AND p.permission_code NOT IN ('PROFILE_VIEW','PASSWORD_CHANGE')"
            );
            st.executeUpdate(
                "INSERT INTO dbo.role_permissions(role_id,permission_id,granted) " +
                "SELECT r.role_id,p.permission_id,1 FROM dbo.roles r CROSS JOIN dbo.permissions p " +
                "WHERE r.role_code IN ('MANAGER','SALES','CUSTOMER_SERVICE') " +
                "AND p.permission_code IN ('PROFILE_VIEW','PASSWORD_CHANGE') " +
                "AND NOT EXISTS (SELECT 1 FROM dbo.role_permissions rp " +
                "WHERE rp.role_id=r.role_id AND rp.permission_id=p.permission_id)"
            );
            st.executeUpdate(
                "UPDATE rp SET granted=1 FROM dbo.role_permissions rp " +
                "JOIN dbo.roles r ON r.role_id=rp.role_id " +
                "JOIN dbo.permissions p ON p.permission_id=rp.permission_id " +
                "WHERE r.role_code IN ('MANAGER','SALES','CUSTOMER_SERVICE') " +
                "AND p.permission_code IN ('PROFILE_VIEW','PASSWORD_CHANGE')"
            );
            st.executeUpdate(
                "INSERT INTO dbo.system_settings(setting_key,decimal_value,text_value,description) " +
                "VALUES('PERMISSION_MODEL_V2_INITIALIZED',1,N'ADMIN_SUPERUSER_FLEXIBLE_ROLES'," +
                "N'Đã chuyển sang mô hình Admin toàn quyền và actor tự được phân quyền từ danh mục đầy đủ')"
            );
        }
    }

    /**
     * Permission defaults v3:
     * - Vẫn hiển thị đầy đủ danh mục chức năng cho cả 3 actor trong màn Admin.
     * - Tick sẵn đúng bộ quyền nghiệp vụ đã dùng trước khi chuyển sang Permission Model V2.
     * - Chỉ seed đúng một lần để các thay đổi về sau của Admin không bị ghi đè.
     */
    private static void ensureDefaultActorPermissions(Connection con) throws SQLException {
        boolean settingsExists;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT CASE WHEN OBJECT_ID(N'dbo.system_settings',N'U') IS NULL THEN 0 ELSE 1 END")) {
            settingsExists = rs.next() && rs.getInt(1) == 1;
        }
        if (!settingsExists) return;

        boolean initialized;
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM dbo.system_settings WHERE setting_key='PERMISSION_DEFAULTS_V3_INITIALIZED'")) {
            initialized = rs.next() && rs.getInt(1) > 0;
        }
        if (initialized) return;

        try (Statement st = con.createStatement()) {
            st.executeUpdate(
                "INSERT INTO dbo.role_permissions(role_id,permission_id,granted) " +
                "SELECT r.role_id,p.permission_id,1 FROM dbo.roles r JOIN dbo.permissions p ON 1=1 " +
                "WHERE (" +
                "(r.role_code='MANAGER' AND p.permission_code IN (" +
                "'DASHBOARD_VIEW','EMPLOYEE_MANAGE','ACCOUNT_MANAGE','SHIFT_MANAGE','REFUND_APPROVE'," +
                "'STATISTICS_VIEW','BONUS_FINALIZE','PRODUCT_MANAGE','SUPPLIER_MANAGE','PROMOTION_MANAGE'," +
                "'MEMBERSHIP_MANAGE','POINT_MANAGE','PROFILE_VIEW','PASSWORD_CHANGE')) OR " +
                "(r.role_code='SALES' AND p.permission_code IN (" +
                "'DASHBOARD_VIEW','STATISTICS_VIEW','PRODUCT_SEARCH','SALES_MANAGE','INVOICE_CREATE'," +
                "'DELIVERY_CREATE','RETURN_CREATE','CUSTOMER_MANAGE','PERSONAL_SALES_VIEW'," +
                "'DELIVERY_MANAGE','DELIVERY_ASSIGN','PROFILE_VIEW','PASSWORD_CHANGE')) OR " +
                "(r.role_code='CUSTOMER_SERVICE' AND p.permission_code IN (" +
                "'DASHBOARD_VIEW','PERSONAL_SALES_VIEW','WARRANTY_MANAGE','COMPLAINT_MANAGE'," +
                "'PROFILE_VIEW','PASSWORD_CHANGE'))" +
                ") AND NOT EXISTS (SELECT 1 FROM dbo.role_permissions rp " +
                "WHERE rp.role_id=r.role_id AND rp.permission_id=p.permission_id)"
            );
            st.executeUpdate(
                "UPDATE rp SET granted=1 FROM dbo.role_permissions rp " +
                "JOIN dbo.roles r ON r.role_id=rp.role_id " +
                "JOIN dbo.permissions p ON p.permission_id=rp.permission_id " +
                "WHERE (" +
                "(r.role_code='MANAGER' AND p.permission_code IN (" +
                "'DASHBOARD_VIEW','EMPLOYEE_MANAGE','ACCOUNT_MANAGE','SHIFT_MANAGE','REFUND_APPROVE'," +
                "'STATISTICS_VIEW','BONUS_FINALIZE','PRODUCT_MANAGE','SUPPLIER_MANAGE','PROMOTION_MANAGE'," +
                "'MEMBERSHIP_MANAGE','POINT_MANAGE','PROFILE_VIEW','PASSWORD_CHANGE')) OR " +
                "(r.role_code='SALES' AND p.permission_code IN (" +
                "'DASHBOARD_VIEW','STATISTICS_VIEW','PRODUCT_SEARCH','SALES_MANAGE','INVOICE_CREATE'," +
                "'DELIVERY_CREATE','RETURN_CREATE','CUSTOMER_MANAGE','PERSONAL_SALES_VIEW'," +
                "'DELIVERY_MANAGE','DELIVERY_ASSIGN','PROFILE_VIEW','PASSWORD_CHANGE')) OR " +
                "(r.role_code='CUSTOMER_SERVICE' AND p.permission_code IN (" +
                "'DASHBOARD_VIEW','PERSONAL_SALES_VIEW','WARRANTY_MANAGE','COMPLAINT_MANAGE'," +
                "'PROFILE_VIEW','PASSWORD_CHANGE'))" +
                ")"
            );
            st.executeUpdate(
                "INSERT INTO dbo.system_settings(setting_key,decimal_value,text_value,description) " +
                "VALUES('PERMISSION_DEFAULTS_V3_INITIALIZED',1,N'LEGACY_ACTOR_DEFAULTS'," +
                "N'Đã tick sẵn bộ quyền mặc định ban đầu cho Manager, Sales và CSKH; Admin vẫn có thể thay đổi sau đó')"
            );
        }
    }

}

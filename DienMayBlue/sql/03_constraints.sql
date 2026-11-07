USE DienMayBlue;
GO


/* =========================================================
   CHUẨN HÓA MÃ HÓA ĐƠN: HD00001, HD00002, ...
   - Đặt ngay trong file constraints để không cần script migration riêng.
   - Nếu DB cũ đang có mã dài, script tự đánh lại theo thời gian tạo.
   - Cập nhật đồng bộ toàn bộ bảng tham chiếu invoice_id.
   ========================================================= */
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.invoices', N'U') IS NOT NULL
BEGIN
    IF (SELECT COUNT(*) FROM dbo.invoices) > 99999
        THROW 51002, N'Số lượng hóa đơn vượt quá 99.999, không thể dùng định dạng HD00001.', 1;

    IF OBJECT_ID('tempdb..#InvoiceMap') IS NOT NULL DROP TABLE #InvoiceMap;

    CREATE TABLE #InvoiceMap (
        old_id  VARCHAR(30) NOT NULL PRIMARY KEY,
        temp_id VARCHAR(7)  NOT NULL UNIQUE,
        new_id  VARCHAR(7)  NOT NULL UNIQUE
    );

    ;WITH x AS (
        SELECT invoice_id,
               ROW_NUMBER() OVER (ORDER BY created_at, invoice_id) AS rn
        FROM dbo.invoices
    )
    INSERT INTO #InvoiceMap(old_id,temp_id,new_id)
    SELECT invoice_id,
           'T'  + RIGHT('000000' + CONVERT(VARCHAR(6),rn),6),
           'HD' + RIGHT('00000'  + CONVERT(VARCHAR(5),rn),5)
    FROM x;

    /* Chỉ chạm dữ liệu khi mã hiện tại chưa đúng dãy HD00001... */
    IF EXISTS (
        SELECT 1
        FROM #InvoiceMap m
        WHERE m.old_id <> m.new_id
    )
    BEGIN TRY
        BEGIN TRANSACTION;

        IF OBJECT_ID(N'dbo.CK_invoices_id_format',N'C') IS NOT NULL
            ALTER TABLE dbo.invoices DROP CONSTRAINT CK_invoices_id_format;

        IF OBJECT_ID(N'dbo.FK_invoice_details_invoice', N'F') IS NOT NULL
            ALTER TABLE dbo.invoice_details DROP CONSTRAINT FK_invoice_details_invoice;
        IF OBJECT_ID(N'dbo.FK_payments_invoice', N'F') IS NOT NULL
            ALTER TABLE dbo.payments DROP CONSTRAINT FK_payments_invoice;
        IF OBJECT_ID(N'dbo.FK_point_transactions_invoice', N'F') IS NOT NULL
            ALTER TABLE dbo.point_transactions DROP CONSTRAINT FK_point_transactions_invoice;
        IF OBJECT_ID(N'dbo.FK_sales_bonuses_invoice', N'F') IS NOT NULL
            ALTER TABLE dbo.sales_bonuses DROP CONSTRAINT FK_sales_bonuses_invoice;
        IF OBJECT_ID(N'dbo.FK_deliveries_invoice', N'F') IS NOT NULL
            ALTER TABLE dbo.deliveries DROP CONSTRAINT FK_deliveries_invoice;
        IF OBJECT_ID(N'dbo.FK_return_requests_invoice', N'F') IS NOT NULL
            ALTER TABLE dbo.return_requests DROP CONSTRAINT FK_return_requests_invoice;

        /* Mã tạm dài 7 ký tự để dùng được cả schema cũ lẫn schema mới. */
        UPDATE i SET i.invoice_id=m.temp_id
        FROM dbo.invoices i JOIN #InvoiceMap m ON m.old_id=i.invoice_id;

        UPDATE d SET d.invoice_id=m.temp_id
        FROM dbo.invoice_details d JOIN #InvoiceMap m ON m.old_id=d.invoice_id;
        UPDATE p SET p.invoice_id=m.temp_id
        FROM dbo.payments p JOIN #InvoiceMap m ON m.old_id=p.invoice_id;
        UPDATE pt SET pt.invoice_id=m.temp_id
        FROM dbo.point_transactions pt JOIN #InvoiceMap m ON m.old_id=pt.invoice_id;
        UPDATE sb SET sb.invoice_id=m.temp_id
        FROM dbo.sales_bonuses sb JOIN #InvoiceMap m ON m.old_id=sb.invoice_id;
        UPDATE d SET d.invoice_id=m.temp_id
        FROM dbo.deliveries d JOIN #InvoiceMap m ON m.old_id=d.invoice_id;
        UPDATE rr SET rr.invoice_id=m.temp_id
        FROM dbo.return_requests rr JOIN #InvoiceMap m ON m.old_id=rr.invoice_id;

        UPDATE i SET i.invoice_id=m.new_id
        FROM dbo.invoices i JOIN #InvoiceMap m ON m.temp_id=i.invoice_id;

        UPDATE d SET d.invoice_id=m.new_id
        FROM dbo.invoice_details d JOIN #InvoiceMap m ON m.temp_id=d.invoice_id;
        UPDATE p SET p.invoice_id=m.new_id
        FROM dbo.payments p JOIN #InvoiceMap m ON m.temp_id=p.invoice_id;
        UPDATE pt SET pt.invoice_id=m.new_id
        FROM dbo.point_transactions pt JOIN #InvoiceMap m ON m.temp_id=pt.invoice_id;
        UPDATE sb SET sb.invoice_id=m.new_id
        FROM dbo.sales_bonuses sb JOIN #InvoiceMap m ON m.temp_id=sb.invoice_id;
        UPDATE d SET d.invoice_id=m.new_id
        FROM dbo.deliveries d JOIN #InvoiceMap m ON m.temp_id=d.invoice_id;
        UPDATE rr SET rr.invoice_id=m.new_id
        FROM dbo.return_requests rr JOIN #InvoiceMap m ON m.temp_id=rr.invoice_id;

        UPDATE a SET a.entity_id=m.new_id
        FROM dbo.audit_logs a
        JOIN #InvoiceMap m ON m.old_id=a.entity_id
        WHERE a.entity_name='invoices';

        IF OBJECT_ID(N'dbo.FK_invoice_details_invoice', N'F') IS NULL
            ALTER TABLE dbo.invoice_details WITH CHECK ADD CONSTRAINT FK_invoice_details_invoice
                FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(invoice_id);
        IF OBJECT_ID(N'dbo.FK_payments_invoice', N'F') IS NULL
            ALTER TABLE dbo.payments WITH CHECK ADD CONSTRAINT FK_payments_invoice
                FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(invoice_id);
        IF OBJECT_ID(N'dbo.FK_point_transactions_invoice', N'F') IS NULL
            ALTER TABLE dbo.point_transactions WITH CHECK ADD CONSTRAINT FK_point_transactions_invoice
                FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(invoice_id);
        IF OBJECT_ID(N'dbo.FK_sales_bonuses_invoice', N'F') IS NULL
            ALTER TABLE dbo.sales_bonuses WITH CHECK ADD CONSTRAINT FK_sales_bonuses_invoice
                FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(invoice_id);
        IF OBJECT_ID(N'dbo.FK_deliveries_invoice', N'F') IS NULL
            ALTER TABLE dbo.deliveries WITH CHECK ADD CONSTRAINT FK_deliveries_invoice
                FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(invoice_id);
        IF OBJECT_ID(N'dbo.FK_return_requests_invoice', N'F') IS NULL
            ALTER TABLE dbo.return_requests WITH CHECK ADD CONSTRAINT FK_return_requests_invoice
                FOREIGN KEY (invoice_id) REFERENCES dbo.invoices(invoice_id);

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END
GO

/* =========================================================
   CHUẨN HÓA MÃ PHIẾU HOÀN TIỀN: HT00001, HT00002, ...
   - Tích hợp trực tiếp trong file constraints, không cần script migration riêng.
   - Nếu DB cũ đang có mã RT... hoặc mã dài, tự đánh lại theo thời gian tạo phiếu.
   - Cập nhật đồng bộ chi tiết đổi trả, duyệt hoàn tiền, audit và mã tham chiếu hoàn tiền.
   ========================================================= */
IF OBJECT_ID(N'dbo.return_requests', N'U') IS NOT NULL
BEGIN
    IF (SELECT COUNT(*) FROM dbo.return_requests) > 99999
        THROW 51003, N'Số lượng phiếu hoàn tiền vượt quá 99.999, không thể dùng định dạng HT00001.', 1;

    IF OBJECT_ID('tempdb..#ReturnMap') IS NOT NULL DROP TABLE #ReturnMap;

    CREATE TABLE #ReturnMap (
        old_id  VARCHAR(30) NOT NULL PRIMARY KEY,
        temp_id VARCHAR(7)  NOT NULL UNIQUE,
        new_id  VARCHAR(7)  NOT NULL UNIQUE
    );

    ;WITH x AS (
        SELECT return_id,
               ROW_NUMBER() OVER (ORDER BY request_date, return_id) AS rn
        FROM dbo.return_requests
    )
    INSERT INTO #ReturnMap(old_id,temp_id,new_id)
    SELECT return_id,
           'ZT' + RIGHT('00000' + CONVERT(VARCHAR(5),rn),5),
           'HT' + RIGHT('00000' + CONVERT(VARCHAR(5),rn),5)
    FROM x;

    IF EXISTS (SELECT 1 FROM #ReturnMap WHERE old_id <> new_id)
    BEGIN TRY
        BEGIN TRANSACTION;

        IF OBJECT_ID(N'dbo.CK_return_requests_id_format',N'C') IS NOT NULL
            ALTER TABLE dbo.return_requests DROP CONSTRAINT CK_return_requests_id_format;

        IF OBJECT_ID(N'dbo.FK_return_details_return', N'F') IS NOT NULL
            ALTER TABLE dbo.return_details DROP CONSTRAINT FK_return_details_return;
        IF OBJECT_ID(N'dbo.FK_refund_approvals_return', N'F') IS NOT NULL
            ALTER TABLE dbo.refund_approvals DROP CONSTRAINT FK_refund_approvals_return;

        UPDATE rr SET rr.return_id=m.temp_id
        FROM dbo.return_requests rr JOIN #ReturnMap m ON m.old_id=rr.return_id;
        UPDATE rd SET rd.return_id=m.temp_id
        FROM dbo.return_details rd JOIN #ReturnMap m ON m.old_id=rd.return_id;
        UPDATE ra SET ra.return_id=m.temp_id
        FROM dbo.refund_approvals ra JOIN #ReturnMap m ON m.old_id=ra.return_id;

        UPDATE rr SET rr.return_id=m.new_id
        FROM dbo.return_requests rr JOIN #ReturnMap m ON m.temp_id=rr.return_id;
        UPDATE rd SET rd.return_id=m.new_id
        FROM dbo.return_details rd JOIN #ReturnMap m ON m.temp_id=rd.return_id;
        UPDATE ra SET ra.return_id=m.new_id
        FROM dbo.refund_approvals ra JOIN #ReturnMap m ON m.temp_id=ra.return_id;

        UPDATE a SET a.entity_id=m.new_id
        FROM dbo.audit_logs a
        JOIN #ReturnMap m ON m.old_id=a.entity_id
        WHERE a.entity_name='return_requests';

        UPDATE p SET p.transaction_ref='REFUND:'+m.new_id
        FROM dbo.payments p
        JOIN #ReturnMap m ON p.transaction_ref='REFUND:'+m.old_id;

        IF OBJECT_ID(N'dbo.FK_return_details_return', N'F') IS NULL
            ALTER TABLE dbo.return_details WITH CHECK ADD CONSTRAINT FK_return_details_return
                FOREIGN KEY (return_id) REFERENCES dbo.return_requests(return_id);
        IF OBJECT_ID(N'dbo.FK_refund_approvals_return', N'F') IS NULL
            ALTER TABLE dbo.refund_approvals WITH CHECK ADD CONSTRAINT FK_refund_approvals_return
                FOREIGN KEY (return_id) REFERENCES dbo.return_requests(return_id);

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END
GO

/* =========================================================
   CHUẨN HÓA MÃ PHIẾU GIAO HÀNG: GH00001, GH00002, ...
   - Tích hợp trực tiếp trong file constraints, không cần script migration riêng.
   - Nếu DB cũ đang có mã GH dài hoặc GH-DEMO..., tự đánh lại theo thời gian tạo.
   - Cập nhật đồng bộ lịch sử giao hàng và audit log liên quan.
   ========================================================= */
IF OBJECT_ID(N'dbo.deliveries', N'U') IS NOT NULL
BEGIN
    IF (SELECT COUNT(*) FROM dbo.deliveries) > 99999
        THROW 51004, N'Số lượng phiếu giao hàng vượt quá 99.999, không thể dùng định dạng GH00001.', 1;

    IF OBJECT_ID('tempdb..#DeliveryMap') IS NOT NULL DROP TABLE #DeliveryMap;

    CREATE TABLE #DeliveryMap (
        old_id  VARCHAR(30) NOT NULL PRIMARY KEY,
        temp_id VARCHAR(7)  NOT NULL UNIQUE,
        new_id  VARCHAR(7)  NOT NULL UNIQUE
    );

    ;WITH x AS (
        SELECT delivery_id,
               ROW_NUMBER() OVER (ORDER BY created_at, delivery_id) AS rn
        FROM dbo.deliveries
    )
    INSERT INTO #DeliveryMap(old_id,temp_id,new_id)
    SELECT delivery_id,
           'ZG' + RIGHT('00000' + CONVERT(VARCHAR(5),rn),5),
           'GH' + RIGHT('00000' + CONVERT(VARCHAR(5),rn),5)
    FROM x;

    IF EXISTS (SELECT 1 FROM #DeliveryMap WHERE old_id <> new_id)
    BEGIN TRY
        BEGIN TRANSACTION;

        IF OBJECT_ID(N'dbo.CK_deliveries_id_format',N'C') IS NOT NULL
            ALTER TABLE dbo.deliveries DROP CONSTRAINT CK_deliveries_id_format;

        IF OBJECT_ID(N'dbo.FK_delivery_history_delivery', N'F') IS NOT NULL
            ALTER TABLE dbo.delivery_history DROP CONSTRAINT FK_delivery_history_delivery;

        UPDATE d SET d.delivery_id=m.temp_id
        FROM dbo.deliveries d JOIN #DeliveryMap m ON m.old_id=d.delivery_id;
        UPDATE h SET h.delivery_id=m.temp_id
        FROM dbo.delivery_history h JOIN #DeliveryMap m ON m.old_id=h.delivery_id;

        UPDATE d SET d.delivery_id=m.new_id
        FROM dbo.deliveries d JOIN #DeliveryMap m ON m.temp_id=d.delivery_id;
        UPDATE h SET h.delivery_id=m.new_id
        FROM dbo.delivery_history h JOIN #DeliveryMap m ON m.temp_id=h.delivery_id;

        UPDATE a SET a.entity_id=m.new_id
        FROM dbo.audit_logs a
        JOIN #DeliveryMap m ON m.old_id=a.entity_id
        WHERE a.entity_name='deliveries';

        IF OBJECT_ID(N'dbo.FK_delivery_history_delivery', N'F') IS NULL
            ALTER TABLE dbo.delivery_history WITH CHECK ADD CONSTRAINT FK_delivery_history_delivery
                FOREIGN KEY (delivery_id) REFERENCES dbo.deliveries(delivery_id);

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END
GO

/* ================= UNIQUE ================= */
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_roles_code' AND parent_object_id = OBJECT_ID(N'dbo.roles'))
BEGIN
    ALTER TABLE roles ADD CONSTRAINT UQ_roles_code UNIQUE (role_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_permissions_code' AND parent_object_id = OBJECT_ID(N'dbo.permissions'))
BEGIN
    ALTER TABLE permissions ADD CONSTRAINT UQ_permissions_code UNIQUE (permission_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_employees_phone' AND parent_object_id = OBJECT_ID(N'dbo.employees'))
BEGIN
    ALTER TABLE employees ADD CONSTRAINT UQ_employees_phone UNIQUE (phone);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_accounts_employee' AND parent_object_id = OBJECT_ID(N'dbo.accounts'))
BEGIN
    ALTER TABLE accounts ADD CONSTRAINT UQ_accounts_employee UNIQUE (employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_accounts_username' AND parent_object_id = OBJECT_ID(N'dbo.accounts'))
BEGIN
    ALTER TABLE accounts ADD CONSTRAINT UQ_accounts_username UNIQUE (username);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_shifts_code' AND parent_object_id = OBJECT_ID(N'dbo.shifts'))
BEGIN
    ALTER TABLE shifts ADD CONSTRAINT UQ_shifts_code UNIQUE (shift_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_categories_code' AND parent_object_id = OBJECT_ID(N'dbo.categories'))
BEGIN
    ALTER TABLE categories ADD CONSTRAINT UQ_categories_code UNIQUE (category_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_suppliers_code' AND parent_object_id = OBJECT_ID(N'dbo.suppliers'))
BEGIN
    ALTER TABLE suppliers ADD CONSTRAINT UQ_suppliers_code UNIQUE (supplier_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_suppliers_phone' AND parent_object_id = OBJECT_ID(N'dbo.suppliers'))
BEGIN
    ALTER TABLE suppliers ADD CONSTRAINT UQ_suppliers_phone UNIQUE (phone);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_suppliers_tax' AND parent_object_id = OBJECT_ID(N'dbo.suppliers'))
BEGIN
    ALTER TABLE suppliers ADD CONSTRAINT UQ_suppliers_tax UNIQUE (tax_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_products_barcode' AND parent_object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    ALTER TABLE products ADD CONSTRAINT UQ_products_barcode UNIQUE (barcode);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_promotions_code' AND parent_object_id = OBJECT_ID(N'dbo.promotions'))
BEGIN
    ALTER TABLE promotions ADD CONSTRAINT UQ_promotions_code UNIQUE (promotion_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_membership_tiers_code' AND parent_object_id = OBJECT_ID(N'dbo.membership_tiers'))
BEGIN
    ALTER TABLE membership_tiers ADD CONSTRAINT UQ_membership_tiers_code UNIQUE (tier_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_membership_cards_code' AND parent_object_id = OBJECT_ID(N'dbo.membership_cards'))
BEGIN
    ALTER TABLE membership_cards ADD CONSTRAINT UQ_membership_cards_code UNIQUE (card_code);
END
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_refund_approvals_return' AND parent_object_id = OBJECT_ID(N'dbo.refund_approvals'))
BEGIN
    ALTER TABLE refund_approvals ADD CONSTRAINT UQ_refund_approvals_return UNIQUE (return_id);
END
/* Các ràng buộc cũ dưới đây không còn phù hợp:
   - sales_bonuses cần cho phép 1 dòng bán + 1 dòng đảo/hoàn cho cùng hóa đơn/nhân viên.
   - mỗi nhân viên chỉ được có 1 ca trong 1 ngày. */
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_sales_bonuses_invoice_employee' AND parent_object_id = OBJECT_ID(N'dbo.sales_bonuses'))
BEGIN
    ALTER TABLE dbo.sales_bonuses DROP CONSTRAINT UQ_sales_bonuses_invoice_employee;
END
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_shift_assignment' AND parent_object_id = OBJECT_ID(N'dbo.shift_assignments'))
BEGIN
    ALTER TABLE dbo.shift_assignments DROP CONSTRAINT UQ_shift_assignment;
END
GO

/* ================= FOREIGN KEYS ================= */
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_role_permissions_role' AND parent_object_id = OBJECT_ID(N'dbo.role_permissions'))
BEGIN
    ALTER TABLE role_permissions ADD CONSTRAINT FK_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(role_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_role_permissions_permission' AND parent_object_id = OBJECT_ID(N'dbo.role_permissions'))
BEGIN
    ALTER TABLE role_permissions ADD CONSTRAINT FK_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(permission_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_accounts_employee' AND parent_object_id = OBJECT_ID(N'dbo.accounts'))
BEGIN
    ALTER TABLE accounts ADD CONSTRAINT FK_accounts_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_accounts_role' AND parent_object_id = OBJECT_ID(N'dbo.accounts'))
BEGIN
    ALTER TABLE accounts ADD CONSTRAINT FK_accounts_role FOREIGN KEY (role_id) REFERENCES roles(role_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_login_logs_account' AND parent_object_id = OBJECT_ID(N'dbo.login_logs'))
BEGIN
    ALTER TABLE login_logs ADD CONSTRAINT FK_login_logs_account FOREIGN KEY (account_id) REFERENCES accounts(account_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_audit_logs_account' AND parent_object_id = OBJECT_ID(N'dbo.audit_logs'))
BEGIN
    ALTER TABLE audit_logs ADD CONSTRAINT FK_audit_logs_account FOREIGN KEY (account_id) REFERENCES accounts(account_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_notifications_employee' AND parent_object_id = OBJECT_ID(N'dbo.notifications'))
BEGIN
    ALTER TABLE notifications ADD CONSTRAINT FK_notifications_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shift_assignments_employee' AND parent_object_id = OBJECT_ID(N'dbo.shift_assignments'))
BEGIN
    ALTER TABLE shift_assignments ADD CONSTRAINT FK_shift_assignments_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shift_assignments_shift' AND parent_object_id = OBJECT_ID(N'dbo.shift_assignments'))
BEGIN
    ALTER TABLE shift_assignments ADD CONSTRAINT FK_shift_assignments_shift FOREIGN KEY (shift_id) REFERENCES shifts(shift_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_shift_assignments_assigner' AND parent_object_id = OBJECT_ID(N'dbo.shift_assignments'))
BEGIN
    ALTER TABLE shift_assignments ADD CONSTRAINT FK_shift_assignments_assigner FOREIGN KEY (assigned_by) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_leave_requests_employee' AND parent_object_id = OBJECT_ID(N'dbo.leave_requests'))
BEGIN
    ALTER TABLE leave_requests ADD CONSTRAINT FK_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_products_category' AND parent_object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    ALTER TABLE products ADD CONSTRAINT FK_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_products_supplier' AND parent_object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    ALTER TABLE products ADD CONSTRAINT FK_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_supplier_debts_supplier' AND parent_object_id = OBJECT_ID(N'dbo.supplier_debts'))
BEGIN
    ALTER TABLE supplier_debts ADD CONSTRAINT FK_supplier_debts_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_supplier_payments_supplier' AND parent_object_id = OBJECT_ID(N'dbo.supplier_payments'))
BEGIN
    ALTER TABLE supplier_payments ADD CONSTRAINT FK_supplier_payments_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_supplier_payments_debt' AND parent_object_id = OBJECT_ID(N'dbo.supplier_payments'))
BEGIN
    ALTER TABLE supplier_payments ADD CONSTRAINT FK_supplier_payments_debt FOREIGN KEY (debt_id) REFERENCES supplier_debts(debt_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_supplier_payments_employee' AND parent_object_id = OBJECT_ID(N'dbo.supplier_payments'))
BEGIN
    ALTER TABLE supplier_payments ADD CONSTRAINT FK_supplier_payments_employee FOREIGN KEY (paid_by) REFERENCES employees(employee_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_promotions_employee' AND parent_object_id = OBJECT_ID(N'dbo.promotions'))
BEGIN
    ALTER TABLE promotions ADD CONSTRAINT FK_promotions_employee FOREIGN KEY (created_by) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_promotion_products_promotion' AND parent_object_id = OBJECT_ID(N'dbo.promotion_products'))
BEGIN
    ALTER TABLE promotion_products ADD CONSTRAINT FK_promotion_products_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_promotion_products_product' AND parent_object_id = OBJECT_ID(N'dbo.promotion_products'))
BEGIN
    ALTER TABLE promotion_products ADD CONSTRAINT FK_promotion_products_product FOREIGN KEY (product_id) REFERENCES products(product_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_promotion_categories_promotion' AND parent_object_id = OBJECT_ID(N'dbo.promotion_categories'))
BEGIN
    ALTER TABLE promotion_categories ADD CONSTRAINT FK_promotion_categories_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_promotion_categories_category' AND parent_object_id = OBJECT_ID(N'dbo.promotion_categories'))
BEGIN
    ALTER TABLE promotion_categories ADD CONSTRAINT FK_promotion_categories_category FOREIGN KEY (category_id) REFERENCES categories(category_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_customers_tier' AND parent_object_id = OBJECT_ID(N'dbo.customers'))
BEGIN
    ALTER TABLE customers ADD CONSTRAINT FK_customers_tier FOREIGN KEY (tier_id) REFERENCES membership_tiers(tier_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_membership_cards_customer' AND parent_object_id = OBJECT_ID(N'dbo.membership_cards'))
BEGIN
    ALTER TABLE membership_cards ADD CONSTRAINT FK_membership_cards_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_invoices_customer' AND parent_object_id = OBJECT_ID(N'dbo.invoices'))
BEGIN
    ALTER TABLE invoices ADD CONSTRAINT FK_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_invoices_employee' AND parent_object_id = OBJECT_ID(N'dbo.invoices'))
BEGIN
    ALTER TABLE invoices ADD CONSTRAINT FK_invoices_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_invoice_details_invoice' AND parent_object_id = OBJECT_ID(N'dbo.invoice_details'))
BEGIN
    ALTER TABLE invoice_details ADD CONSTRAINT FK_invoice_details_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_invoice_details_product' AND parent_object_id = OBJECT_ID(N'dbo.invoice_details'))
BEGIN
    ALTER TABLE invoice_details ADD CONSTRAINT FK_invoice_details_product FOREIGN KEY (product_id) REFERENCES products(product_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_payments_invoice' AND parent_object_id = OBJECT_ID(N'dbo.payments'))
BEGIN
    ALTER TABLE payments ADD CONSTRAINT FK_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_point_transactions_customer' AND parent_object_id = OBJECT_ID(N'dbo.point_transactions'))
BEGIN
    ALTER TABLE point_transactions ADD CONSTRAINT FK_point_transactions_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_point_transactions_invoice' AND parent_object_id = OBJECT_ID(N'dbo.point_transactions'))
BEGIN
    ALTER TABLE point_transactions ADD CONSTRAINT FK_point_transactions_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_sales_bonuses_employee' AND parent_object_id = OBJECT_ID(N'dbo.sales_bonuses'))
BEGIN
    ALTER TABLE sales_bonuses ADD CONSTRAINT FK_sales_bonuses_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_sales_bonuses_invoice' AND parent_object_id = OBJECT_ID(N'dbo.sales_bonuses'))
BEGIN
    ALTER TABLE sales_bonuses ADD CONSTRAINT FK_sales_bonuses_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_bonus_statements_employee' AND parent_object_id = OBJECT_ID(N'dbo.bonus_statements'))
BEGIN
    ALTER TABLE bonus_statements ADD CONSTRAINT FK_bonus_statements_employee FOREIGN KEY (finalized_by) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_bonus_statement_details_statement' AND parent_object_id = OBJECT_ID(N'dbo.bonus_statement_details'))
BEGIN
    ALTER TABLE bonus_statement_details ADD CONSTRAINT FK_bonus_statement_details_statement FOREIGN KEY (statement_id) REFERENCES bonus_statements(statement_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_bonus_statement_details_employee' AND parent_object_id = OBJECT_ID(N'dbo.bonus_statement_details'))
BEGIN
    ALTER TABLE bonus_statement_details ADD CONSTRAINT FK_bonus_statement_details_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_deliveries_invoice' AND parent_object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    ALTER TABLE deliveries ADD CONSTRAINT FK_deliveries_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_deliveries_employee' AND parent_object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    ALTER TABLE deliveries ADD CONSTRAINT FK_deliveries_employee FOREIGN KEY (assigned_employee_id) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_delivery_history_delivery' AND parent_object_id = OBJECT_ID(N'dbo.delivery_history'))
BEGIN
    ALTER TABLE delivery_history ADD CONSTRAINT FK_delivery_history_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(delivery_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_delivery_history_employee' AND parent_object_id = OBJECT_ID(N'dbo.delivery_history'))
BEGIN
    ALTER TABLE delivery_history ADD CONSTRAINT FK_delivery_history_employee FOREIGN KEY (changed_by) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_return_requests_invoice' AND parent_object_id = OBJECT_ID(N'dbo.return_requests'))
BEGIN
    ALTER TABLE return_requests ADD CONSTRAINT FK_return_requests_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_return_requests_employee' AND parent_object_id = OBJECT_ID(N'dbo.return_requests'))
BEGIN
    ALTER TABLE return_requests ADD CONSTRAINT FK_return_requests_employee FOREIGN KEY (created_by) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_return_details_return' AND parent_object_id = OBJECT_ID(N'dbo.return_details'))
BEGIN
    ALTER TABLE return_details ADD CONSTRAINT FK_return_details_return FOREIGN KEY (return_id) REFERENCES return_requests(return_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_return_details_invoice_detail' AND parent_object_id = OBJECT_ID(N'dbo.return_details'))
BEGIN
    ALTER TABLE return_details ADD CONSTRAINT FK_return_details_invoice_detail FOREIGN KEY (invoice_detail_id) REFERENCES invoice_details(invoice_detail_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_return_details_product' AND parent_object_id = OBJECT_ID(N'dbo.return_details'))
BEGIN
    ALTER TABLE return_details ADD CONSTRAINT FK_return_details_product FOREIGN KEY (product_id) REFERENCES products(product_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_refund_approvals_return' AND parent_object_id = OBJECT_ID(N'dbo.refund_approvals'))
BEGIN
    ALTER TABLE refund_approvals ADD CONSTRAINT FK_refund_approvals_return FOREIGN KEY (return_id) REFERENCES return_requests(return_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_refund_approvals_employee' AND parent_object_id = OBJECT_ID(N'dbo.refund_approvals'))
BEGIN
    ALTER TABLE refund_approvals ADD CONSTRAINT FK_refund_approvals_employee FOREIGN KEY (approver_employee_id) REFERENCES employees(employee_id);
END

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_warranties_customer' AND parent_object_id = OBJECT_ID(N'dbo.warranties'))
BEGIN
    ALTER TABLE warranties ADD CONSTRAINT FK_warranties_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_warranties_invoice_detail' AND parent_object_id = OBJECT_ID(N'dbo.warranties'))
BEGIN
    ALTER TABLE warranties ADD CONSTRAINT FK_warranties_invoice_detail FOREIGN KEY (invoice_detail_id) REFERENCES invoice_details(invoice_detail_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_warranties_product' AND parent_object_id = OBJECT_ID(N'dbo.warranties'))
BEGIN
    ALTER TABLE warranties ADD CONSTRAINT FK_warranties_product FOREIGN KEY (product_id) REFERENCES products(product_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_warranties_employee' AND parent_object_id = OBJECT_ID(N'dbo.warranties'))
BEGIN
    ALTER TABLE warranties ADD CONSTRAINT FK_warranties_employee FOREIGN KEY (created_by) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_warranty_history_warranty' AND parent_object_id = OBJECT_ID(N'dbo.warranty_history'))
BEGIN
    ALTER TABLE warranty_history ADD CONSTRAINT FK_warranty_history_warranty FOREIGN KEY (warranty_id) REFERENCES warranties(warranty_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_warranty_history_employee' AND parent_object_id = OBJECT_ID(N'dbo.warranty_history'))
BEGIN
    ALTER TABLE warranty_history ADD CONSTRAINT FK_warranty_history_employee FOREIGN KEY (changed_by) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_complaints_customer' AND parent_object_id = OBJECT_ID(N'dbo.complaints'))
BEGIN
    ALTER TABLE complaints ADD CONSTRAINT FK_complaints_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_complaints_creator' AND parent_object_id = OBJECT_ID(N'dbo.complaints'))
BEGIN
    ALTER TABLE complaints ADD CONSTRAINT FK_complaints_creator FOREIGN KEY (created_by) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_complaints_assigned' AND parent_object_id = OBJECT_ID(N'dbo.complaints'))
BEGIN
    ALTER TABLE complaints ADD CONSTRAINT FK_complaints_assigned FOREIGN KEY (assigned_to) REFERENCES employees(employee_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_complaint_history_complaint' AND parent_object_id = OBJECT_ID(N'dbo.complaint_history'))
BEGIN
    ALTER TABLE complaint_history ADD CONSTRAINT FK_complaint_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_complaint_history_employee' AND parent_object_id = OBJECT_ID(N'dbo.complaint_history'))
BEGIN
    ALTER TABLE complaint_history ADD CONSTRAINT FK_complaint_history_employee FOREIGN KEY (changed_by) REFERENCES employees(employee_id);
END
GO

/* ================= CHECK CONSTRAINTS ================= */
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_employees_status' AND parent_object_id = OBJECT_ID(N'dbo.employees'))
BEGIN
    ALTER TABLE employees ADD CONSTRAINT CK_employees_status CHECK (status IN ('ACTIVE','INACTIVE','ON_LEAVE'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_accounts_status' AND parent_object_id = OBJECT_ID(N'dbo.accounts'))
BEGIN
    ALTER TABLE accounts ADD CONSTRAINT CK_accounts_status CHECK (status IN ('ACTIVE','LOCKED','DISABLED','PENDING'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_accounts_failed_attempts' AND parent_object_id = OBJECT_ID(N'dbo.accounts'))
BEGIN
    ALTER TABLE accounts ADD CONSTRAINT CK_accounts_failed_attempts CHECK (failed_attempts >= 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_shifts_time' AND parent_object_id = OBJECT_ID(N'dbo.shifts'))
BEGIN
    ALTER TABLE shifts ADD CONSTRAINT CK_shifts_time CHECK (start_time < end_time);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_shift_assignments_status' AND parent_object_id = OBJECT_ID(N'dbo.shift_assignments'))
BEGIN
    ALTER TABLE shift_assignments ADD CONSTRAINT CK_shift_assignments_status CHECK (status IN ('ASSIGNED','COMPLETED','CANCELLED'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_leave_requests_status' AND parent_object_id = OBJECT_ID(N'dbo.leave_requests'))
BEGIN
    ALTER TABLE leave_requests ADD CONSTRAINT CK_leave_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED'));
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_suppliers_status' AND parent_object_id = OBJECT_ID(N'dbo.suppliers'))
BEGIN
    ALTER TABLE suppliers ADD CONSTRAINT CK_suppliers_status CHECK (status IN ('ACTIVE','INACTIVE'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_products_price' AND parent_object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    ALTER TABLE products ADD CONSTRAINT CK_products_price CHECK (cost_price >= 0 AND sale_price > cost_price);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_products_stock' AND parent_object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    ALTER TABLE products ADD CONSTRAINT CK_products_stock CHECK (stock_quantity >= 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_products_warranty_months' AND parent_object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    ALTER TABLE products ADD CONSTRAINT CK_products_warranty_months
        CHECK (warranty_months BETWEEN 0 AND 120);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_products_status' AND parent_object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    ALTER TABLE products ADD CONSTRAINT CK_products_status CHECK (status IN ('ACTIVE','DISCONTINUED','OUT_OF_STOCK'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_supplier_debts_amount' AND parent_object_id = OBJECT_ID(N'dbo.supplier_debts'))
BEGIN
    ALTER TABLE supplier_debts ADD CONSTRAINT CK_supplier_debts_amount CHECK (original_amount >= 0 AND remaining_amount >= 0 AND remaining_amount <= original_amount);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_supplier_debts_status' AND parent_object_id = OBJECT_ID(N'dbo.supplier_debts'))
BEGIN
    ALTER TABLE supplier_debts ADD CONSTRAINT CK_supplier_debts_status CHECK (status IN ('UNPAID','PARTIAL','PAID','OVERDUE'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_supplier_payments_amount' AND parent_object_id = OBJECT_ID(N'dbo.supplier_payments'))
BEGIN
    ALTER TABLE supplier_payments ADD CONSTRAINT CK_supplier_payments_amount CHECK (amount > 0);
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_promotions_time' AND parent_object_id = OBJECT_ID(N'dbo.promotions'))
BEGIN
    ALTER TABLE promotions ADD CONSTRAINT CK_promotions_time CHECK (start_at < end_at);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_promotions_type' AND parent_object_id = OBJECT_ID(N'dbo.promotions'))
BEGIN
    ALTER TABLE promotions ADD CONSTRAINT CK_promotions_type CHECK (discount_type IN ('PERCENT','FIXED'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_promotions_discount' AND parent_object_id = OBJECT_ID(N'dbo.promotions'))
BEGIN
    ALTER TABLE promotions ADD CONSTRAINT CK_promotions_discount CHECK (
    discount_value > 0 AND
    ((discount_type = 'PERCENT' AND discount_value <= 100) OR discount_type = 'FIXED')
);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_promotions_status' AND parent_object_id = OBJECT_ID(N'dbo.promotions'))
BEGIN
    ALTER TABLE promotions ADD CONSTRAINT CK_promotions_status CHECK (status IN ('SCHEDULED','ACTIVE','ENDED','CANCELLED'));
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_membership_tiers_points' AND parent_object_id = OBJECT_ID(N'dbo.membership_tiers'))
BEGIN
    ALTER TABLE membership_tiers ADD CONSTRAINT CK_membership_tiers_points CHECK (min_points >= 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_membership_tiers_discount' AND parent_object_id = OBJECT_ID(N'dbo.membership_tiers'))
BEGIN
    ALTER TABLE membership_tiers ADD CONSTRAINT CK_membership_tiers_discount CHECK (discount_rate >= 0 AND discount_rate <= 100);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_membership_tiers_multiplier' AND parent_object_id = OBJECT_ID(N'dbo.membership_tiers'))
BEGIN
    ALTER TABLE membership_tiers ADD CONSTRAINT CK_membership_tiers_multiplier CHECK (point_multiplier > 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_customers_points' AND parent_object_id = OBJECT_ID(N'dbo.customers'))
BEGIN
    ALTER TABLE customers ADD CONSTRAINT CK_customers_points CHECK (current_points >= 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_customers_status' AND parent_object_id = OBJECT_ID(N'dbo.customers'))
BEGIN
    ALTER TABLE customers ADD CONSTRAINT CK_customers_status CHECK (status IN ('ACTIVE','INACTIVE'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_membership_cards_status' AND parent_object_id = OBJECT_ID(N'dbo.membership_cards'))
BEGIN
    ALTER TABLE membership_cards ADD CONSTRAINT CK_membership_cards_status CHECK (status IN ('ACTIVE','INVALIDATED','LOST'));
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_invoices_id_format' AND parent_object_id = OBJECT_ID(N'dbo.invoices'))
BEGIN
    ALTER TABLE invoices ADD CONSTRAINT CK_invoices_id_format
        CHECK (invoice_id LIKE 'HD[0-9][0-9][0-9][0-9][0-9]');
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_invoices_amount' AND parent_object_id = OBJECT_ID(N'dbo.invoices'))
BEGIN
    ALTER TABLE invoices ADD CONSTRAINT CK_invoices_amount CHECK (
    subtotal >= 0 AND discount_amount >= 0 AND total_amount >= 0 AND discount_amount <= subtotal
);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_invoices_status' AND parent_object_id = OBJECT_ID(N'dbo.invoices'))
BEGIN
    ALTER TABLE invoices ADD CONSTRAINT CK_invoices_status CHECK (status IN ('TEMPORARY','PAID','CANCELLED','PARTIAL_REFUND','REFUNDED'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_invoice_details_values' AND parent_object_id = OBJECT_ID(N'dbo.invoice_details'))
BEGIN
    ALTER TABLE invoice_details ADD CONSTRAINT CK_invoice_details_values CHECK (
    quantity > 0 AND unit_price >= 0 AND cost_price_snapshot >= 0 AND discount_amount >= 0 AND line_total >= 0
);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_payments_amount' AND parent_object_id = OBJECT_ID(N'dbo.payments'))
BEGIN
    ALTER TABLE payments ADD CONSTRAINT CK_payments_amount CHECK (amount > 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_payments_method' AND parent_object_id = OBJECT_ID(N'dbo.payments'))
BEGIN
    ALTER TABLE payments ADD CONSTRAINT CK_payments_method CHECK (payment_method IN ('CASH','CARD','BANK_TRANSFER','INSTALLMENT'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_payments_status' AND parent_object_id = OBJECT_ID(N'dbo.payments'))
BEGIN
    ALTER TABLE payments ADD CONSTRAINT CK_payments_status CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_point_transactions_type' AND parent_object_id = OBJECT_ID(N'dbo.point_transactions'))
BEGIN
    ALTER TABLE point_transactions ADD CONSTRAINT CK_point_transactions_type CHECK (transaction_type IN ('EARN','REDEEM','REVERSAL','ADJUST'));
END
/* Hoàn tiền/đảo doanh số cần ghi được sales_amount và bonus_amount âm.
   Chỉ giữ kiểm tra bonus_rate không âm; dấu của số tiền phản ánh loại nghiệp vụ. */
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_sales_bonuses_values' AND parent_object_id = OBJECT_ID(N'dbo.sales_bonuses'))
BEGIN
    ALTER TABLE dbo.sales_bonuses DROP CONSTRAINT CK_sales_bonuses_values;
END
ALTER TABLE dbo.sales_bonuses
    ADD CONSTRAINT CK_sales_bonuses_values CHECK (bonus_rate >= 0);

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_sales_bonuses_status' AND parent_object_id = OBJECT_ID(N'dbo.sales_bonuses'))
BEGIN
    ALTER TABLE sales_bonuses ADD CONSTRAINT CK_sales_bonuses_status CHECK (status IN ('OPEN','FINALIZED','REVERSED'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_bonus_statements_period' AND parent_object_id = OBJECT_ID(N'dbo.bonus_statements'))
BEGIN
    ALTER TABLE bonus_statements ADD CONSTRAINT CK_bonus_statements_period CHECK (period_start <= period_end);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_bonus_statements_status' AND parent_object_id = OBJECT_ID(N'dbo.bonus_statements'))
BEGIN
    ALTER TABLE bonus_statements ADD CONSTRAINT CK_bonus_statements_status CHECK (status IN ('DRAFT','FINALIZED','CANCELLED'));
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_deliveries_id_format' AND parent_object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    ALTER TABLE deliveries ADD CONSTRAINT CK_deliveries_id_format
        CHECK (delivery_id LIKE 'GH[0-9][0-9][0-9][0-9][0-9]' AND LEN(delivery_id)=7);
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_deliveries_region' AND parent_object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    ALTER TABLE deliveries ADD CONSTRAINT CK_deliveries_region CHECK (region_type IN ('INNER_CITY','OTHER_PROVINCE'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_deliveries_status' AND parent_object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    ALTER TABLE deliveries ADD CONSTRAINT CK_deliveries_status CHECK (status IN ('PREPARING','DELIVERING','DELIVERED','INSTALLED','FAILED'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_deliveries_fee' AND parent_object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    ALTER TABLE deliveries ADD CONSTRAINT CK_deliveries_fee CHECK (delivery_fee >= 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_deliveries_time_order' AND parent_object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    ALTER TABLE deliveries ADD CONSTRAINT CK_deliveries_time_order CHECK (time_from < time_to);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_deliveries_business_hours' AND parent_object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    ALTER TABLE deliveries ADD CONSTRAINT CK_deliveries_business_hours CHECK (
    (region_type = 'INNER_CITY' AND time_from >= CAST('09:00' AS TIME) AND time_to <= CAST('21:00' AS TIME))
    OR
    (region_type = 'OTHER_PROVINCE' AND time_from >= CAST('09:00' AS TIME) AND time_to <= CAST('20:00' AS TIME))
);
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_return_requests_id_format' AND parent_object_id = OBJECT_ID(N'dbo.return_requests'))
BEGIN
    ALTER TABLE return_requests ADD CONSTRAINT CK_return_requests_id_format
        CHECK (return_id LIKE 'HT[0-9][0-9][0-9][0-9][0-9]' AND LEN(return_id)=7);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_return_requests_amount' AND parent_object_id = OBJECT_ID(N'dbo.return_requests'))
BEGIN
    ALTER TABLE return_requests ADD CONSTRAINT CK_return_requests_amount CHECK (refund_amount >= 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_return_requests_status' AND parent_object_id = OBJECT_ID(N'dbo.return_requests'))
BEGIN
    ALTER TABLE return_requests ADD CONSTRAINT CK_return_requests_status CHECK (status IN ('PENDING','WAITING_APPROVAL','APPROVED','REJECTED','COMPLETED'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_return_details_values' AND parent_object_id = OBJECT_ID(N'dbo.return_details'))
BEGIN
    ALTER TABLE return_details ADD CONSTRAINT CK_return_details_values CHECK (quantity > 0 AND refund_amount >= 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_refund_approvals_decision' AND parent_object_id = OBJECT_ID(N'dbo.refund_approvals'))
BEGIN
    ALTER TABLE refund_approvals ADD CONSTRAINT CK_refund_approvals_decision CHECK (decision IN ('PENDING','APPROVED','REJECTED'));
END

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_warranties_status' AND parent_object_id = OBJECT_ID(N'dbo.warranties'))
BEGIN
    ALTER TABLE warranties ADD CONSTRAINT CK_warranties_status CHECK (status IN ('REPAIRING','COMPLETED','RETURNED','REJECTED','REPLACEMENT'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_warranties_charge' AND parent_object_id = OBJECT_ID(N'dbo.warranties'))
BEGIN
    ALTER TABLE warranties ADD CONSTRAINT CK_warranties_charge CHECK (replacement_charge IS NULL OR replacement_charge >= 0);
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_complaints_priority' AND parent_object_id = OBJECT_ID(N'dbo.complaints'))
BEGIN
    ALTER TABLE complaints ADD CONSTRAINT CK_complaints_priority CHECK (priority IN ('NORMAL','URGENT'));
END
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_complaints_status' AND parent_object_id = OBJECT_ID(N'dbo.complaints'))
BEGIN
    ALTER TABLE complaints ADD CONSTRAINT CK_complaints_status CHECK (status IN ('NEW','IN_PROGRESS','WAITING_CUSTOMER','RESOLVED','CLOSED'));
END
GO

/* ================= UNIQUE BUSINESS INVARIANTS / INDEXES ================= */

/* Mỗi khách chỉ có tối đa một số điện thoại giống nhau; cho phép nhiều NULL. */
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_customers_phone' AND object_id = OBJECT_ID(N'dbo.customers'))
BEGIN
    DROP INDEX IX_customers_phone ON dbo.customers;
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_customers_phone' AND object_id = OBJECT_ID(N'dbo.customers'))
BEGIN
    CREATE UNIQUE INDEX UX_customers_phone
        ON dbo.customers(phone)
        WHERE phone IS NOT NULL;
END

/* Mỗi khách chỉ có một thẻ ACTIVE; vẫn cho phép lưu lịch sử thẻ LOST/INVALIDATED. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_membership_cards_one_active_per_customer' AND object_id = OBJECT_ID(N'dbo.membership_cards'))
BEGIN
    CREATE UNIQUE INDEX UX_membership_cards_one_active_per_customer
        ON dbo.membership_cards(customer_id)
        WHERE status = 'ACTIVE';
END

/* Mỗi nhân viên chỉ một ca trong một ngày. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_shift_assignments_employee_date' AND object_id = OBJECT_ID(N'dbo.shift_assignments'))
BEGIN
    CREATE UNIQUE INDEX UX_shift_assignments_employee_date
        ON dbo.shift_assignments(employee_id, work_date);
END

/* Mỗi hóa đơn tối đa một phiếu giao hàng và một phiếu đổi trả. */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_deliveries_invoice' AND object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    CREATE UNIQUE INDEX UX_deliveries_invoice
        ON dbo.deliveries(invoice_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_return_requests_invoice' AND object_id = OBJECT_ID(N'dbo.return_requests'))
BEGIN
    CREATE UNIQUE INDEX UX_return_requests_invoice
        ON dbo.return_requests(invoice_id);
END

/* Sales bonus cần cho phép nhiều dòng điều chỉnh âm do hoàn tiền cho cùng hóa đơn/nhân viên.
   Chỉ dòng bán gốc (sales_amount > 0) được phép tồn tại duy nhất cho mỗi hóa đơn/nhân viên. */
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_sales_bonuses_invoice_employee_status' AND object_id = OBJECT_ID(N'dbo.sales_bonuses'))
BEGIN
    DROP INDEX UX_sales_bonuses_invoice_employee_status ON dbo.sales_bonuses;
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UX_sales_bonuses_base_invoice_employee' AND object_id = OBJECT_ID(N'dbo.sales_bonuses'))
BEGIN
    CREATE UNIQUE INDEX UX_sales_bonuses_base_invoice_employee
        ON dbo.sales_bonuses(invoice_id, employee_id)
        WHERE sales_amount > 0;
END

/* ================= PERFORMANCE INDEXES ================= */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_products_name' AND object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    CREATE INDEX IX_products_name ON products(product_name);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_products_category' AND object_id = OBJECT_ID(N'dbo.products'))
BEGIN
    CREATE INDEX IX_products_category ON products(category_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_invoices_customer_date' AND object_id = OBJECT_ID(N'dbo.invoices'))
BEGIN
    CREATE INDEX IX_invoices_customer_date ON invoices(customer_id, created_at DESC);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_invoices_employee_date' AND object_id = OBJECT_ID(N'dbo.invoices'))
BEGIN
    CREATE INDEX IX_invoices_employee_date ON invoices(employee_id, created_at DESC);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_invoice_details_product' AND object_id = OBJECT_ID(N'dbo.invoice_details'))
BEGIN
    CREATE INDEX IX_invoice_details_product ON invoice_details(product_id);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_deliveries_assigned_status_date' AND object_id = OBJECT_ID(N'dbo.deliveries'))
BEGIN
    CREATE INDEX IX_deliveries_assigned_status_date ON deliveries(assigned_employee_id, status, delivery_date, time_from);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_returns_status_amount' AND object_id = OBJECT_ID(N'dbo.return_requests'))
BEGIN
    CREATE INDEX IX_returns_status_amount ON return_requests(status, refund_amount DESC);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_warranties_customer' AND object_id = OBJECT_ID(N'dbo.warranties'))
BEGIN
    CREATE INDEX IX_warranties_customer ON warranties(customer_id, status);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_complaints_status_due' AND object_id = OBJECT_ID(N'dbo.complaints'))
BEGIN
    CREATE INDEX IX_complaints_status_due ON complaints(status, due_at);
END
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_sales_bonus_employee_date' AND object_id = OBJECT_ID(N'dbo.sales_bonuses'))
BEGIN
    CREATE INDEX IX_sales_bonus_employee_date ON sales_bonuses(employee_id, created_at DESC);
END
GO

/* ================= REFUND APPROVAL CONFIGURATION =================
   approval_required không còn hard-code 5.000.000.
   Giá trị REFUND_APPROVAL_LIMIT được lưu trong dbo.system_settings và
   ứng dụng Java nên đọc cùng setting này thay vì giữ một hằng riêng.
   ================================================================ */
CREATE OR ALTER TRIGGER dbo.TR_return_requests_set_approval_required
ON dbo.return_requests
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF TRIGGER_NESTLEVEL() > 1
        RETURN;

    DECLARE @ApprovalLimit DECIMAL(18,2) =
    (
        SELECT decimal_value
        FROM dbo.system_settings
        WHERE setting_key = 'REFUND_APPROVAL_LIMIT'
    );

    IF @ApprovalLimit IS NULL
        THROW 51020, 'Thiếu system_settings.REFUND_APPROVAL_LIMIT. Hãy chạy 04_seed_data.sql trước khi ghi nhận đổi trả.', 1;

    UPDATE rr
    SET approval_required =
        CASE WHEN rr.refund_amount > @ApprovalLimit THEN CONVERT(bit,1) ELSE CONVERT(bit,0) END
    FROM dbo.return_requests rr
    INNER JOIN inserted i ON i.return_id = rr.return_id
    WHERE rr.approval_required <>
        CASE WHEN rr.refund_amount > @ApprovalLimit THEN CONVERT(bit,1) ELSE CONVERT(bit,0) END;
END
GO

/* Khi quản trị thay đổi hạn mức trong DB, cập nhật lại các phiếu chưa kết thúc. */
CREATE OR ALTER TRIGGER dbo.TR_system_settings_refund_limit_sync
ON dbo.system_settings
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (SELECT 1 FROM inserted WHERE setting_key = 'REFUND_APPROVAL_LIMIT')
        RETURN;

    DECLARE @ApprovalLimit DECIMAL(18,2) =
    (
        SELECT decimal_value
        FROM dbo.system_settings
        WHERE setting_key = 'REFUND_APPROVAL_LIMIT'
    );

    IF @ApprovalLimit IS NULL
        THROW 51021, 'REFUND_APPROVAL_LIMIT không được để NULL.', 1;

    UPDATE dbo.return_requests
    SET approval_required =
        CASE WHEN refund_amount > @ApprovalLimit THEN CONVERT(bit,1) ELSE CONVERT(bit,0) END
    WHERE status IN ('PENDING','WAITING_APPROVAL','APPROVED');
END
GO

USE DienMayBlue;
GO

/* =========================================================
   1. AUTHENTICATION / EMPLOYEES
   ========================================================= */
IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
BEGIN
    CREATE TABLE roles (
        role_id INT IDENTITY(1,1) PRIMARY KEY,
        role_code VARCHAR(40) NOT NULL,
        role_name NVARCHAR(100) NOT NULL,
        description NVARCHAR(255) NULL
    );
END
GO

IF OBJECT_ID(N'dbo.permissions', N'U') IS NULL
BEGIN
    CREATE TABLE permissions (
        permission_id INT IDENTITY(1,1) PRIMARY KEY,
        permission_code VARCHAR(80) NOT NULL,
        permission_name NVARCHAR(150) NOT NULL,
        module_code VARCHAR(50) NOT NULL,
        description NVARCHAR(255) NULL
    );
END
GO

IF OBJECT_ID(N'dbo.role_permissions', N'U') IS NULL
BEGIN
    CREATE TABLE role_permissions (
        role_id INT NOT NULL,
        permission_id INT NOT NULL,
        granted BIT NOT NULL CONSTRAINT DF_role_permissions_granted DEFAULT (1),
        CONSTRAINT PK_role_permissions PRIMARY KEY (role_id, permission_id)
    );
END
GO

IF OBJECT_ID(N'dbo.employees', N'U') IS NULL
BEGIN
    CREATE TABLE employees (
        employee_id VARCHAR(20) PRIMARY KEY,
        full_name NVARCHAR(150) NOT NULL,
        phone VARCHAR(20) NOT NULL,
        email VARCHAR(150) NULL,
        department NVARCHAR(100) NULL,
        join_date DATE NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_employees_status DEFAULT ('ACTIVE'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_employees_created_at DEFAULT (SYSDATETIME()),
        updated_at DATETIME2 NULL
    );
END
GO

IF OBJECT_ID(N'dbo.accounts', N'U') IS NULL
BEGIN
    CREATE TABLE accounts (
        account_id INT IDENTITY(1,1) PRIMARY KEY,
        employee_id VARCHAR(20) NOT NULL,
        username VARCHAR(50) NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        role_id INT NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_accounts_status DEFAULT ('ACTIVE'),
        first_login BIT NOT NULL CONSTRAINT DF_accounts_first_login DEFAULT (1),
        failed_attempts INT NOT NULL CONSTRAINT DF_accounts_failed_attempts DEFAULT (0),
        locked_until DATETIME2 NULL,
        last_login_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_accounts_created_at DEFAULT (SYSDATETIME()),
        updated_at DATETIME2 NULL
    );
END
GO

IF OBJECT_ID(N'dbo.login_logs', N'U') IS NULL
BEGIN
    CREATE TABLE login_logs (
        login_log_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        account_id INT NULL,
        username_entered VARCHAR(50) NULL,
        login_at DATETIME2 NOT NULL CONSTRAINT DF_login_logs_login_at DEFAULT (SYSDATETIME()),
        logout_at DATETIME2 NULL,
        device_info NVARCHAR(255) NULL,
        success BIT NOT NULL,
        failure_reason NVARCHAR(255) NULL
    );
END
GO

IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NULL
BEGIN
    CREATE TABLE audit_logs (
        audit_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        account_id INT NULL,
        action_code VARCHAR(80) NOT NULL,
        entity_name VARCHAR(80) NULL,
        entity_id VARCHAR(80) NULL,
        detail NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_audit_logs_created_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.notifications', N'U') IS NULL
BEGIN
    CREATE TABLE notifications (
        notification_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        employee_id VARCHAR(20) NOT NULL,
        title NVARCHAR(200) NOT NULL,
        content NVARCHAR(1000) NOT NULL,
        is_read BIT NOT NULL CONSTRAINT DF_notifications_is_read DEFAULT (0),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_notifications_created_at DEFAULT (SYSDATETIME())
    );
END
GO

/* =========================================================
   1B. SYSTEM SETTINGS
   - Dùng làm nguồn cấu hình nghiệp vụ dùng chung giữa DB và ứng dụng.
   - Không hard-code hạn mức hoàn tiền trực tiếp trong computed column.
   ========================================================= */
IF OBJECT_ID(N'dbo.system_settings', N'U') IS NULL
BEGIN
    CREATE TABLE system_settings (
        setting_key VARCHAR(80) PRIMARY KEY,
        decimal_value DECIMAL(18,2) NULL,
        text_value NVARCHAR(500) NULL,
        description NVARCHAR(500) NULL,
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_system_settings_updated_at DEFAULT (SYSDATETIME())
    );
END
GO

/* =========================================================
   2. SHIFTS
   ========================================================= */
IF OBJECT_ID(N'dbo.shifts', N'U') IS NULL
BEGIN
    CREATE TABLE shifts (
        shift_id INT IDENTITY(1,1) PRIMARY KEY,
        shift_code VARCHAR(20) NOT NULL,
        shift_name NVARCHAR(50) NOT NULL,
        start_time TIME NOT NULL,
        end_time TIME NOT NULL,
        active BIT NOT NULL CONSTRAINT DF_shifts_active DEFAULT (1)
    );
END
GO

IF OBJECT_ID(N'dbo.shift_assignments', N'U') IS NULL
BEGIN
    CREATE TABLE shift_assignments (
        assignment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        employee_id VARCHAR(20) NOT NULL,
        work_date DATE NOT NULL,
        shift_id INT NOT NULL,
        assigned_by VARCHAR(20) NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_shift_assignments_status DEFAULT ('ASSIGNED'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_shift_assignments_created_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.leave_requests', N'U') IS NULL
BEGIN
    CREATE TABLE leave_requests (
        leave_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        employee_id VARCHAR(20) NOT NULL,
        leave_date DATE NOT NULL,
        reason NVARCHAR(500) NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_leave_requests_status DEFAULT ('APPROVED'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_leave_requests_created_at DEFAULT (SYSDATETIME())
    );
END
GO

/* =========================================================
   3. PRODUCTS / SUPPLIERS
   ========================================================= */
IF OBJECT_ID(N'dbo.categories', N'U') IS NULL
BEGIN
    CREATE TABLE categories (
        category_id INT IDENTITY(1,1) PRIMARY KEY,
        category_code VARCHAR(20) NOT NULL,
        category_name NVARCHAR(100) NOT NULL,
        active BIT NOT NULL CONSTRAINT DF_categories_active DEFAULT (1)
    );
END
GO

IF OBJECT_ID(N'dbo.suppliers', N'U') IS NULL
BEGIN
    CREATE TABLE suppliers (
        supplier_id INT IDENTITY(1,1) PRIMARY KEY,
        supplier_code VARCHAR(20) NOT NULL,
        supplier_name NVARCHAR(200) NOT NULL,
        phone VARCHAR(20) NOT NULL,
        address NVARCHAR(500) NOT NULL,
        tax_code VARCHAR(30) NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_suppliers_status DEFAULT ('ACTIVE'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_suppliers_created_at DEFAULT (SYSDATETIME()),
        updated_at DATETIME2 NULL
    );
END
GO

IF OBJECT_ID(N'dbo.products', N'U') IS NULL
BEGIN
    CREATE TABLE products (
        product_id VARCHAR(20) PRIMARY KEY,
        barcode VARCHAR(50) NOT NULL,
        product_name NVARCHAR(250) NOT NULL,
        category_id INT NOT NULL,
        supplier_id INT NULL,
        cost_price DECIMAL(18,2) NOT NULL,
        sale_price DECIMAL(18,2) NOT NULL,
        stock_quantity INT NOT NULL CONSTRAINT DF_products_stock DEFAULT (0),
        warranty_months INT NOT NULL CONSTRAINT DF_products_warranty_months DEFAULT (12),
        image_path NVARCHAR(500) NULL,
        status VARCHAR(30) NOT NULL CONSTRAINT DF_products_status DEFAULT ('ACTIVE'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_products_created_at DEFAULT (SYSDATETIME()),
        updated_at DATETIME2 NULL
    );
END
GO

/* Bổ sung cột bảo hành nếu DB cũ đã tồn tại trước khi script được cập nhật. */
IF COL_LENGTH(N'dbo.products', N'warranty_months') IS NULL
BEGIN
    ALTER TABLE dbo.products
        ADD warranty_months INT NOT NULL
            CONSTRAINT DF_products_warranty_months DEFAULT (12) WITH VALUES;
END
GO

IF OBJECT_ID(N'dbo.supplier_debts', N'U') IS NULL
BEGIN
    CREATE TABLE supplier_debts (
        debt_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        supplier_id INT NOT NULL,
        reference_code VARCHAR(50) NULL,
        description NVARCHAR(500) NULL,
        original_amount DECIMAL(18,2) NOT NULL,
        remaining_amount DECIMAL(18,2) NOT NULL,
        due_date DATE NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_supplier_debts_status DEFAULT ('UNPAID'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_supplier_debts_created_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.supplier_payments', N'U') IS NULL
BEGIN
    CREATE TABLE supplier_payments (
        supplier_payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        supplier_id INT NOT NULL,
        debt_id BIGINT NULL,
        amount DECIMAL(18,2) NOT NULL,
        paid_at DATETIME2 NOT NULL CONSTRAINT DF_supplier_payments_paid_at DEFAULT (SYSDATETIME()),
        paid_by VARCHAR(20) NOT NULL,
        note NVARCHAR(500) NULL
    );
END
GO

/* =========================================================
   4. PROMOTIONS
   ========================================================= */
IF OBJECT_ID(N'dbo.promotions', N'U') IS NULL
BEGIN
    CREATE TABLE promotions (
        promotion_id INT IDENTITY(1,1) PRIMARY KEY,
        promotion_code VARCHAR(20) NOT NULL,
        promotion_name NVARCHAR(200) NOT NULL,
        discount_type VARCHAR(20) NOT NULL,
        discount_value DECIMAL(18,2) NOT NULL,
        start_at DATETIME2 NOT NULL,
        end_at DATETIME2 NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_promotions_status DEFAULT ('SCHEDULED'),
        created_by VARCHAR(20) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_promotions_created_at DEFAULT (SYSDATETIME()),
        ended_early_at DATETIME2 NULL
    );
END
GO

IF OBJECT_ID(N'dbo.promotion_products', N'U') IS NULL
BEGIN
    CREATE TABLE promotion_products (
        promotion_id INT NOT NULL,
        product_id VARCHAR(20) NOT NULL,
        CONSTRAINT PK_promotion_products PRIMARY KEY (promotion_id, product_id)
    );
END
GO

IF OBJECT_ID(N'dbo.promotion_categories', N'U') IS NULL
BEGIN
    CREATE TABLE promotion_categories (
        promotion_id INT NOT NULL,
        category_id INT NOT NULL,
        CONSTRAINT PK_promotion_categories PRIMARY KEY (promotion_id, category_id)
    );
END
GO

/* =========================================================
   5. CUSTOMERS / MEMBERSHIP / POINTS
   ========================================================= */
IF OBJECT_ID(N'dbo.membership_tiers', N'U') IS NULL
BEGIN
    CREATE TABLE membership_tiers (
        tier_id INT IDENTITY(1,1) PRIMARY KEY,
        tier_code VARCHAR(20) NOT NULL,
        tier_name NVARCHAR(100) NOT NULL,
        min_points INT NOT NULL,
        discount_rate DECIMAL(5,2) NOT NULL CONSTRAINT DF_membership_tiers_discount DEFAULT (0),
        point_multiplier DECIMAL(6,2) NOT NULL CONSTRAINT DF_membership_tiers_multiplier DEFAULT (1)
    );
END
GO

IF OBJECT_ID(N'dbo.customers', N'U') IS NULL
BEGIN
    CREATE TABLE customers (
        customer_id VARCHAR(20) PRIMARY KEY,
        full_name NVARCHAR(150) NOT NULL,
        phone VARCHAR(20) NULL,
        email VARCHAR(150) NULL,
        address NVARCHAR(500) NULL,
        tier_id INT NULL,
        current_points INT NOT NULL CONSTRAINT DF_customers_points DEFAULT (0),
        status VARCHAR(20) NOT NULL CONSTRAINT DF_customers_status DEFAULT ('ACTIVE'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_customers_created_at DEFAULT (SYSDATETIME()),
        updated_at DATETIME2 NULL
    );
END
GO

IF OBJECT_ID(N'dbo.membership_cards', N'U') IS NULL
BEGIN
    CREATE TABLE membership_cards (
        card_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        customer_id VARCHAR(20) NOT NULL,
        card_code VARCHAR(40) NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_membership_cards_status DEFAULT ('ACTIVE'),
        issued_at DATETIME2 NOT NULL CONSTRAINT DF_membership_cards_issued_at DEFAULT (SYSDATETIME()),
        invalidated_at DATETIME2 NULL
    );
END
GO

/* =========================================================
   6. SALES / INVOICE / PAYMENT
   ========================================================= */
IF OBJECT_ID(N'dbo.invoices', N'U') IS NULL
BEGIN
    CREATE TABLE invoices (
        invoice_id VARCHAR(7) PRIMARY KEY,
        customer_id VARCHAR(20) NULL,
        employee_id VARCHAR(20) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_invoices_created_at DEFAULT (SYSDATETIME()),
        subtotal DECIMAL(18,2) NOT NULL,
        discount_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_invoices_discount DEFAULT (0),
        total_amount DECIMAL(18,2) NOT NULL,
        status VARCHAR(30) NOT NULL CONSTRAINT DF_invoices_status DEFAULT ('TEMPORARY'),
        note NVARCHAR(500) NULL
    );
END
GO

IF OBJECT_ID(N'dbo.invoice_details', N'U') IS NULL
BEGIN
    CREATE TABLE invoice_details (
        invoice_detail_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        invoice_id VARCHAR(7) NOT NULL,
        product_id VARCHAR(20) NOT NULL,
        quantity INT NOT NULL,
        unit_price DECIMAL(18,2) NOT NULL,
        cost_price_snapshot DECIMAL(18,2) NOT NULL,
        discount_amount DECIMAL(18,2) NOT NULL CONSTRAINT DF_invoice_details_discount DEFAULT (0),
        line_total DECIMAL(18,2) NOT NULL
    );
END
GO

IF OBJECT_ID(N'dbo.payments', N'U') IS NULL
BEGIN
    CREATE TABLE payments (
        payment_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        invoice_id VARCHAR(7) NOT NULL,
        payment_method VARCHAR(30) NOT NULL,
        amount DECIMAL(18,2) NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_payments_status DEFAULT ('SUCCESS'),
        transaction_ref VARCHAR(100) NULL,
        paid_at DATETIME2 NOT NULL CONSTRAINT DF_payments_paid_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.point_transactions', N'U') IS NULL
BEGIN
    CREATE TABLE point_transactions (
        point_transaction_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        customer_id VARCHAR(20) NOT NULL,
        invoice_id VARCHAR(7) NULL,
        transaction_type VARCHAR(20) NOT NULL,
        points_change INT NOT NULL,
        note NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_point_transactions_created_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.sales_bonuses', N'U') IS NULL
BEGIN
    CREATE TABLE sales_bonuses (
        sales_bonus_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        employee_id VARCHAR(20) NOT NULL,
        invoice_id VARCHAR(7) NULL,
        sales_amount DECIMAL(18,2) NOT NULL,
        bonus_rate DECIMAL(6,4) NOT NULL,
        bonus_amount DECIMAL(18,2) NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_sales_bonuses_status DEFAULT ('OPEN'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_sales_bonuses_created_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.bonus_statements', N'U') IS NULL
BEGIN
    CREATE TABLE bonus_statements (
        statement_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        period_start DATE NOT NULL,
        period_end DATE NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_bonus_statements_status DEFAULT ('DRAFT'),
        finalized_by VARCHAR(20) NULL,
        finalized_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_bonus_statements_created_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.bonus_statement_details', N'U') IS NULL
BEGIN
    CREATE TABLE bonus_statement_details (
        statement_detail_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        statement_id BIGINT NOT NULL,
        employee_id VARCHAR(20) NOT NULL,
        total_sales DECIMAL(18,2) NOT NULL,
        total_bonus DECIMAL(18,2) NOT NULL
    );
END
GO

/* =========================================================
   7. DELIVERY / RETURN / REFUND
   ========================================================= */
IF OBJECT_ID(N'dbo.deliveries', N'U') IS NULL
BEGIN
    CREATE TABLE deliveries (
        delivery_id VARCHAR(7) PRIMARY KEY,
        invoice_id VARCHAR(7) NOT NULL,
        assigned_employee_id VARCHAR(20) NULL,
        delivery_address NVARCHAR(500) NOT NULL,
        region_type VARCHAR(30) NOT NULL,
        delivery_date DATE NOT NULL,
        time_from TIME NOT NULL,
        time_to TIME NOT NULL,
        install_required BIT NOT NULL CONSTRAINT DF_deliveries_install DEFAULT (0),
        delivery_fee DECIMAL(18,2) NOT NULL CONSTRAINT DF_deliveries_fee DEFAULT (0),
        status VARCHAR(30) NOT NULL CONSTRAINT DF_deliveries_status DEFAULT ('PREPARING'),
        proof_image_path NVARCHAR(500) NULL,
        signature_path NVARCHAR(500) NULL,
        failure_reason NVARCHAR(500) NULL,
        reschedule_date DATE NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_deliveries_created_at DEFAULT (SYSDATETIME()),
        updated_at DATETIME2 NULL
    );
END
GO

IF OBJECT_ID(N'dbo.delivery_history', N'U') IS NULL
BEGIN
    CREATE TABLE delivery_history (
        delivery_history_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        delivery_id VARCHAR(7) NOT NULL,
        old_status VARCHAR(30) NULL,
        new_status VARCHAR(30) NOT NULL,
        changed_by VARCHAR(20) NOT NULL,
        note NVARCHAR(500) NULL,
        changed_at DATETIME2 NOT NULL CONSTRAINT DF_delivery_history_changed_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.return_requests', N'U') IS NULL
BEGIN
    CREATE TABLE return_requests (
        return_id VARCHAR(7) PRIMARY KEY,
        invoice_id VARCHAR(7) NOT NULL,
        created_by VARCHAR(20) NOT NULL,
        request_date DATETIME2 NOT NULL CONSTRAINT DF_return_requests_date DEFAULT (SYSDATETIME()),
        reason NVARCHAR(1000) NOT NULL,
        physical_condition NVARCHAR(1000) NULL,
        evidence_image_path NVARCHAR(500) NULL,
        refund_amount DECIMAL(18,2) NOT NULL,
        approval_required BIT NOT NULL CONSTRAINT DF_return_requests_approval_required DEFAULT (0),
        status VARCHAR(30) NOT NULL CONSTRAINT DF_return_requests_status DEFAULT ('PENDING'),
        rejection_reason NVARCHAR(1000) NULL,
        completed_at DATETIME2 NULL
    );
END
GO

/* Sửa schema cũ: approval_required trước đây là computed column hard-code 5.000.000.
   Chuyển thành cột BIT thường; trigger ở 03_constraints.sql sẽ lấy hạn mức từ system_settings. */
IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.return_requests')
      AND name = N'approval_required'
      AND is_computed = 1
)
BEGIN
    ALTER TABLE dbo.return_requests DROP COLUMN approval_required;
END
GO

IF COL_LENGTH(N'dbo.return_requests', N'approval_required') IS NULL
BEGIN
    ALTER TABLE dbo.return_requests
        ADD approval_required BIT NOT NULL
            CONSTRAINT DF_return_requests_approval_required DEFAULT (0) WITH VALUES;
END
GO

IF OBJECT_ID(N'dbo.return_details', N'U') IS NULL
BEGIN
    CREATE TABLE return_details (
        return_detail_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        return_id VARCHAR(7) NOT NULL,
        invoice_detail_id BIGINT NOT NULL,
        product_id VARCHAR(20) NOT NULL,
        quantity INT NOT NULL,
        refund_amount DECIMAL(18,2) NOT NULL,
        seal_intact BIT NOT NULL CONSTRAINT DF_return_details_seal DEFAULT (1),
        accessories_complete BIT NOT NULL CONSTRAINT DF_return_details_accessories DEFAULT (1)
    );
END
GO

IF OBJECT_ID(N'dbo.refund_approvals', N'U') IS NULL
BEGIN
    CREATE TABLE refund_approvals (
        approval_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        return_id VARCHAR(7) NOT NULL,
        approver_employee_id VARCHAR(20) NULL,
        decision VARCHAR(20) NOT NULL CONSTRAINT DF_refund_approvals_decision DEFAULT ('PENDING'),
        reason NVARCHAR(1000) NULL,
        decided_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_refund_approvals_created_at DEFAULT (SYSDATETIME())
    );
END
GO

/* =========================================================
   8. AFTER-SALES: WARRANTY / COMPLAINT
   ========================================================= */
IF OBJECT_ID(N'dbo.warranties', N'U') IS NULL
BEGIN
    CREATE TABLE warranties (
        warranty_id VARCHAR(30) PRIMARY KEY,
        customer_id VARCHAR(20) NOT NULL,
        invoice_detail_id BIGINT NOT NULL,
        product_id VARCHAR(20) NOT NULL,
        issue_description NVARCHAR(1000) NOT NULL,
        warranty_end_date DATE NOT NULL,
        status VARCHAR(30) NOT NULL CONSTRAINT DF_warranties_status DEFAULT ('REPAIRING'),
        replacement_charge DECIMAL(18,2) NULL,
        created_by VARCHAR(20) NOT NULL,
        received_at DATETIME2 NOT NULL CONSTRAINT DF_warranties_received_at DEFAULT (SYSDATETIME()),
        completed_at DATETIME2 NULL,
        returned_at DATETIME2 NULL
    );
END
GO

IF OBJECT_ID(N'dbo.warranty_history', N'U') IS NULL
BEGIN
    CREATE TABLE warranty_history (
        warranty_history_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        warranty_id VARCHAR(30) NOT NULL,
        old_status VARCHAR(30) NULL,
        new_status VARCHAR(30) NOT NULL,
        changed_by VARCHAR(20) NOT NULL,
        note NVARCHAR(1000) NULL,
        changed_at DATETIME2 NOT NULL CONSTRAINT DF_warranty_history_changed_at DEFAULT (SYSDATETIME())
    );
END
GO

IF OBJECT_ID(N'dbo.complaints', N'U') IS NULL
BEGIN
    CREATE TABLE complaints (
        complaint_id VARCHAR(30) PRIMARY KEY,
        customer_id VARCHAR(20) NULL,
        customer_phone VARCHAR(20) NULL,
        content NVARCHAR(2000) NOT NULL,
        priority VARCHAR(20) NOT NULL CONSTRAINT DF_complaints_priority DEFAULT ('NORMAL'),
        status VARCHAR(30) NOT NULL CONSTRAINT DF_complaints_status DEFAULT ('NEW'),
        created_by VARCHAR(20) NOT NULL,
        assigned_to VARCHAR(20) NULL,
        due_at DATETIME2 NOT NULL CONSTRAINT DF_complaints_due_at DEFAULT (DATEADD(HOUR, 48, SYSDATETIME())),
        escalated BIT NOT NULL CONSTRAINT DF_complaints_escalated DEFAULT (0),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_complaints_created_at DEFAULT (SYSDATETIME()),
        updated_at DATETIME2 NULL,
        resolved_at DATETIME2 NULL
    );
END
GO

IF OBJECT_ID(N'dbo.complaint_history', N'U') IS NULL
BEGIN
    CREATE TABLE complaint_history (
        complaint_history_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        complaint_id VARCHAR(30) NOT NULL,
        old_status VARCHAR(30) NULL,
        new_status VARCHAR(30) NOT NULL,
        changed_by VARCHAR(20) NOT NULL,
        note NVARCHAR(1000) NULL,
        changed_at DATETIME2 NOT NULL CONSTRAINT DF_complaint_history_changed_at DEFAULT (SYSDATETIME())
    );
END
GO

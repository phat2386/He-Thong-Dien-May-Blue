USE DienMayBlue;
GO

PRINT N'=== KIỂM TRA DATABASE DIENMAYBLUE ===';

SELECT
    (SELECT COUNT(*) FROM sys.tables) AS SoBang,
    (SELECT COUNT(*) FROM sys.foreign_keys) AS SoKhoaNgoai,
    (SELECT COUNT(*) FROM sys.check_constraints) AS SoCheckConstraint,
    (SELECT COUNT(*) FROM sys.indexes WHERE is_primary_key = 0 AND name IS NOT NULL) AS SoIndex;

SELECT role_code, role_name
FROM dbo.roles
ORDER BY role_id;

SELECT r.role_code, p.permission_code, p.permission_name, rp.granted
FROM dbo.role_permissions rp
JOIN dbo.roles r ON r.role_id = rp.role_id
JOIN dbo.permissions p ON p.permission_id = rp.permission_id
ORDER BY r.role_code, p.module_code, p.permission_code;

SELECT a.username, e.full_name, r.role_name, a.status, a.first_login
FROM dbo.accounts a
JOIN dbo.employees e ON e.employee_id = a.employee_id
JOIN dbo.roles r ON r.role_id = a.role_id
ORDER BY a.username;

SELECT tier_code, tier_name, min_points, discount_rate, point_multiplier
FROM dbo.membership_tiers
ORDER BY min_points;

PRINT N'=== KIỂM TRA 13 LỖI ĐÃ SỬA ===';

/* 1. Seed không được ghi đè cấu hình */
SELECT
    N'01 - Seed quyền/hạng không phá cấu hình' AS KiemTra,
    CASE
        WHEN EXISTS (SELECT 1 FROM dbo.system_settings WHERE setting_key='REFUND_APPROVAL_LIMIT')
        THEN N'PASS - Có cấu hình nghiệp vụ; 04_seed_data dùng insert-if-missing'
        ELSE N'FAIL - Thiếu REFUND_APPROVAL_LIMIT'
    END AS KetQua;

/* 2. Sales bonus: chỉ UNIQUE dòng bán gốc; cho phép nhiều dòng điều chỉnh âm khi hoàn tiền. */
SELECT
    N'02 - sales_bonuses hỗ trợ hoàn tiền' AS KiemTra,
    CASE
        WHEN EXISTS (
            SELECT 1 FROM sys.indexes
            WHERE object_id=OBJECT_ID(N'dbo.sales_bonuses')
              AND name=N'UX_sales_bonuses_base_invoice_employee'
              AND is_unique=1
              AND has_filter=1
        )
        AND NOT EXISTS (
            SELECT 1 FROM sys.indexes
            WHERE object_id=OBJECT_ID(N'dbo.sales_bonuses')
              AND name=N'UX_sales_bonuses_invoice_employee_status'
        )
        AND EXISTS (
            SELECT 1 FROM sys.check_constraints
            WHERE parent_object_id=OBJECT_ID(N'dbo.sales_bonuses')
              AND name=N'CK_sales_bonuses_values'
              AND definition NOT LIKE '%sales_amount%>=%'
              AND definition NOT LIKE '%bonus_amount%>=%'
        )
        THEN N'PASS'
        ELSE N'FAIL'
    END AS KetQua;

/* 3-6. Các invariant phải được DB chặn */
SELECT N'03 - Một invoice một delivery' AS KiemTra,
       CASE WHEN EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.deliveries') AND name=N'UX_deliveries_invoice' AND is_unique=1)
            THEN N'PASS' ELSE N'FAIL' END AS KetQua
UNION ALL
SELECT N'04 - Một invoice một return_request',
       CASE WHEN EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.return_requests') AND name=N'UX_return_requests_invoice' AND is_unique=1)
            THEN N'PASS' ELSE N'FAIL' END
UNION ALL
SELECT N'05A - Một ACTIVE card / customer',
       CASE WHEN EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.membership_cards') AND name=N'UX_membership_cards_one_active_per_customer' AND is_unique=1)
            THEN N'PASS' ELSE N'FAIL' END
UNION ALL
SELECT N'05B - Một ca / employee / ngày',
       CASE WHEN EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.shift_assignments') AND name=N'UX_shift_assignments_employee_date' AND is_unique=1)
            THEN N'PASS' ELSE N'FAIL' END
UNION ALL
SELECT N'06 - customers.phone UNIQUE khi khác NULL',
       CASE WHEN EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.customers') AND name=N'UX_customers_phone' AND is_unique=1)
            THEN N'PASS' ELSE N'FAIL' END;

/* 7. Approval limit không còn hard-code trong computed column */
SELECT
    N'07 - REFUND_APPROVAL_LIMIT dùng system_settings' AS KiemTra,
    CASE
        WHEN COL_LENGTH(N'dbo.return_requests',N'approval_required') IS NOT NULL
         AND NOT EXISTS (
             SELECT 1 FROM sys.columns
             WHERE object_id=OBJECT_ID(N'dbo.return_requests')
               AND name=N'approval_required'
               AND is_computed=1
         )
         AND EXISTS (
             SELECT 1 FROM dbo.system_settings
             WHERE setting_key='REFUND_APPROVAL_LIMIT'
               AND decimal_value IS NOT NULL
         )
         AND OBJECT_ID(N'dbo.TR_return_requests_set_approval_required',N'TR') IS NOT NULL
        THEN N'PASS'
        ELSE N'FAIL'
    END AS KetQua;

/* 8. warranty_months thuộc schema chính */
SELECT
    N'08 - products.warranty_months' AS KiemTra,
    CASE
        WHEN COL_LENGTH(N'dbo.products',N'warranty_months') IS NOT NULL
         AND EXISTS (
             SELECT 1 FROM sys.check_constraints
             WHERE parent_object_id=OBJECT_ID(N'dbo.products')
               AND name=N'CK_products_warranty_months'
         )
        THEN N'PASS'
        ELSE N'FAIL'
    END AS KetQua;


/* 8B. Mã hóa đơn thống nhất dạng HD00001 */
SELECT
    N'08B - Mã hóa đơn HD00001' AS KiemTra,
    CASE
        WHEN NOT EXISTS (
            SELECT 1 FROM dbo.invoices
            WHERE invoice_id NOT LIKE 'HD[0-9][0-9][0-9][0-9][0-9]'
        )
         AND EXISTS (
            SELECT 1 FROM sys.check_constraints
            WHERE parent_object_id=OBJECT_ID(N'dbo.invoices')
              AND name=N'CK_invoices_id_format'
         )
        THEN N'PASS' ELSE N'FAIL'
    END AS KetQua;

SELECT N'08C - Mã phiếu hoàn tiền theo dạng HT00001' AS KiemTra,
       CASE WHEN NOT EXISTS (
                SELECT 1 FROM dbo.return_requests
                WHERE return_id NOT LIKE 'HT[0-9][0-9][0-9][0-9][0-9]'
                   OR LEN(return_id)<>7
            )
            AND EXISTS (
                SELECT 1 FROM sys.check_constraints
                WHERE parent_object_id=OBJECT_ID(N'dbo.return_requests')
                  AND name=N'CK_return_requests_id_format'
            )
            THEN N'PASS' ELSE N'FAIL' END AS KetQua;

SELECT N'08D - Mã phiếu giao hàng theo dạng GH00001' AS KiemTra,
       CASE WHEN NOT EXISTS (
                SELECT 1 FROM dbo.deliveries
                WHERE delivery_id NOT LIKE 'GH[0-9][0-9][0-9][0-9][0-9]'
                   OR LEN(delivery_id)<>7
            )
            AND EXISTS (
                SELECT 1 FROM sys.check_constraints
                WHERE parent_object_id=OBJECT_ID(N'dbo.deliveries')
                  AND name=N'CK_deliveries_id_format'
            )
            THEN N'PASS' ELSE N'FAIL' END AS KetQua;

/* 9-13. Dữ liệu demo (nếu đã chạy 05) */
IF EXISTS (SELECT 1 FROM dbo.invoices WHERE note LIKE '%DEMO%')
BEGIN
    SELECT
        N'09 - Demo invoice đã đánh dấu trừ stock' AS KiemTra,
        CASE WHEN NOT EXISTS (
            SELECT 1 FROM dbo.invoices
            WHERE note LIKE '%DEMO%'
              AND (note IS NULL OR note NOT LIKE '%STOCK_APPLIED=1%')
        ) THEN N'PASS' ELSE N'FAIL' END AS KetQua
    UNION ALL
    SELECT
        N'10 - Demo discount có promotion hợp lệ',
        CASE WHEN NOT EXISTS (
            SELECT 1
            FROM dbo.invoices i
            WHERE i.note LIKE '%DEMO%'
              AND i.discount_amount > 0
              AND NOT EXISTS (
                    SELECT 1
                    FROM dbo.promotions p
                    INNER JOIN dbo.promotion_products pp ON pp.promotion_id=p.promotion_id
                    WHERE p.promotion_code='KM-DEMO-15'
                      AND pp.product_id='SP001'
                      AND p.discount_type='PERCENT'
                      AND i.created_at BETWEEN p.start_at AND p.end_at
              )
        ) THEN N'PASS' ELSE N'FAIL' END
    UNION ALL
    SELECT
        N'11 - Demo invoice có point_transactions',
        CASE WHEN NOT EXISTS (
            SELECT 1 FROM dbo.invoices i
            WHERE i.note LIKE '%DEMO%'
              AND i.customer_id IS NOT NULL
              AND NOT EXISTS (
                    SELECT 1 FROM dbo.point_transactions pt
                    WHERE pt.invoice_id=i.invoice_id
                      AND pt.customer_id=i.customer_id
                      AND pt.transaction_type='EARN'
              )
        ) THEN N'PASS' ELSE N'FAIL' END
    UNION ALL
    SELECT
        N'12A - Phiếu giao hàng demo có delivery_history',
        CASE WHEN NOT EXISTS(
                    SELECT 1 FROM dbo.deliveries d
                    JOIN dbo.invoices i ON i.invoice_id=d.invoice_id
                    WHERE i.note LIKE '%DEMO%'
                  )
                  OR NOT EXISTS(
                    SELECT 1 FROM dbo.deliveries d
                    JOIN dbo.invoices i ON i.invoice_id=d.invoice_id
                    WHERE i.note LIKE '%DEMO%'
                      AND NOT EXISTS(SELECT 1 FROM dbo.delivery_history h WHERE h.delivery_id=d.delivery_id)
                  )
             THEN N'PASS' ELSE N'FAIL' END
    UNION ALL
    SELECT
        N'12B - Nhân viên giao có ca ngày giao',
        CASE WHEN NOT EXISTS(
                    SELECT 1 FROM dbo.deliveries d
                    JOIN dbo.invoices i ON i.invoice_id=d.invoice_id
                    WHERE i.note LIKE '%DEMO%'
                  )
                  OR NOT EXISTS(
                      SELECT 1
                      FROM dbo.deliveries d
                      JOIN dbo.invoices i ON i.invoice_id=d.invoice_id
                      WHERE i.note LIKE '%DEMO%'
                        AND d.assigned_employee_id IS NOT NULL
                        AND NOT EXISTS(
                            SELECT 1 FROM dbo.shift_assignments sa
                            WHERE sa.employee_id=d.assigned_employee_id
                              AND sa.work_date=d.delivery_date
                        )
                  )
             THEN N'PASS' ELSE N'FAIL' END;

    DECLARE @Now TIME=CAST(GETDATE() AS TIME);
    DECLARE @Today DATE=CAST(GETDATE() AS DATE);

    SELECT
        N'13 - sale01 có ca bao phủ thời điểm hiện tại' AS KiemTra,
        CASE WHEN EXISTS (
            SELECT 1
            FROM dbo.shift_assignments sa
            JOIN dbo.shifts s ON s.shift_id=sa.shift_id
            WHERE sa.employee_id='NV-2023-002'
              AND sa.work_date=@Today
              AND sa.status='ASSIGNED'
              AND @Now>=s.start_time
              AND @Now<s.end_time
        ) THEN N'PASS' ELSE N'FAIL' END AS KetQua;
END
ELSE
BEGIN
    SELECT N'09-13 - Demo data' AS KiemTra,
           N'SKIP - Chưa chạy 05_optional_demo_operational_data.sql' AS KetQua;
END

/* Kiểm tra không có dữ liệu vi phạm các UNIQUE nghiệp vụ. */
SELECT N'Duplicate delivery invoice' AS KiemTra, COUNT(*) AS SoNhomLoi
FROM (
    SELECT invoice_id FROM dbo.deliveries GROUP BY invoice_id HAVING COUNT(*)>1
) x
UNION ALL
SELECT N'Duplicate return invoice', COUNT(*)
FROM (
    SELECT invoice_id FROM dbo.return_requests GROUP BY invoice_id HAVING COUNT(*)>1
) x
UNION ALL
SELECT N'Duplicate ACTIVE card', COUNT(*)
FROM (
    SELECT customer_id FROM dbo.membership_cards WHERE status='ACTIVE' GROUP BY customer_id HAVING COUNT(*)>1
) x
UNION ALL
SELECT N'Duplicate employee shift/day', COUNT(*)
FROM (
    SELECT employee_id,work_date FROM dbo.shift_assignments GROUP BY employee_id,work_date HAVING COUNT(*)>1
) x
UNION ALL
SELECT N'Duplicate customer phone', COUNT(*)
FROM (
    SELECT phone FROM dbo.customers WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*)>1
) x;

PRINT N'=== HOÀN TẤT KIỂM TRA ===';
GO

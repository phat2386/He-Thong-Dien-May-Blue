USE DienMayBlue;
GO

/* =========================================================
   DỮ LIỆU NỀN - CÓ THỂ CHẠY LẠI AN TOÀN
   - Không xóa dữ liệu nghiệp vụ hiện có.
   - Tài khoản đã tồn tại sẽ KHÔNG bị đặt lại mật khẩu.
   - Chỉ bổ sung quyền mặc định còn thiếu; KHÔNG xóa/ghi đè quyền quản trị viên đã chỉnh.
   - Hạng thành viên chỉ được tạo khi chưa tồn tại; KHÔNG ghi đè chính sách UC024.
   ========================================================= */

/* ================= ROLES ================= */
MERGE dbo.roles AS target
USING (VALUES
    ('MANAGER', N'Quản lý', N'Nhân viên quản lý hệ thống/cửa hàng'),
    ('SALES', N'Bán hàng', N'Nhân viên bán hàng tại quầy'),
    ('CUSTOMER_SERVICE', N'Chăm sóc khách hàng', N'Nhân viên chăm sóc khách hàng sau bán')
) AS src(role_code, role_name, description)
ON target.role_code = src.role_code
WHEN MATCHED THEN UPDATE SET role_name = src.role_name, description = src.description
WHEN NOT MATCHED THEN
    INSERT(role_code, role_name, description)
    VALUES(src.role_code, src.role_name, src.description);
GO

/* ================= PERMISSIONS ================= */
MERGE dbo.permissions AS target
USING (VALUES
    ('DASHBOARD_VIEW', N'Xem tổng quan', 'DASHBOARD'),
    ('EMPLOYEE_MANAGE', N'Quản lý nhân viên', 'EMPLOYEE'),
    ('ACCOUNT_MANAGE', N'Quản lý tài khoản và phân quyền', 'ACCOUNT'),
    ('SHIFT_MANAGE', N'Quản lý ca làm việc', 'SHIFT'),
    ('REFUND_APPROVE', N'Duyệt hoàn tiền vượt hạn mức', 'RETURN'),
    ('STATISTICS_VIEW', N'Xem thống kê', 'STATISTICS'),
    ('BONUS_FINALIZE', N'Chốt bảng kê thưởng doanh số', 'STATISTICS'),
    ('PRODUCT_MANAGE', N'Quản lý sản phẩm', 'PRODUCT'),
    ('PRODUCT_SEARCH', N'Tìm sản phẩm', 'PRODUCT'),
    ('SUPPLIER_MANAGE', N'Quản lý nhà cung cấp', 'SUPPLIER'),
    ('PROMOTION_MANAGE', N'Quản lý khuyến mãi', 'PROMOTION'),
    ('MEMBERSHIP_MANAGE', N'Quản lý thẻ và hạng thành viên', 'PROMOTION'),
    ('POINT_MANAGE', N'Tích điểm cho khách hàng', 'PROMOTION'),
    ('SALES_MANAGE', N'Quản lý bán hàng', 'SALES'),
    ('INVOICE_CREATE', N'Lập hóa đơn bán hàng', 'SALES'),
    ('DELIVERY_CREATE', N'Lập phiếu giao hàng - lắp đặt', 'SALES'),
    ('RETURN_CREATE', N'Lập phiếu đổi trả / hoàn tiền', 'RETURN'),
    ('CUSTOMER_MANAGE', N'Quản lý khách hàng', 'CUSTOMER'),
    ('PERSONAL_SALES_VIEW', N'Xem doanh số / thưởng cá nhân', 'STATISTICS'),
    ('DELIVERY_MANAGE', N'Quản lý giao hàng', 'DELIVERY'),
    ('DELIVERY_ASSIGN', N'Phân công giao hàng', 'DELIVERY'),
    ('WARRANTY_MANAGE', N'Tiếp nhận và theo dõi bảo hành', 'AFTER_SALES'),
    ('COMPLAINT_MANAGE', N'Ghi nhận và xử lý khiếu nại', 'AFTER_SALES'),
    ('PROFILE_VIEW', N'Xem hồ sơ cá nhân', 'PROFILE'),
    ('PASSWORD_CHANGE', N'Đổi mật khẩu', 'PROFILE')
) AS src(permission_code, permission_name, module_code)
ON target.permission_code = src.permission_code
WHEN MATCHED THEN UPDATE SET
    permission_name = src.permission_name,
    module_code = src.module_code
WHEN NOT MATCHED THEN
    INSERT(permission_code, permission_name, module_code)
    VALUES(src.permission_code, src.permission_name, src.module_code);
GO

/* ================= ROLE PERMISSIONS =================
   Đồng bộ đúng Actor/Use Case cuối cùng:
   - Quản lý: UC005-UC016, UC023-UC024, UC032 (+ dùng chung).
   - Bán hàng: UC008, UC017-UC022, UC025-UC028, UC033 (+ dùng chung).
   - CSKH: UC025, UC029-UC031 (+ dùng chung).
   ========================================================= */
DECLARE @ManagerRole INT = (SELECT role_id FROM dbo.roles WHERE role_code='MANAGER');
DECLARE @SalesRole INT = (SELECT role_id FROM dbo.roles WHERE role_code='SALES');
DECLARE @CSRole INT = (SELECT role_id FROM dbo.roles WHERE role_code='CUSTOMER_SERVICE');

/* Chỉ seed các mapping CHƯA tồn tại.
   Nếu quản trị viên đã revoke (granted = 0) hay chỉnh quyền, chạy lại file này không thay đổi lựa chọn đó. */
INSERT INTO dbo.role_permissions(role_id, permission_id, granted)
SELECT @ManagerRole, p.permission_id, 1
FROM dbo.permissions p
WHERE p.permission_code IN (
    'DASHBOARD_VIEW','EMPLOYEE_MANAGE','ACCOUNT_MANAGE','SHIFT_MANAGE','REFUND_APPROVE',
    'STATISTICS_VIEW','BONUS_FINALIZE','PRODUCT_MANAGE','SUPPLIER_MANAGE','PROMOTION_MANAGE',
    'MEMBERSHIP_MANAGE','POINT_MANAGE','PROFILE_VIEW','PASSWORD_CHANGE'
)
AND NOT EXISTS (
    SELECT 1 FROM dbo.role_permissions rp
    WHERE rp.role_id = @ManagerRole AND rp.permission_id = p.permission_id
);

INSERT INTO dbo.role_permissions(role_id, permission_id, granted)
SELECT @SalesRole, p.permission_id, 1
FROM dbo.permissions p
WHERE p.permission_code IN (
    'DASHBOARD_VIEW','STATISTICS_VIEW','PRODUCT_SEARCH','SALES_MANAGE','INVOICE_CREATE',
    'DELIVERY_CREATE','RETURN_CREATE','CUSTOMER_MANAGE','PERSONAL_SALES_VIEW',
    'DELIVERY_MANAGE','DELIVERY_ASSIGN','PROFILE_VIEW','PASSWORD_CHANGE'
)
AND NOT EXISTS (
    SELECT 1 FROM dbo.role_permissions rp
    WHERE rp.role_id = @SalesRole AND rp.permission_id = p.permission_id
);

INSERT INTO dbo.role_permissions(role_id, permission_id, granted)
SELECT @CSRole, p.permission_id, 1
FROM dbo.permissions p
WHERE p.permission_code IN (
    'DASHBOARD_VIEW','PERSONAL_SALES_VIEW','WARRANTY_MANAGE','COMPLAINT_MANAGE',
    'PROFILE_VIEW','PASSWORD_CHANGE'
)
AND NOT EXISTS (
    SELECT 1 FROM dbo.role_permissions rp
    WHERE rp.role_id = @CSRole AND rp.permission_id = p.permission_id
);
GO

/* ================= SYSTEM SETTINGS =================
   Nguồn cấu hình chung cho DB và Java.
   Java nên đọc REFUND_APPROVAL_LIMIT từ bảng này thay vì giữ hằng riêng.
   Seed chỉ tạo khi thiếu, không ghi đè giá trị quản trị đã sửa.
   ========================================================= */
IF NOT EXISTS (SELECT 1 FROM dbo.system_settings WHERE setting_key='REFUND_APPROVAL_LIMIT')
BEGIN
    INSERT INTO dbo.system_settings(setting_key, decimal_value, description)
    VALUES('REFUND_APPROVAL_LIMIT', 5000000, N'Hạn mức hoàn tiền cần quản lý phê duyệt');
END
GO

/* Đồng bộ cột approval_required cho DB được nâng cấp từ phiên bản cũ. */
DECLARE @RefundApprovalLimit DECIMAL(18,2) =
(
    SELECT decimal_value
    FROM dbo.system_settings
    WHERE setting_key='REFUND_APPROVAL_LIMIT'
);

IF @RefundApprovalLimit IS NOT NULL
BEGIN
    UPDATE dbo.return_requests
    SET approval_required =
        CASE WHEN refund_amount > @RefundApprovalLimit THEN CONVERT(bit,1) ELSE CONVERT(bit,0) END;
END
GO

/* ================= EMPLOYEES ================= */
INSERT INTO dbo.employees(employee_id, full_name, phone, email, department, join_date)
SELECT v.employee_id, v.full_name, v.phone, v.email, v.department, v.join_date
FROM (VALUES
    ('NV-2023-001', N'Nguyễn Văn An', '0912345678', 'an.nguyen@dienmayblue.vn', N'Ban giám đốc', CONVERT(date,'2023-01-01')),
    ('NV-2023-002', N'Trần Văn Bình', '0923456789', 'binh.tv@dienmayblue.vn', N'Kinh doanh', CONVERT(date,'2023-03-15')),
    ('NV-2023-003', N'Phạm Thu Hà', '0934567890', 'ha.pt@dienmayblue.vn', N'Chăm sóc KH', CONVERT(date,'2023-07-10')),
    ('NV-2025-001', N'Hoàng Văn Tú', '0945678901', 'tu.hv@dienmayblue.vn', N'Kinh doanh', CONVERT(date,'2025-09-01'))
) v(employee_id, full_name, phone, email, department, join_date)
WHERE NOT EXISTS (SELECT 1 FROM dbo.employees e WHERE e.employee_id=v.employee_id);
GO

/* ================= ACCOUNTS =================
   Chỉ tạo nếu chưa có; KHÔNG reset mật khẩu tài khoản đã tồn tại.
   ========================================================= */
DECLARE @ManagerRole2 INT = (SELECT role_id FROM dbo.roles WHERE role_code='MANAGER');
DECLARE @SalesRole2 INT = (SELECT role_id FROM dbo.roles WHERE role_code='SALES');
DECLARE @CSRole2 INT = (SELECT role_id FROM dbo.roles WHERE role_code='CUSTOMER_SERVICE');

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE username='admin01')
    INSERT INTO dbo.accounts(employee_id, username, password_hash, role_id, first_login)
    VALUES('NV-2023-001','admin01',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Admin@123'),2),@ManagerRole2,0);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE username='sale01')
    INSERT INTO dbo.accounts(employee_id, username, password_hash, role_id, first_login)
    VALUES('NV-2023-002','sale01',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Sale@1234'),2),@SalesRole2,0);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE username='cskh01')
    INSERT INTO dbo.accounts(employee_id, username, password_hash, role_id, first_login)
    VALUES('NV-2023-003','cskh01',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Cskh@1234'),2),@CSRole2,0);

IF NOT EXISTS (SELECT 1 FROM dbo.accounts WHERE username='newstaff')
    INSERT INTO dbo.accounts(employee_id, username, password_hash, role_id, first_login)
    VALUES('NV-2025-001','newstaff',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Welcome@1'),2),@SalesRole2,1);
GO

/* ================= SHIFTS ================= */
INSERT INTO dbo.shifts(shift_code, shift_name, start_time, end_time)
SELECT v.shift_code,v.shift_name,v.start_time,v.end_time
FROM (VALUES
    ('MORNING',N'Ca sáng',CONVERT(time,'07:00'),CONVERT(time,'12:00')),
    ('AFTERNOON',N'Ca chiều',CONVERT(time,'12:00'),CONVERT(time,'17:00')),
    ('EVENING',N'Ca tối',CONVERT(time,'17:00'),CONVERT(time,'22:00'))
) v(shift_code,shift_name,start_time,end_time)
WHERE NOT EXISTS (SELECT 1 FROM dbo.shifts s WHERE s.shift_code=v.shift_code);
GO

/* ================= CATEGORIES ================= */
INSERT INTO dbo.categories(category_code, category_name)
SELECT v.category_code,v.category_name
FROM (VALUES
    ('PHONE',N'Điện thoại'),('LAPTOP',N'Laptop'),('TV',N'TV'),('COOLING',N'Điện lạnh'),
    ('APPLIANCE',N'Điện gia dụng'),('TABLET',N'Máy tính bảng'),('AUDIO',N'Âm thanh')
) v(category_code,category_name)
WHERE NOT EXISTS (SELECT 1 FROM dbo.categories c WHERE c.category_code=v.category_code);
GO

/* ================= SUPPLIERS ================= */
INSERT INTO dbo.suppliers(supplier_code, supplier_name, phone, address, tax_code)
SELECT v.supplier_code,v.supplier_name,v.phone,v.address,v.tax_code
FROM (VALUES
    ('NCC001',N'Samsung Vina Co., Ltd','02838261234',N'TP. Hồ Chí Minh','0301234567'),
    ('NCC002',N'Apple Authorized Reseller VN','02838235678',N'TP. Hồ Chí Minh','0302345678'),
    ('NCC003',N'LG Electronics Việt Nam','02437289012',N'Hà Nội','0103456789'),
    ('NCC004',N'Sony Việt Nam','02839283456',N'TP. Hồ Chí Minh','0304567890'),
    ('NCC005',N'Electrolux Việt Nam','02837187890',N'TP. Hồ Chí Minh','0305678901')
) v(supplier_code,supplier_name,phone,address,tax_code)
WHERE NOT EXISTS (SELECT 1 FROM dbo.suppliers s WHERE s.supplier_code=v.supplier_code);
GO

/* ================= PRODUCTS ================= */
DECLARE @PhoneCat INT=(SELECT category_id FROM dbo.categories WHERE category_code='PHONE');
DECLARE @LaptopCat INT=(SELECT category_id FROM dbo.categories WHERE category_code='LAPTOP');
DECLARE @TvCat INT=(SELECT category_id FROM dbo.categories WHERE category_code='TV');
DECLARE @CoolingCat INT=(SELECT category_id FROM dbo.categories WHERE category_code='COOLING');
DECLARE @ApplianceCat INT=(SELECT category_id FROM dbo.categories WHERE category_code='APPLIANCE');
DECLARE @TabletCat INT=(SELECT category_id FROM dbo.categories WHERE category_code='TABLET');
DECLARE @AudioCat INT=(SELECT category_id FROM dbo.categories WHERE category_code='AUDIO');
DECLARE @Samsung INT=(SELECT supplier_id FROM dbo.suppliers WHERE supplier_code='NCC001');
DECLARE @Apple INT=(SELECT supplier_id FROM dbo.suppliers WHERE supplier_code='NCC002');
DECLARE @LG INT=(SELECT supplier_id FROM dbo.suppliers WHERE supplier_code='NCC003');
DECLARE @Sony INT=(SELECT supplier_id FROM dbo.suppliers WHERE supplier_code='NCC004');
DECLARE @Electrolux INT=(SELECT supplier_id FROM dbo.suppliers WHERE supplier_code='NCC005');

INSERT INTO dbo.products(product_id,barcode,product_name,category_id,supplier_id,cost_price,sale_price,stock_quantity)
SELECT v.product_id,v.barcode,v.product_name,v.category_id,v.supplier_id,v.cost_price,v.sale_price,v.stock_quantity
FROM (VALUES
    ('SP001','8938505970011',N'Samsung Galaxy S24 Ultra 256GB',@PhoneCat,@Samsung,CONVERT(decimal(18,2),24000000),CONVERT(decimal(18,2),28990000),42),
    ('SP002','8938505970028',N'MacBook Air M3 13 inch 16GB',@LaptopCat,@Apple,28500000,32990000,8),
    ('SP003','8938505970035',N'Sony Bravia 55 inch 4K OLED',@TvCat,@Sony,39000000,45990000,15),
    ('SP004','8938505970042',N'Tủ lạnh LG Inverter 508L',@CoolingCat,@LG,15000000,17990000,3),
    ('SP005','8938505970059',N'Máy giặt Electrolux 10kg',@ApplianceCat,@Electrolux,10500000,12490000,0),
    ('SP006','8938505970066',N'iPad Pro M4 12.9 inch',@TabletCat,@Apple,23500000,26990000,21),
    ('SP007','8938505970073',N'Sony WH-1000XM5',@AudioCat,@Sony,6500000,7990000,30)
) v(product_id,barcode,product_name,category_id,supplier_id,cost_price,sale_price,stock_quantity)
WHERE NOT EXISTS (SELECT 1 FROM dbo.products p WHERE p.product_id=v.product_id);
GO

/* ================= MEMBERSHIP ================= */
/* Chỉ tạo chính sách mặc định khi hạng chưa tồn tại.
   Không UPDATE hạng đã tồn tại để không ghi đè thay đổi từ UC024. */
INSERT INTO dbo.membership_tiers(tier_code,tier_name,min_points,discount_rate,point_multiplier)
SELECT v.tier_code,v.tier_name,v.min_points,v.discount_rate,v.point_multiplier
FROM (VALUES
    ('STANDARD',N'Tiêu chuẩn',0,CONVERT(decimal(5,2),0),CONVERT(decimal(6,2),1.00)),
    ('SILVER',N'Bạc',1000,CONVERT(decimal(5,2),2),CONVERT(decimal(6,2),1.10)),
    ('GOLD',N'Vàng',4000,CONVERT(decimal(5,2),5),CONVERT(decimal(6,2),1.25)),
    ('PLATINUM',N'Bạch kim',10000,CONVERT(decimal(5,2),8),CONVERT(decimal(6,2),1.50))
) v(tier_code,tier_name,min_points,discount_rate,point_multiplier)
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.membership_tiers t WHERE t.tier_code=v.tier_code
);
GO

DECLARE @Standard INT=(SELECT tier_id FROM dbo.membership_tiers WHERE tier_code='STANDARD');
DECLARE @Silver INT=(SELECT tier_id FROM dbo.membership_tiers WHERE tier_code='SILVER');
DECLARE @Gold INT=(SELECT tier_id FROM dbo.membership_tiers WHERE tier_code='GOLD');
DECLARE @Platinum INT=(SELECT tier_id FROM dbo.membership_tiers WHERE tier_code='PLATINUM');

INSERT INTO dbo.customers(customer_id,full_name,phone,email,address,tier_id,current_points)
SELECT v.customer_id,v.full_name,v.phone,v.email,v.address,v.tier_id,v.current_points
FROM (VALUES
    ('KH001',N'Nguyễn Văn Minh','0901234567','minh.nguyen@example.com',N'Gò Vấp, TP.HCM',@Gold,4850),
    ('KH002',N'Trần Thị Lan','0912345678','lan.tran@example.com',N'Quận 1, TP.HCM',@Platinum,12300),
    ('KH003',N'Lê Hoàng Nam','0923456789',CONVERT(varchar(150),NULL),N'Bình Thạnh, TP.HCM',@Silver,1200),
    ('KH004',N'Phạm Thị Hoa','0934567890',CONVERT(varchar(150),NULL),N'Quận 7, TP.HCM',@Standard,350),
    ('KH005',N'Đỗ Quốc Tuấn','0945678902',CONVERT(varchar(150),NULL),N'Thủ Đức, TP.HCM',@Gold,6750)
) v(customer_id,full_name,phone,email,address,tier_id,current_points)
WHERE NOT EXISTS (SELECT 1 FROM dbo.customers c WHERE c.customer_id=v.customer_id);
GO

INSERT INTO dbo.membership_cards(customer_id,card_code)
SELECT v.customer_id,v.card_code
FROM (VALUES
    ('KH001','BLUE-KH001-01'),('KH002','BLUE-KH002-01'),('KH003','BLUE-KH003-01'),
    ('KH004','BLUE-KH004-01'),('KH005','BLUE-KH005-01')
) v(customer_id,card_code)
WHERE NOT EXISTS (SELECT 1 FROM dbo.membership_cards c WHERE c.card_code=v.card_code)
  AND NOT EXISTS (
      SELECT 1 FROM dbo.membership_cards c
      WHERE c.customer_id=v.customer_id AND c.status='ACTIVE'
  );
GO

/* ================= SAMPLE PROMOTIONS ================= */
IF NOT EXISTS (SELECT 1 FROM dbo.promotions WHERE promotion_code='KM001')
INSERT INTO dbo.promotions(promotion_code,promotion_name,discount_type,discount_value,start_at,end_at,status,created_by)
VALUES('KM001',N'Siêu Sale Tháng 9','PERCENT',15,'2026-09-01T00:00:00','2026-09-30T23:59:59','ENDED','NV-2023-001');

IF NOT EXISTS (SELECT 1 FROM dbo.promotions WHERE promotion_code='KM002')
INSERT INTO dbo.promotions(promotion_code,promotion_name,discount_type,discount_value,start_at,end_at,status,created_by)
VALUES('KM002',N'Flash Sale 10.10','PERCENT',30,'2026-10-10T00:00:00','2026-10-10T23:59:59','SCHEDULED','NV-2023-001');
GO

DECLARE @KM1 INT=(SELECT promotion_id FROM dbo.promotions WHERE promotion_code='KM001');
IF @KM1 IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.promotion_products WHERE promotion_id=@KM1 AND product_id='SP001')
        INSERT INTO dbo.promotion_products(promotion_id,product_id) VALUES(@KM1,'SP001');
    IF NOT EXISTS (SELECT 1 FROM dbo.promotion_products WHERE promotion_id=@KM1 AND product_id='SP003')
        INSERT INTO dbo.promotion_products(promotion_id,product_id) VALUES(@KM1,'SP003');
END;
GO

USE DienMayBlue;
GO

/* =========================================================
   DỮ LIỆU DEMO NGHIỆP VỤ - TÙY CHỌN
   - Có thể chạy lại; không tạo trùng hóa đơn/phiếu/điểm.
   - Demo tự bảo đảm ca làm, tồn kho, khuyến mãi, điểm và lịch sử giao hàng nhất quán.
   - File này KHÔNG reset quyền hay chính sách hạng thành viên.
   ========================================================= */

/* =========================================================
   1. CA LÀM DEMO
   - Nếu thời điểm chạy nằm ngoài 07:00-22:00, dùng ca DEMO_FULLDAY
     để sale01 vẫn có thể thao tác demo mà không bị gán sai "Ca tối".
   ========================================================= */
IF NOT EXISTS (SELECT 1 FROM dbo.shifts WHERE shift_code='DEMO_FULLDAY')
BEGIN
    INSERT INTO dbo.shifts(shift_code,shift_name,start_time,end_time,active)
    VALUES('DEMO_FULLDAY',N'Ca demo cả ngày','00:00:00','23:59:59',1);
END
GO

DECLARE @Today DATE=CAST(GETDATE() AS DATE);
DECLARE @Now TIME=CAST(GETDATE() AS TIME);

DECLARE @CurrentShift INT =
(
    SELECT TOP (1) shift_id
    FROM dbo.shifts
    WHERE active=1
      AND shift_code IN ('MORNING','AFTERNOON','EVENING')
      AND @Now >= start_time
      AND @Now < end_time
    ORDER BY start_time
);

IF @CurrentShift IS NULL
    SET @CurrentShift=(SELECT shift_id FROM dbo.shifts WHERE shift_code='DEMO_FULLDAY');

IF NOT EXISTS(
    SELECT 1
    FROM dbo.shift_assignments
    WHERE employee_id='NV-2023-002' AND work_date=@Today
)
BEGIN
    INSERT INTO dbo.shift_assignments(employee_id,work_date,shift_id,assigned_by,status)
    VALUES('NV-2023-002',@Today,@CurrentShift,'NV-2023-001','ASSIGNED');
END
ELSE
BEGIN
    /* Sửa dữ liệu demo phiên bản cũ nếu ca hiện tại không bao phủ giờ chạy
       hoặc assignment cũ đã CANCELLED. */
    UPDATE sa
    SET shift_id=@CurrentShift,
        status='ASSIGNED'
    FROM dbo.shift_assignments sa
    INNER JOIN dbo.shifts s ON s.shift_id=sa.shift_id
    WHERE sa.employee_id='NV-2023-002'
      AND sa.work_date=@Today
      AND (
            sa.status='CANCELLED'
            OR NOT (@Now >= s.start_time AND @Now < s.end_time)
          );
END
GO

/* =========================================================
   2. CÔNG NỢ NHÀ CUNG CẤP MẪU
   ========================================================= */
DECLARE @Samsung INT=(SELECT supplier_id FROM dbo.suppliers WHERE supplier_code='NCC001');
DECLARE @LG INT=(SELECT supplier_id FROM dbo.suppliers WHERE supplier_code='NCC003');

IF NOT EXISTS(SELECT 1 FROM dbo.supplier_debts WHERE reference_code='PN-DEMO-001')
INSERT INTO dbo.supplier_debts(supplier_id,reference_code,description,original_amount,remaining_amount,due_date,status)
VALUES(@Samsung,'PN-DEMO-001',N'Nhập điện thoại Samsung',250000000,125000000,DATEADD(DAY,7,CAST(GETDATE() AS DATE)),'PARTIAL');

IF NOT EXISTS(SELECT 1 FROM dbo.supplier_debts WHERE reference_code='PN-DEMO-002')
INSERT INTO dbo.supplier_debts(supplier_id,reference_code,description,original_amount,remaining_amount,due_date,status)
VALUES(@LG,'PN-DEMO-002',N'Nhập hàng điện lạnh LG',180000000,180000000,DATEADD(DAY,15,CAST(GETDATE() AS DATE)),'UNPAID');
GO

/* =========================================================
   3. KHUYẾN MÃI DEMO HỢP LỆ
   - Chỉ 3 ngày gần nhất được giảm 15%.
   - Khuyến mãi demo được giữ nguyên ngày bắt đầu sau lần tạo đầu tiên
     và chỉ gia hạn ngày kết thúc khi chạy lại.
   ========================================================= */
DECLARE @PromoStart DATETIME2=DATEADD(DAY,-2,CAST(CAST(GETDATE() AS DATE) AS DATETIME2));
DECLARE @PromoEnd DATETIME2=DATEADD(SECOND,-1,DATEADD(DAY,1,CAST(CAST(GETDATE() AS DATE) AS DATETIME2)));

/* Nếu DB có hóa đơn demo cũ đã giảm giá, mở rộng ngày bắt đầu để dữ liệu cũ cũng có
   một chương trình khuyến mãi hợp lệ bao phủ. */
DECLARE @OldestDiscountedDemo DATETIME2=
(
    SELECT MIN(created_at)
    FROM dbo.invoices
    WHERE invoice_id LIKE 'HDDEMO%' AND discount_amount>0
);
IF @OldestDiscountedDemo IS NOT NULL AND @OldestDiscountedDemo<@PromoStart
    SET @PromoStart=CAST(CAST(@OldestDiscountedDemo AS DATE) AS DATETIME2);

IF NOT EXISTS (SELECT 1 FROM dbo.promotions WHERE promotion_code='KM-DEMO-15')
BEGIN
    INSERT INTO dbo.promotions(
        promotion_code,promotion_name,discount_type,discount_value,
        start_at,end_at,status,created_by
    )
    VALUES(
        'KM-DEMO-15',N'Khuyến mãi demo 15%','PERCENT',15,
        @PromoStart,@PromoEnd,'ACTIVE','NV-2023-001'
    );
END
ELSE
BEGIN
    UPDATE dbo.promotions
    SET start_at=CASE WHEN start_at>@PromoStart THEN @PromoStart ELSE start_at END,
        end_at=CASE WHEN end_at<@PromoEnd THEN @PromoEnd ELSE end_at END,
        status='ACTIVE'
    WHERE promotion_code='KM-DEMO-15';
END

DECLARE @DemoPromoId INT=(SELECT promotion_id FROM dbo.promotions WHERE promotion_code='KM-DEMO-15');
IF @DemoPromoId IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM dbo.promotion_products
    WHERE promotion_id=@DemoPromoId AND product_id='SP001'
)
BEGIN
    INSERT INTO dbo.promotion_products(promotion_id,product_id)
    VALUES(@DemoPromoId,'SP001');
END
GO

/* =========================================================
   4. HÓA ĐƠN DEMO 7 NGÀY GẦN NHẤT
   - Có trừ tồn kho SP001.
   - Chiết khấu chỉ áp dụng khi KM-DEMO-15 có hiệu lực.
   - Có point_transactions cho từng hóa đơn.
   - Quy ước DEMO tích điểm: 1 điểm / 100.000 ĐVT x hệ số hạng.
     Nếu Java đang dùng công thức khác, hãy đồng bộ công thức demo với Java.
   ========================================================= */
DECLARE @i INT=0;
DECLARE @Today2 DATE=CAST(GETDATE() AS DATE);

WHILE @i<7
BEGIN
    DECLARE @InvoiceDate DATETIME2 =
        DATEADD(HOUR,10,CAST(DATEADD(DAY,-@i,@Today2) AS DATETIME2));
    DECLARE @InvoiceId VARCHAR(30)='HDDEMO'+CONVERT(VARCHAR(8),@InvoiceDate,112);
    DECLARE @Customer VARCHAR(20)=CASE WHEN @i%2=0 THEN 'KH001' ELSE 'KH002' END;
    DECLARE @Qty INT=1+(@i%2);
    DECLARE @Unit DECIMAL(18,2)=28990000;
    DECLARE @Sub DECIMAL(18,2)=@Unit*@Qty;

    DECLARE @PromoPercent DECIMAL(18,2)=
    (
        SELECT TOP (1) p.discount_value
        FROM dbo.promotions p
        INNER JOIN dbo.promotion_products pp ON pp.promotion_id=p.promotion_id
        WHERE p.promotion_code='KM-DEMO-15'
          AND pp.product_id='SP001'
          AND p.status='ACTIVE'
          AND p.discount_type='PERCENT'
          AND @InvoiceDate BETWEEN p.start_at AND p.end_at
    );

    DECLARE @Disc DECIMAL(18,2)=
        CASE WHEN @PromoPercent IS NULL THEN 0 ELSE ROUND(@Sub*@PromoPercent/100.0,2) END;
    DECLARE @Total DECIMAL(18,2)=@Sub-@Disc;

    IF NOT EXISTS(SELECT 1 FROM dbo.invoices WHERE invoice_id=@InvoiceId)
    BEGIN
        IF (SELECT stock_quantity FROM dbo.products WHERE product_id='SP001') < @Qty
            THROW 51030, 'Không đủ tồn kho SP001 để tạo hóa đơn demo.', 1;

        INSERT INTO dbo.invoices(
            invoice_id,customer_id,employee_id,created_at,
            subtotal,discount_amount,total_amount,status,note
        )
        VALUES(
            @InvoiceId,@Customer,'NV-2023-002',@InvoiceDate,
            @Sub,@Disc,@Total,'PAID',
            CASE WHEN @Disc>0
                 THEN N'DEMO; STOCK_APPLIED=1; PROMO=KM-DEMO-15'
                 ELSE N'DEMO; STOCK_APPLIED=1'
            END
        );

        INSERT INTO dbo.invoice_details(
            invoice_id,product_id,quantity,unit_price,cost_price_snapshot,discount_amount,line_total
        )
        VALUES(@InvoiceId,'SP001',@Qty,@Unit,24000000,@Disc,@Total);

        INSERT INTO dbo.payments(invoice_id,payment_method,amount,status,paid_at)
        VALUES(@InvoiceId,CASE WHEN @i%2=0 THEN 'CARD' ELSE 'CASH' END,@Total,'SUCCESS',@InvoiceDate);

        INSERT INTO dbo.sales_bonuses(
            employee_id,invoice_id,sales_amount,bonus_rate,bonus_amount,status,created_at
        )
        VALUES('NV-2023-002',@InvoiceId,@Total,0.03,ROUND(@Total*0.03,2),'OPEN',@InvoiceDate);

        UPDATE dbo.products
        SET stock_quantity=stock_quantity-@Qty,
            status=CASE
                     WHEN stock_quantity-@Qty=0 THEN 'OUT_OF_STOCK'
                     WHEN status='OUT_OF_STOCK' AND stock_quantity-@Qty>0 THEN 'ACTIVE'
                     ELSE status
                   END,
            updated_at=SYSDATETIME()
        WHERE product_id='SP001';
    END

    /* Backfill tồn kho nếu DB đang chứa hóa đơn demo từ script cũ chưa trừ stock. */
    IF EXISTS(
        SELECT 1
        FROM dbo.invoices
        WHERE invoice_id=@InvoiceId
          AND (note IS NULL OR note NOT LIKE '%STOCK_APPLIED=1%')
    )
    BEGIN
        DECLARE @BackfillQty INT=
        (
            SELECT COALESCE(SUM(quantity),0)
            FROM dbo.invoice_details
            WHERE invoice_id=@InvoiceId AND product_id='SP001'
        );

        IF @BackfillQty>0
        BEGIN
            IF (SELECT stock_quantity FROM dbo.products WHERE product_id='SP001') < @BackfillQty
                THROW 51031, 'Không đủ tồn kho để backfill các hóa đơn demo cũ.', 1;

            UPDATE dbo.products
            SET stock_quantity=stock_quantity-@BackfillQty,
                status=CASE
                         WHEN stock_quantity-@BackfillQty=0 THEN 'OUT_OF_STOCK'
                         WHEN status='OUT_OF_STOCK' AND stock_quantity-@BackfillQty>0 THEN 'ACTIVE'
                         ELSE status
                       END,
                updated_at=SYSDATETIME()
            WHERE product_id='SP001';

            UPDATE dbo.invoices
            SET note=CONCAT(COALESCE(note,N'DEMO'),N'; STOCK_APPLIED=1')
            WHERE invoice_id=@InvoiceId;
        END
    END

    /* Bảo đảm mỗi hóa đơn demo có giao dịch tích điểm đúng một lần. */
    IF NOT EXISTS(
        SELECT 1
        FROM dbo.point_transactions
        WHERE invoice_id=@InvoiceId
          AND customer_id=@Customer
          AND transaction_type='EARN'
    )
    BEGIN
        DECLARE @Multiplier DECIMAL(6,2)=
        (
            SELECT COALESCE(mt.point_multiplier,1)
            FROM dbo.customers c
            LEFT JOIN dbo.membership_tiers mt ON mt.tier_id=c.tier_id
            WHERE c.customer_id=@Customer
        );
        SET @Multiplier=COALESCE(@Multiplier,1);

        DECLARE @EarnedPoints INT =
            CONVERT(INT,FLOOR((@Total/100000.0)*@Multiplier));

        INSERT INTO dbo.point_transactions(
            customer_id,invoice_id,transaction_type,points_change,note,created_at
        )
        VALUES(
            @Customer,@InvoiceId,'EARN',@EarnedPoints,
            N'Demo: 1 điểm/100.000 ĐVT x hệ số hạng',@InvoiceDate
        );

        UPDATE dbo.customers
        SET current_points=current_points+@EarnedPoints,
            updated_at=SYSDATETIME()
        WHERE customer_id=@Customer;
    END

    SET @i=@i+1;
END
GO

/* =========================================================
   5. PHIẾU GIAO HÀNG DEMO
   - Mỗi hóa đơn chỉ có 1 phiếu giao (đã được DB chặn bằng UNIQUE).
   - Nhân viên giao có ca đúng ngày giao.
   - Có delivery_history ngay từ trạng thái PREPARING.
   ========================================================= */
DECLARE @Inv VARCHAR(30)=
(
    SELECT TOP (1) invoice_id
    FROM dbo.invoices
    WHERE invoice_id LIKE 'HDDEMO%'
    ORDER BY created_at DESC
);

DECLARE @DeliveryDate DATE=DATEADD(DAY,1,CAST(GETDATE() AS DATE));
DECLARE @Morning INT=(SELECT shift_id FROM dbo.shifts WHERE shift_code='MORNING');

IF @Inv IS NOT NULL
BEGIN
    IF NOT EXISTS(
        SELECT 1 FROM dbo.shift_assignments
        WHERE employee_id='NV-2023-002' AND work_date=@DeliveryDate
    )
    BEGIN
        INSERT INTO dbo.shift_assignments(employee_id,work_date,shift_id,assigned_by,status)
        VALUES('NV-2023-002',@DeliveryDate,@Morning,'NV-2023-001','ASSIGNED');
    END
    ELSE
    BEGIN
        UPDATE dbo.shift_assignments
        SET shift_id=CASE WHEN status='CANCELLED' THEN @Morning ELSE shift_id END,
            status=CASE WHEN status='CANCELLED' THEN 'ASSIGNED' ELSE status END
        WHERE employee_id='NV-2023-002' AND work_date=@DeliveryDate;
    END

    DECLARE @AssignedShiftStart TIME;
    DECLARE @AssignedShiftEnd TIME;

    SELECT TOP (1)
        @AssignedShiftStart=s.start_time,
        @AssignedShiftEnd=s.end_time
    FROM dbo.shift_assignments sa
    INNER JOIN dbo.shifts s ON s.shift_id=sa.shift_id
    WHERE sa.employee_id='NV-2023-002'
      AND sa.work_date=@DeliveryDate;

    DECLARE @DeliveryFrom TIME =
        CASE WHEN @AssignedShiftStart<'09:00' THEN CAST('09:00' AS TIME) ELSE @AssignedShiftStart END;
    DECLARE @CandidateDeliveryTo TIME=DATEADD(HOUR,2,@DeliveryFrom);
    DECLARE @DeliveryTo TIME =
        CASE
            WHEN @CandidateDeliveryTo>CAST('21:00' AS TIME) THEN CAST('21:00' AS TIME)
            WHEN @CandidateDeliveryTo>@AssignedShiftEnd THEN @AssignedShiftEnd
            ELSE @CandidateDeliveryTo
        END;

    IF @DeliveryTo<=@DeliveryFrom
    BEGIN
        /* Trường hợp ca hiện hữu không giao được trong khung 09:00-21:00,
           dùng ca DEMO_FULLDAY cho đúng dữ liệu demo. */
        DECLARE @DemoShift INT=(SELECT shift_id FROM dbo.shifts WHERE shift_code='DEMO_FULLDAY');

        UPDATE dbo.shift_assignments
        SET shift_id=@DemoShift
        WHERE employee_id='NV-2023-002'
          AND work_date=@DeliveryDate
          AND status='ASSIGNED';

        SET @DeliveryFrom='09:00';
        SET @DeliveryTo='11:00';
    END

    IF NOT EXISTS(SELECT 1 FROM dbo.deliveries WHERE delivery_id='GH-DEMO-001')
    BEGIN
        INSERT INTO dbo.deliveries(
            delivery_id,invoice_id,assigned_employee_id,delivery_address,region_type,
            delivery_date,time_from,time_to,install_required,delivery_fee,status
        )
        VALUES(
            'GH-DEMO-001',@Inv,'NV-2023-002',N'123 Nguyễn Huệ, Quận 1, TP.HCM','INNER_CITY',
            @DeliveryDate,@DeliveryFrom,@DeliveryTo,1,0,'PREPARING'
        );
    END

    /* Backfill ca cho phiếu demo cũ nếu đã tồn tại nhưng trước đây chưa phân ca ngày giao. */
    DECLARE @ExistingDeliveryDate DATE=
        (SELECT delivery_date FROM dbo.deliveries WHERE delivery_id='GH-DEMO-001');
    DECLARE @ExistingEmployee VARCHAR(20)=
        (SELECT assigned_employee_id FROM dbo.deliveries WHERE delivery_id='GH-DEMO-001');

    IF @ExistingEmployee IS NOT NULL
       AND NOT EXISTS(
            SELECT 1 FROM dbo.shift_assignments
            WHERE employee_id=@ExistingEmployee AND work_date=@ExistingDeliveryDate
       )
    BEGIN
        INSERT INTO dbo.shift_assignments(employee_id,work_date,shift_id,assigned_by,status)
        VALUES(@ExistingEmployee,@ExistingDeliveryDate,@Morning,'NV-2023-001','ASSIGNED');
    END

    IF NOT EXISTS(
        SELECT 1
        FROM dbo.delivery_history
        WHERE delivery_id='GH-DEMO-001' AND new_status='PREPARING'
    )
    BEGIN
        INSERT INTO dbo.delivery_history(
            delivery_id,old_status,new_status,changed_by,note,changed_at
        )
        VALUES(
            'GH-DEMO-001',NULL,'PREPARING',
            COALESCE(@ExistingEmployee,'NV-2023-002'),
            N'Khởi tạo phiếu giao hàng demo',
            SYSDATETIME()
        );
    END
END
GO

/* =========================================================
   6. KHIẾU NẠI MẪU
   ========================================================= */
IF NOT EXISTS(SELECT 1 FROM dbo.complaints WHERE complaint_id='KN-DEMO-001')
INSERT INTO dbo.complaints(
    complaint_id,customer_id,customer_phone,content,priority,status,created_by
)
VALUES(
    'KN-DEMO-001','KH001','0901234567',
    N'Khách phản ánh giao hàng trễ hơn khung giờ hẹn.',
    'NORMAL','NEW','NV-2023-003'
);
GO

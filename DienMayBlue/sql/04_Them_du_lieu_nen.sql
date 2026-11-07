/* ĐIỆN MÁY BLUE - FILE 04: DỮ LIỆU NỀN */
USE DienMayBlue;
GO

INSERT INTO dbo.NhanVien
(MaNhanVien,HoTen,SoDienThoai,Email,NgayVaoLam,TrinhDo,HeSoLuong,LuongCoBan,LoaiNhanVien,TrangThai)
VALUES
('NV000',N'Quản trị hệ thống','0901000000','admin@blue.vn','2024-01-01',N'Đại học',3.50,15000000,N'Quản lý',N'Hoạt động'),
('NV001',N'Nguyễn Văn Quản Lý','0901000001','quanly@blue.vn','2024-01-10',N'Đại học',3.20,12000000,N'Quản lý',N'Hoạt động'),
('NV002',N'Trần Minh Bán Hàng','0901000002','banhang1@blue.vn','2024-03-01',N'Cao đẳng',2.50,8500000,N'Bán hàng',N'Hoạt động'),
('NV003',N'Lê Thu Chăm Sóc','0901000003','cskh1@blue.vn','2024-04-15',N'Đại học',2.70,9000000,N'CSKH',N'Hoạt động'),
('NV004',N'Phạm Anh Bán Hàng','0901000004','banhang2@blue.vn','2025-01-05',N'Cao đẳng',2.30,8000000,N'Bán hàng',N'Hoạt động');
GO

/* Giữ nguyên đúng các tài khoản/mật khẩu mẫu đã có trên giao diện code mới. */
INSERT INTO dbo.TaiKhoan
(MaTaiKhoan,TenDangNhap,MatKhau,TrangThai,SoLanDangNhapSai,ThoiGianMoKhoa,LanDangNhapCuoi,MaNhanVien)
VALUES
('TK000','admin',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Admin@1234'),2),N'Hoạt động',0,NULL,NULL,'NV000'),
('TK001','admin01',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Admin@123'),2),N'Hoạt động',0,NULL,NULL,'NV001'),
('TK002','sale01',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Sale@1234'),2),N'Hoạt động',0,NULL,NULL,'NV002'),
('TK003','cskh01',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256','Cskh@1234'),2),N'Hoạt động',0,NULL,NULL,'NV003');
GO

INSERT INTO dbo.NhaCungCap
(MaNhaCungCap,TenNhaCungCap,SoDienThoai,DiaChi,Email,MaSoThue,CongNo,TrangThai)
VALUES
('NCC001',N'Công ty Samsung Việt Nam','0902000001',N'TP.HCM','lienhe@samsung.vn','MST001',0,N'Hoạt động'),
('NCC002',N'Công ty LG Việt Nam','0902000002',N'TP.HCM','lienhe@lg.vn','MST002',0,N'Hoạt động'),
('NCC003',N'Công ty Sony Việt Nam','0902000003',N'TP.HCM','lienhe@sony.vn','MST003',0,N'Hoạt động'),
('NCC004',N'Công ty Panasonic Việt Nam','0902000004',N'TP.HCM','lienhe@panasonic.vn','MST004',0,N'Hoạt động'),
('NCC005',N'Công ty Electrolux Việt Nam','0902000005',N'TP.HCM','lienhe@electrolux.vn','MST005',0,N'Hoạt động');
GO

/* Mã danh mục lấy đúng danh sách từ code/SQL cũ:
   1 Điện thoại, 2 Laptop, 3 TV, 4 Điện lạnh, 5 Điện gia dụng, 6 Máy tính bảng, 7 Âm thanh. */
INSERT INTO dbo.SanPham
(MaSanPham,TenSanPham,MaVach,MaDanhMuc,GiaBan,GiaVon,SoLuongTonKho,SoThangBaoHanh,HinhAnh,TrangThai,TyLeThuong,MaNhaCungCap)
VALUES
('SP001',N'Tivi Samsung 55 inch','8930000000001',3,14990000,12000000,20,24,NULL,N'Hoạt động',0.03,'NCC001'),
('SP002',N'Tủ lạnh LG Inverter 335L','8930000000002',4,12990000,10500000,15,24,NULL,N'Hoạt động',0.03,'NCC002'),
('SP003',N'Máy giặt Panasonic 10 kg','8930000000003',5,8990000,7200000,18,24,NULL,N'Hoạt động',0.03,'NCC004'),
('SP004',N'Máy lạnh Sony Inverter 1.5 HP','8930000000004',4,10990000,9000000,12,24,NULL,N'Hoạt động',0.03,'NCC003'),
('SP005',N'Máy hút bụi Electrolux','8930000000005',5,3990000,3000000,30,12,NULL,N'Hoạt động',0.03,'NCC005'),
('SP006',N'Samsung Galaxy S24 Ultra 256GB','8930000000006',1,29990000,25500000,12,12,NULL,N'Hoạt động',0.03,'NCC001'),
('SP007',N'LG Gram 16 2024','8930000000007',2,32490000,27800000,9,24,NULL,N'Hoạt động',0.03,'NCC002'),
('SP008',N'Sony Bravia 4K 65 inch','8930000000008',3,24990000,21000000,8,24,NULL,N'Hoạt động',0.03,'NCC003'),
('SP009',N'Panasonic Inverter 2 HP','8930000000009',4,16990000,14200000,11,24,NULL,N'Hoạt động',0.03,'NCC004'),
('SP010',N'Electrolux UltimateCare 9 kg','8930000000010',5,11990000,9800000,16,24,NULL,N'Hoạt động',0.03,'NCC005'),
('SP011',N'Samsung Galaxy Tab S9 FE','8930000000011',6,10990000,9200000,14,12,NULL,N'Hoạt động',0.03,'NCC001'),
('SP012',N'LG XBoom XL7S','8930000000012',7,7990000,6400000,10,12,NULL,N'Hoạt động',0.03,'NCC002'),
('SP013',N'Sony Xperia 1 VI 256GB','8930000000013',1,27990000,23900000,7,12,NULL,N'Hoạt động',0.03,'NCC003'),
('SP014',N'Panasonic Toughbook 14','8930000000014',2,28990000,24500000,6,24,NULL,N'Hoạt động',0.03,'NCC004'),
('SP015',N'Electrolux EOT4022XFG','8930000000015',5,2990000,2250000,25,12,NULL,N'Hoạt động',0.03,'NCC005'),
('SP016',N'Samsung Neo QLED 55 inch','8930000000016',3,22990000,19500000,13,24,NULL,N'Hoạt động',0.03,'NCC001'),
('SP017',N'LG OLED C4 55 inch','8930000000017',3,31990000,27400000,7,24,NULL,N'Hoạt động',0.03,'NCC002'),
('SP018',N'Sony WH-1000XM5','8930000000018',7,7990000,6500000,22,12,NULL,N'Hoạt động',0.03,'NCC003'),
('SP019',N'Panasonic NR-BV361BPKV','8930000000019',4,14990000,12300000,10,24,NULL,N'Hoạt động',0.03,'NCC004'),
('SP020',N'Electrolux EWF1024D3WB','8930000000020',5,10490000,8500000,17,24,NULL,N'Hoạt động',0.03,'NCC005'),
('SP021',N'Samsung Galaxy A55 5G','8930000000021',1,9990000,8200000,28,12,NULL,N'Hoạt động',0.03,'NCC001'),
('SP022',N'LG UltraPC 16','8930000000022',2,21990000,18500000,12,24,NULL,N'Hoạt động',0.03,'NCC002'),
('SP023',N'Sony SRS-XV800','8930000000023',7,12490000,10100000,9,12,NULL,N'Hoạt động',0.03,'NCC003'),
('SP024',N'Panasonic Lumix G100D','8930000000024',5,17990000,15000000,8,12,NULL,N'Hoạt động',0.03,'NCC004'),
('SP025',N'Electrolux EHE5224B-A','8930000000025',4,21990000,18400000,6,24,NULL,N'Hoạt động',0.03,'NCC005'),
('SP026',N'Samsung Galaxy Tab A9+','8930000000026',6,6990000,5600000,20,12,NULL,N'Hoạt động',0.03,'NCC001'),
('SP027',N'LG Soundbar S75Q','8930000000027',7,8990000,7200000,15,12,NULL,N'Hoạt động',0.03,'NCC002'),
('SP028',N'Sony Bravia 4K 50 inch','8930000000028',3,15990000,13200000,14,24,NULL,N'Hoạt động',0.03,'NCC003'),
('SP029',N'Panasonic NN-ST34NBYUE','8930000000029',5,3490000,2700000,23,12,NULL,N'Hoạt động',0.03,'NCC004'),
('SP030',N'Electrolux EOT3218XG','8930000000030',5,2490000,1900000,31,12,NULL,N'Hoạt động',0.03,'NCC005'),
('SP031',N'Samsung Galaxy S24 FE','8930000000031',1,18990000,15800000,19,12,NULL,N'Hoạt động',0.03,'NCC001'),
('SP032',N'LG DualCool 1 HP','8930000000032',4,9490000,7700000,16,24,NULL,N'Hoạt động',0.03,'NCC002'),
('SP033',N'Sony LinkBuds S','8930000000033',7,4490000,3500000,24,12,NULL,N'Hoạt động',0.03,'NCC003'),
('SP034',N'Panasonic F-PXV50A','8930000000034',5,5990000,4700000,18,12,NULL,N'Hoạt động',0.03,'NCC004'),
('SP035',N'Electrolux EFP31315','8930000000035',5,4590000,3600000,26,12,NULL,N'Hoạt động',0.03,'NCC005');
GO

INSERT INTO dbo.KhachHang(MaKhachHang,HoTen,SoDienThoai,Email,DiaChi,TrangThai)
VALUES
('KH000',N'Khách vãng lai','0000000000',NULL,NULL,N'Hoạt động'),
('KH001',N'Nguyễn Minh Anh','0911000001','minhanh@gmail.com',N'Quận 1, TP.HCM',N'Hoạt động'),
('KH002',N'Trần Quốc Huy','0911000002','quochuy@gmail.com',N'Quận 3, TP.HCM',N'Hoạt động'),
('KH003',N'Lê Ngọc Mai','0911000003','ngocmai@gmail.com',N'Gò Vấp, TP.HCM',N'Hoạt động'),
('KH004',N'Trần Kha','0897564432','kha@gmail.com',N'Tân Phú, TP.HCM',N'Hoạt động'),
('KH005',N'Phạm Gia Bảo','0911000005','giabao@gmail.com',N'Bình Thạnh, TP.HCM',N'Hoạt động'),
('KH006',N'Võ Thanh Hà','0911000006','thanhha@gmail.com',N'Quận 7, TP.HCM',N'Hoạt động'),
('KH007',N'Đặng Quốc Nam','0911000007','quocnam@gmail.com',N'Thủ Đức, TP.HCM',N'Hoạt động'),
('KH008',N'Bùi Khánh Linh','0911000008','khanhlinh@gmail.com',N'Quận 10, TP.HCM',N'Hoạt động'),
('KH009',N'Nguyễn Hoàng Phúc','0911000009','hoangphuc@gmail.com',N'Quận 5, TP.HCM',N'Hoạt động'),
('KH010',N'Trần Thảo Vy','0911000010','thaovy@gmail.com',N'Quận 8, TP.HCM',N'Hoạt động'),
('KH011',N'Lê Minh Khang','0911000011','minhkhang@gmail.com',N'Bình Tân, TP.HCM',N'Hoạt động'),
('KH012',N'Phạm Tú Anh','0911000012','tuanh@gmail.com',N'Phú Nhuận, TP.HCM',N'Hoạt động'),
('KH013',N'Võ Nhật Minh','0911000013','nhatminh@gmail.com',N'Quận 4, TP.HCM',N'Hoạt động'),
('KH014',N'Đỗ Ngọc Hân','0911000014','ngochan@gmail.com',N'Tân Bình, TP.HCM',N'Hoạt động'),
('KH015',N'Nguyễn Đức Long','0911000015','duclong@gmail.com',N'Quận 11, TP.HCM',N'Hoạt động'),
('KH016',N'Trần Gia Hân','0911000016','giahan@gmail.com',N'Quận 6, TP.HCM',N'Hoạt động'),
('KH017',N'Lê Quốc Bảo','0911000017','quocbao@gmail.com',N'Nhà Bè, TP.HCM',N'Hoạt động'),
('KH018',N'Phan Thùy Dương','0911000018','thuyduong@gmail.com',N'Quận 12, TP.HCM',N'Hoạt động'),
('KH019',N'Huỳnh Minh Tú','0911000019','minhtu@gmail.com',N'Hóc Môn, TP.HCM',N'Hoạt động'),
('KH020',N'Ngô Thanh Trúc','0911000020','thanhtruc@gmail.com',N'Củ Chi, TP.HCM',N'Hoạt động'),
('KH021',N'Đinh Anh Khoa','0911000021','anhkhoa@gmail.com',N'Quận 2, TP.HCM',N'Hoạt động'),
('KH022',N'Đoàn Mỹ Duyên','0911000022','myduyen@gmail.com',N'Quận 9, TP.HCM',N'Hoạt động'),
('KH023',N'Tạ Quang Vinh','0911000023','quangvinh@gmail.com',N'Bình Chánh, TP.HCM',N'Hoạt động'),
('KH024',N'Mai Bảo Ngọc','0911000024','baongoc@gmail.com',N'Gò Vấp, TP.HCM',N'Hoạt động'),
('KH025',N'Nguyễn Hải Yến','0911000025','haiyen@gmail.com',N'Quận 1, TP.HCM',N'Hoạt động'),
('KH026',N'Trần Minh Quân','0911000026','minhquan@gmail.com',N'Quận 3, TP.HCM',N'Hoạt động'),
('KH027',N'Lê Kim Oanh','0911000027','kimoanh@gmail.com',N'Tân Phú, TP.HCM',N'Hoạt động'),
('KH028',N'Phạm Quốc Thịnh','0911000028','quocthinh@gmail.com',N'Thủ Đức, TP.HCM',N'Hoạt động'),
('KH029',N'Võ Hà My','0911000029','hamy@gmail.com',N'Bình Thạnh, TP.HCM',N'Hoạt động'),
('KH030',N'Bùi Tuấn Kiệt','0911000030','tuankiet@gmail.com',N'Quận 7, TP.HCM',N'Hoạt động');
GO

INSERT INTO dbo.TheThanhVien(MaThe,DiemTichLuy,HangThanhVien,NgayCap,NgayVoHieu,TrangThai,MaKhachHang)
VALUES
('BLUE-KH001-01',1200,N'Bạc','2026-01-10',NULL,N'Hoạt động','KH001'),
('BLUE-KH002-01',4500,N'Vàng','2026-02-15',NULL,N'Hoạt động','KH002'),
('BLUE-KH003-01',200,N'Tiêu chuẩn','2026-03-01',NULL,N'Hoạt động','KH003'),
('BLUE-KH004-01',650,N'Tiêu chuẩn','2026-04-05',NULL,N'Hoạt động','KH004'),
('BLUE-KH005-01',1800,N'Bạc','2026-04-12',NULL,N'Hoạt động','KH005'),
('BLUE-KH006-01',5200,N'Vàng','2026-04-20',NULL,N'Hoạt động','KH006'),
('BLUE-KH007-01',300,N'Tiêu chuẩn','2026-05-01',NULL,N'Hoạt động','KH007'),
('BLUE-KH008-01',2600,N'Bạc','2026-05-08',NULL,N'Hoạt động','KH008'),
('BLUE-KH009-01',10200,N'Bạch kim','2026-05-15',NULL,N'Hoạt động','KH009'),
('BLUE-KH010-01',900,N'Tiêu chuẩn','2026-05-22',NULL,N'Hoạt động','KH010'),
('BLUE-KH011-01',3400,N'Bạc','2026-06-01',NULL,N'Hoạt động','KH011'),
('BLUE-KH012-01',6100,N'Vàng','2026-06-10',NULL,N'Hoạt động','KH012');
GO

INSERT INTO dbo.KhuyenMai
(MaKhuyenMai,TenChuongTrinh,NgayBatDau,NgayKetThuc,LoaiGiam,GiaTriGiam,TrangThai,MaNhanVienTao)
VALUES
('KM001',N'Khuyến mãi cuối tuần','2026-11-01','2026-11-30',N'Phần trăm',10,N'Hoạt động','NV001'),
('KM002',N'Ưu đãi máy hút bụi','2026-11-01','2026-12-31',N'Số tiền',300000,N'Hoạt động','NV001');
GO

INSERT INTO dbo.ChiTietKhuyenMai(MaSanPham,MaKhuyenMai)
VALUES ('SP001','KM001'),('SP003','KM001'),('SP005','KM002');
GO

INSERT INTO dbo.CaLamViec
(MaCa,NgayLamViec,GioBatDau,GioKetThuc,LoaiCa,GhiChu,TrangThai,MaNhanVien,MaNguoiPhanCong)
VALUES
('CA001','2026-11-05','08:00','12:00',N'Sáng',NULL,N'Đã phân công','NV002','NV001'),
('CA002','2026-11-05','12:00','17:00',N'Chiều',NULL,N'Đã phân công','NV004','NV001'),
('CA003','2026-11-05','17:00','21:00',N'Tối',NULL,N'Đã phân công','NV002','NV001');
GO

PRINT N'Đã thêm dữ liệu nền.';
GO

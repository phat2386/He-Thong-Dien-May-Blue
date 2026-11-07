/* ĐIỆN MÁY BLUE - FILE 05: DỮ LIỆU MẪU NGHIỆP VỤ */
USE DienMayBlue;
GO

INSERT INTO dbo.HoaDon
(MaHoaDon,NgayLap,TamTinh,TienGiam,TongTien,TrangThai,PhuongThucThanhToan,MaThamChieuThanhToan,GhiChu,DaChotThuong,NgayChotThuong,MaNhanVienBanHang,MaKhachHang)
VALUES
('HD00001','2026-11-05T09:15:00',18980000,300000,18680000,N'Đã thanh toán',N'Tiền mặt',NULL,NULL,0,NULL,'NV002','KH001'),
('HD00002','2026-11-05T14:20:00',12990000,0,12990000,N'Đã thanh toán',N'Chuyển khoản','GD00002',NULL,0,NULL,'NV004','KH002'),
('HD00003','2026-11-05T18:10:00',8990000,0,8990000,N'Đã thanh toán',N'Tiền mặt',NULL,NULL,0,NULL,'NV002','KH003');
GO

INSERT INTO dbo.ChiTietHoaDon
(MaHoaDon,MaSanPham,SoLuong,DonGia,GiaVonLucBan,TienGiam,ThanhTien)
VALUES
('HD00001','SP001',1,14990000,12000000,0,14990000),
('HD00001','SP005',1,3990000,3000000,300000,3690000),
('HD00002','SP002',1,12990000,10500000,0,12990000),
('HD00003','SP003',1,8990000,7200000,0,8990000);
GO

INSERT INTO dbo.PhieuGiaoHang
(MaPhieu,NgayGiao,DiaChiGiao,TrangThai,CanLapDat,PhiGiaoHang,MaHoaDon,MaNhanVienGiaoHang,MaKhachHang)
VALUES
('GH00001','2026-11-06',N'Quận 1, TP.HCM',N'Đang chờ',1,100000,'HD00001','NV002','KH001'),
('GH00002','2026-11-06',N'Quận 3, TP.HCM',N'Hoàn tất',0,0,'HD00002','NV004','KH002');
GO

INSERT INTO dbo.PhieuBaoHanh(MaPhieu,NgayTiepNhan,TinhTrang,MoTaLoi,HanBaoHanh,NgayHoanThanh,MaKhachHang,MaNhanVienCSKH)
VALUES ('BH00001','2026-11-05',N'Đang kiểm tra',N'Máy hoạt động không ổn định','2027-11-05',NULL,'KH001','NV003');
INSERT INTO dbo.ChiTietPhieuBaoHanh(MaSanPham,MaPhieuBaoHanh) VALUES ('SP005','BH00001');
GO

INSERT INTO dbo.KhieuNai(MaKhieuNai,NoiDung,NgayGhiNhan,TrangThaiXuLy,MucUuTien,MaNguoiTao,HanXuLy,DaLeoThang,MaKhachHang,MaNhanVienCSKH)
VALUES ('KN001',N'Khách phản ánh thời gian giao hàng chậm.','2026-11-05',N'Đang xử lý',N'Bình thường','NV003','2026-11-07T17:00:00',0,'KH002','NV003');
GO

INSERT INTO dbo.PhieuDoiTraHoanTien
(MaPhieu,NgayLap,LoaiLyDo,LyDo,TinhTrangVatLy,ConTemMac,DayDuHop,DuPhuKien,GiaTriHoan,CanDuyet,TrangThai,NgayHoanTat,MaNhanVienQuanLy,MaNhanVienBanHang,MaHoaDon,MaKhachHang)
VALUES ('HT00001','2026-11-05',N'Lỗi nhà sản xuất',N'Sản phẩm lỗi kỹ thuật',N'Còn nguyên trạng',1,1,1,3690000,0,N'Đã duyệt','2026-11-05T19:00:00','NV001','NV002','HD00001','KH001');
INSERT INTO dbo.ChiTietPhieuDoiTra(MaPhieu,MaSanPham,SoLuongTra,GiaTriHoan)
VALUES ('HT00001','SP005',1,3690000);
GO

PRINT N'Đã thêm dữ liệu mẫu nghiệp vụ.';
GO

/* ĐIỆN MÁY BLUE - FILE 06: KIỂM TRA SAU CÀI ĐẶT */
USE DienMayBlue;
GO
DECLARE @SoBang INT=(SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID('dbo'));
IF @SoBang<>17 THROW 51010,N'CSDL phải có đúng 17 bảng vật lý.',1;
GO
DECLARE @Bang TABLE(TenBang SYSNAME);
INSERT INTO @Bang VALUES
('TheThanhVien'),('KhachHang'),('NhanVien'),('TaiKhoan'),('CaLamViec'),('KhieuNai'),('HoaDon'),('ChiTietHoaDon'),
('SanPham'),('NhaCungCap'),('PhieuGiaoHang'),('PhieuBaoHanh'),('ChiTietPhieuBaoHanh'),('PhieuDoiTraHoanTien'),('ChiTietPhieuDoiTra'),('KhuyenMai'),('ChiTietKhuyenMai');
IF EXISTS(SELECT 1 FROM @Bang b WHERE OBJECT_ID(N'dbo.'+QUOTENAME(b.TenBang),'U') IS NULL)
    THROW 51011,N'Thiếu bảng bắt buộc trong lược đồ.',1;
GO
IF COL_LENGTH('dbo.PhieuGiaoHang','MaNhanVienGiaoHang') IS NULL OR COL_LENGTH('dbo.PhieuGiaoHang','MaKhachHang') IS NULL
    THROW 51012,N'Phiếu giao hàng chưa có đủ Mã nhân viên giao hàng và Mã khách hàng.',1;
IF COL_LENGTH('dbo.NhaCungCap','Email') IS NULL
    THROW 51022,N'Nhà cung cấp chưa có cột Email.',1;
IF COL_LENGTH('dbo.PhieuDoiTraHoanTien','MaNhanVienBanHang') IS NULL OR COL_LENGTH('dbo.PhieuDoiTraHoanTien','MaNhanVienQuanLy') IS NULL OR COL_LENGTH('dbo.PhieuDoiTraHoanTien','MaKhachHang') IS NULL
    THROW 51013,N'Phiếu đổi trả/hoàn tiền chưa có đủ các mã nhân viên và khách hàng.',1;
IF COL_LENGTH('dbo.ChiTietPhieuDoiTra','MaPhieu') IS NULL OR COL_LENGTH('dbo.ChiTietPhieuDoiTra','MaSanPham') IS NULL OR COL_LENGTH('dbo.ChiTietPhieuDoiTra','SoLuongTra') IS NULL
    THROW 51016,N'Chi tiết phiếu đổi trả chưa có đủ mã phiếu, mã sản phẩm và số lượng trả.',1;
GO
SELECT t.name AS TenBang FROM sys.tables t WHERE t.schema_id=SCHEMA_ID('dbo') ORDER BY t.name;
GO
SELECT N'NhanVien' Bang,COUNT(*) SoDong FROM dbo.NhanVien
UNION ALL SELECT N'TaiKhoan',COUNT(*) FROM dbo.TaiKhoan
UNION ALL SELECT N'KhachHang',COUNT(*) FROM dbo.KhachHang
UNION ALL SELECT N'TheThanhVien',COUNT(*) FROM dbo.TheThanhVien
UNION ALL SELECT N'NhaCungCap',COUNT(*) FROM dbo.NhaCungCap
UNION ALL SELECT N'SanPham',COUNT(*) FROM dbo.SanPham
UNION ALL SELECT N'HoaDon',COUNT(*) FROM dbo.HoaDon
UNION ALL SELECT N'ChiTietHoaDon',COUNT(*) FROM dbo.ChiTietHoaDon
UNION ALL SELECT N'PhieuGiaoHang',COUNT(*) FROM dbo.PhieuGiaoHang
UNION ALL SELECT N'PhieuBaoHanh',COUNT(*) FROM dbo.PhieuBaoHanh
UNION ALL SELECT N'KhieuNai',COUNT(*) FROM dbo.KhieuNai
UNION ALL SELECT N'PhieuDoiTraHoanTien',COUNT(*) FROM dbo.PhieuDoiTraHoanTien
UNION ALL SELECT N'ChiTietPhieuDoiTra',COUNT(*) FROM dbo.ChiTietPhieuDoiTra
UNION ALL SELECT N'KhuyenMai',COUNT(*) FROM dbo.KhuyenMai
UNION ALL SELECT N'ChiTietKhuyenMai',COUNT(*) FROM dbo.ChiTietKhuyenMai
UNION ALL SELECT N'CaLamViec',COUNT(*) FROM dbo.CaLamViec
UNION ALL SELECT N'ChiTietPhieuBaoHanh',COUNT(*) FROM dbo.ChiTietPhieuBaoHanh;
GO
IF EXISTS(SELECT 1 FROM dbo.HoaDon WHERE TrangThai<>N'Đã thanh toán')
    THROW 51014,N'Hóa đơn đã lưu phải có trạng thái Đã thanh toán.',1;
IF EXISTS(SELECT 1 FROM dbo.PhieuGiaoHang WHERE TrangThai NOT IN(N'Đang chờ',N'Hoàn tất'))
    THROW 51015,N'Phiếu giao hàng chỉ được có trạng thái Đang chờ hoặc Hoàn tất.',1;
GO
IF EXISTS(SELECT 1 FROM dbo.HoaDon WHERE PhuongThucThanhToan IS NULL OR PhuongThucThanhToan NOT IN (N'Tiền mặt',N'Chuyển khoản'))
    THROW 51018,N'Hóa đơn chỉ được dùng Tiền mặt hoặc Chuyển khoản.',1;
GO
IF NOT EXISTS(SELECT 1 FROM dbo.KhachHang WHERE MaKhachHang='KH000' AND HoTen=N'Khách vãng lai')
    THROW 51019,N'Thiếu khách vãng lai KH000 trong dữ liệu nền.',1;
IF EXISTS(SELECT 1 FROM dbo.PhieuDoiTraHoanTien WHERE TrangThai=N'Đã duyệt' AND MaNhanVienQuanLy IS NULL)
    THROW 51020,N'Phiếu đổi trả đã duyệt phải có Mã nhân viên quản lý.',1;
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.TaiKhoan') AND name='DangNhapLanDau')
    THROW 51021,N'Cột DangNhapLanDau đã bị loại khỏi nghiệp vụ và không được tồn tại.',1;
GO

PRINT N'KIỂM TRA HOÀN TẤT: đúng 17 bảng vật lý và các cột nghiệp vụ cập nhật.';
GO

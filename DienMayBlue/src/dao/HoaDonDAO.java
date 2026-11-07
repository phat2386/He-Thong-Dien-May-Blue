package dao;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import entity.*;
public class HoaDonDAO extends CoSoDAO {
    public String goiYMaHoaDon() throws SQLException {
        return goiYMaTiepTheo("HoaDon", "MaHoaDon", "HD", 5);
    }
    public Map<String, Object> timTrangThaiBanHangTaiKhoan(Connection ketNoi, String maTaiKhoan) throws SQLException {
        return motDong(ketNoi, "SELECT CASE tk.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END account_status," +
            "nv.TrangThai employee_status,CASE WHEN tk.TenDangNhap='admin' THEN 'ADMIN' WHEN nv.LoaiNhanVien=N'Quản lý' THEN 'MANAGER' " +
            "WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 'SALES' WHEN nv.LoaiNhanVien=N'CSKH' THEN 'CUSTOMER_SERVICE' ELSE 'UNKNOWN' END role_code," +
            "CASE WHEN nv.LoaiNhanVien IN(N'Bán hàng',N'CSKH') THEN CAST(1 AS bit) ELSE CAST(0 AS bit) END can_shift " +
            "FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien WHERE tk.MaTaiKhoan=?", maTaiKhoan);
    }
    public boolean coCaHienTai(Connection ketNoi, String maNhanVien) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.CaLamViec WHERE MaNhanVien=? AND NgayLamViec=CAST(GETDATE() AS DATE) " +
            "AND TrangThai=N'Đã phân công' AND CAST(GETDATE() AS TIME)>=GioBatDau AND CAST(GETDATE() AS TIME)<GioKetThuc", maNhanVien);
        return v != null && ((Number) v).intValue() > 0;
    }
    public boolean maHoaDonTonTai(Connection ketNoi, String maHoaDon) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.HoaDon WHERE MaHoaDon=?", maHoaDon);
        return v != null && ((Number) v).intValue() > 0;
    }
    public List<Map<String, Object>> timKhuyenMaiDangHoatDong(Connection ketNoi, String maSanPham, int maDanhMuc) throws SQLException {
        return truyVan(ketNoi, "SELECT DISTINCT CASE km.LoaiGiam WHEN N'Phần trăm' THEN 'PERCENT' ELSE 'FIXED' " + "END discount_type,km.GiaTriGiam discount_value " +
            "FROM dbo.KhuyenMai km JOIN dbo.ChiTietKhuyenMai ct ON ct.MaKhuyenMai=km.MaKhuyenMai " + "WHERE ct.MaSanPham=? AND km.TrangThai=N'Hoạt động' AND CAST(GETDATE() AS DATE) " + "BETWEEN km.NgayBatDau AND km.NgayKetThuc", maSanPham);
    }
    public void themHoaDon(Connection ketNoi, String maHoaDon, String maKhachHang, String maNhanVien, BigDecimal tamTinh, BigDecimal tienGiam, BigDecimal tong, String phuongThucThanhToan, Object ghiChu) throws SQLException {
        capNhat(ketNoi, "INSERT INTO dbo.HoaDon(MaHoaDon,NgayLap,TamTinh,TienGiam,TongTien,TrangThai," + "PhuongThucThanhToan,GhiChu,MaNhanVienBanHang,MaKhachHang) VALUES(?,SYSDATETIME(),?,?,?,N'Đã thanh " + "toán',?,?,?,?)",
            maHoaDon, tamTinh, tienGiam, tong, phuongThucThanhToan, ghiChu, maNhanVien, maKhachHang);
    }
    public void yeuCauKhachVangLaiTonTai(Connection ketNoi) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.KhachHang WHERE MaKhachHang='KH000' AND TrangThai=N'Hoạt động'");
        if (v == null || ((Number) v).intValue() == 0) throw new SQLException("Thiếu dữ liệu nền KH000 - Khách vãng lai.");
    }
    public int truTonKhoSanPham(Connection ketNoi, String maSanPham, int soLuong) throws SQLException {
        return capNhat(ketNoi, "UPDATE dbo.SanPham SET SoLuongTonKho=SoLuongTonKho-?,TrangThai=CASE WHEN " + "SoLuongTonKho-?=0 THEN N'Hết hàng' ELSE TrangThai END WHERE MaSanPham=? AND " +
            "TrangThai=N'Hoạt động' AND SoLuongTonKho>=?", soLuong, soLuong, maSanPham, soLuong);
    }
    public void themChiTietHoaDon(Connection ketNoi, String maHoaDon, String maSanPham, int soLuong, BigDecimal donGia, BigDecimal giaVon, BigDecimal tienGiam, BigDecimal thanhTienDong) throws SQLException {
        capNhat(ketNoi, "INSERT INTO dbo.ChiTietHoaDon(MaHoaDon,MaSanPham,SoLuong,DonGia,GiaVonLucBan," + "TienGiam,ThanhTien) VALUES(?,?,?,?,?,?,?)", maHoaDon, maSanPham, soLuong, donGia, giaVon, tienGiam, thanhTienDong);
    }
    public void themThanhToan(Connection ketNoi, String maHoaDon, String phuongThuc, BigDecimal soTien, Object maThamChieuGiaoDich) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.HoaDon SET PhuongThucThanhToan=?,MaThamChieuThanhToan=? WHERE MaHoaDon=?", phuongThuc, maThamChieuGiaoDich, maHoaDon);
    }
    public void congDiemKhachHang(Connection ketNoi, String maKhachHang, int diem) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TheThanhVien SET DiemTichLuy=DiemTichLuy+? WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", diem, maKhachHang);
    }
    public void capNhatHangKhachHang(Connection ketNoi, String maKhachHang, String hangThanhVien) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TheThanhVien SET HangThanhVien=? WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", hangThanhVien, maKhachHang);
    }
    public int diemThanhVien(Connection ketNoi, String maKhachHang) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT ISNULL(DiemTichLuy,0) FROM dbo.TheThanhVien WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", maKhachHang);
        return v == null ? 0 : ((Number) v).intValue();
    }
    public int demBangKeTrungDaChot(Connection ketNoi, LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.HoaDon WHERE DaChotThuong=1 AND CAST(NgayLap AS date) BETWEEN ? AND ?", tuNgay, denNgay);
        return v == null ? 0 : ((Number) v).intValue();
    }
    public List<Map<String, Object>> timThuongMoTheoNhanVien(Connection ketNoi, LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        return truyVan(ketNoi, "WITH hoan AS (SELECT dt.MaHoaDon,ctdt.MaSanPham,SUM(ctdt.GiaTriHoan) tienHoan FROM " + "dbo.PhieuDoiTraHoanTien dt JOIN dbo.ChiTietPhieuDoiTra ctdt ON ctdt.MaPhieu=dt.MaPhieu WHERE dt.TrangThai=N'Đã duyệt' AND dt.NgayHoanTat IS NOT " +
            "NULL GROUP BY dt.MaHoaDon,ctdt.MaSanPham) SELECT hd.MaNhanVienBanHang employee_id," + "SUM(ct.ThanhTien-ISNULL(h.tienHoan,0)) total_sales," +
            "SUM((ct.ThanhTien-ISNULL(h.tienHoan,0))*sp.TyLeThuong) total_bonus FROM " + "dbo.HoaDon hd JOIN dbo.ChiTietHoaDon ct ON ct.MaHoaDon=hd.MaHoaDon JOIN " +
            "dbo.SanPham sp ON sp.MaSanPham=ct.MaSanPham LEFT JOIN hoan h ON " + "h.MaHoaDon=ct.MaHoaDon AND h.MaSanPham=ct.MaSanPham WHERE CAST(hd.NgayLap AS " +
            "date) BETWEEN ? AND ? AND hd.TrangThai=N'Đã thanh toán' AND hd.DaChotThuong=0 " + "GROUP BY hd.MaNhanVienBanHang", tuNgay, denNgay);
    }
    public void danhDauThuongDaChot(Connection ketNoi, LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.HoaDon SET DaChotThuong=1,NgayChotThuong=SYSDATETIME() WHERE " + "DaChotThuong=0 AND TrangThai=N'Đã thanh toán' AND CAST(NgayLap AS date) " + "BETWEEN ? AND ?", tuNgay, denNgay);
    }
    public List<Map<String, Object>> danhSachKhachHangHoatDong() throws SQLException {
        return truyVan("SELECT MaKhachHang customer_id,HoTen full_name,SoDienThoai phone FROM " + "dbo.KhachHang WHERE TrangThai=N'Hoạt động' ORDER BY HoTen");
    }
    public List<Map<String, Object>> danhSachSanPhamConHang() throws SQLException {
        return truyVan("SELECT MaSanPham product_id,MaVach barcode,TenSanPham product_name,GiaBan " + "sale_price,SoLuongTonKho stock_quantity FROM dbo.SanPham WHERE " + "TrangThai=N'Hoạt động' AND SoLuongTonKho>0 ORDER BY TenSanPham");
    }
    public List<Map<String, Object>> timKhuyenMaiDangHoatDong(String maSanPham, int maDanhMuc) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            return timKhuyenMaiDangHoatDong(c, maSanPham, maDanhMuc);
        }
    }
    public BigDecimal doanhThuThuanHomNay() throws SQLException {
        Object v = motGiaTri("SELECT ISNULL(SUM(h.TongTien-ISNULL(r.refund,0)),0) FROM dbo.HoaDon h LEFT " + "JOIN (SELECT MaHoaDon,SUM(GiaTriHoan) refund FROM dbo.PhieuDoiTraHoanTien " +
            "WHERE TrangThai=N'Đã duyệt' GROUP BY MaHoaDon) r ON r.MaHoaDon=h.MaHoaDon " + "WHERE CAST(h.NgayLap AS date)=CAST(GETDATE() AS date) AND h.TrangThai=N'Đã " + "thanh toán'");
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }
    public int demHoaDonHomNay() throws SQLException {
        Object v = motGiaTri("SELECT COUNT(*) FROM dbo.HoaDon WHERE CAST(NgayLap AS date)=CAST(GETDATE() AS " + "date) AND TrangThai=N'Đã thanh toán'");
        return v == null ? 0 : ((Number) v).intValue();
    }
    public int demPhieuGiaoDangChuanBi() throws SQLException {
        Object v = motGiaTri("SELECT COUNT(*) FROM dbo.PhieuGiaoHang WHERE TrangThai=N'Đang chờ'");
        return v == null ? 0 : ((Number) v).intValue();
    }
    public int demPhieuDoiTraDangXuLy() throws SQLException {
        Object v = motGiaTri("SELECT COUNT(*) FROM dbo.PhieuDoiTraHoanTien WHERE TrangThai=N'Chờ duyệt'");
        return v == null ? 0 : ((Number) v).intValue();
    }
    public List<Map<String, Object>> danhSachHoaDonQuanLy() throws SQLException {
        return truyVan("SELECT h.MaHoaDon invoice_id,c.HoTen customer,nv.HoTen employee,h.NgayLap created_at," + "h.PhuongThucThanhToan payment_method,h.TongTien total_amount," +
            "h.TongTien-ISNULL((SELECT SUM(dt.GiaTriHoan) FROM dbo.PhieuDoiTraHoanTien dt " + "WHERE dt.MaHoaDon=h.MaHoaDon AND dt.TrangThai=N'Đã duyệt'),0) net_total," +
            "h.TrangThai status FROM dbo.HoaDon h " + "JOIN dbo.KhachHang c ON c.MaKhachHang=h.MaKhachHang " + "JOIN dbo.NhanVien nv ON nv.MaNhanVien=h.MaNhanVienBanHang ORDER BY h.NgayLap DESC");
    }
    public List<Map<String, Object>> dongChiTietHoaDonQuanLy(String maHoaDon) throws SQLException {
        return truyVan( "SELECT ct.MaSanPham product_id,sp.TenSanPham product_name,ct.SoLuong quantity,ct.DonGia unit_price," +
            "ct.TienGiam discount_amount,ct.ThanhTien line_total FROM dbo.ChiTietHoaDon ct " + "JOIN dbo.SanPham sp ON sp.MaSanPham=ct.MaSanPham WHERE ct.MaHoaDon=? ORDER BY sp.TenSanPham", maHoaDon);
    }
    public List<Map<String, Object>> danhSachHoaDonGanDay(int soLuong) throws SQLException {
        int gioiHan = Math.max(1, Math.min(100, soLuong));
        return truyVan("SELECT TOP " + gioiHan + " h.MaHoaDon invoice_id,c.HoTen customer,h.NgayLap created_at," + "h.TongTien-ISNULL((SELECT SUM(dt.GiaTriHoan) FROM dbo.PhieuDoiTraHoanTien dt " +
            "WHERE dt.MaHoaDon=h.MaHoaDon AND dt.TrangThai=N'Đã duyệt'),0) net_total,h.TrangThai status,ISNULL((SELECT SUM(ct.SoLuong) FROM dbo.ChiTietHoaDon " + "ct WHERE ct.MaHoaDon=h.MaHoaDon),0) item_count FROM dbo.HoaDon h JOIN " + "dbo.KhachHang c ON c.MaKhachHang=h.MaKhachHang ORDER BY h.NgayLap DESC");
    }
    private static LocalDateTime ngayGio(Object v) {
        if (v instanceof Timestamp) return ((Timestamp) v).toLocalDateTime();
        if (v instanceof LocalDateTime) return (LocalDateTime) v;
        return null;
    }
    private static String trangThaiHoaDon(String v) {
        return "Đã hủy".equals(v) ? "Đã hủy" : "Đã thanh toán";
    }
    public KhachHang timKhachHangThucThe(String maKhachHang) throws SQLException {
        try (Connection ketNoi = connectDB.KetNoiCSDL.layKetNoi()) {
            return timKhachHangThucThe(ketNoi, maKhachHang);
        }
    }
    public KhachHang timKhachHangThucThe(Connection c, String maKhachHang) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT kh.MaKhachHang customer_id,kh.HoTen full_name,kh.SoDienThoai phone,kh.Email email,kh.DiaChi address,kh.TrangThai status,ISNULL(tv.DiemTichLuy,0) points FROM dbo.KhachHang kh LEFT JOIN dbo.TheThanhVien tv ON tv.MaKhachHang=kh.MaKhachHang AND tv.TrangThai=N'Hoạt động' WHERE kh.MaKhachHang=?", maKhachHang);
        if (d == null) return null;
        return new KhachHang(String.valueOf(d.get("customer_id")), String.valueOf(d.get("full_name")), d.get("phone") == null ? null : String.valueOf(d.get("phone")), d.get("email") == null ? null : String.valueOf(d.get("email")), d.get("address") == null ? null : String.valueOf(d.get("address")), null, ((Number) d.get("points")).intValue(), String.valueOf(d.get("status")));
    }
    public String hangThanhVien(String maKhachHang) throws SQLException {
        Object v = motGiaTri("SELECT ISNULL(HangThanhVien,N'Tiêu chuẩn') FROM dbo.TheThanhVien WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", maKhachHang);
        return v == null ? "Tiêu chuẩn" : String.valueOf(v);
    }
    public String hangThanhVien(Connection ketNoi, String maKhachHang) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT ISNULL(HangThanhVien,N'Tiêu chuẩn') FROM dbo.TheThanhVien WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", maKhachHang);
        return v == null ? "Tiêu chuẩn" : String.valueOf(v);
    }
    public SanPham timSanPhamThucThe(String maSanPham) throws SQLException {
        try (Connection ketNoi = connectDB.KetNoiCSDL.layKetNoi()) {
            return timSanPhamThucThe(ketNoi, maSanPham);
        }
    }
    public SanPham timSanPhamThucThe(Connection c, String maSanPham) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT MaSanPham product_id,MaVach barcode,TenSanPham product_name,MaDanhMuc category_id,GiaVon cost_price,GiaBan sale_price,SoLuongTonKho stock_quantity,SoThangBaoHanh warranty_months,HinhAnh image_path,TrangThai status FROM dbo.SanPham WHERE MaSanPham=?", maSanPham);
        if (d == null) return null;
        return new SanPham(String.valueOf(d.get("product_id")), d.get("barcode") == null ? null : String.valueOf(d.get("barcode")), String.valueOf(d.get("product_name")), ((Number) d.get("category_id")).intValue(), null, (BigDecimal) d.get("cost_price"), (BigDecimal) d.get("sale_price"), ((Number) d.get("stock_quantity")).intValue(), ((Number) d.get("warranty_months")).intValue(), d.get("image_path") == null ? null : String.valueOf(d.get("image_path")), String.valueOf(d.get("status")));
    }
    public List<KhachHang> danhSachKhachHangHoatDongThucThe() throws SQLException {
        List<KhachHang> ketQua = new ArrayList<>();
        try (Connection ketNoi = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : danhSachKhachHangHoatDong()) {
                KhachHang kh = timKhachHangThucThe(ketNoi, String.valueOf(d.get("customer_id")));
                if (kh != null) ketQua.add(kh);
            }
        }
        return ketQua;
    }
    public List<SanPham> danhSachSanPhamConHangThucThe() throws SQLException {
        List<SanPham> ketQua = new ArrayList<>();
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : danhSachSanPhamConHang()) {
                SanPham sp = timSanPhamThucThe(c, String.valueOf(d.get("product_id")));
                if (sp != null) ketQua.add(sp);
            }
        }
        return ketQua;
    }
    public List<KhuyenMai> timKhuyenMaiDangHoatDongThucThe(Connection c, String maSanPham, int maDanhMuc) throws SQLException {
        List<KhuyenMai> ketQua = new ArrayList<>();
        List<Map<String, Object>> ds = truyVan(c, "SELECT DISTINCT km.MaKhuyenMai promotion_code,km.TenChuongTrinh promotion_name,CASE km.LoaiGiam WHEN N'Phần trăm' THEN 'PERCENT' ELSE 'FIXED' END discount_type,km.GiaTriGiam discount_value,km.NgayBatDau start_date,km.NgayKetThuc end_date,km.TrangThai status FROM dbo.KhuyenMai km JOIN dbo.ChiTietKhuyenMai ct ON ct.MaKhuyenMai=km.MaKhuyenMai WHERE ct.MaSanPham=? AND km.TrangThai=N'Hoạt động' AND CAST(GETDATE() AS DATE) BETWEEN km.NgayBatDau AND km.NgayKetThuc", maSanPham);
        for (Map<String, Object> d : ds) ketQua.add(new KhuyenMai(String.valueOf(d.get("promotion_code")), String.valueOf(d.get("promotion_name")), String.valueOf(d.get("discount_type")), (BigDecimal) d.get("discount_value"), ((java.sql.Date) d.get("start_date")).toLocalDate().atStartOfDay(), ((java.sql.Date) d.get("end_date")).toLocalDate().atTime(23, 59, 59), String.valueOf(d.get("status")), null));
        return ketQua;
    }
    public List<KhuyenMai> timKhuyenMaiDangHoatDongThucThe(String maSanPham, int maDanhMuc) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            return timKhuyenMaiDangHoatDongThucThe(c, maSanPham, maDanhMuc);
        }
    }
    private HoaDon anhXaHoaDon(Map<String, Object> d) {
        if (d == null) return null;
        KhachHang kh = new KhachHang(String.valueOf(d.get("customer_id")), String.valueOf(d.get("customer")), d.get("customer_phone") == null ? null : String.valueOf(d.get("customer_phone")), d.get("customer_email") == null ? null : String.valueOf(d.get("customer_email")), d.get("customer_address") == null ? null : String.valueOf(d.get("customer_address")), null, 0, String.valueOf(d.get("customer_status")));
        NhanVien nv = new NhanVien(String.valueOf(d.get("employee_id")), String.valueOf(d.get("employee")), String.valueOf(d.get("employee_phone")), String.valueOf(d.get("employee_email")), ((java.sql.Date) d.get("employee_start_date")).toLocalDate(), d.get("employee_education") == null ? null : String.valueOf(d.get("employee_education")), d.get("employee_salary_factor") == null ? null : ((Number) d.get("employee_salary_factor")).doubleValue(), (BigDecimal) d.get("employee_base_salary"), d.get("employee_type") == null ? null : String.valueOf(d.get("employee_type")), String.valueOf(d.get("employee_status")));
        HoaDon hd = new HoaDon(String.valueOf(d.get("invoice_id")), kh, nv, ngayGio(d.get("created_at")), (BigDecimal) d.get("subtotal"), (BigDecimal) d.get("discount_amount"), (BigDecimal) d.get("total_amount"), trangThaiHoaDon(String.valueOf(d.get("status"))), d.get("note") == null ? null : String.valueOf(d.get("note")));
        hd.setPhuongThucThanhToan(d.get("payment_method") == null ? null : String.valueOf(d.get("payment_method")));
        hd.setMaThamChieuThanhToan(d.get("payment_reference") == null ? null : String.valueOf(d.get("payment_reference")));
        if (d.get("net_total") != null) hd.setTongTienThuan((BigDecimal) d.get("net_total"));
        return hd;
    }
    public HoaDon timHoaDonThucThe(String maHoaDon) throws SQLException {
        Map<String, Object> d = motDong("SELECT h.MaHoaDon invoice_id,h.NgayLap created_at,h.TamTinh subtotal,h.TienGiam discount_amount,h.TongTien total_amount,h.TongTien-ISNULL((SELECT SUM(dt.GiaTriHoan) FROM dbo.PhieuDoiTraHoanTien dt WHERE dt.MaHoaDon=h.MaHoaDon AND dt.TrangThai=N'Đã duyệt'),0) net_total,h.TrangThai status,h.PhuongThucThanhToan payment_method,h.MaThamChieuThanhToan payment_reference,h.GhiChu note,c.MaKhachHang customer_id,c.HoTen customer,c.SoDienThoai customer_phone,c.Email customer_email,c.DiaChi customer_address,c.TrangThai customer_status,nv.MaNhanVien employee_id,nv.HoTen employee,nv.SoDienThoai employee_phone,nv.Email employee_email,nv.NgayVaoLam employee_start_date,nv.TrinhDo employee_education,nv.HeSoLuong employee_salary_factor,nv.LuongCoBan employee_base_salary,nv.LoaiNhanVien employee_type,nv.TrangThai employee_status FROM dbo.HoaDon h JOIN dbo.KhachHang c ON c.MaKhachHang=h.MaKhachHang JOIN dbo.NhanVien nv ON nv.MaNhanVien=h.MaNhanVienBanHang WHERE h.MaHoaDon=?", maHoaDon);
        return anhXaHoaDon(d);
    }
    public List<HoaDon> danhSachHoaDonQuanLyThucThe() throws SQLException {
        List<HoaDon> ketQua = new ArrayList<>();
        for (Map<String, Object> d : danhSachHoaDonQuanLy()) {
            HoaDon hd = timHoaDonThucThe(String.valueOf(d.get("invoice_id")));
            if (hd != null) ketQua.add(hd);
        }
        return ketQua;
    }
    public List<ChiTietHoaDon> dongChiTietHoaDonQuanLyThucThe(String maHoaDon) throws SQLException {
        HoaDon hd = timHoaDonThucThe(maHoaDon);
        List<ChiTietHoaDon> ketQua = new ArrayList<>();
        if (hd == null) return ketQua;
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : dongChiTietHoaDonQuanLy(maHoaDon)) {
                SanPham sp = timSanPhamThucThe(c, String.valueOf(d.get("product_id")));
                ChiTietHoaDon ct = new ChiTietHoaDon(hd, sp, ((Number) d.get("quantity")).intValue(), (BigDecimal) d.get("unit_price"), sp.getGiaVon(), (BigDecimal) d.get("discount_amount"), (BigDecimal) d.get("line_total"));
                ketQua.add(ct);
                hd.themChiTiet(ct);
            }
        }
        return ketQua;
    }
}

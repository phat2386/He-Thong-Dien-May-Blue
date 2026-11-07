package dao;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.Date;
import entity.*;
public class KhuyenMaiDAO extends CoSoDAO {
    private static final String TEN_DANH_MUC_SQL = "CASE sp.MaDanhMuc WHEN 1 THEN N'Điện thoại' WHEN 2 THEN N'Laptop' WHEN 3 THEN N'TV' WHEN 4 THEN N'Điện lạnh' WHEN 5 THEN N'Điện gia dụng' WHEN 6 THEN N'Máy tính bảng' WHEN 7 THEN N'Âm thanh' ELSE N'Khác' END";
    private final SanPhamDAO sanPhamDAO = new SanPhamDAO();
    public String goiYMaKhuyenMai() throws SQLException {
        return goiYMaTiepTheo("KhuyenMai", "MaKhuyenMai", "KM", 3);
    }
    public void capNhatTrangThaiKhuyenMai(String maKhuyenMai, String trangThai) throws SQLException {
        capNhat("UPDATE dbo.KhuyenMai SET TrangThai=? WHERE MaKhuyenMai=?", trangThai, maKhuyenMai);
    }
    public List<Map<String, Object>> danhSachKhuyenMai() throws SQLException {
        return truyVan("SELECT MaKhuyenMai promotion_id,MaKhuyenMai " + "promotion_code,TenChuongTrinh promotion_name,CASE LoaiGiam WHEN N'Phần trăm' " +
            "THEN 'PERCENT' ELSE 'FIXED' END discount_type,GiaTriGiam discount_value," + "CAST(NgayBatDau AS datetime2) start_at,DATEADD(SECOND,-1,DATEADD(DAY,1," +
            "CAST(NgayKetThuc AS datetime2))) end_at,TrangThai status FROM dbo.KhuyenMai " + "ORDER BY NgayBatDau DESC");
    }
    public boolean maKhuyenMaiTonTai(String code) throws SQLException {
        return((Number) motGiaTri("SELECT COUNT(*) FROM dbo.KhuyenMai WHERE MaKhuyenMai=?", code)).intValue() > 0;
    }
    public List<Map<String, Object>> danhSachSanPhamCoTheKhuyenMai() throws SQLException {
        return truyVan("SELECT sp.MaSanPham product_id,sp.TenSanPham product_name,sp.MaDanhMuc category_id," + TEN_DANH_MUC_SQL + " category_name,sp.GiaBan sale_price,sp.TrangThai status FROM dbo.SanPham sp ORDER BY sp.MaSanPham");
    }
    public List<Map<String, Object>> danhSachSanPhamAnhHuongKhuyenMai() throws SQLException {
        return truyVan("SELECT MaSanPham product_id,MaDanhMuc category_id FROM dbo.SanPham");
    }
    public Map<String, Object> timKhuyenMaiTrungThoiGian(LocalDateTime start, LocalDateTime end, String product, Integer category) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            return timKhuyenMaiTrungThoiGian(c, start, end, product, category);
        }
    }
    public Map<String, Object> timKhuyenMaiTrungThoiGian(Connection c, LocalDateTime start, LocalDateTime end, String product, Integer category) throws SQLException {
        String sql = "SELECT TOP 1 km.MaKhuyenMai promotion_code,km.TenChuongTrinh promotion_name " + "FROM dbo.KhuyenMai km JOIN dbo.ChiTietKhuyenMai ct ON " +
            "ct.MaKhuyenMai=km.MaKhuyenMai JOIN dbo.SanPham sp ON sp.MaSanPham=ct.MaSanPham " + "WHERE km.NgayBatDau<=CAST(? AS date) AND km.NgayKetThuc>=CAST(? AS date) AND " +
            "km.TrangThai IN(N'Sắp diễn ra',N'Hoạt động') AND ((? IS NOT NULL AND ct.MaSanPham=?) OR (? IS NOT NULL AND sp.MaDanhMuc=?))";
        return motDong(c, sql, end, start, product, product, category, category);
    }
    public KhuyenMai timKhuyenMaiTrungThoiGianThucThe(LocalDateTime start, LocalDateTime end, String product, Integer category) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            return timKhuyenMaiTrungThoiGianThucThe(c, start, end, product, category);
        }
    }
    public KhuyenMai timKhuyenMaiTrungThoiGianThucThe(Connection c, LocalDateTime start, LocalDateTime end, String product, Integer category) throws SQLException {
        Map<String, Object> d = timKhuyenMaiTrungThoiGian(c, start, end, product, category);
        return d == null ? null : timKhuyenMaiThucThe(c, String.valueOf(d.get("promotion_code")));
    }
    public int xoaKhuyenMaiTrungKhoiSanPham(Connection c, LocalDateTime start, LocalDateTime end, String product) throws SQLException {
        return capNhat(c, "DELETE ct FROM dbo.ChiTietKhuyenMai ct JOIN dbo.KhuyenMai km ON km.MaKhuyenMai=ct.MaKhuyenMai " +
            "WHERE ct.MaSanPham=? AND km.NgayBatDau<=CAST(? AS date) AND km.NgayKetThuc>=CAST(? AS date) " +
            "AND km.TrangThai IN(N'Sắp diễn ra',N'Hoạt động')", product, end, start);
    }
    public String themKhuyenMai(Connection c, String code, String name, String type, BigDecimal value, LocalDateTime start, LocalDateTime end, String createdBy) throws SQLException {
        capNhat(c, "INSERT INTO dbo.KhuyenMai(MaKhuyenMai,TenChuongTrinh,NgayBatDau,NgayKetThuc," + "LoaiGiam,GiaTriGiam,TrangThai,MaNhanVienTao) VALUES(?,?,?,?,?,?,CASE WHEN " +
            "CAST(GETDATE() AS date)<? THEN N'Sắp diễn ra' WHEN CAST(GETDATE() AS date)>? THEN " + "N'Đã kết thúc' ELSE N'Hoạt động' END,?)", code, name, start.toLocalDate(), end.toLocalDate(),
            "PERCENT".equals(type) ? "Phần trăm" : "Số tiền", value, start.toLocalDate(), end.toLocalDate(), createdBy);
        return code;
    }
    public void themSanPhamKhuyenMai(Connection c, String promo, String product) throws SQLException {
        capNhat(c, "IF NOT EXISTS(SELECT 1 FROM dbo.ChiTietKhuyenMai WHERE MaKhuyenMai=? AND " + "MaSanPham=?) INSERT INTO dbo.ChiTietKhuyenMai(MaSanPham,MaKhuyenMai) VALUES(?," + "?)", promo, product, product, promo);
    }
    public void themDanhMucKhuyenMai(Connection c, String promo, int category) throws SQLException {
        capNhat(c, "INSERT INTO dbo.ChiTietKhuyenMai(MaSanPham,MaKhuyenMai) SELECT sp.MaSanPham,? " + "FROM dbo.SanPham sp WHERE sp.MaDanhMuc=? AND NOT EXISTS(SELECT 1 FROM " +
            "dbo.ChiTietKhuyenMai ct WHERE ct.MaSanPham=sp.MaSanPham AND ct.MaKhuyenMai=?)", promo, category, promo);
    }
    public List<Map<String, Object>> phamViKhuyenMai(String code) throws SQLException {
        return truyVan("SELECT 'PRODUCT' scope_type,ct.MaSanPham scope_id,sp.TenSanPham scope_name " + "FROM dbo.ChiTietKhuyenMai ct JOIN dbo.SanPham sp ON sp.MaSanPham=ct.MaSanPham " + "WHERE ct.MaKhuyenMai=?", code);
    }
    public int ketThucSomKhuyenMai(String code) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            return ketThucSomKhuyenMai(c, code);
        }
    }
    public int ketThucSomKhuyenMai(Connection c, String code) throws SQLException {
        return capNhat(c, "UPDATE dbo.KhuyenMai SET TrangThai=N'Đã kết thúc',NgayKetThuc=CASE WHEN " + "NgayBatDau>CAST(GETDATE() AS date) THEN NgayBatDau ELSE CAST(GETDATE() AS " + "date) END WHERE MaKhuyenMai=?", code);
    }
    public KhuyenMai timKhuyenMaiThucThe(Connection c, String code) throws SQLException {
        Map<String, Object> d = timKhuyenMaiTheoMa(c, code);
        if (d == null) return null;
        Date s = (Date) d.get("start_date"), e = (Date) d.get("end_date");
        return new KhuyenMai(code, String.valueOf(d.get("promotion_name")), String.valueOf(d.get( "discount_type")), (BigDecimal) d.get("discount_value"), s.toLocalDate().atStartOfDay(),
            e.toLocalDate().atTime(23, 59, 59), String.valueOf(d.get("status")), null);
    }
    public Map<String, Object> timKhuyenMaiTheoMa(Connection c, String code) throws SQLException {
        return motDong(c, "SELECT TenChuongTrinh promotion_name,CASE LoaiGiam WHEN N'Phần trăm' THEN " + "'PERCENT' ELSE 'FIXED' END discount_type,GiaTriGiam discount_value,NgayBatDau " +
            "start_date,NgayKetThuc end_date,TrangThai status FROM dbo.KhuyenMai WHERE " + "MaKhuyenMai=?", code);
    }
    public void xoaPhamViKhuyenMai(Connection c, String code) throws SQLException {
        capNhat(c, "DELETE FROM dbo.ChiTietKhuyenMai WHERE MaKhuyenMai=?", code);
    }
    public int xoaKhuyenMaiDaKetThuc(Connection c, String code) throws SQLException {
        return capNhat(c, "DELETE FROM dbo.KhuyenMai WHERE MaKhuyenMai=?", code);
    }
    public List<KhuyenMai> danhSachKhuyenMaiThucThe() throws SQLException {
        List<KhuyenMai> ketQua = new ArrayList<>();
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : danhSachKhuyenMai()) {
                KhuyenMai km = timKhuyenMaiThucThe(c, String.valueOf(d.get("promotion_code")));
                if (km != null) ketQua.add(km);
            }
        }
        return ketQua;
    }
    public List<SanPham> danhSachSanPhamCoTheKhuyenMaiThucThe() throws SQLException {
        List<SanPham> ketQua = new ArrayList<>();
        for (Map<String, Object> d : danhSachSanPhamCoTheKhuyenMai()) {
            SanPham sp = sanPhamDAO.timSanPhamThucThe(String.valueOf(d.get("product_id")));
            if (sp != null) ketQua.add(sp);
        }
        return ketQua;
    }
    public List<SanPham> danhSachSanPhamAnhHuongKhuyenMaiThucThe() throws SQLException {
        List<SanPham> ketQua = new ArrayList<>();
        for (Map<String, Object> d : danhSachSanPhamAnhHuongKhuyenMai()) {
            SanPham sp = sanPhamDAO.timSanPhamThucThe(String.valueOf(d.get("product_id")));
            if (sp != null) ketQua.add(sp);
        }
        return ketQua;
    }
    public List<SanPham> phamViKhuyenMaiThucThe(String code) throws SQLException {
        List<SanPham> ketQua = new ArrayList<>();
        for (Map<String, Object> d : phamViKhuyenMai(code)) {
            SanPham sp = sanPhamDAO.timSanPhamThucThe(String.valueOf(d.get("scope_id")));
            if (sp != null) ketQua.add(sp);
        }
        return ketQua;
    }
}

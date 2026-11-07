package dao;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import entity.*;
public class SanPhamDAO extends CoSoDAO {
    public String goiYMaSanPham() throws SQLException {
        return goiYMaTiepTheo("SanPham", "MaSanPham", "SP", 3);
    }
    private static final String TEN_DANH_MUC_SQL = "CASE sp.MaDanhMuc WHEN 1 THEN N'Điện thoại' WHEN 2 THEN N'Laptop' WHEN 3 THEN " +
        "N'TV' WHEN 4 THEN N'Điện lạnh' WHEN 5 THEN N'Điện gia dụng' WHEN 6 THEN N'Máy " + "tính bảng' WHEN 7 THEN N'Âm thanh' ELSE N'Khác' END";
    public void capNhatTrangThaiSanPham(Connection c, String id, String status) throws SQLException {
        capNhat(c, "UPDATE dbo.SanPham SET TrangThai=? WHERE MaSanPham=?", status, id);
    }
    public void congTonKho(Connection c, String product, String supplier, int qty) throws SQLException {
        capNhat(c, "UPDATE dbo.SanPham SET SoLuongTonKho=SoLuongTonKho+?,MaNhaCungCap=?," + "TrangThai=CASE WHEN TrangThai=N'Hết hàng' THEN N'Hoạt động' ELSE TrangThai END " + "WHERE MaSanPham=?", qty, supplier, product);
    }
    public List<Map<String, Object>> timDanhSachSanPham(String key, Integer category) throws SQLException {
        String pattern = "%" + (key == null ? "" : key.trim()) + "%";
        String sql = "SELECT sp.MaSanPham product_id,sp.MaVach barcode,sp.TenSanPham product_name,sp.MaDanhMuc category_id," + TEN_DANH_MUC_SQL + " category_name," +
            "sp.MaNhaCungCap supplier_id,ncc.MaNhaCungCap supplier_code,ncc.TenNhaCungCap supplier_name,ncc.SoDienThoai supplier_phone,ncc.DiaChi supplier_address," +
            "ncc.MaSoThue tax_code,ncc.TrangThai supplier_status,ncc.CongNo debt,sp.GiaVon cost_price,sp.GiaBan sale_price,sp.SoLuongTonKho stock_quantity," +
            "sp.SoThangBaoHanh warranty_months,sp.HinhAnh image_path,sp.TrangThai status FROM dbo.SanPham sp LEFT JOIN dbo.NhaCungCap ncc ON " +
            "ncc.MaNhaCungCap=sp.MaNhaCungCap WHERE (sp.MaSanPham LIKE ? OR sp.TenSanPham LIKE ? OR sp.MaVach LIKE ?) ";
        if (category != null) return truyVan(sql + "AND sp.MaDanhMuc=? ORDER BY sp.MaSanPham", pattern, pattern, pattern, category);
        return truyVan(sql + "ORDER BY sp.MaSanPham", pattern, pattern, pattern);
    }
    public SanPham timSanPhamThucThe(String id) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            return timSanPhamThucThe(c, id);
        }
    }
    public SanPham timSanPhamThucThe(Connection c, String id) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT sp.MaSanPham product_id,sp.MaVach barcode,sp.TenSanPham product_name,sp.MaDanhMuc category_id," + TEN_DANH_MUC_SQL + " category_name," +
            "sp.MaNhaCungCap supplier_id,ncc.MaNhaCungCap supplier_code,ncc.TenNhaCungCap supplier_name,ncc.SoDienThoai supplier_phone,ncc.DiaChi supplier_address," +
            "ncc.MaSoThue tax_code,ncc.TrangThai supplier_status,ncc.CongNo debt,sp.GiaVon cost_price,sp.GiaBan sale_price,sp.SoLuongTonKho stock_quantity," +
            "sp.SoThangBaoHanh warranty_months,sp.HinhAnh image_path,sp.TrangThai status FROM dbo.SanPham sp LEFT JOIN dbo.NhaCungCap ncc ON " +
            "ncc.MaNhaCungCap=sp.MaNhaCungCap WHERE sp.MaSanPham=?", id);
        return anhXaSanPham(d);
    }
    private SanPham anhXaSanPham(Map<String, Object> d) {
        if (d == null) return null;
        NhaCungCap nhaCungCap = null;
        if (d.get("supplier_id") != null) {
            nhaCungCap = new NhaCungCap(String.valueOf(d.get("supplier_code")), String.valueOf(d.get("supplier_name")), String.valueOf(d.get("supplier_phone")),
                d.get("supplier_address") == null ? null : String.valueOf(d.get("supplier_address")), d.get("tax_code") == null ? null : String.valueOf(d.get("tax_code")),
                String.valueOf(d.get("supplier_status")), (BigDecimal) d.get("debt"));
        }
        return new SanPham(String.valueOf(d.get("product_id")), d.get("barcode") == null ? null : String.valueOf(d.get("barcode")), String.valueOf(d.get("product_name")),
            ((Number) d.get("category_id")).intValue(), nhaCungCap, (BigDecimal) d.get("cost_price"), (BigDecimal) d.get("sale_price"),
            ((Number) d.get("stock_quantity")).intValue(), ((Number) d.get("warranty_months")).intValue(),
            d.get("image_path") == null ? null : String.valueOf(d.get("image_path")), String.valueOf(d.get("status")));
    }
    public boolean sanPhamTonTai(Connection c, String id) throws SQLException {
        return((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.SanPham WHERE MaSanPham=?", id)).intValue() > 0;
    }
    public boolean maVachTonTai(Connection c, String barcode) throws SQLException {
        return((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.SanPham WHERE MaVach=?", barcode)).intValue() > 0;
    }
    public void themSanPham(Connection c, String id, String barcode, String name, int category, String supplier, BigDecimal cost, BigDecimal sale, int warranty, Object image) throws SQLException {
        capNhat(c, "INSERT INTO dbo.SanPham(MaSanPham,TenSanPham,MaVach,MaDanhMuc,GiaBan,GiaVon," + "SoLuongTonKho,SoThangBaoHanh,HinhAnh,TrangThai,MaNhaCungCap) VALUES(?,?,?,?,?," +
            "?,0,?,?,N'Hết hàng',?)", id, name, barcode, category, sale, cost, warranty, image, supplier);
    }
    public void capNhatSanPham(Connection c, String id, String name, int category, String supplier, BigDecimal cost, BigDecimal sale, int warranty, Object image) throws SQLException {
        capNhat(c, "UPDATE dbo.SanPham SET TenSanPham=?,MaDanhMuc=?,MaNhaCungCap=?,GiaVon=?," + "GiaBan=?,SoThangBaoHanh=?,HinhAnh=? WHERE MaSanPham=?", name, category, supplier, cost, sale, warranty, image, id);
    }
    public Map<String, Object> timThongTinNhapKho(String id) throws SQLException {
        return motDong("SELECT sp.MaSanPham product_id,sp.TenSanPham product_name,sp.GiaVon cost_price," + "sp.SoLuongTonKho stock_quantity,sp.MaNhaCungCap supplier_id,ncc.TenNhaCungCap supplier_name,sp.TrangThai status FROM " + "dbo.SanPham sp LEFT JOIN dbo.NhaCungCap ncc ON " + "ncc.MaNhaCungCap=sp.MaNhaCungCap WHERE sp.MaSanPham=?", id);
    }
    public List<Map<String, Object>> timSanPhamDangBan(String key, Integer cat) throws SQLException {
        String pattern = "%" + (key == null ? "" : key.trim()) + "%";
        String sql = "SELECT sp.MaSanPham product_id,sp.MaVach barcode,sp.TenSanPham product_name,sp.MaDanhMuc category_id," + TEN_DANH_MUC_SQL + " category_name," +
            "sp.MaNhaCungCap supplier_id,ncc.MaNhaCungCap supplier_code,ncc.TenNhaCungCap supplier_name,ncc.SoDienThoai supplier_phone,ncc.DiaChi supplier_address," +
            "ncc.MaSoThue tax_code,ncc.TrangThai supplier_status,ncc.CongNo debt,sp.GiaVon cost_price,sp.GiaBan sale_price,sp.SoLuongTonKho stock_quantity," +
            "sp.SoThangBaoHanh warranty_months,sp.HinhAnh image_path,sp.TrangThai status FROM dbo.SanPham sp LEFT JOIN dbo.NhaCungCap ncc ON " +
            "ncc.MaNhaCungCap=sp.MaNhaCungCap WHERE sp.TrangThai=N'Hoạt động' AND sp.SoLuongTonKho>0 AND (sp.MaSanPham LIKE ? OR sp.TenSanPham LIKE ? OR sp.MaVach LIKE ?) ";
        if (cat != null) return truyVan(sql + "AND sp.MaDanhMuc=? ORDER BY sp.MaSanPham", pattern, pattern, pattern, cat);
        return truyVan(sql + "ORDER BY sp.MaSanPham", pattern, pattern, pattern);
    }
    public List<SanPham> timDanhSachSanPhamThucThe(String key, Integer category) throws SQLException {
        List<SanPham> ketQua = new ArrayList<>();
        for (Map<String, Object> d : timDanhSachSanPham(key, category)) {
            SanPham sp = anhXaSanPham(d);
            if (sp != null) ketQua.add(sp);
        }
        return ketQua;
    }
    public List<SanPham> timSanPhamDangBanThucThe(String key, Integer category) throws SQLException {
        List<SanPham> ketQua = new ArrayList<>();
        for (Map<String, Object> d : timSanPhamDangBan(key, category)) {
            SanPham sp = anhXaSanPham(d);
            if (sp != null) ketQua.add(sp);
        }
        return ketQua;
    }
}

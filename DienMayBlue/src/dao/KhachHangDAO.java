package dao;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import entity.*;
import java.sql.Date;
public class KhachHangDAO extends CoSoDAO {
    private static final String SQL_KHACH_HANG_CO_THE = "SELECT kh.MaKhachHang customer_id,kh.HoTen full_name,kh.SoDienThoai phone,kh.Email email,kh.DiaChi address," +
        "CASE tv.HangThanhVien WHEN N'Tiêu chuẩn' THEN 1 WHEN N'Bạc' THEN 2 WHEN " + "N'Vàng' THEN 3 WHEN N'Bạch kim' THEN 4 ELSE 1 END tier_id," +
        "ISNULL(tv.DiemTichLuy,0) current_points,kh.TrangThai status " + "FROM dbo.KhachHang kh LEFT JOIN dbo.TheThanhVien tv ON " + "tv.MaKhachHang=kh.MaKhachHang AND tv.TrangThai=N'Hoạt động' ";
    public String goiYMaKhachHang() throws SQLException {
        return goiYMaTiepTheo("KhachHang", "MaKhachHang", "KH", 3);
    }
    public List<Map<String, Object>> timKiemKhachHang(String tuKhoa, String tierCode) throws SQLException {
        String sql = "SELECT kh.MaKhachHang customer_id,kh.HoTen full_name,kh.SoDienThoai phone,kh.Email email,kh.DiaChi address," +
            "CASE tv.HangThanhVien WHEN N'Tiêu chuẩn' THEN 'STANDARD' WHEN N'Bạc' THEN " + "'SILVER' WHEN N'Vàng' THEN 'GOLD' WHEN N'Bạch kim' THEN 'PLATINUM' ELSE " + "'STANDARD' END tier_code," +
            "ISNULL(tv.DiemTichLuy,0) current_points,kh.TrangThai status," + "ISNULL((SELECT SUM(ct.SoLuong*ct.DonGia) FROM dbo.HoaDon hd JOIN " +
            "dbo.ChiTietHoaDon ct ON ct.MaHoaDon=hd.MaHoaDon WHERE " + "hd.MaKhachHang=kh.MaKhachHang AND hd.TrangThai=N'Đã thanh toán'),0) spent," +
            "tv.MaThe card FROM dbo.KhachHang kh LEFT JOIN dbo.TheThanhVien tv ON tv.MaKhachHang=kh.MaKhachHang " + "WHERE kh.MaKhachHang<>'KH000' AND (kh.MaKhachHang LIKE ? OR kh.HoTen LIKE ? OR kh.SoDienThoai LIKE ?) ";
        if (tierCode != null && !tierCode.isBlank()) {
            sql += "AND CASE tv.HangThanhVien WHEN N'Tiêu chuẩn' THEN 'STANDARD' WHEN N'Bạc' THEN " + "'SILVER' WHEN N'Vàng' THEN 'GOLD' WHEN N'Bạch kim' THEN 'PLATINUM' ELSE " + "'STANDARD' END=? ";
            return truyVan(sql + "ORDER BY kh.MaKhachHang", tuKhoa, tuKhoa, tuKhoa, tierCode);
        }
        return truyVan(sql + "ORDER BY kh.MaKhachHang", tuKhoa, tuKhoa, tuKhoa);
    }
    public List<KhachHang> timKiemKhachHangThucThe(String tuKhoa, String tierCode) throws SQLException {
        List<KhachHang> ketQua = new ArrayList<KhachHang>();
        for (Map<String, Object> dong : timKiemKhachHang(tuKhoa, tierCode)) {
            int maHang = "SILVER".equals(dong.get("tier_code")) ? 2 : "GOLD".equals(dong.get("tier_code")) ? 3 : "PLATINUM".equals(dong.get("tier_code")) ? 4 : 1;
            KhachHang khachHang = new KhachHang(String.valueOf(dong.get("customer_id")), String.valueOf(dong.get("full_name")),
                dong.get("phone") == null ? null : String.valueOf(dong.get("phone")), dong.get("email") == null ? null : String.valueOf(dong.get("email")),
                dong.get("address") == null ? null : String.valueOf(dong.get("address")), maHang,
                dong.get("current_points") == null ? 0 : ((Number) dong.get("current_points")).intValue(), String.valueOf(dong.get("status")));
            ketQua.add(khachHang);
        }
        return ketQua;
    }
    public Map<String, Object> timKhachHang(String maKhachHang) throws SQLException {
        return motDong(SQL_KHACH_HANG_CO_THE + "WHERE kh.MaKhachHang=?", maKhachHang);
    }
    public KhachHang timKhachHangThucThe(String maKhachHang) throws SQLException {
        return anhXaKhachHang(timKhachHang(maKhachHang));
    }
    public KhachHang timKhachHangThucThe(Connection ketNoi, String maKhachHang) throws SQLException {
        return anhXaKhachHang(motDong(ketNoi, SQL_KHACH_HANG_CO_THE + "WHERE kh.MaKhachHang=?", maKhachHang));
    }
    private KhachHang anhXaKhachHang(Map<String, Object> d) {
        if (d == null) return null;
        return new KhachHang(String.valueOf(d.get("customer_id")), String.valueOf(d.get("full_name")), d.get("phone") == null ? null : String.valueOf(d.get("phone")), d.get("email") == null ? null : String.valueOf(
            d.get("email")), d.get("address") == null ? null : String.valueOf(d.get("address")), d.get("tier_id") == null ? 1 : ((Number) d.get("tier_id")).intValue(), d.get("current_points") ==
            null ? 0 : ((Number) d.get("current_points")).intValue(), String.valueOf(d.get("status")));
    }
    public boolean khachHangTonTai(String maKhachHang) throws SQLException {
        return((Number) motGiaTri("SELECT COUNT(*) FROM dbo.KhachHang WHERE MaKhachHang=?", maKhachHang)).intValue() > 0;
    }
    public void themKhachHang(String maKhachHang, String hoTen, Object soDienThoai, Object email, Object diaChi) throws SQLException {
        capNhat("INSERT INTO dbo.KhachHang(MaKhachHang,HoTen,SoDienThoai,Email,DiaChi,TrangThai) VALUES(?,?,?,?,?,N'Hoạt động')", maKhachHang, hoTen, soDienThoai, email, diaChi);
    }
    public void capNhatKhachHang(String maKhachHang, String hoTen, Object soDienThoai, Object email, Object diaChi) throws SQLException {
        capNhat("UPDATE dbo.KhachHang SET HoTen=?,SoDienThoai=?,Email=?,DiaChi=? WHERE MaKhachHang=?", hoTen, soDienThoai, email, diaChi, maKhachHang);
    }
    public boolean coDuLieuLienQuan(Connection ketNoi, String maKhachHang) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT (SELECT COUNT(*) FROM dbo.HoaDon WHERE MaKhachHang=?)+" +
            "(SELECT COUNT(*) FROM dbo.PhieuGiaoHang WHERE MaKhachHang=?)+" +
            "(SELECT COUNT(*) FROM dbo.PhieuBaoHanh WHERE MaKhachHang=?)+" +
            "(SELECT COUNT(*) FROM dbo.KhieuNai WHERE MaKhachHang=?)+" +
            "(SELECT COUNT(*) FROM dbo.PhieuDoiTraHoanTien WHERE MaKhachHang=?)", maKhachHang,maKhachHang,maKhachHang,maKhachHang,maKhachHang);
        return v != null && ((Number)v).intValue()>0;
    }
    public void xoaKhachHang(Connection ketNoi, String maKhachHang) throws SQLException {
        capNhat(ketNoi, "DELETE FROM dbo.TheThanhVien WHERE MaKhachHang=?", maKhachHang);
        capNhat(ketNoi, "DELETE FROM dbo.KhachHang WHERE MaKhachHang=?", maKhachHang);
    }
    public List<Map<String, Object>> timMaKhachHangTheoSoDienThoai(String soDienThoai) throws SQLException {
        return truyVan("SELECT MaKhachHang customer_id FROM dbo.KhachHang WHERE SoDienThoai=?", soDienThoai);
    }
}

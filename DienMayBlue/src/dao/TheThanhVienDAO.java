package dao;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import entity.*;
import java.sql.Date;
public class TheThanhVienDAO extends CoSoDAO {
    public String goiYMaThe(String maKhachHang) throws SQLException {
        String tienTo = "BLUE-" + maKhachHang + "-";
        Object v = motGiaTri("SELECT ISNULL(MAX(TRY_CONVERT(int,RIGHT(MaThe,2))),0) FROM dbo.TheThanhVien " + "WHERE MaKhachHang=? AND MaThe LIKE ?", maKhachHang, tienTo + "%");
        int lonNhat = v == null ? 0 : ((Number) v).intValue();
        return tienTo + String.format("%02d", lonNhat + 1);
    }
    public List<Map<String, Object>> timKhachHangThanhVien(String tuKhoa) throws SQLException {
        return truyVan( "SELECT tv.MaThe card_code,kh.MaKhachHang customer_id,kh.HoTen full_name,kh.SoDienThoai phone," +
            "tv.HangThanhVien tier_name,tv.DiemTichLuy current_points,tv.NgayCap issued_at," + "tv.NgayVoHieu invalidated_at,tv.TrangThai card_status " +
            "FROM dbo.TheThanhVien tv JOIN dbo.KhachHang kh ON kh.MaKhachHang=tv.MaKhachHang " + "WHERE tv.MaThe LIKE ? OR kh.MaKhachHang LIKE ? OR kh.HoTen LIKE ? OR kh.SoDienThoai LIKE ? " +
            "ORDER BY CASE WHEN tv.TrangThai=N'Hoạt động' THEN 0 ELSE 1 END,kh.HoTen,tv.NgayCap DESC", tuKhoa, tuKhoa, tuKhoa, tuKhoa);
    }
    public List<TheThanhVien> timKhachHangThanhVienThucThe(String tuKhoa) throws SQLException {
        List<TheThanhVien> ketQua = new ArrayList<TheThanhVien>();
        for (Map<String, Object> dong : timKhachHangThanhVien(tuKhoa)) {
            KhachHang khachHang = new KhachHang();
            khachHang.setMaKhachHang(String.valueOf(dong.get("customer_id")));
            khachHang.setHoTen(String.valueOf(dong.get("full_name")));
            khachHang.setSoDienThoai(String.valueOf(dong.get("phone")));
            khachHang.setDiemHienTai(((Number) dong.get("current_points")).intValue());
            khachHang.setTrangThai("Hoạt động");
            TheThanhVien the = new TheThanhVien();
            the.setMaThe(String.valueOf(dong.get("card_code")));
            the.setKhachHang(khachHang);
            the.setMaTheHienThi(String.valueOf(dong.get("card_code")));
            the.setTrangThai(String.valueOf(dong.get("card_status")));
            the.setHangThanhVien(String.valueOf(dong.get("tier_name")));
            the.setDiemTichLuy(((Number) dong.get("current_points")).intValue());
            the.setNgayCap(toLocalDateTime(dong.get("issued_at")));
            the.setNgayVoHieu(toLocalDateTime(dong.get("invalidated_at")));
            ketQua.add(the);
        }
        return ketQua;
    }
    public void voHieuTheThanhVienDangDung(Connection c, String id) throws SQLException {
        capNhat(c, "UPDATE dbo.TheThanhVien SET TrangThai=N'Đã vô hiệu hóa',NgayVoHieu=CAST(GETDATE() " + "AS date) WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", id);
    }
    public boolean maTheThanhVienTonTai(Connection c, String code) throws SQLException {
        return((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.TheThanhVien WHERE MaThe=?", code)).intValue() > 0;
    }
    public TheThanhVien timTheThanhVienDangHoatDong(Connection c, String id) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT tv.MaThe card_code,tv.DiemTichLuy,tv.HangThanhVien,tv.NgayCap," + "kh.MaKhachHang,kh.HoTen,kh.SoDienThoai,kh.Email,kh.DiaChi,kh.TrangThai " +
            "khTrangThai FROM dbo.TheThanhVien tv JOIN dbo.KhachHang kh ON " + "kh.MaKhachHang=tv.MaKhachHang WHERE tv.MaKhachHang=? AND tv.TrangThai=N'Hoạt động'", id);
        if (d == null) return null;
        KhachHang kh = new KhachHang(String.valueOf(d.get("MaKhachHang")), String.valueOf(d.get("HoTen")), String.valueOf(d.get("SoDienThoai")), d.get("Email") == null ? null : String.valueOf(d.get("Email")),
            d.get("DiaChi") == null ? null : String.valueOf(d.get("DiaChi")), hangId(String.valueOf(d.get( "HangThanhVien"))), ((Number) d.get("DiemTichLuy")).intValue(), String.valueOf(d.get("khTrangThai")));
        TheThanhVien tv = new TheThanhVien(String.valueOf(d.get("card_code")), kh, "Hoạt động", ((Date) d.get("NgayCap")).toLocalDate().atStartOfDay(), null, String.valueOf(d.get("HangThanhVien")));
        tv.setDiemTichLuy(((Number) d.get("DiemTichLuy")).intValue());
        return tv;
    }
    public void themTheThanhVien(Connection c, String id, String code, int diem, String hang) throws SQLException {
        capNhat(c, "INSERT INTO dbo.TheThanhVien(MaThe,DiemTichLuy,HangThanhVien,NgayCap,TrangThai," + "MaKhachHang) VALUES(?,?,?,CAST(GETDATE() AS date),N'Hoạt động',?)", code, diem, hang, id);
    }
    private static LocalDateTime toLocalDateTime(Object giaTri) {
        if (giaTri == null) return null;
        if (giaTri instanceof Timestamp) return ((Timestamp) giaTri).toLocalDateTime();
        if (giaTri instanceof Date) return ((Date) giaTri).toLocalDate().atStartOfDay();
        return null;
    }
    private static int hangId(String s) {
        if ("Bạc".equals(s)) return 2;
        if ("Vàng".equals(s)) return 3;
        if ("Bạch kim".equals(s)) return 4;
        return 1;
    }
}

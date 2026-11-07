package dao;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import entity.*;
import java.sql.Date;
public class NhanVienDAO extends CoSoDAO {
    public String goiYMaNhanVien() throws SQLException {
        return goiYMaTiepTheo("NhanVien", "MaNhanVien", "NV", 3);
    }
    public List<Map<String, Object>> timKiem(String tuKhoa, String maTrangThai) throws SQLException {
        String sql = "SELECT nv.MaNhanVien employee_id,nv.HoTen full_name,nv.SoDienThoai phone,nv.Email email," + "nv.LoaiNhanVien department,nv.LoaiNhanVien employee_type,nv.NgayVaoLam join_date,nv.TrangThai status," +
            "nv.TrinhDo education_level,nv.HeSoLuong salary_coefficient,nv.LuongCoBan base_salary," + "CASE WHEN tk.MaTaiKhoan IS NULL THEN NULL WHEN tk.TrangThai=N'Hoạt động' THEN " +
            "N'Hoạt động' ELSE N'Bị khóa' END account_status " + "FROM dbo.NhanVien nv LEFT JOIN dbo.TaiKhoan tk ON tk.MaNhanVien=nv.MaNhanVien " +
            "WHERE ISNULL(tk.TenDangNhap,'')<>'admin' AND (nv.MaNhanVien LIKE ? OR nv.HoTen " + "LIKE ? OR nv.SoDienThoai LIKE ? OR ISNULL(nv.Email,'') LIKE ?) ";
        if (maTrangThai != null && !maTrangThai.isBlank()) return truyVan(sql + "AND nv.TrangThai=? ORDER BY nv.MaNhanVien", tuKhoa, tuKhoa, tuKhoa, tuKhoa, maTrangThai);
        return truyVan(sql + "ORDER BY nv.MaNhanVien", tuKhoa, tuKhoa, tuKhoa, tuKhoa);
    }
    public List<NhanVien> timKiemThucThe(String tuKhoa, String maTrangThai) throws SQLException {
        List<NhanVien> ketQua = new ArrayList<NhanVien>();
        for (Map<String, Object> dong : timKiem(tuKhoa, maTrangThai)) {
            ketQua.add(anhXaNhanVien(dong));
        }
        return ketQua;
    }
    public Map<String, Object> timTheoMa(String maNhanVien) throws SQLException {
        return motDong("SELECT nv.MaNhanVien employee_id,nv.HoTen full_name,nv.SoDienThoai phone,nv.Email email," + "nv.LoaiNhanVien department,nv.NgayVaoLam join_date,nv.TrangThai status," +
            "nv.TrinhDo education_level,nv.HeSoLuong salary_coefficient,nv.LuongCoBan base_salary " + "FROM dbo.NhanVien nv WHERE nv.MaNhanVien=?", maNhanVien);
    }
    public NhanVien timThucTheTheoMa(String maNhanVien) throws SQLException {
        Map<String, Object> d = timTheoMa(maNhanVien);
        if (d == null) return null;
        LocalDate ngay = ((java.sql.Date) d.get("join_date")).toLocalDate();
        NhanVien nv = new NhanVien(String.valueOf(d.get("employee_id")), String.valueOf(d.get("full_name")), String.valueOf(d.get("phone")), d.get("email") == null ? null : String.valueOf(d.get("email")),
            d.get("department") == null ? null : String.valueOf(d.get("department")), ngay, String.valueOf(d.get("status")));
        if (d.get("education_level") != null) nv.setTrinhDo(String.valueOf(d.get("education_level")));
        if (d.get("salary_coefficient") instanceof Number) nv.setHeSoLuong(((Number) d.get("salary_coefficient")).doubleValue());
        if (d.get("base_salary") != null) nv.setLuongCoBan(new java.math.BigDecimal(String.valueOf(d.get("base_salary"))));
        return nv;
    }
    public String timTrangThai(String maNhanVien) throws SQLException {
        Object v = motGiaTri("SELECT TrangThai FROM dbo.NhanVien WHERE MaNhanVien=?", maNhanVien);
        return v == null ? null : String.valueOf(v);
    }
    public boolean maHoacSoDienThoaiTonTai(Connection ketNoi, String maNhanVien, String soDienThoai) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.NhanVien WHERE MaNhanVien=? OR SoDienThoai=?", maNhanVien, soDienThoai);
        return v != null && ((Number) v).intValue() > 0;
    }
    public boolean tonTai(Connection ketNoi, String maNhanVien) throws SQLException {
        return motGiaTri(ketNoi, "SELECT MaNhanVien FROM dbo.NhanVien WHERE MaNhanVien=?", maNhanVien) != null;
    }
    public boolean laQuanTri(Connection ketNoi, String maNhanVien) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.TaiKhoan WHERE MaNhanVien=? AND TenDangNhap='admin'", maNhanVien);
        return v != null && ((Number) v).intValue() > 0;
    }
    public boolean soDienThoaiDaDung(Connection ketNoi, String soDienThoai, String maNhanVien) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.NhanVien WHERE SoDienThoai=? AND MaNhanVien<>?", soDienThoai, maNhanVien);
        return v != null && ((Number) v).intValue() > 0;
    }
    public int demQuanLyDangHoatDong(Connection ketNoi) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.NhanVien nv JOIN dbo.TaiKhoan tk ON tk.MaNhanVien=nv.MaNhanVien " +
            "WHERE nv.LoaiNhanVien=N'Quản lý' AND nv.TrangThai=N'Hoạt động' AND " + "tk.TenDangNhap<>'admin' AND tk.TrangThai=N'Hoạt động'");
        return v == null ? 0 : ((Number) v).intValue();
    }
    public void them(Connection ketNoi, String maNhanVien, String hoTen, String soDienThoai, String email, LocalDate ngayVaoLam, String trinhDo, Double heSoLuong, java.math.BigDecimal luongCoBan, String loaiNhanVien) throws SQLException {
        capNhat(ketNoi, "INSERT INTO dbo.NhanVien(MaNhanVien,HoTen,SoDienThoai,Email,NgayVaoLam,TrinhDo," + "HeSoLuong,LuongCoBan,LoaiNhanVien,TrangThai) " + "VALUES(?,?,?,?,?,?,?,?,?,N'Hoạt động')",
            maNhanVien, hoTen, soDienThoai, email, ngayVaoLam, trinhDo, heSoLuong, luongCoBan, loaiNhanVien);
    }
    public void capNhat(Connection ketNoi, String maNhanVien, String hoTen, String soDienThoai, String email, LocalDate ngayVaoLam, String trinhDo, Double heSoLuong, java.math.BigDecimal luongCoBan, String loaiNhanVien) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.NhanVien SET HoTen=?,SoDienThoai=?,Email=?,NgayVaoLam=?,TrinhDo=?," + "HeSoLuong=?,LuongCoBan=?,LoaiNhanVien=? WHERE MaNhanVien=?", hoTen, soDienThoai,
            email, ngayVaoLam, trinhDo, heSoLuong, luongCoBan, loaiNhanVien, maNhanVien);
    }
    public void capNhatTrangThai(Connection ketNoi, String maNhanVien, String trangThai) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.NhanVien SET TrangThai=? WHERE MaNhanVien=?", trangThai, maNhanVien);
    }
    public void voHieuTaiKhoanCua(Connection ketNoi, String maNhanVien) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TrangThai=N'Bị khóa',ThoiGianMoKhoa=NULL WHERE MaNhanVien=?", maNhanVien);
    }
    public TaiKhoan timTaiKhoanTheoNhanVien(Connection ketNoi, String maNhanVien) throws SQLException {
        Map<String, Object> dong = motDong(ketNoi,
            "SELECT tk.MaTaiKhoan account_id,tk.TenDangNhap username,tk.MatKhau password_hash," +
            "CASE WHEN tk.TrangThai=N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END account_status," +
            "nv.MaNhanVien employee_id,nv.HoTen full_name,nv.SoDienThoai phone,nv.Email email,nv.NgayVaoLam join_date," +
            "nv.LoaiNhanVien employee_type,nv.TrangThai employee_status FROM dbo.NhanVien nv " +
            "JOIN dbo.TaiKhoan tk ON tk.MaNhanVien=nv.MaNhanVien WHERE nv.MaNhanVien=?", maNhanVien);
        if (dong == null) return null;
        NhanVien nhanVien = new NhanVien();
        nhanVien.setMaNhanVien(String.valueOf(dong.get("employee_id")));
        nhanVien.setHoTen(String.valueOf(dong.get("full_name")));
        nhanVien.setSoDienThoai(String.valueOf(dong.get("phone")));
        if (dong.get("email") != null) nhanVien.setEmail(String.valueOf(dong.get("email")));
        if (dong.get("join_date") != null) nhanVien.setNgayVaoLam(((java.sql.Date) dong.get("join_date")).toLocalDate());
        nhanVien.setPhongBan(String.valueOf(dong.get("employee_type")));
        nhanVien.setTrangThai(String.valueOf(dong.get("employee_status")));
        String maVaiTro = roleCode(nhanVien.getPhongBan());
        int idVaiTro = "MANAGER".equals(maVaiTro) ? 2 : "SALES".equals(maVaiTro) ? 3 : 4;
        String tenVaiTro = "MANAGER".equals(maVaiTro) ? "Quản lý" : "SALES".equals(maVaiTro) ? "Bán hàng" : "Chăm sóc khách hàng";
        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setMaTaiKhoan(String.valueOf(dong.get("account_id")));
        taiKhoan.setTenDangNhap(String.valueOf(dong.get("username")));
        taiKhoan.setMaBamMatKhau(String.valueOf(dong.get("password_hash")));
        taiKhoan.setTrangThai(String.valueOf(dong.get("account_status")));
        taiKhoan.setNhanVien(nhanVien);
        taiKhoan.setVaiTro(new VaiTro(idVaiTro, maVaiTro, tenVaiTro, "Vai trò lấy từ NhanVien.LoaiNhanVien"));
        return taiKhoan;
    }
    public boolean coDuLieuNghiepVu(Connection ketNoi, String maNhanVien) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT (SELECT COUNT(*) FROM dbo.HoaDon WHERE MaNhanVienBanHang=?)+" +
            "(SELECT COUNT(*) FROM dbo.PhieuGiaoHang WHERE MaNhanVienGiaoHang=?)+" +
            "(SELECT COUNT(*) FROM dbo.PhieuBaoHanh WHERE MaNhanVienCSKH=?)+" +
            "(SELECT COUNT(*) FROM dbo.KhieuNai WHERE MaNhanVienCSKH=? OR MaNguoiTao=?)+" +
            "(SELECT COUNT(*) FROM dbo.PhieuDoiTraHoanTien WHERE MaNhanVienBanHang=? OR MaNhanVienQuanLy=?)",
            maNhanVien, maNhanVien, maNhanVien, maNhanVien, maNhanVien, maNhanVien, maNhanVien);
        return v != null && ((Number) v).intValue() > 0;
    }
    public void xoaNhanVien(Connection ketNoi, String maNhanVien) throws SQLException {
        capNhat(ketNoi, "DELETE FROM dbo.CaLamViec WHERE MaNhanVien=? OR MaNguoiPhanCong=?", maNhanVien, maNhanVien);
        capNhat(ketNoi, "DELETE FROM dbo.TaiKhoan WHERE MaNhanVien=?", maNhanVien);
        capNhat(ketNoi, "DELETE FROM dbo.NhanVien WHERE MaNhanVien=?", maNhanVien);
    }
    private static NhanVien anhXaNhanVien(Map<String, Object> dong) {
        NhanVien nhanVien = new NhanVien();
        nhanVien.setMaNhanVien(String.valueOf(dong.get("employee_id")));
        nhanVien.setHoTen(String.valueOf(dong.get("full_name")));
        nhanVien.setSoDienThoai(String.valueOf(dong.get("phone")));
        if (dong.get("email") != null) nhanVien.setEmail(String.valueOf(dong.get("email")));
        if (dong.get("join_date") != null) nhanVien.setNgayVaoLam(((java.sql.Date) dong.get("join_date")).toLocalDate());
        if (dong.get("education_level") != null) nhanVien.setTrinhDo(String.valueOf(dong.get("education_level")));
        if (dong.get("salary_coefficient") instanceof Number) nhanVien.setHeSoLuong(((Number) dong.get("salary_coefficient")).doubleValue());
        if (dong.get("base_salary") != null) nhanVien.setLuongCoBan(new java.math.BigDecimal(String.valueOf(dong.get("base_salary"))));
        if (dong.get("employee_type") != null) nhanVien.setPhongBan(String.valueOf(dong.get("employee_type")));
        else if (dong.get("department") != null) nhanVien.setPhongBan(String.valueOf(dong.get("department")));
        nhanVien.setTrangThai(String.valueOf(dong.get("status")));
        return nhanVien;
    }
    private static String loaiNhanVienTuPhongBan(String p) {
        if (p == null) return "Bán hàng";
        String s = p.toLowerCase();
        if (s.contains("quản") || s.contains("quan")) return "Quản lý";
        if (s.contains("cskh") || s.contains("chăm") || s.contains("cham")) return "CSKH";
        return "Bán hàng";
    }
    private static String roleCode(String loai) {
        if ("Quản lý".equals(loai)) return "MANAGER";
        if ("Bán hàng".equals(loai)) return "SALES";
        return "CUSTOMER_SERVICE";
    }
    private static String roleCodeSql(String c) {
        return "CASE " + c + " WHEN N'Quản lý' THEN 'MANAGER' WHEN N'Bán hàng' THEN 'SALES' ELSE 'CUSTOMER_SERVICE' END";
    }
}

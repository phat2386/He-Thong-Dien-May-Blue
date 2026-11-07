package dao;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import entity.*;

public class NhanVienDAO extends CoSoDAO {

    private static final String COT_NHAN_VIEN = "nv.MaNhanVien employee_id,nv.HoTen full_name,nv.SoDienThoai phone,nv.Email email,"
        + "nv.LoaiNhanVien department,nv.NgayVaoLam join_date,nv.TrangThai status,"
        + "nv.TrinhDo education_level,nv.HeSoLuong salary_coefficient,nv.LuongCoBan base_salary";

    public String goiYMaNhanVien() throws SQLException {
        return goiYMaTiepTheo("NhanVien", "MaNhanVien", "NV", 3);
    }

    public List<Map<String, Object>> timKiem(String tuKhoa, String maTrangThai) throws SQLException {
        String sql = "SELECT " + COT_NHAN_VIEN + ",nv.LoaiNhanVien employee_type,"
            + "CASE WHEN tk.MaTaiKhoan IS NULL THEN NULL WHEN tk.TrangThai=N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END account_status "
            + "FROM dbo.NhanVien nv LEFT JOIN dbo.TaiKhoan tk ON tk.MaNhanVien=nv.MaNhanVien "
            + "WHERE ISNULL(tk.TenDangNhap,'')<>'admin' "
            + "AND (nv.MaNhanVien LIKE ? OR nv.HoTen LIKE ? OR nv.SoDienThoai LIKE ? OR ISNULL(nv.Email,'') LIKE ?) ";
        if (maTrangThai == null || maTrangThai.isBlank()) {
            return truyVan(sql + "ORDER BY nv.MaNhanVien", tuKhoa, tuKhoa, tuKhoa, tuKhoa);
        }
        return truyVan(sql + "AND nv.TrangThai=? ORDER BY nv.MaNhanVien", tuKhoa, tuKhoa, tuKhoa, tuKhoa, maTrangThai);
    }

    public List<NhanVien> timKiemThucThe(String tuKhoa, String maTrangThai) throws SQLException {
        List<NhanVien> ketQua = new ArrayList<NhanVien>();
        for (Map<String, Object> dong : timKiem(tuKhoa, maTrangThai)) {
            ketQua.add(anhXaNhanVien(dong));
        }
        return ketQua;
    }

    public Map<String, Object> timTheoMa(String maNhanVien) throws SQLException {
        return motDong("SELECT " + COT_NHAN_VIEN + " FROM dbo.NhanVien nv WHERE nv.MaNhanVien=?", maNhanVien);
    }

    public NhanVien timThucTheTheoMa(String maNhanVien) throws SQLException {
        Map<String, Object> dong = timTheoMa(maNhanVien);
        if (dong == null) return null;
        return anhXaNhanVien(dong);
    }

    public String timTrangThai(String maNhanVien) throws SQLException {
        return chuoi(motGiaTri("SELECT TrangThai FROM dbo.NhanVien WHERE MaNhanVien=?", maNhanVien));
    }

    public boolean maHoacSoDienThoaiTonTai(Connection ketNoi, String maNhanVien, String soDienThoai) throws SQLException {
        return coKetQua(motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.NhanVien WHERE MaNhanVien=? OR SoDienThoai=?", maNhanVien, soDienThoai));
    }

    public boolean tonTai(Connection ketNoi, String maNhanVien) throws SQLException {
        return motGiaTri(ketNoi, "SELECT MaNhanVien FROM dbo.NhanVien WHERE MaNhanVien=?", maNhanVien) != null;
    }

    public boolean laQuanTri(Connection ketNoi, String maNhanVien) throws SQLException {
        return coKetQua(motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.TaiKhoan WHERE MaNhanVien=? AND TenDangNhap='admin'", maNhanVien));
    }

    public boolean soDienThoaiDaDung(Connection ketNoi, String soDienThoai, String maNhanVien) throws SQLException {
        return coKetQua(motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.NhanVien WHERE SoDienThoai=? AND MaNhanVien<>?", soDienThoai, maNhanVien));
    }

    public int demQuanLyDangHoatDong(Connection ketNoi) throws SQLException {
        return new TaiKhoanDAO().demQuanLyDangHoatDong(ketNoi);
    }

    public void them(Connection ketNoi, String maNhanVien, String hoTen, String soDienThoai, String email, LocalDate ngayVaoLam,
            String trinhDo, Double heSoLuong, BigDecimal luongCoBan, String loaiNhanVien) throws SQLException {
        capNhat(ketNoi, "INSERT INTO dbo.NhanVien(MaNhanVien,HoTen,SoDienThoai,Email,NgayVaoLam,TrinhDo,HeSoLuong,LuongCoBan,LoaiNhanVien,TrangThai) "
            + "VALUES(?,?,?,?,?,?,?,?,?,N'Hoạt động')",
            maNhanVien, hoTen, soDienThoai, email, ngayVaoLam, trinhDo, heSoLuong, luongCoBan, loaiNhanVien);
    }

    public void capNhat(Connection ketNoi, String maNhanVien, String hoTen, String soDienThoai, String email, LocalDate ngayVaoLam,
            String trinhDo, Double heSoLuong, BigDecimal luongCoBan, String loaiNhanVien) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.NhanVien SET HoTen=?,SoDienThoai=?,Email=?,NgayVaoLam=?,TrinhDo=?,HeSoLuong=?,LuongCoBan=?,LoaiNhanVien=? "
            + "WHERE MaNhanVien=?",
            hoTen, soDienThoai, email, ngayVaoLam, trinhDo, heSoLuong, luongCoBan, loaiNhanVien, maNhanVien);
    }

    public void capNhatTrangThai(Connection ketNoi, String maNhanVien, String trangThai) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.NhanVien SET TrangThai=? WHERE MaNhanVien=?", trangThai, maNhanVien);
    }

    public void voHieuTaiKhoanCua(Connection ketNoi, String maNhanVien) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TrangThai=N'Bị khóa',ThoiGianMoKhoa=NULL WHERE MaNhanVien=?", maNhanVien);
    }

    public TaiKhoan timTaiKhoanTheoNhanVien(Connection ketNoi, String maNhanVien) throws SQLException {
        return new TaiKhoanDAO().timTheoMaNhanVien(ketNoi, maNhanVien);
    }

    public boolean coDuLieuNghiepVu(Connection ketNoi, String maNhanVien) throws SQLException {
        String m = maNhanVien;
        Object v = motGiaTri(ketNoi, "SELECT (SELECT COUNT(*) FROM dbo.HoaDon WHERE MaNhanVienBanHang=?)"
            + "+(SELECT COUNT(*) FROM dbo.PhieuGiaoHang WHERE MaNhanVienGiaoHang=?)"
            + "+(SELECT COUNT(*) FROM dbo.PhieuBaoHanh WHERE MaNhanVienCSKH=?)"
            + "+(SELECT COUNT(*) FROM dbo.KhieuNai WHERE MaNhanVienCSKH=? OR MaNguoiTao=?)"
            + "+(SELECT COUNT(*) FROM dbo.PhieuDoiTraHoanTien WHERE MaNhanVienBanHang=? OR MaNhanVienQuanLy=?)",
            m, m, m, m, m, m, m);
        return coKetQua(v);
    }

    public void xoaNhanVien(Connection ketNoi, String maNhanVien) throws SQLException {
        capNhat(ketNoi, "DELETE FROM dbo.CaLamViec WHERE MaNhanVien=? OR MaNguoiPhanCong=?", maNhanVien, maNhanVien);
        capNhat(ketNoi, "DELETE FROM dbo.TaiKhoan WHERE MaNhanVien=?", maNhanVien);
        capNhat(ketNoi, "DELETE FROM dbo.NhanVien WHERE MaNhanVien=?", maNhanVien);
    }

    private static NhanVien anhXaNhanVien(Map<String, Object> dong) {
        NhanVien nhanVien = new NhanVien();
        nhanVien.setMaNhanVien(chuoi(dong.get("employee_id")));
        nhanVien.setHoTen(chuoi(dong.get("full_name")));
        nhanVien.setSoDienThoai(chuoi(dong.get("phone")));
        nhanVien.setPhongBan(chuoi(dong.get("department")));
        nhanVien.setTrangThai(chuoi(dong.get("status")));
        if (dong.get("email") != null) nhanVien.setEmail(chuoi(dong.get("email")));
        if (dong.get("education_level") != null) nhanVien.setTrinhDo(chuoi(dong.get("education_level")));
        if (dong.get("join_date") != null) nhanVien.setNgayVaoLam(((Date) dong.get("join_date")).toLocalDate());
        if (dong.get("salary_coefficient") instanceof Number) nhanVien.setHeSoLuong(((Number) dong.get("salary_coefficient")).doubleValue());
        if (dong.get("base_salary") != null) nhanVien.setLuongCoBan(new BigDecimal(chuoi(dong.get("base_salary"))));
        return nhanVien;
    }

    private static String chuoi(Object giaTri) {
        if (giaTri == null) return null;
        return String.valueOf(giaTri);
    }

    private static boolean coKetQua(Object soLuong) {
        return soLuong != null && ((Number) soLuong).intValue() > 0;
    }
}
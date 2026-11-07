package dao;

import java.sql.*;
import java.util.*;
import connectDB.KetNoiCSDL;
import entity.*;

public class TaiKhoanDAO extends CoSoDAO {

    // Các đoạn SQL dùng lại nhiều lần
    private static final String TU_BANG = " FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien ";

    private static final String SQL_ID_VAI_TRO = "CASE WHEN tk.TenDangNhap='admin' THEN 1 WHEN nv.LoaiNhanVien=N'Quản lý' THEN 2 "
        + "WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 3 WHEN nv.LoaiNhanVien=N'CSKH' THEN 4 ELSE 0 END";

    private static final String SQL_MA_VAI_TRO = "CASE WHEN tk.TenDangNhap='admin' THEN 'ADMIN' WHEN nv.LoaiNhanVien=N'Quản lý' THEN 'MANAGER' "
        + "WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 'SALES' WHEN nv.LoaiNhanVien=N'CSKH' THEN 'CUSTOMER_SERVICE' ELSE 'UNKNOWN' END";

    private static final String SQL_TRANG_THAI_TK = "CASE tk.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END";

    private static final String SQL_TRANG_THAI_NV = "CASE nv.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Không hoạt động' END";

    private static final String SQL_TAI_KHOAN = "SELECT tk.MaTaiKhoan account_id,tk.TenDangNhap username,tk.MatKhau password_hash,"
        + SQL_TRANG_THAI_TK + " account_status,tk.SoLanDangNhapSai failed_attempts,tk.ThoiGianMoKhoa locked_until,tk.LanDangNhapCuoi last_login_at,"
        + "nv.MaNhanVien employee_id,nv.HoTen full_name,nv.SoDienThoai phone,nv.Email email,nv.LoaiNhanVien department,nv.NgayVaoLam join_date,"
        + SQL_TRANG_THAI_NV + " employee_status," + SQL_ID_VAI_TRO + " role_id," + SQL_MA_VAI_TRO + " role_code,"
        + "CASE WHEN tk.TenDangNhap='admin' THEN N'Quản trị hệ thống' WHEN nv.LoaiNhanVien=N'Quản lý' THEN N'Quản lý' "
        + "WHEN nv.LoaiNhanVien=N'Bán hàng' THEN N'Bán hàng' ELSE N'Chăm sóc khách hàng' END role_name,"
        + "N'Vai trò lấy từ NhanVien.LoaiNhanVien' description" + TU_BANG;

    public String goiYMaTaiKhoan() throws SQLException {
        return goiYMaTiepTheo("TaiKhoan", "MaTaiKhoan", "TK", 3);
    }

    // ===== Tìm 1 tài khoản =====

    public TaiKhoan timTheoTenDangNhap(String tenDangNhap) throws SQLException {
        try (Connection ketNoi = KetNoiCSDL.layKetNoi()) {
            return timMot(ketNoi, "tk.TenDangNhap", tenDangNhap);
        }
    }

    public TaiKhoan timTheoMa(String maTaiKhoan) throws SQLException {
        try (Connection ketNoi = KetNoiCSDL.layKetNoi()) {
            return timMot(ketNoi, "tk.MaTaiKhoan", maTaiKhoan);
        }
    }

    // Dùng cho NhanVienDAO và PhanCongDAO
    public TaiKhoan timTheoMaNhanVien(Connection ketNoi, String maNhanVien) throws SQLException {
        return timMot(ketNoi, "nv.MaNhanVien", maNhanVien);
    }

    public TaiKhoan timThongTinTaiKhoanThucThe(String maTaiKhoan) throws SQLException {
        return timTheoMa(maTaiKhoan);
    }

    public TaiKhoan timTrangThaiPhienThucThe(String maTaiKhoan) throws SQLException {
        return timTheoMa(maTaiKhoan);
    }

    // ===== Đăng nhập =====

    public int tangSoLanDangNhapSai(String maTaiKhoan) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET SoLanDangNhapSai=SoLanDangNhapSai+1 WHERE MaTaiKhoan=?", maTaiKhoan);
        Object soLanSai = motGiaTri("SELECT SoLanDangNhapSai FROM dbo.TaiKhoan WHERE MaTaiKhoan=?", maTaiKhoan);
        if (soLanSai == null) throw new SQLException("Không tìm thấy tài khoản.");
        return ((Number) soLanSai).intValue();
    }

    public void datKhoaTamThoi(String maTaiKhoan, int soPhut) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET ThoiGianMoKhoa=DATEADD(MINUTE,?,SYSDATETIME()) WHERE MaTaiKhoan=?", soPhut, maTaiKhoan);
    }

    public void xoaKhoaTamHetHan(String maTaiKhoan) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET SoLanDangNhapSai=0,ThoiGianMoKhoa=NULL "
            + "WHERE MaTaiKhoan=? AND ThoiGianMoKhoa IS NOT NULL AND ThoiGianMoKhoa<=SYSDATETIME()", maTaiKhoan);
    }

    public void ghiNhanDangNhapThanhCong(String maTaiKhoan) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET SoLanDangNhapSai=0,ThoiGianMoKhoa=NULL,LanDangNhapCuoi=SYSDATETIME() WHERE MaTaiKhoan=?", maTaiKhoan);
    }

    public void capNhatMatKhau(String maTaiKhoan, String maBamMatKhau) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET MatKhau=? WHERE MaTaiKhoan=?", maBamMatKhau, maTaiKhoan);
    }

    public void capNhatMatKhau(Connection ketNoi, String maTaiKhoan, String maBamMatKhau) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET MatKhau=? WHERE MaTaiKhoan=?", maBamMatKhau, maTaiKhoan);
    }

    // ===== Tìm kiếm danh sách =====

    public List<Map<String, Object>> timKiemTaiKhoan(String tuKhoa, String maVaiTro, String trangThai) throws SQLException {
        String sql = "SELECT tk.MaTaiKhoan account_code,tk.MaTaiKhoan account_id,nv.MaNhanVien employee_id,nv.HoTen full_name,"
            + "tk.TenDangNhap username," + SQL_MA_VAI_TRO + " role_code,nv.LoaiNhanVien employee_type,nv.SoDienThoai phone,"
            + SQL_TRANG_THAI_TK + " status,tk.LanDangNhapCuoi last_login_at" + TU_BANG + "WHERE 1=1";
        List<Object> thamSo = new ArrayList<Object>();

        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            sql += " AND (tk.MaTaiKhoan LIKE ? OR nv.HoTen LIKE ? OR tk.TenDangNhap LIKE ? OR nv.SoDienThoai LIKE ? OR nv.MaNhanVien LIKE ?)";
            for (int i = 0; i < 5; i++) {
                thamSo.add(tuKhoa);
            }
        }

        String loaiNhanVien = loaiNhanVienTheoMaVaiTro(maVaiTro);
        if ("ADMIN".equals(maVaiTro)) {
            sql += " AND tk.TenDangNhap='admin'";
        } else if (loaiNhanVien != null) {
            sql += " AND nv.LoaiNhanVien=? AND tk.TenDangNhap<>'admin'";
            thamSo.add(loaiNhanVien);
        }

        if ("Hoạt động".equals(trangThai) || "Bị khóa".equals(trangThai)) {
            sql += " AND tk.TrangThai=?";
            thamSo.add(trangThai);
        } else if (trangThai != null) {
            sql += " AND 1=0";
        }

        return truyVan(sql + " ORDER BY tk.MaTaiKhoan", thamSo.toArray());
    }

    public List<TaiKhoan> timKiemTaiKhoanThucThe(String tuKhoa, String maVaiTro, String trangThai) throws SQLException {
        List<TaiKhoan> ketQua = new ArrayList<TaiKhoan>();
        for (Map<String, Object> dong : timKiemTaiKhoan(tuKhoa, maVaiTro, trangThai)) {
            TaiKhoan taiKhoan = timTheoMa(String.valueOf(dong.get("account_id")));
            if (taiKhoan != null) ketQua.add(taiKhoan);
        }
        return ketQua;
    }

    public List<Map<String, Object>> timNhanVienChuaCoTaiKhoan() throws SQLException {
        return truyVan("SELECT nv.MaNhanVien employee_id,nv.HoTen full_name,nv.SoDienThoai phone FROM dbo.NhanVien nv "
            + "WHERE NOT EXISTS(SELECT 1 FROM dbo.TaiKhoan tk WHERE tk.MaNhanVien=nv.MaNhanVien) ORDER BY nv.HoTen");
    }

    public List<NhanVien> timNhanVienChuaCoTaiKhoanThucThe() throws SQLException {
        List<NhanVien> ketQua = new ArrayList<NhanVien>();
        for (Map<String, Object> dong : timNhanVienChuaCoTaiKhoan()) {
            NhanVien nhanVien = new NhanVien();
            nhanVien.setMaNhanVien(String.valueOf(dong.get("employee_id")));
            nhanVien.setHoTen(String.valueOf(dong.get("full_name")));
            nhanVien.setSoDienThoai(String.valueOf(dong.get("phone")));
            nhanVien.setTrangThai("Hoạt động");
            ketQua.add(nhanVien);
        }
        return ketQua;
    }

    // ===== Kiểm tra =====

    public String timTrangThai(String maTaiKhoan) throws SQLException {
        Object trangThai = motGiaTri("SELECT " + SQL_TRANG_THAI_TK + " FROM dbo.TaiKhoan tk WHERE tk.MaTaiKhoan=?", maTaiKhoan);
        if (trangThai == null) return null;
        return String.valueOf(trangThai);
    }

    public Map<String, Object> timTomTat(String maTaiKhoan) throws SQLException {
        return motDong("SELECT " + SQL_ID_VAI_TRO + " role_id," + SQL_MA_VAI_TRO + " role_code," + SQL_TRANG_THAI_TK + " status,"
            + SQL_TRANG_THAI_NV + " employee_status,nv.MaNhanVien employee_id" + TU_BANG + "WHERE tk.MaTaiKhoan=?", maTaiKhoan);
    }

    public boolean tonTaiTheoNhanVienHoacTenDangNhap(String maNhanVien, String tenDangNhap) throws SQLException {
        return coKetQua(motGiaTri("SELECT COUNT(*) FROM dbo.TaiKhoan WHERE MaNhanVien=? OR TenDangNhap=?", maNhanVien, tenDangNhap));
    }

    public boolean tonTaiTenDangNhapKhacMa(String tenDangNhap, String maTaiKhoan) throws SQLException {
        return coKetQua(motGiaTri("SELECT COUNT(*) FROM dbo.TaiKhoan WHERE TenDangNhap=? AND MaTaiKhoan<>?", tenDangNhap, maTaiKhoan));
    }

    public boolean tonTaiMaTaiKhoan(Connection ketNoi, String maTaiKhoan) throws SQLException {
        return coKetQua(motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.TaiKhoan WHERE MaTaiKhoan=?", maTaiKhoan));
    }

    public boolean coLichSu(String maTaiKhoan) throws SQLException {
        Map<String, Object> tomTat = timTomTat(maTaiKhoan);
        if (tomTat == null) return false;
        String maNhanVien = String.valueOf(tomTat.get("employee_id"));
        try (Connection ketNoi = KetNoiCSDL.layKetNoi()) {
            return new NhanVienDAO().coDuLieuNghiepVu(ketNoi, maNhanVien);
        }
    }

    public int demQuanLyDangHoatDong(Connection ketNoi) throws SQLException {
        Object soLuong = motGiaTri(ketNoi, "SELECT COUNT(*)" + TU_BANG
            + "WHERE nv.LoaiNhanVien=N'Quản lý' AND nv.TrangThai=N'Hoạt động' AND tk.TrangThai=N'Hoạt động' AND tk.TenDangNhap<>'admin'");
        if (soLuong == null) return 0;
        return ((Number) soLuong).intValue();
    }

    public int demQuanLyDangHoatDong() throws SQLException {
        try (Connection ketNoi = KetNoiCSDL.layKetNoi()) {
            return demQuanLyDangHoatDong(ketNoi);
        }
    }

    // ===== Thêm / sửa / xóa =====

    public void them(Connection ketNoi, String maTaiKhoan, String maNhanVien, String tenDangNhap, String maBamMatKhau) throws SQLException {
        capNhat(ketNoi, "INSERT INTO dbo.TaiKhoan(MaTaiKhoan,TenDangNhap,MatKhau,TrangThai,SoLanDangNhapSai,ThoiGianMoKhoa,LanDangNhapCuoi,MaNhanVien) "
            + "VALUES(?,?,?,N'Hoạt động',0,NULL,NULL,?)", maTaiKhoan, tenDangNhap, maBamMatKhau, maNhanVien);
    }

    public void capNhatTenDangNhap(Connection ketNoi, String maTaiKhoan, String tenDangNhap) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TenDangNhap=? WHERE MaTaiKhoan=?", tenDangNhap, maTaiKhoan);
    }

    public void capNhatTrangThai(Connection ketNoi, String maTaiKhoan, String trangThaiMoi) throws SQLException {
        if ("Hoạt động".equals(trangThaiMoi)) {
            capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TrangThai=N'Hoạt động',ThoiGianMoKhoa=NULL,SoLanDangNhapSai=0 WHERE MaTaiKhoan=?", maTaiKhoan);
        } else {
            capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TrangThai=N'Bị khóa' WHERE MaTaiKhoan=?", maTaiKhoan);
        }
    }

    public void voHieuHoa(Connection ketNoi, String maTaiKhoan) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TrangThai=N'Bị khóa',ThoiGianMoKhoa=NULL WHERE MaTaiKhoan=?", maTaiKhoan);
    }

    public void xoaTaiKhoan(Connection ketNoi, String maTaiKhoan) throws SQLException {
        capNhat(ketNoi, "DELETE FROM dbo.TaiKhoan WHERE MaTaiKhoan=?", maTaiKhoan);
    }

    // ===== Hàm phụ =====

    private TaiKhoan timMot(Connection ketNoi, String cot, String giaTri) throws SQLException {
        try (PreparedStatement cauLenh = ketNoi.prepareStatement(SQL_TAI_KHOAN + "WHERE " + cot + "=?")) {
            cauLenh.setString(1, giaTri);
            try (ResultSet tapKetQua = cauLenh.executeQuery()) {
                if (!tapKetQua.next()) return null;
                return anhXaTaiKhoan(tapKetQua);
            }
        }
    }

    private TaiKhoan anhXaTaiKhoan(ResultSet rs) throws SQLException {
        NhanVien nhanVien = new NhanVien();
        nhanVien.setMaNhanVien(rs.getString("employee_id"));
        nhanVien.setHoTen(rs.getString("full_name"));
        nhanVien.setSoDienThoai(rs.getString("phone"));
        nhanVien.setEmail(rs.getString("email"));
        nhanVien.setPhongBan(rs.getString("department"));
        nhanVien.setTrangThai(rs.getString("employee_status"));
        if (rs.getDate("join_date") != null) nhanVien.setNgayVaoLam(rs.getDate("join_date").toLocalDate());

        TaiKhoan taiKhoan = new TaiKhoan();
        taiKhoan.setMaTaiKhoan(rs.getString("account_id"));
        taiKhoan.setTenDangNhap(rs.getString("username"));
        taiKhoan.setMaBamMatKhau(rs.getString("password_hash"));
        taiKhoan.setTrangThai(rs.getString("account_status"));
        taiKhoan.setSoLanDangNhapSai(rs.getInt("failed_attempts"));
        if (rs.getTimestamp("locked_until") != null) taiKhoan.setKhoaDen(rs.getTimestamp("locked_until").toLocalDateTime());
        if (rs.getTimestamp("last_login_at") != null) taiKhoan.setLanDangNhapCuoi(rs.getTimestamp("last_login_at").toLocalDateTime());
        taiKhoan.setNhanVien(nhanVien);
        taiKhoan.setVaiTro(new VaiTro(rs.getInt("role_id"), rs.getString("role_code"), rs.getString("role_name"), rs.getString("description")));
        return taiKhoan;
    }

    private static boolean coKetQua(Object soLuong) {
        return soLuong != null && ((Number) soLuong).intValue() > 0;
    }

    private static String loaiNhanVienTheoMaVaiTro(String maVaiTro) {
        if ("MANAGER".equals(maVaiTro)) return "Quản lý";
        if ("SALES".equals(maVaiTro)) return "Bán hàng";
        if ("CUSTOMER_SERVICE".equals(maVaiTro)) return "CSKH";
        return null;
    }
}
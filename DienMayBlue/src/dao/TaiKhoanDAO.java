package dao;
import java.sql.*;
import java.util.*;
import connectDB.KetNoiCSDL;
import entity.*;
public class TaiKhoanDAO extends CoSoDAO {
    public String goiYMaTaiKhoan() throws SQLException {
        return goiYMaTiepTheo("TaiKhoan", "MaTaiKhoan", "TK", 3);
    }
    private static final String SQL_TAI_KHOAN = "SELECT tk.MaTaiKhoan account_id, " + "tk.TenDangNhap username, tk.MatKhau password_hash, " +
        "CASE tk.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END account_status, " + "tk.SoLanDangNhapSai failed_attempts, " +
        "tk.ThoiGianMoKhoa locked_until, tk.LanDangNhapCuoi last_login_at, " + "nv.MaNhanVien employee_id, nv.HoTen full_name, nv.SoDienThoai phone, nv.Email email, " +
        "nv.LoaiNhanVien department, nv.NgayVaoLam join_date, CASE nv.TrangThai WHEN " + "N'Hoạt động' THEN N'Hoạt động' ELSE N'Không hoạt động' END employee_status, " + "CASE WHEN tk.TenDangNhap='admin' THEN 1 " +
        "     WHEN nv.LoaiNhanVien=N'Quản lý' THEN 2 " + "     WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 3 " + "     WHEN nv.LoaiNhanVien=N'CSKH' THEN 4 ELSE 0 END role_id, " +
        "CASE WHEN tk.TenDangNhap='admin' THEN 'ADMIN' " + "     WHEN nv.LoaiNhanVien=N'Quản lý' THEN 'MANAGER' " + "     WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 'SALES' " +
        "     WHEN nv.LoaiNhanVien=N'CSKH' THEN 'CUSTOMER_SERVICE' ELSE 'UNKNOWN' END role_code, " + "CASE WHEN tk.TenDangNhap='admin' THEN N'Quản trị hệ thống' " +
        "     WHEN nv.LoaiNhanVien=N'Quản lý' THEN N'Quản lý' " + "     WHEN nv.LoaiNhanVien=N'Bán hàng' THEN N'Bán hàng' " +
        "     ELSE N'Chăm sóc khách hàng' END role_name, " + "N'Vai trò lấy từ NhanVien.LoaiNhanVien' description " + "FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien ";
    public TaiKhoan timTheoTenDangNhap(String tenDangNhap) throws SQLException {
        try (Connection ketNoi = KetNoiCSDL.layKetNoi();
        PreparedStatement cauLenh = ketNoi.prepareStatement(SQL_TAI_KHOAN + "WHERE tk.TenDangNhap=?")) {
            cauLenh.setString(1, tenDangNhap);
            try (ResultSet tapKetQua = cauLenh.executeQuery()) {
                return tapKetQua.next() ? anhXaTaiKhoan(tapKetQua) : null;
            }
        }
    }
    public TaiKhoan timTheoMa(String maTaiKhoan) throws SQLException {
        try (Connection ketNoi = KetNoiCSDL.layKetNoi();
        PreparedStatement cauLenh = ketNoi.prepareStatement(SQL_TAI_KHOAN + "WHERE tk.MaTaiKhoan=?")) {
            cauLenh.setString(1, maTaiKhoan);
            try (ResultSet tapKetQua = cauLenh.executeQuery()) {
                return tapKetQua.next() ? anhXaTaiKhoan(tapKetQua) : null;
            }
        }
    }
    public int tangSoLanDangNhapSai(String maTaiKhoan) throws SQLException {
        try (Connection ketNoi = KetNoiCSDL.layKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                Object v = motGiaTri(ketNoi, "SELECT SoLanDangNhapSai FROM dbo.TaiKhoan WITH (UPDLOCK,ROWLOCK) WHERE MaTaiKhoan=?", maTaiKhoan);
                if (v == null) throw new SQLException("Không tìm thấy tài khoản.");
                int soLanSai = ((Number) v).intValue() + 1;
                capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET SoLanDangNhapSai=? WHERE MaTaiKhoan=?", soLanSai, maTaiKhoan);
                ketNoi.commit();
                return soLanSai;
            } catch (SQLException ex) {
                ketNoi.rollback();
                throw ex;
            }
        }
    }
    public void datKhoaTamThoi(String maTaiKhoan, int soPhut) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET ThoiGianMoKhoa=DATEADD(MINUTE,?,SYSDATETIME()) WHERE MaTaiKhoan=?", soPhut, maTaiKhoan);
    }
    public void xoaKhoaTamHetHan(String maTaiKhoan) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET SoLanDangNhapSai=0,ThoiGianMoKhoa=NULL " + "WHERE MaTaiKhoan=? AND ThoiGianMoKhoa IS NOT NULL AND ThoiGianMoKhoa<=SYSDATETIME()", maTaiKhoan);
    }
    public void ghiNhanDangNhapThanhCong(String maTaiKhoan) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET SoLanDangNhapSai=0,ThoiGianMoKhoa=NULL," + "LanDangNhapCuoi=SYSDATETIME() WHERE MaTaiKhoan=?", maTaiKhoan);
    }
    public void capNhatMatKhau(String maTaiKhoan, String maBamMatKhau) throws SQLException {
        capNhat("UPDATE dbo.TaiKhoan SET MatKhau=? WHERE MaTaiKhoan=?", maBamMatKhau, maTaiKhoan);
    }
    public void capNhatMatKhau(Connection ketNoi, String maTaiKhoan, String maBamMatKhau) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET MatKhau=? WHERE MaTaiKhoan=?", maBamMatKhau, maTaiKhoan);
    }
    public List<Map<String, Object>> timKiemTaiKhoan(String tuKhoa, String maVaiTro, String trangThai) throws SQLException {
        StringBuilder sql = new StringBuilder( "SELECT tk.MaTaiKhoan account_code,tk.MaTaiKhoan account_id," +
            "nv.MaNhanVien employee_id,nv.HoTen full_name,tk.TenDangNhap username," + "CASE WHEN tk.TenDangNhap='admin' THEN 'ADMIN' WHEN nv.LoaiNhanVien=N'Quản lý' THEN 'MANAGER' " +
            "WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 'SALES' WHEN nv.LoaiNhanVien=N'CSKH' " + "THEN 'CUSTOMER_SERVICE' ELSE 'UNKNOWN' END role_code," +
            "nv.LoaiNhanVien employee_type,nv.SoDienThoai phone," + "CASE tk.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END status," +
            "tk.LanDangNhapCuoi last_login_at " + "FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien WHERE 1=1");
        List<Object> p = new ArrayList<Object>();
        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            sql.append(" AND (tk.MaTaiKhoan LIKE ? OR nv.HoTen LIKE ? OR tk.TenDangNhap LIKE ? OR " + "nv.SoDienThoai LIKE ? OR nv.MaNhanVien LIKE ?)");
            p.add(tuKhoa);
            p.add(tuKhoa);
            p.add(tuKhoa);
            p.add(tuKhoa);
            p.add(tuKhoa);
        }
        if (maVaiTro != null) {
            if ("ADMIN".equals(maVaiTro)) sql.append(" AND tk.TenDangNhap='admin'");
            else {
                String loai = loaiNhanVienTheoMaVaiTro(maVaiTro);
                if (loai != null) {
                    sql.append(" AND nv.LoaiNhanVien=? AND tk.TenDangNhap<>'admin'");
                    p.add(loai);
                }
            }
        }
        if (trangThai != null) {
            if ("Hoạt động".equals(trangThai)) sql.append(" AND tk.TrangThai=N'Hoạt động'");
            else if ("Bị khóa".equals(trangThai))
                sql.append(" AND tk.TrangThai=N'Bị khóa'");
            else sql.append(" AND 1=0");
        }
        sql.append(" ORDER BY tk.MaTaiKhoan");
        return truyVan(sql.toString(), p.toArray());
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
        return truyVan("SELECT nv.MaNhanVien employee_id,nv.HoTen full_name,nv.SoDienThoai phone " + "FROM dbo.NhanVien nv WHERE NOT EXISTS(SELECT 1 FROM dbo.TaiKhoan tk WHERE tk.MaNhanVien=nv.MaNhanVien) " + "ORDER BY nv.HoTen");
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
    public TaiKhoan timThongTinTaiKhoanThucThe(String maTaiKhoan) throws SQLException {
        return timTheoMa(maTaiKhoan);
    }
    public TaiKhoan timTrangThaiPhienThucThe(String maTaiKhoan) throws SQLException {
        return timTheoMa(maTaiKhoan);
    }
    public String timTrangThai(String maTaiKhoan) throws SQLException {
        Object v = motGiaTri( "SELECT CASE TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END FROM dbo.TaiKhoan WHERE MaTaiKhoan=?", maTaiKhoan);
        return v == null ? null : String.valueOf(v);
    }
    public Map<String, Object> timTomTat(String maTaiKhoan) throws SQLException {
        return motDong("SELECT CASE WHEN tk.TenDangNhap='admin' THEN 1 WHEN nv.LoaiNhanVien=N'Quản lý' " + "THEN 2 WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 3 ELSE 4 END role_id," +
            "CASE WHEN tk.TenDangNhap='admin' THEN 'ADMIN' WHEN nv.LoaiNhanVien=N'Quản lý' " + "THEN 'MANAGER' WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 'SALES' ELSE " +
            "'CUSTOMER_SERVICE' END role_code," + "CASE tk.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END status," +
            "CASE nv.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Không hoạt động' END employee_status,nv.MaNhanVien employee_id " + "FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien WHERE tk.MaTaiKhoan=?", maTaiKhoan);
    }
    public boolean tonTaiTheoNhanVienHoacTenDangNhap(String maNhanVien, String tenDangNhap) throws SQLException {
        Object v = motGiaTri("SELECT COUNT(*) FROM dbo.TaiKhoan WHERE MaNhanVien=? OR TenDangNhap=?", maNhanVien, tenDangNhap);
        return v != null && ((Number) v).intValue() > 0;
    }
    public boolean tonTaiTenDangNhapKhacMa(String tenDangNhap, String maTaiKhoan) throws SQLException {
        Object v = motGiaTri("SELECT COUNT(*) FROM dbo.TaiKhoan WHERE TenDangNhap=? AND MaTaiKhoan<>?", tenDangNhap, maTaiKhoan);
        return v != null && ((Number) v).intValue() > 0;
    }
    public boolean coLichSu(String maTaiKhoan) throws SQLException {
        Map<String, Object> t = timTomTat(maTaiKhoan);
        if (t == null) return false;
        String nv = String.valueOf(t.get("employee_id"));
        Object v = motGiaTri("SELECT (SELECT COUNT(*) FROM dbo.HoaDon WHERE MaNhanVienBanHang=?)+" + "(SELECT COUNT(*) FROM dbo.PhieuGiaoHang WHERE MaNhanVienGiaoHang=?)+" +
            "(SELECT COUNT(*) FROM dbo.PhieuBaoHanh WHERE MaNhanVienCSKH=?)+" + "(SELECT COUNT(*) FROM dbo.KhieuNai WHERE MaNhanVienCSKH=?)+" +
            "(SELECT COUNT(*) FROM dbo.PhieuDoiTraHoanTien WHERE MaNhanVienBanHang=? OR MaNhanVienQuanLy=?)", nv, nv, nv, nv, nv, nv);
        return v != null && ((Number) v).intValue() > 0;
    }
    public void them(Connection ketNoi, String maTaiKhoan, String maNhanVien, String tenDangNhap, String maBamMatKhau) throws SQLException {
        capNhat(ketNoi, "INSERT INTO dbo.TaiKhoan(MaTaiKhoan,TenDangNhap,MatKhau,TrangThai," + "SoLanDangNhapSai,ThoiGianMoKhoa,LanDangNhapCuoi,MaNhanVien) " +
            "VALUES(?,?,?,N'Hoạt động',0,NULL,NULL,?)", maTaiKhoan, tenDangNhap, maBamMatKhau, maNhanVien);
    }
    public boolean tonTaiMaTaiKhoan(Connection ketNoi, String maTaiKhoan) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.TaiKhoan WHERE MaTaiKhoan=?", maTaiKhoan);
        return v != null && ((Number) v).intValue() > 0;
    }
    public void capNhatTenDangNhap(Connection ketNoi, String maTaiKhoan, String tenDangNhap) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TenDangNhap=? WHERE MaTaiKhoan=?", tenDangNhap, maTaiKhoan);
    }
    public void capNhatTrangThai(Connection ketNoi, String maTaiKhoan, String trangThaiMoi) throws SQLException {
        String dbStatus = "Hoạt động".equals(trangThaiMoi) ? "Hoạt động" : "Bị khóa";
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TrangThai=?,ThoiGianMoKhoa=CASE WHEN ?=N'Hoạt động' THEN NULL ELSE ThoiGianMoKhoa END," +
            "SoLanDangNhapSai=CASE WHEN ?=N'Hoạt động' THEN 0 ELSE SoLanDangNhapSai END WHERE MaTaiKhoan=?", dbStatus, dbStatus, dbStatus, maTaiKhoan);
    }
    public void voHieuHoa(Connection ketNoi, String maTaiKhoan) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.TaiKhoan SET TrangThai=N'Bị khóa',ThoiGianMoKhoa=NULL WHERE MaTaiKhoan=?", maTaiKhoan);
    }
    public void xoaTaiKhoan(Connection ketNoi, String maTaiKhoan) throws SQLException {
        capNhat(ketNoi, "DELETE FROM dbo.TaiKhoan WHERE MaTaiKhoan=?", maTaiKhoan);
    }
    public int demQuanLyDangHoatDong(Connection ketNoi) throws SQLException {
        Object v = motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien " +
            "WHERE nv.LoaiNhanVien=N'Quản lý' AND nv.TrangThai=N'Hoạt động' AND " + "tk.TrangThai=N'Hoạt động' AND tk.TenDangNhap<>'admin'");
        return v == null ? 0 : ((Number) v).intValue();
    }
    public int demQuanLyDangHoatDong() throws SQLException {
        try (Connection ketNoi = KetNoiCSDL.layKetNoi()) {
            return demQuanLyDangHoatDong(ketNoi);
        }
    }
    private TaiKhoan anhXaTaiKhoan(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("employee_id"));
        nv.setHoTen(rs.getString("full_name"));
        nv.setSoDienThoai(rs.getString("phone"));
        nv.setEmail(rs.getString("email"));
        nv.setPhongBan(rs.getString("department"));
        if (rs.getDate("join_date") != null) nv.setNgayVaoLam(rs.getDate("join_date").toLocalDate());
        nv.setTrangThai(rs.getString("employee_status"));
        VaiTro vt = new VaiTro(rs.getInt("role_id"), rs.getString("role_code"), rs.getString("role_name"), rs.getString("description"));
        TaiKhoan tk = new TaiKhoan();
        tk.setMaTaiKhoan(rs.getString("account_id"));
        tk.setTenDangNhap(rs.getString("username"));
        tk.setMaBamMatKhau(rs.getString("password_hash"));
        tk.setTrangThai(rs.getString("account_status"));
        tk.setSoLanDangNhapSai(rs.getInt("failed_attempts"));
        Timestamp t = rs.getTimestamp("locked_until");
        if (t != null) tk.setKhoaDen(t.toLocalDateTime());
        Timestamp lanCuoi = rs.getTimestamp("last_login_at");
        if (lanCuoi != null) tk.setLanDangNhapCuoi(lanCuoi.toLocalDateTime());
        tk.setNhanVien(nv);
        tk.setVaiTro(vt);
        return tk;
    }
    private static String loaiNhanVienTheoMaVaiTro(String code) {
        if ("MANAGER".equals(code)) return "Quản lý";
        if ("SALES".equals(code)) return "Bán hàng";
        if ("CUSTOMER_SERVICE".equals(code)) return "CSKH";
        return null;
    }
}

package dao;

import java.sql.*;
import java.sql.Date;
import java.time.*;
import java.util.*;
import entity.*;

public class PhanCongDAO extends CoSoDAO {

    public String goiYMaCa() throws SQLException {
        return goiYMaTiepTheo("CaLamViec", "MaCa", "CA", 3);
    }

    public List<PhanCong> timPhanCongTheoNgayThucThe(LocalDate ngay) throws SQLException {
        List<PhanCong> ketQua = new ArrayList<PhanCong>();
        for (Map<String, Object> dong : timPhanCongTheoNgay(ngay)) {
            NhanVien nhanVien = new NhanVien();
            nhanVien.setMaNhanVien(chuoi(dong.get("employee_id")));
            nhanVien.setHoTen(chuoi(dong.get("full_name")));
            nhanVien.setPhongBan(chuoi(dong.get("employee_type")));
            nhanVien.setTrangThai("Hoạt động");
            ketQua.add(anhXaPhanCong(dong, nhanVien));
        }
        return ketQua;
    }

    public List<Map<String, Object>> timPhanCongTheoNgay(LocalDate ngay) throws SQLException {
        return truyVan("SELECT c.MaCa assignment_code,c.NgayLamViec work_date," + shiftIdSql("c.LoaiCa") + " shift_id,"
            + "c.LoaiCa shift_name,c.GioBatDau start_time,c.GioKetThuc end_time,"
            + "nv.MaNhanVien employee_id,nv.HoTen full_name,nv.LoaiNhanVien employee_type," + roleCodeSql("nv.LoaiNhanVien") + " role_code,"
            + "c.GhiChu note,c.MaNguoiPhanCong assigned_by,c.TrangThai status "
            + "FROM dbo.CaLamViec c JOIN dbo.NhanVien nv ON nv.MaNhanVien=c.MaNhanVien "
            + "WHERE c.NgayLamViec=? AND c.TrangThai=N'Đã phân công' ORDER BY c.GioBatDau,nv.HoTen", ngay);
    }

    public List<NhanVien> timNhanVienCoThePhanCongThucThe() throws SQLException {
        List<NhanVien> ketQua = new ArrayList<NhanVien>();
        for (Map<String, Object> dong : timNhanVienCoThePhanCong()) {
            NhanVien nhanVien = new NhanVien();
            nhanVien.setMaNhanVien(chuoi(dong.get("employee_id")));
            nhanVien.setHoTen(chuoi(dong.get("full_name")));
            nhanVien.setPhongBan("SALES".equals(dong.get("role_code")) ? "Bán hàng" : "CSKH");
            nhanVien.setTrangThai("Hoạt động");
            ketQua.add(nhanVien);
        }
        return ketQua;
    }

    public List<Map<String, Object>> timNhanVienCoThePhanCong() throws SQLException {
        return truyVan("SELECT nv.MaNhanVien employee_id,nv.HoTen full_name,"
            + "CASE nv.LoaiNhanVien WHEN N'Bán hàng' THEN N'Bán hàng' ELSE N'Chăm sóc khách hàng' END role_name,"
            + roleCodeSql("nv.LoaiNhanVien") + " role_code "
            + "FROM dbo.NhanVien nv JOIN dbo.TaiKhoan tk ON tk.MaNhanVien=nv.MaNhanVien "
            + "WHERE tk.TrangThai=N'Hoạt động' AND nv.TrangThai=N'Hoạt động' AND nv.LoaiNhanVien IN(N'Bán hàng',N'CSKH') "
            + "ORDER BY nv.MaNhanVien");
    }

    public int demBanHangDaPhanCong(LocalDate ngay, String tenCa) throws SQLException {
        return laySo(motGiaTri("SELECT COUNT(*) FROM dbo.CaLamViec c JOIN dbo.NhanVien nv ON nv.MaNhanVien=c.MaNhanVien "
            + "WHERE c.NgayLamViec=? AND c.LoaiCa=? AND c.TrangThai=N'Đã phân công' AND nv.LoaiNhanVien=N'Bán hàng'", ngay, tenCa));
    }

    public TaiKhoan timTaiKhoanTheoNhanVien(Connection ketNoi, String maNhanVien) throws SQLException {
        return new TaiKhoanDAO().timTheoMaNhanVien(ketNoi, maNhanVien);
    }

    public boolean daDuocPhanCa(Connection ketNoi, String maNhanVien, LocalDate ngay) throws SQLException {
        return laySo(motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.CaLamViec WHERE MaNhanVien=? AND NgayLamViec=? AND TrangThai=N'Đã phân công'",
            maNhanVien, ngay)) > 0;
    }

    public PhanCong timPhanCongThucThe(Connection ketNoi, String maPhanCong) throws SQLException {
        Map<String, Object> d = motDong(ketNoi, "SELECT c.MaCa assignment_code,c.NgayLamViec work_date,c.LoaiCa shift_name,"
            + "c.GioBatDau start_time,c.GioKetThuc end_time,c.GhiChu note,c.TrangThai status,c.MaNguoiPhanCong assigned_by,"
            + "nv.MaNhanVien employee_id,nv.HoTen full_name,nv.SoDienThoai phone,nv.Email email,nv.NgayVaoLam join_date,"
            + "nv.LoaiNhanVien employee_type,nv.TrangThai employee_status "
            + "FROM dbo.CaLamViec c JOIN dbo.NhanVien nv ON nv.MaNhanVien=c.MaNhanVien WHERE c.MaCa=?", maPhanCong);
        if (d == null) return null;
        LocalDate ngayVaoLam = ((Date) d.get("join_date")).toLocalDate();
        NhanVien nhanVien = new NhanVien(chuoi(d.get("employee_id")), chuoi(d.get("full_name")), chuoi(d.get("phone")),
            chuoi(d.get("email")), chuoi(d.get("employee_type")), ngayVaoLam, chuoi(d.get("employee_status")));
        return anhXaPhanCong(d, nhanVien);
    }

    public String timMaVaiTroNhanVien(Connection ketNoi, String maNhanVien) throws SQLException {
        Object loai = motGiaTri(ketNoi, "SELECT LoaiNhanVien FROM dbo.NhanVien WHERE MaNhanVien=?", maNhanVien);
        if (loai == null) return null;
        return roleCode(String.valueOf(loai));
    }

    public int demNguoiKhacDaPhanCong(Connection ketNoi, Object ngayLamViec, String tenCa, String maPhanCong) throws SQLException {
        return laySo(motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.CaLamViec WHERE NgayLamViec=? AND LoaiCa=? AND MaCa<>?",
            ngayLamViec, tenCa, maPhanCong));
    }

    public int demBanHangKhacDaPhanCong(Connection ketNoi, Object ngayLamViec, String tenCa, String maPhanCong) throws SQLException {
        return laySo(motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.CaLamViec c JOIN dbo.NhanVien nv ON nv.MaNhanVien=c.MaNhanVien "
            + "WHERE c.NgayLamViec=? AND c.LoaiCa=? AND c.MaCa<>? AND c.TrangThai=N'Đã phân công' AND nv.LoaiNhanVien=N'Bán hàng'",
            ngayLamViec, tenCa, maPhanCong));
    }

    public boolean maCaTonTai(Connection ketNoi, String maCaKyHieu) throws SQLException {
        return laySo(motGiaTri(ketNoi, "SELECT COUNT(*) FROM dbo.CaLamViec WHERE MaCa=?", maCaKyHieu)) > 0;
    }

    public void themPhanCong(Connection ketNoi, String maCaKyHieu, String maNhanVien, LocalDate ngay, CaLamViec ca,
            String maNguoiPhanCong, String ghiChu) throws SQLException {
        String ghiChuLuu = null;
        if (ghiChu != null && !ghiChu.trim().isEmpty()) ghiChuLuu = ghiChu.trim();
        capNhat(ketNoi, "INSERT INTO dbo.CaLamViec(MaCa,NgayLamViec,GioBatDau,GioKetThuc,LoaiCa,GhiChu,TrangThai,MaNhanVien,MaNguoiPhanCong) "
            + "VALUES(?,?,?,?,?,?,N'Đã phân công',?,?)",
            maCaKyHieu, ngay, ca.getGioBatDau(), ca.getGioKetThuc(), ca.getTenCa(), ghiChuLuu, maNhanVien, maNguoiPhanCong);
    }

    public void huyPhanCong(Connection ketNoi, String maPhanCong) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.CaLamViec SET TrangThai=N'Đã hủy' WHERE MaCa=?", maPhanCong);
    }

    public void huyPhanCongTuongLai(Connection ketNoi, String maNhanVien) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.CaLamViec SET TrangThai=N'Đã hủy' "
            + "WHERE MaNhanVien=? AND NgayLamViec>=CAST(GETDATE() AS DATE) AND TrangThai=N'Đã phân công'", maNhanVien);
    }

    public void thayNhanVienPhanCong(Connection ketNoi, String maPhanCong, String maNhanVienMoi) throws SQLException {
        capNhat(ketNoi, "UPDATE dbo.CaLamViec SET MaNhanVien=? WHERE MaCa=? AND TrangThai=N'Đã phân công'", maNhanVienMoi, maPhanCong);
    }

    // Dùng chung cho 2 hàm tìm phân công
    private static PhanCong anhXaPhanCong(Map<String, Object> d, NhanVien nhanVien) {
        String maCa = chuoi(d.get("assignment_code"));
        LocalTime gioBatDau = ((Time) d.get("start_time")).toLocalTime();
        LocalTime gioKetThuc = ((Time) d.get("end_time")).toLocalTime();
        CaLamViec ca = new CaLamViec(maCa, chuoi(d.get("shift_name")), gioBatDau, gioKetThuc, true);
        ca.setNgayLamViec(((Date) d.get("work_date")).toLocalDate());
        String nguoiPhanCong = d.get("assigned_by") == null ? "SYSTEM" : chuoi(d.get("assigned_by"));
        PhanCong phanCong = new PhanCong(maCa, nhanVien, ca.getNgayLamViec(), ca, nguoiPhanCong, chuoi(d.get("status")), null);
        phanCong.setGhiChu(chuoi(d.get("note")));
        return phanCong;
    }

    private static String chuoi(Object giaTri) {
        if (giaTri == null) return null;
        return String.valueOf(giaTri);
    }

    private static int laySo(Object soLuong) {
        if (soLuong == null) return 0;
        return ((Number) soLuong).intValue();
    }

    private static String roleCode(String loai) {
        if ("Quản lý".equals(loai)) return "MANAGER";
        if ("Bán hàng".equals(loai)) return "SALES";
        return "CUSTOMER_SERVICE";
    }

    private static String roleCodeSql(String cot) {
        return "CASE " + cot + " WHEN N'Quản lý' THEN 'MANAGER' WHEN N'Bán hàng' THEN 'SALES' ELSE 'CUSTOMER_SERVICE' END";
    }

    private static String shiftIdSql(String cot) {
        return "CASE " + cot + " WHEN N'Sáng' THEN 1 WHEN N'Chiều' THEN 2 ELSE 3 END";
    }
}
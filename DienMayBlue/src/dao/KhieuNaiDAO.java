package dao;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import entity.*;
import java.sql.Date;
public class KhieuNaiDAO extends CoSoDAO {
    public String goiYMaKhieuNai() throws SQLException {
        return goiYMaTiepTheo("KhieuNai", "MaKhieuNai", "KN", 3);
    }
    public List<Map<String, Object>> timKhieuNai() throws SQLException {
        return truyVan("SELECT kn.MaKhieuNai complaint_id,kn.MaKhachHang customer_id,kh.HoTen customer," + "kh.SoDienThoai customer_phone,kh.Email customer_email,kh.DiaChi " + "customer_address," +
            "kn.NoiDung content,kn.NgayGhiNhan recorded_at,kn.MucUuTien priority," + "CASE kn.TrangThaiXuLy WHEN N'Chưa xử lý' THEN 'NEW' WHEN N'Đang xử lý' THEN " +
            "'IN_PROGRESS' WHEN N'Chờ khách hàng' THEN 'WAITING_CUSTOMER' WHEN N'Đã xử lý' " + "THEN 'RESOLVED' ELSE 'CLOSED' END status," +
            "kn.MaNguoiTao creator_id,nvTao.HoTen creator_name,kn.MaNhanVienCSKH " + "assigned_id,nv.HoTen assigned_name,kn.HanXuLy due_at,kn.DaLeoThang escalated," +
            "kn.NgayGiaiQuyet resolved_at " + "FROM dbo.KhieuNai kn JOIN dbo.KhachHang kh ON kh.MaKhachHang=kn.MaKhachHang " +
            "LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien=kn.MaNhanVienCSKH LEFT JOIN " + "dbo.NhanVien nvTao ON nvTao.MaNhanVien=kn.MaNguoiTao ORDER BY kn.NgayGhiNhan " + "DESC,kn.MaKhieuNai DESC");
    }
    public List<KhieuNai> timKhieuNaiThucThe() throws SQLException {
        List<KhieuNai> ketQua = new ArrayList<KhieuNai>();
        try (Connection ketNoi = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> dong : timKhieuNai()) {
                KhieuNai khieuNai = timKhieuNaiThucThe(ketNoi, String.valueOf(dong.get("complaint_id")));
                if (khieuNai != null) ketQua.add(khieuNai);
            }
        }
        return ketQua;
    }
    public boolean maKhieuNaiTonTai(Connection c, String id) throws SQLException {
        Object v = motGiaTri(c, "SELECT COUNT(*) FROM dbo.KhieuNai WHERE MaKhieuNai=?", id);
        return v != null && ((Number) v).intValue() > 0;
    }
    public void themKhieuNai(Connection c, String id, Object customerId, String phone, String content, String priority, String employee) throws SQLException {
        String cid = customerId == null ? null : String.valueOf(customerId);
        if (cid == null || cid.isBlank()) {
            Map<String, Object> k = motDong(c, "SELECT TOP 1 MaKhachHang FROM dbo.KhachHang WHERE SoDienThoai=?", phone);
            if (k != null) cid = String.valueOf(k.get("MaKhachHang"));
        }
        if (cid == null || cid.isBlank()) throw new SQLException("Khiếu nại cần khách hàng đã tồn tại trong CSDL.");
        capNhat(c, "INSERT INTO dbo.KhieuNai(MaKhieuNai,NoiDung,NgayGhiNhan,TrangThaiXuLy," + "MucUuTien,MaNguoiTao,HanXuLy,DaLeoThang,MaKhachHang,MaNhanVienCSKH) VALUES(?,?," +
            "CAST(GETDATE() AS date),N'Chưa xử lý',?,?,DATEADD(HOUR,48,SYSDATETIME()),0,?," + "?)", id, content, priority, employee, cid, employee);
    }
    public List<Map<String, Object>> timKhieuNaiQuaHan(Connection c) throws SQLException {
        return truyVan(c, "SELECT MaKhieuNai complaint_id,kh.SoDienThoai customer_phone,NoiDung content " + "FROM dbo.KhieuNai kn JOIN dbo.KhachHang kh ON kh.MaKhachHang=kn.MaKhachHang " +
            "WHERE kn.TrangThaiXuLy NOT IN(N'Đã xử lý',N'Đóng') AND kn.DaLeoThang=0 AND " + "kn.HanXuLy<SYSDATETIME()");
    }
    public void danhDauKhieuNaiLeoThang(Connection c, String id) throws SQLException {
        capNhat(c, "UPDATE dbo.KhieuNai SET DaLeoThang=1 WHERE MaKhieuNai=?", id);
    }
    public KhieuNai timKhieuNaiThucThe(Connection c, String id) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT kn.*,kh.HoTen khTen,kh.SoDienThoai khPhone,kh.Email khEmail,kh.DiaChi " +
            "khDiaChi,kh.TrangThai khTrangThai,nv.HoTen nvTen,nv.SoDienThoai nvPhone," + "nv.Email nvEmail,nv.NgayVaoLam,nv.LoaiNhanVien,nv.TrangThai nvTrangThai FROM " +
            "dbo.KhieuNai kn JOIN dbo.KhachHang kh ON kh.MaKhachHang=kn.MaKhachHang JOIN " + "dbo.NhanVien nv ON nv.MaNhanVien=kn.MaNhanVienCSKH WHERE kn.MaKhieuNai=?", id);
        if (d == null) return null;
        KhachHang kh = new KhachHang(String.valueOf(d.get("MaKhachHang")), String.valueOf(d.get("khTen")), String.valueOf(d.get("khPhone")), d.get("khEmail") == null ? null : String.valueOf(d.get("khEmail")),
            d.get("khDiaChi") == null ? null : String.valueOf(d.get("khDiaChi")), 1, 0, String.valueOf(d.get( "khTrangThai")));
        NhanVien nv = new NhanVien(String.valueOf(d.get("MaNhanVienCSKH")), String.valueOf(d.get("nvTen")), String.valueOf(d.get("nvPhone")), d.get("nvEmail") == null ? null : String.valueOf(d.get("nvEmail")),
            String.valueOf(d.get("LoaiNhanVien")), ((Date) d.get("NgayVaoLam")).toLocalDate(), String.valueOf(d.get("nvTrangThai")));
        String st = mapKhieuNai(String.valueOf(d.get("TrangThaiXuLy")));
        KhieuNai k = new KhieuNai(String.valueOf(d.get("MaKhieuNai")), kh, String.valueOf(d.get("khPhone")), String.valueOf(d.get("NoiDung")), String.valueOf(d.get("MucUuTien")), st, nv, nv,
            d.get("HanXuLy") == null ? ((Date) d.get("NgayGhiNhan")).toLocalDate().plusDays(2).atStartOfDay() : (( Timestamp) d.get("HanXuLy")).toLocalDateTime(), Boolean.TRUE.equals(d.get("DaLeoThang")),
            ((Date) d.get("NgayGhiNhan")).toLocalDate().atStartOfDay());
        if (d.get("NgayGiaiQuyet") != null) k.setNgayGiaiQuyet(((Timestamp) d.get("NgayGiaiQuyet")).toLocalDateTime());
        return k;
    }
    public void capNhatTrangThaiKhieuNai(Connection c, String id, String st, String assigned) throws SQLException {
        String db = switch (st) {
            case "NEW" -> "Chưa xử lý";
            case "WAITING_CUSTOMER" -> "Chờ khách hàng";
            case "RESOLVED" -> "Đã xử lý";
            case "CLOSED" -> "Đóng";
            default -> "Đang xử lý";
        };
        capNhat(c, "UPDATE dbo.KhieuNai SET TrangThaiXuLy=?,MaNhanVienCSKH=COALESCE(?," + "MaNhanVienCSKH),NgayGiaiQuyet=CASE WHEN ? IN ('RESOLVED','CLOSED') THEN " +
            "COALESCE(NgayGiaiQuyet,SYSDATETIME()) ELSE NgayGiaiQuyet END WHERE " + "MaKhieuNai=?", db, assigned, st, id);
    }
    private static LocalDateTime toLocalDateTime(Object giaTri) {
        if (giaTri == null) return null;
        if (giaTri instanceof Timestamp) return ((Timestamp) giaTri).toLocalDateTime();
        if (giaTri instanceof Date) return ((Date) giaTri).toLocalDate().atStartOfDay();
        return null;
    }
    private static String mapKhieuNai(String s) {
        if ("Chưa xử lý".equals(s)) return "NEW";
        if ("Chờ khách hàng".equals(s)) return "WAITING_CUSTOMER";
        if ("Đã xử lý".equals(s)) return "RESOLVED";
        if ("Đóng".equals(s)) return "CLOSED";
        return "IN_PROGRESS";
    }
}

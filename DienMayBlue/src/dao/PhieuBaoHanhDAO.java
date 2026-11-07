package dao;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import entity.*;
import java.sql.Date;
public class PhieuBaoHanhDAO extends CoSoDAO {
    public String goiYMaBaoHanh() throws SQLException {
        return goiYMaTiepTheo("PhieuBaoHanh", "MaPhieu", "BH", 5);
    }
    public List<Map<String, Object>> timBaoHanh() throws SQLException {
        return truyVan("SELECT bh.MaPhieu warranty_id,bh.MaKhachHang customer_id,kh.HoTen full_name," + "kh.SoDienThoai phone,sp.MaSanPham product_id,sp.TenSanPham product_name," +
            "CAST(bh.NgayTiepNhan AS datetime2) received_at,bh.HanBaoHanh warranty_end_date," + "COALESCE(bh.MoTaLoi,bh.TinhTrang) issue_description,bh.MaNhanVienCSKH " +
            "employee_id,nv.HoTen employee_name," + "CASE WHEN bh.NgayTraKhach IS NOT NULL THEN 'RETURNED' WHEN bh.NgayHoanThanh IS " +
            "NOT NULL THEN N'Hoàn tất' WHEN bh.TinhTrang=N'Đổi máy' THEN 'REPLACEMENT' WHEN " + "bh.TinhTrang=N'Từ chối bảo hành' THEN 'REJECTED' ELSE 'REPAIRING' END status," +
            "ISNULL(bh.PhiDoiMay,0) replacement_charge,bh.NgayHoanThanh completed_at,bh.NgayTraKhach returned_at " + "FROM dbo.PhieuBaoHanh bh JOIN dbo.KhachHang kh ON " +
            "kh.MaKhachHang=bh.MaKhachHang JOIN dbo.ChiTietPhieuBaoHanh ctb ON " + "ctb.MaPhieuBaoHanh=bh.MaPhieu JOIN dbo.SanPham sp ON " +
            "sp.MaSanPham=ctb.MaSanPham LEFT JOIN dbo.NhanVien nv ON " + "nv.MaNhanVien=bh.MaNhanVienCSKH ORDER BY bh.NgayTiepNhan DESC");
    }
    public List<PhieuBaoHanh> timBaoHanhThucThe() throws SQLException {
        List<PhieuBaoHanh> ketQua = new ArrayList<PhieuBaoHanh>();
        try (Connection ketNoi = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> dong : timBaoHanh()) {
                PhieuBaoHanh phieu = timPhieuBaoHanhThucThe(ketNoi, String.valueOf(dong.get("warranty_id")));
                if (phieu != null) ketQua.add(phieu);
            }
        }
        return ketQua;
    }
    public List<Map<String, Object>> timLuaChonBaoHanh(String soDienThoai) throws SQLException {
        return truyVan("SELECT kh.MaKhachHang customer_id,kh.HoTen full_name,hd.MaHoaDon invoice_id,ct.MaSanPham product_id,sp.TenSanPham product_name,hd.NgayLap created_at," + "sp.SoThangBaoHanh warranty_months,DATEADD(MONTH,sp.SoThangBaoHanh," +
            "CAST(hd.NgayLap AS date)) warranty_end_date,ct.SoLuong-ISNULL((SELECT SUM(ctdt.SoLuongTra) FROM dbo.PhieuDoiTraHoanTien dt JOIN dbo.ChiTietPhieuDoiTra ctdt ON ctdt.MaPhieu=dt.MaPhieu WHERE dt.MaHoaDon=hd.MaHoaDon AND " +
            "ctdt.MaSanPham=ct.MaSanPham AND dt.TrangThai=N'Đã duyệt'),0) remaining_qty " + "FROM dbo.KhachHang kh JOIN dbo.HoaDon hd ON hd.MaKhachHang=kh.MaKhachHang JOIN " +
            "dbo.ChiTietHoaDon ct ON ct.MaHoaDon=hd.MaHoaDon JOIN dbo.SanPham sp ON " + "sp.MaSanPham=ct.MaSanPham WHERE kh.SoDienThoai=? AND hd.TrangThai=N'Đã thanh " +
            "toán' AND DATEADD(MONTH,sp.SoThangBaoHanh,CAST(hd.NgayLap AS " + "date))>=CAST(GETDATE() AS date) ORDER BY hd.NgayLap DESC", soDienThoai);
    }
    public Map<String, Object> timGiaoDichBaoHanh(Connection c, String maHoaDon, String maSanPham) throws SQLException {
        return motDong(c, "SELECT hd.MaKhachHang customer_id,hd.MaHoaDon invoice_id,hd.NgayLap created_at,ct.MaSanPham product_id,sp.TenSanPham product_name,sp.MaVach " +
            "barcode,sp.GiaVon cost_price,sp.GiaBan sale_price,sp.SoLuongTonKho stock_quantity,sp.HinhAnh image_path,sp.SoThangBaoHanh warranty_months," +
            "ct.SoLuong quantity,ct.DonGia unit_price,DATEADD(MONTH,sp.SoThangBaoHanh,CAST(hd.NgayLap AS date)) warranty_end_date,N'Đã thanh toán' status," +
            "ct.SoLuong-ISNULL((SELECT SUM(ctdt.SoLuongTra) FROM dbo.PhieuDoiTraHoanTien dt JOIN dbo.ChiTietPhieuDoiTra ctdt ON ctdt.MaPhieu=dt.MaPhieu " +
            "WHERE dt.MaHoaDon=hd.MaHoaDon AND ctdt.MaSanPham=ct.MaSanPham AND dt.TrangThai=N'Đã duyệt'),0) remaining_qty " +
            "FROM dbo.HoaDon hd JOIN dbo.ChiTietHoaDon ct ON ct.MaHoaDon=hd.MaHoaDon JOIN dbo.SanPham sp ON sp.MaSanPham=ct.MaSanPham " +
            "WHERE hd.MaHoaDon=? AND ct.MaSanPham=? AND hd.TrangThai=N'Đã thanh toán'", maHoaDon, maSanPham);
    }
    public int demBaoHanhDangHoatDong(Connection c, String maKhachHang, String maSanPham) throws SQLException {
        Object v = motGiaTri(c, "SELECT COUNT(*) FROM dbo.PhieuBaoHanh bh JOIN dbo.ChiTietPhieuBaoHanh ctb ON ctb.MaPhieuBaoHanh=bh.MaPhieu " +
            "WHERE bh.MaKhachHang=? AND ctb.MaSanPham=? AND bh.NgayHoanThanh IS NULL", maKhachHang, maSanPham);
        return ((Number) v).intValue();
    }
    public boolean maBaoHanhTonTai(Connection c, String id) throws SQLException {
        Object v = motGiaTri(c, "SELECT COUNT(*) FROM dbo.PhieuBaoHanh WHERE MaPhieu=?", id);
        return v != null && ((Number) v).intValue() > 0;
    }
    public void themBaoHanh(Connection c, String maBaoHanh, String maKhachHang, String maSanPham, String issue, LocalDate warrantyEnd, String employeeId) throws SQLException {
        capNhat(c, "INSERT INTO dbo.PhieuBaoHanh(MaPhieu,NgayTiepNhan,TinhTrang,MoTaLoi,HanBaoHanh,PhiDoiMay,NgayHoanThanh,NgayTraKhach,MaKhachHang,MaNhanVienCSKH) VALUES(?," +
            "CAST(GETDATE() AS date),N'Đang sửa chữa',?,?,NULL,NULL,NULL,?,?)", maBaoHanh, issue, warrantyEnd, maKhachHang, employeeId);
        capNhat(c, "INSERT INTO dbo.ChiTietPhieuBaoHanh(MaSanPham,MaPhieuBaoHanh) VALUES(?,?)", maSanPham, maBaoHanh);
    }
    public PhieuBaoHanh timPhieuBaoHanhThucThe(Connection c, String id) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT bh.MaPhieu,bh.NgayTiepNhan,bh.TinhTrang,bh.MoTaLoi,bh.HanBaoHanh," +
            "bh.PhiDoiMay,bh.NgayHoanThanh,bh.NgayTraKhach,kh.MaKhachHang,kh.HoTen," + "kh.SoDienThoai,kh.Email,kh.DiaChi,kh.TrangThai khTrangThai,sp.MaSanPham," +
            "sp.TenSanPham,sp.MaVach,sp.MaDanhMuc,sp.GiaVon,sp.GiaBan,sp.SoLuongTonKho," + "sp.SoThangBaoHanh,sp.HinhAnh,sp.TrangThai,sp.MaNhaCungCap,ncc.TenNhaCungCap," +
            "ncc.SoDienThoai nccPhone,ncc.DiaChi nccDiaChi,ncc.MaSoThue,ncc.CongNo," + "ncc.TrangThai nccTrangThai,nv.MaNhanVien,nv.HoTen nvTen,nv.SoDienThoai nvPhone," +
            "nv.Email nvEmail,nv.NgayVaoLam,nv.LoaiNhanVien,nv.TrangThai nvTrangThai FROM " + "dbo.PhieuBaoHanh bh JOIN dbo.KhachHang kh ON kh.MaKhachHang=bh.MaKhachHang " +
            "JOIN dbo.ChiTietPhieuBaoHanh ctb ON ctb.MaPhieuBaoHanh=bh.MaPhieu JOIN " + "dbo.SanPham sp ON sp.MaSanPham=ctb.MaSanPham JOIN dbo.NhaCungCap ncc ON " +
            "ncc.MaNhaCungCap=sp.MaNhaCungCap JOIN dbo.NhanVien nv ON " + "nv.MaNhanVien=bh.MaNhanVienCSKH WHERE bh.MaPhieu=?", id);
        if (d == null) return null;
        KhachHang kh = new KhachHang(String.valueOf(d.get("MaKhachHang")), String.valueOf(d.get("HoTen")), String.valueOf(d.get("SoDienThoai")), d.get("Email") == null ? null : String.valueOf(d.get("Email")),
            d.get("DiaChi") == null ? null : String.valueOf(d.get("DiaChi")), 1, 0, String.valueOf(d.get( "khTrangThai")));
        NhaCungCap ncc = new NhaCungCap(String.valueOf(d.get("MaNhaCungCap")), String.valueOf(d.get("TenNhaCungCap")), String.valueOf(d.get(
            "nccPhone")), d.get("nccDiaChi") == null ? null : String.valueOf(d.get("nccDiaChi")), d.get("MaSoThue") == null ? null : String.valueOf(d.get("MaSoThue")), String.valueOf(d.get(
            "nccTrangThai")), d.get("CongNo") == null ? BigDecimal.ZERO : (BigDecimal) d.get("CongNo"));
        SanPham sp = new SanPham(String.valueOf(d.get("MaSanPham")), d.get("MaVach") == null ? null : String.valueOf( d.get("MaVach")), String.valueOf(d.get("TenSanPham")), ((Number) d.get("MaDanhMuc")).intValue(),
            ncc, (BigDecimal) d.get("GiaVon"), (BigDecimal) d.get("GiaBan"), ((Number) d.get("SoLuongTonKho")).intValue(), ((Number) d.get("SoThangBaoHanh")).intValue(), d.get("HinhAnh") == null ? null : String.valueOf(d.get(
            "HinhAnh")), String.valueOf(d.get("TrangThai")));
        NhanVien nv = new NhanVien(String.valueOf(d.get("MaNhanVien")), String.valueOf(d.get("nvTen")), String.valueOf(d.get("nvPhone")), d.get("nvEmail") == null ? null : String.valueOf(d.get("nvEmail")),
            String.valueOf(d.get("LoaiNhanVien")), ((Date) d.get("NgayVaoLam")).toLocalDate(), String.valueOf(d.get("nvTrangThai")));
        String tt = String.valueOf(d.get("TinhTrang"));
        String st = d.get("NgayTraKhach") != null ? "RETURNED" : d.get("NgayHoanThanh") != null ? "Hoàn tất" : "Đổi máy".equals(tt) ? "REPLACEMENT" : "Từ chối bảo hành".equals(tt) ? "REJECTED" : "REPAIRING";
        PhieuBaoHanh p = new PhieuBaoHanh(String.valueOf(d.get("MaPhieu")), kh, sp, d.get("MoTaLoi") == null ? tt : String.valueOf(d.get("MoTaLoi")),
            d.get("HanBaoHanh") == null ? ((Date) d.get("NgayTiepNhan")).toLocalDate().plusMonths(sp.getSoThangBaoHanh()) : (( Date) d.get("HanBaoHanh")).toLocalDate(), st, d.get("PhiDoiMay") == null ? BigDecimal.ZERO : (BigDecimal)
            d.get("PhiDoiMay"), nv, ((Date) d.get("NgayTiepNhan")).toLocalDate().atStartOfDay());
        if (d.get("NgayHoanThanh") != null) p.setNgayHoanThanh(((Timestamp) d.get("NgayHoanThanh")).toLocalDateTime());
        if (d.get("NgayTraKhach") != null) p.setNgayTraKhach(((Timestamp) d.get("NgayTraKhach")).toLocalDateTime());
        return p;
    }
    public Map<String, Object> timDuLieuPhiBaoHanh(String id) throws SQLException {
        return motDong("SELECT TOP 1 ct.DonGia unit_price,hd.NgayLap created_at,CASE WHEN " + "bh.NgayTraKhach IS NOT NULL THEN 'RETURNED' WHEN bh.NgayHoanThanh IS NOT NULL " +
            "THEN N'Hoàn tất' WHEN bh.TinhTrang=N'Đổi máy' THEN 'REPLACEMENT' WHEN " + "bh.TinhTrang=N'Từ chối bảo hành' THEN 'REJECTED' ELSE 'REPAIRING' END status " +
            "FROM dbo.PhieuBaoHanh bh JOIN dbo.ChiTietPhieuBaoHanh ctb ON " + "ctb.MaPhieuBaoHanh=bh.MaPhieu JOIN dbo.ChiTietHoaDon ct ON " +
            "ct.MaSanPham=ctb.MaSanPham JOIN dbo.HoaDon hd ON hd.MaHoaDon=ct.MaHoaDon AND " + "hd.MaKhachHang=bh.MaKhachHang WHERE bh.MaPhieu=? ORDER BY hd.NgayLap DESC", id);
    }
    public Map<String, Object> timDuLieuPhiBaoHanh(Connection c, String id) throws SQLException {
        return motDong(c, "SELECT TOP 1 ct.DonGia unit_price,hd.NgayLap created_at FROM dbo.PhieuBaoHanh " + "bh JOIN dbo.ChiTietPhieuBaoHanh ctb ON ctb.MaPhieuBaoHanh=bh.MaPhieu JOIN " +
            "dbo.ChiTietHoaDon ct ON ct.MaSanPham=ctb.MaSanPham JOIN dbo.HoaDon hd ON " + "hd.MaHoaDon=ct.MaHoaDon AND hd.MaKhachHang=bh.MaKhachHang WHERE bh.MaPhieu=? " + "ORDER BY hd.NgayLap DESC", id);
    }
    public void capNhatTrangThaiBaoHanh(Connection c, String id, String st, BigDecimal charge) throws SQLException {
        String tt = switch (st) {
            case "REPLACEMENT" -> "Đổi máy";
            case "REJECTED" -> "Từ chối bảo hành";
            case "Hoàn tất" -> "Hoàn tất sửa chữa";
            case "RETURNED" -> "Đã trả khách";
            default -> "Đang sửa chữa";
        };
        capNhat(c, "UPDATE dbo.PhieuBaoHanh SET TinhTrang=?,PhiDoiMay=CASE WHEN ?='REPLACEMENT' " + "THEN ? ELSE PhiDoiMay END,NgayHoanThanh=CASE WHEN ?=N'Hoàn tất' THEN " +
            "SYSDATETIME() ELSE NgayHoanThanh END,NgayTraKhach=CASE WHEN ?='RETURNED' THEN " + "SYSDATETIME() ELSE NgayTraKhach END WHERE MaPhieu=?", tt, st, charge, st, st, id);
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
    private static int parseIntDigits(String s) {
        try {
            String d = s == null ? "" : s.replaceAll("\\D", "");
            return d.isEmpty() ? 1 : Integer.parseInt(d);
        } catch (Exception e) {
            return 1;
        }
    }
}

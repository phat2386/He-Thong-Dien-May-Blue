package dao;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import entity.*;
import java.sql.Date;
public class PhieuDoiTraHoanTienDAO extends CoSoDAO {
    public String goiYMaPhieuDoiTra() throws SQLException {
        return goiYMaTiepTheo("PhieuDoiTraHoanTien", "MaPhieu", "HT", 5);
    }
    private static String mapHoaDon(String giaTri) {
        return "Đã thanh toán".equals(giaTri) ? "Đã thanh toán" : "Đã hủy";
    }
    private static String mapTra(String giaTri) {
        if ("Chờ duyệt".equals(giaTri)) return "WAITING_APPROVAL";
        if ("Từ chối".equals(giaTri)) return "REJECTED";
        return "APPROVED";
    }
    public Map<String, Object> timTrangThaiThaoTacTaiKhoan(Connection c, String maTaiKhoan) throws SQLException {
        return motDong(c, "SELECT CASE tk.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Bị khóa' END account_status," +
            "CASE nv.TrangThai WHEN N'Hoạt động' THEN N'Hoạt động' ELSE N'Không hoạt động' END employee_status," +
            "CASE WHEN tk.TenDangNhap='admin' THEN 'ADMIN' WHEN nv.LoaiNhanVien=N'Quản lý' THEN 'MANAGER' " +
            "WHEN nv.LoaiNhanVien=N'Bán hàng' THEN 'SALES' WHEN nv.LoaiNhanVien=N'CSKH' THEN 'CUSTOMER_SERVICE' ELSE 'UNKNOWN' END role_code " +
            "FROM dbo.TaiKhoan tk JOIN dbo.NhanVien nv ON nv.MaNhanVien=tk.MaNhanVien " +
            "WHERE tk.MaTaiKhoan=?", maTaiKhoan);
    }
    public boolean coCaHienTai(Connection c, String maNhanVien) throws SQLException {
        Object v = motGiaTri(c, "SELECT COUNT(*) FROM dbo.CaLamViec WHERE MaNhanVien=? AND " +
            "NgayLamViec=CAST(GETDATE() AS date) AND TrangThai=N'Đã phân công' AND " +
            "CAST(GETDATE() AS time)>=GioBatDau AND CAST(GETDATE() AS time)<GioKetThuc", maNhanVien);
        return ((Number) v).intValue() > 0;
    }
    public boolean phieuDoiTraTonTaiChoHoaDon(Connection c, String maHoaDon) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.PhieuDoiTraHoanTien WHERE MaHoaDon=?", maHoaDon)).intValue() > 0;
    }
    public boolean maPhieuDoiTraTonTai(Connection c, String maPhieu) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.PhieuDoiTraHoanTien WHERE MaPhieu=?", maPhieu)).intValue() > 0;
    }
    public void themPhieuDoiTra(Connection c, String maPhieu, String maHoaDon, String loaiLyDo, String lyDo, String tinhTrang, BigDecimal giaTriHoan, String trangThai, boolean conTem, boolean dayDuHop, boolean duPhuKien) throws SQLException {
        Map<String, Object> hd = motDong(c, "SELECT MaKhachHang,MaNhanVienBanHang FROM dbo.HoaDon WHERE MaHoaDon=?", maHoaDon);
        if (hd == null) throw new SQLException("Không tìm thấy hóa đơn.");
        String db = "WAITING_APPROVAL".equals(trangThai) ? "Chờ duyệt" : "Đã duyệt";
        capNhat(c, "INSERT INTO dbo.PhieuDoiTraHoanTien(MaPhieu,NgayLap,LoaiLyDo,LyDo,TinhTrangVatLy,ConTemMac,DayDuHop,DuPhuKien,GiaTriHoan,CanDuyet,TrangThai," +
            "MaNhanVienQuanLy,MaNhanVienBanHang,MaHoaDon,MaKhachHang) VALUES(?,CAST(GETDATE() AS date),?,?,?,?,?,?,?,?,?,NULL,?,?,?)",
            maPhieu, loaiLyDo, lyDo, tinhTrang, conTem, dayDuHop, duPhuKien, giaTriHoan, "WAITING_APPROVAL".equals(trangThai), db, hd.get("MaNhanVienBanHang"), maHoaDon, hd.get("MaKhachHang"));
    }
    public void themChiTietDoiTra(Connection c, String maPhieu, String maSanPham, int soLuongTra, BigDecimal giaTriHoan) throws SQLException {
        capNhat(c, "INSERT INTO dbo.ChiTietPhieuDoiTra(MaPhieu,MaSanPham,SoLuongTra,GiaTriHoan) VALUES(?,?,?,?)", maPhieu, maSanPham, soLuongTra, giaTriHoan);
    }
    public void themYeuCauDuyetHoanTien(Connection c, String maPhieu) throws SQLException {
        capNhat(c, "UPDATE dbo.PhieuDoiTraHoanTien SET TrangThai=N'Chờ duyệt' WHERE MaPhieu=?", maPhieu);
    }
    public void hoanTonKhoSanPham(Connection c, String maSanPham, int soLuong) throws SQLException {
        capNhat(c, "UPDATE dbo.SanPham SET SoLuongTonKho=SoLuongTonKho+? WHERE MaSanPham=?", soLuong, maSanPham);
    }
    public int diemHienTaiKhachHang(Connection c, Object maKhachHang) throws SQLException {
        Object v = motGiaTri(c, "SELECT ISNULL(DiemTichLuy,0) FROM dbo.TheThanhVien WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", maKhachHang);
        return v == null ? 0 : ((Number) v).intValue();
    }
    public void truDiemKhachHang(Connection c, Object maKhachHang, int diem) throws SQLException {
        capNhat(c, "UPDATE dbo.TheThanhVien SET DiemTichLuy=CASE WHEN DiemTichLuy>=? THEN DiemTichLuy-? ELSE 0 END WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", diem, diem, maKhachHang);
    }
    public void capNhatHangKhachHang(Connection c, Object maKhachHang, String hang) throws SQLException {
        capNhat(c, "UPDATE dbo.TheThanhVien SET HangThanhVien=? WHERE MaKhachHang=? AND TrangThai=N'Hoạt động'", hang, maKhachHang);
    }
    public BigDecimal timTyLeThuong(Connection c, String maHoaDon, Object maNhanVien, BigDecimal macDinh) throws SQLException {
        Object v = motGiaTri(c, "SELECT CASE WHEN SUM(ct.ThanhTien)=0 THEN ? ELSE SUM(ct.ThanhTien*sp.TyLeThuong)/SUM(ct.ThanhTien) END " +
            "FROM dbo.ChiTietHoaDon ct JOIN dbo.SanPham sp ON sp.MaSanPham=ct.MaSanPham WHERE ct.MaHoaDon=?", macDinh, maHoaDon);
        return v == null ? macDinh : new BigDecimal(String.valueOf(v));
    }
    public void moLaiThuongHoaDon(Connection c, String maHoaDon) throws SQLException {
        capNhat(c, "UPDATE dbo.HoaDon SET DaChotThuong=0,NgayChotThuong=NULL WHERE MaHoaDon=?", maHoaDon);
    }
    public void danhDauDoiTraHoanTat(Connection c, String maPhieu) throws SQLException {
        capNhat(c, "UPDATE dbo.PhieuDoiTraHoanTien SET TrangThai=N'Đã duyệt',NgayHoanTat=SYSDATETIME() WHERE MaPhieu=?", maPhieu);
    }
    public int quyetDinhDuyetHoanTien(Connection c, String maPhieu, String quyetDinh, String maQuanLy, Object lyDo) throws SQLException {
        return capNhat(c, "UPDATE dbo.PhieuDoiTraHoanTien SET TrangThai=?,MaNhanVienQuanLy=?,LyDoTuChoi=CASE WHEN ?='REJECTED' THEN ? ELSE NULL END " +
            "WHERE MaPhieu=? AND TrangThai=N'Chờ duyệt'", "APPROVED".equals(quyetDinh) ? "Đã duyệt" : "Từ chối", maQuanLy, quyetDinh, lyDo, maPhieu);
    }
    public void danhDauDoiTraDaDuyet(Connection c, String maPhieu) throws SQLException {
        capNhat(c, "UPDATE dbo.PhieuDoiTraHoanTien SET TrangThai=N'Đã duyệt' WHERE MaPhieu=?", maPhieu);
    }
    public void danhDauDoiTraTuChoi(Connection c, String maPhieu, String lyDo) throws SQLException {
        capNhat(c, "UPDATE dbo.PhieuDoiTraHoanTien SET TrangThai=N'Từ chối',LyDoTuChoi=? WHERE MaPhieu=?", lyDo, maPhieu);
    }
    public List<Map<String, Object>> timHoaDonCoTheDoiTra(String tuKhoa) throws SQLException {
        String pattern = "%" + (tuKhoa == null ? "" : tuKhoa.trim()) + "%";
        return truyVan("SELECT hd.MaHoaDon invoice_id FROM dbo.HoaDon hd JOIN dbo.KhachHang kh ON kh.MaKhachHang=hd.MaKhachHang " +
            "WHERE hd.TrangThai=N'Đã thanh toán' AND NOT EXISTS(SELECT 1 FROM dbo.PhieuDoiTraHoanTien dt WHERE dt.MaHoaDon=hd.MaHoaDon) " +
            "AND (hd.MaHoaDon LIKE ? OR kh.MaKhachHang LIKE ? OR kh.HoTen LIKE ? OR kh.SoDienThoai LIKE ?) ORDER BY hd.MaHoaDon", pattern, pattern, pattern, pattern);
    }
    public List<HoaDon> timHoaDonCoTheDoiTraThucThe(String tuKhoa) throws SQLException {
        List<HoaDon> ketQua = new ArrayList<>();
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : timHoaDonCoTheDoiTra(tuKhoa)) {
                HoaDon hd = timThucTheHoaDon(c, String.valueOf(d.get("invoice_id")));
                if (hd != null) {
                    ketQua.add(hd);
                }
            }
        }
        return ketQua;
    }
    public List<ChiTietHoaDon> timSanPhamTrongHoaDonThucThe(String maHoaDon) throws SQLException {
        List<ChiTietHoaDon> ketQua = new ArrayList<>();
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : truyVan(c, "SELECT MaSanPham FROM dbo.ChiTietHoaDon WHERE MaHoaDon=? ORDER BY MaSanPham", maHoaDon)) {
                ChiTietHoaDon ct = timThucTheChiTietHoaDon(c, maHoaDon, String.valueOf(d.get("MaSanPham")));
                if (ct != null) ketQua.add(ct);
            }
        }
        return ketQua;
    }
    public ChiTietHoaDon timChiTietHoaDonDeHoanTienThucThe(String maHoaDon, String maSanPham) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            return timThucTheChiTietHoaDon(c, maHoaDon, maSanPham);
        }
    }
    public ChiTietHoaDon timThucTheChiTietHoaDon(Connection c, String maHoaDon, String maSanPham) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT * FROM dbo.ChiTietHoaDon WHERE MaHoaDon=? AND MaSanPham=?", maHoaDon, maSanPham);
        if (d == null) return null;
        HoaDon hd = timThucTheHoaDon(c, maHoaDon);
        SanPham sp = timSanPhamThucThe(c, maSanPham);
        return new ChiTietHoaDon(hd, sp, ((Number) d.get("SoLuong")).intValue(), (BigDecimal) d.get("DonGia"),
            (BigDecimal) d.get("GiaVonLucBan"), (BigDecimal) d.get("TienGiam"), (BigDecimal) d.get("ThanhTien"));
    }
    public List<ChiTietHoaDon> timChiTietPhieuDoiTraThucThe(Connection c, String maPhieu) throws SQLException {
        List<ChiTietHoaDon> ketQua = new ArrayList<>();
        Map<String, Object> phieu = motDong(c, "SELECT MaHoaDon FROM dbo.PhieuDoiTraHoanTien WHERE MaPhieu=?", maPhieu);
        if (phieu == null) return ketQua;
        String maHoaDon = String.valueOf(phieu.get("MaHoaDon"));
        for (Map<String, Object> d : truyVan(c, "SELECT MaSanPham,SoLuongTra,GiaTriHoan FROM dbo.ChiTietPhieuDoiTra WHERE MaPhieu=? ORDER BY MaSanPham", maPhieu)) {
            String maSanPham = String.valueOf(d.get("MaSanPham"));
            ChiTietHoaDon goc = timThucTheChiTietHoaDon(c, maHoaDon, maSanPham);
            if (goc != null) ketQua.add(new ChiTietHoaDon(goc.getHoaDon(), goc.getSanPham(), ((Number) d.get("SoLuongTra")).intValue(),
                goc.getDonGia(), goc.getGiaVonTaiThoiDiemBan(), goc.getTienGiam(), (BigDecimal) d.get("GiaTriHoan")));
        }
        return ketQua;
    }
    public List<Map<String, Object>> timDanhSachDuyetHoanTien(String quyetDinh) throws SQLException {
        String sql = "SELECT MaPhieu return_id FROM dbo.PhieuDoiTraHoanTien WHERE CanDuyet=1";
        if (quyetDinh != null && !quyetDinh.isBlank()) {
            sql += " AND TrangThai=" + ("WAITING_APPROVAL".equals(quyetDinh) ? "N'Chờ duyệt'" : "APPROVED".equals(quyetDinh) ? "N'Đã duyệt'" : "N'Từ chối'");
        }
        return truyVan(sql + " ORDER BY MaPhieu");
    }
    public List<PhieuDoiTraHoanTien> timDanhSachDuyetHoanTienThucThe(String quyetDinh) throws SQLException {
        List<PhieuDoiTraHoanTien> ketQua = new ArrayList<>();
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : timDanhSachDuyetHoanTien(quyetDinh)) {
                PhieuDoiTraHoanTien p = timThucThePhieuDoiTra(c, String.valueOf(d.get("return_id")));
                if (p != null) {
                    ketQua.add(p);
                }
            }
        }
        return ketQua;
    }
    public PhieuDoiTraHoanTien timThucThePhieuDoiTra(Connection c, String maPhieu) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT * FROM dbo.PhieuDoiTraHoanTien WHERE MaPhieu=?", maPhieu);
        if (d == null) return null;
        HoaDon hd = timThucTheHoaDon(c, String.valueOf(d.get("MaHoaDon")));
        NhanVien banHang = timNhanVien(c, d.get("MaNhanVienBanHang"));
        NhanVien quanLy = timNhanVien(c, d.get("MaNhanVienQuanLy"));
        String trangThai = d.get("NgayHoanTat") != null ? "Hoàn tất" : mapTra(String.valueOf(d.get("TrangThai")));
        PhieuDoiTraHoanTien p = new PhieuDoiTraHoanTien(String.valueOf(d.get("MaPhieu")), hd, banHang,
            ((Date) d.get("NgayLap")).toLocalDate().atStartOfDay(), String.valueOf(d.get("LyDo")),
            d.get("TinhTrangVatLy") == null ? null : String.valueOf(d.get("TinhTrangVatLy")), (BigDecimal) d.get("GiaTriHoan"), Boolean.TRUE.equals(d.get("CanDuyet")), trangThai);
        p.setNhanVienQuanLy(quanLy);
        p.setLoaiLyDo(d.get("LoaiLyDo") == null ? null : String.valueOf(d.get("LoaiLyDo")));
        for (Map<String, Object> ct : truyVan(c, "SELECT MaSanPham,SoLuongTra,GiaTriHoan FROM dbo.ChiTietPhieuDoiTra WHERE MaPhieu=? ORDER BY MaSanPham", maPhieu)) {
            SanPham sp = timSanPhamThucThe(c, String.valueOf(ct.get("MaSanPham")));
            if (sp != null) p.themChiTiet(new ChiTietPhieuDoiTra(p, sp, ((Number) ct.get("SoLuongTra")).intValue(), (BigDecimal) ct.get("GiaTriHoan")));
        }
        p.setConTemMac(Boolean.TRUE.equals(d.get("ConTemMac")));
        p.setDayDuHop(Boolean.TRUE.equals(d.get("DayDuHop")));
        p.setDuPhuKien(Boolean.TRUE.equals(d.get("DuPhuKien")));
        p.setLyDoTuChoi(d.get("LyDoTuChoi") == null ? null : String.valueOf(d.get("LyDoTuChoi")));
        if (d.get("NgayHoanTat") != null) {
            p.setNgayHoanTat(((Timestamp) d.get("NgayHoanTat")).toLocalDateTime());
        }
        return p;
    }
    public HoaDon timThucTheHoaDon(Connection c, String maHoaDon) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT hd.*,kh.HoTen khTen,kh.SoDienThoai khPhone,kh.Email khEmail,kh.DiaChi khAddress,kh.TrangThai khTrangThai," +
            "tv.HangThanhVien,tv.DiemTichLuy,nv.HoTen nvTen,nv.SoDienThoai nvPhone,nv.Email nvEmail,nv.NgayVaoLam,nv.TrinhDo,nv.HeSoLuong,nv.LuongCoBan,nv.LoaiNhanVien,nv.TrangThai nvTrangThai " +
            "FROM dbo.HoaDon hd JOIN dbo.KhachHang kh ON kh.MaKhachHang=hd.MaKhachHang " +
            "LEFT JOIN dbo.TheThanhVien tv ON tv.MaKhachHang=kh.MaKhachHang AND tv.TrangThai=N'Hoạt động' " +
            "JOIN dbo.NhanVien nv ON nv.MaNhanVien=hd.MaNhanVienBanHang WHERE hd.MaHoaDon=?", maHoaDon);
        if (d == null) return null;
        KhachHang kh = new KhachHang(String.valueOf(d.get("MaKhachHang")), String.valueOf(d.get("khTen")), String.valueOf(d.get("khPhone")),
            d.get("khEmail") == null ? null : String.valueOf(d.get("khEmail")), d.get("khAddress") == null ? null : String.valueOf(d.get("khAddress")),
            hangId(d.get("HangThanhVien")), d.get("DiemTichLuy") == null ? 0 : ((Number) d.get("DiemTichLuy")).intValue(), String.valueOf(d.get("khTrangThai")));
        NhanVien nv = taoNhanVien(d, "MaNhanVienBanHang", "nvTen", "nvPhone", "nvEmail");
        HoaDon hd = new HoaDon(String.valueOf(d.get("MaHoaDon")), kh, nv, ((Timestamp) d.get("NgayLap")).toLocalDateTime(),
            (BigDecimal) d.get("TamTinh"), (BigDecimal) d.get("TienGiam"), (BigDecimal) d.get("TongTien"), mapHoaDon(String.valueOf(d.get("TrangThai"))),
            d.get("GhiChu") == null ? null : String.valueOf(d.get("GhiChu")));
        hd.setPhuongThucThanhToan(d.get("PhuongThucThanhToan") == null ? null : String.valueOf(d.get("PhuongThucThanhToan")));
        hd.setMaThamChieuThanhToan(d.get("MaThamChieuThanhToan") == null ? null : String.valueOf(d.get("MaThamChieuThanhToan")));
        return hd;
    }
    private NhanVien timNhanVien(Connection c, Object maNhanVien) throws SQLException {
        if (maNhanVien == null) return null;
        Map<String, Object> d = motDong(c, "SELECT MaNhanVien,HoTen nvTen,SoDienThoai nvPhone,Email nvEmail,NgayVaoLam,TrinhDo,HeSoLuong,LuongCoBan,LoaiNhanVien,TrangThai nvTrangThai " +
            "FROM dbo.NhanVien WHERE MaNhanVien=?", maNhanVien);
        return taoNhanVien(d, "MaNhanVien", "nvTen", "nvPhone", "nvEmail");
    }
    private SanPham timSanPhamThucThe(Connection c, String maSanPham) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT sp.*,ncc.TenNhaCungCap,ncc.SoDienThoai nccPhone,ncc.DiaChi nccDiaChi,ncc.MaSoThue,ncc.CongNo,ncc.TrangThai nccTrangThai " +
            "FROM dbo.SanPham sp JOIN dbo.NhaCungCap ncc ON ncc.MaNhaCungCap=sp.MaNhaCungCap WHERE sp.MaSanPham=?", maSanPham);
        if (d == null) return null;
        NhaCungCap ncc = new NhaCungCap(String.valueOf(d.get("MaNhaCungCap")), String.valueOf(d.get("TenNhaCungCap")),
            String.valueOf(d.get("nccPhone")), d.get("nccDiaChi") == null ? null : String.valueOf(d.get("nccDiaChi")), d.get("MaSoThue") == null ? null : String.valueOf(d.get("MaSoThue")),
            String.valueOf(d.get("nccTrangThai")), d.get("CongNo") == null ? BigDecimal.ZERO : (BigDecimal) d.get("CongNo"));
        return new SanPham(String.valueOf(d.get("MaSanPham")), d.get("MaVach") == null ? null : String.valueOf(d.get("MaVach")), String.valueOf(d.get("TenSanPham")),
            ((Number) d.get("MaDanhMuc")).intValue(), ncc, (BigDecimal) d.get("GiaVon"), (BigDecimal) d.get("GiaBan"), ((Number) d.get("SoLuongTonKho")).intValue(),
            ((Number) d.get("SoThangBaoHanh")).intValue(), d.get("HinhAnh") == null ? null : String.valueOf(d.get("HinhAnh")), String.valueOf(d.get("TrangThai")));
    }
    private NhanVien taoNhanVien(Map<String, Object> d, String maKey, String tenKey, String phoneKey, String emailKey) {
        if (d == null) return null;
        return new NhanVien(String.valueOf(d.get(maKey)), String.valueOf(d.get(tenKey)), String.valueOf(d.get(phoneKey)),
            d.get(emailKey) == null ? null : String.valueOf(d.get(emailKey)), ((Date) d.get("NgayVaoLam")).toLocalDate(),
            d.get("TrinhDo") == null ? null : String.valueOf(d.get("TrinhDo")), d.get("HeSoLuong") == null ? null : ((Number) d.get("HeSoLuong")).doubleValue(),
            d.get("LuongCoBan") == null ? null : (BigDecimal) d.get("LuongCoBan"), String.valueOf(d.get("LoaiNhanVien")), String.valueOf(d.get("nvTrangThai")));
    }
    private static Integer hangId(Object ten) {
        String s = ten == null ? "" : String.valueOf(ten);
        if ("Bạc".equals(s)) return 2;
        if ("Vàng".equals(s)) return 3;
        if ("Bạch kim".equals(s)) return 4;
        return 1;
    }
    private static int parseIntDigits(String s) {
        try {
            String d = s == null ? "" : s.replaceAll("\\D", "");
            return d.isEmpty() ? 0 : Integer.parseInt(d);
        } catch (Exception ex) {
            return 0;
        }
    }
}

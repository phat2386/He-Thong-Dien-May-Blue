package dao;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import entity.*;
import java.sql.Date;
public class PhieuGiaoHangDAO extends CoSoDAO {
    public String goiYMaPhieuGiao() throws SQLException {
        return goiYMaTiepTheo("PhieuGiaoHang", "MaPhieu", "GH", 5);
    }
    private static String mapHoaDon(String giaTri) {
        return "Đã thanh toán".equals(giaTri) ? "Đã thanh toán" : "Đã hủy";
    }
    private static String mapGiao(String giaTri) {
        return "Hoàn tất".equals(giaTri) ? "Hoàn tất" : "WAITING";
    }
    private static String dbGiao(String giaTri) {
        return "Hoàn tất".equals(giaTri) ? "Hoàn tất" : "Đang chờ";
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
    public boolean phieuGiaoTonTaiChoHoaDon(Connection c, String maHoaDon) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.PhieuGiaoHang WHERE MaHoaDon=?", maHoaDon)).intValue() > 0;
    }
    public boolean maPhieuGiaoTonTai(Connection c, String maPhieu) throws SQLException {
        return ((Number) motGiaTri(c, "SELECT COUNT(*) FROM dbo.PhieuGiaoHang WHERE MaPhieu=?", maPhieu)).intValue() > 0;
    }
    public boolean khachHangLaThanhVienHoatDong(Connection c, String maKhachHang) throws SQLException {
        Object v = motGiaTri(c, "SELECT COUNT(*) FROM dbo.TheThanhVien WHERE MaKhachHang=? AND TrangThai=N'Hoạt động' AND NgayVoHieu IS NULL", maKhachHang);
        return ((Number) v).intValue() > 0;
    }
    public boolean nhanVienDuDieuKienGiaoHang(Connection c, LocalDate ngayGiao, String maNhanVien) throws SQLException {
        Object v = motGiaTri(c, "SELECT COUNT(*) FROM dbo.NhanVien nv " +
            "WHERE nv.MaNhanVien=? AND nv.TrangThai=N'Hoạt động' AND nv.LoaiNhanVien IN(N'Bán hàng',N'CSKH') AND NOT EXISTS(" +
            "SELECT 1 FROM dbo.CaLamViec ca WHERE ca.MaNhanVien=nv.MaNhanVien AND ca.NgayLamViec=? AND ca.TrangThai=N'Đã phân công')", maNhanVien, ngayGiao);
        return ((Number) v).intValue() > 0;
    }
    public void themPhieuGiaoHang(Connection c, String maPhieu, String maHoaDon, String maNhanVienGiaoHang, String diaChi, LocalDate ngayGiao, boolean canLapDat, BigDecimal phiGiaoHang) throws SQLException {
        Map<String, Object> hd = motDong(c, "SELECT MaKhachHang FROM dbo.HoaDon WHERE MaHoaDon=?", maHoaDon);
        if (hd == null) {
            throw new SQLException("Không tìm thấy hóa đơn để lập phiếu giao.");
        }
        capNhat(c, "INSERT INTO dbo.PhieuGiaoHang(MaPhieu,NgayGiao,DiaChiGiao,TrangThai,CanLapDat,PhiGiaoHang,MaHoaDon,MaNhanVienGiaoHang,MaKhachHang) " +
            "VALUES(?,?,?,N'Đang chờ',?,?,?,?,?)", maPhieu, ngayGiao, diaChi, canLapDat, phiGiaoHang, maHoaDon, maNhanVienGiaoHang, hd.get("MaKhachHang"));
    }
    public int phanCongGiaoHang(Connection c, String maPhieu, String maNhanVien) throws SQLException {
        return capNhat(c, "UPDATE dbo.PhieuGiaoHang SET MaNhanVienGiaoHang=? WHERE MaPhieu=? AND TrangThai=N'Đang chờ'", maNhanVien, maPhieu);
    }
    public void capNhatTrangThaiGiaoHang(Connection c, String maPhieu, String trangThai) throws SQLException {
        capNhat(c, "UPDATE dbo.PhieuGiaoHang SET TrangThai=? WHERE MaPhieu=?", dbGiao(trangThai), maPhieu);
    }
    public List<Map<String, Object>> timHoaDonCoTheGiao() throws SQLException {
        return truyVan("SELECT hd.MaHoaDon invoice_id FROM dbo.HoaDon hd JOIN dbo.KhachHang kh ON kh.MaKhachHang=hd.MaKhachHang " +
            "WHERE hd.TrangThai=N'Đã thanh toán' AND kh.MaKhachHang<>'KH000' " +
            "AND EXISTS(SELECT 1 FROM dbo.TheThanhVien tv WHERE tv.MaKhachHang=kh.MaKhachHang AND tv.TrangThai=N'Hoạt động' AND tv.NgayVoHieu IS NULL) " +
            "AND NOT EXISTS(SELECT 1 FROM dbo.PhieuGiaoHang pg WHERE pg.MaHoaDon=hd.MaHoaDon) " +
            "AND EXISTS(SELECT 1 FROM dbo.ChiTietHoaDon ct WHERE ct.MaHoaDon=hd.MaHoaDon AND ct.SoLuong>ISNULL((SELECT SUM(ctdt.SoLuongTra) FROM dbo.PhieuDoiTraHoanTien dt " +
            "JOIN dbo.ChiTietPhieuDoiTra ctdt ON ctdt.MaPhieu=dt.MaPhieu WHERE dt.MaHoaDon=hd.MaHoaDon AND ctdt.MaSanPham=ct.MaSanPham AND dt.TrangThai=N'Đã duyệt' AND dt.NgayHoanTat IS NOT NULL),0)) ORDER BY hd.MaHoaDon");
    }
    public List<Map<String, Object>> timDanhSachGiaoHang(String phamVi, String maNhanVien, String trangThai, LocalDate ngayGiao) throws SQLException {
        String sql = "SELECT pg.MaPhieu delivery_id FROM dbo.PhieuGiaoHang pg WHERE 1=1";
        List<Object> p = new ArrayList<>();
        if ("MINE".equals(phamVi)) {
            sql += " AND pg.MaNhanVienGiaoHang=?";
            p.add(maNhanVien);
        }
        if (trangThai != null && !trangThai.isBlank()) {
            sql += " AND pg.TrangThai=?";
            p.add(dbGiao(trangThai));
        }
        if (ngayGiao != null) {
            sql += " AND pg.NgayGiao=?";
            p.add(ngayGiao);
        }
        sql += " ORDER BY pg.MaPhieu";
        return truyVan(sql, p.toArray());
    }
    public List<NhanVien> timNhanVienGiaoHangPhuHopThucThe(LocalDate ngayGiao) throws SQLException {
        List<NhanVien> ketQua = new ArrayList<>();
        String sql = "SELECT nv.MaNhanVien,nv.HoTen nvTen,nv.SoDienThoai nvPhone,nv.Email nvEmail,nv.NgayVaoLam," +
            "nv.TrinhDo,nv.HeSoLuong,nv.LuongCoBan,nv.LoaiNhanVien,nv.TrangThai nvTrangThai " +
            "FROM dbo.NhanVien nv " +
            "WHERE nv.TrangThai=N'Hoạt động' AND nv.LoaiNhanVien IN(N'Bán hàng',N'CSKH') AND NOT EXISTS(SELECT 1 FROM dbo.CaLamViec ca " +
            "WHERE ca.MaNhanVien=nv.MaNhanVien AND ca.NgayLamViec=? AND ca.TrangThai=N'Đã phân công') ORDER BY nv.MaNhanVien";
        for (Map<String, Object> d : truyVan(sql, ngayGiao)) {
            ketQua.add(taoNhanVien(d, "MaNhanVien", "nvTen", "nvPhone", "nvEmail"));
        }
        return ketQua;
    }
    public List<NhanVien> timNhanVienGiaoHangPhuHopThucThe(String maPhieu) throws SQLException {
        Map<String, Object> d = motDong("SELECT NgayGiao FROM dbo.PhieuGiaoHang WHERE MaPhieu=?", maPhieu);
        if (d == null || d.get("NgayGiao") == null) {
            return Collections.emptyList();
        }
        return timNhanVienGiaoHangPhuHopThucThe(((Date) d.get("NgayGiao")).toLocalDate());
    }
    public List<PhieuGiaoHang> timDanhSachGiaoHangThucThe(String phamVi, String maNhanVien, String trangThai, LocalDate ngayGiao) throws SQLException {
        List<PhieuGiaoHang> ketQua = new ArrayList<>();
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : timDanhSachGiaoHang(phamVi, maNhanVien, trangThai, ngayGiao)) {
                PhieuGiaoHang phieu = timThucThePhieuGiaoHang(c, String.valueOf(d.get("delivery_id")));
                if (phieu != null) {
                    ketQua.add(phieu);
                }
            }
        }
        return ketQua;
    }
    public PhieuGiaoHang timThucThePhieuGiaoHang(Connection c, String maPhieu) throws SQLException {
        Map<String, Object> d = motDong(c, "SELECT pg.*,nv.HoTen nvTen,nv.SoDienThoai nvPhone,nv.Email nvEmail,nv.NgayVaoLam," +
            "nv.TrinhDo,nv.HeSoLuong,nv.LuongCoBan,nv.LoaiNhanVien,nv.TrangThai nvTrangThai " +
            "FROM dbo.PhieuGiaoHang pg JOIN dbo.NhanVien nv ON nv.MaNhanVien=pg.MaNhanVienGiaoHang WHERE pg.MaPhieu=?", maPhieu);
        if (d == null) return null;
        HoaDon hoaDon = timThucTheHoaDon(c, String.valueOf(d.get("MaHoaDon")));
        NhanVien nhanVien = taoNhanVien(d, "MaNhanVienGiaoHang", "nvTen", "nvPhone", "nvEmail");
        PhieuGiaoHang phieu = new PhieuGiaoHang(String.valueOf(d.get("MaPhieu")), hoaDon, nhanVien, String.valueOf(d.get("DiaChiGiao")),
            ((Date) d.get("NgayGiao")).toLocalDate(), Boolean.TRUE.equals(d.get("CanLapDat")), d.get("PhiGiaoHang") == null ? BigDecimal.ZERO : (BigDecimal) d.get("PhiGiaoHang"), mapGiao(String.valueOf(d.get("TrangThai"))));
        for (ChiTietHoaDon ct : timChiTietConLaiDeGiaoThucThe(c, hoaDon.getMaHoaDon())) {
            phieu.themChiTiet(new ChiTietPhieuGiaoHang(phieu, ct.getSanPham(), ct.getSoLuong(), null));
        }
        return phieu;
    }
    public List<ChiTietHoaDon> timChiTietConLaiDeGiaoThucThe(Connection c, String maHoaDon) throws SQLException {
        HoaDon hd = timThucTheHoaDon(c, maHoaDon);
        List<ChiTietHoaDon> ketQua = new ArrayList<>();
        if (hd == null) return ketQua;
        String sql = "SELECT ct.*,ct.SoLuong-ISNULL((SELECT SUM(ctdt.SoLuongTra) FROM dbo.PhieuDoiTraHoanTien dt JOIN dbo.ChiTietPhieuDoiTra ctdt ON ctdt.MaPhieu=dt.MaPhieu " +
            "WHERE dt.MaHoaDon=ct.MaHoaDon AND ctdt.MaSanPham=ct.MaSanPham AND dt.TrangThai=N'Đã duyệt' AND dt.NgayHoanTat IS NOT NULL),0) SoLuongCon FROM dbo.ChiTietHoaDon ct WHERE ct.MaHoaDon=? ORDER BY ct.MaSanPham";
        for (Map<String, Object> x : truyVan(c, sql, maHoaDon)) {
            int mua = ((Number) x.get("SoLuong")).intValue(), con = ((Number) x.get("SoLuongCon")).intValue();
            if (con <= 0) continue;
            SanPham sp = timSanPhamThucThe(c, String.valueOf(x.get("MaSanPham")));
            BigDecimal giam = ((BigDecimal) x.get("TienGiam")).multiply(BigDecimal.valueOf(con)).divide(BigDecimal.valueOf(mua), 2, RoundingMode.HALF_UP);
            BigDecimal tien = ((BigDecimal) x.get("ThanhTien")).multiply(BigDecimal.valueOf(con)).divide(BigDecimal.valueOf(mua), 2, RoundingMode.HALF_UP);
            ketQua.add(new ChiTietHoaDon(hd, sp, con, (BigDecimal) x.get("DonGia"), (BigDecimal) x.get("GiaVonLucBan"), giam, tien));
        }
        return ketQua;
    }
    public List<ChiTietHoaDon> timChiTietConLaiDeGiaoThucThe(String maHoaDon) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            return timChiTietConLaiDeGiaoThucThe(c, maHoaDon);
        }
    }
    public List<HoaDon> timHoaDonCoTheGiaoThucThe() throws SQLException {
        List<HoaDon> ketQua = new ArrayList<>();
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            for (Map<String, Object> d : timHoaDonCoTheGiao()) {
                HoaDon hd = timThucTheHoaDon(c, String.valueOf(d.get("invoice_id")));
                if (hd != null) {
                    ketQua.add(hd);
                }
            }
        }
        return ketQua;
    }
    public KhachHang timKhachHangTheoHoaDonThucThe(String maHoaDon) throws SQLException {
        try (Connection c = connectDB.KetNoiCSDL.layKetNoi()) {
            HoaDon hd = timThucTheHoaDon(c, maHoaDon);
            return hd == null ? null : hd.getKhachHang();
        }
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

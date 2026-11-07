package dao;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
public class ThongKeDAO extends CoSoDAO {
    private static final String TEN_DANH_MUC = "CASE sp.MaDanhMuc " + "WHEN 1 THEN N'Điện thoại' WHEN 2 THEN N'Laptop' WHEN 3 THEN N'TV' " +
        "WHEN 4 THEN N'Điện lạnh' WHEN 5 THEN N'Điện gia dụng' " + "WHEN 6 THEN N'Máy tính bảng' WHEN 7 THEN N'Âm thanh' ELSE N'Khác' END";
    private static final String REFUND = "SELECT dt.MaHoaDon,SUM(ctdt.GiaTriHoan) refunded," + "SUM(ctdt.GiaTriHoan*ISNULL(sp.TyLeThuong,0)) refund_bonus " + "FROM dbo.PhieuDoiTraHoanTien dt " +
        "JOIN dbo.ChiTietPhieuDoiTra ctdt ON ctdt.MaPhieu=dt.MaPhieu LEFT JOIN dbo.SanPham sp ON sp.MaSanPham=ctdt.MaSanPham " + "WHERE dt.TrangThai=N'Đã duyệt' GROUP BY dt.MaHoaDon";
    private static final String INVOICE = "SELECT hd.MaHoaDon,hd.NgayLap,hd.MaNhanVienBanHang,hd.MaKhachHang," + "SUM(ct.ThanhTien) gross,SUM(ct.GiaVonLucBan*ct.SoLuong) cost," +
        "SUM(ct.ThanhTien*ISNULL(sp.TyLeThuong,0)) gross_bonus " + "FROM dbo.HoaDon hd JOIN dbo.ChiTietHoaDon ct ON ct.MaHoaDon=hd.MaHoaDon " +
        "JOIN dbo.SanPham sp ON sp.MaSanPham=ct.MaSanPham " + "WHERE hd.TrangThai=N'Đã thanh toán' GROUP BY hd.MaHoaDon,hd.NgayLap,hd.MaNhanVienBanHang,hd.MaKhachHang";
    public Map<String, Object> tongHopDoanhSoNhanVien(LocalDate from, LocalDate to) throws SQLException {
        return motDong("WITH x AS (" + INVOICE + "),r AS (" + REFUND + ") " + "SELECT ISNULL(SUM(x.gross),0) gross_sales," + "ISNULL(SUM(ISNULL(r.refunded,0)),0) refund_amount," +
            "ISNULL(SUM(x.gross_bonus-ISNULL(r.refund_bonus,0)),0) bonus,COUNT(*) orders " + "FROM x LEFT JOIN r ON r.MaHoaDon=x.MaHoaDon WHERE CAST(x.NgayLap AS date) BETWEEN ? AND ?", from, to);
    }
    public List<Map<String, Object>> chiTietDoanhSoNhanVien(LocalDate from, LocalDate to) throws SQLException {
        return truyVan("WITH x AS (" + INVOICE + "),r AS (" + REFUND + ") " + "SELECT CAST(x.NgayLap AS date) d,nv.MaNhanVien employee_id,nv.HoTen full_name,COUNT(*) cnt," +
            "SUM(x.gross) gross_sales,SUM(ISNULL(r.refunded,0)) refund_amount," + "SUM(x.gross-ISNULL(r.refunded,0)) net_sales," + "SUM(x.gross_bonus-ISNULL(r.refund_bonus,0)) bonus " +
            "FROM x JOIN dbo.NhanVien nv ON nv.MaNhanVien=x.MaNhanVienBanHang " + "LEFT JOIN r ON r.MaHoaDon=x.MaHoaDon WHERE CAST(x.NgayLap AS date) BETWEEN ? AND ? " +
            "GROUP BY CAST(x.NgayLap AS date),nv.MaNhanVien,nv.HoTen ORDER BY d,nv.MaNhanVien", from, to);
    }
    public List<Map<String, Object>> thongKeKhachHang(LocalDate from, LocalDate to, boolean byCount) throws SQLException {
        String order = byCount ? "purchase_count DESC,spent DESC" : "spent DESC,purchase_count DESC";
        return truyVan("WITH x AS (" + INVOICE + "),r AS (" + REFUND + ") " + "SELECT kh.MaKhachHang customer_id,kh.HoTen full_name,kh.SoDienThoai phone,COUNT(*) purchase_count," +
            "SUM(CASE WHEN x.gross-ISNULL(r.refunded,0)>0 THEN x.gross-ISNULL(r.refunded,0) ELSE 0 END) spent " + "FROM x JOIN dbo.KhachHang kh ON kh.MaKhachHang=x.MaKhachHang LEFT JOIN r ON r.MaHoaDon=x.MaHoaDon " +
            "WHERE CAST(x.NgayLap AS date) BETWEEN ? AND ? GROUP BY kh.MaKhachHang,kh.HoTen,kh.SoDienThoai " + "HAVING SUM(CASE WHEN x.gross-ISNULL(r.refunded,0)>0 THEN x.gross-ISNULL(r.refunded,0) ELSE 0 END)>0 ORDER BY " + order, from, to);
    }
    public List<Map<String, Object>> thongKeDoanhThu(LocalDate from, LocalDate to, String group) throws SQLException {
        String p = "CONVERT(char(10),CAST(x.NgayLap AS date),23)";
        if ("MONTH".equals(group)) p = "CONVERT(char(7),x.NgayLap,120)";
        else if ("YEAR".equals(group))
            p = "CONVERT(char(4),YEAR(x.NgayLap))";
        return truyVan("WITH x AS (" + INVOICE + "),r AS (" + REFUND + ") SELECT " + p + " period," + "SUM(x.gross-ISNULL(r.refunded,0)) revenue," +
            "SUM((x.gross-x.cost)-ISNULL(r.refunded,0)) profit,COUNT(*) orders " + "FROM x LEFT JOIN r ON r.MaHoaDon=x.MaHoaDon WHERE CAST(x.NgayLap AS date) BETWEEN ? AND ? " + "GROUP BY " + p + " ORDER BY MIN(x.NgayLap)", from, to);
    }
    public List<Map<String, Object>> thongKeSanPham(LocalDate from, LocalDate to) throws SQLException {
        return truyVan("SELECT sp.TenSanPham product_name," + "ISNULL(SUM(CASE WHEN hd.MaHoaDon IS NOT NULL THEN ct.SoLuong ELSE 0 END),0) qty," +
            "ISNULL(SUM(CASE WHEN hd.MaHoaDon IS NOT NULL THEN ct.ThanhTien ELSE 0 END),0) revenue " + "FROM dbo.SanPham sp LEFT JOIN dbo.ChiTietHoaDon ct ON ct.MaSanPham=sp.MaSanPham " +
            "LEFT JOIN dbo.HoaDon hd ON hd.MaHoaDon=ct.MaHoaDon AND CAST(hd.NgayLap AS date) BETWEEN ? AND ? " + "AND hd.TrangThai=N'Đã thanh toán' GROUP BY sp.MaSanPham,sp.TenSanPham ORDER BY " + "qty DESC,revenue DESC,sp.TenSanPham", from, to);
    }
    public List<Map<String, Object>> thongKeThuong(LocalDate from, LocalDate to) throws SQLException {
        return truyVan("WITH x AS (" + INVOICE + "),r AS (" + REFUND + ") " + "SELECT nv.HoTen full_name,SUM(x.gross) gross_sales,-SUM(ISNULL(r.refunded,0)) adjustments," +
            "SUM(x.gross-ISNULL(r.refunded,0)) net_sales," + "SUM(x.gross_bonus-ISNULL(r.refund_bonus,0)) bonus " + "FROM x JOIN dbo.NhanVien nv ON nv.MaNhanVien=x.MaNhanVienBanHang LEFT JOIN r ON r.MaHoaDon=x.MaHoaDon " +
            "WHERE CAST(x.NgayLap AS date) BETWEEN ? AND ? GROUP BY nv.MaNhanVien,nv.HoTen ORDER BY net_sales DESC", from, to);
    }
    public Map<String, Object> tongQuanHomNay() throws SQLException {
        return motDong("WITH r AS (" + REFUND + ") SELECT " + "(SELECT ISNULL(SUM(h.TongTien-ISNULL(r2.refunded,0)),0) FROM dbo.HoaDon h LEFT " +
            "JOIN r r2 ON r2.MaHoaDon=h.MaHoaDon WHERE CAST(h.NgayLap AS " + "date)=CAST(GETDATE() AS date) AND h.TrangThai=N'Đã thanh toán') revenue," +
            "(SELECT COUNT(*) FROM dbo.HoaDon WHERE CAST(NgayLap AS date)=CAST(GETDATE() AS " + "date) AND TrangThai=N'Đã thanh toán') orders," +
            "(SELECT COUNT(*) FROM dbo.KhachHang WHERE TrangThai=N'Hoạt động') customers," + "(SELECT COUNT(*) FROM dbo.SanPham WHERE TrangThai<>N'Ngừng kinh doanh' AND SoLuongTonKho<=5) low_stock");
    }
    public List<Map<String, Object>> doanhThuBayNgayGanNhat() throws SQLException {
        return truyVan("WITH d AS (SELECT DATEADD(DAY,-6,CAST(GETDATE() AS date)) dt UNION ALL SELECT " + "DATEADD(DAY,1,dt) FROM d WHERE dt<CAST(GETDATE() AS date))," + "r AS (" + REFUND +
            "),x AS (SELECT CAST(hd.NgayLap AS date) dt,SUM(hd.TongTien-ISNULL(r.refunded," + "0)) revenue FROM dbo.HoaDon hd LEFT JOIN r ON r.MaHoaDon=hd.MaHoaDon WHERE " +
            "hd.TrangThai=N'Đã thanh toán' GROUP BY CAST(hd.NgayLap AS date)) " + "SELECT d.dt,ISNULL(x.revenue,0) revenue FROM d LEFT JOIN x ON x.dt=d.dt ORDER BY d.dt OPTION(MAXRECURSION 10)");
    }
    public List<Map<String, Object>> topDanhMucBanChay() throws SQLException {
        return truyVan("SELECT " + TEN_DANH_MUC + " category_name,ISNULL(SUM(ct.SoLuong),0) qty,ISNULL(SUM(ct.ThanhTien),0) val " +
            "FROM dbo.SanPham sp JOIN dbo.ChiTietHoaDon ct ON ct.MaSanPham=sp.MaSanPham " + "JOIN dbo.HoaDon hd ON hd.MaHoaDon=ct.MaHoaDon WHERE hd.TrangThai=N'Đã thanh toán' " + "GROUP BY sp.MaDanhMuc ORDER BY qty DESC,val DESC");
    }
    public List<Map<String, Object>> hoaDonGanDay() throws SQLException {
        return truyVan("SELECT TOP 8 hd.MaHoaDon invoice_id,kh.HoTen customer," + "ISNULL((SELECT SUM(SoLuong) FROM dbo.ChiTietHoaDon ct WHERE ct.MaHoaDon=hd.MaHoaDon),0) items," +
            "hd.TongTien-ISNULL((SELECT SUM(GiaTriHoan) FROM dbo.PhieuDoiTraHoanTien dt " + "WHERE dt.MaHoaDon=hd.MaHoaDon AND dt.TrangThai=N'Đã duyệt'),0) net_total," +
            "nv.HoTen employee,hd.NgayLap created_at,hd.TrangThai status " +
            "FROM dbo.HoaDon hd JOIN dbo.KhachHang kh ON kh.MaKhachHang=hd.MaKhachHang JOIN " + "dbo.NhanVien nv ON nv.MaNhanVien=hd.MaNhanVienBanHang ORDER BY hd.NgayLap DESC");
    }
}

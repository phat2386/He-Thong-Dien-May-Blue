package entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
public class HoaDon {
    private String maHoaDon;
    private KhachHang khachHang;
    private NhanVien nhanVien;
    private LocalDateTime ngayLap;
    private BigDecimal tamTinh = BigDecimal.ZERO;
    private BigDecimal tienGiam = BigDecimal.ZERO;
    private BigDecimal tongTien = BigDecimal.ZERO;
    private String trangThai;
    private String phuongThucThanhToan;
    private String maThamChieuThanhToan;
    private BigDecimal tongTienThuan;
    private String ghiChu;
    private final List<ChiTietHoaDon> chiTietHoaDon = new ArrayList<>();
    public HoaDon() {
    }
    public HoaDon(String maHoaDon, KhachHang khachHang, NhanVien nhanVien, LocalDateTime ngayLap, BigDecimal tamTinh, BigDecimal tienGiam, BigDecimal tongTien, String trangThai, String ghiChu) {
        setMaHoaDon(maHoaDon);
        this.khachHang = khachHang;
        setNhanVien(nhanVien);
        this.ngayLap = ngayLap;
        setTamTinh(tamTinh);
        setTienGiam(tienGiam);
        setTongTien(tongTien);
        this.tongTienThuan = tongTien;
        setTrangThai(trangThai);
        this.ghiChu = ghiChu;
    }
    public HoaDon(HoaDon khac) {
        this(khac.maHoaDon, khac.khachHang, khac.nhanVien, khac.ngayLap, khac.tamTinh, khac.tienGiam, khac.tongTien, khac.trangThai, khac.ghiChu);
        this.chiTietHoaDon.addAll(khac.chiTietHoaDon);
    }
    public String getMaHoaDon() {
        return maHoaDon;
    }
    public void setMaHoaDon(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã hóa đơn không được rỗng.");
        maHoaDon = giaTri.trim();
    }
    public KhachHang getKhachHang() {
        return khachHang;
    }
    public void setKhachHang(KhachHang giaTri) {
        khachHang = giaTri;
    }
    public NhanVien getNhanVien() {
        return nhanVien;
    }
    public void setNhanVien(NhanVien giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Hóa đơn phải có nhân viên lập.");
        nhanVien = giaTri;
    }
    public LocalDateTime getNgayLap() {
        return ngayLap;
    }
    public void setNgayLap(LocalDateTime giaTri) {
        ngayLap = giaTri;
    }
    public BigDecimal getTamTinh() {
        return tamTinh;
    }
    public void setTamTinh(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) throw new IllegalArgumentException("Tạm tính không hợp lệ.");
        tamTinh = giaTri;
    }
    public BigDecimal getTienGiam() {
        return tienGiam;
    }
    public void setTienGiam(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) throw new IllegalArgumentException("Tiền giảm không hợp lệ.");
        tienGiam = giaTri;
    }
    public BigDecimal getTongTien() {
        return tongTien;
    }
    public void setTongTien(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) throw new IllegalArgumentException("Tổng tiền không hợp lệ.");
        tongTien = giaTri;
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("Đã thanh toán", "Đã hủy").contains(giaTri)) throw new IllegalArgumentException("Trạng thái hóa đơn không hợp lệ.");
        trangThai = giaTri;
    }
    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }
    public void setPhuongThucThanhToan(String giaTri) {
        phuongThucThanhToan = giaTri;
    }
    public String getMaThamChieuThanhToan() {
        return maThamChieuThanhToan;
    }
    public void setMaThamChieuThanhToan(String giaTri) {
        maThamChieuThanhToan = giaTri;
    }
    public BigDecimal getTongTienThuan() {
        return tongTienThuan == null ? tongTien : tongTienThuan;
    }
    public void setTongTienThuan(BigDecimal giaTri) {
        tongTienThuan = giaTri == null ? tongTien : giaTri;
    }
    public String getGhiChu() {
        return ghiChu;
    }
    public void setGhiChu(String giaTri) {
        ghiChu = giaTri;
    }
    public List<ChiTietHoaDon> getChiTietHoaDon() {
        return new ArrayList<>(chiTietHoaDon);
    }
    public void themChiTiet(ChiTietHoaDon chiTiet) {
        if (chiTiet != null) chiTietHoaDon.add(chiTiet);
    }
    public BigDecimal tinhTongTuChiTiet() {
        return chiTietHoaDon.stream().map(ChiTietHoaDon::getThanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    @Override
    public String toString() {
        return maHoaDon + " - " + trangThai;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof HoaDon && Objects.equals(maHoaDon, ((HoaDon) doiTuong).maHoaDon);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maHoaDon);
    }
}

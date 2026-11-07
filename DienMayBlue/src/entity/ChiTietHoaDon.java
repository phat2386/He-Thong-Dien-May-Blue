package entity;
import java.math.BigDecimal;
import java.util.Objects;
public class ChiTietHoaDon {
    private HoaDon hoaDon;
    private SanPham sanPham;
    private int soLuong;
    private BigDecimal donGia;
    private BigDecimal giaVonTaiThoiDiemBan;
    private BigDecimal tienGiam;
    private BigDecimal thanhTien;
    public ChiTietHoaDon() {
    }
    public ChiTietHoaDon(HoaDon hoaDon, SanPham sanPham, int soLuong, BigDecimal donGia, BigDecimal giaVonTaiThoiDiemBan, BigDecimal tienGiam, BigDecimal thanhTien) {
        setHoaDon(hoaDon);
        setSanPham(sanPham);
        setSoLuong(soLuong);
        setDonGia(donGia);
        setGiaVonTaiThoiDiemBan(giaVonTaiThoiDiemBan);
        setTienGiam(tienGiam);
        setThanhTien(thanhTien);
    }
    public ChiTietHoaDon(ChiTietHoaDon khac) {
        this(khac.hoaDon, khac.sanPham, khac.soLuong, khac.donGia, khac.giaVonTaiThoiDiemBan, khac.tienGiam, khac.thanhTien);
    }
    public HoaDon getHoaDon() {
        return hoaDon;
    }
    public void setHoaDon(HoaDon giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Chi tiết phải thuộc hóa đơn.");
        hoaDon = giaTri;
    }
    public SanPham getSanPham() {
        return sanPham;
    }
    public void setSanPham(SanPham giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Chi tiết phải có sản phẩm.");
        sanPham = giaTri;
    }
    public int getSoLuong() {
        return soLuong;
    }
    public void setSoLuong(int giaTri) {
        if (giaTri <= 0) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        soLuong = giaTri;
    }
    public BigDecimal getDonGia() {
        return donGia;
    }
    public void setDonGia(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) throw new IllegalArgumentException("Đơn giá không hợp lệ.");
        donGia = giaTri;
    }
    public BigDecimal getGiaVonTaiThoiDiemBan() {
        return giaVonTaiThoiDiemBan;
    }
    public void setGiaVonTaiThoiDiemBan(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) throw new IllegalArgumentException("Giá vốn không hợp lệ.");
        giaVonTaiThoiDiemBan = giaTri;
    }
    public BigDecimal getTienGiam() {
        return tienGiam;
    }
    public void setTienGiam(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) throw new IllegalArgumentException("Tiền giảm không hợp lệ.");
        tienGiam = giaTri;
    }
    public BigDecimal getThanhTien() {
        return thanhTien;
    }
    public void setThanhTien(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) throw new IllegalArgumentException("Thành tiền không hợp lệ.");
        thanhTien = giaTri;
    }
    public BigDecimal tinhThanhTien() {
        return donGia.multiply(BigDecimal.valueOf(soLuong)).subtract(tienGiam == null ? BigDecimal.ZERO : tienGiam);
    }
    @Override
    public String toString() {
        return sanPham + " x " + soLuong;
    }
    @Override
    public boolean equals(Object doiTuong) {
        if (!(doiTuong instanceof ChiTietHoaDon)) return false;
        ChiTietHoaDon khac = (ChiTietHoaDon) doiTuong;
        return Objects.equals(hoaDon == null ? null : hoaDon.getMaHoaDon(), khac.hoaDon == null ? null : khac.hoaDon.getMaHoaDon()) &&
            Objects.equals(sanPham == null ? null : sanPham.getMaSanPham(), khac.sanPham == null ? null : khac.sanPham.getMaSanPham());
    }
    @Override
    public int hashCode() {
        return Objects.hash(hoaDon == null ? null : hoaDon.getMaHoaDon(), sanPham == null ? null : sanPham.getMaSanPham());
    }
}

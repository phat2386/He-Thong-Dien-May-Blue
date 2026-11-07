package entity;
import java.math.BigDecimal;
import java.util.Objects;
public class ChiTietPhieuDoiTra {
    private PhieuDoiTraHoanTien phieu;
    private SanPham sanPham;
    private int soLuongTra;
    private BigDecimal giaTriHoan;
    public ChiTietPhieuDoiTra() {
    }
    public ChiTietPhieuDoiTra(PhieuDoiTraHoanTien phieu, SanPham sanPham, int soLuongTra, BigDecimal giaTriHoan) {
        setPhieu(phieu);
        setSanPham(sanPham);
        setSoLuongTra(soLuongTra);
        setGiaTriHoan(giaTriHoan);
    }
    public PhieuDoiTraHoanTien getPhieu() {
        return phieu;
    }
    public void setPhieu(PhieuDoiTraHoanTien giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Chi tiết đổi trả phải thuộc một phiếu.");
        phieu = giaTri;
    }
    public SanPham getSanPham() {
        return sanPham;
    }
    public void setSanPham(SanPham giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Chi tiết đổi trả phải có sản phẩm.");
        sanPham = giaTri;
    }
    public int getSoLuongTra() {
        return soLuongTra;
    }
    public void setSoLuongTra(int giaTri) {
        if (giaTri <= 0) throw new IllegalArgumentException("Số lượng trả phải lớn hơn 0.");
        soLuongTra = giaTri;
    }
    public BigDecimal getGiaTriHoan() {
        return giaTriHoan;
    }
    public void setGiaTriHoan(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() <= 0) throw new IllegalArgumentException("Giá trị hoàn phải lớn hơn 0.");
        giaTriHoan = giaTri;
    }
    @Override
    public boolean equals(Object doiTuong) {
        if (!(doiTuong instanceof ChiTietPhieuDoiTra)) return false;
        ChiTietPhieuDoiTra khac = (ChiTietPhieuDoiTra) doiTuong;
        return Objects.equals(phieu == null ? null : phieu.getMaPhieu(), khac.phieu == null ? null : khac.phieu.getMaPhieu()) &&
            Objects.equals(sanPham == null ? null : sanPham.getMaSanPham(), khac.sanPham == null ? null : khac.sanPham.getMaSanPham());
    }
    @Override
    public int hashCode() {
        return Objects.hash(phieu == null ? null : phieu.getMaPhieu(), sanPham == null ? null : sanPham.getMaSanPham());
    }
}

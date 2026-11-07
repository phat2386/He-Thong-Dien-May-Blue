package entity;
public class ChiTietPhieuGiaoHang {
    private PhieuGiaoHang phieuGiaoHang;
    private SanPham sanPham;
    private int soLuong;
    private String ghiChu;
    public ChiTietPhieuGiaoHang() {
    }
    public ChiTietPhieuGiaoHang(PhieuGiaoHang phieuGiaoHang, SanPham sanPham, int soLuong, String ghiChu) {
        setPhieuGiaoHang(phieuGiaoHang);
        setSanPham(sanPham);
        setSoLuong(soLuong);
        this.ghiChu = ghiChu;
    }
    public ChiTietPhieuGiaoHang(ChiTietPhieuGiaoHang khac) {
        this(khac.phieuGiaoHang, khac.sanPham, khac.soLuong, khac.ghiChu);
    }
    public PhieuGiaoHang getPhieuGiaoHang() {
        return phieuGiaoHang;
    }
    public void setPhieuGiaoHang(PhieuGiaoHang giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Chi tiết phải thuộc phiếu giao hàng.");
        phieuGiaoHang = giaTri;
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
    public String getGhiChu() {
        return ghiChu;
    }
    public void setGhiChu(String giaTri) {
        ghiChu = giaTri;
    }
    @Override
    public String toString() {
        return sanPham + " x " + soLuong;
    }
}

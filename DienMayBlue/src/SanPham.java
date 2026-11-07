package entity;
import java.math.BigDecimal;
import java.util.*;
public class SanPham {
    private String maSanPham;
    private String maVach;
    private String tenSanPham;
    private int maDanhMuc;
    private NhaCungCap nhaCungCap;
    private BigDecimal giaVon;
    private BigDecimal giaBan;
    private int soLuongTonKho;
    private int soThangBaoHanh;
    private String hinhAnh;
    private String trangThai;
    public SanPham() {
    }
    public SanPham(String maSanPham, String maVach, String tenSanPham, int maDanhMuc, NhaCungCap nhaCungCap, BigDecimal giaVon, BigDecimal giaBan, int soLuongTonKho, int soThangBaoHanh, String hinhAnh, String trangThai) {
        setMaSanPham(maSanPham);
        setMaVach(maVach);
        setTenSanPham(tenSanPham);
        this.maDanhMuc = maDanhMuc;
        this.nhaCungCap = nhaCungCap;
        setGiaVon(giaVon);
        setGiaBan(giaBan);
        setSoLuongTonKho(soLuongTonKho);
        setSoThangBaoHanh(soThangBaoHanh);
        this.hinhAnh = hinhAnh;
        setTrangThai(trangThai);
    }
    public SanPham(SanPham khac) {
        this(khac.maSanPham, khac.maVach, khac.tenSanPham, khac.maDanhMuc, khac.nhaCungCap, khac.giaVon, khac.giaBan, khac.soLuongTonKho, khac.soThangBaoHanh, khac.hinhAnh, khac.trangThai);
    }
    public String getMaSanPham() {
        return maSanPham;
    }
    public void setMaSanPham(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã sản phẩm không được rỗng.");
        maSanPham = giaTri.trim();
    }
    public String getMaVach() {
        return maVach;
    }
    public void setMaVach(String giaTri) {
        maVach = giaTri == null || giaTri.trim().isEmpty() ? null : giaTri.trim();
    }
    public String getTenSanPham() {
        return tenSanPham;
    }
    public void setTenSanPham(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Tên sản phẩm không được rỗng.");
        tenSanPham = giaTri.trim();
    }
    public int getMaDanhMuc() {
        return maDanhMuc;
    }
    public void setMaDanhMuc(int giaTri) {
        maDanhMuc = giaTri;
    }
    public NhaCungCap getNhaCungCap() {
        return nhaCungCap;
    }
    public void setNhaCungCap(NhaCungCap giaTri) {
        nhaCungCap = giaTri;
    }
    public BigDecimal getGiaVon() {
        return giaVon;
    }
    public void setGiaVon(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() <= 0) throw new IllegalArgumentException("Giá vốn phải lớn hơn 0.");
        giaVon = giaTri;
        if (giaBan != null && giaBan.compareTo(giaVon) < 0) throw new IllegalArgumentException("Giá bán phải lớn hơn hoặc bằng giá vốn.");
    }
    public BigDecimal getGiaBan() {
        return giaBan;
    }
    public void setGiaBan(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() <= 0) throw new IllegalArgumentException("Giá bán phải lớn hơn 0.");
        if (giaVon != null && giaTri.compareTo(giaVon) < 0) throw new IllegalArgumentException("Giá bán phải lớn hơn hoặc bằng giá vốn.");
        giaBan = giaTri;
    }
    public int getSoLuongTonKho() {
        return soLuongTonKho;
    }
    public void setSoLuongTonKho(int giaTri) {
        if (giaTri < 0) throw new IllegalArgumentException("Tồn kho không được âm.");
        soLuongTonKho = giaTri;
    }
    public int getSoThangBaoHanh() {
        return soThangBaoHanh;
    }
    public void setSoThangBaoHanh(int giaTri) {
        if (giaTri < 12 || giaTri > 120) throw new IllegalArgumentException("Thời hạn bảo hành phải từ 12 đến 120 tháng.");
        soThangBaoHanh = giaTri;
    }
    public String getHinhAnh() {
        return hinhAnh;
    }
    public void setHinhAnh(String giaTri) {
        hinhAnh = giaTri;
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("Hoạt động", "Ngừng kinh doanh", "Hết hàng").contains(giaTri)) throw new IllegalArgumentException("Trạng thái sản phẩm không hợp lệ.");
        trangThai = giaTri;
    }
    public boolean duTonKho(int soLuong) {
        return soLuong > 0 && soLuong <= soLuongTonKho;
    }
    @Override
    public String toString() {
        return maSanPham + " - " + tenSanPham;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof SanPham && Objects.equals(maSanPham, ((SanPham) doiTuong).maSanPham);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maSanPham);
    }
}

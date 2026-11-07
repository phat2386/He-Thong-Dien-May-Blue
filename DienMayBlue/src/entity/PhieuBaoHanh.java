package entity;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public class PhieuBaoHanh {
    private String maPhieu;
    private KhachHang khachHang;
    private SanPham sanPham;
    private String moTaLoi;
    private LocalDate hanBaoHanh;
    private String tinhTrang;
    private BigDecimal phiDoiMay;
    private NhanVien nhanVien;
    private LocalDateTime ngayTiepNhan;
    private LocalDateTime ngayHoanThanh;
    private LocalDateTime ngayTraKhach;
    public PhieuBaoHanh() {
    }
    public PhieuBaoHanh(String maPhieu, KhachHang khachHang, SanPham sanPham, String moTaLoi, LocalDate hanBaoHanh, String trangThai, BigDecimal phiDoiMay, NhanVien nguoiTao, LocalDateTime ngayTiepNhan) {
        setMaPhieu(maPhieu);
        setKhachHang(khachHang);
        setSanPham(sanPham);
        setMoTaLoi(moTaLoi);
        setHanBaoHanh(hanBaoHanh);
        setTrangThai(trangThai);
        this.phiDoiMay = phiDoiMay;
        setNguoiTao(nguoiTao);
        this.ngayTiepNhan = ngayTiepNhan;
    }
    public PhieuBaoHanh(PhieuBaoHanh khac) {
        this(khac.maPhieu, khac.khachHang, khac.sanPham, khac.moTaLoi, khac.hanBaoHanh, khac.tinhTrang, khac.phiDoiMay, khac.nhanVien, khac.ngayTiepNhan);
        this.ngayHoanThanh = khac.ngayHoanThanh;
        this.ngayTraKhach = khac.ngayTraKhach;
    }
    public String getMaPhieu() {
        return maPhieu;
    }
    public void setMaPhieu(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã bảo hành không được rỗng.");
        maPhieu = giaTri.trim();
    }
    public KhachHang getKhachHang() {
        return khachHang;
    }
    public void setKhachHang(KhachHang giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Bảo hành phải có khách hàng.");
        khachHang = giaTri;
    }
    public SanPham getSanPham() {
        return sanPham;
    }
    public void setSanPham(SanPham giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Bảo hành phải có sản phẩm.");
        sanPham = giaTri;
    }
    public String getMoTaLoi() {
        return moTaLoi;
    }
    public void setMoTaLoi(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mô tả lỗi không được rỗng.");
        moTaLoi = giaTri.trim();
    }
    public LocalDate getHanBaoHanh() {
        return hanBaoHanh;
    }
    public void setHanBaoHanh(LocalDate giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Hạn bảo hành không được rỗng.");
        hanBaoHanh = giaTri;
    }
    public String getTinhTrang() {
        return tinhTrang;
    }
    public void setTinhTrang(String giaTri) {
        if (!Arrays.asList("REPAIRING", "Hoàn tất", "RETURNED", "REJECTED", "REPLACEMENT").contains(giaTri)) throw new IllegalArgumentException("Tình trạng bảo hành không hợp lệ.");
        tinhTrang = giaTri;
    }
    public String getTrangThai() {
        return tinhTrang;
    }
    public void setTrangThai(String giaTri) {
        setTinhTrang(giaTri);
    }
    public BigDecimal getPhiDoiMay() {
        return phiDoiMay;
    }
    public void setPhiDoiMay(BigDecimal giaTri) {
        if (giaTri != null && giaTri.signum() < 0) throw new IllegalArgumentException("Phí đổi máy không hợp lệ.");
        phiDoiMay = giaTri;
    }
    public NhanVien getNhanVien() {
        return nhanVien;
    }
    public void setNhanVien(NhanVien giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Phiếu bảo hành phải có nhân viên tiếp nhận.");
        nhanVien = giaTri;
    }
    public NhanVien getNguoiTao() {
        return nhanVien;
    }
    public void setNguoiTao(NhanVien giaTri) {
        setNhanVien(giaTri);
    }
    public LocalDateTime getNgayTiepNhan() {
        return ngayTiepNhan;
    }
    public void setNgayTiepNhan(LocalDateTime giaTri) {
        ngayTiepNhan = giaTri;
    }
    public LocalDateTime getNgayHoanThanh() {
        return ngayHoanThanh;
    }
    public void setNgayHoanThanh(LocalDateTime giaTri) {
        ngayHoanThanh = giaTri;
    }
    public LocalDateTime getNgayTraKhach() {
        return ngayTraKhach;
    }
    public void setNgayTraKhach(LocalDateTime giaTri) {
        ngayTraKhach = giaTri;
    }
    @Override
    public String toString() {
        return maPhieu + " - " + tinhTrang;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof PhieuBaoHanh && Objects.equals(maPhieu, ((PhieuBaoHanh) doiTuong).maPhieu);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maPhieu);
    }
}

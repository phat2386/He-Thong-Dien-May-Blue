package entity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public class PhieuGiaoHang {
    private String maPhieu;
    private HoaDon hoaDon;
    private NhanVien nhanVienGiaoHang;
    private KhachHang khachHang;
    private String diaChiGiao;
    private LocalDate ngayGiao;
    private boolean canLapDat;
    private BigDecimal phiGiaoHang = BigDecimal.ZERO;
    private String trangThai;
    private final List<ChiTietPhieuGiaoHang> chiTiet = new ArrayList<>();
    public PhieuGiaoHang() {
    }
    public PhieuGiaoHang(String maPhieu, HoaDon hoaDon, NhanVien nhanVienGiaoHang, String diaChiGiao, LocalDate ngayGiao, boolean canLapDat, BigDecimal phiGiaoHang, String trangThai) {
        setMaPhieu(maPhieu);
        setHoaDon(hoaDon);
        setNhanVienGiaoHang(nhanVienGiaoHang);
        setDiaChiGiao(diaChiGiao);
        setNgayGiao(ngayGiao);
        setCanLapDat(canLapDat);
        setPhiGiaoHang(phiGiaoHang);
        setTrangThai(trangThai);
    }
    public PhieuGiaoHang(PhieuGiaoHang khac) {
        this(khac.maPhieu, khac.hoaDon, khac.nhanVienGiaoHang, khac.diaChiGiao, khac.ngayGiao, khac.canLapDat, khac.phiGiaoHang, khac.trangThai);
        this.chiTiet.addAll(khac.chiTiet);
    }
    public String getMaPhieu() {
        return maPhieu;
    }
    public void setMaPhieu(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phiếu giao không được rỗng.");
        }
        maPhieu = giaTri.trim();
    }
    public HoaDon getHoaDon() {
        return hoaDon;
    }
    public void setHoaDon(HoaDon giaTri) {
        if (giaTri == null) {
            throw new IllegalArgumentException("Phiếu giao phải tham chiếu hóa đơn.");
        }
        hoaDon = giaTri;
        khachHang = giaTri.getKhachHang();
    }
    public NhanVien getNhanVien() {
        return nhanVienGiaoHang;
    }
    public void setNhanVien(NhanVien giaTri) {
        setNhanVienGiaoHang(giaTri);
    }
    public NhanVien getNhanVienPhuTrach() {
        return nhanVienGiaoHang;
    }
    public void setNhanVienPhuTrach(NhanVien giaTri) {
        setNhanVienGiaoHang(giaTri);
    }
    public NhanVien getNhanVienGiaoHang() {
        return nhanVienGiaoHang;
    }
    public void setNhanVienGiaoHang(NhanVien giaTri) {
        if (giaTri == null) {
            throw new IllegalArgumentException("Phiếu giao phải có nhân viên giao hàng.");
        }
        nhanVienGiaoHang = giaTri;
    }
    public KhachHang getKhachHang() {
        return khachHang;
    }
    public void setKhachHang(KhachHang giaTri) {
        if (giaTri == null) {
            throw new IllegalArgumentException("Phiếu giao phải có khách hàng.");
        }
        khachHang = giaTri;
    }
    public String getDiaChiGiao() {
        return diaChiGiao;
    }
    public void setDiaChiGiao(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ giao không được rỗng.");
        }
        diaChiGiao = giaTri.trim();
    }
    public LocalDate getNgayGiao() {
        return ngayGiao;
    }
    public void setNgayGiao(LocalDate giaTri) {
        if (giaTri == null) {
            throw new IllegalArgumentException("Ngày giao không được rỗng.");
        }
        ngayGiao = giaTri;
    }
    public boolean isCanLapDat() {
        return canLapDat;
    }
    public void setCanLapDat(boolean giaTri) {
        canLapDat = giaTri;
    }
    public BigDecimal getPhiGiaoHang() {
        return phiGiaoHang;
    }
    public void setPhiGiaoHang(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) {
            throw new IllegalArgumentException("Phí giao hàng không hợp lệ.");
        }
        phiGiaoHang = giaTri;
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("WAITING", "Hoàn tất").contains(giaTri)) {
            throw new IllegalArgumentException("Trạng thái giao hàng không hợp lệ.");
        }
        trangThai = giaTri;
    }
    public List<ChiTietPhieuGiaoHang> getChiTiet() {
        return new ArrayList<>(chiTiet);
    }
    public void themChiTiet(ChiTietPhieuGiaoHang giaTri) {
        if (giaTri != null) {
            chiTiet.add(giaTri);
        }
    }
    @Override
    public String toString() {
        return maPhieu + " - " + trangThai;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof PhieuGiaoHang && Objects.equals(maPhieu, ((PhieuGiaoHang) doiTuong).maPhieu);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maPhieu);
    }
}

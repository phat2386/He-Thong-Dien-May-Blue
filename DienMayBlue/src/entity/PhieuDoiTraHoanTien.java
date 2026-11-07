package entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
public class PhieuDoiTraHoanTien {
    private String maPhieu;
    private HoaDon hoaDon;
    private final List<ChiTietPhieuDoiTra> chiTiet = new ArrayList<>();
    private NhanVien nhanVienBanHang;
    private NhanVien nhanVienQuanLy;
    private LocalDateTime ngayLap;
    private String loaiLyDo;
    private String lyDo;
    private String tinhTrangVatLy;
    private boolean conTemMac;
    private boolean dayDuHop;
    private boolean duPhuKien;
    private BigDecimal giaTriHoan;
    private boolean canDuyet;
    private String trangThai;
    private String lyDoTuChoi;
    private LocalDateTime ngayHoanTat;
    public PhieuDoiTraHoanTien() {
    }
    public PhieuDoiTraHoanTien(String maPhieu, HoaDon hoaDon, NhanVien nhanVienBanHang, LocalDateTime ngayLap, String lyDo, String tinhTrangVatLy, BigDecimal giaTriHoan, boolean canDuyet, String trangThai) {
        setMaPhieu(maPhieu);
        setHoaDon(hoaDon);
        setNhanVienBanHang(nhanVienBanHang);
        this.ngayLap = ngayLap;
        setLyDo(lyDo);
        this.tinhTrangVatLy = tinhTrangVatLy;
        setGiaTriHoan(giaTriHoan);
        this.canDuyet = canDuyet;
        setTrangThai(trangThai);
    }
    public PhieuDoiTraHoanTien(PhieuDoiTraHoanTien khac) {
        this(khac.maPhieu, khac.hoaDon, khac.nhanVienBanHang, khac.ngayLap, khac.lyDo, khac.tinhTrangVatLy, khac.giaTriHoan, khac.canDuyet, khac.trangThai);
        this.nhanVienQuanLy = khac.nhanVienQuanLy;
        for (ChiTietPhieuDoiTra d : khac.chiTiet) this.chiTiet.add(new ChiTietPhieuDoiTra(this, d.getSanPham(), d.getSoLuongTra(), d.getGiaTriHoan()));
        this.loaiLyDo = khac.loaiLyDo;
        this.conTemMac = khac.conTemMac;
        this.dayDuHop = khac.dayDuHop;
        this.duPhuKien = khac.duPhuKien;
        this.lyDoTuChoi = khac.lyDoTuChoi;
        this.ngayHoanTat = khac.ngayHoanTat;
    }
    public String getMaPhieu() {
        return maPhieu;
    }
    public void setMaPhieu(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phiếu đổi trả không được rỗng.");
        }
        maPhieu = giaTri.trim();
    }
    public HoaDon getHoaDon() {
        return hoaDon;
    }
    public void setHoaDon(HoaDon giaTri) {
        if (giaTri == null) {
            throw new IllegalArgumentException("Phiếu đổi trả phải có hóa đơn gốc.");
        }
        hoaDon = giaTri;
    }
    public KhachHang getKhachHang() {
        return hoaDon == null ? null : hoaDon.getKhachHang();
    }
    public List<ChiTietPhieuDoiTra> getChiTiet() {
        return Collections.unmodifiableList(chiTiet);
    }
    public void themChiTiet(ChiTietPhieuDoiTra giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Chi tiết đổi trả không hợp lệ.");
        if (giaTri.getPhieu() != this) giaTri.setPhieu(this);
        chiTiet.add(giaTri);
    }
    public void xoaChiTiet() {
        chiTiet.clear();
    }
    public NhanVien getNguoiLap() {
        return nhanVienBanHang;
    }
    public void setNguoiLap(NhanVien giaTri) {
        setNhanVienBanHang(giaTri);
    }
    public NhanVien getNhanVienBanHang() {
        return nhanVienBanHang;
    }
    public void setNhanVienBanHang(NhanVien giaTri) {
        if (giaTri == null) {
            throw new IllegalArgumentException("Phiếu đổi trả phải xác định nhân viên bán hàng của hóa đơn.");
        }
        nhanVienBanHang = giaTri;
    }
    public NhanVien getNhanVienQuanLy() {
        return nhanVienQuanLy;
    }
    public void setNhanVienQuanLy(NhanVien giaTri) {
        nhanVienQuanLy = giaTri;
    }
    public LocalDateTime getNgayLap() {
        return ngayLap;
    }
    public void setNgayLap(LocalDateTime giaTri) {
        ngayLap = giaTri;
    }
    public String getLoaiLyDo() {
        return loaiLyDo;
    }
    public void setLoaiLyDo(String giaTri) {
        loaiLyDo = giaTri;
    }
    public String getLyDo() {
        return lyDo;
    }
    public void setLyDo(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do không được rỗng.");
        }
        lyDo = giaTri.trim();
    }
    public String getTinhTrangVatLy() {
        return tinhTrangVatLy;
    }
    public void setTinhTrangVatLy(String giaTri) {
        tinhTrangVatLy = giaTri;
    }
    public boolean isConTemMac() {
        return conTemMac;
    }
    public void setConTemMac(boolean giaTri) {
        conTemMac = giaTri;
    }
    public boolean isDayDuHop() {
        return dayDuHop;
    }
    public void setDayDuHop(boolean giaTri) {
        dayDuHop = giaTri;
    }
    public boolean isDuPhuKien() {
        return duPhuKien;
    }
    public void setDuPhuKien(boolean giaTri) {
        duPhuKien = giaTri;
    }
    public BigDecimal getGiaTriHoan() {
        return giaTriHoan;
    }
    public void setGiaTriHoan(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) {
            throw new IllegalArgumentException("Giá trị hoàn không hợp lệ.");
        }
        giaTriHoan = giaTri;
    }
    public boolean isCanDuyet() {
        return canDuyet;
    }
    public void setCanDuyet(boolean giaTri) {
        canDuyet = giaTri;
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("PENDING", "WAITING_APPROVAL", "APPROVED", "REJECTED", "Hoàn tất").contains(giaTri)) {
            throw new IllegalArgumentException("Trạng thái đổi trả không hợp lệ.");
        }
        trangThai = giaTri;
    }
    public String getLyDoTuChoi() {
        return lyDoTuChoi;
    }
    public void setLyDoTuChoi(String giaTri) {
        lyDoTuChoi = giaTri;
    }
    public LocalDateTime getNgayHoanTat() {
        return ngayHoanTat;
    }
    public void setNgayHoanTat(LocalDateTime giaTri) {
        ngayHoanTat = giaTri;
    }
    public boolean vuotHanMuc(BigDecimal hanMuc) {
        return hanMuc != null && giaTriHoan != null && giaTriHoan.compareTo(hanMuc) > 0;
    }
    @Override
    public String toString() {
        return maPhieu + " - " + trangThai;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof PhieuDoiTraHoanTien && Objects.equals(maPhieu, ((PhieuDoiTraHoanTien) doiTuong).maPhieu);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maPhieu);
    }
}

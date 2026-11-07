package entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
public class KhuyenMai {
    private String maKhuyenMai;
    private String tenChuongTrinh;
    private String loaiGiam;
    private BigDecimal giaTriGiam;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private String trangThai;
    private NhanVien nguoiTao;
    private final List<SanPham> danhSachSanPham = new ArrayList<>();
    public KhuyenMai() {
    }
    public KhuyenMai(String maKhuyenMai, String tenChuongTrinh, String loaiGiam, BigDecimal giaTriGiam, LocalDateTime ngayBatDau, LocalDateTime ngayKetThuc, String trangThai, NhanVien nguoiTao) {
        setMaKhuyenMai(maKhuyenMai);
        setTenChuongTrinh(tenChuongTrinh);
        setLoaiGiam(loaiGiam);
        setGiaTriGiam(giaTriGiam);
        setNgayBatDau(ngayBatDau);
        setNgayKetThuc(ngayKetThuc);
        setTrangThai(trangThai);
        this.nguoiTao = nguoiTao;
    }
    public KhuyenMai(KhuyenMai khac) {
        this(khac.maKhuyenMai, khac.tenChuongTrinh, khac.loaiGiam, khac.giaTriGiam, khac.ngayBatDau, khac.ngayKetThuc, khac.trangThai, khac.nguoiTao);
        this.danhSachSanPham.addAll(khac.danhSachSanPham);
    }
    public String getMaKhuyenMai() {
        return maKhuyenMai;
    }
    public void setMaKhuyenMai(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã khuyến mãi không được rỗng.");
        maKhuyenMai = giaTri.trim();
    }
    public String getTenChuongTrinh() {
        return tenChuongTrinh;
    }
    public void setTenChuongTrinh(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Tên chương trình không được rỗng.");
        tenChuongTrinh = giaTri.trim();
    }
    public String getLoaiGiam() {
        return loaiGiam;
    }
    public void setLoaiGiam(String giaTri) {
        if (!Arrays.asList("PERCENT", "FIXED").contains(giaTri)) throw new IllegalArgumentException("Loại giảm không hợp lệ.");
        loaiGiam = giaTri;
    }
    public BigDecimal getGiaTriGiam() {
        return giaTriGiam;
    }
    public void setGiaTriGiam(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0 || ("PERCENT".equals(loaiGiam) && giaTri.compareTo(new BigDecimal( "100")) > 0)) throw new IllegalArgumentException("Giá trị giảm không hợp lệ.");
        giaTriGiam = giaTri;
    }
    public LocalDateTime getNgayBatDau() {
        return ngayBatDau;
    }
    public void setNgayBatDau(LocalDateTime giaTri) {
        ngayBatDau = giaTri;
        if (ngayKetThuc != null) kiemTraNgay();
    }
    public LocalDateTime getNgayKetThuc() {
        return ngayKetThuc;
    }
    public void setNgayKetThuc(LocalDateTime giaTri) {
        ngayKetThuc = giaTri;
        if (ngayBatDau != null) kiemTraNgay();
    }
    private void kiemTraNgay() {
        if (ngayBatDau == null || ngayKetThuc == null || ngayKetThuc.isBefore(ngayBatDau)) throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("Sắp diễn ra", "Hoạt động", "Đã kết thúc", "Đã hủy").contains(giaTri)) throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ.");
        trangThai = giaTri;
    }
    public NhanVien getNguoiTao() {
        return nguoiTao;
    }
    public void setNguoiTao(NhanVien giaTri) {
        nguoiTao = giaTri;
    }
    public List<SanPham> getDanhSachSanPham() {
        return new ArrayList<>(danhSachSanPham);
    }
    public void themSanPham(SanPham sanPham) {
        if (sanPham != null && !danhSachSanPham.contains(sanPham)) danhSachSanPham.add(sanPham);
    }
    public boolean conHieuLuc(LocalDateTime thoiDiem) {
        return "Hoạt động".equals(trangThai) && thoiDiem != null && !thoiDiem.isBefore(ngayBatDau) && !thoiDiem .isAfter(ngayKetThuc);
    }
    public BigDecimal tinhGiaSauKhuyenMai(BigDecimal giaGoc) {
        if (giaGoc == null) return BigDecimal.ZERO;
        if ("PERCENT".equals(loaiGiam)) return giaGoc.subtract(giaGoc.multiply(giaTriGiam).divide(new BigDecimal("100")));
        return giaGoc.subtract(giaTriGiam).max(BigDecimal.ZERO);
    }
    @Override
    public String toString() {
        return maKhuyenMai + " - " + tenChuongTrinh;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof KhuyenMai && Objects.equals(maKhuyenMai, ((KhuyenMai) doiTuong).maKhuyenMai);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maKhuyenMai);
    }
}

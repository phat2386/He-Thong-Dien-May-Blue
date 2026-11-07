package entity;
import java.time.LocalDateTime;
import java.util.*;
public class TheThanhVien {
    private String maThe;
    private KhachHang khachHang;
    private String trangThai;
    private LocalDateTime ngayCap;
    private LocalDateTime ngayVoHieu;
    private String hangThanhVien;
    private int diemTichLuy;
    public TheThanhVien() {
    }
    public TheThanhVien(String maThe, KhachHang khachHang, String trangThai, LocalDateTime ngayCap, LocalDateTime ngayVoHieu, String hangThanhVien) {
        setMaThe(maThe);
        setKhachHang(khachHang);
        setTrangThai(trangThai);
        this.ngayCap = ngayCap;
        this.ngayVoHieu = ngayVoHieu;
        this.hangThanhVien = hangThanhVien;
        this.diemTichLuy = khachHang == null ? 0 : khachHang.getDiemHienTai();
    }
    public TheThanhVien(TheThanhVien khac) {
        this(khac.maThe, khac.khachHang, khac.trangThai, khac.ngayCap, khac.ngayVoHieu, khac.hangThanhVien);
        this.diemTichLuy = khac.diemTichLuy;
    }
    public String getMaThe() {
        return maThe;
    }
    public void setMaThe(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã thẻ không được rỗng.");
        maThe = giaTri.trim();
    }
    public String getMaTheHienThi() {
        return maThe;
    }
    public void setMaTheHienThi(String giaTri) {
        setMaThe(giaTri);
    }
    public KhachHang getKhachHang() {
        return khachHang;
    }
    public void setKhachHang(KhachHang giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Thẻ phải thuộc khách hàng.");
        khachHang = giaTri;
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("Hoạt động", "Đã vô hiệu hóa", "Đã mất").contains(giaTri)) throw new IllegalArgumentException("Trạng thái thẻ không hợp lệ.");
        trangThai = giaTri;
    }
    public LocalDateTime getNgayCap() {
        return ngayCap;
    }
    public void setNgayCap(LocalDateTime giaTri) {
        ngayCap = giaTri;
    }
    public LocalDateTime getNgayVoHieu() {
        return ngayVoHieu;
    }
    public void setNgayVoHieu(LocalDateTime giaTri) {
        ngayVoHieu = giaTri;
    }
    public String getHangThanhVien() {
        return hangThanhVien;
    }
    public void setHangThanhVien(String giaTri) {
        hangThanhVien = giaTri;
    }
    public int getDiemTichLuy() {
        return diemTichLuy;
    }
    public void setDiemTichLuy(int giaTri) {
        if (giaTri < 0) throw new IllegalArgumentException("Điểm tích lũy không được âm.");
        diemTichLuy = giaTri;
    }
    @Override
    public String toString() {
        return maThe + (hangThanhVien == null ? "" : " - " + hangThanhVien);
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof TheThanhVien && Objects.equals(maThe, ((TheThanhVien) doiTuong).maThe);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maThe);
    }
}

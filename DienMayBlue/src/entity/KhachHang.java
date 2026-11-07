package entity;
import java.util.*;
public class KhachHang {
    private String maKhachHang;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private Integer maHangThanhVien;
    private int diemHienTai;
    private String trangThai;
    public KhachHang() {
    }
    public KhachHang(String maKhachHang, String hoTen, String soDienThoai, String email, String diaChi, Integer maHangThanhVien, int diemHienTai, String trangThai) {
        setMaKhachHang(maKhachHang);
        setHoTen(hoTen);
        setSoDienThoai(soDienThoai);
        setEmail(email);
        this.diaChi = diaChi;
        this.maHangThanhVien = maHangThanhVien;
        setDiemHienTai(diemHienTai);
        setTrangThai(trangThai);
    }
    public KhachHang(KhachHang khac) {
        this(khac.maKhachHang, khac.hoTen, khac.soDienThoai, khac.email, khac.diaChi, khac.maHangThanhVien, khac.diemHienTai, khac.trangThai);
    }
    public String getMaKhachHang() {
        return maKhachHang;
    }
    public void setMaKhachHang(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã khách hàng không được rỗng.");
        maKhachHang = giaTri.trim();
    }
    public String getHoTen() {
        return hoTen;
    }
    public void setHoTen(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Họ tên không được rỗng.");
        hoTen = giaTri.trim();
    }
    public String getSoDienThoai() {
        return soDienThoai;
    }
    public void setSoDienThoai(String giaTri) {
        if (giaTri != null && !giaTri.trim().isEmpty() && !giaTri.trim().matches("[0-9+]{9,15}"))
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        soDienThoai = giaTri == null ? null : giaTri.trim();
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String giaTri) {
        if (giaTri != null && !giaTri.trim().isEmpty() && !giaTri.contains("@")) throw new IllegalArgumentException("Email không hợp lệ.");
        email = giaTri == null ? null : giaTri.trim();
    }
    public String getDiaChi() {
        return diaChi;
    }
    public void setDiaChi(String giaTri) {
        diaChi = giaTri;
    }
    public Integer getMaHangThanhVien() {
        return maHangThanhVien;
    }
    public void setMaHangThanhVien(Integer giaTri) {
        maHangThanhVien = giaTri;
    }
    public int getDiemHienTai() {
        return diemHienTai;
    }
    public void setDiemHienTai(int giaTri) {
        if (giaTri < 0) throw new IllegalArgumentException("Điểm khách hàng không được âm.");
        diemHienTai = giaTri;
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("Hoạt động", "Không hoạt động").contains(giaTri)) throw new IllegalArgumentException("Trạng thái khách hàng không hợp lệ.");
        trangThai = giaTri;
    }
    public boolean dangHoatDong() {
        return "Hoạt động".equals(trangThai);
    }
    public void congDiem(int diem) {
        if (diem < 0) throw new IllegalArgumentException("Điểm cộng không hợp lệ.");
        diemHienTai += diem;
    }
    public void truDiem(int diem) {
        if (diem < 0 || diem > diemHienTai) throw new IllegalArgumentException("Điểm trừ không hợp lệ.");
        diemHienTai -= diem;
    }
    @Override
    public String toString() {
        return maKhachHang + " - " + hoTen;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof KhachHang && Objects.equals(maKhachHang, ((KhachHang) doiTuong).maKhachHang);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maKhachHang);
    }
}

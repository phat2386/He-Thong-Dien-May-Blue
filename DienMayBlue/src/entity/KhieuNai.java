package entity;
import java.time.LocalDateTime;
import java.util.*;
public class KhieuNai {
    private String maKhieuNai;
    private KhachHang khachHang;
    private String soDienThoaiKhach;
    private String noiDung;
    private String mucUuTien;
    private String trangThaiXuLy;
    private NhanVien nguoiTao;
    private NhanVien nguoiPhuTrach;
    private LocalDateTime hanXuLy;
    private boolean daLeoThang;
    private LocalDateTime ngayGhiNhan;
    private LocalDateTime ngayGiaiQuyet;
    public KhieuNai() {
    }
    public KhieuNai(String maKhieuNai, KhachHang khachHang, String soDienThoaiKhach, String noiDung, String mucUuTien, String trangThai, NhanVien nguoiTao, NhanVien nguoiPhuTrach, LocalDateTime hanXuLy, boolean daLeoThang, LocalDateTime ngayTao) {
        setMaKhieuNai(maKhieuNai);
        this.khachHang = khachHang;
        this.soDienThoaiKhach = soDienThoaiKhach;
        setNoiDung(noiDung);
        setMucUuTien(mucUuTien);
        setTrangThai(trangThai);
        setNguoiTao(nguoiTao);
        this.nguoiPhuTrach = nguoiPhuTrach;
        this.hanXuLy = hanXuLy;
        this.daLeoThang = daLeoThang;
        this.ngayGhiNhan = ngayTao;
    }
    public KhieuNai(KhieuNai khac) {
        this(khac.maKhieuNai, khac.khachHang, khac.soDienThoaiKhach, khac.noiDung, khac.mucUuTien, khac.trangThaiXuLy, khac.nguoiTao, khac.nguoiPhuTrach, khac.hanXuLy, khac.daLeoThang, khac.ngayGhiNhan);
        this.ngayGiaiQuyet = khac.ngayGiaiQuyet;
    }
    public String getMaKhieuNai() {
        return maKhieuNai;
    }
    public void setMaKhieuNai(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã khiếu nại không được rỗng.");
        maKhieuNai = giaTri.trim();
    }
    public KhachHang getKhachHang() {
        return khachHang;
    }
    public void setKhachHang(KhachHang giaTri) {
        khachHang = giaTri;
    }
    public String getSoDienThoaiKhach() {
        return soDienThoaiKhach;
    }
    public void setSoDienThoaiKhach(String giaTri) {
        soDienThoaiKhach = giaTri;
    }
    public String getNoiDung() {
        return noiDung;
    }
    public void setNoiDung(String giaTri) {
        if (giaTri == null || giaTri.trim().length() < 5) throw new IllegalArgumentException("Nội dung khiếu nại phải có ít nhất 5 ký tự.");
        noiDung = giaTri.trim();
    }
    public String getMucUuTien() {
        return mucUuTien;
    }
    public void setMucUuTien(String giaTri) {
        if (!Arrays.asList("Bình thường", "Khẩn cấp").contains(giaTri)) throw new IllegalArgumentException("Mức ưu tiên không hợp lệ.");
        mucUuTien = giaTri;
    }
    public String getTrangThaiXuLy() {
        return trangThaiXuLy;
    }
    public void setTrangThaiXuLy(String giaTri) {
        if (!Arrays.asList("NEW", "IN_PROGRESS", "WAITING_CUSTOMER", "RESOLVED", "CLOSED").contains(giaTri)) throw new IllegalArgumentException("Trạng thái khiếu nại không hợp lệ.");
        trangThaiXuLy = giaTri;
    }
    public String getTrangThai() {
        return trangThaiXuLy;
    }
    public void setTrangThai(String giaTri) {
        setTrangThaiXuLy(giaTri);
    }
    public NhanVien getNguoiTao() {
        return nguoiTao;
    }
    public void setNguoiTao(NhanVien giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Khiếu nại phải có người ghi nhận.");
        nguoiTao = giaTri;
    }
    public NhanVien getNguoiPhuTrach() {
        return nguoiPhuTrach;
    }
    public void setNguoiPhuTrach(NhanVien giaTri) {
        nguoiPhuTrach = giaTri;
    }
    public LocalDateTime getHanXuLy() {
        return hanXuLy;
    }
    public void setHanXuLy(LocalDateTime giaTri) {
        hanXuLy = giaTri;
    }
    public boolean isDaLeoThang() {
        return daLeoThang;
    }
    public void setDaLeoThang(boolean giaTri) {
        daLeoThang = giaTri;
    }
    public LocalDateTime getNgayGhiNhan() {
        return ngayGhiNhan;
    }
    public void setNgayGhiNhan(LocalDateTime giaTri) {
        ngayGhiNhan = giaTri;
    }
    public LocalDateTime getNgayTao() {
        return ngayGhiNhan;
    }
    public void setNgayTao(LocalDateTime giaTri) {
        setNgayGhiNhan(giaTri);
    }
    public LocalDateTime getNgayGiaiQuyet() {
        return ngayGiaiQuyet;
    }
    public void setNgayGiaiQuyet(LocalDateTime giaTri) {
        ngayGiaiQuyet = giaTri;
    }
    @Override
    public String toString() {
        return maKhieuNai + " - " + trangThaiXuLy;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof KhieuNai && Objects.equals(maKhieuNai, ((KhieuNai) doiTuong).maKhieuNai);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maKhieuNai);
    }
}

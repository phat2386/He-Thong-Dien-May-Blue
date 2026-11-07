package entity;
import java.time.*;
import java.util.*;
public class PhanCong {
    private String maPhanCong;
    private NhanVien nhanVien;
    private LocalDate ngayLamViec;
    private CaLamViec caLamViec;
    private String maNguoiPhanCong;
    private String trangThai;
    private LocalDateTime ngayTao;
    private LocalDate ngayPhanCong;
    private String ghiChu;
    public PhanCong() {
    }
    public PhanCong(String maPhanCong, NhanVien nhanVien, LocalDate ngayLamViec, CaLamViec caLamViec, String maNguoiPhanCong, String trangThai, LocalDateTime ngayTao) {
        this.maPhanCong = maPhanCong;
        setNhanVien(nhanVien);
        setNgayLamViec(ngayLamViec);
        setCaLamViec(caLamViec);
        setMaNguoiPhanCong(maNguoiPhanCong);
        setTrangThai(trangThai);
        this.ngayTao = ngayTao;
        this.ngayPhanCong = ngayTao == null ? null : ngayTao.toLocalDate();
    }
    public PhanCong(PhanCong khac) {
        this(khac.maPhanCong, khac.nhanVien, khac.ngayLamViec, khac.caLamViec, khac.maNguoiPhanCong, khac.trangThai, khac.ngayTao);
    }
    public String getMaPhanCong() {
        return maPhanCong;
    }
    public void setMaPhanCong(String giaTri) {
        maPhanCong = giaTri;
    }
    public NhanVien getNhanVien() {
        return nhanVien;
    }
    public void setNhanVien(NhanVien giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Phân công phải có nhân viên.");
        nhanVien = giaTri;
    }
    public LocalDate getNgayLamViec() {
        return ngayLamViec;
    }
    public void setNgayLamViec(LocalDate giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Ngày làm việc không được rỗng.");
        ngayLamViec = giaTri;
    }
    public CaLamViec getCaLamViec() {
        return caLamViec;
    }
    public void setCaLamViec(CaLamViec giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Phân công phải có ca làm việc.");
        caLamViec = giaTri;
    }
    public String getMaNguoiPhanCong() {
        return maNguoiPhanCong;
    }
    public void setMaNguoiPhanCong(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Người phân công không được rỗng.");
        maNguoiPhanCong = giaTri.trim();
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("Đã phân công", "Hoàn tất", "Đã hủy").contains(giaTri)) throw new IllegalArgumentException("Trạng thái phân công không hợp lệ.");
        trangThai = giaTri;
    }
    public LocalDateTime getNgayTao() {
        return ngayTao;
    }
    public void setNgayTao(LocalDateTime giaTri) {
        ngayTao = giaTri;
        ngayPhanCong = giaTri == null ? null : giaTri.toLocalDate();
    }
    public LocalDate getNgayPhanCong() {
        return ngayPhanCong;
    }
    public void setNgayPhanCong(LocalDate giaTri) {
        ngayPhanCong = giaTri;
    }
    public String getGhiChu() {
        return ghiChu;
    }
    public void setGhiChu(String giaTri) {
        ghiChu = giaTri == null ? null : giaTri.trim();
    }
    @Override
    public String toString() {
        return nhanVien + " - " + ngayLamViec + " - " + caLamViec;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof PhanCong && Objects.equals(maPhanCong, ((PhanCong) doiTuong).maPhanCong);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maPhanCong);
    }
}

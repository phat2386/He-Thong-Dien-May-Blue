package entity;
import java.time.*;
import java.util.Objects;
public class CaLamViec {
    private String maCa;
    private String loaiCa;
    private LocalDate ngayLamViec;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private boolean hoatDong;
    public CaLamViec() {
    }
    public CaLamViec(String maCa, String loaiCa, LocalTime gioBatDau, LocalTime gioKetThuc, boolean hoatDong) {
        setMaCa(maCa);
        setLoaiCa(loaiCa);
        this.gioBatDau = gioBatDau;
        this.gioKetThuc = gioKetThuc;
        this.hoatDong = hoatDong;
        kiemTraKhungGio();
    }
    public CaLamViec(CaLamViec khac) {
        this(khac.maCa, khac.loaiCa, khac.gioBatDau, khac.gioKetThuc, khac.hoatDong);
        this.ngayLamViec = khac.ngayLamViec;
    }
    public String getMaCa() {
        return maCa;
    }
    public void setMaCa(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã ca không được rỗng.");
        maCa = giaTri.trim();
    }
    public String getTenCa() {
        return loaiCa;
    }
    public void setTenCa(String giaTri) {
        setLoaiCa(giaTri);
    }
    public String getLoaiCa() {
        return loaiCa;
    }
    public void setLoaiCa(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Loại ca không được rỗng.");
        loaiCa = giaTri.trim();
    }
    public LocalDate getNgayLamViec() {
        return ngayLamViec;
    }
    public void setNgayLamViec(LocalDate giaTri) {
        ngayLamViec = giaTri;
    }
    public LocalTime getGioBatDau() {
        return gioBatDau;
    }
    public void setGioBatDau(LocalTime giaTri) {
        gioBatDau = giaTri;
        if (gioKetThuc != null) kiemTraKhungGio();
    }
    public LocalTime getGioKetThuc() {
        return gioKetThuc;
    }
    public void setGioKetThuc(LocalTime giaTri) {
        gioKetThuc = giaTri;
        if (gioBatDau != null) kiemTraKhungGio();
    }
    public boolean isHoatDong() {
        return hoatDong;
    }
    public void setHoatDong(boolean giaTri) {
        hoatDong = giaTri;
    }
    private void kiemTraKhungGio() {
        if (gioBatDau == null || gioKetThuc == null || !gioBatDau.isBefore(gioKetThuc)) throw new IllegalArgumentException("Giờ bắt đầu phải nhỏ hơn giờ kết thúc.");
    }
    @Override
    public String toString() {
        return maCa + " - " + loaiCa + " (" + gioBatDau + "-" + gioKetThuc + ")";
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof CaLamViec && Objects.equals(maCa, ((CaLamViec) doiTuong).maCa);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maCa);
    }
}

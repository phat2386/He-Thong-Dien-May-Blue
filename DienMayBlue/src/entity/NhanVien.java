package entity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public class NhanVien {
    private String maNhanVien;
    private String hoTen;
    private String soDienThoai;
    private String email;
    private LocalDate ngayVaoLam;
    private String trinhDo;
    private Double heSoLuong;
    private BigDecimal luongCoBan;
    private String phongBan;
    private String trangThai;
    public NhanVien() {
    }
    public NhanVien(String maNhanVien, String hoTen, String soDienThoai, String email, String phongBan, LocalDate ngayVaoLam, String trangThai) {
        this(maNhanVien, hoTen, soDienThoai, email, ngayVaoLam, null, null, null, phongBan, trangThai);
    }
    public NhanVien(String maNhanVien, String hoTen, String soDienThoai, String email, LocalDate ngayVaoLam, String trinhDo, Double heSoLuong, BigDecimal luongCoBan, String phongBan, String trangThai) {
        setMaNhanVien(maNhanVien);
        setHoTen(hoTen);
        setSoDienThoai(soDienThoai);
        setEmail(email);
        setNgayVaoLam(ngayVaoLam);
        setTrinhDo(trinhDo);
        setHeSoLuong(heSoLuong);
        setLuongCoBan(luongCoBan);
        setPhongBan(phongBan);
        setTrangThai(trangThai);
    }
    public NhanVien(NhanVien khac) {
        this(khac.maNhanVien, khac.hoTen, khac.soDienThoai, khac.email, khac.ngayVaoLam, khac.trinhDo, khac.heSoLuong, khac.luongCoBan, khac.phongBan, khac.trangThai);
    }
    public String getMaNhanVien() {
        return maNhanVien;
    }
    public void setMaNhanVien(String giaTri) {
        String ma = giaTri == null ? "" : giaTri.trim().toUpperCase();
        if (!ma.matches("NV\\d{3}")) throw new IllegalArgumentException("Mã nhân viên phải theo dạng NV001.");
        maNhanVien = ma;
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
        String sdt = giaTri == null ? "" : giaTri.trim();
        if (!sdt.matches("0\\d{9}")) throw new IllegalArgumentException("Số điện thoại phải gồm 10 số, bắt đầu bằng 0.");
        soDienThoai = sdt;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String giaTri) {
        if (giaTri != null && !giaTri.trim().isEmpty() && !giaTri.contains("@")) throw new IllegalArgumentException("Email không hợp lệ.");
        email = giaTri == null ? null : giaTri.trim();
    }
    public LocalDate getNgayVaoLam() {
        return ngayVaoLam;
    }
    public void setNgayVaoLam(LocalDate giaTri) {
        if (giaTri == null) throw new IllegalArgumentException("Ngày vào làm không được rỗng.");
        if (giaTri.isAfter(LocalDate.now())) throw new IllegalArgumentException("Ngày vào làm không được lớn hơn ngày hiện tại.");
        ngayVaoLam = giaTri;
    }
    public String getTrinhDo() {
        return trinhDo;
    }
    public void setTrinhDo(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) {
            trinhDo = null;
            return;
        }
        String chuanHoa = giaTri.trim();
        if (!Arrays.asList("Trung cấp", "Cao đẳng", "Đại học", "Sau đại học").contains(chuanHoa)) {
            throw new IllegalArgumentException("Trình độ không hợp lệ.");
        }
        trinhDo = chuanHoa;
    }
    public Double getHeSoLuong() {
        return heSoLuong;
    }
    public void setHeSoLuong(Double giaTri) {
        if (giaTri != null && (giaTri.doubleValue() < 1.84 || giaTri.doubleValue() > 6.5)) {
            throw new IllegalArgumentException("Hệ số lương phải nằm trong khoảng 1,84 đến 6,5.");
        }
        heSoLuong = giaTri;
    }
    public BigDecimal getLuongCoBan() {
        return luongCoBan;
    }
    public void setLuongCoBan(BigDecimal giaTri) {
        if (giaTri != null && giaTri.signum() <= 0) throw new IllegalArgumentException("Lương cơ bản phải lớn hơn 0.");
        luongCoBan = giaTri;
    }
    public String getPhongBan() {
        return phongBan;
    }
    public void setPhongBan(String giaTri) {
        phongBan = giaTri == null ? null : giaTri.trim();
    }
    public String getTrangThai() {
        return trangThai;
    }
    public void setTrangThai(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Trạng thái nhân viên không được rỗng.");
        trangThai = giaTri.trim();
    }
    public boolean dangHoatDong() {
        return "Hoạt động".equalsIgnoreCase(trangThai);
    }
    @Override
    public String toString() {
        return maNhanVien + " - " + hoTen;
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof NhanVien && Objects.equals(maNhanVien, ((NhanVien) doiTuong).maNhanVien);
    }
    @Override
    public int hashCode() {
        return Objects.hash(maNhanVien);
    }
}

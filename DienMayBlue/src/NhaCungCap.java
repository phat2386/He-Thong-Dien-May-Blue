package entity;
import java.math.BigDecimal;
import java.util.*;
public class NhaCungCap {
    private String maNhaCungCap;
    private String tenNhaCungCap;
    private String soDienThoai;
    private String diaChi;
    private String email;
    private String maSoThue;
    private String trangThai;
    private BigDecimal congNo = BigDecimal.ZERO;
    public NhaCungCap() {
    }
    public NhaCungCap(String maNhaCungCap, String tenNhaCungCap, String soDienThoai, String diaChi, String maSoThue, String trangThai, BigDecimal congNo) {
        this(maNhaCungCap, tenNhaCungCap, soDienThoai, diaChi, null, maSoThue, trangThai, congNo);
    }
    public NhaCungCap(String maNhaCungCap, String tenNhaCungCap, String soDienThoai, String diaChi, String email, String maSoThue, String trangThai, BigDecimal congNo) {
        setMaNhaCungCap(maNhaCungCap);
        setTenNhaCungCap(tenNhaCungCap);
        setSoDienThoai(soDienThoai);
        setDiaChi(diaChi);
        setEmail(email);
        setMaSoThue(maSoThue);
        setTrangThai(trangThai);
        setCongNo(congNo);
    }
    public NhaCungCap(NhaCungCap khac) {
        this(khac.maNhaCungCap, khac.tenNhaCungCap, khac.soDienThoai, khac.diaChi, khac.email, khac.maSoThue, khac.trangThai, khac.congNo);
    }
    public String getMaNhaCungCap() { return maNhaCungCap; }
    public void setMaNhaCungCap(String giaTri) {
        if (giaTri == null || !giaTri.trim().toUpperCase().matches("NCC\\d{3}")) throw new IllegalArgumentException("Mã nhà cung cấp phải theo dạng NCC001.");
        maNhaCungCap = giaTri.trim().toUpperCase();
    }
    public String getTenNhaCungCap() { return tenNhaCungCap; }
    public void setTenNhaCungCap(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Tên nhà cung cấp không được rỗng.");
        tenNhaCungCap = giaTri.trim();
    }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String giaTri) {
        if (giaTri == null || !giaTri.trim().matches("\\d{10}")) throw new IllegalArgumentException("Số điện thoại nhà cung cấp phải gồm đúng 10 chữ số.");
        soDienThoai = giaTri.trim();
    }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String giaTri) { diaChi = giaTri == null || giaTri.trim().isEmpty() ? null : giaTri.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String giaTri) {
        String e = giaTri == null ? "" : giaTri.trim();
        if (!e.isEmpty() && !e.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Email nhà cung cấp không hợp lệ.");
        email = e.isEmpty() ? null : e;
    }
    public String getMaSoThue() { return maSoThue; }
    public void setMaSoThue(String giaTri) {
        if (giaTri == null || !giaTri.trim().toUpperCase().matches("MST\\d{3}")) throw new IllegalArgumentException("Mã số thuế phải theo dạng MST001.");
        maSoThue = giaTri.trim().toUpperCase();
    }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String giaTri) {
        if (!Arrays.asList("Hoạt động", "Không hoạt động").contains(giaTri)) throw new IllegalArgumentException("Trạng thái nhà cung cấp không hợp lệ.");
        trangThai = giaTri;
    }
    public BigDecimal getCongNo() { return congNo; }
    public void setCongNo(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() < 0) throw new IllegalArgumentException("Công nợ không được âm.");
        congNo = giaTri;
    }
    @Override public String toString() { return maNhaCungCap + " - " + tenNhaCungCap; }
    @Override public boolean equals(Object doiTuong) {
        return doiTuong instanceof NhaCungCap && Objects.equals(maNhaCungCap, ((NhaCungCap) doiTuong).maNhaCungCap);
    }
    @Override public int hashCode() { return Objects.hash(maNhaCungCap); }
}

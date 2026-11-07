package entity;
import java.time.LocalDateTime;
import java.util.Objects;
public class TaiKhoan {
    private String maTaiKhoan;
    private String tenDangNhap;
    private String matKhau;
    private String trangThai;
    private int soLanDangNhapSai;
    private LocalDateTime thoiGianMoKhoa;
    private LocalDateTime lanDangNhapCuoi;
    private NhanVien nhanVien;
    private VaiTro vaiTro;
    public TaiKhoan() {
    }
    public TaiKhoan(String maTaiKhoan, String tenDangNhap, String matKhau, String trangThai, int soLanDangNhapSai, LocalDateTime thoiGianMoKhoa, LocalDateTime lanDangNhapCuoi, NhanVien nhanVien, VaiTro vaiTro) {
        setMaTaiKhoan(maTaiKhoan);
        setTenDangNhap(tenDangNhap);
        setMatKhau(matKhau);
        setTrangThai(trangThai);
        setSoLanDangNhapSai(soLanDangNhapSai);
        this.thoiGianMoKhoa = thoiGianMoKhoa;
        this.lanDangNhapCuoi = lanDangNhapCuoi;
        this.nhanVien = nhanVien;
        this.vaiTro = vaiTro;
    }
    public TaiKhoan(TaiKhoan khac) {
        this(khac.maTaiKhoan, khac.tenDangNhap, khac.matKhau, khac.trangThai, khac.soLanDangNhapSai, khac.thoiGianMoKhoa, khac.lanDangNhapCuoi, khac.nhanVien, khac.vaiTro);
    }
    public String getMaTaiKhoan() {
        return maTaiKhoan;
    }
    public void setMaTaiKhoan(String giaTri) {
        if (giaTri == null || !giaTri.trim().toUpperCase().matches("TK\\d{3}")) throw new IllegalArgumentException("Mã tài khoản phải theo dạng TK001.");
        maTaiKhoan = giaTri.trim().toUpperCase();
    }
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Tên đăng nhập không được rỗng.");
        tenDangNhap = giaTri.trim();
    }
    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mật khẩu/mã băm mật khẩu không được rỗng.");
        matKhau = giaTri;
    }
    public String getMaBamMatKhau() { return matKhau; }
    public void setMaBamMatKhau(String giaTri) { setMatKhau(giaTri); }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Trạng thái tài khoản không được rỗng.");
        trangThai = giaTri.trim();
    }
    public int getSoLanDangNhapSai() { return soLanDangNhapSai; }
    public void setSoLanDangNhapSai(int giaTri) {
        if (giaTri < 0) throw new IllegalArgumentException("Số lần đăng nhập sai không được âm.");
        soLanDangNhapSai = giaTri;
    }
    public LocalDateTime getThoiGianMoKhoa() { return thoiGianMoKhoa; }
    public void setThoiGianMoKhoa(LocalDateTime giaTri) { thoiGianMoKhoa = giaTri; }
    public LocalDateTime getKhoaDen() { return thoiGianMoKhoa; }
    public void setKhoaDen(LocalDateTime giaTri) { thoiGianMoKhoa = giaTri; }
    public LocalDateTime getLanDangNhapCuoi() { return lanDangNhapCuoi; }
    public void setLanDangNhapCuoi(LocalDateTime giaTri) { lanDangNhapCuoi = giaTri; }
    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien giaTri) { nhanVien = giaTri; }
    public VaiTro getVaiTro() { return vaiTro; }
    public void setVaiTro(VaiTro giaTri) { vaiTro = giaTri; }
    public boolean dangHoatDong() { return "Hoạt động".equalsIgnoreCase(trangThai); }
    @Override public String toString() { return tenDangNhap; }
    @Override public boolean equals(Object doiTuong) {
        return doiTuong instanceof TaiKhoan && Objects.equals(maTaiKhoan, ((TaiKhoan) doiTuong).maTaiKhoan);
    }
    @Override public int hashCode() { return Objects.hash(maTaiKhoan); }
}

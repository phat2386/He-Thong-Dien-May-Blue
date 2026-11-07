package entity;
import java.util.Objects;
public class VaiTro {
    private int maVaiTro;
    private String maQuyenVaiTro;
    private String tenVaiTro;
    private String moTa;
    public VaiTro() {
    }
    public VaiTro(int maVaiTro, String maQuyenVaiTro, String tenVaiTro, String moTa) {
        this.maVaiTro = maVaiTro;
        setMaQuyenVaiTro(maQuyenVaiTro);
        setTenVaiTro(tenVaiTro);
        this.moTa = moTa;
    }
    public VaiTro(VaiTro khac) {
        this(khac.maVaiTro, khac.maQuyenVaiTro, khac.tenVaiTro, khac.moTa);
    }
    public int getMaVaiTro() {
        return maVaiTro;
    }
    public void setMaVaiTro(int giaTri) {
        maVaiTro = giaTri;
    }
    public String getMaQuyenVaiTro() {
        return maQuyenVaiTro;
    }
    public void setMaQuyenVaiTro(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Mã vai trò không được rỗng.");
        maQuyenVaiTro = giaTri.trim();
    }
    public String getTenVaiTro() {
        return tenVaiTro;
    }
    public void setTenVaiTro(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) throw new IllegalArgumentException("Tên vai trò không được rỗng.");
        tenVaiTro = giaTri.trim();
    }
    public String getMoTa() {
        return moTa;
    }
    public void setMoTa(String giaTri) {
        moTa = giaTri;
    }
    @Override
    public String toString() {
        return tenVaiTro + " (" + maQuyenVaiTro + ")";
    }
    @Override
    public boolean equals(Object doiTuong) {
        return doiTuong instanceof VaiTro && maVaiTro == ((VaiTro) doiTuong).maVaiTro;
    }
    @Override
    public int hashCode() {
        return Objects.hash(maVaiTro);
    }
}

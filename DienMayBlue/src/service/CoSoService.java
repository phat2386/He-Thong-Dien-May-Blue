package service;
import dao.CoSoDAO;
import entity.TaiKhoan;
public abstract class CoSoService<D extends CoSoDAO> {
    protected final D truyCap;
    protected CoSoService(D truyCap) {
        this.truyCap = truyCap;
    }
    protected TaiKhoan yeuCauQuyen(String quyen) {
        TaiKhoan taiKhoan = TaiKhoanService.getTaiKhoanHienTai();
        if (taiKhoan == null || taiKhoan.getNhanVien() == null || taiKhoan.getVaiTro() == null) {
            throw new IllegalStateException("Phiên đăng nhập không hợp lệ.");
        }
        if (!TaiKhoanService.coQuyen(quyen)) {
            throw new IllegalStateException("Tài khoản chưa được cấp quyền thực hiện chức năng này.");
        }
        return taiKhoan;
    }
    protected TaiKhoan yeuCauQuyenQuanLy(String quyen) {
        return yeuCauQuyen(quyen);
    }
    protected TaiKhoan yeuCauQuyenChamSocKhachHang(String quyen) {
        return yeuCauQuyen(quyen);
    }
    protected static Object rongThanhNull(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) return null;
        return giaTri.trim();
    }
}

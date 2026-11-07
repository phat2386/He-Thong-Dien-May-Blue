package service;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import dao.PhanCongDAO;
import entity.*;
public class PhanCongService extends CoSoService<PhanCongDAO> {
    public PhanCongService() {
        super(new PhanCongDAO());
    }
    public String goiYMaCa() throws SQLException {
        return truyCap.goiYMaCa();
    }
    public List<PhanCong> phanCongTheoNgay(LocalDate ngay) throws SQLException {
        return truyCap.timPhanCongTheoNgayThucThe(ngay);
    }
    public List<NhanVien> nhanVienCoThePhanCong() throws SQLException {
        return truyCap.timNhanVienCoThePhanCongThucThe();
    }
    public List<CaLamViec> caDangHoatDong() {
        return Arrays.asList(taoCaMau(1), taoCaMau(2), taoCaMau(3));
    }
    public int demBanHangDaPhanCong(LocalDate ngay, int maCa) throws SQLException {
        return truyCap.demBanHangDaPhanCong(ngay, yeuCauCaMau(maCa).getTenCa());
    }
    public void phanCa(String maCaKyHieu, String maNhanVien, LocalDate ngay, int maCa, String ghiChu) throws Exception {
        TaiKhoan tacNhan = yeuCauQuyen("SHIFT_MANAGE");
        String maCaNhap = maCaKyHieu == null ? "" : maCaKyHieu.trim().toUpperCase();
        if (!maCaNhap.matches("CA\\d{3}")) {
            throw new IllegalArgumentException("Mã ca phải theo dạng CA001, CA002, ...");
        }
        if (ngay == null || ngay.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể phân ca cho ngày đã qua.");
        }
        truyCap.giaoDich(ketNoi -> {
            if (truyCap.maCaTonTai(ketNoi, maCaNhap)) {
                throw new IllegalArgumentException("Mã ca đã tồn tại.");
            }
            TaiKhoan taiKhoanNhanVien = truyCap.timTaiKhoanTheoNhanVien(ketNoi, maNhanVien);
            if (taiKhoanNhanVien == null || taiKhoanNhanVien.getNhanVien() == null || taiKhoanNhanVien.getVaiTro() == null) {
                throw new IllegalArgumentException("Nhân viên chưa có tài khoản/vai trò.");
            }
            if (!"Hoạt động".equals(taiKhoanNhanVien.getNhanVien().getTrangThai())) {
                throw new IllegalArgumentException("Nhân viên không ở trạng thái làm việc.");
            }
            if (!"Hoạt động".equals(taiKhoanNhanVien.getTrangThai())) {
                throw new IllegalArgumentException("Tài khoản nhân viên không hoạt động.");
            }
            String vaiTro = taiKhoanNhanVien.getVaiTro().getMaQuyenVaiTro();
            if (!"SALES".equals(vaiTro) && !"CUSTOMER_SERVICE".equals(vaiTro)) {
                throw new IllegalArgumentException("Chỉ phân ca cho Nhân viên bán hàng và Nhân viên CSKH.");
            }
            CaLamViec caLamViec = taoCaMau(maCa);
            if (caLamViec == null || !caLamViec.isHoatDong()) {
                throw new IllegalArgumentException("Ca làm việc không tồn tại hoặc đã ngừng sử dụng.");
            }
            if (truyCap.daDuocPhanCa(ketNoi, maNhanVien, ngay)) {
                throw new IllegalArgumentException("Nhân viên đã có ca làm việc trong ngày này.");
            }
            String maNguoiPhanCong = tacNhan.getNhanVien().getMaNhanVien();
            truyCap.themPhanCong(ketNoi, maCaNhap, maNhanVien, ngay, caLamViec, maNguoiPhanCong, ghiChu);
            return null;
        });
    }
    public void huyPhanCa(String maPhanCong) throws Exception {
        yeuCauQuyen("SHIFT_MANAGE");
        truyCap.giaoDich(ketNoi -> {
            PhanCong phanCong = truyCap.timPhanCongThucThe(ketNoi, maPhanCong);
            if (phanCong == null) throw new IllegalArgumentException("Không tìm thấy phân ca.");
            if (!"Đã phân công".equals(phanCong.getTrangThai())) throw new IllegalArgumentException("Phân ca này không còn ở trạng thái đang phân công.");
            truyCap.huyPhanCong(ketNoi, maPhanCong);
            return null;
        });
    }
    public void thayNhanVienPhanCa(String maPhanCong, String maNhanVienMoi) throws Exception {
        yeuCauQuyen("SHIFT_MANAGE");
        if (maNhanVienMoi == null || maNhanVienMoi.trim().isEmpty()) throw new IllegalArgumentException("Chưa chọn nhân viên thay thế.");
        truyCap.giaoDich(ketNoi -> {
            PhanCong phanCong = truyCap.timPhanCongThucThe(ketNoi, maPhanCong);
            if (phanCong == null || !"Đã phân công".equals(phanCong.getTrangThai())) throw new IllegalArgumentException("Phân ca không còn hiệu lực.");
            TaiKhoan taiKhoanMoi = truyCap.timTaiKhoanTheoNhanVien(ketNoi, maNhanVienMoi);
            if (taiKhoanMoi == null || taiKhoanMoi.getNhanVien() == null || taiKhoanMoi.getVaiTro() == null) throw new IllegalArgumentException("Nhân viên thay thế chưa có tài khoản/vai trò.");
            if (!"Hoạt động".equals(taiKhoanMoi.getTrangThai()) || !"Hoạt động".equals(taiKhoanMoi.getNhanVien().getTrangThai())) throw new IllegalArgumentException("Nhân viên thay thế không ở trạng thái hoạt động.");
            String vaiTro = taiKhoanMoi.getVaiTro().getMaQuyenVaiTro();
            if (!"SALES".equals(vaiTro) && !"CUSTOMER_SERVICE".equals(vaiTro)) throw new IllegalArgumentException("Chỉ nhân viên bán hàng hoặc CSKH được phân ca.");
            if (truyCap.daDuocPhanCa(ketNoi, maNhanVienMoi, phanCong.getNgayLamViec())) throw new IllegalArgumentException("Nhân viên thay thế đã có ca trong ngày này.");
            truyCap.thayNhanVienPhanCong(ketNoi, maPhanCong, maNhanVienMoi);
            return null;
        });
    }
    private static CaLamViec taoCaMau(int maCa) {
        if (maCa == 1) return new CaLamViec("MORNING", "Sáng", LocalTime.of(8, 0), LocalTime.of(12, 0), true);
        if (maCa == 2) return new CaLamViec("AFTERNOON", "Chiều", LocalTime.of(12, 0), LocalTime.of(17, 0), true);
        if (maCa == 3) return new CaLamViec("EVENING", "Tối", LocalTime.of(17, 0), LocalTime.of(21, 0), true);
        return null;
    }
    private static CaLamViec yeuCauCaMau(int maCa) {
        CaLamViec ca = taoCaMau(maCa);
        if (ca == null) throw new IllegalArgumentException("Ca làm việc không hợp lệ.");
        return ca;
    }
}

package service;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import dao.PhieuGiaoHangDAO;
import entity.*;
public class PhieuGiaoHangService extends CoSoService<PhieuGiaoHangDAO> {
    public static final BigDecimal PHI_LAP_DAT = new BigDecimal("100000");
    public PhieuGiaoHangService() {
        super(new PhieuGiaoHangDAO());
    }
    public String goiYMaPhieuGiao() throws SQLException {
        return truyCap.goiYMaPhieuGiao();
    }
    protected void damBaoDuDieuKienBanHang(Connection ketNoi, TaiKhoan taiKhoan) throws Exception {
        Map<String, Object> trangThai = truyCap.timTrangThaiThaoTacTaiKhoan(ketNoi, taiKhoan.getMaTaiKhoan());
        if (trangThai == null) {
            throw new IllegalStateException("Tài khoản không còn tồn tại trong hệ thống.");
        }
        if (!"Hoạt động".equals(String.valueOf(trangThai.get("account_status")))) {
            throw new IllegalStateException("Tài khoản hiện không ở trạng thái hoạt động.");
        }
        if (!"Hoạt động".equals(String.valueOf(trangThai.get("employee_status")))) {
            throw new IllegalStateException("Nhân viên hiện không ở trạng thái làm việc.");
        }
        if ("SALES".equals(String.valueOf(trangThai.get("role_code"))) && !truyCap.coCaHienTai(ketNoi, taiKhoan.getNhanVien().getMaNhanVien())) {
            throw new IllegalStateException("Bạn chưa có ca làm việc hợp lệ tại thời điểm hiện tại.");
        }
    }
    public BigDecimal tinhPhiGiaoHang(boolean lapDat) {
        return lapDat ? PHI_LAP_DAT : BigDecimal.ZERO;
    }
    public String taoPhieuGiaoHang(String maPhieuGiao, String maHoaDon, String maNhanVienGiaoHang, String diaChi, LocalDate ngayGiao, boolean lapDat) throws Exception {
        String maPhieu = maPhieuGiao == null ? "" : maPhieuGiao.trim().toUpperCase(Locale.ROOT);
        if (!maPhieu.matches("GH\\d{5}")) {
            throw new IllegalArgumentException("Mã phiếu giao hàng phải theo dạng GH00001.");
        }
        if (maHoaDon == null || maHoaDon.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn hóa đơn.");
        }
        if (maNhanVienGiaoHang == null || maNhanVienGiaoHang.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn nhân viên giao hàng.");
        }
        if (diaChi == null || diaChi.trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ giao hàng không được rỗng.");
        }
        if (ngayGiao == null || ngayGiao.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày giao hàng không được ở trong quá khứ.");
        }
        TaiKhoan taiKhoan = yeuCauQuyen("DELIVERY_CREATE");
        String hoaDon = maHoaDon.trim();
        String nhanVien = maNhanVienGiaoHang.trim();
        BigDecimal phi = tinhPhiGiaoHang(lapDat);
        return truyCap.giaoDich(ketNoi -> {
            damBaoDuDieuKienBanHang(ketNoi, taiKhoan);
            HoaDon hd = truyCap.timThucTheHoaDon(ketNoi, hoaDon);
            if (hd == null) {
                throw new IllegalArgumentException("Không tìm thấy hóa đơn.");
            }
            if (!"Đã thanh toán".equals(hd.getTrangThai())) {
                throw new IllegalArgumentException("Chỉ hóa đơn đã thanh toán mới được tạo phiếu giao hàng.");
            }
            KhachHang kh = hd.getKhachHang();
            if (kh == null || "KH000".equals(kh.getMaKhachHang()) || !truyCap.khachHangLaThanhVienHoatDong(ketNoi, kh.getMaKhachHang())) {
                throw new IllegalArgumentException("Chỉ khách hàng thành viên đang hoạt động mới được lập phiếu giao hàng.");
            }
            if (truyCap.phieuGiaoTonTaiChoHoaDon(ketNoi, hoaDon)) {
                throw new IllegalArgumentException("Hóa đơn này đã có phiếu giao hàng.");
            }
            if (truyCap.timChiTietConLaiDeGiaoThucThe(ketNoi, hoaDon).isEmpty()) {
                throw new IllegalArgumentException("Toàn bộ sản phẩm của hóa đơn đã được đổi trả/hoàn tiền, không còn hàng để giao.");
            }
            if (truyCap.maPhieuGiaoTonTai(ketNoi, maPhieu)) {
                throw new IllegalArgumentException("Mã phiếu giao hàng đã tồn tại.");
            }
            if (!truyCap.nhanVienDuDieuKienGiaoHang(ketNoi, ngayGiao, nhanVien)) {
                throw new IllegalArgumentException("Nhân viên giao hàng đang có ca làm tại cửa hàng trong ngày đã chọn.");
            }
            truyCap.themPhieuGiaoHang(ketNoi, maPhieu, hoaDon, nhanVien, diaChi.trim(), ngayGiao, lapDat, phi);
            return maPhieu;
        });
    }
    public List<NhanVien> layNhanVienGiaoHangPhuHop(LocalDate ngayGiao) throws Exception {
        if (ngayGiao == null) {
            return Collections.emptyList();
        }
        return truyCap.timNhanVienGiaoHangPhuHopThucThe(ngayGiao);
    }
    public List<NhanVien> layNhanVienGiaoHangPhuHop(String maPhieuGiao) throws Exception {
        if (maPhieuGiao == null || maPhieuGiao.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return truyCap.timNhanVienGiaoHangPhuHopThucThe(maPhieuGiao.trim());
    }
    public void phanCongGiaoHang(String maPhieuGiao, String maNhanVien, boolean xacNhanPhanCongLai) throws Exception {
        if (maPhieuGiao == null || maPhieuGiao.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn phiếu giao hàng.");
        }
        if (maNhanVien == null || maNhanVien.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn nhân viên giao hàng.");
        }
        TaiKhoan taiKhoan = yeuCauQuyen("DELIVERY_ASSIGN");
        String maPhieu = maPhieuGiao.trim();
        String nhanVien = maNhanVien.trim();
        truyCap.giaoDich(ketNoi -> {
            damBaoDuDieuKienBanHang(ketNoi, taiKhoan);
            PhieuGiaoHang phieu = truyCap.timThucThePhieuGiaoHang(ketNoi, maPhieu);
            if (phieu == null) {
                throw new IllegalArgumentException("Không tìm thấy phiếu giao hàng.");
            }
            if (!"WAITING".equals(phieu.getTrangThai())) {
                throw new IllegalArgumentException("Chỉ phiếu đang chờ mới được đổi nhân viên giao hàng.");
            }
            String cu = phieu.getNhanVienGiaoHang().getMaNhanVien();
            if (nhanVien.equals(cu)) {
                throw new IllegalArgumentException("Phiếu đã được giao cho nhân viên này.");
            }
            if (!xacNhanPhanCongLai) {
                throw new IllegalArgumentException("Cần xác nhận trước khi đổi nhân viên giao hàng.");
            }
            if (!truyCap.nhanVienDuDieuKienGiaoHang(ketNoi, phieu.getNgayGiao(), nhanVien)) {
                throw new IllegalArgumentException("Nhân viên được chọn đang có ca làm tại cửa hàng trong ngày giao.");
            }
            if (truyCap.phanCongGiaoHang(ketNoi, maPhieu, nhanVien) != 1) {
                throw new IllegalStateException("Phiếu vừa được thay đổi, vui lòng tải lại.");
            }
            return null;
        });
    }
    public void hoanTatGiaoHang(String maPhieuGiao) throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyen("DELIVERY_MANAGE");
        String maPhieu = maPhieuGiao == null ? "" : maPhieuGiao.trim();
        if (maPhieu.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn phiếu giao hàng.");
        }
        truyCap.giaoDich(ketNoi -> {
            damBaoDuDieuKienBanHang(ketNoi, taiKhoan);
            PhieuGiaoHang phieu = truyCap.timThucThePhieuGiaoHang(ketNoi, maPhieu);
            if (phieu == null) {
                throw new IllegalArgumentException("Không tìm thấy phiếu giao hàng.");
            }
            if (!"WAITING".equals(phieu.getTrangThai())) {
                throw new IllegalArgumentException("Phiếu này đã hoàn tất.");
            }
            truyCap.capNhatTrangThaiGiaoHang(ketNoi, maPhieu, "Hoàn tất");
            return null;
        });
    }
    public List<HoaDon> layHoaDonCoTheGiao() throws Exception {
        return truyCap.timHoaDonCoTheGiaoThucThe();
    }
    public List<ChiTietHoaDon> layChiTietConLaiDeGiao(String maHoaDon) throws Exception {
        return truyCap.timChiTietConLaiDeGiaoThucThe(maHoaDon);
    }
    public KhachHang layKhachHangTheoHoaDon(String maHoaDon) throws Exception {
        return truyCap.timKhachHangTheoHoaDonThucThe(maHoaDon);
    }
    public List<PhieuGiaoHang> layDanhSachGiaoHang(String phamVi, String maNhanVien, String maTrangThai, LocalDate ngayGiao) throws Exception {
        return truyCap.timDanhSachGiaoHangThucThe(phamVi, maNhanVien, maTrangThai, ngayGiao);
    }
}

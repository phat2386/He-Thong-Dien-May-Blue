package service;
import java.math.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import dao.HoaDonDAO;
import entity.*;
public class HoaDonService extends CoSoService<HoaDonDAO> {
    public static final BigDecimal TY_LE_THUONG_MAC_DINH = new BigDecimal("0.03");
    public HoaDonService() {
        super(new HoaDonDAO());
    }
    public String goiYMaHoaDon() throws SQLException {
        return truyCap.goiYMaHoaDon();
    }
    public static class MatHangHoaDon {
        public final String maSanPham;
        public final int soLuong;
        public MatHangHoaDon(String maSanPham, int soLuong) {
            this.maSanPham = maSanPham;
            this.soLuong = soLuong;
        }
    }
    public void damBaoDuDieuKienBanHang() throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyen("INVOICE_CREATE");
        truyCap.giaoDich(ketNoi -> {
            damBaoDuDieuKienBanHang(ketNoi, taiKhoan);
            return null;
        });
    }
    protected void damBaoDuDieuKienBanHang(Connection ketNoi, TaiKhoan taiKhoan) throws Exception {
        Map<String, Object> trangThai = truyCap.timTrangThaiBanHangTaiKhoan(ketNoi, taiKhoan.getMaTaiKhoan());
        if (trangThai == null) throw new IllegalStateException("Tài khoản không còn tồn tại trong hệ thống.");
        if (!"Hoạt động".equals(String.valueOf(trangThai.get("account_status")))) {
            throw new IllegalStateException("Tài khoản hiện không ở trạng thái hoạt động.");
        }
        if (!"Hoạt động".equals(String.valueOf(trangThai.get("employee_status")))) {
            throw new IllegalStateException("Nhân viên hiện không ở trạng thái làm việc.");
        }
        Object coPhanCa = trangThai.get("can_shift");
        boolean yeuCauCa = Boolean.TRUE.equals(coPhanCa) || coPhanCa instanceof Number && ((Number) coPhanCa).intValue() == 1;
        if (!yeuCauCa) return;
        if (!truyCap.coCaHienTai(ketNoi, taiKhoan.getNhanVien().getMaNhanVien())) {
            throw new IllegalStateException("Bạn chưa có ca làm việc hợp lệ tại thời điểm hiện tại.");
        }
    }
    public String taoHoaDon(String maHoaDonNhap, String maKhachHang, List<MatHangHoaDon> cacMatHang, String phuongThucThanhToan, String maThamChieuGiaoDich, String ghiChu) throws Exception {
        if (cacMatHang == null || cacMatHang.isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn chưa có sản phẩm.");
        }
        String maHoaDonChuan = maHoaDonNhap == null ? "" : maHoaDonNhap.trim().toUpperCase(Locale.ROOT);
        if (!maHoaDonChuan.matches("HD\\d{5}")) {
            throw new IllegalArgumentException("Mã hóa đơn phải theo dạng HD00001.");
        }
        TaiKhoan taiKhoan = yeuCauQuyen("INVOICE_CREATE");
        String maPhuongThuc = phuongThucThanhToan == null ? "" : phuongThucThanhToan.trim();
        if (!Arrays.asList("Tiền mặt", "Chuyển khoản").contains(maPhuongThuc)) {
            throw new IllegalArgumentException("Hình thức thanh toán không hợp lệ.");
        }
        LinkedHashMap<String, Integer> soLuongTheoSanPham = new LinkedHashMap<>();
        for (MatHangHoaDon matHang : cacMatHang) {
            if (matHang == null || matHang.maSanPham == null || matHang.maSanPham.trim().isEmpty() || matHang.soLuong <= 0) {
                throw new IllegalArgumentException("Danh sách sản phẩm không hợp lệ.");
            }
            soLuongTheoSanPham.merge(matHang.maSanPham.trim(), matHang.soLuong, Integer::sum);
        }
        return truyCap.giaoDich(ketNoi -> {
            damBaoDuDieuKienBanHang(ketNoi, taiKhoan);
            String maNhanVien = taiKhoan.getNhanVien().getMaNhanVien();
            BigDecimal tyLeGiamThanhVien = BigDecimal.ZERO;
            BigDecimal heSoDiem = BigDecimal.ONE;
            String maKhachHangChuan = maKhachHang == null ? "" : maKhachHang.trim();
            if (!maKhachHangChuan.isEmpty()) {
                KhachHang khachHang = truyCap.timKhachHangThucThe(ketNoi, maKhachHangChuan);
                if (khachHang == null || !khachHang.dangHoatDong()) throw new IllegalArgumentException("Khách hàng không tồn tại hoặc đã ngừng hoạt động.");
                String hangThanhVien = truyCap.hangThanhVien(ketNoi, maKhachHangChuan);
                tyLeGiamThanhVien = ChinhSachThanhVien.tyLeGiam(hangThanhVien);
                heSoDiem = ChinhSachThanhVien.heSoDiem(hangThanhVien);
            }
            String maHoaDon = maHoaDonChuan;
            if (truyCap.maHoaDonTonTai(ketNoi, maHoaDon)) {
                throw new IllegalArgumentException("Mã hóa đơn đã tồn tại.");
            }
            BigDecimal tamTinh = BigDecimal.ZERO;
            BigDecimal tongTienGiam = BigDecimal.ZERO;
            List<DongHoaDonTinhToan> cacDongTinhToan = new ArrayList<>();
            for (Map.Entry<String, Integer> muc : soLuongTheoSanPham.entrySet()) {
                String maSanPham = muc.getKey();
                int soLuong = muc.getValue();
                SanPham sanPham = truyCap.timSanPhamThucThe(ketNoi, maSanPham);
                if (sanPham == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm " + maSanPham);
                if (!"Hoạt động".equals(sanPham.getTrangThai())) throw new IllegalArgumentException("Sản phẩm " + maSanPham + " không còn kinh doanh hoặc đã hết hàng.");
                int tonKho = sanPham.getSoLuongTonKho();
                if (soLuong > tonKho) {
                    throw new IllegalArgumentException("Sản phẩm " + maSanPham + " chỉ còn " + tonKho + " sản phẩm.");
                }
                BigDecimal giaBan = sanPham.getGiaBan();
                BigDecimal giaVon = sanPham.getGiaVon();
                BigDecimal tienTruocGiam = giaBan.multiply(BigDecimal.valueOf(soLuong));
                int maDanhMuc = sanPham.getMaDanhMuc();
                BigDecimal giamKhuyenMai = tinhGiamGiaTotNhat(ketNoi, maSanPham, maDanhMuc, tienTruocGiam, soLuong);
                BigDecimal giamThanhVien = tienTruocGiam.multiply(tyLeGiamThanhVien).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal tienGiam = giamKhuyenMai.max(giamThanhVien).min(tienTruocGiam);
                BigDecimal thanhTienDong = tienTruocGiam.subtract(tienGiam).max(BigDecimal.ZERO);
                cacDongTinhToan.add(new DongHoaDonTinhToan(maSanPham, soLuong, giaBan, giaVon, tienGiam, thanhTienDong));
                tamTinh = tamTinh.add(tienTruocGiam);
                tongTienGiam = tongTienGiam.add(tienGiam);
            }
            BigDecimal tong = tamTinh.subtract(tongTienGiam);
            if (tong.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Tổng thanh toán phải lớn hơn 0.");
            }
            String maKhachHangLuu = maKhachHangChuan == null || maKhachHangChuan.isBlank() ? "KH000" : maKhachHangChuan;
            if ("KH000".equals(maKhachHangLuu)) truyCap.yeuCauKhachVangLaiTonTai(ketNoi);
            truyCap.themHoaDon(ketNoi, maHoaDon, maKhachHangLuu, maNhanVien, tamTinh, tongTienGiam, tong, maPhuongThuc, rongThanhNull(ghiChu));
            for (DongHoaDonTinhToan dong : cacDongTinhToan) {
                int soDongCapNhat = truyCap.truTonKhoSanPham(ketNoi, dong.maSanPham, dong.soLuong);
                if (soDongCapNhat != 1) {
                    throw new IllegalArgumentException("Tồn kho của sản phẩm " + dong.maSanPham + " vừa thay đổi. Vui lòng kiểm tra lại hóa đơn.");
                }
                truyCap.themChiTietHoaDon(ketNoi, maHoaDon, dong.maSanPham, dong.soLuong, dong.giaBan, dong.giaVon, dong.tienGiam, dong.thanhTienDong);
            }
            truyCap.themThanhToan(ketNoi, maHoaDon, maPhuongThuc, tong, rongThanhNull(maThamChieuGiaoDich));
            if (!maKhachHangChuan.isEmpty()) {
                BigDecimal diemCoSo = tong.divide(ChinhSachThanhVien.SO_TIEN_MOT_DIEM, 0, RoundingMode.DOWN);
                int diem = diemCoSo.multiply(heSoDiem).setScale(0, RoundingMode.DOWN).intValue();
                if (diem > 0) {
                    truyCap.congDiemKhachHang(ketNoi, maKhachHangChuan, diem);
                    int diemMoi = truyCap.diemThanhVien(ketNoi, maKhachHangChuan);
                    truyCap.capNhatHangKhachHang(ketNoi, maKhachHangChuan, ChinhSachThanhVien.hangTheoDiem(diemMoi));
                }
            }
            return maHoaDon;
        });
    }
    private BigDecimal tinhGiamGiaTotNhat(Connection ketNoi, String maSanPham, int maDanhMuc, BigDecimal tienTruocGiam, int soLuong) throws SQLException {
        List<KhuyenMai> cacDong = truyCap.timKhuyenMaiDangHoatDongThucThe(ketNoi, maSanPham, maDanhMuc);
        BigDecimal giamTotNhat = BigDecimal.ZERO;
        for (KhuyenMai dong : cacDong) {
            String loaiGiam = dong.getLoaiGiam();
            BigDecimal giaTri = dong.getGiaTriGiam();
            BigDecimal tienGiam = "PERCENT".equals(loaiGiam) ? tienTruocGiam.multiply(giaTri).divide(new BigDecimal( "100"), 2, RoundingMode.HALF_UP) : giaTri.multiply(BigDecimal.valueOf(soLuong));
            if (tienGiam.compareTo(giamTotNhat) > 0) giamTotNhat = tienGiam;
        }
        return giamTotNhat.min(tienTruocGiam);
    }
    public void chotBangKeThuong(LocalDate tuNgay, LocalDate denNgay) throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyen("BONUS_FINALIZE");
        if (tuNgay == null || denNgay == null || tuNgay.isAfter(denNgay)) {
            throw new IllegalArgumentException("Kỳ chốt thưởng không hợp lệ.");
        }
        truyCap.giaoDich(ketNoi -> {
            if (truyCap.demBangKeTrungDaChot(ketNoi, tuNgay, denNgay) > 0) {
                throw new IllegalStateException("Khoảng thời gian này bị trùng với một bảng kê thưởng đã chốt.");
            }
            List<Map<String, Object>> cacDong = truyCap.timThuongMoTheoNhanVien(ketNoi, tuNgay, denNgay);
            if (cacDong.isEmpty()) {
                throw new IllegalStateException("Không có dữ liệu thưởng chưa chốt trong kỳ đã chọn.");
            }
            truyCap.danhDauThuongDaChot(ketNoi, tuNgay, denNgay);
            return null;
        });
    }
    public List<KhachHang> layKhachHangHoatDong() throws SQLException {
        return truyCap.danhSachKhachHangHoatDongThucThe();
    }
    public List<SanPham> laySanPhamConHang() throws SQLException {
        return truyCap.danhSachSanPhamConHangThucThe();
    }
    public KhachHang layThongTinKhachHang(String maKhachHang) throws SQLException {
        return truyCap.timKhachHangThucThe(maKhachHang);
    }
    public String layHangThanhVien(String maKhachHang) throws SQLException {
        return truyCap.hangThanhVien(maKhachHang);
    }
    public BigDecimal layHeSoDiemThanhVien(String maKhachHang) throws SQLException {
        return ChinhSachThanhVien.heSoDiem(truyCap.hangThanhVien(maKhachHang));
    }
    public SanPham laySanPhamCoTheBan(String maSanPham) throws SQLException {
        return truyCap.timSanPhamThucThe(maSanPham);
    }
    public List<KhuyenMai> layKhuyenMaiDangHoatDong(String maSanPham, int maDanhMuc) throws SQLException {
        return truyCap.timKhuyenMaiDangHoatDongThucThe(maSanPham, maDanhMuc);
    }
    public BigDecimal layTyLeGiamThanhVien(String maKhachHang) throws SQLException {
        return ChinhSachThanhVien.tyLeGiam(truyCap.hangThanhVien(maKhachHang));
    }
    public BigDecimal layDoanhThuThuanHomNay() throws SQLException {
        return truyCap.doanhThuThuanHomNay();
    }
    public int laySoHoaDonHomNay() throws SQLException {
        return truyCap.demHoaDonHomNay();
    }
    public int laySoPhieuGiaoDangChuanBi() throws SQLException {
        return truyCap.demPhieuGiaoDangChuanBi();
    }
    public int laySoPhieuDoiTraDangXuLy() throws SQLException {
        return truyCap.demPhieuDoiTraDangXuLy();
    }
    public List<Map<String, Object>> layHoaDonGanDay(int soLuong) throws SQLException {
        return truyCap.danhSachHoaDonGanDay(soLuong);
    }
    public List<HoaDon> layDanhSachHoaDonQuanLy() throws SQLException {
        return truyCap.danhSachHoaDonQuanLyThucThe();
    }
    public HoaDon layChiTietHoaDonQuanLy(String maHoaDon) throws SQLException {
        return truyCap.timHoaDonThucThe(maHoaDon);
    }
    public List<ChiTietHoaDon> layDongChiTietHoaDonQuanLy(String maHoaDon) throws SQLException {
        return truyCap.dongChiTietHoaDonQuanLyThucThe(maHoaDon);
    }
    private static final class DongHoaDonTinhToan {
        private final String maSanPham;
        private final int soLuong;
        private final BigDecimal giaBan;
        private final BigDecimal giaVon;
        private final BigDecimal tienGiam;
        private final BigDecimal thanhTienDong;
        private DongHoaDonTinhToan(String maSanPham, int soLuong, BigDecimal giaBan, BigDecimal giaVon, BigDecimal tienGiam, BigDecimal thanhTienDong) {
            this.maSanPham = maSanPham;
            this.soLuong = soLuong;
            this.giaBan = giaBan;
            this.giaVon = giaVon;
            this.tienGiam = tienGiam;
            this.thanhTienDong = thanhTienDong;
        }
    }
}

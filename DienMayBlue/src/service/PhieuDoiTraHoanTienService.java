package service;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import dao.PhieuDoiTraHoanTienDAO;
import entity.*;
public class PhieuDoiTraHoanTienService extends CoSoService<PhieuDoiTraHoanTienDAO> {
    public static final BigDecimal TY_LE_THUONG_MAC_DINH = new BigDecimal("0.03");
    private static final BigDecimal HAN_MUC_DUYET_HOAN_TIEN_MAC_DINH = new BigDecimal("5000000");
    public static final class MatHangDoiTra {
        public final String maSanPham;
        public final int soLuong;
        public MatHangDoiTra(String maSanPham, int soLuong) {
            this.maSanPham = maSanPham;
            this.soLuong = soLuong;
        }
    }
    public PhieuDoiTraHoanTienService() {
        super(new PhieuDoiTraHoanTienDAO());
    }
    public String goiYMaPhieuDoiTra() throws SQLException {
        return truyCap.goiYMaPhieuDoiTra();
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
    public BigDecimal tinhTienHoan(String maHoaDon, String maSanPham, int soLuong) throws Exception {
        ChiTietHoaDon chiTiet = truyCap.timChiTietHoaDonDeHoanTienThucThe(maHoaDon, maSanPham);
        if (chiTiet == null) {
            throw new IllegalArgumentException("Không tìm thấy chi tiết hóa đơn.");
        }
        if (soLuong <= 0 || soLuong > chiTiet.getSoLuong()) {
            throw new IllegalArgumentException("Số lượng đổi trả không hợp lệ.");
        }
        return tinhHoanTienTheoTyLe(chiTiet.getThanhTien(), chiTiet.getSoLuong(), soLuong);
    }
    private BigDecimal tinhHoanTienTheoTyLe(BigDecimal thanhTienDong, int soLuongDaMua, int soLuongTra) {
        return thanhTienDong.multiply(BigDecimal.valueOf(soLuongTra)).divide(BigDecimal.valueOf(soLuongDaMua), 2, RoundingMode.HALF_UP);
    }
    public BigDecimal tinhTongTienHoan(String maHoaDon, List<MatHangDoiTra> matHang) throws Exception {
        if (matHang == null || matHang.isEmpty()) return BigDecimal.ZERO;
        BigDecimal tong = BigDecimal.ZERO;
        Set<String> daChon = new HashSet<>();
        for (MatHangDoiTra m : matHang) {
            if (m == null || !daChon.add(m.maSanPham)) throw new IllegalArgumentException("Danh sách sản phẩm đổi trả bị trùng.");
            tong = tong.add(tinhTienHoan(maHoaDon, m.maSanPham, m.soLuong));
        }
        return tong;
    }
    public String taoPhieuDoiTra(String maPhieu, String maHoaDon, List<MatHangDoiTra> matHang, String loaiLyDo, String lyDo, String tinhTrang, boolean conTemMac, boolean dayDuHop, boolean duPhuKien) throws Exception {
        String maPhieuChuanHoa = maPhieu == null ? "" : maPhieu.trim().toUpperCase(Locale.ROOT);
        if (maPhieuChuanHoa.isEmpty() || maPhieuChuanHoa.length() > 7) throw new IllegalArgumentException("Mã phiếu là bắt buộc và tối đa 7 ký tự.");
        if (maHoaDon == null || maHoaDon.trim().isEmpty()) throw new IllegalArgumentException("Chưa chọn hóa đơn.");
        if (matHang == null || matHang.isEmpty()) throw new IllegalArgumentException("Phải chọn ít nhất một sản phẩm đổi trả.");
        if (lyDo == null || lyDo.trim().isEmpty()) throw new IllegalArgumentException("Cần nhập lý do đổi trả/hoàn tiền.");
        if (tinhTrang == null || tinhTrang.trim().isEmpty()) throw new IllegalArgumentException("Cần ghi nhận tình trạng thực tế của sản phẩm.");
        if (!"Lỗi nhà sản xuất".equals(loaiLyDo)) throw new IllegalArgumentException("Hoàn tiền trong 7 ngày chỉ áp dụng cho sản phẩm lỗi do nhà sản xuất.");
        if (!conTemMac || !dayDuHop || !duPhuKien) throw new IllegalArgumentException("Không đủ điều kiện đổi trả: sản phẩm phải còn tem/mác, đầy đủ hộp và phụ kiện.");
        TaiKhoan taiKhoan = yeuCauQuyen("RETURN_CREATE");
        String maHoaDonChuanHoa = maHoaDon.trim();
        return truyCap.giaoDich(ketNoi -> {
            damBaoDuDieuKienBanHang(ketNoi, taiKhoan);
            if (truyCap.maPhieuDoiTraTonTai(ketNoi, maPhieuChuanHoa)) throw new IllegalArgumentException("Mã phiếu đổi trả đã tồn tại.");
            HoaDon hoaDon = truyCap.timThucTheHoaDon(ketNoi, maHoaDonChuanHoa);
            if (hoaDon == null || !"Đã thanh toán".equals(hoaDon.getTrangThai())) throw new IllegalArgumentException("Hóa đơn không tồn tại hoặc chưa thanh toán.");
            if (hoaDon.getNgayLap().toLocalDate().plusDays(7).isBefore(LocalDate.now())) throw new IllegalArgumentException("Hóa đơn đã quá thời hạn đổi trả 7 ngày.");
            if (truyCap.phieuDoiTraTonTaiChoHoaDon(ketNoi, maHoaDonChuanHoa)) throw new IllegalArgumentException("Hóa đơn này đã có phiếu đổi trả/hoàn tiền.");
            BigDecimal tongTienHoan = BigDecimal.ZERO;
            Set<String> daChon = new HashSet<>();
            List<ChiTietHoaDon> chiTietDaChon = new ArrayList<>();
            for (MatHangDoiTra m : matHang) {
                if (m == null || !daChon.add(m.maSanPham)) throw new IllegalArgumentException("Danh sách sản phẩm đổi trả bị trùng.");
                ChiTietHoaDon chiTiet = truyCap.timThucTheChiTietHoaDon(ketNoi, maHoaDonChuanHoa, m.maSanPham);
                if (chiTiet == null || m.soLuong <= 0 || m.soLuong > chiTiet.getSoLuong()) throw new IllegalArgumentException("Chi tiết hóa đơn hoặc số lượng đổi trả không hợp lệ.");
                BigDecimal tienDong = tinhHoanTienTheoTyLe(chiTiet.getThanhTien(), chiTiet.getSoLuong(), m.soLuong);
                tongTienHoan = tongTienHoan.add(tienDong);
                chiTietDaChon.add(new ChiTietHoaDon(chiTiet.getHoaDon(), chiTiet.getSanPham(), m.soLuong, chiTiet.getDonGia(), chiTiet.getGiaVonTaiThoiDiemBan(), chiTiet.getTienGiam(), tienDong));
            }
            boolean canDuyet = tongTienHoan.compareTo(layHanMucDuyetHoanTien()) > 0;
            String trangThai = canDuyet ? "WAITING_APPROVAL" : "PENDING";
            truyCap.themPhieuDoiTra(ketNoi, maPhieuChuanHoa, maHoaDonChuanHoa, loaiLyDo, lyDo.trim(), tinhTrang.trim(), tongTienHoan, trangThai, conTemMac, dayDuHop, duPhuKien);
            for (ChiTietHoaDon chiTiet : chiTietDaChon) truyCap.themChiTietDoiTra(ketNoi, maPhieuChuanHoa, chiTiet.getSanPham().getMaSanPham(), chiTiet.getSoLuong(), chiTiet.getThanhTien());
            if (canDuyet) truyCap.themYeuCauDuyetHoanTien(ketNoi, maPhieuChuanHoa);
            else hoanTatHoanTien(ketNoi, maPhieuChuanHoa);
            return maPhieuChuanHoa;
        });
    }
    private void hoanTatHoanTien(Connection ketNoi, String maPhieu) throws Exception {
        PhieuDoiTraHoanTien yeuCau = truyCap.timThucThePhieuDoiTra(ketNoi, maPhieu);
        if (yeuCau == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu đổi trả.");
        }
        if ("Hoàn tất".equals(yeuCau.getTrangThai())) {
            throw new IllegalStateException("Phiếu này đã được hoàn tiền trước đó.");
        }
        if ("REJECTED".equals(yeuCau.getTrangThai())) {
            throw new IllegalStateException("Phiếu đã bị từ chối.");
        }
        HoaDon hoaDon = yeuCau.getHoaDon();
        List<ChiTietHoaDon> chiTietTra = truyCap.timChiTietPhieuDoiTraThucThe(ketNoi, maPhieu);
        if (hoaDon == null || chiTietTra.isEmpty()) throw new IllegalArgumentException("Thiếu hóa đơn hoặc sản phẩm của phiếu đổi trả.");
        for (ChiTietHoaDon dong : chiTietTra) truyCap.hoanTonKhoSanPham(ketNoi, dong.getSanPham().getMaSanPham(), dong.getSoLuong());
        String maKhachHang = hoaDon.getKhachHang() == null ? null : hoaDon.getKhachHang().getMaKhachHang();
        if (maKhachHang != null) {
            int diemCanThuHoi = yeuCau.getGiaTriHoan().divide(ChinhSachThanhVien.SO_TIEN_MOT_DIEM, 0, RoundingMode.DOWN).intValue();
            int diemHienTai = truyCap.diemHienTaiKhachHang(ketNoi, maKhachHang);
            int diemThuHoi = Math.min(diemCanThuHoi, diemHienTai);
            if (diemThuHoi > 0) {
                truyCap.truDiemKhachHang(ketNoi, maKhachHang, diemThuHoi);
                int diemSau = truyCap.diemHienTaiKhachHang(ketNoi, maKhachHang);
                truyCap.capNhatHangKhachHang(ketNoi, maKhachHang, ChinhSachThanhVien.hangTheoDiem(diemSau));
            }
        }
        BigDecimal tyLeThuong = truyCap.timTyLeThuong(ketNoi, hoaDon.getMaHoaDon(), hoaDon.getNhanVien().getMaNhanVien(), TY_LE_THUONG_MAC_DINH);
        if (yeuCau.getGiaTriHoan().multiply(tyLeThuong).signum() > 0) {
            truyCap.moLaiThuongHoaDon(ketNoi, hoaDon.getMaHoaDon());
        }
        truyCap.danhDauDoiTraHoanTat(ketNoi, maPhieu);
    }
    public void duyetHoanTien(String maPhieu, boolean dongY, String lyDo) throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyen("REFUND_APPROVE");
        if (!dongY && (lyDo == null || lyDo.trim().isEmpty())) {
            throw new IllegalArgumentException("Cần nhập lý do từ chối.");
        }
        String maQuanLy = taiKhoan.getNhanVien().getMaNhanVien();
        truyCap.giaoDich(ketNoi -> {
            PhieuDoiTraHoanTien yeuCau = truyCap.timThucThePhieuDoiTra(ketNoi, maPhieu);
            if (yeuCau == null || !"WAITING_APPROVAL".equals(yeuCau.getTrangThai())) {
                throw new IllegalStateException("Phiếu không còn ở trạng thái chờ duyệt.");
            }
            if (yeuCau.getGiaTriHoan().compareTo(layHanMucDuyetHoanTien()) <= 0) {
                throw new IllegalStateException("Phiếu này không thuộc nhóm vượt hạn mức cần Quản lý duyệt.");
            }
            int thayDoi = truyCap.quyetDinhDuyetHoanTien(ketNoi, maPhieu, dongY ? "APPROVED" : "REJECTED", maQuanLy, dongY ? null : lyDo.trim());
            if (thayDoi != 1) {
                throw new IllegalStateException("Phiếu đã được Quản lý khác xử lý.");
            }
            if (dongY) {
                truyCap.danhDauDoiTraDaDuyet(ketNoi, maPhieu);
                hoanTatHoanTien(ketNoi, maPhieu);
            } else {
                truyCap.danhDauDoiTraTuChoi(ketNoi, maPhieu, lyDo.trim());
            }
            return null;
        });
    }
    public List<HoaDon> layHoaDonCoTheDoiTra(String tuKhoa) throws Exception {
        return truyCap.timHoaDonCoTheDoiTraThucThe(tuKhoa);
    }
    public List<ChiTietHoaDon> laySanPhamTrongHoaDon(String maHoaDon) throws Exception {
        return truyCap.timSanPhamTrongHoaDonThucThe(maHoaDon);
    }
    public List<PhieuDoiTraHoanTien> layDanhSachDuyetHoanTien(String maQuyetDinh) throws Exception {
        return truyCap.timDanhSachDuyetHoanTienThucThe(maQuyetDinh);
    }
    public BigDecimal layHanMucDuyetHoanTien() {
        return HAN_MUC_DUYET_HOAN_TIEN_MAC_DINH;
    }
}

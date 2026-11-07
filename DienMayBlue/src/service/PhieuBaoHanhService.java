package service;
import java.math.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import dao.PhieuBaoHanhDAO;
import entity.*;
public class PhieuBaoHanhService extends CoSoService<PhieuBaoHanhDAO> {
    public PhieuBaoHanhService() {
        super(new PhieuBaoHanhDAO());
    }
    public String goiYMaBaoHanh() throws SQLException {
        return truyCap.goiYMaBaoHanh();
    }
    public List<PhieuBaoHanh> danhSachBaoHanh() throws SQLException {
        return truyCap.timBaoHanhThucThe();
    }
    public List<Map<String, Object>> luaChonBaoHanh(String soDienThoai) throws SQLException {
        return truyCap.timLuaChonBaoHanh(soDienThoai);
    }
    public String taoBaoHanh(String maBaoHanh, String maKhachHang, String maHoaDon, String maSanPham, String issue) throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyenChamSocKhachHang("WARRANTY_MANAGE");
        String maPhieu = maBaoHanh == null ? "" : maBaoHanh.trim().toUpperCase(java.util.Locale.ROOT);
        if (!maPhieu.matches("BH\\d{5}")) {
            throw new IllegalArgumentException("Mã phiếu bảo hành phải theo dạng BH00001.");
        }
        if (issue == null || issue.trim().isEmpty()) {
            throw new IllegalArgumentException("Mô tả lỗi không được để trống.");
        }
        return truyCap.giaoDich(ketNoi -> {
            Map<String, Object> purchase = truyCap.timGiaoDichBaoHanh(ketNoi, maHoaDon, maSanPham);
            if (purchase == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm đã mua.");
            }
            if (!String.valueOf(purchase.get("customer_id")).equals(maKhachHang)) {
                throw new IllegalArgumentException("Sản phẩm không thuộc khách hàng đã chọn.");
            }
            String trangThaiHoaDon = String.valueOf(purchase.get("status"));
            if (!"Đã thanh toán".equals(trangThaiHoaDon)) {
                throw new IllegalArgumentException("Hóa đơn không còn đủ điều kiện tiếp nhận bảo hành.");
            }
            int conLai = ((Number) purchase.get("remaining_qty")).intValue();
            if (conLai <= 0) {
                throw new IllegalArgumentException( "Sản phẩm này đã được trả/hoàn toàn bộ, không thể tiếp nhận bảo hành.");
            }
            LocalDate ngayMua = ((java.sql.Timestamp) purchase.get("created_at")).toLocalDateTime().toLocalDate();
            int months = ((Number) purchase.get("warranty_months")).intValue();
            LocalDate hanBaoHanh = ngayMua.plusMonths(months);
            if (hanBaoHanh.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Sản phẩm đã hết hạn bảo hành từ " + hanBaoHanh + ".");
            }
            if (truyCap.demBaoHanhDangHoatDong(ketNoi, maKhachHang, maSanPham) >= conLai) {
                throw new IllegalArgumentException("Sản phẩm này đang có phiếu bảo hành chưa kết thúc.");
            }
            if (truyCap.maBaoHanhTonTai(ketNoi, maPhieu)) {
                throw new IllegalArgumentException("Mã phiếu bảo hành đã tồn tại.");
            }
            truyCap.themBaoHanh(ketNoi, maPhieu, maKhachHang, maSanPham, issue.trim(), hanBaoHanh, taiKhoan.getNhanVien().getMaNhanVien());
            return maPhieu;
        });
    }
    public BigDecimal tinhPhiDoiMay(String maBaoHanh, BigDecimal phiPhuKien) throws Exception {
        yeuCauQuyenChamSocKhachHang("WARRANTY_MANAGE");
        BigDecimal phi = phiPhuKien == null ? BigDecimal.ZERO : phiPhuKien;
        if (phi.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí phụ kiện không được âm.");
        }
        PhieuBaoHanh phieuBaoHanh = truyCap.giaoDich(ketNoi -> truyCap.timPhieuBaoHanhThucThe(ketNoi, maBaoHanh));
        if (phieuBaoHanh == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu bảo hành.");
        }
        if (!"REPAIRING".equals(phieuBaoHanh.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ phiếu đang sửa chữa mới được chuyển sang đổi máy.");
        }
        return tinhPhiDoiMay(truyCap.timDuLieuPhiBaoHanh(maBaoHanh), phi);
    }
    public void capNhatTrangThaiBaoHanh(String id, String trangThaiMoi, String ghiChu) throws Exception {
        capNhatTrangThaiBaoHanh(id, trangThaiMoi, ghiChu, BigDecimal.ZERO);
    }
    public void capNhatTrangThaiBaoHanh(String id, String trangThaiMoi, String ghiChu, BigDecimal phiPhuKien) throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyenChamSocKhachHang("WARRANTY_MANAGE");
        String trangThai = trangThaiMoi == null ? "" : trangThaiMoi.trim();
        BigDecimal phi = phiPhuKien == null ? BigDecimal.ZERO : phiPhuKien;
        truyCap.giaoDich(ketNoi -> {
            PhieuBaoHanh phieuBaoHanh = truyCap.timPhieuBaoHanhThucThe(ketNoi, id);
            if (phieuBaoHanh == null) {
                throw new IllegalArgumentException("Không tìm thấy phiếu bảo hành.");
            }
            String trangThaiCu = phieuBaoHanh.getTrangThai();
            if (trangThaiCu.equals(trangThai)) {
                throw new IllegalArgumentException("Phiếu đã ở trạng thái này.");
            }
            if (!chuyenTrangThaiBaoHanhHopLe(trangThaiCu, trangThai)) {
                throw new IllegalArgumentException("Không thể chuyển bảo hành từ '" + trangThaiCu + "' sang '" + trangThai + "'.");
            }
            if (("REJECTED".equals(trangThai) || "REPLACEMENT".equals(trangThai)) && (ghiChu == null || ghiChu.trim().isEmpty())) {
                throw new IllegalArgumentException("Vui lòng nhập ghi chú/lý do cho trạng thái này.");
            }
            BigDecimal replacementCharge = null;
            if ("REPLACEMENT".equals(trangThai)) {
                if (phi.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Phí phụ kiện không được âm.");
                }
                Map<String, Object> duLieuBaoHanh = truyCap.timDuLieuPhiBaoHanh(ketNoi, id);
                replacementCharge = tinhPhiDoiMay(duLieuBaoHanh, phi);
            }
            truyCap.capNhatTrangThaiBaoHanh(ketNoi, id, trangThai, replacementCharge);
            return null;
        });
    }
    private BigDecimal tinhPhiDoiMay(Map<String, Object> duLieuBaoHanh, BigDecimal phiPhuKien) {
        LocalDate ngayMua = ((java.sql.Timestamp) duLieuBaoHanh.get("created_at")).toLocalDateTime().toLocalDate();
        long months = Math.max(0, ChronoUnit.MONTHS.between(ngayMua, LocalDate.now()));
        int percent = (int) Math.min(60, months * 10);
        BigDecimal tyLe = new BigDecimal(percent).divide(new BigDecimal("100"));
        return((BigDecimal) duLieuBaoHanh.get("unit_price")).multiply(tyLe).add(phiPhuKien).setScale(2, RoundingMode.HALF_UP);
    }
    private boolean chuyenTrangThaiBaoHanhHopLe(String trangThaiCu, String trangThaiMoi) {
        if ("REPAIRING".equals(trangThaiCu)) {
            return "Hoàn tất".equals(trangThaiMoi) || "REPLACEMENT".equals(trangThaiMoi) || "REJECTED" .equals(trangThaiMoi);
        }
        if ("REPLACEMENT".equals(trangThaiCu)) {
            return "Hoàn tất".equals(trangThaiMoi);
        }
        if ("Hoàn tất".equals(trangThaiCu)) {
            return "RETURNED".equals(trangThaiMoi);
        }
        return false;
    }
}

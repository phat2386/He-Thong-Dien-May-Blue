package service;
import java.sql.SQLException;
import java.util.*;
import dao.KhieuNaiDAO;
import dao.KhachHangDAO;
import entity.*;
public class KhieuNaiService extends CoSoService<KhieuNaiDAO> {
    private final KhachHangDAO khachHangDAO = new KhachHangDAO();
    public KhieuNaiService() {
        super(new KhieuNaiDAO());
    }
    public String goiYMaKhieuNai() throws SQLException {
        return truyCap.goiYMaKhieuNai();
    }
    public List<KhieuNai> danhSachKhieuNai() throws SQLException {
        return truyCap.timKhieuNaiThucThe();
    }
    public String taoKhieuNai(String maKhieuNaiNhap, String maKhachHang, String soDienThoai, String noiDungGoc, String mucUuTien) throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyenChamSocKhachHang("COMPLAINT_MANAGE");
        String maKhieuNai = maKhieuNaiNhap == null ? "" : maKhieuNaiNhap.trim().toUpperCase(Locale.ROOT);
        String tel = soDienThoai == null ? "" : soDienThoai.trim();
        String noiDung = noiDungGoc == null ? "" : noiDungGoc.trim();
        String doUuTien = mucUuTien == null ? "" : mucUuTien.trim();
        if (!maKhieuNai.matches("KN\\d{3}")) {
            throw new IllegalArgumentException("Mã khiếu nại phải theo dạng KN001.");
        }
        if (tel.isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại khách hàng không được để trống.");
        }
        if (!tel.matches("\\d{10}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm đúng 10 chữ số.");
        }
        if (noiDung.length() < 5) {
            throw new IllegalArgumentException("Nội dung khiếu nại quá ngắn.");
        }
        if (!"Bình thường".equals(doUuTien) && !"Khẩn cấp".equals(doUuTien)) {
            throw new IllegalArgumentException("Mức ưu tiên khiếu nại không hợp lệ.");
        }
        return truyCap.giaoDich(ketNoi -> {
            if (truyCap.maKhieuNaiTonTai(ketNoi, maKhieuNai)) {
                throw new IllegalArgumentException("Mã khiếu nại đã tồn tại.");
            }
            String maNhanVien = taiKhoan.getNhanVien().getMaNhanVien();
            KhachHang khachHang = maKhachHang == null ? null : khachHangDAO.timKhachHangThucThe(ketNoi, maKhachHang);
            if (khachHang == null) {
                throw new IllegalArgumentException("Khiếu nại phải gắn với khách hàng đã tồn tại trong hệ thống.");
            }
            KhieuNai khieuNai = new KhieuNai(maKhieuNai, khachHang, tel, noiDung, doUuTien, "NEW", taiKhoan.getNhanVien(), taiKhoan.getNhanVien(), java.time.LocalDateTime.now().plusHours(48), false, java.time.LocalDateTime.now());
            truyCap.themKhieuNai(ketNoi, khieuNai.getMaKhieuNai(), maKhachHang, khieuNai.getSoDienThoaiKhach(), khieuNai.getNoiDung(), khieuNai.getMucUuTien(), maNhanVien);
            return maKhieuNai;
        });
    }
    public void capNhatLeoThangKhieuNai() throws Exception {
        truyCap.giaoDich(ketNoi -> {
            List<Map<String, Object>> overdue = truyCap.timKhieuNaiQuaHan(ketNoi);
            if (overdue.isEmpty()) {
                return null;
            }
            for (Map<String, Object> duLieuKhieuNai : overdue) {
                String maKhieuNai = String.valueOf(duLieuKhieuNai.get("complaint_id"));
                truyCap.danhDauKhieuNaiLeoThang(ketNoi, maKhieuNai);
            }
            return null;
        });
    }
    public void capNhatTrangThaiKhieuNai(String id, String trangThaiMoi, String ghiChu) throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyenChamSocKhachHang("COMPLAINT_MANAGE");
        String trangThai = trangThaiMoi == null ? "" : trangThaiMoi.trim();
        truyCap.giaoDich(ketNoi -> {
            KhieuNai khieuNai = truyCap.timKhieuNaiThucThe(ketNoi, id);
            if (khieuNai == null) {
                throw new IllegalArgumentException("Không tìm thấy khiếu nại.");
            }
            String trangThaiCu = khieuNai.getTrangThai();
            if (trangThaiCu.equals(trangThai)) {
                throw new IllegalArgumentException("Khiếu nại đã ở trạng thái này.");
            }
            if (!chuyenTrangThaiKhieuNaiHopLe(trangThaiCu, trangThai)) {
                throw new IllegalArgumentException("Không thể chuyển khiếu nại từ '" + trangThaiCu + "' sang '" + trangThai + "'.");
            }
            if (("RESOLVED".equals(trangThai) || "CLOSED".equals(trangThai)) && (ghiChu == null || ghiChu.trim().isEmpty())) {
                throw new IllegalArgumentException( "Vui lòng ghi rõ kết quả xử lý trước khi hoàn tất/đóng khiếu nại.");
            }
            String maNhanVien = taiKhoan.getNhanVien().getMaNhanVien();
            truyCap.capNhatTrangThaiKhieuNai(ketNoi, id, trangThai, maNhanVien);
            return null;
        });
    }
    private boolean chuyenTrangThaiKhieuNaiHopLe(String trangThaiCu, String trangThaiMoi) {
        if ("NEW".equals(trangThaiCu)) {
            return "IN_PROGRESS".equals(trangThaiMoi);
        }
        if ("IN_PROGRESS".equals(trangThaiCu)) {
            return "WAITING_CUSTOMER".equals(trangThaiMoi) || "RESOLVED".equals(trangThaiMoi);
        }
        if ("WAITING_CUSTOMER".equals(trangThaiCu)) {
            return "IN_PROGRESS".equals(trangThaiMoi) || "RESOLVED".equals(trangThaiMoi);
        }
        if ("RESOLVED".equals(trangThaiCu)) {
            return "CLOSED".equals(trangThaiMoi);
        }
        return false;
    }
}

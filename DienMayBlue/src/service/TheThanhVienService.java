package service;
import java.math.*;
import java.sql.SQLException;
import java.util.*;
import dao.TheThanhVienDAO;
import dao.KhachHangDAO;
import entity.*;
public class TheThanhVienService extends CoSoService<TheThanhVienDAO> {
    private final KhachHangDAO khachHangDAO = new KhachHangDAO();
    public TheThanhVienService() {
        super(new TheThanhVienDAO());
    }
    public String goiYMaThe(String maKhachHang) throws SQLException {
        return truyCap.goiYMaThe(maKhachHang);
    }
    public List<TheThanhVien> khachHangThanhVien(String tuKhoa) throws SQLException {
        return truyCap.timKhachHangThanhVienThucThe(tuKhoa);
    }
    public List<Map<String, Object>> cacHangThanhVien() {
        return ChinhSachThanhVien.danhSachMacDinh();
    }
    public String capLaiTheThanhVien(String maKhachHang, String maTheMoi) throws Exception {
        TaiKhoan taiKhoan = yeuCauQuyen("MEMBERSHIP_MANAGE");
        if (maKhachHang == null || maKhachHang.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn khách hàng.");
        }
        String normalizedId = maKhachHang.trim();
        String cardCode = maTheMoi == null ? "" : maTheMoi.trim();
        if (cardCode.isEmpty() || cardCode.length() > 20) {
            throw new IllegalArgumentException("Mã thẻ là bắt buộc và tối đa 20 ký tự.");
        }
        return truyCap.giaoDich(ketNoi -> {
            KhachHang khachHang = khachHangDAO.timKhachHangThucThe(ketNoi, normalizedId);
            if (khachHang == null) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng.");
            }
            if (!khachHang.dangHoatDong()) {
                throw new IllegalStateException("Khách hàng đang ngừng hoạt động nên không thể cấp thẻ mới.");
            }
            if (truyCap.maTheThanhVienTonTai(ketNoi, cardCode)) {
                throw new IllegalArgumentException("Mã thẻ đã tồn tại.");
            }
            TheThanhVien theCu = truyCap.timTheThanhVienDangHoatDong(ketNoi, normalizedId);
            int diem = theCu == null ? 0 : theCu.getDiemTichLuy();
            String hang = theCu == null || theCu.getHangThanhVien() == null ? "Tiêu chuẩn" : theCu.getHangThanhVien();
            truyCap.voHieuTheThanhVienDangDung(ketNoi, normalizedId);
            truyCap.themTheThanhVien(ketNoi, normalizedId, cardCode, diem, hang);
            return cardCode;
        });
    }
    public void capNhatChinhSachHangThanhVien(int maHang, int diemToiThieu, BigDecimal tyLeGiam, BigDecimal heSoDiem) {
        yeuCauQuyen("POINT_MANAGE");
        throw new UnsupportedOperationException("Lược đồ CSDL hiện tại không có bảng cấu hình chính sách hạng thành viên; không thể lưu thay đổi chính sách.");
    }
}

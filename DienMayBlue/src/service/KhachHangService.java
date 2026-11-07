package service;
import java.sql.SQLException;
import java.util.*;
import dao.KhachHangDAO;
import entity.*;
public class KhachHangService extends CoSoService<KhachHangDAO> {
    public KhachHangService() {
        super(new KhachHangDAO());
    }
    public String goiYMaKhachHang() throws SQLException {
        return truyCap.goiYMaKhachHang();
    }
    public List<KhachHang> timKiemKhachHang(String tuKhoa, String tierCode) throws SQLException {
        return truyCap.timKiemKhachHangThucThe(tuKhoa, tierCode);
    }
    public KhachHang layKhachHang(String maKhachHang) throws SQLException {
        return truyCap.timKhachHangThucThe(maKhachHang);
    }
    public void luuKhachHang(KhachHang khachHang, boolean taoMoi) throws Exception {
        if (khachHang == null) throw new IllegalArgumentException("Thông tin khách hàng không được rỗng.");
        luuKhachHang(khachHang.getMaKhachHang(), khachHang.getHoTen(), khachHang.getSoDienThoai(), khachHang.getEmail(), khachHang.getDiaChi(), taoMoi);
    }
    public void luuKhachHang(String id, String ten, String soDienThoai, String email, String diaChi, boolean taoMoi) throws Exception {
        String maKhachHang = id == null ? "" : id.trim().toUpperCase(Locale.ROOT);
        String hoTen = ten == null ? "" : ten.trim();
        String tel = soDienThoai == null ? "" : soDienThoai.trim();
        String mail = email == null ? "" : email.trim();
        if (hoTen.isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được rỗng.");
        }
        if (!tel.matches("[0-9]{10}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm đúng 10 chữ số.");
        }
        if (!mail.isEmpty() && (!mail.contains("@") || mail.startsWith("@") || mail.endsWith("@"))) {
            throw new IllegalArgumentException("Thư điện tử không hợp lệ.");
        }
        if (taoMoi) {
            if (!maKhachHang.matches("KH\\d{3,}")) {
                throw new IllegalArgumentException("Mã khách hàng phải có dạng KH001, KH002, ...");
            }
            if (truyCap.khachHangTonTai(maKhachHang)) {
                throw new IllegalArgumentException("Mã khách hàng đã tồn tại.");
            }
            truyCap.themKhachHang(maKhachHang, hoTen, rongThanhNull(tel), rongThanhNull(mail), rongThanhNull(diaChi));
        } else {
            truyCap.capNhatKhachHang(maKhachHang, hoTen, rongThanhNull(tel), rongThanhNull(mail), rongThanhNull(diaChi));
        }
    }
    public void xoaKhachHang(String maKhachHang) throws Exception {
        yeuCauQuyen("CUSTOMER_MANAGE");
        String ma = maKhachHang == null ? "" : maKhachHang.trim().toUpperCase(Locale.ROOT);
        if (ma.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn khách hàng cần xóa.");
        }
        if ("KH000".equals(ma)) {
            throw new IllegalArgumentException("Không thể xóa khách vãng lai hệ thống.");
        }
        truyCap.giaoDich(ketNoi -> {
            KhachHang khachHang = truyCap.timKhachHangThucThe(ketNoi, ma);
            if (khachHang == null) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng.");
            }
            if (truyCap.coDuLieuLienQuan(ketNoi, ma)) {
                throw new IllegalStateException("Khách hàng đã phát sinh hóa đơn hoặc nghiệp vụ liên quan nên không thể xóa khỏi hệ thống.");
            }
            truyCap.xoaKhachHang(ketNoi, ma);
            return null;
        });
    }
    public String maKhachHangTheoSoDienThoai(String soDienThoai) throws SQLException {
        List<Map<String, Object>> cacDong = truyCap.timMaKhachHangTheoSoDienThoai(soDienThoai);
        return cacDong.size() == 1 ? String.valueOf(cacDong.get(0).get("customer_id")) : null;
    }
}

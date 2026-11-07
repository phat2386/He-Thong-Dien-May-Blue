package service;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import dao.ThongKeDAO;
public class ThongKeService extends CoSoService<ThongKeDAO> {
    public ThongKeService() {
        super(new ThongKeDAO());
    }
    public Map<String, Object> layTongHopDoanhSoNhanVien(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        return truyCap.tongHopDoanhSoNhanVien(tuNgay, denNgay);
    }
    public List<Map<String, Object>> layChiTietDoanhSoNhanVien(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        return truyCap.chiTietDoanhSoNhanVien(tuNgay, denNgay);
    }
    public List<Map<String, Object>> layThongKeKhachHang(LocalDate tuNgay, LocalDate denNgay, boolean xepTheoSoLanMua) throws SQLException {
        return truyCap.thongKeKhachHang(tuNgay, denNgay, xepTheoSoLanMua);
    }
    public List<Map<String, Object>> layThongKeDoanhThu(LocalDate tuNgay, LocalDate denNgay, String nhomTheo) throws SQLException {
        return truyCap.thongKeDoanhThu(tuNgay, denNgay, nhomTheo);
    }
    public List<Map<String, Object>> layThongKeSanPham(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        return truyCap.thongKeSanPham(tuNgay, denNgay);
    }
    public List<Map<String, Object>> layThongKeThuong(LocalDate tuNgay, LocalDate denNgay) throws SQLException {
        return truyCap.thongKeThuong(tuNgay, denNgay);
    }
    public Map<String, Object> layTongQuanHomNay() throws SQLException {
        return truyCap.tongQuanHomNay();
    }
    public List<Map<String, Object>> layDoanhThuBayNgayGanNhat() throws SQLException {
        return truyCap.doanhThuBayNgayGanNhat();
    }
    public List<Map<String, Object>> layTopDanhMucBanChay() throws SQLException {
        return truyCap.topDanhMucBanChay();
    }
    public List<Map<String, Object>> layHoaDonGanDay() throws SQLException {
        return truyCap.hoaDonGanDay();
    }
}

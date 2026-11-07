package service;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import dao.NhanVienDAO;
import entity.*;
public class NhanVienService extends CoSoService<NhanVienDAO> {
    public NhanVienService() {
        super(new NhanVienDAO());
    }
    public String goiYMaNhanVien() throws SQLException {
        return truyCap.goiYMaNhanVien();
    }
    public List<NhanVien> timKiemNhanVien(String tuKhoa, String maTrangThai) throws SQLException {
        return truyCap.timKiemThucThe(tuKhoa, maTrangThai);
    }
    public NhanVien timNhanVien(String maNhanVien) throws SQLException {
        return truyCap.timThucTheTheoMa(maNhanVien);
    }
    public String trangThaiNhanVien(String maNhanVien) throws SQLException {
        return truyCap.timTrangThai(maNhanVien);
    }
    public void taoNhanVien(NhanVien nhanVien) throws Exception {
        if (nhanVien == null) throw new IllegalArgumentException("Thông tin nhân viên không được rỗng.");
        taoNhanVien(nhanVien.getMaNhanVien(), nhanVien.getHoTen(), nhanVien.getSoDienThoai(), nhanVien.getEmail(), nhanVien.getNgayVaoLam(), nhanVien.getTrinhDo(), nhanVien.getHeSoLuong(), nhanVien.getLuongCoBan(), nhanVien.getPhongBan());
    }
    public void taoNhanVien(String maNhanVien, String hoTen, String soDienThoai, String email, LocalDate ngayVaoLam, String trinhDo, Double heSoLuong, java.math.BigDecimal luongCoBan, String loaiNhanVien) throws Exception {
        yeuCauQuyen("EMPLOYEE_MANAGE");
        kiemTraDuLieuNhanVien(maNhanVien, hoTen, soDienThoai, email, ngayVaoLam, trinhDo, heSoLuong, luongCoBan, loaiNhanVien);
        String ma = maNhanVien.trim();
        String dienThoai = soDienThoai.trim();
        truyCap.giaoDich(ketNoi -> {
            if (truyCap.maHoacSoDienThoaiTonTai(ketNoi, ma, dienThoai)) {
                throw new IllegalArgumentException("Mã nhân viên hoặc số điện thoại đã tồn tại.");
            }
            truyCap.them(ketNoi, ma, hoTen.trim(), dienThoai, email.trim(), ngayVaoLam, trinhDo, heSoLuong, luongCoBan, loaiNhanVien);
            return null;
        });
    }
    public void capNhatNhanVien(NhanVien nhanVien) throws Exception {
        if (nhanVien == null) throw new IllegalArgumentException("Thông tin nhân viên không được rỗng.");
        capNhatNhanVien(nhanVien.getMaNhanVien(), nhanVien.getHoTen(), nhanVien.getSoDienThoai(), nhanVien.getEmail(), nhanVien.getNgayVaoLam(), nhanVien.getTrinhDo(), nhanVien.getHeSoLuong(), nhanVien.getLuongCoBan(), nhanVien.getPhongBan());
    }
    public void capNhatNhanVien(String maNhanVien, String hoTen, String soDienThoai, String email, LocalDate ngayVaoLam, String trinhDo, Double heSoLuong, java.math.BigDecimal luongCoBan, String loaiNhanVien) throws Exception {
        yeuCauQuyen("EMPLOYEE_MANAGE");
        kiemTraDuLieuNhanVien(maNhanVien, hoTen, soDienThoai, email, ngayVaoLam, trinhDo, heSoLuong, luongCoBan, loaiNhanVien);
        truyCap.giaoDich(ketNoi -> {
            if (!truyCap.tonTai(ketNoi, maNhanVien)) {
                throw new IllegalArgumentException("Không tìm thấy nhân viên.");
            }
            if (truyCap.laQuanTri(ketNoi, maNhanVien)) {
                throw new IllegalStateException("Hồ sơ Admin hệ thống không được chỉnh sửa tại UC032.");
            }
            if (truyCap.soDienThoaiDaDung(ketNoi, soDienThoai.trim(), maNhanVien)) {
                throw new IllegalArgumentException("Số điện thoại đã thuộc nhân viên khác.");
            }
            truyCap.capNhat(ketNoi, maNhanVien, hoTen.trim(), soDienThoai.trim(), email.trim(), ngayVaoLam, trinhDo, heSoLuong, luongCoBan, loaiNhanVien);
            return null;
        });
    }
    public void xoaNhanVien(String maNhanVien) throws Exception {
        TaiKhoan tacNhan = yeuCauQuyen("EMPLOYEE_MANAGE");
        if (maNhanVien == null || maNhanVien.trim().isEmpty()) throw new IllegalArgumentException("Chưa chọn nhân viên cần xóa.");
        if (tacNhan.getNhanVien().getMaNhanVien().equals(maNhanVien)) throw new IllegalStateException("Không thể xóa hồ sơ nhân viên đang đăng nhập.");
        truyCap.giaoDich(ketNoi -> {
            if (!truyCap.tonTai(ketNoi, maNhanVien)) throw new IllegalArgumentException("Không tìm thấy nhân viên.");
            if (truyCap.laQuanTri(ketNoi, maNhanVien)) throw new IllegalStateException("Không thể xóa tài khoản quản trị hệ thống.");
            TaiKhoan taiKhoan = truyCap.timTaiKhoanTheoNhanVien(ketNoi, maNhanVien);
            if (taiKhoan != null && taiKhoan.getVaiTro() != null && "MANAGER".equals(taiKhoan.getVaiTro().getMaQuyenVaiTro()) && "Hoạt động".equals(taiKhoan.getTrangThai()) && truyCap.demQuanLyDangHoatDong(ketNoi) <= 1) {
                throw new IllegalStateException("Không thể xóa quản lý đang hoạt động cuối cùng của hệ thống.");
            }
            if (truyCap.coDuLieuNghiepVu(ketNoi, maNhanVien)) throw new IllegalStateException("Nhân viên đã phát sinh hóa đơn/phiếu nghiệp vụ nên không thể xóa để bảo toàn lịch sử.");
            truyCap.xoaNhanVien(ketNoi, maNhanVien);
            return null;
        });
    }
    private void kiemTraDuLieuNhanVien(String maNhanVien, String hoTen, String soDienThoai, String email, LocalDate ngayVaoLam, String trinhDo, Double heSoLuong, java.math.BigDecimal luongCoBan, String loaiNhanVien) {
        if (maNhanVien == null || maNhanVien.trim().isEmpty() || hoTen == null || hoTen.trim().isEmpty() || soDienThoai == null || soDienThoai.trim().isEmpty() || email == null || email.trim().isEmpty() || ngayVaoLam == null || trinhDo == null || heSoLuong == null || luongCoBan == null || loaiNhanVien == null) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin.");
        }
        if (!maNhanVien.trim().toUpperCase().matches("NV\\d{3}")) {
            throw new IllegalArgumentException("Mã nhân viên phải theo dạng NV001.");
        }
        if (!soDienThoai.trim().matches("0\\d{9}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 10 số, bắt đầu bằng 0.");
        }
        if (!email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (ngayVaoLam == null || ngayVaoLam.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày vào làm không được để trống hoặc nằm trong tương lai.");
        }
        if (!java.util.Arrays.asList("Trung cấp", "Cao đẳng", "Đại học", "Sau đại học").contains(trinhDo)) {
            throw new IllegalArgumentException("Trình độ không hợp lệ.");
        }
        if (heSoLuong == null || heSoLuong < 1.84 || heSoLuong > 6.5) {
            throw new IllegalArgumentException("Hệ số lương phải nằm trong khoảng 1,84 đến 6,5.");
        }
        if (luongCoBan == null || luongCoBan.signum() <= 0) {
            throw new IllegalArgumentException("Lương cơ bản phải lớn hơn 0.");
        }
        if (!java.util.Arrays.asList("Quản lý", "Bán hàng", "CSKH").contains(loaiNhanVien)) {
            throw new IllegalArgumentException("Loại nhân viên không hợp lệ.");
        }
    }
}

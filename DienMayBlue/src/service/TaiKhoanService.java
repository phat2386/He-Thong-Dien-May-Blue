package service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import dao.*;
import entity.*;
public class TaiKhoanService {
    private static final int VAI_TRO_ADMIN = 1;
    private static final int VAI_TRO_QUAN_LY = 2;
    private static final int VAI_TRO_BAN_HANG = 3;
    private static final int VAI_TRO_CSKH = 4;
    private static final int SO_LAN_SAI_TRUOC_KHI_KHOA = 5;
    private static final int SO_PHUT_KHOA_TAM = 15;
    private static TaiKhoan taiKhoanHienTai;
    private static Set<String> danhSachQuyen = Collections.emptySet();
    private static void batDauPhienDangNhap(TaiKhoan taiKhoan, Set<String> cacMaQuyen) {
        taiKhoanHienTai = taiKhoan;
        danhSachQuyen = Collections.unmodifiableSet(new HashSet<String>(cacMaQuyen));
    }
    public static TaiKhoan getTaiKhoanHienTai() {
        return taiKhoanHienTai;
    }
    public static Set<String> getDanhSachQuyen() {
        return danhSachQuyen;
    }
    public static boolean coQuyen(String maQuyen) {
        return taiKhoanHienTai != null && danhSachQuyen.contains(maQuyen);
    }
    public static boolean daDangNhap() {
        return taiKhoanHienTai != null;
    }
    public static void xoaPhien() {
        taiKhoanHienTai = null;
        danhSachQuyen = Collections.emptySet();
    }
    private static final String THUAT_TOAN_MAT_KHAU = "PBKDF2WithHmacSHA256";
    private static final int SO_VONG_LAP_MAT_KHAU = 120000;
    private static final int DO_DAI_KHOA_MAT_KHAU = 256;
    private static final int DO_DAI_MUOI_MAT_KHAU = 16;
    private static final String TIEN_TO_MAT_KHAU = "PBKDF2";
    private static final SecureRandom BO_NGAU_NHIEN_MAT_KHAU = new SecureRandom();
    private static String bamMatKhau(String matKhauNhap) {
        if (matKhauNhap == null) throw new IllegalArgumentException("Mật khẩu không được null.");
        byte[] muoi = new byte[DO_DAI_MUOI_MAT_KHAU];
        BO_NGAU_NHIEN_MAT_KHAU.nextBytes(muoi);
        byte[] maBam = pbkdf2(matKhauNhap.toCharArray(), muoi, SO_VONG_LAP_MAT_KHAU, DO_DAI_KHOA_MAT_KHAU);
        return TIEN_TO_MAT_KHAU + "$" + SO_VONG_LAP_MAT_KHAU + "$" + Base64.getEncoder().encodeToString(muoi) + "$" + Base64.getEncoder().encodeToString(maBam);
    }
    private static boolean khopMatKhau(String matKhauNhap, String maBamDaLuu) {
        if (matKhauNhap == null || maBamDaLuu == null || maBamDaLuu.trim().isEmpty()) return false;
        if (!maBamDaLuu.startsWith(TIEN_TO_MAT_KHAU + "$")) {
            return soSanhAnToan(bamSha256(matKhauNhap).getBytes(StandardCharsets.UTF_8), maBamDaLuu.toUpperCase().getBytes( StandardCharsets.UTF_8));
        }
        try {
            String[] phan = maBamDaLuu.split("\\$");
            if (phan.length != 4) return false;
            int soVongLap = Integer.parseInt(phan[1]);
            byte[] muoi = Base64.getDecoder().decode(phan[2]);
            byte[] maBamMongDoi = Base64.getDecoder().decode(phan[3]);
            byte[] maBamThucTe = pbkdf2(matKhauNhap.toCharArray(), muoi, soVongLap, maBamMongDoi.length * 8);
            return soSanhAnToan(maBamThucTe, maBamMongDoi);
        } catch (RuntimeException ex) {
            return false;
        }
    }
    private static String bamSha256(String matKhauNhap) {
        try {
            MessageDigest boBam = MessageDigest.getInstance("SHA-256");
            byte[] maBam = boBam.digest(matKhauNhap.getBytes(StandardCharsets.UTF_8));
            StringBuilder ketQua = new StringBuilder();
            for (byte giaTri : maBam) ketQua.append(String.format("%02X", giaTri));
            return ketQua.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }
    private static byte[] pbkdf2(char[] matKhau, byte[] muoi, int soVongLap, int doDaiKhoa) {
        PBEKeySpec dacTa = new PBEKeySpec(matKhau, muoi, soVongLap, doDaiKhoa);
        try {
            return SecretKeyFactory.getInstance(THUAT_TOAN_MAT_KHAU).generateSecret(dacTa).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Không thể băm mật khẩu bằng PBKDF2.", ex);
        } finally {
            dacTa.clearPassword();
        }
    }
    private static boolean soSanhAnToan(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
    private static boolean duManh(String matKhau) {
        if (matKhau == null || matKhau.length() < 8) return false;
        boolean coChu = false;
        boolean coSo = false;
        for (char kyTu : matKhau.toCharArray()) {
            if (Character.isLetter(kyTu)) coChu = true;
            else if (Character.isDigit(kyTu))
                coSo = true;
        }
        return coChu && coSo;
    }
    private final TaiKhoanDAO truyCapTaiKhoan = new TaiKhoanDAO();
    private final NhanVienDAO truyCapNhanVien = new NhanVienDAO();
    public static final class KetQuaDangNhap {
        private final boolean thanhCong;
        private final String thongBao;
        private final TaiKhoan taiKhoan;
        private final boolean canDoiMatKhau;
        private KetQuaDangNhap(boolean thanhCong, String thongBao, TaiKhoan taiKhoan, boolean canDoiMatKhau) {
            this.thanhCong = thanhCong;
            this.thongBao = thongBao;
            this.taiKhoan = taiKhoan;
            this.canDoiMatKhau = canDoiMatKhau;
        }
        public static KetQuaDangNhap thanhCong(TaiKhoan taiKhoan, boolean canDoiMatKhau) {
            return new KetQuaDangNhap(true, "Đăng nhập thành công.", taiKhoan, canDoiMatKhau);
        }
        public static KetQuaDangNhap thatBai(String thongBao) {
            return new KetQuaDangNhap(false, thongBao, null, false);
        }
        public boolean isThanhCong() {
            return thanhCong;
        }
        public String getThongBao() {
            return thongBao;
        }
        public TaiKhoan getTaiKhoan() {
            return taiKhoan;
        }
        public boolean isCanDoiMatKhau() {
            return canDoiMatKhau;
        }
    }
    public static final class KetQua {
        private final boolean thanhCong;
        private final String thongBao;
        private KetQua(boolean thanhCong, String thongBao) {
            this.thanhCong = thanhCong;
            this.thongBao = thongBao;
        }
        public static KetQua thanhCong(String thongBao) {
            return new KetQua(true, thongBao);
        }
        public static KetQua thatBai(String thongBao) {
            return new KetQua(false, thongBao);
        }
        public boolean isThanhCong() {
            return thanhCong;
        }
        public String getThongBao() {
            return thongBao;
        }
    }
    public KetQuaDangNhap dangNhap(String tenDangNhap, String matKhauNhap) {
        tenDangNhap = tenDangNhap == null ? "" : tenDangNhap.trim();
        matKhauNhap = matKhauNhap == null ? "" : matKhauNhap;
        if (tenDangNhap.isEmpty() || matKhauNhap.isEmpty()) {
            return KetQuaDangNhap.thatBai("Tên đăng nhập và mật khẩu không được để trống.");
        }
        try {
            TaiKhoan taiKhoan = truyCapTaiKhoan.timTheoTenDangNhap(tenDangNhap);
            if (taiKhoan == null) {
                return KetQuaDangNhap.thatBai("Sai tài khoản hoặc mật khẩu.");
            }
            String trangThaiTaiKhoan = taiKhoan.getTrangThai() == null ? "" : taiKhoan.getTrangThai().trim();
            if (!"Hoạt động".equals(trangThaiTaiKhoan)) {
                if ("Bị khóa".equals(trangThaiTaiKhoan)) {
                    return KetQuaDangNhap.thatBai("Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên.");
                }
                if ("DISABLED".equals(trangThaiTaiKhoan)) {
                    return KetQuaDangNhap.thatBai("Tài khoản đã ngừng hoạt động. Vui lòng liên hệ quản trị viên.");
                }
                if ("PENDING".equals(trangThaiTaiKhoan)) {
                    return KetQuaDangNhap.thatBai("Tài khoản chưa được kích hoạt. Vui lòng liên hệ quản trị viên.");
                }
                return KetQuaDangNhap.thatBai("Tài khoản hiện không thể đăng nhập. Vui lòng liên hệ quản trị viên.");
            }
            if (taiKhoan.getNhanVien() == null || !"Hoạt động".equalsIgnoreCase(taiKhoan.getNhanVien().getTrangThai())) {
                return KetQuaDangNhap.thatBai( "Hồ sơ nhân viên hiện không ở trạng thái làm việc. Vui lòng liên hệ quản lý.");
            }
            if (taiKhoan.getKhoaDen() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (taiKhoan.getKhoaDen().isAfter(now)) {
                    return KetQuaDangNhap.thatBai("Tài khoản đang bị khóa tạm thời đến " + taiKhoan.getKhoaDen().toLocalTime().withSecond( 0).withNano(0) + ". Vui lòng thử lại sau.");
                }
                truyCapTaiKhoan.xoaKhoaTamHetHan(taiKhoan.getMaTaiKhoan());
                taiKhoan.setSoLanDangNhapSai(0);
                taiKhoan.setKhoaDen(null);
            }
            if (!khopMatKhau(matKhauNhap, taiKhoan.getMaBamMatKhau())) {
                int soLanSai = truyCapTaiKhoan.tangSoLanDangNhapSai(taiKhoan.getMaTaiKhoan());
                if (soLanSai > SO_LAN_SAI_TRUOC_KHI_KHOA) {
                    truyCapTaiKhoan.datKhoaTamThoi(taiKhoan.getMaTaiKhoan(), SO_PHUT_KHOA_TAM);
                    return KetQuaDangNhap.thatBai("Bạn đã nhập sai quá 5 lần liên tiếp. Tài khoản bị khóa tạm thời 15 phút.");
                }
                if (soLanSai == 5) {
                    return KetQuaDangNhap.thatBai("Sai tài khoản hoặc mật khẩu. Đây là lần sai thứ 5; " + "nếu nhập sai thêm một lần, tài khoản sẽ bị khóa tạm thời 15 phút.");
                }
                return KetQuaDangNhap.thatBai("Sai tài khoản hoặc mật khẩu. Lần nhập sai liên tiếp: " + soLanSai + "/5.");
            }
            Set<String> danhSachQuyen = quyenTheoVaiTro(taiKhoan.getVaiTro().getMaVaiTro());
            truyCapTaiKhoan.ghiNhanDangNhapThanhCong(taiKhoan.getMaTaiKhoan());
            batDauPhienDangNhap(taiKhoan, danhSachQuyen);
            return KetQuaDangNhap.thanhCong(taiKhoan, false);
        } catch (SQLException e) {
            e.printStackTrace();
            return KetQuaDangNhap.thatBai("Không thể kết nối hoặc truy vấn cơ sở dữ liệu.");
        }
    }
    public KetQua doiMatKhau(String matKhauHienTai, String matKhauMoi, String xacNhanMatKhau) {
        if (!daDangNhap()) {
            return KetQua.thatBai("Phiên đăng nhập không còn hiệu lực.");
        }
        matKhauHienTai = matKhauHienTai == null ? "" : matKhauHienTai;
        matKhauMoi = matKhauMoi == null ? "" : matKhauMoi;
        xacNhanMatKhau = xacNhanMatKhau == null ? "" : xacNhanMatKhau;
        if (matKhauHienTai.isEmpty() || matKhauMoi.isEmpty() || xacNhanMatKhau.isEmpty()) {
            return KetQua.thatBai("Vui lòng nhập đầy đủ mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu.");
        }
        try {
            TaiKhoan taiKhoanPhien = getTaiKhoanHienTai();
            TaiKhoan taiKhoan = truyCapTaiKhoan.timTheoMa(taiKhoanPhien.getMaTaiKhoan());
            if (taiKhoan == null || !"Hoạt động".equalsIgnoreCase(taiKhoan.getTrangThai()) || taiKhoan.getNhanVien() == null || !"Hoạt động".equalsIgnoreCase(taiKhoan.getNhanVien().getTrangThai())) {
                return KetQua.thatBai("Tài khoản hoặc hồ sơ nhân viên không còn hoạt động. Vui lòng đăng nhập lại.");
            }
            if (!khopMatKhau(matKhauHienTai, taiKhoan.getMaBamMatKhau())) {
                return KetQua.thatBai("Mật khẩu hiện tại không đúng.");
            }
            if (!duManh(matKhauMoi)) {
                return KetQua.thatBai("Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ và số.");
            }
            if (!matKhauMoi.equals(xacNhanMatKhau)) {
                return KetQua.thatBai("Mật khẩu xác nhận không khớp.");
            }
            if (khopMatKhau(matKhauMoi, taiKhoan.getMaBamMatKhau())) {
                return KetQua.thatBai("Mật khẩu mới không được trùng mật khẩu hiện tại.");
            }
            String maBamMoi = bamMatKhau(matKhauMoi);
            truyCapTaiKhoan.capNhatMatKhau(taiKhoan.getMaTaiKhoan(), maBamMoi);
            taiKhoanPhien.setMaBamMatKhau(maBamMoi);
            return KetQua.thanhCong("Đổi mật khẩu thành công.");
        } catch (SQLException e) {
            e.printStackTrace();
            return KetQua.thatBai("Không thể cập nhật mật khẩu.");
        }
    }
    public TaiKhoan layTaiKhoanHienTaiTuCSDL() throws SQLException {
        if (!daDangNhap()) return null;
        return truyCapTaiKhoan.timTheoMa(getTaiKhoanHienTai().getMaTaiKhoan());
    }
    public void dangXuat() {
        xoaPhien();
    }
    private TaiKhoan yeuCauQuyen(String maQuyen) {
        TaiKhoan taiKhoan = getTaiKhoanHienTai();
        if (taiKhoan == null || taiKhoan.getNhanVien() == null || taiKhoan.getVaiTro() == null) {
            throw new IllegalStateException("Phiên đăng nhập không hợp lệ.");
        }
        if (!coQuyen(maQuyen)) {
            throw new IllegalStateException("Tài khoản chưa được cấp quyền thực hiện chức năng này.");
        }
        return taiKhoan;
    }
    public String goiYMaTaiKhoan() throws SQLException {
        return truyCapTaiKhoan.goiYMaTaiKhoan();
    }
    public List<TaiKhoan> timKiemTaiKhoan(String tuKhoa, String vaiTro, String trangThai) throws SQLException {
        return truyCapTaiKhoan.timKiemTaiKhoanThucThe(tuKhoa, vaiTro, trangThai);
    }
    public List<NhanVien> nhanVienChuaCoTaiKhoan() throws SQLException {
        return truyCapTaiKhoan.timNhanVienChuaCoTaiKhoanThucThe();
    }
    public List<VaiTro> vaiTroCoTheQuanLy() {
        return Arrays.asList(
            new VaiTro(VAI_TRO_QUAN_LY, "MANAGER", "Quản lý", "Vai trò quản lý"),
            new VaiTro(VAI_TRO_BAN_HANG, "SALES", "Bán hàng", "Vai trò bán hàng"),
            new VaiTro(VAI_TRO_CSKH, "CUSTOMER_SERVICE", "Chăm sóc khách hàng", "Vai trò chăm sóc khách hàng")
        );
    }
    public TaiKhoan thongTinTaiKhoan(String maTaiKhoan) throws SQLException {
        return truyCapTaiKhoan.timThongTinTaiKhoanThucThe(maTaiKhoan);
    }
    public String trangThaiTaiKhoan(String maTaiKhoan) throws SQLException {
        return truyCapTaiKhoan.timTrangThai(maTaiKhoan);
    }
    public boolean coPhienDangHoatDong(String maTaiKhoan) {
        return taiKhoanHienTai != null && Objects.equals(taiKhoanHienTai.getMaTaiKhoan(), maTaiKhoan);
    }
    public TaiKhoan trangThaiPhien(String maTaiKhoan) throws SQLException {
        return truyCapTaiKhoan.timTrangThaiPhienThucThe(maTaiKhoan);
    }
    public Set<String> quyenTheoVaiTro(int maVaiTro) {
        if (maVaiTro == VAI_TRO_ADMIN) return tatCaQuyen();
        LinkedHashSet<String> quyen = new LinkedHashSet<>();
        quyen.addAll(Arrays.asList("DASHBOARD_VIEW", "PROFILE_VIEW", "PASSWORD_CHANGE"));
        if (maVaiTro == VAI_TRO_QUAN_LY) {
            quyen.addAll(Arrays.asList("EMPLOYEE_MANAGE", "ACCOUNT_MANAGE", "SHIFT_MANAGE", "REFUND_APPROVE", "STATISTICS_VIEW", "BONUS_FINALIZE", "PRODUCT_MANAGE", "PRODUCT_SEARCH", "SUPPLIER_MANAGE", "PROMOTION_MANAGE", "MEMBERSHIP_MANAGE", "POINT_MANAGE"));
        } else if (maVaiTro == VAI_TRO_BAN_HANG) {
            quyen.addAll(Arrays.asList("STATISTICS_VIEW", "PRODUCT_SEARCH", "SALES_MANAGE", "INVOICE_CREATE", "DELIVERY_CREATE", "RETURN_CREATE", "CUSTOMER_MANAGE", "PERSONAL_SALES_VIEW", "DELIVERY_MANAGE", "DELIVERY_ASSIGN"));
        } else if (maVaiTro == VAI_TRO_CSKH) {
            quyen.addAll(Arrays.asList("PERSONAL_SALES_VIEW", "WARRANTY_MANAGE", "COMPLAINT_MANAGE"));
        }
        return quyen;
    }
    private static Set<String> tatCaQuyen() {
        return new LinkedHashSet<>(Arrays.asList("DASHBOARD_VIEW", "EMPLOYEE_MANAGE", "ACCOUNT_MANAGE", "SHIFT_MANAGE", "REFUND_APPROVE", "STATISTICS_VIEW", "BONUS_FINALIZE", "PRODUCT_MANAGE", "PRODUCT_SEARCH", "SUPPLIER_MANAGE", "PROMOTION_MANAGE", "MEMBERSHIP_MANAGE", "POINT_MANAGE", "SALES_MANAGE", "INVOICE_CREATE", "DELIVERY_CREATE", "RETURN_CREATE", "CUSTOMER_MANAGE", "PERSONAL_SALES_VIEW", "DELIVERY_MANAGE", "DELIVERY_ASSIGN", "WARRANTY_MANAGE", "COMPLAINT_MANAGE", "PROFILE_VIEW", "PASSWORD_CHANGE"));
    }
    public void taoTaiKhoanChoNhanVien(String maTaiKhoan, String maNhanVien, String tenDangNhap, String matKhauNhap) throws Exception {
        TaiKhoan tacNhan = yeuCauQuyen("ACCOUNT_MANAGE");
        String maTaiKhoanChuan = maTaiKhoan == null ? "" : maTaiKhoan.trim().toUpperCase();
        if (!maTaiKhoanChuan.matches("TK\\d{3}")) {
            throw new IllegalArgumentException("Mã tài khoản phải theo dạng TK000, TK001, ...");
        }
        if (maNhanVien == null || maNhanVien.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn nhân viên.");
        }
        if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        }
        if (!duManh(matKhauNhap)) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số.");
        }
        final String tenDangNhapDaChuanHoa = tenDangNhap.trim();
        truyCapTaiKhoan.giaoDich(ketNoi -> {
            if (!"Hoạt động".equals(truyCapNhanVien.timTrangThai(maNhanVien))) {
                throw new IllegalArgumentException("Chỉ có thể cấp tài khoản cho nhân viên đang hoạt động.");
            }
            if (truyCapTaiKhoan.tonTaiMaTaiKhoan(ketNoi, maTaiKhoanChuan)) {
                throw new IllegalArgumentException("Mã tài khoản đã tồn tại.");
            }
            if (truyCapTaiKhoan.tonTaiTheoNhanVienHoacTenDangNhap(maNhanVien, tenDangNhapDaChuanHoa)) {
                throw new IllegalArgumentException("Nhân viên đã có tài khoản hoặc tên đăng nhập đã tồn tại.");
            }
            truyCapTaiKhoan.them(ketNoi, maTaiKhoanChuan, maNhanVien, tenDangNhapDaChuanHoa, bamMatKhau(matKhauNhap));
            return null;
        });
    }
    public void capNhatTaiKhoan(String maTaiKhoan, String tenDangNhap, String matKhauMoi, String xacNhanMatKhau) throws Exception {
        yeuCauQuyen("ACCOUNT_MANAGE");
        if (tenDangNhap == null || tenDangNhap.trim().isEmpty()) throw new IllegalArgumentException("Tên đăng nhập không được để trống.");
        final String tenDangNhapDaChuanHoa = tenDangNhap.trim();
        final String matKhau = matKhauMoi == null ? "" : matKhauMoi;
        final String xacNhan = xacNhanMatKhau == null ? "" : xacNhanMatKhau;
        if (!matKhau.isEmpty()) {
            if (!duManh(matKhau)) throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ và số.");
            if (!matKhau.equals(xacNhan)) throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp.");
        } else if (!xacNhan.isEmpty()) throw new IllegalArgumentException("Hãy nhập mật khẩu mới trước khi xác nhận.");
        truyCapTaiKhoan.giaoDich(ketNoi -> {
            TaiKhoan taiKhoanDich = truyCapTaiKhoan.timTheoMa(maTaiKhoan);
            if (taiKhoanDich == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            if (taiKhoanDich.getVaiTro() != null && "ADMIN".equals(taiKhoanDich.getVaiTro().getMaQuyenVaiTro())) throw new IllegalArgumentException("Tài khoản Admin hệ thống được giữ cố định tại màn hình này.");
            if (truyCapTaiKhoan.tonTaiTenDangNhapKhacMa(tenDangNhapDaChuanHoa, maTaiKhoan)) throw new IllegalArgumentException("Tên đăng nhập đã tồn tại.");
            truyCapTaiKhoan.capNhatTenDangNhap(ketNoi, maTaiKhoan, tenDangNhapDaChuanHoa);
            if (!matKhau.isEmpty()) truyCapTaiKhoan.capNhatMatKhau(ketNoi, maTaiKhoan, bamMatKhau(matKhau));
            return null;
        });
    }
    public void doiTrangThaiTaiKhoan(String maTaiKhoan, String trangThaiMoi) throws Exception {
        TaiKhoan tacNhan = yeuCauQuyen("ACCOUNT_MANAGE");
        if (!Arrays.asList("Hoạt động", "Bị khóa", "DISABLED").contains(trangThaiMoi)) {
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        }
        if (Objects.equals(maTaiKhoan, tacNhan.getMaTaiKhoan()) && !"Hoạt động".equals(trangThaiMoi)) {
            throw new IllegalArgumentException("Không thể tự khóa/vô hiệu hóa tài khoản đang sử dụng.");
        }
        truyCapTaiKhoan.giaoDich(ketNoi -> {
            TaiKhoan taiKhoanDich = truyCapTaiKhoan.timTheoMa(maTaiKhoan);
            if (taiKhoanDich == null) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            }
            String vaiTroHienTai = taiKhoanDich.getVaiTro() == null ? null : taiKhoanDich.getVaiTro().getMaQuyenVaiTro();
            if ("ADMIN".equals(vaiTroHienTai) && !"Hoạt động".equals(trangThaiMoi)) {
                throw new IllegalArgumentException("Không được khóa tài khoản Admin hệ thống.");
            }
            if ("Hoạt động".equals(trangThaiMoi) && (taiKhoanDich.getNhanVien() == null || !"Hoạt động".equals(taiKhoanDich.getNhanVien().getTrangThai()))) {
                throw new IllegalArgumentException( "Không thể kích hoạt tài khoản khi hồ sơ nhân viên chưa hoạt động.");
            }
            if (!"Hoạt động".equals(trangThaiMoi) && "MANAGER".equals(vaiTroHienTai) && "Hoạt động".equals(taiKhoanDich.getTrangThai()) && truyCapTaiKhoan.demQuanLyDangHoatDong(ketNoi) <= 1) {
                throw new IllegalStateException("Không thể khóa/vô hiệu hóa quản lý đang hoạt động cuối cùng.");
            }
            truyCapTaiKhoan.capNhatTrangThai(ketNoi, maTaiKhoan, trangThaiMoi);
            if (!"Hoạt động".equals(trangThaiMoi)) {
            }
            return null;
        });
    }
    public boolean xoaHoacVoHieuTaiKhoan(String maTaiKhoan) throws Exception {
        TaiKhoan tacNhan = yeuCauQuyen("ACCOUNT_MANAGE");
        if (Objects.equals(maTaiKhoan, tacNhan.getMaTaiKhoan())) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản đang sử dụng.");
        }
        return truyCapTaiKhoan.giaoDich(ketNoi -> {
            TaiKhoan taiKhoanDich = truyCapTaiKhoan.timTheoMa(maTaiKhoan);
            if (taiKhoanDich == null) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            }
            String vaiTroHienTai = taiKhoanDich.getVaiTro() == null ? null : taiKhoanDich.getVaiTro().getMaQuyenVaiTro();
            if ("MANAGER".equals(vaiTroHienTai) && "Hoạt động".equals(taiKhoanDich.getTrangThai()) && truyCapTaiKhoan.demQuanLyDangHoatDong(ketNoi) <= 1) {
                throw new IllegalStateException("Không thể xóa quản lý đang hoạt động cuối cùng.");
            }
            if (truyCapTaiKhoan.coLichSu(maTaiKhoan)) {
                truyCapTaiKhoan.voHieuHoa(ketNoi, maTaiKhoan);
                return false;
            }
            truyCapTaiKhoan.xoaTaiKhoan(ketNoi, maTaiKhoan);
            return true;
        });
    }
}

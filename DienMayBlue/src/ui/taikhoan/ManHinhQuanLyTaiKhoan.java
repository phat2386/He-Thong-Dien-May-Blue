package ui.taikhoan;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.*;
import entity.*;
import ui.dungchung.*;
import java.util.List;
import ui.xacthuc.HopThoaiDoiMatKhau;
public class ManHinhQuanLyTaiKhoan extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final TaiKhoanService dichVu = new TaiKhoanService();
    private final JTextField txtTimKiem = TaoGiaoDien.oNhap(340);
    private final JComboBox<String> cmbLoaiNhanVien = TaoGiaoDien.oChon("Tất cả loại nhân viên");
    private final Map<String, String> maVaiTroTheoNhan = new LinkedHashMap<String, String>();
    private final JComboBox<String> cmbTrangThai = TaoGiaoDien.oChon("Tất cả trạng thái", "Đang hoạt động", "Bị khóa");
    private final JLabel lblTongTaiKhoan = taoGiaTriTongHop("0");
    private final JLabel lblDangHoatDong = taoGiaTriTongHop("0");
    private final JLabel lblBiKhoa = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinh = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã tài khoản", "Tên đăng nhập", "Mã NV", "Họ tên", "Loại nhân viên", "Trạng thái", "Đăng nhập gần nhất"
    });
    private final JTable tblTaiKhoan = TaoGiaoDien.bang(moHinh);
    public ManHinhQuanLyTaiKhoan() {
        super("Quản lý tài khoản", "Quản lý tài khoản đăng nhập của nhân viên và trạng thái khóa/mở tài khoản.");
        TaoGiaoDien.datChuGoiY(txtTimKiem, "Mã TK, tên đăng nhập, mã NV, họ tên hoặc SĐT");
        JButton btnThem = TaoGiaoDien.nutChinh("Thêm tài khoản");
        JButton btnSua = TaoGiaoDien.nutPhu("Đổi mật khẩu");
        JButton btnKhoa = TaoGiaoDien.nutPhu("Khóa / Kích hoạt");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        thaoTac.add(btnThem);
        thaoTac.add(btnSua);
        thaoTac.add(btnKhoa);
        thaoTac.add(btnLamMoi);
        JPanel pnlKhung = new JPanel(new BorderLayout(0, 12));
        pnlKhung.setOpaque(false);
        pnlKhung.add(taoKhungTongHop(), BorderLayout.NORTH);
        JPanel pnlTrungTam = new JPanel(new BorderLayout(0, 10));
        pnlTrungTam.setOpaque(false);
        pnlTrungTam.add(TaoGiaoDien.thanhCongCu(new JLabel("Tìm kiếm"), txtTimKiem, new JLabel("Loại nhân viên"), cmbLoaiNhanVien, new JLabel("Trạng thái"), cmbTrangThai), BorderLayout.NORTH);
        BangPhanTrang pnlCuon = TaoGiaoDien.bangPhanTrang(tblTaiKhoan);
        tblTaiKhoan.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        datDoRongCot();
        pnlTrungTam.add(pnlCuon, BorderLayout.CENTER);
        pnlKhung.add(pnlTrungTam, BorderLayout.CENTER);
        noiDung.add(pnlKhung, BorderLayout.CENTER);
        napBoLocLoaiNhanVien();
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimKiem, tblTaiKhoan, 0, m -> lamMoiDuLieu(), 0, 1, 2, 3);
        cmbLoaiNhanVien.addActionListener(e -> lamMoiDuLieu());
        cmbTrangThai.addActionListener(e -> lamMoiDuLieu());
        btnThem.addActionListener(e -> themTaiKhoan());
        btnSua.addActionListener(e -> suaTaiKhoan());
        btnKhoa.addActionListener(e -> khoaHoacKichHoat());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu());
    }
    private JPanel taoKhungTongHop() {
        JPanel pnlTongHop = new JPanel(new GridLayout(1, 3, 10, 0));
        pnlTongHop.setOpaque(false);
        pnlTongHop.add(taoTheTongHop("Tổng số tài khoản", lblTongTaiKhoan));
        pnlTongHop.add(taoTheTongHop("Đang hoạt động", lblDangHoatDong));
        pnlTongHop.add(taoTheTongHop("Bị khóa", lblBiKhoa));
        return pnlTongHop;
    }
    private JPanel taoTheTongHop(String tieuDe, JLabel lblGiaTri) {
        KhungBoGoc pnlThe = new KhungBoGoc(12, Color.WHITE);
        pnlThe.setLayout(new BorderLayout(0, 6));
        pnlThe.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel lblTieuDe = new JLabel(tieuDe);
        lblTieuDe.setFont(HangSoGiaoDien.MEDIUM);
        lblTieuDe.setForeground(HangSoGiaoDien.MUTED);
        pnlThe.add(lblTieuDe, BorderLayout.NORTH);
        pnlThe.add(lblGiaTri, BorderLayout.CENTER);
        return pnlThe;
    }
    private static JLabel taoGiaTriTongHop(String vanBan) {
        JLabel lblGiaTri = new JLabel(vanBan);
        lblGiaTri.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 24));
        lblGiaTri.setForeground(HangSoGiaoDien.TEXT);
        return lblGiaTri;
    }
    private void datDoRongCot() {
        int[] rong = {
            110, 150, 90, 180, 155, 120, 165
        };
        for (int i = 0; i < rong.length && i < tblTaiKhoan.getColumnCount(); i++) {
            tblTaiKhoan.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            capNhatTongHop();
            String tuKhoa = "%" + txtTimKiem.getText().trim() + "%";
            String loai = maLoaiNhanVienDangChon();
            String trangThai = maTrangThaiDangChon();
            List<TaiKhoan> danhSach = dichVu.timKiemTaiKhoan(tuKhoa, loai, trangThai);
            moHinh.setRowCount(0);
            for (TaiKhoan dong : danhSach) {
                moHinh.addRow(new Object[] {
                    dong.getMaTaiKhoan(), dong.getTenDangNhap(), dong.getNhanVien().getMaNhanVien(), dong.getNhanVien().getHoTen(), nhanLoaiNhanVien(dong),
                    nhanTrangThai(dong.getTrangThai()), DinhDangHienThi.ngay(dong.getLanDangNhapCuoi())
                });
            }
            if (!txtTimKiem.getText().trim().isEmpty() && moHinh.getRowCount() > 0) {
                TaoGiaoDien.chonDongDauTienSauKhiCapNhat(tblTaiKhoan);
            }
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void capNhatTongHop() throws Exception {
        List<TaiKhoan> tatCa = dichVu.timKiemTaiKhoan("%", null, null);
        int active = 0;
        int locked = 0;
        for (TaiKhoan r : tatCa) {
            if ("Hoạt động".equals(r.getTrangThai())) active++;
            else locked++;
        }
        lblTongTaiKhoan.setText(String.valueOf(tatCa.size()));
        lblDangHoatDong.setText(String.valueOf(active));
        lblBiKhoa.setText(String.valueOf(locked));
    }
    private void napBoLocLoaiNhanVien() {
        try {
            maVaiTroTheoNhan.clear();
            cmbLoaiNhanVien.removeAllItems();
            cmbLoaiNhanVien.addItem("Tất cả loại nhân viên");
            maVaiTroTheoNhan.put("Admin hệ thống", "ADMIN");
            cmbLoaiNhanVien.addItem("Admin hệ thống");
            for (VaiTro vaiTro : dichVu.vaiTroCoTheQuanLy()) {
                String nhan = vaiTro.getTenVaiTro();
                maVaiTroTheoNhan.put(nhan, vaiTro.getMaQuyenVaiTro());
                cmbLoaiNhanVien.addItem(nhan);
            }
            cmbLoaiNhanVien.setSelectedIndex(0);
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private String maLoaiNhanVienDangChon() {
        Object chon = cmbLoaiNhanVien.getSelectedItem();
        if (chon == null || "Tất cả loại nhân viên".equals(String.valueOf(chon))) return null;
        return maVaiTroTheoNhan.get(String.valueOf(chon));
    }
    private String maTrangThaiDangChon() {
        String chon = String.valueOf(cmbTrangThai.getSelectedItem());
        if ("Đang hoạt động".equals(chon)) return "Hoạt động";
        if ("Bị khóa".equals(chon)) return "Bị khóa";
        return null;
    }
    private String maTaiKhoanDangChon() {
        int vr = tblTaiKhoan.getSelectedRow();
        if (vr < 0) return null;
        return String.valueOf(moHinh.getValueAt(tblTaiKhoan.convertRowIndexToModel(vr), 0));
    }
    private void themTaiKhoan() {
        try {
            JComboBox<String> cmbNhanVien = new JComboBox<String>();
            for (NhanVien nv : dichVu.nhanVienChuaCoTaiKhoan()) {
                cmbNhanVien.addItem(nv.getMaNhanVien() + " - " + nv.getHoTen());
            }
            if (cmbNhanVien.getItemCount() == 0) {
                thongBao("Không còn nhân viên đang hoạt động nào chưa có tài khoản.");
                return;
            }
            JTextField txtMaTaiKhoan = TaoGiaoDien.oNhap(160);
            TaoGiaoDien.datGoiYMa(txtMaTaiKhoan, dichVu.goiYMaTaiKhoan());
            JTextField txtTenDangNhap = TaoGiaoDien.oNhap(200);
            JPasswordField txtMatKhau = TaoGiaoDien.oMatKhau(220);
            JPanel pnlBieuMau = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Mã tài khoản", txtMaTaiKhoan), TaoGiaoDien.truongBieuMau("Nhân viên", cmbNhanVien), TaoGiaoDien.truongBieuMau("Tên đăng nhập",
                txtTenDangNhap), TaoGiaoDien.truongBieuMau("Mật khẩu", txtMatKhau));
            if (!TienIchHopThoai.xacNhanBieuMau(this, pnlBieuMau, "Thêm tài khoản", "Tạo tài khoản")) return;
            String dongNhanVien = String.valueOf(cmbNhanVien.getSelectedItem());
            String maNhanVien = dongNhanVien.substring(0, dongNhanVien.indexOf(" - "));
            dichVu.taoTaiKhoanChoNhanVien(txtMaTaiKhoan.getText().trim(), maNhanVien, txtTenDangNhap.getText().trim(), new String(txtMatKhau.getPassword()));
            thongBao("Đã cấp tài khoản.");
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void suaTaiKhoan() {
        String maTaiKhoan = maTaiKhoanDangChon();
        if (maTaiKhoan == null) {
            thongBao("Hãy chọn tài khoản cần đổi mật khẩu.");
            return;
        }
        TaiKhoan hienTai = TaiKhoanService.getTaiKhoanHienTai();
        if (hienTai == null || !maTaiKhoan.equals(hienTai.getMaTaiKhoan())) {
            thongBao("Chỉ chủ tài khoản đang đăng nhập mới được đổi mật khẩu của chính mình.");
            return;
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        Frame chuSoHuu = w instanceof Frame ? (Frame) w : null;
        HopThoaiDoiMatKhau hopThoai = new HopThoaiDoiMatKhau(chuSoHuu, dichVu, () -> lamMoiDuLieu());
        hopThoai.setVisible(true);
    }
    private void khoaHoacKichHoat() {
        String maTaiKhoan = maTaiKhoanDangChon();
        if (maTaiKhoan == null) {
            thongBao("Hãy chọn tài khoản.");
            return;
        }
        try {
            TaiKhoan thongTin = dichVu.thongTinTaiKhoan(maTaiKhoan);
            if (thongTin == null) throw new IllegalArgumentException("Không tìm thấy tài khoản.");
            if (thongTin.getVaiTro() != null && "ADMIN".equals(thongTin.getVaiTro().getMaQuyenVaiTro())) {
                thongBao("Không được khóa tài khoản Admin hệ thống.");
                return;
            }
            String trangThaiHienTai = dichVu.trangThaiTaiKhoan(maTaiKhoan);
            String trangThaiMoi = "Hoạt động".equals(trangThaiHienTai) ? "Bị khóa" : "Hoạt động";
            String hanhDong = "Hoạt động".equals(trangThaiMoi) ? "Kích hoạt" : "Khóa";
            if (!xacNhan(hanhDong + " tài khoản " + maTaiKhoan + "?")) return;
            dichVu.doiTrangThaiTaiKhoan(maTaiKhoan, trangThaiMoi);
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private String nhanLoaiNhanVien(TaiKhoan dong) {
        if (dong.getVaiTro() != null && "ADMIN".equals(dong.getVaiTro().getMaQuyenVaiTro())) return "Admin hệ thống";
        return dong.getNhanVien() == null ? "" : DinhDangHienThi.anToan(dong.getNhanVien().getPhongBan());
    }
    private String nhanTrangThai(String trangThai) {
        return "Hoạt động".equals(trangThai) ? "Đang hoạt động" : "Bị khóa";
    }
}
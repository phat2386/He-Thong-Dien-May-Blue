package ui.giaohang;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import entity.*;
import service.*;
import ui.dungchung.*;
public class ManHinhQuanLyGiaoHang extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final PhieuGiaoHangService dichVu = new PhieuGiaoHangService();
    private final JTextField txtTimKiem = TaoGiaoDien.oNhap(260);
    private final JTextField txtNgayGiao = TaoGiaoDien.oNhap(120);
    private final JComboBox<String> cmbPhamVi = TaoGiaoDien.oChon("Tất cả phiếu", "Phiếu của tôi");
    private final JComboBox<String> cmbTrangThai = TaoGiaoDien.oChon("Tất cả trạng thái", "Đang chờ", "Hoàn tất");
    private final JLabel lblTong = taoGiaTriTongHop("0");
    private final JLabel lblDangCho = taoGiaTriTongHop("0");
    private final JLabel lblHoanTat = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinh = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã phiếu", "Mã KH", "Khách hàng", "Hóa đơn", "Mã NV giao", "Nhân viên giao", "Ngày giao", "Trạng thái"
    });
    private final JTable tblDanhSach = TaoGiaoDien.bang(moHinh);
    private final List<PhieuGiaoHang> duLieuHienTai = new ArrayList<>();
    private final JLabel lblMaPhieu = taoGiaTriChiTiet();
    private final JLabel lblMaKhachHang = taoGiaTriChiTiet();
    private final JLabel lblKhachHang = taoGiaTriChiTiet();
    private final JLabel lblHoaDon = taoGiaTriChiTiet();
    private final JLabel lblMaNhanVien = taoGiaTriChiTiet();
    private final JLabel lblNhanVien = taoGiaTriChiTiet();
    private final JLabel lblNgayGiao = taoGiaTriChiTiet();
    private final JLabel lblDiaChi = taoGiaTriChiTiet();
    private final JLabel lblLapDat = taoGiaTriChiTiet();
    private final JLabel lblPhi = taoGiaTriChiTiet();
    private final JLabel lblTrangThai = taoGiaTriChiTiet();
    private final JButton btnPhanCong = TaoGiaoDien.nutPhu("Đổi nhân viên giao");
    private final JButton btnHoanTat = TaoGiaoDien.nutChinh("Hoàn tất");
    public ManHinhQuanLyGiaoHang() {
        super("Quản lý phiếu giao hàng", "Theo dõi phiếu giao cho khách thành viên với hai trạng thái Đang chờ và Hoàn tất.");
        TaoGiaoDien.datChuGoiY(txtTimKiem, "Mã phiếu, mã KH, khách hàng, hóa đơn hoặc mã NV giao");
        if (TaiKhoanService.coQuyen("DELIVERY_ASSIGN")) {
            thaoTac.add(btnPhanCong);
        }
        thaoTac.add(btnHoanTat);
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        thaoTac.add(btnLamMoi);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(taoKhungTongHop(), BorderLayout.NORTH);
        JPanel trungTam = new JPanel(new BorderLayout(0, 10));
        trungTam.setOpaque(false);
        trungTam.add(taoThanhBoLoc(), BorderLayout.NORTH);
        BangPhanTrang cuon = TaoGiaoDien.bangPhanTrang(tblDanhSach);
        tblDanhSach.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        datDoRongCot();
        JPanel danhSach = new JPanel(new BorderLayout(0, 10));
        danhSach.setOpaque(false);
        danhSach.add(cuon, BorderLayout.CENTER);
        danhSach.add(taoKhungChiTiet(), BorderLayout.SOUTH);
        trungTam.add(danhSach, BorderLayout.CENTER);
        khung.add(trungTam, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimKiem, tblDanhSach, 0, m -> lamMoiDuLieu(), 0, 1, 2, 3, 4, 5);
        txtNgayGiao.addActionListener(e -> lamMoiDuLieu());
        cmbPhamVi.addActionListener(e -> lamMoiDuLieu());
        cmbTrangThai.addActionListener(e -> lamMoiDuLieu());
        tblDanhSach.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                capNhatChiTietDangChon();
            }
        });
        btnPhanCong.addActionListener(e -> doiNhanVienGiao());
        btnHoanTat.addActionListener(e -> hoanTat());
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            txtNgayGiao.setText("");
            cmbPhamVi.setSelectedIndex(0);
            cmbTrangThai.setSelectedIndex(0);
            lamMoiDuLieu();
        });
    }
    private JPanel taoThanhBoLoc() {
        JPanel p = new JPanel(new BoCucXuongDong(FlowLayout.LEFT, 8, 6));
        p.setOpaque(false);
        p.add(nhomLoc("Tìm kiếm", txtTimKiem));
        p.add(nhomLoc("Phạm vi", cmbPhamVi));
        p.add(nhomLoc("Ngày giao (dd/MM/yyyy)", txtNgayGiao));
        p.add(nhomLoc("Trạng thái", cmbTrangThai));
        return p;
    }
    private JPanel nhomLoc(String nhan, JComponent truong) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        p.add(new JLabel(nhan));
        p.add(truong);
        return p;
    }
    private JPanel taoKhungTongHop() {
        JPanel p = new JPanel(new GridLayout(1, 3, 10, 0));
        p.setOpaque(false);
        p.add(taoTheTongHop("Tổng số phiếu", lblTong));
        p.add(taoTheTongHop("Đang chờ", lblDangCho));
        p.add(taoTheTongHop("Hoàn tất", lblHoanTat));
        return p;
    }
    private JPanel taoTheTongHop(String tieuDe, JLabel giaTri) {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel t = new JLabel(tieuDe);
        t.setFont(HangSoGiaoDien.MEDIUM);
        t.setForeground(HangSoGiaoDien.MUTED);
        p.add(t, BorderLayout.NORTH);
        p.add(giaTri, BorderLayout.CENTER);
        return p;
    }
    private static JLabel taoGiaTriTongHop(String text) {
        JLabel l = new JLabel(text);
        l.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 24));
        l.setForeground(HangSoGiaoDien.TEXT);
        return l;
    }
    private static JLabel taoGiaTriChiTiet() {
        JLabel l = new JLabel("-");
        l.setFont(HangSoGiaoDien.NORMAL);
        l.setForeground(HangSoGiaoDien.TEXT);
        return l;
    }
    private JPanel taoKhungChiTiet() {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setMauVien(HangSoGiaoDien.BORDER);
        p.setLayout(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel t = new JLabel("Chi tiết phiếu giao hàng");
        t.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 16));
        p.add(t, BorderLayout.NORTH);
        JPanel g = new JPanel(new GridLayout(0, 3, 14, 6));
        g.setOpaque(false);
        g.add(taoDong("Mã phiếu", lblMaPhieu));
        g.add(taoDong("Mã khách hàng", lblMaKhachHang));
        g.add(taoDong("Khách hàng", lblKhachHang));
        g.add(taoDong("Hóa đơn", lblHoaDon));
        g.add(taoDong("Mã NV giao", lblMaNhanVien));
        g.add(taoDong("Nhân viên giao", lblNhanVien));
        g.add(taoDong("Ngày giao", lblNgayGiao));
        g.add(taoDong("Địa chỉ giao", lblDiaChi));
        g.add(taoDong("Lắp đặt", lblLapDat));
        g.add(taoDong("Phí giao", lblPhi));
        g.add(taoDong("Trạng thái", lblTrangThai));
        p.add(g, BorderLayout.CENTER);
        return p;
    }
    private JPanel taoDong(String ten, JLabel value) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(ten + ":");
        l.setForeground(HangSoGiaoDien.MUTED);
        p.add(l, BorderLayout.WEST);
        p.add(value, BorderLayout.CENTER);
        return p;
    }
    private void datDoRongCot() {
        int[] w = {90, 90, 160, 100, 100, 170, 110, 100};
        for (int i = 0; i < w.length && i < tblDanhSach.getColumnCount(); i++) {
            tblDanhSach.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            String phamVi = "Phiếu của tôi".equals(String.valueOf(cmbPhamVi.getSelectedItem())) ? "MINE" : "ALL";
            String maNhanVien = TaiKhoanService.getTaiKhoanHienTai() == null ? null : TaiKhoanService.getTaiKhoanHienTai().getNhanVien().getMaNhanVien();
            String trangThai = "Đang chờ".equals(String.valueOf(cmbTrangThai.getSelectedItem())) ? "WAITING" : "Hoàn tất".equals(String.valueOf(cmbTrangThai.getSelectedItem())) ? "Hoàn tất" : null;
            LocalDate ngay = null;
            String ngayText = txtNgayGiao.getText().trim();
            if (!ngayText.isEmpty()) {
                ngay = LocalDate.parse(ngayText, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            List<PhieuGiaoHang> tatCa = dichVu.layDanhSachGiaoHang(phamVi, maNhanVien, trangThai, ngay);
            String key = txtTimKiem.getText().trim().toLowerCase();
            duLieuHienTai.clear();
            moHinh.setRowCount(0);
            int cho = 0;
            int xong = 0;
            for (PhieuGiaoHang p : tatCa) {
                if ("WAITING".equals(p.getTrangThai())) cho++;
                else if ("Hoàn tất".equals(p.getTrangThai())) xong++;
                String tong = (p.getMaPhieu() + " " + p.getKhachHang().getMaKhachHang() + " " + p.getKhachHang().getHoTen() + " " + p.getHoaDon().getMaHoaDon() + " " + p.getNhanVienGiaoHang().getMaNhanVien() + " " + p.getNhanVienGiaoHang().getHoTen()).toLowerCase();
                if (!key.isEmpty() && !tong.contains(key)) continue;
                duLieuHienTai.add(p);
                moHinh.addRow(new Object[] {
                    p.getMaPhieu(), p.getKhachHang().getMaKhachHang(), p.getKhachHang().getHoTen(), p.getHoaDon().getMaHoaDon(), p.getNhanVienGiaoHang().getMaNhanVien(), p.getNhanVienGiaoHang().getHoTen(), DinhDangHienThi.ngay(p.getNgayGiao()), nhanTrangThai(p.getTrangThai())
                });
            }
            lblTong.setText(String.valueOf(tatCa.size()));
            lblDangCho.setText(String.valueOf(cho));
            lblHoanTat.setText(String.valueOf(xong));
            if (moHinh.getRowCount() > 0) TaoGiaoDien.chonDongDauTienSauKhiCapNhat(tblDanhSach);
            else xoaChiTiet();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void capNhatChiTietDangChon() {
        int vr = tblDanhSach.getSelectedRow();
        if (vr < 0) {
            xoaChiTiet();
            return;
        }
        int r = tblDanhSach.convertRowIndexToModel(vr);
        if (r < 0 || r >= duLieuHienTai.size()) {
            xoaChiTiet();
            return;
        }
        PhieuGiaoHang p = duLieuHienTai.get(r);
        lblMaPhieu.setText(p.getMaPhieu());
        lblMaKhachHang.setText(p.getKhachHang().getMaKhachHang());
        lblKhachHang.setText(p.getKhachHang().getHoTen());
        lblHoaDon.setText(p.getHoaDon().getMaHoaDon());
        lblMaNhanVien.setText(p.getNhanVienGiaoHang().getMaNhanVien());
        lblNhanVien.setText(p.getNhanVienGiaoHang().getHoTen());
        lblNgayGiao.setText(DinhDangHienThi.ngay(p.getNgayGiao()));
        lblDiaChi.setText(p.getDiaChiGiao());
        lblLapDat.setText(p.isCanLapDat() ? "Có" : "Không");
        lblPhi.setText(DinhDangHienThi.tien(p.getPhiGiaoHang()));
        lblTrangThai.setText(nhanTrangThai(p.getTrangThai()));
        btnPhanCong.setEnabled("WAITING".equals(p.getTrangThai()));
        btnHoanTat.setEnabled("WAITING".equals(p.getTrangThai()));
    }
    private PhieuGiaoHang phieuDangChon() {
        int vr = tblDanhSach.getSelectedRow();
        if (vr < 0) return null;
        int r = tblDanhSach.convertRowIndexToModel(vr);
        return r >= 0 && r < duLieuHienTai.size() ? duLieuHienTai.get(r) : null;
    }
    private void doiNhanVienGiao() {
        PhieuGiaoHang p = phieuDangChon();
        if (p == null) {
            thongBao("Hãy chọn phiếu giao hàng.");
            return;
        }
        try {
            List<NhanVien> ds = dichVu.layNhanVienGiaoHangPhuHop(p.getMaPhieu());
            List<String> luaChon = new ArrayList<>();
            for (NhanVien nv : ds) {
                if (!nv.getMaNhanVien().equals(p.getNhanVienGiaoHang().getMaNhanVien())) {
                    luaChon.add(nv.getMaNhanVien() + " - " + nv.getHoTen());
                }
            }
            if (luaChon.isEmpty()) {
                thongBao("Không có nhân viên khác đang rảnh (không có ca tại cửa hàng) trong ngày giao.");
                return;
            }
            Object chon = JOptionPane.showInputDialog(this, "Chọn nhân viên giao hàng thay thế", "Đổi nhân viên giao", JOptionPane.QUESTION_MESSAGE, null, luaChon.toArray(), luaChon.get(0));
            if (chon == null) return;
            String text = String.valueOf(chon);
            String ma = text.substring(0, text.indexOf(" - "));
            if (!xacNhan("Đổi nhân viên giao của phiếu " + p.getMaPhieu() + " sang " + text + "?")) return;
            dichVu.phanCongGiaoHang(p.getMaPhieu(), ma, true);
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void hoanTat() {
        PhieuGiaoHang p = phieuDangChon();
        if (p == null) {
            thongBao("Hãy chọn phiếu giao hàng.");
            return;
        }
        if (!xacNhan("Xác nhận phiếu " + p.getMaPhieu() + " đã hoàn tất giao hàng?")) return;
        try {
            dichVu.hoanTatGiaoHang(p.getMaPhieu());
            lamMoiDuLieu();
            thongBao("Đã hoàn tất giao hàng.");
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private String nhanTrangThai(String ma) {
        return "Hoàn tất".equals(ma) ? "Hoàn tất" : "Đang chờ";
    }
    private void xoaChiTiet() {
        for (JLabel l : Arrays.asList(lblMaPhieu, lblMaKhachHang, lblKhachHang, lblHoaDon, lblMaNhanVien, lblNhanVien, lblNgayGiao, lblDiaChi, lblLapDat, lblPhi, lblTrangThai)) {
            l.setText("-");
        }
        btnPhanCong.setEnabled(false);
        btnHoanTat.setEnabled(false);
    }
}

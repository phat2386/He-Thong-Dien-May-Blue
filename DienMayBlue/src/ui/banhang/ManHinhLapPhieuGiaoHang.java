package ui.banhang;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import entity.*;
import service.PhieuGiaoHangService;
import ui.dungchung.*;
public class ManHinhLapPhieuGiaoHang extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final PhieuGiaoHangService dichVu = new PhieuGiaoHangService();
    private final JTextField txtMaPhieu = TaoGiaoDien.oNhap(150);
    private final JComboBox<String> cmbHoaDon = TaoGiaoDien.oChon();
    private final JTextField txtNgayGiao = TaoGiaoDien.oNhap(130);
    private final JTextField txtMaKhachHang = TaoGiaoDien.oNhap(110);
    private final JTextField txtKhachHang = TaoGiaoDien.oNhap(220);
    private final JTextField txtSoDienThoai = TaoGiaoDien.oNhap(140);
    private final JTextField txtDiaChiGiao = TaoGiaoDien.oNhap(360);
    private final JComboBox<String> cmbNhanVienGiao = TaoGiaoDien.oChon();
    private final JCheckBox chkLapDat = new JCheckBox("Có lắp đặt");
    private final JTextField txtPhiGiaoHang = TaoGiaoDien.oNhap(130);
    private final DefaultTableModel moHinhChiTiet = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã SP", "Tên sản phẩm", "Số lượng", "Đơn giá", "Thành tiền"
    });
    private final JTable tblChiTiet = TaoGiaoDien.bang(moHinhChiTiet);
    private final Map<String, HoaDon> hoaDonTheoMa = new LinkedHashMap<>();
    private final Map<String, NhanVien> nhanVienTheoMa = new LinkedHashMap<>();
    public ManHinhLapPhieuGiaoHang() {
        super("Lập phiếu giao hàng", "Chỉ lập phiếu cho hóa đơn đã thanh toán của khách hàng thành viên.");
        JButton btnTao = TaoGiaoDien.nutChinh("Tạo phiếu");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        thaoTac.add(btnTao);
        thaoTac.add(btnLamMoi);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(taoKhungBieuMau(), BorderLayout.NORTH);
        KhungBoGoc chiTiet = new KhungBoGoc(12, Color.WHITE);
        chiTiet.setMauVien(HangSoGiaoDien.BORDER);
        chiTiet.setLayout(new BorderLayout(0, 8));
        chiTiet.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel tieuDe = new JLabel("Sản phẩm giao từ hóa đơn đã chọn");
        tieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 15));
        chiTiet.add(tieuDe, BorderLayout.NORTH);
        chiTiet.add(TaoGiaoDien.bangPhanTrang(tblChiTiet), BorderLayout.CENTER);
        khung.add(chiTiet, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        for (JTextField field : Arrays.asList(txtMaKhachHang, txtKhachHang, txtSoDienThoai, txtPhiGiaoHang)) {
            field.setEditable(false);
            field.setBackground(new Color(248, 250, 252));
        }
        chkLapDat.setOpaque(false);
        cmbHoaDon.addActionListener(e -> taiHoaDonDangChon());
        txtNgayGiao.addActionListener(e -> taiNhanVienGiaoHang());
        chkLapDat.addActionListener(e -> capNhatPhi());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu());
        btnTao.addActionListener(e -> luuPhieuGiao());
    }
    private JPanel taoKhungBieuMau() {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setMauVien(HangSoGiaoDien.BORDER);
        p.setLayout(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int y = 0;
        y = dong(p, c, y, "Mã phiếu giao", txtMaPhieu, "Hóa đơn", cmbHoaDon, "Ngày giao", txtNgayGiao);
        y = dong(p, c, y, "Mã khách hàng", txtMaKhachHang, "Khách hàng", txtKhachHang, "Số điện thoại", txtSoDienThoai);
        y = dong(p, c, y, "Nhân viên giao hàng", cmbNhanVienGiao, "Yêu cầu lắp đặt", chkLapDat, "Phí giao hàng", txtPhiGiaoHang);
        c.gridy = y;
        c.gridx = 0;
        c.weightx = 0;
        p.add(new JLabel("Địa chỉ giao"), c);
        c.gridx = 1;
        c.gridwidth = 5;
        c.weightx = 1;
        p.add(txtDiaChiGiao, c);
        return p;
    }
    private int dong(JPanel p, GridBagConstraints c, int y, String l1, Component v1, String l2, Component v2, String l3, Component v3) {
        String[] labels = {l1, l2, l3};
        Component[] values = {v1, v2, v3};
        c.gridy = y;
        c.gridwidth = 1;
        for (int i = 0; i < 3; i++) {
            c.gridx = i * 2;
            c.weightx = 0;
            p.add(new JLabel(labels[i]), c);
            c.gridx = i * 2 + 1;
            c.weightx = 1;
            p.add(values[i], c);
        }
        return y + 1;
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            TaoGiaoDien.datGoiYMa(txtMaPhieu, dichVu.goiYMaPhieuGiao());
            txtNgayGiao.setText(LocalDate.now().plusDays(1).toString());
            hoaDonTheoMa.clear();
            cmbHoaDon.removeAllItems();
            for (HoaDon hd : dichVu.layHoaDonCoTheGiao()) {
                hoaDonTheoMa.put(hd.getMaHoaDon(), hd);
                cmbHoaDon.addItem(hd.getMaHoaDon() + " - " + hd.getKhachHang().getMaKhachHang() + " - " + hd.getKhachHang().getHoTen() + " - " + DinhDangHienThi.tien(hd.getTongTien()));
            }
            chkLapDat.setSelected(false);
            taiHoaDonDangChon();
            taiNhanVienGiaoHang();
            capNhatPhi();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private String maHoaDonDangChon() {
        Object item = cmbHoaDon.getSelectedItem();
        if (item == null) return null;
        String s = String.valueOf(item);
        int i = s.indexOf(" - ");
        return i > 0 ? s.substring(0, i) : null;
    }
    private String maNhanVienDangChon() {
        Object item = cmbNhanVienGiao.getSelectedItem();
        if (item == null) return null;
        String s = String.valueOf(item);
        int i = s.indexOf(" - ");
        return i > 0 ? s.substring(0, i) : null;
    }
    private void taiHoaDonDangChon() {
        moHinhChiTiet.setRowCount(0);
        String maHoaDon = maHoaDonDangChon();
        HoaDon hd = maHoaDon == null ? null : hoaDonTheoMa.get(maHoaDon);
        if (hd == null) {
            txtMaKhachHang.setText("");
            txtKhachHang.setText("");
            txtSoDienThoai.setText("");
            txtDiaChiGiao.setText("");
            return;
        }
        KhachHang kh = hd.getKhachHang();
        txtMaKhachHang.setText(kh.getMaKhachHang());
        txtKhachHang.setText(kh.getHoTen());
        txtSoDienThoai.setText(kh.getSoDienThoai());
        txtDiaChiGiao.setText(kh.getDiaChi() == null ? "" : kh.getDiaChi());
        try {
            for (ChiTietHoaDon r : dichVu.layChiTietConLaiDeGiao(maHoaDon)) {
                moHinhChiTiet.addRow(new Object[] {
                    r.getSanPham().getMaSanPham(), r.getSanPham().getTenSanPham(), r.getSoLuong(), DinhDangHienThi.tien(r.getDonGia()), DinhDangHienThi.tien(r.getThanhTien())
                });
            }
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void taiNhanVienGiaoHang() {
        nhanVienTheoMa.clear();
        cmbNhanVienGiao.removeAllItems();
        try {
            LocalDate ngay = LocalDate.parse(txtNgayGiao.getText().trim());
            List<NhanVien> danhSach = dichVu.layNhanVienGiaoHangPhuHop(ngay);
            for (NhanVien nv : danhSach) {
                nhanVienTheoMa.put(nv.getMaNhanVien(), nv);
                cmbNhanVienGiao.addItem(nv.getMaNhanVien() + " - " + nv.getHoTen());
            }
        } catch (Exception ex) {
            cmbNhanVienGiao.removeAllItems();
        }
    }
    private void capNhatPhi() {
        BigDecimal phi = dichVu.tinhPhiGiaoHang(chkLapDat.isSelected());
        txtPhiGiaoHang.setText(DinhDangHienThi.tien(phi));
    }
    private void luuPhieuGiao() {
        try {
            String maHoaDon = maHoaDonDangChon();
            String maNhanVien = maNhanVienDangChon();
            if (maHoaDon == null) {
                throw new IllegalArgumentException("Không có hóa đơn thành viên đủ điều kiện giao hàng.");
            }
            if (maNhanVien == null) {
                throw new IllegalArgumentException("Không có nhân viên đang rảnh (không có ca tại cửa hàng) trong ngày giao.");
            }
            String maPhieu = dichVu.taoPhieuGiaoHang(txtMaPhieu.getText().trim(), maHoaDon, maNhanVien, txtDiaChiGiao.getText().trim(), LocalDate.parse(txtNgayGiao.getText().trim()), chkLapDat.isSelected());
            thongBao("Đã tạo phiếu giao hàng " + maPhieu + ".");
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
}

package ui.banhang;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.*;
import entity.*;
import ui.dungchung.*;
public class ManHinhLapPhieuDoiTra extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final PhieuDoiTraHoanTienService dichVu = new PhieuDoiTraHoanTienService();
    private final JTextField txtTimKiem = TaoGiaoDien.oNhap(300);
    private final JTextField txtMaPhieu = TaoGiaoDien.oNhap(180);
    private final JComboBox<String> cmbHoaDon = new JComboBox<String>();
    private final JTextField txtLyDo = TaoGiaoDien.oNhap(300);
    private final JTextField txtTinhTrang = TaoGiaoDien.oNhap(300);
    private final JTextField txtTienHoan = TaoGiaoDien.oNhap(180);
    private final JCheckBox chkTemMac = new JCheckBox("Còn tem/mác", true);
    private final JCheckBox chkDayDuHop = new JCheckBox("Đầy đủ hộp", true);
    private final JCheckBox chkPhuKien = new JCheckBox("Đủ phụ kiện", true);
    private final JLabel lblHoaDon = taoGiaTriThongTin("-");
    private final JLabel lblMaKhachHang = taoGiaTriThongTin("-");
    private final JLabel lblMaNhanVienBanHang = taoGiaTriThongTin("-");
    private final JLabel lblKhachHang = taoGiaTriThongTin("-");
    private final JLabel lblSoDienThoai = taoGiaTriThongTin("-");
    private final JLabel lblNgayLap = taoGiaTriThongTin("-");
    private final JLabel lblTongTien = taoGiaTriThongTin("-");
    private final JLabel lblQuyDinh = new JLabel("Chọn một hoặc nhiều sản phẩm để hệ thống tính tổng giá trị hoàn.");
    private final Map<String, HoaDon> hoaDonTheoMa = new LinkedHashMap<>();
    private final List<ChiTietHoaDon> chiTietHienTai = new ArrayList<>();
    private boolean dangCapNhatBang;
    private boolean dangNapHoaDon;
    private final DefaultTableModel moHinhSanPham = new DefaultTableModel(new Object[] {
        "Chọn", "Mã SP", "Sản phẩm", "SL mua", "SL trả", "Giá trị hoàn"
    }, 0) {
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0 || column == 4;
        }
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;
            if (columnIndex == 3 || columnIndex == 4) return Integer.class;
            return String.class;
        }
    };
    private final JTable tblSanPham = TaoGiaoDien.bang(moHinhSanPham);
    public ManHinhLapPhieuDoiTra() {
        super("Lập phiếu đổi trả / hoàn tiền", "Một phiếu có thể đổi trả nhiều sản phẩm thuộc cùng hóa đơn đã thanh toán.");
        TaoGiaoDien.datChuGoiY(txtTimKiem, "Mã hóa đơn, tên khách hàng hoặc SĐT");
        JButton btnTraCuu = TaoGiaoDien.nutPhu("Tra cứu");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        JButton btnLuu = TaoGiaoDien.nutChinh("Lập phiếu");
        thaoTac.add(btnLuu);
        thaoTac.add(btnLamMoi);
        txtTienHoan.setEditable(false);
        txtTienHoan.setBackground(new Color(248, 250, 252));
        lblQuyDinh.setFont(HangSoGiaoDien.SMALL);
        lblQuyDinh.setForeground(HangSoGiaoDien.MUTED);
        datCoGian(cmbHoaDon, 300);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(TaoGiaoDien.thanhCongCu(new JLabel("Tìm hóa đơn"), txtTimKiem, btnTraCuu), BorderLayout.NORTH);
        JPanel giua = new JPanel(new BorderLayout(0, 12));
        giua.setOpaque(false);
        giua.add(taoKhungThongTinHoaDon(), BorderLayout.NORTH);
        giua.add(taoKhungBieuMau(), BorderLayout.CENTER);
        khung.add(giua, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        cmbHoaDon.addActionListener(e -> { if (!dangNapHoaDon) taiChiTiet(); });
        moHinhSanPham.addTableModelListener(e -> {
            if (!dangCapNhatBang && (e.getColumn() == 0 || e.getColumn() == 4 || e.getColumn() == -1)) capNhatTongTienHoan();
        });
        btnTraCuu.addActionListener(e -> traCuuHoaDon());
        txtTimKiem.addActionListener(e -> traCuuHoaDon());
        GoiYTimKiem.gan(txtTimKiem, q -> {
            List<GoiYTimKiem.Muc> goiY = new ArrayList<>();
            try {
                for (HoaDon hd : dichVu.layHoaDonCoTheDoiTra(q)) {
                    goiY.add(new GoiYTimKiem.Muc(hd.getMaHoaDon(), hd.getMaHoaDon() + " - " + hd.getKhachHang().getMaKhachHang() + " - " + hd.getKhachHang().getHoTen() + " - " + hd.getKhachHang().getSoDienThoai()));
                    if (goiY.size() >= 12) break;
                }
            } catch (Exception ex) { }
            return goiY;
        }, m -> traCuuHoaDon());
        btnLamMoi.addActionListener(e -> lamMoiToanBo());
        btnLuu.addActionListener(e -> luuPhieuDoiTra());
        lamMoiToanBo();
    }
    private JPanel taoKhungThongTinHoaDon() {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setMauVien(HangSoGiaoDien.BORDER);
        p.setLayout(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;
        y = dongThongTin(p, c, y, "Hóa đơn", lblHoaDon, "Mã khách hàng", lblMaKhachHang);
        y = dongThongTin(p, c, y, "Khách hàng", lblKhachHang, "Số điện thoại", lblSoDienThoai);
        y = dongThongTin(p, c, y, "Mã NV bán hàng", lblMaNhanVienBanHang, "Ngày lập", lblNgayLap);
        dongThongTin(p, c, y, "Tổng tiền", lblTongTien, "Điều kiện", taoGiaTriThongTin("Trong 7 ngày, lỗi nhà sản xuất"));
        return p;
    }
    private JPanel taoKhungBieuMau() {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setMauVien(HangSoGiaoDien.BORDER);
        p.setLayout(new BorderLayout(0, 10));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel tren = new JPanel(new GridBagLayout());
        tren.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;
        y = dong(tren, c, y, "Mã phiếu", txtMaPhieu, "Hóa đơn", cmbHoaDon);
        y = dong(tren, c, y, "Lý do đổi trả", txtLyDo, "Tình trạng thực tế", txtTinhTrang);
        JPanel dieuKien = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        dieuKien.setOpaque(false);
        dieuKien.add(chkTemMac);
        dieuKien.add(chkDayDuHop);
        dieuKien.add(chkPhuKien);
        dong(tren, c, y, "Điều kiện sản phẩm", dieuKien, "Tổng giá trị hoàn", txtTienHoan);
        p.add(tren, BorderLayout.NORTH);
        JPanel bang = new JPanel(new BorderLayout(0, 6));
        bang.setOpaque(false);
        JLabel tieuDe = new JLabel("Sản phẩm đổi trả - chọn sản phẩm và điều chỉnh SL trả");
        tieuDe.setFont(HangSoGiaoDien.MEDIUM);
        JPanel dauBang = new JPanel(new BorderLayout(8, 0));
        dauBang.setOpaque(false);
        dauBang.add(tieuDe, BorderLayout.WEST);
        JPanel nutSoLuong = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        nutSoLuong.setOpaque(false);
        JButton btnGiam = TaoGiaoDien.nutPhu("− SL trả");
        JButton btnTang = TaoGiaoDien.nutPhu("+ SL trả");
        btnGiam.addActionListener(e -> dieuChinhSoLuongTra(-1));
        btnTang.addActionListener(e -> dieuChinhSoLuongTra(1));
        nutSoLuong.add(btnGiam);
        nutSoLuong.add(btnTang);
        dauBang.add(nutSoLuong, BorderLayout.EAST);
        bang.add(dauBang, BorderLayout.NORTH);
        BangPhanTrang cuon = TaoGiaoDien.bangPhanTrang(tblSanPham);
        bang.add(cuon, BorderLayout.CENTER);
        p.add(bang, BorderLayout.CENTER);
        p.add(lblQuyDinh, BorderLayout.SOUTH);
        return p;
    }
    private void dieuChinhSoLuongTra(int delta) {
        int viewRow = tblSanPham.getSelectedRow();
        if (viewRow < 0) {
            thongBao("Hãy chọn sản phẩm cần điều chỉnh số lượng trả.");
            return;
        }
        int row = tblSanPham.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= chiTietHienTai.size()) return;
        int toiDa = chiTietHienTai.get(row).getSoLuong();
        Object hienTai = moHinhSanPham.getValueAt(row, 4);
        int soLuong = hienTai instanceof Number ? ((Number) hienTai).intValue() : 1;
        soLuong = Math.max(1, Math.min(toiDa, soLuong + delta));
        dangCapNhatBang = true;
        moHinhSanPham.setValueAt(Boolean.TRUE, row, 0);
        moHinhSanPham.setValueAt(Integer.valueOf(soLuong), row, 4);
        dangCapNhatBang = false;
        capNhatTongTienHoan();
    }
    private int dong(JPanel p, GridBagConstraints c, int y, String l1, Component v1, String l2, Component v2) {
        c.gridwidth = 1;
        c.gridy = y;
        c.weightx = 0;
        c.gridx = 0;
        p.add(nhan(l1), c);
        c.weightx = .52;
        c.gridx = 1;
        p.add(v1, c);
        c.weightx = 0;
        c.gridx = 2;
        p.add(nhan(l2), c);
        c.weightx = .48;
        c.gridx = 3;
        p.add(v2, c);
        return y + 1;
    }
    private int dongThongTin(JPanel p, GridBagConstraints c, int y, String l1, Component v1, String l2, Component v2) {
        return dong(p, c, y, l1, v1, l2, v2);
    }
    private JLabel nhan(String s) {
        JLabel l = new JLabel(s);
        l.setFont(HangSoGiaoDien.MEDIUM);
        l.setForeground(HangSoGiaoDien.MUTED);
        return l;
    }
    private static JLabel taoGiaTriThongTin(String s) {
        JLabel l = new JLabel(s);
        l.setFont(HangSoGiaoDien.NORMAL);
        l.setForeground(HangSoGiaoDien.TEXT);
        return l;
    }
    private void datCoGian(JComponent component, int minWidth) {
        Dimension d = component.getPreferredSize();
        component.setMinimumSize(new Dimension(minWidth, Math.max(32, d.height)));
    }
    @Override
    public void lamMoiDuLieu() {
        traCuuHoaDon();
    }
    private void traCuuHoaDon() {
        String tuKhoa = txtTimKiem.getText().trim();
        if (tuKhoa.isEmpty()) { xoaKetQuaTraCuu(); return; }
        try {
            List<HoaDon> ketQua = dichVu.layHoaDonCoTheDoiTra(tuKhoa);
            dangNapHoaDon = true;
            cmbHoaDon.removeAllItems();
            hoaDonTheoMa.clear();
            for (HoaDon hd : ketQua) {
                hoaDonTheoMa.put(hd.getMaHoaDon(), hd);
                cmbHoaDon.addItem(hd.getMaHoaDon() + " - " + hd.getKhachHang().getMaKhachHang() + " - " + hd.getKhachHang().getHoTen() + " - " + hd.getKhachHang().getSoDienThoai());
            }
            cmbHoaDon.setSelectedIndex(-1);
            dangNapHoaDon = false;
            xoaDuLieuHoaDonDangChon();
            if (ketQua.size() == 1) {
                cmbHoaDon.setSelectedIndex(0);
                taiChiTiet();
            } else if (ketQua.isEmpty()) {
                lblQuyDinh.setText("Không tìm thấy hóa đơn đủ điều kiện đổi trả.");
            } else {
                lblQuyDinh.setText("Có " + ketQua.size() + " hóa đơn phù hợp. Hãy chọn đúng hóa đơn trong danh sách.");
            }
        } catch (Exception ex) {
            dangNapHoaDon = false;
            hienLoi(ex);
        }
    }
    private void xoaKetQuaTraCuu() {
        dangNapHoaDon = true;
        cmbHoaDon.removeAllItems();
        hoaDonTheoMa.clear();
        dangNapHoaDon = false;
        xoaDuLieuHoaDonDangChon();
        lblQuyDinh.setText("Nhập mã hóa đơn, tên khách hàng hoặc SĐT để tra cứu.");
    }
    private void xoaDuLieuHoaDonDangChon() {
        chiTietHienTai.clear();
        dangCapNhatBang = true;
        moHinhSanPham.setRowCount(0);
        dangCapNhatBang = false;
        txtTienHoan.setText("0đ");
        xoaThongTinHoaDon();
    }
    private void taiChiTiet() {
        chiTietHienTai.clear();
        dangCapNhatBang = true;
        moHinhSanPham.setRowCount(0);
        dangCapNhatBang = false;
        String item = String.valueOf(cmbHoaDon.getSelectedItem());
        if (item == null || "null".equals(item) || !item.contains(" - ")) {
            txtTienHoan.setText("");
            xoaThongTinHoaDon();
            return;
        }
        String maHoaDon = item.substring(0, item.indexOf(" - "));
        try {
            if (txtMaPhieu.getText().trim().isEmpty()) TaoGiaoDien.datGoiYMa(txtMaPhieu, dichVu.goiYMaPhieuDoiTra());
            capNhatThongTinHoaDon(maHoaDon);
            chiTietHienTai.addAll(dichVu.laySanPhamTrongHoaDon(maHoaDon));
            dangCapNhatBang = true;
            for (ChiTietHoaDon dong : chiTietHienTai) {
                BigDecimal motSanPham = dichVu.tinhTienHoan(dong.getHoaDon().getMaHoaDon(), dong.getSanPham().getMaSanPham(), 1);
                moHinhSanPham.addRow(new Object[] {false, dong.getSanPham().getMaSanPham(), dong.getSanPham().getTenSanPham(), dong.getSoLuong(), 1, DinhDangHienThi.tien(motSanPham)});
            }
            dangCapNhatBang = false;
            capNhatTongTienHoan();
        } catch (Exception ex) {
            dangCapNhatBang = false;
            hienLoi(ex);
        }
    }
    private void capNhatThongTinHoaDon(String maHoaDon) {
        HoaDon hd = hoaDonTheoMa.get(maHoaDon);
        if (hd == null) {
            xoaThongTinHoaDon();
            return;
        }
        lblHoaDon.setText(hd.getMaHoaDon());
        lblMaKhachHang.setText(hd.getKhachHang().getMaKhachHang());
        lblKhachHang.setText(hd.getKhachHang().getHoTen());
        lblSoDienThoai.setText(hd.getKhachHang().getSoDienThoai());
        lblMaNhanVienBanHang.setText(hd.getNhanVien().getMaNhanVien());
        lblNgayLap.setText(DinhDangHienThi.ngay(hd.getNgayLap()));
        lblTongTien.setText(DinhDangHienThi.tien(hd.getTongTien()));
    }
    private void xoaThongTinHoaDon() {
        lblHoaDon.setText("-");
        lblMaKhachHang.setText("-");
        lblMaNhanVienBanHang.setText("-");
        lblKhachHang.setText("-");
        lblSoDienThoai.setText("-");
        lblNgayLap.setText("-");
        lblTongTien.setText("-");
    }
    private List<PhieuDoiTraHoanTienService.MatHangDoiTra> layMatHangDaChon() {
        List<PhieuDoiTraHoanTienService.MatHangDoiTra> ketQua = new ArrayList<>();
        for (int i = 0; i < moHinhSanPham.getRowCount(); i++) {
            if (!Boolean.TRUE.equals(moHinhSanPham.getValueAt(i, 0))) continue;
            int soLuong;
            try {
                soLuong = Integer.parseInt(String.valueOf(moHinhSanPham.getValueAt(i, 4)));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Số lượng trả ở dòng " + (i + 1) + " không hợp lệ.");
            }
            ChiTietHoaDon goc = chiTietHienTai.get(i);
            if (soLuong <= 0 || soLuong > goc.getSoLuong()) throw new IllegalArgumentException("SL trả của " + goc.getSanPham().getMaSanPham() + " phải từ 1 đến " + goc.getSoLuong() + ".");
            ketQua.add(new PhieuDoiTraHoanTienService.MatHangDoiTra(goc.getSanPham().getMaSanPham(), soLuong));
        }
        return ketQua;
    }
    private void capNhatTongTienHoan() {
        if (dangCapNhatBang) return;
        try {
            dangCapNhatBang = true;
            BigDecimal tong = BigDecimal.ZERO;
            for (int i = 0; i < moHinhSanPham.getRowCount(); i++) {
                ChiTietHoaDon goc = chiTietHienTai.get(i);
                int soLuong;
                try {
                    soLuong = Integer.parseInt(String.valueOf(moHinhSanPham.getValueAt(i, 4)));
                } catch (Exception ex) {
                    soLuong = 1;
                }
                if (soLuong < 1) soLuong = 1;
                if (soLuong > goc.getSoLuong()) soLuong = goc.getSoLuong();
                BigDecimal tien = dichVu.tinhTienHoan(goc.getHoaDon().getMaHoaDon(), goc.getSanPham().getMaSanPham(), soLuong);
                moHinhSanPham.setValueAt(soLuong, i, 4);
                moHinhSanPham.setValueAt(DinhDangHienThi.tien(tien), i, 5);
                if (Boolean.TRUE.equals(moHinhSanPham.getValueAt(i, 0))) tong = tong.add(tien);
            }
            txtTienHoan.setText(DinhDangHienThi.tien(tong));
            BigDecimal hanMuc = dichVu.layHanMucDuyetHoanTien();
            lblQuyDinh.setText(tong.signum() == 0 ? "Chọn ít nhất một sản phẩm để đổi trả." : tong.compareTo(hanMuc) > 0 ?
                "Tổng hoàn vượt " + DinhDangHienThi.tien(hanMuc) + ": phiếu sẽ chuyển Quản lý duyệt." : "Tổng hoàn trong hạn mức: hệ thống xử lý sau khi xác nhận.");
        } catch (Exception ex) {
            txtTienHoan.setText("");
            lblQuyDinh.setText(ex.getMessage());
        } finally {
            dangCapNhatBang = false;
        }
    }
    private void luuPhieuDoiTra() {
        try {
            String maPhieu = txtMaPhieu.getText().trim();
            if (maPhieu.isEmpty()) throw new IllegalArgumentException("Mã phiếu không được để trống.");
            String hoaDonDangChon = String.valueOf(cmbHoaDon.getSelectedItem());
            if (hoaDonDangChon == null || "null".equals(hoaDonDangChon) || !hoaDonDangChon.contains(" - ")) throw new IllegalArgumentException("Chưa chọn hóa đơn.");
            String maHoaDon = hoaDonDangChon.substring(0, hoaDonDangChon.indexOf(" - "));
            List<PhieuDoiTraHoanTienService.MatHangDoiTra> matHang = layMatHangDaChon();
            if (matHang.isEmpty()) throw new IllegalArgumentException("Phải chọn ít nhất một sản phẩm đổi trả.");
            BigDecimal soTien = dichVu.tinhTongTienHoan(maHoaDon, matHang);
            BigDecimal hanMuc = dichVu.layHanMucDuyetHoanTien();
            boolean vuotHanMuc = soTien.compareTo(hanMuc) > 0;
            String noiDung = "Xác nhận lập phiếu " + maPhieu + " cho " + matHang.size() + " sản phẩm?\nTổng giá trị hoàn: " + DinhDangHienThi.tien(soTien) +
                (vuotHanMuc ? "\nPhiếu sẽ chuyển Quản lý duyệt." : "\nPhiếu sẽ được xử lý trong hạn mức.");
            if (!xacNhan(noiDung)) return;
            String maDaLuu = dichVu.taoPhieuDoiTra(maPhieu, maHoaDon, matHang, "Lỗi nhà sản xuất", txtLyDo.getText().trim(), txtTinhTrang.getText().trim(),
                chkTemMac.isSelected(), chkDayDuHop.isSelected(), chkPhuKien.isSelected());
            thongBao("Đã tạo phiếu " + maDaLuu + ". " + (vuotHanMuc ? "Phiếu đang chờ Quản lý duyệt." : "Đã xử lý hoàn tiền."));
            lamMoiToanBo();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void lamMoiToanBo() {
        txtTimKiem.setText("");
        txtMaPhieu.setText("");
        txtLyDo.setText("");
        txtTinhTrang.setText("");
        chkTemMac.setSelected(true);
        chkDayDuHop.setSelected(true);
        chkPhuKien.setSelected(true);
        xoaKetQuaTraCuu();
    }
}

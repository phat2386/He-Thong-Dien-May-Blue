package ui.banhang;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.*;
import java.text.Normalizer;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import entity.*;
import service.*;
import ui.dungchung.*;
import java.util.List;
public class ManHinhLapHoaDon extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final HoaDonService dichVu = new HoaDonService();
    private final SanPhamService dichVuSanPham = new SanPhamService();
    private final JComboBox<String> khachHang = new JComboBox<String>();
    private final JTextField sanPhamDaChon = TaoGiaoDien.oNhap(300);
    private String maSanPhamDangChon;
    private final JSpinner soLuongNhap = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JComboBox<String> thanhToan = TaoGiaoDien.oChon(DinhDangHienThi.nhan("Tiền mặt", "Chuyển khoản"));
    private final JTextField maHoaDonNhap = TaoGiaoDien.oNhap(180);
    private final DefaultTableModel moHinh = new DefaultTableModel(new Object[] {
        "Mã SP", "Sản phẩm", "SL", "Đơn giá", "Khuyến mãi / ưu đãi", "Thành tiền"
    }, 0) {
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable(int dong, int cot) {
            return false;
        }
    };
    private final JTable bang = TaoGiaoDien.bang(moHinh);
    private final List<DongHoaDonTam> cacDongHoaDon = new ArrayList<DongHoaDonTam>();
    private final List<String> luaChonKhachHang = new ArrayList<String>();
    private boolean dangCapNhatOChonTimKiem = false;
    private final JLabel lblGiaDangChon = nhanGiaTri("0đ");
    private final JLabel lblTonKhoDangChon = nhanGiaTri("-");
    private final JLabel lblTenKhachHang = nhanGiaTri("Khách vãng lai");
    private final JLabel lblThongTinKhachHang = nhanGiaTri("Không tích điểm thành viên");
    private final JLabel lblNhanVien = nhanGiaTri("");
    private final JLabel lblTamTinh = nhanGiaTri("0đ");
    private final JLabel lblTienGiam = nhanGiaTri("0đ");
    private final JLabel lblTongTien = new JLabel("0đ");
    private final JLabel goiYThanhToan = new JLabel();
    private final JButton btnThanhToan = TaoGiaoDien.nutChinh("Xác nhận thanh toán");
    private final JButton btnXoaDong = TaoGiaoDien.nutPhu("Xóa dòng");
    private boolean duocPhepBanHang = false;
    public ManHinhLapHoaDon() {
        super("Lập hóa đơn bán hàng", "Quét sản phẩm, kiểm tra khách hàng, áp dụng khuyến mãi, ưu đãi thành viên và xác nhận thanh toán.");
        JButton nutLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        JButton nutHuyHoaDonTam = TaoGiaoDien.nutPhu("Hủy hóa đơn tạm");
        thaoTac.add(btnThanhToan);
        thaoTac.add(nutLamMoi);
        thaoTac.add(nutHuyHoaDonTam);
        JPanel khungGoc = new JPanel(new BorderLayout(0, 14));
        khungGoc.setOpaque(false);
        JPanel khungTren = new JPanel();
        khungTren.setOpaque(false);
        khungTren.setLayout(new BoxLayout(khungTren, BoxLayout.Y_AXIS));
        khungTren.add(taoDongThemSanPham());
        khungTren.add(Box.createVerticalStrut(12));
        khungTren.add(taoDongThongTin());
        khungGoc.add(khungTren, BorderLayout.NORTH);
        KhungBoGoc theChiTiet = new KhungBoGoc(12, Color.WHITE);
        theChiTiet.setMauVien(HangSoGiaoDien.BORDER);
        theChiTiet.setLayout(new BorderLayout(0, 8));
        theChiTiet.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel tieuDeChiTiet = new JLabel("Chi tiết hóa đơn");
        tieuDeChiTiet.setFont(HangSoGiaoDien.SUBHEADING);
        tieuDeChiTiet.setForeground(HangSoGiaoDien.TEXT);
        JPanel dauTheChiTiet = new JPanel(new BorderLayout(8, 0));
        dauTheChiTiet.setOpaque(false);
        dauTheChiTiet.add(tieuDeChiTiet, BorderLayout.WEST);
        theChiTiet.add(dauTheChiTiet, BorderLayout.NORTH);
        theChiTiet.add(TaoGiaoDien.bangPhanTrang(bang), BorderLayout.CENTER);
        theChiTiet.add(taoThaoTacDong(), BorderLayout.SOUTH);
        khungGoc.add(theChiTiet, BorderLayout.CENTER);
        JPanel phiaDuoi = new JPanel(new GridLayout(1, 2, 14, 0));
        phiaDuoi.setOpaque(false);
        phiaDuoi.add(taoTheThanhToan());
        phiaDuoi.add(taoTheTongKet());
        khungGoc.add(phiaDuoi, BorderLayout.SOUTH);
        noiDung.add(khungGoc, BorderLayout.CENTER);
        caiDatOChonTimKiem(khachHang, luaChonKhachHang, "Nhập mã hoặc tên khách hàng...");
        sanPhamDaChon.setEditable(false);
        sanPhamDaChon.setToolTipText("Sản phẩm đã chọn từ danh sách tìm kiếm");
        nutLamMoi.addActionListener(e -> lamMoiDuLieu());
        nutHuyHoaDonTam.addActionListener(e -> xoaHoaDon());
        btnThanhToan.addActionListener(e -> thanhToanHoaDon());
        btnXoaDong.addActionListener(e -> xoaDangChon());
        bang.getSelectionModel().addListSelectionListener(e -> capNhatTrangThaiThaoTacDong());
        khachHang.addActionListener(e -> {
            if (!dangCapNhatOChonTimKiem) khachHangThayDoi();
        });
        thanhToan.addActionListener(e -> capNhatTruongThanhToan());
        btnThanhToan.setEnabled(false);
        btnXoaDong.setEnabled(false);
        capNhatTruongThanhToan();
    }
    private JPanel taoDongThemSanPham() {
        KhungBoGoc p = new KhungBoGoc(10, Color.WHITE);
        p.setMauVien(HangSoGiaoDien.BORDER);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel dongSanPham = new JPanel(new BoCucXuongDong(FlowLayout.LEFT, 8, 4));
        dongSanPham.setOpaque(false);
        khachHang.setPreferredSize(new Dimension(205, 36));
        khachHang.setMinimumSize(new Dimension(120, 36));
        sanPhamDaChon.setPreferredSize(new Dimension(270, 36));
        sanPhamDaChon.setMinimumSize(new Dimension(180, 36));
        soLuongNhap.setPreferredSize(new Dimension(58, 36));
        soLuongNhap.setMinimumSize(new Dimension(52, 36));
        JButton btnTimSanPham = TaoGiaoDien.nutPhu("Tìm sản phẩm");
        JButton add = TaoGiaoDien.nutChinh("+ Thêm sản phẩm");
        dongSanPham.add(new JLabel("Khách hàng"));
        dongSanPham.add(khachHang);
        dongSanPham.add(new JLabel("Sản phẩm"));
        dongSanPham.add(sanPhamDaChon);
        dongSanPham.add(btnTimSanPham);
        dongSanPham.add(new JLabel("SL"));
        dongSanPham.add(soLuongNhap);
        dongSanPham.add(add);
        btnTimSanPham.addActionListener(e -> hienHopThoaiTimSanPham());
        JPanel dongThongTin = new JPanel(new BoCucXuongDong(FlowLayout.LEFT, 12, 2));
        dongThongTin.setOpaque(false);
        JLabel priceTitle = new JLabel("Giá bán:");
        priceTitle.setFont(HangSoGiaoDien.MEDIUM);
        lblGiaDangChon.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 13));
        lblGiaDangChon.setForeground(HangSoGiaoDien.ORANGE);
        JLabel stockTitle = new JLabel("Tồn kho:");
        stockTitle.setFont(HangSoGiaoDien.MEDIUM);
        JLabel paymentTitle = new JLabel("Phương thức thanh toán:");
        paymentTitle.setFont(HangSoGiaoDien.MEDIUM);
        thanhToan.setPreferredSize(new Dimension(170, 36));
        thanhToan.setMinimumSize(new Dimension(140, 36));
        dongThongTin.add(priceTitle);
        dongThongTin.add(lblGiaDangChon);
        dongThongTin.add(Box.createHorizontalStrut(8));
        dongThongTin.add(stockTitle);
        dongThongTin.add(lblTonKhoDangChon);
        dongThongTin.add(Box.createHorizontalStrut(18));
        dongThongTin.add(paymentTitle);
        dongThongTin.add(thanhToan);
        p.add(dongSanPham);
        p.add(dongThongTin);
        add.addActionListener(e -> themSanPham());
        return p;
    }
    private JPanel taoDongThongTin() {
        JPanel dong = new JPanel(new GridLayout(1, 2, 14, 0));
        dong.setOpaque(false);
        KhungBoGoc theKhachHang = the();
        theKhachHang.setLayout(new GridLayout(0, 1, 0, 4));
        theKhachHang.add(tieuDePhan("Thông tin khách hàng"));
        theKhachHang.add(lblTenKhachHang);
        theKhachHang.add(lblThongTinKhachHang);
        KhungBoGoc theHoaDon = the();
        theHoaDon.setLayout(new GridLayout(0, 1, 0, 4));
        theHoaDon.add(tieuDePhan("Thông tin hóa đơn"));
        theHoaDon.add(nhanGiaTri("Mã hóa đơn: nhập tại phần thanh toán"));
        theHoaDon.add(lblNhanVien);
        dong.add(theKhachHang);
        dong.add(theHoaDon);
        return dong;
    }
    private JPanel taoThaoTacDong() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JButton minus = TaoGiaoDien.nutPhu("− SL");
        JButton plus = TaoGiaoDien.nutPhu("+ SL");
        minus.addActionListener(e -> dieuChinhDangChon( - 1));
        plus.addActionListener(e -> dieuChinhDangChon(1));
        p.add(plus);
        p.add(minus);
        p.add(btnXoaDong);
        return p;
    }
    private JPanel taoTheThanhToan() {
        KhungBoGoc p = the();
        p.setLayout(new BorderLayout(0, 8));
        p.add(tieuDePhan("Thông tin thanh toán"), BorderLayout.NORTH);
        JPanel trungTam = new JPanel(new GridBagLayout());
        trungTam.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 4, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0;
        trungTam.add(new JLabel("Mã hóa đơn"), c);
        c.gridx = 1;
        c.weightx = 1;
        trungTam.add(maHoaDonNhap, c);
        goiYThanhToan.setFont(HangSoGiaoDien.SMALL);
        goiYThanhToan.setForeground(HangSoGiaoDien.MUTED);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 2;
        trungTam.add(goiYThanhToan, c);
        p.add(trungTam, BorderLayout.CENTER);
        return p;
    }
    private JPanel taoTheTongKet() {
        KhungBoGoc p = the();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 2, 5, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        p.add(tieuDePhan("Tóm tắt thanh toán"), c);
        c.gridwidth = 1;
        themDongTongKet(p, c, 1, "Tạm tính", lblTamTinh);
        themDongTongKet(p, c, 2, "Giảm KM / ưu đãi", lblTienGiam);
        JLabel tieuDeTong = new JLabel("Tổng thanh toán");
        tieuDeTong.setFont(HangSoGiaoDien.MEDIUM);
        tieuDeTong.setForeground(HangSoGiaoDien.TEXT);
        lblTongTien.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 22));
        lblTongTien.setForeground(HangSoGiaoDien.ORANGE);
        themDongTongKet(p, c, 3, tieuDeTong, lblTongTien);
        JLabel point = new JLabel("Điểm thưởng và thưởng doanh số chỉ được ghi nhận khi thanh toán thành công.");
        point.setFont(HangSoGiaoDien.SMALL);
        point.setForeground(HangSoGiaoDien.MUTED);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        p.add(point, c);
        return p;
    }
    private void themDongTongKet(JPanel p, GridBagConstraints c, int y, String nhan, JLabel giaTri) {
        JLabel l = new JLabel(nhan);
        l.setFont(HangSoGiaoDien.NORMAL);
        l.setForeground(HangSoGiaoDien.MUTED);
        themDongTongKet(p, c, y, l, giaTri);
    }
    private void themDongTongKet(JPanel p, GridBagConstraints c, int y, JLabel nhan, JLabel giaTri) {
        c.gridwidth = 1;
        c.gridy = y;
        c.gridx = 0;
        c.weightx = 1;
        p.add(nhan, c);
        c.gridx = 1;
        c.weightx = 0;
        p.add(giaTri, c);
    }
    private KhungBoGoc the() {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setMauVien(HangSoGiaoDien.BORDER);
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        return p;
    }
    private JLabel tieuDePhan(String vanBan) {
        JLabel l = new JLabel(vanBan);
        l.setFont(HangSoGiaoDien.SUBHEADING);
        l.setForeground(HangSoGiaoDien.TEXT);
        return l;
    }
    private static JLabel nhanGiaTri(String vanBan) {
        JLabel l = new JLabel(vanBan);
        l.setFont(HangSoGiaoDien.NORMAL);
        l.setForeground(HangSoGiaoDien.TEXT);
        return l;
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            String khachHangCu = vanBanOChon(khachHang);
            String maKhachCu = tachMa(khachHangCu);
            luaChonKhachHang.clear();
            luaChonKhachHang.add("Khách vãng lai");
            for (KhachHang r : dichVu.layKhachHangHoatDong()) luaChonKhachHang.add(r.getMaKhachHang() + " - " + r.getHoTen() + " - " + DinhDangHienThi.anToan(r.getSoDienThoai()));
            datLuaChonOChon(khachHang, luaChonKhachHang);
            if (maKhachCu != null) {
                if (!chonBatDauBang(khachHang, maKhachCu)) khachHang.setSelectedItem("Khách vãng lai");
            } else if (khachHangCu != null && khachHangCu.startsWith("Khách vãng lai")) {
                khachHang.setSelectedItem("Khách vãng lai");
            } else {
                khachHang.setSelectedItem("Khách vãng lai");
            }
            if (maHoaDonNhap.getText().trim().isEmpty()) {
                try {
                    TaoGiaoDien.datGoiYMa(maHoaDonNhap, dichVu.goiYMaHoaDon());
                } catch (Exception ignored) {
                }
            }
            maSanPhamDangChon = null;
            sanPhamDaChon.setText("");
            capNhatThongTinSanPhamDangChon();
            String tenNhanVien = TaiKhoanService.getTaiKhoanHienTai() == null ? "" : TaiKhoanService.getTaiKhoanHienTai() .getNhanVien().getHoTen();
            lblNhanVien.setText("Nhân viên: " + tenNhanVien);
            capNhatThongTinKhachHang();
            capNhatTongTien();
            try {
                dichVu.damBaoDuDieuKienBanHang();
                duocPhepBanHang = true;
                capNhatTrangThaiNutThanhToan();
                capNhatTruongThanhToan();
            } catch (Exception ex) {
                duocPhepBanHang = false;
                capNhatTrangThaiNutThanhToan();
            }
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private boolean chonBatDauBang(JComboBox<String> c, String tienTo) {
        for (int i = 0; i < c.getItemCount(); i++) {
            if (String.valueOf(c.getItemAt(i)).startsWith(tienTo + " -")) {
                c.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }
    private void caiDatOChonTimKiem(final JComboBox<String> oChon, final List<String> source, String tooltip) {
        oChon.setEditable(true);
        oChon.setMaximumRowCount(10);
        Component component = oChon.getEditor().getEditorComponent();
        if (!(component instanceof JTextField)) return;
        final JTextField editor = (JTextField) component;
        editor.setToolTipText(tooltip);
        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> editor.selectAll());
            }
        });
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int maPhim = e.getKeyCode();
                if (maPhim == KeyEvent.VK_UP || maPhim == KeyEvent.VK_DOWN || maPhim == KeyEvent.VK_ENTER || maPhim == KeyEvent.VK_ESCAPE || maPhim == KeyEvent.VK_TAB) return;
                locOChon(oChon, source, editor.getText());
            }
        });
    }
    private void locOChon(JComboBox<String> oChon, List<String> source, String typedText) {
        if (dangCapNhatOChonTimKiem) return;
        String vanBan = typedText == null ? "" : typedText;
        String truyVan = chuanHoaTimKiem(vanBan);
        DefaultComboBoxModel<String> daLoc = new DefaultComboBoxModel<String>();
        for (String option : source) {
            if (truyVan.isEmpty() || chuanHoaTimKiem(option).contains(truyVan)) daLoc.addElement(option);
        }
        dangCapNhatOChonTimKiem = true;
        try {
            oChon.setModel(daLoc);
            oChon.setSelectedIndex( - 1);
            Component component = oChon.getEditor().getEditorComponent();
            if (component instanceof JTextField) {
                JTextField editor = (JTextField) component;
                editor.setText(vanBan);
                editor.setCaretPosition(editor.getText().length());
            }
        } finally {
            dangCapNhatOChonTimKiem = false;
        }
        if (oChon == khachHang) {
            if (vanBan.trim().isEmpty()) {
                lblTenKhachHang.setText("Khách vãng lai");
                lblThongTinKhachHang.setText("Không tích điểm thành viên");
            } else {
                lblTenKhachHang.setText("Chưa chọn khách hàng");
                lblThongTinKhachHang.setText("Chọn một khách hàng trong danh sách gợi ý");
            }
            try {
                for (DongHoaDonTam dongHoaDon : cacDongHoaDon) tinhLaiDong(dongHoaDon, taiSanPhamCoTheBan(dongHoaDon.maSanPham));
                taoLaiBang();
            } catch (Exception ex) {
                hienLoi(ex);
            }
        }
        if (oChon.isShowing() && !vanBan.trim().isEmpty() && daLoc.getSize() > 0) {
            oChon.setPopupVisible(true);
        } else if (daLoc.getSize() == 0) {
            oChon.setPopupVisible(false);
        }
    }
    private void datLuaChonOChon(JComboBox<String> oChon, List<String> source) {
        dangCapNhatOChonTimKiem = true;
        try {
            oChon.setModel(new DefaultComboBoxModel<String>(source.toArray(new String[0])));
        } finally {
            dangCapNhatOChonTimKiem = false;
        }
    }
    private void xoaLuaChonOChon(JComboBox<String> oChon) {
        dangCapNhatOChonTimKiem = true;
        try {
            oChon.setSelectedIndex( - 1);
            oChon.getEditor().setItem("");
        } finally {
            dangCapNhatOChonTimKiem = false;
        }
    }
    private String vanBanOChon(JComboBox<String> oChon) {
        if (oChon.isEditable() && oChon.getEditor() != null) {
            Object muc = oChon.getEditor().getItem();
            return muc == null ? "" : String.valueOf(muc).trim();
        }
        Object muc = oChon.getSelectedItem();
        return muc == null ? "" : String.valueOf(muc).trim();
    }
    private String tachMa(String giaTri) {
        if (giaTri == null) return null;
        int split = giaTri.indexOf(" - ");
        if (split <= 0) return null;
        return giaTri.substring(0, split).trim();
    }
    private String chuanHoaTimKiem(String giaTri) {
        String normalized = Normalizer.normalize(giaTri == null ? "" : giaTri, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
    private void khachHangThayDoi() {
        capNhatThongTinKhachHang();
        try {
            for (DongHoaDonTam dongHoaDon : cacDongHoaDon) tinhLaiDong(dongHoaDon, taiSanPhamCoTheBan(dongHoaDon.maSanPham));
            taoLaiBang();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void capNhatThongTinKhachHang() {
        String s = vanBanOChon(khachHang);
        if (s.isEmpty() || s.startsWith("Khách vãng lai")) {
            lblTenKhachHang.setText("Khách vãng lai");
            lblThongTinKhachHang.setText("Không tích điểm thành viên");
            return;
        }
        if (!s.contains(" - ")) {
            lblTenKhachHang.setText("Chưa chọn khách hàng");
            lblThongTinKhachHang.setText("Chọn một khách hàng trong danh sách gợi ý");
            return;
        }
        try {
            String maKhachHang = tachMa(s);
            KhachHang r = dichVu.layThongTinKhachHang(maKhachHang);
            if (r == null) return;
            lblTenKhachHang.setText(r.getHoTen());
            lblThongTinKhachHang.setText(DinhDangHienThi.anToan(r.getSoDienThoai()) + "  •  Hạng: " + dichVu.layHangThanhVien(maKhachHang) + "  •  Điểm: " + r.getDiemHienTai() + "  •  Ưu đãi: " + dichVu.layTyLeGiamThanhVien(maKhachHang) + "%" +
                "  •  Hệ số điểm: " + dichVu.layHeSoDiemThanhVien(maKhachHang));
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void hienHopThoaiTimSanPham() {
        try {
            JTextField txtTim = TaoGiaoDien.oNhap(300);
            TaoGiaoDien.datChuGoiY(txtTim, "Mã SP hoặc tên sản phẩm");
            JComboBox<String> cmbDanhMuc = TaoGiaoDien.oChon("Tất cả danh mục");
            Map<String, Integer> maDanhMucTheoTen = new java.util.LinkedHashMap<String, Integer>();
            for (Map<String, Object> r : dichVuSanPham.layDanhMucHoatDong()) {
                String ten = String.valueOf(r.get("category_name"));
                cmbDanhMuc.addItem(ten);
                maDanhMucTheoTen.put(ten, ((Number) r.get("category_id")).intValue());
            }
            DefaultTableModel model = TaoGiaoDien.moHinhChiDoc(new Object[] {
                "Mã SP", "Tên sản phẩm", "Danh mục", "Giá bán", "Tồn kho"
            });
            JTable table = TaoGiaoDien.bang(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getColumnModel().getColumn(0).setPreferredWidth(85);
            table.getColumnModel().getColumn(1).setPreferredWidth(260);
            table.getColumnModel().getColumn(2).setPreferredWidth(130);
            table.getColumnModel().getColumn(3).setPreferredWidth(120);
            table.getColumnModel().getColumn(4).setPreferredWidth(80);
            Runnable taiKetQua = () -> {
                try {
                    String tenDanhMuc = String.valueOf(cmbDanhMuc.getSelectedItem());
                    Integer maDanhMuc = "Tất cả danh mục".equals(tenDanhMuc) ? null : maDanhMucTheoTen .get(tenDanhMuc);
                    List<SanPham> ketQua = dichVuSanPham.timSanPhamDangBan(txtTim.getText().trim(), maDanhMuc);
                    model.setRowCount(0);
                    for (SanPham r : ketQua) {
                        String tenDanhMucHienThi = maDanhMucTheoTen.entrySet().stream().filter(e -> e.getValue().intValue() == r.getMaDanhMuc()).map(Map.Entry::getKey).findFirst().orElse("Khác");
                        model.addRow(new Object[] {
                            r.getMaSanPham(), r.getTenSanPham(), tenDanhMucHienThi, DinhDangHienThi.tien(r.getGiaBan()), r.getSoLuongTonKho()
                        });
                    }
                    if (model.getRowCount() > 0) TaoGiaoDien.chonDongDauTienSauKhiCapNhat(table);
                } catch (Exception ex) {
                    hienLoi(ex);
                }
            };
            txtTim.addActionListener(e -> taiKetQua.run());
            GoiYTimKiem.gan(txtTim, q -> {
                List<GoiYTimKiem.Muc> goiY = new ArrayList<>();
                try {
                    String tenDanhMuc = String.valueOf(cmbDanhMuc.getSelectedItem());
                    Integer maDanhMuc = "Tất cả danh mục".equals(tenDanhMuc) ? null : maDanhMucTheoTen.get(tenDanhMuc);
                    for (SanPham r : dichVuSanPham.timSanPhamDangBan(q, maDanhMuc)) {
                        goiY.add(new GoiYTimKiem.Muc(r.getMaSanPham(), r.getMaSanPham() + " - " + r.getTenSanPham() + " - " + DinhDangHienThi.tien(r.getGiaBan()) + " - Tồn " + r.getSoLuongTonKho()));
                        if (goiY.size() >= 12) break;
                    }
                } catch (Exception ex) { }
                return goiY;
            }, m -> taiKetQua.run());
            cmbDanhMuc.addActionListener(e -> taiKetQua.run());
            taiKetQua.run();
            JPanel thanhTim = TaoGiaoDien.thanhCongCu(new JLabel("Tìm kiếm"), txtTim, new JLabel("Danh mục"), cmbDanhMuc);
            JPanel noiDungTim = new JPanel(new BorderLayout(0, 10));
            noiDungTim.setPreferredSize(new Dimension(760, 410));
            noiDungTim.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            noiDungTim.add(thanhTim, BorderLayout.NORTH);
            noiDungTim.add(TaoGiaoDien.bangPhanTrang(table), BorderLayout.CENTER);
            if (!TienIchHopThoai.xacNhanBieuMau(this, noiDungTim, "Tìm sản phẩm", "Chọn sản phẩm")) return;
            int vr = table.getSelectedRow();
            if (vr < 0) {
                thongBao("Hãy chọn một sản phẩm trong danh sách.");
                return;
            }
            int row = table.convertRowIndexToModel(vr);
            maSanPhamDangChon = String.valueOf(model.getValueAt(row, 0));
            sanPhamDaChon.setText(maSanPhamDangChon + " - " + String.valueOf(model.getValueAt(row, 1)));
            capNhatThongTinSanPhamDangChon();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void capNhatThongTinSanPhamDangChon() {
        String id = maSanPhamDangChon;
        if (id == null || id.trim().isEmpty()) {
            lblGiaDangChon.setText("0đ");
            lblTonKhoDangChon.setText("-");
            return;
        }
        try {
            SanPham p = taiSanPhamCoTheBan(id);
            lblGiaDangChon.setText(DinhDangHienThi.tien(p.getGiaBan()));
            lblTonKhoDangChon.setText(String.valueOf(p.getSoLuongTonKho()));
        } catch (Exception ex) {
            lblGiaDangChon.setText("0đ");
            lblTonKhoDangChon.setText("-");
        }
    }
    private void themSanPham() {
        String id = maSanPhamDangChon;
        if (id == null || id.trim().isEmpty()) {
            thongBao("Hãy tìm và chọn sản phẩm trước khi thêm vào hóa đơn.");
            return;
        }
        try {
            SanPham p = taiSanPhamCoTheBan(id);
            int q = ((Number) soLuongNhap.getValue()).intValue();
            int tonKho = p.getSoLuongTonKho();
            if (q > tonKho) throw new IllegalArgumentException("Số lượng vượt tồn kho hiện tại (" + tonKho + ").");
            for (DongHoaDonTam l : cacDongHoaDon) if (l.maSanPham.equals(id)) {
                if (l.soLuongNhap + q > tonKho) throw new IllegalArgumentException("Tổng số lượng vượt tồn kho hiện tại (" + tonKho + ").");
                l.soLuongNhap += q;
                tinhLaiDong(l, p);
                taoLaiBang();
                return;
            }
            DongHoaDonTam dongHoaDon = new DongHoaDonTam();
            dongHoaDon.maSanPham = id;
            dongHoaDon.tenSanPham = p.getTenSanPham();
            dongHoaDon.soLuongNhap = q;
            tinhLaiDong(dongHoaDon, p);
            cacDongHoaDon.add(dongHoaDon);
            taoLaiBang();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private SanPham taiSanPhamCoTheBan(String id) throws Exception {
        SanPham p = dichVu.laySanPhamCoTheBan(id);
        if (p == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        if (!"Hoạt động".equals(p.getTrangThai()) || p.getSoLuongTonKho() <= 0) throw new IllegalArgumentException("Sản phẩm đã hết hàng hoặc ngừng kinh doanh.");
        return p;
    }
    private void dieuChinhDangChon(int chenhLech) {
        int dongDangChon = bang.getSelectedRow();
        if (dongDangChon < 0) {
            thongBao("Hãy chọn dòng sản phẩm cần chỉnh.");
            return;
        }
        int dong = bang.convertRowIndexToModel(dongDangChon);
        try {
            DongHoaDonTam dongHoaDon = cacDongHoaDon.get(dong);
            int soLuongMoi = dongHoaDon.soLuongNhap + chenhLech;
            if (soLuongMoi <= 0) {
                cacDongHoaDon.remove(dong);
                taoLaiBang();
                return;
            }
            SanPham p = taiSanPhamCoTheBan(dongHoaDon.maSanPham);
            int tonKho = p.getSoLuongTonKho();
            if (soLuongMoi > tonKho) throw new IllegalArgumentException("Số lượng vượt tồn kho hiện tại (" + tonKho + ").");
            dongHoaDon.soLuongNhap = soLuongMoi;
            tinhLaiDong(dongHoaDon, p);
            taoLaiBang();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void xoaDangChon() {
        int dongDangChon = bang.getSelectedRow();
        if (dongDangChon < 0) {
            thongBao("Hãy chọn dòng sản phẩm cần xóa.");
            return;
        }
        cacDongHoaDon.remove(bang.convertRowIndexToModel(dongDangChon));
        taoLaiBang();
    }
    private void tinhLaiDong(DongHoaDonTam dongHoaDon, SanPham dongSanPham) throws Exception {
        dongHoaDon.donGia = dongSanPham.getGiaBan();
        BigDecimal tienTruocGiam = dongHoaDon.donGia.multiply(BigDecimal.valueOf(dongHoaDon.soLuongNhap));
        dongHoaDon.tienGiam = giamGiaTotNhat(dongHoaDon.maSanPham, dongSanPham.getMaDanhMuc(), tienTruocGiam, dongHoaDon.soLuongNhap);
        dongHoaDon.tong = tienTruocGiam.subtract(dongHoaDon.tienGiam).max(BigDecimal.ZERO);
    }
    private BigDecimal giamGiaTotNhat(String maSanPham, int maDanhMuc, BigDecimal tienTruocGiam, int soLuongNhap) throws Exception {
        BigDecimal giamKhuyenMaiTotNhat = BigDecimal.ZERO;
        for (KhuyenMai r : dichVu.layKhuyenMaiDangHoatDong(maSanPham, maDanhMuc)) {
            String type = r.getLoaiGiam();
            BigDecimal v = r.getGiaTriGiam();
            BigDecimal d = "PERCENT".equals(type) ? tienTruocGiam.multiply(v).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP) : v.multiply(BigDecimal.valueOf(soLuongNhap));
            if (d.compareTo(giamKhuyenMaiTotNhat) > 0) giamKhuyenMaiTotNhat = d;
        }
        BigDecimal tyLeGiamThanhVien = tyLeGiamThanhVienHienTai();
        BigDecimal giamThanhVien = tienTruocGiam.multiply(tyLeGiamThanhVien).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return giamKhuyenMaiTotNhat.max(giamThanhVien).min(tienTruocGiam);
    }
    private BigDecimal tyLeGiamThanhVienHienTai() throws Exception {
        String s = vanBanOChon(khachHang);
        if (s.isEmpty() || s.startsWith("Khách vãng lai") || !s.contains(" - ")) return BigDecimal.ZERO;
        String maKhachHang = tachMa(s);
        Object tyLe = dichVu.layTyLeGiamThanhVien(maKhachHang);
        return tyLe instanceof BigDecimal ? (BigDecimal) tyLe : new BigDecimal(String.valueOf(tyLe == null ? 0 : tyLe));
    }
    private void taoLaiBang() {
        moHinh.setRowCount(0);
        for (DongHoaDonTam l : cacDongHoaDon) moHinh.addRow(new Object[] {
            l.maSanPham, l.tenSanPham, l.soLuongNhap, DinhDangHienThi.tien(l.donGia), l.tienGiam.compareTo(BigDecimal.ZERO) >
                0 ? "-" + DinhDangHienThi.tien(l.tienGiam) : "Không có", DinhDangHienThi.tien(l.tong)
        });
        capNhatTongTien();
        capNhatTrangThaiThaoTacDong();
    }
    private BigDecimal tongHienTai() {
        BigDecimal tong = BigDecimal.ZERO;
        for (DongHoaDonTam l : cacDongHoaDon) tong = tong.add(l.tong);
        return tong;
    }
    private void capNhatTongTien() {
        BigDecimal tamTinh = tamTinhHienTai();
        BigDecimal tienGiam = giamGiaHienTai();
        BigDecimal tong = tongHienTai();
        lblTamTinh.setText(DinhDangHienThi.tien(tamTinh));
        lblTienGiam.setText(tienGiam.signum() == 0 ? "0đ" : "-" + DinhDangHienThi.tien(tienGiam));
        lblTienGiam.setForeground(new Color(22, 163, 74));
        lblTongTien.setText(DinhDangHienThi.tien(tong));
        capNhatTrangThaiNutThanhToan();
        capNhatTrangThaiThaoTacDong();
    }
    private void capNhatTrangThaiNutThanhToan() {
        boolean coMatHang = !cacDongHoaDon.isEmpty();
        btnThanhToan.setEnabled(duocPhepBanHang && coMatHang);
        if (!duocPhepBanHang) {
            btnThanhToan.setToolTipText("Thanh toán hiện đang bị khóa.");
        } else if (!coMatHang) {
            btnThanhToan.setToolTipText("Thêm ít nhất một sản phẩm vào chi tiết hóa đơn để thanh toán.");
        } else {
            btnThanhToan.setToolTipText(null);
        }
    }
    private void capNhatTrangThaiThaoTacDong() {
        btnXoaDong.setEnabled(!cacDongHoaDon.isEmpty() && bang.getSelectedRow() >= 0);
    }
    private void capNhatTruongThanhToan() {
        String phuongThuc = DinhDangHienThi.ma(thanhToan.getSelectedItem());
        if ("Tiền mặt".equals(phuongThuc)) {
            goiYThanhToan.setText("Thanh toán tiền mặt: kiểm tra hóa đơn rồi nhấn Xác nhận thanh toán.");
        } else if ("Chuyển khoản".equals(phuongThuc)) {
            goiYThanhToan.setText("Chuyển khoản: kiểm tra giao dịch rồi nhấn Xác nhận thanh toán.");
        } else {
            goiYThanhToan.setText("");
        }
        revalidate();
        repaint();
    }
    private void xoaHoaDon() {
        if (!cacDongHoaDon.isEmpty() && !xacNhan("Hủy toàn bộ hóa đơn tạm hiện tại?")) return;
        datLaiHoaDon();
    }
    private void datLaiHoaDon() {
        cacDongHoaDon.clear();
        moHinh.setRowCount(0);
        try {
            TaoGiaoDien.datGoiYMa(maHoaDonNhap, dichVu.goiYMaHoaDon());
        } catch (Exception ex) {
            maHoaDonNhap.setText("");
        }
        soLuongNhap.setValue(1);
        capNhatTongTien();
    }
    private void thanhToanHoaDon() {
        if (cacDongHoaDon.isEmpty() || !duocPhepBanHang) return;
        try {
            dichVu.damBaoDuDieuKienBanHang();
            hienXemTruocHoaDon();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void hienXemTruocHoaDon() {
        Window chuSoHuu = SwingUtilities.getWindowAncestor(this);
        final JDialog hopThoai = new JDialog(chuSoHuu, "Hóa đơn điện tử", Dialog.ModalityType.APPLICATION_MODAL);
        hopThoai.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        hopThoai.setResizable(true);

        JPanel khungGoc = new JPanel(new BorderLayout(0, 12));
        khungGoc.setBackground(HangSoGiaoDien.BACKGROUND);
        khungGoc.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        KhungBoGoc hoaDon = new KhungBoGoc(14, Color.WHITE);
        hoaDon.setMauVien(HangSoGiaoDien.BORDER);
        hoaDon.setLayout(new BorderLayout(0, 12));
        hoaDon.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        KhungBoGoc logo = new KhungBoGoc(10, HangSoGiaoDien.ORANGE);
        logo.setVeVien(false);
        logo.setPreferredSize(new Dimension(34, 34));
        logo.setMinimumSize(new Dimension(34, 34));
        logo.setMaximumSize(new Dimension(34, 34));
        logo.setLayout(new GridBagLayout());
        JLabel b = new JLabel("B");
        b.setForeground(Color.WHITE);
        b.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 15));
        logo.add(b);
        JLabel store = new JLabel("Điện Máy Blue");
        store.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 15));
        store.setForeground(HangSoGiaoDien.TEXT);
        store.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        brand.add(logo);
        brand.add(store);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel tieuDe = new JLabel("HÓA ĐƠN BÁN HÀNG ĐIỆN TỬ");
        tieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 22));
        tieuDe.setForeground(HangSoGiaoDien.TEXT);
        tieuDe.setAlignmentX(Component.RIGHT_ALIGNMENT);
        header.add(brand, BorderLayout.WEST);
        header.add(titleBox, BorderLayout.EAST);
        hoaDon.add(header, BorderLayout.NORTH);

        JPanel noiDungChinh = new JPanel(new BorderLayout(0, 12));
        noiDungChinh.setOpaque(false);

        JPanel meta = new JPanel(new GridLayout(0, 2, 16, 8));
        meta.setOpaque(false);
        meta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0, HangSoGiaoDien.BORDER),
            BorderFactory.createEmptyBorder(10, 0, 10, 0)));
        meta.add(thongTinHoaDon("Mã hóa đơn", maHoaDonNhap.getText().trim().isEmpty() ? "Chưa nhập" : maHoaDonNhap.getText().trim()));
        meta.add(thongTinHoaDon("Ngày lập", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        meta.add(thongTinHoaDon("Người mua", lblTenKhachHang.getText()));
        meta.add(thongTinHoaDon("Nhân viên bán hàng", tenNhanVienHienTai()));
        meta.add(thongTinHoaDon("Thông tin khách hàng", lblThongTinKhachHang.getText()));
        meta.add(thongTinHoaDon("Phương thức thanh toán", String.valueOf(thanhToan.getSelectedItem())));
        noiDungChinh.add(meta, BorderLayout.NORTH);

        DefaultTableModel moHinhXemTruoc = new DefaultTableModel(new Object[] {
            "STT", "Mã SP", "Tên hàng hóa", "SL", "Đơn giá", "Giảm", "Thành tiền"
        }, 0) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int dong, int cot) {
                return false;
            }
        };
        int stt = 1;
        for (DongHoaDonTam l : cacDongHoaDon) {
            moHinhXemTruoc.addRow(new Object[] {
                stt++, l.maSanPham, l.tenSanPham, l.soLuongNhap,
                DinhDangHienThi.tien(l.donGia),
                l.tienGiam.signum() == 0 ? "0đ" : "-" + DinhDangHienThi.tien(l.tienGiam),
                DinhDangHienThi.tien(l.tong)
            });
        }
        JTable bangXemTruoc = TaoGiaoDien.bang(moHinhXemTruoc);
        bangXemTruoc.setAutoCreateRowSorter(false);
        bangXemTruoc.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        bangXemTruoc.setRowHeight(32);
        bangXemTruoc.getColumnModel().getColumn(0).setPreferredWidth(38);
        bangXemTruoc.getColumnModel().getColumn(1).setPreferredWidth(68);
        bangXemTruoc.getColumnModel().getColumn(2).setPreferredWidth(220);
        bangXemTruoc.getColumnModel().getColumn(3).setPreferredWidth(42);
        bangXemTruoc.getColumnModel().getColumn(4).setPreferredWidth(92);
        bangXemTruoc.getColumnModel().getColumn(5).setPreferredWidth(78);
        bangXemTruoc.getColumnModel().getColumn(6).setPreferredWidth(100);

        JScrollPane cuonBang = new JScrollPane(bangXemTruoc,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cuonBang.setBorder(BorderFactory.createEmptyBorder());
        cuonBang.getViewport().setBackground(Color.WHITE);
        cuonBang.getVerticalScrollBar().setUnitIncrement(24);
        int soDongHienThi = Math.max(1, Math.min(6, moHinhXemTruoc.getRowCount()));
        int chieuCaoBang = bangXemTruoc.getTableHeader().getPreferredSize().height
            + soDongHienThi * bangXemTruoc.getRowHeight() + 4;
        cuonBang.setPreferredSize(new Dimension(680, chieuCaoBang));
        cuonBang.setMinimumSize(new Dimension(680,
            bangXemTruoc.getTableHeader().getPreferredSize().height + bangXemTruoc.getRowHeight() + 4));
        noiDungChinh.add(cuonBang, BorderLayout.CENTER);

        JPanel phanDuoiHoaDon = new JPanel();
        phanDuoiHoaDon.setOpaque(false);
        phanDuoiHoaDon.setLayout(new BoxLayout(phanDuoiHoaDon, BoxLayout.Y_AXIS));

        JPanel cacTong = new JPanel(new GridBagLayout());
        cacTong.setOpaque(false);
        cacTong.setAlignmentX(Component.LEFT_ALIGNMENT);
        cacTong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        GridBagConstraints tc = new GridBagConstraints();
        tc.insets = new Insets(3, 8, 3, 0);
        tc.anchor = GridBagConstraints.EAST;
        tc.gridx = 0;
        tc.gridy = 0;
        tc.weightx = 1;
        JLabel subTitle = new JLabel("Tạm tính:");
        subTitle.setForeground(HangSoGiaoDien.MUTED);
        subTitle.setFont(HangSoGiaoDien.NORMAL);
        cacTong.add(subTitle, tc);
        tc.gridx = 1;
        tc.weightx = 0;
        JLabel giaTriTamTinh = new JLabel(DinhDangHienThi.tien(tamTinhHienTai()));
        giaTriTamTinh.setFont(HangSoGiaoDien.MEDIUM);
        giaTriTamTinh.setForeground(HangSoGiaoDien.TEXT);
        cacTong.add(giaTriTamTinh, tc);
        tc.gridy = 1;
        tc.gridx = 0;
        tc.weightx = 1;
        JLabel disTitle = new JLabel("Khuyến mãi / ưu đãi:");
        disTitle.setForeground(HangSoGiaoDien.MUTED);
        disTitle.setFont(HangSoGiaoDien.NORMAL);
        cacTong.add(disTitle, tc);
        tc.gridx = 1;
        tc.weightx = 0;
        JLabel giaTriGiam = new JLabel(giamGiaHienTai().signum() == 0 ? "0đ" : "-" + DinhDangHienThi.tien(giamGiaHienTai()));
        giaTriGiam.setFont(HangSoGiaoDien.MEDIUM);
        giaTriGiam.setForeground(new Color(22, 163, 74));
        cacTong.add(giaTriGiam, tc);
        tc.gridy = 2;
        tc.gridx = 0;
        tc.weightx = 1;
        JLabel tieuDeTong = new JLabel("TỔNG THANH TOÁN:");
        tieuDeTong.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 14));
        tieuDeTong.setForeground(HangSoGiaoDien.TEXT);
        cacTong.add(tieuDeTong, tc);
        tc.gridx = 1;
        tc.weightx = 0;
        JLabel tongGiaTri = new JLabel(DinhDangHienThi.tien(tongHienTai()));
        tongGiaTri.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 20));
        tongGiaTri.setForeground(HangSoGiaoDien.ORANGE);
        cacTong.add(tongGiaTri, tc);
        phanDuoiHoaDon.add(cacTong);


        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, HangSoGiaoDien.BORDER));
        foot.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel thongBao = new JLabel("Vui lòng kiểm tra thông tin trước khi xác nhận thanh toán.");
        thongBao.setFont(HangSoGiaoDien.SMALL);
        thongBao.setForeground(HangSoGiaoDien.MUTED);
        thongBao.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        foot.add(thongBao, BorderLayout.WEST);
        phanDuoiHoaDon.add(Box.createVerticalStrut(8));
        phanDuoiHoaDon.add(foot);
        noiDungChinh.add(phanDuoiHoaDon, BorderLayout.SOUTH);

        hoaDon.add(noiDungChinh, BorderLayout.CENTER);
        khungGoc.add(hoaDon, BorderLayout.CENTER);

        JPanel cacNut = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cacNut.setOpaque(false);
        JButton cancel = TaoGiaoDien.nutPhu("Hủy");
        JButton xacNhan = TaoGiaoDien.nutChinh("Xác nhận");
        cacNut.add(cancel);
        cacNut.add(xacNhan);
        khungGoc.add(cacNut, BorderLayout.SOUTH);

        cancel.addActionListener(e -> hopThoai.dispose());
        xacNhan.addActionListener(e -> hoanTatThanhToan(hopThoai, hoaDon));
        hopThoai.getRootPane().setDefaultButton(xacNhan);
        hopThoai.setContentPane(khungGoc);
        hopThoai.pack();

        Rectangle vungManHinh = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension uuTien = hopThoai.getPreferredSize();
        int chieuRong = Math.min(Math.max(900, uuTien.width), Math.max(900, vungManHinh.width - 80));
        int chieuCaoToiDa = Math.max(620, vungManHinh.height - 60);
        int chieuCao = Math.min(chieuCaoToiDa, Math.max(650, uuTien.height));
        hopThoai.setSize(chieuRong, chieuCao);
        hopThoai.setMinimumSize(new Dimension(860, 620));
        hopThoai.setLocationRelativeTo(chuSoHuu);
        hopThoai.setVisible(true);
    }
    private JPanel thongTinHoaDon(String nhan, String giaTri) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(nhan);
        l.setFont(HangSoGiaoDien.SMALL);
        l.setForeground(HangSoGiaoDien.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        String display = giaTri == null || giaTri.trim().isEmpty() ? "-" : giaTri;
        JLabel v = new JLabel("<html><div style='width:260px'>" + thoatHtml(display) + "</div></html>");
        v.setFont(HangSoGiaoDien.NORMAL);
        v.setForeground(HangSoGiaoDien.TEXT);
        v.setAlignmentX(Component.LEFT_ALIGNMENT);
        v.setToolTipText(display);
        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        return p;
    }
    private String tenNhanVienHienTai() {
        try {
            if (TaiKhoanService.getTaiKhoanHienTai() != null && TaiKhoanService.getTaiKhoanHienTai().getNhanVien() != null) return TaiKhoanService.getTaiKhoanHienTai().getNhanVien().getHoTen();
        } catch (Exception ignore) {
        }
        return "-";
    }
    private BigDecimal tamTinhHienTai() {
        BigDecimal giaTri = BigDecimal.ZERO;
        for (DongHoaDonTam l : cacDongHoaDon) giaTri = giaTri.add(l.donGia.multiply(BigDecimal.valueOf(l.soLuongNhap)));
        return giaTri;
    }
    private BigDecimal giamGiaHienTai() {
        BigDecimal giaTri = BigDecimal.ZERO;
        for (DongHoaDonTam l : cacDongHoaDon) giaTri = giaTri.add(l.tienGiam);
        return giaTri;
    }
    private String thoatHtml(String giaTri) {
        return giaTri.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
    private void hoanTatThanhToan(JDialog hopThoaiXemTruoc, JPanel noiDungHoaDon) {
        try {
            dichVu.damBaoDuDieuKienBanHang();
            String phuongThuc = DinhDangHienThi.ma(thanhToan.getSelectedItem());
            String maHoaDon = maHoaDonNhap.getText().trim().toUpperCase(Locale.ROOT);
            if (!maHoaDon.matches("HD\\d{5}")) throw new IllegalArgumentException("Mã hóa đơn phải theo dạng HD00001.");
            List<HoaDonService.MatHangHoaDon> cacMatHang = new ArrayList<HoaDonService.MatHangHoaDon>();
            for (DongHoaDonTam l : cacDongHoaDon) cacMatHang.add(new HoaDonService.MatHangHoaDon(l.maSanPham, l.soLuongNhap));
            String cs = vanBanOChon(khachHang);
            String maKhachHang = (cs.isEmpty() || cs.startsWith("Khách vãng lai") || !cs.contains(" - ")) ? null : cs.substring(0, cs.indexOf(" - "));
            String id = dichVu.taoHoaDon(maHoaDon, maKhachHang, cacMatHang, phuongThuc, null, null);
            boolean xuatHoaDon = xacNhanXuatHoaDon(hopThoaiXemTruoc, id);
            if (xuatHoaDon) xuatHoaDon(noiDungHoaDon, id);
            hopThoaiXemTruoc.dispose();
            thongBao("Thanh toán thành công. Mã hóa đơn: " + id + "\nCó thể xem lại đầy đủ tại Quản lý hóa đơn > Xem chi tiết.");
            datLaiHoaDon();
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private boolean xacNhanXuatHoaDon(Component source, String maHoaDon) {
        Object[] luaChon = {
            "Không xuất", "Xuất PDF"
        };
        int ketQua = JOptionPane.showOptionDialog(source,
            "Thanh toán thành công hóa đơn " + maHoaDon + ".\nBạn có muốn xuất hóa đơn ra file PDF không?",
            "Xuất hóa đơn PDF", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, luaChon, luaChon[1]);
        return ketQua == 1;
    }
    private void xuatHoaDon(JPanel noiDungHoaDon, String maHoaDon) {
        JFileChooser chonFile = new JFileChooser();
        chonFile.setDialogTitle("Lưu hóa đơn PDF");
        chonFile.setSelectedFile(new File(maHoaDon + ".pdf"));
        chonFile.setFileFilter(new FileNameExtensionFilter("Tệp PDF (*.pdf)", "pdf"));
        if (chonFile.showSaveDialog(noiDungHoaDon) != JFileChooser.APPROVE_OPTION) return;
        File file = chonFile.getSelectedFile();
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            file = new File(file.getParentFile(), file.getName() + ".pdf");
        }
        if (file.exists()) {
            int ghiDe = JOptionPane.showConfirmDialog(noiDungHoaDon,
                "Tệp " + file.getName() + " đã tồn tại. Bạn có muốn ghi đè không?",
                "Xác nhận ghi đè", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ghiDe != JOptionPane.YES_OPTION) return;
        }
        try {
            BufferedImage anhHoaDon = chupHoaDon(noiDungHoaDon);
            new XuatHoaDonPdfService().xuatAnhRaPdf(anhHoaDon, file);
            JOptionPane.showMessageDialog(noiDungHoaDon,
                "Đã xuất hóa đơn PDF:\n" + file.getAbsolutePath(),
                "Xuất hóa đơn", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            hienLoi(new IllegalStateException(
                "Thanh toán đã được ghi nhận nhưng không thể xuất PDF: " + ex.getMessage(), ex));
        }
    }
    private BufferedImage chupHoaDon(JPanel noiDungHoaDon) {
        Dimension kichThuocCu = noiDungHoaDon.getSize();
        List<JScrollPane> cacBangCuon = new ArrayList<JScrollPane>();
        timBangCuon(noiDungHoaDon, cacBangCuon);
        List<Dimension> kichThuocUuTienCu = new ArrayList<Dimension>();
        List<Dimension> kichThuocToiThieuCu = new ArrayList<Dimension>();
        List<Integer> chinhSachCuonCu = new ArrayList<Integer>();
        for (JScrollPane cuon : cacBangCuon) {
            kichThuocUuTienCu.add(cuon.getPreferredSize());
            kichThuocToiThieuCu.add(cuon.getMinimumSize());
            chinhSachCuonCu.add(cuon.getVerticalScrollBarPolicy());
            Component view = cuon.getViewport().getView();
            if (view instanceof JTable) {
                JTable table = (JTable) view;
                int caoBang = table.getTableHeader().getPreferredSize().height
                    + Math.max(1, table.getRowCount()) * table.getRowHeight() + 4;
                int rongBang = Math.max(680, cuon.getPreferredSize().width);
                cuon.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
                cuon.setPreferredSize(new Dimension(rongBang, caoBang));
                cuon.setMinimumSize(new Dimension(rongBang, caoBang));
            }
        }
        Dimension kichThuocDayDu = noiDungHoaDon.getPreferredSize();
        int rong = Math.max(Math.max(1, kichThuocCu.width), Math.max(1, kichThuocDayDu.width));
        int cao = Math.max(Math.max(1, kichThuocDayDu.height), 1);
        noiDungHoaDon.setSize(rong, cao);
        sapXepLai(noiDungHoaDon);
        int heSo = 2;
        BufferedImage anh = new BufferedImage(rong * heSo, cao * heSo, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = anh.createGraphics();
        try {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, anh.getWidth(), anh.getHeight());
            g2.scale(heSo, heSo);
            noiDungHoaDon.printAll(g2);
        } finally {
            g2.dispose();
            for (int i = 0; i < cacBangCuon.size(); i++) {
                JScrollPane cuon = cacBangCuon.get(i);
                cuon.setPreferredSize(kichThuocUuTienCu.get(i));
                cuon.setMinimumSize(kichThuocToiThieuCu.get(i));
                cuon.setVerticalScrollBarPolicy(chinhSachCuonCu.get(i));
            }
            noiDungHoaDon.setSize(kichThuocCu);
            sapXepLai(noiDungHoaDon);
        }
        return anh;
    }
    private void timBangCuon(Container container, List<JScrollPane> ketQua) {
        for (Component component : container.getComponents()) {
            if (component instanceof JScrollPane) {
                JScrollPane cuon = (JScrollPane) component;
                if (cuon.getViewport().getView() instanceof JTable) ketQua.add(cuon);
            }
            if (component instanceof Container) timBangCuon((Container) component, ketQua);
        }
    }
    private void sapXepLai(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container) sapXepLai((Container) component);
        }
    }
    private static class DongHoaDonTam {
        String maSanPham, tenSanPham;
        int soLuongNhap;
        BigDecimal donGia = BigDecimal.ZERO, tienGiam = BigDecimal.ZERO, tong = BigDecimal.ZERO;
    }
}

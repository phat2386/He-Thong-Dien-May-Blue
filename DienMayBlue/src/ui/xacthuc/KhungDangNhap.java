package ui.xacthuc;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import service.TaiKhoanService;
import ui.chinh.KhungChinh;
import ui.dungchung.*;
public class KhungDangNhap extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JTextField txtTenDangNhap = new JTextField();
    private final JPasswordField txtMatKhau = new JPasswordField();
    private final NutBoGoc btnDangNhap = new NutBoGoc("Đăng nhập", HangSoGiaoDien.ORANGE, HangSoGiaoDien.ORANGE_HOVER, 12);
    private final JLabel lblLoi = new JLabel(" ");
    private final JPanel pnlLoi = new KhungBoGoc(10, new Color(254, 242, 242));
    private final JPanel pnlThongBaoPhien = new KhungBoGoc(12, new Color(255, 251, 235));
    private final JLabel lblTieuDePhien = new JLabel("Phiên làm việc đã hết hạn");
    private final JLabel lblThongBaoPhien = new JLabel("Bạn đã không hoạt động trong 15 phút. Vui lòng đăng nhập lại.");
    private final TaiKhoanService dichVuXacThuc = new TaiKhoanService();
    private boolean dangHienMatKhau = false;
    public KhungDangNhap() {
        setTitle("Điện Máy Blue - Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1180, 720);
        setResizable(false);
        setLocationRelativeTo(null);
        setBackground(Color.WHITE);
        setContentPane(taoKhungChinh());
        getRootPane().setDefaultButton(btnDangNhap);
        SwingUtilities.invokeLater(() -> txtTenDangNhap.requestFocusInWindow());
    }
    private JPanel taoKhungChinh() {
        JPanel pnlKhung = new JPanel(new GridBagLayout());
        pnlKhung.setBackground(Color.WHITE);
        GridBagConstraints trai = new GridBagConstraints();
        trai.gridx = 0;
        trai.gridy = 0;
        trai.weightx = 0.40;
        trai.weighty = 1;
        trai.fill = GridBagConstraints.BOTH;
        GridBagConstraints phai = new GridBagConstraints();
        phai.gridx = 1;
        phai.gridy = 0;
        phai.weightx = 0.60;
        phai.weighty = 1;
        phai.fill = GridBagConstraints.BOTH;
        pnlKhung.add(taoPhanGioiThieu(), trai);
        pnlKhung.add(taoPhanDangNhap(), phai);
        return pnlKhung;
    }
    private JPanel taoPhanGioiThieu() {
        JPanel pnlKhung = new JPanel(new BorderLayout());
        pnlKhung.setBackground(HangSoGiaoDien.NAVY);
        pnlKhung.setBorder(new EmptyBorder(34, 34, 34, 34));
        pnlKhung.add(taoLogo(), BorderLayout.NORTH);
        pnlKhung.add(taoNoiDungGioiThieu(), BorderLayout.CENTER);
        JLabel lblBanQuyen = new JLabel("© 2025 Điện Máy Blue. Bảo lưu mọi quyền.");
        lblBanQuyen.setForeground(new Color(107, 137, 168));
        lblBanQuyen.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 11));
        pnlKhung.add(lblBanQuyen, BorderLayout.SOUTH);
        return pnlKhung;
    }
    private JPanel taoLogo() {
        KhungBoGoc pnlOVuong = new KhungBoGoc(12, HangSoGiaoDien.ORANGE);
        pnlOVuong.setVeVien(false);
        pnlOVuong.setPreferredSize(new Dimension(36, 36));
        pnlOVuong.setMinimumSize(new Dimension(36, 36));
        pnlOVuong.setMaximumSize(new Dimension(36, 36));
        pnlOVuong.setLayout(new GridBagLayout());
        JLabel lblChuB = new JLabel("B");
        lblChuB.setForeground(Color.WHITE);
        lblChuB.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 16));
        pnlOVuong.add(lblChuB);
        JLabel lblTenCuaHang = new JLabel("Điện Máy Blue");
        lblTenCuaHang.setForeground(Color.WHITE);
        lblTenCuaHang.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 15));
        lblTenCuaHang.setBorder(new EmptyBorder(0, 12, 0, 0));
        JPanel pnlLogo = new JPanel();
        pnlLogo.setOpaque(false);
        pnlLogo.setLayout(new BoxLayout(pnlLogo, BoxLayout.X_AXIS));
        pnlLogo.add(pnlOVuong);
        pnlLogo.add(lblTenCuaHang);
        return pnlLogo;
    }
    private JPanel taoNoiDungGioiThieu() {
        JPanel pnlNoiDung = new JPanel();
        pnlNoiDung.setOpaque(false);
        pnlNoiDung.setLayout(new BoxLayout(pnlNoiDung, BoxLayout.Y_AXIS));
        pnlNoiDung.add(Box.createVerticalGlue());
        JLabel lblTieuDe = new JLabel("<html>Hệ thống quản lý<br>cửa hàng điện máy</html>");
        lblTieuDe.setForeground(Color.WHITE);
        lblTieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 31));
        lblTieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlNoiDung.add(lblTieuDe);
        pnlNoiDung.add(Box.createVerticalStrut(18));
        JLabel lblMoTa = new JLabel( "<html><div style='width:300px'>Quản lý toàn diện từ bán hàng, kho hàng, khách hàng " + "đến thống kê kinh doanh theo thời gian thực.</div></html>");
        lblMoTa.setForeground(new Color(159, 184, 210));
        lblMoTa.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 15));
        lblMoTa.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlNoiDung.add(lblMoTa);
        pnlNoiDung.add(Box.createVerticalStrut(46));
        pnlNoiDung.add(taoHangSoLieu());
        pnlNoiDung.add(Box.createVerticalGlue());
        return pnlNoiDung;
    }
    private JPanel taoHangSoLieu() {
        JPanel pnlHang = new JPanel(new GridLayout(1, 3, 12, 0));
        pnlHang.setOpaque(false);
        pnlHang.setBorder(new EmptyBorder(25, 0, 0, 0));
        pnlHang.add(taoSoLieu("12.500+", "Sản phẩm"));
        pnlHang.add(taoSoLieu("48.000+", "Khách hàng"));
        pnlHang.add(taoSoLieu("320+", "Nhân viên"));
        JPanel pnlKhung = new JPanel(new BorderLayout());
        pnlKhung.setOpaque(false);
        pnlKhung.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 25)));
        pnlKhung.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        pnlKhung.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlKhung.add(pnlHang, BorderLayout.CENTER);
        return pnlKhung;
    }
    private JPanel taoSoLieu(String so, String ten) {
        JLabel lblSo = new JLabel(so);
        lblSo.setForeground(HangSoGiaoDien.ORANGE);
        lblSo.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 24));
        JLabel lblTen = new JLabel(ten);
        lblTen.setForeground(new Color(112, 151, 189));
        lblTen.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 12));
        JPanel pnlKhung = new JPanel();
        pnlKhung.setOpaque(false);
        pnlKhung.setLayout(new BoxLayout(pnlKhung, BoxLayout.Y_AXIS));
        pnlKhung.add(lblSo);
        pnlKhung.add(Box.createVerticalStrut(3));
        pnlKhung.add(lblTen);
        return pnlKhung;
    }
    private JPanel taoPhanDangNhap() {
        JPanel pnlBieuMau = new JPanel();
        pnlBieuMau.setOpaque(false);
        pnlBieuMau.setPreferredSize(new Dimension(360, 610));
        pnlBieuMau.setMaximumSize(new Dimension(360, Integer.MAX_VALUE));
        pnlBieuMau.setLayout(new BoxLayout(pnlBieuMau, BoxLayout.Y_AXIS));
        pnlBieuMau.add(Box.createVerticalGlue());
        pnlBieuMau.add(taoKhungHetPhien());
        pnlBieuMau.add(Box.createVerticalStrut(18));
        JLabel lblTieuDe = new JLabel("Đăng nhập");
        lblTieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 26));
        lblTieuDe.setForeground(HangSoGiaoDien.TEXT);
        lblTieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlBieuMau.add(lblTieuDe);
        pnlBieuMau.add(Box.createVerticalStrut(5));
        JLabel lblPhuDe = new JLabel("Chào mừng trở lại! Vui lòng đăng nhập để tiếp tục.");
        lblPhuDe.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 13));
        lblPhuDe.setForeground(HangSoGiaoDien.MUTED_LIGHT);
        lblPhuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlBieuMau.add(lblPhuDe);
        pnlBieuMau.add(Box.createVerticalStrut(25));
        pnlBieuMau.add(taoNhan("Tên đăng nhập"));
        pnlBieuMau.add(Box.createVerticalStrut(7));
        pnlBieuMau.add(taoONhap(txtTenDangNhap, "user", false));
        pnlBieuMau.add(Box.createVerticalStrut(16));
        pnlBieuMau.add(taoNhan("Mật khẩu"));
        pnlBieuMau.add(Box.createVerticalStrut(7));
        pnlBieuMau.add(taoONhap(txtMatKhau, null, true));
        pnlBieuMau.add(Box.createVerticalStrut(14));
        pnlBieuMau.add(taoKhungLoi());
        pnlBieuMau.add(Box.createVerticalStrut(14));
        btnDangNhap.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDangNhap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnDangNhap.setPreferredSize(new Dimension(360, 44));
        btnDangNhap.addActionListener(e -> dangNhap());
        pnlBieuMau.add(btnDangNhap);
        pnlBieuMau.add(Box.createVerticalStrut(16));
        JLabel lblHoTro = new JLabel("<html><div style='text-align:center'>Gặp sự cố? Liên hệ " + "<b><font color='#0f2744'>Bộ phận kỹ thuật</font></b></div></html>", SwingConstants.CENTER);
        lblHoTro.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 12));
        lblHoTro.setForeground(HangSoGiaoDien.MUTED_LIGHT);
        lblHoTro.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblHoTro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        // pnlBieuMau.add(lblHoTro);
        // pnlBieuMau.add(Box.createVerticalStrut(22));
        // pnlBieuMau.add(taoBangTaiKhoanMau());
        // pnlBieuMau.add(Box.createVerticalGlue());
        JPanel pnlVung = new JPanel(new GridBagLayout());
        pnlVung.setBackground(Color.WHITE);
        pnlVung.setBorder(new EmptyBorder(24, 24, 24, 24));
        GridBagConstraints giua = new GridBagConstraints();
        giua.gridx = 0;
        giua.gridy = 0;
        giua.weightx = 1;
        giua.weighty = 1;
        giua.anchor = GridBagConstraints.CENTER;
        pnlVung.add(pnlBieuMau, giua);
        return pnlVung;
    }
    private JPanel taoKhungHetPhien() {
        JLabel lblBieuTuong = new JLabel("◷");
        lblBieuTuong.setForeground(new Color(245, 158, 11));
        lblBieuTuong.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 17));
        lblTieuDePhien.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 12));
        lblTieuDePhien.setForeground(new Color(146, 64, 14));
        lblThongBaoPhien.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 11));
        lblThongBaoPhien.setForeground(new Color(180, 83, 9));
        JPanel pnlChu = new JPanel();
        pnlChu.setOpaque(false);
        pnlChu.setLayout(new BoxLayout(pnlChu, BoxLayout.Y_AXIS));
        pnlChu.add(lblTieuDePhien);
        pnlChu.add(Box.createVerticalStrut(2));
        pnlChu.add(lblThongBaoPhien);
        pnlThongBaoPhien.setVisible(false);
        pnlThongBaoPhien.setLayout(new BorderLayout(10, 0));
        pnlThongBaoPhien.setBorder(new EmptyBorder(12, 13, 12, 13));
        pnlThongBaoPhien.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        pnlThongBaoPhien.add(lblBieuTuong, BorderLayout.WEST);
        pnlThongBaoPhien.add(pnlChu, BorderLayout.CENTER);
        return pnlThongBaoPhien;
    }
    private JPanel taoKhungLoi() {
        JLabel lblDauChamThan = new JLabel("!");
        lblDauChamThan.setHorizontalAlignment(SwingConstants.CENTER);
        lblDauChamThan.setForeground(Color.WHITE);
        lblDauChamThan.setOpaque(true);
        lblDauChamThan.setBackground(new Color(239, 68, 68));
        lblDauChamThan.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 11));
        lblDauChamThan.setPreferredSize(new Dimension(18, 18));
        lblLoi.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 11));
        lblLoi.setForeground(new Color(220, 38, 38));
        pnlLoi.setVisible(false);
        pnlLoi.setLayout(new BorderLayout(8, 0));
        pnlLoi.setBorder(new EmptyBorder(10, 11, 10, 11));
        pnlLoi.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        pnlLoi.add(lblDauChamThan, BorderLayout.WEST);
        pnlLoi.add(lblLoi, BorderLayout.CENTER);
        return pnlLoi;
    }
    private JLabel taoNhan(String chu) {
        JLabel lblNhan = new JLabel(chu);
        lblNhan.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 13));
        lblNhan.setForeground(HangSoGiaoDien.TEXT);
        lblNhan.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lblNhan;
    }
    private JPanel taoONhap(JTextField txtNhap, String tenIcon, boolean coNutXemMatKhau) {
        final Color mauVienThuong = new Color(203, 213, 225);
        KhungBoGoc pnlVien = new KhungBoGoc(9, HangSoGiaoDien.INPUT_BG);
        pnlVien.setMauVien(mauVienThuong);
        pnlVien.setLayout(new BorderLayout());
        pnlVien.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        pnlVien.setPreferredSize(new Dimension(360, 46));
        pnlVien.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (tenIcon != null) {
            JLabel lblBieuTuong = new JLabel(new BieuTuongUngDung(tenIcon, 16, HangSoGiaoDien.MUTED));
            lblBieuTuong.setBorder(new EmptyBorder(0, 12, 0, 8));
            pnlVien.add(lblBieuTuong, BorderLayout.WEST);
        }
        txtNhap.setBorder(new EmptyBorder(0, tenIcon == null ? 12 : 0, 0, 0));
        txtNhap.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 14));
        txtNhap.setForeground(HangSoGiaoDien.TEXT);
        txtNhap.setCaretColor(HangSoGiaoDien.TEXT);
        txtNhap.setBackground(HangSoGiaoDien.INPUT_BG);
        txtNhap.setOpaque(false);
        txtNhap.setPreferredSize(new Dimension(300, 44));
        pnlVien.add(txtNhap, BorderLayout.CENTER);
        if (coNutXemMatKhau) {
            JButton btnXemMatKhau = new JButton(new BieuTuongUngDung("eye", 16, HangSoGiaoDien.MUTED));
            btnXemMatKhau.setPreferredSize(new Dimension(42, 44));
            btnXemMatKhau.setBorderPainted(false);
            btnXemMatKhau.setContentAreaFilled(false);
            btnXemMatKhau.setFocusPainted(false);
            btnXemMatKhau.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnXemMatKhau.addActionListener(e -> doiHienMatKhau());
            pnlVien.add(btnXemMatKhau, BorderLayout.EAST);
        }
        txtNhap.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                pnlVien.setMauVien(HangSoGiaoDien.ORANGE);
                pnlVien.setMauNen(Color.WHITE);
            }
            @Override
            public void focusLost(FocusEvent e) {
                pnlVien.setMauVien(mauVienThuong);
                pnlVien.setMauNen(HangSoGiaoDien.INPUT_BG);
            }
        });
        txtNhap.addActionListener(e -> dangNhap());
        txtNhap.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() != '\n' && e.getKeyChar() != '\r') xoaLoi();
            }
        });
        return pnlVien;
    }
    private JPanel taoBangTaiKhoanMau() {
        JLabel lblTieuDe = new JLabel("TÀI KHOẢN MẪU — NHẤN ĐỂ ĐIỀN NHANH", SwingConstants.CENTER);
        lblTieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 10));
        lblTieuDe.setForeground(HangSoGiaoDien.MUTED);
        lblTieuDe.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTieuDe.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        KhungBoGoc pnlBang = new KhungBoGoc(12, HangSoGiaoDien.INPUT_BG);
        pnlBang.setMauVien(new Color(241, 245, 249));
        pnlBang.setLayout(new BoxLayout(pnlBang, BoxLayout.Y_AXIS));
        pnlBang.setBorder(new EmptyBorder(13, 14, 13, 14));
        pnlBang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        pnlBang.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlBang.add(lblTieuDe);
        pnlBang.add(Box.createVerticalStrut(9));
        pnlBang.add(Box.createVerticalStrut(5));
        pnlBang.add(taoDongTaiKhoanMau("Quản trị hệ thống", "admin", "Admin@1234"));
        pnlBang.add(Box.createVerticalStrut(5));
        pnlBang.add(taoDongTaiKhoanMau("Quản lý", "admin01", "Admin@123"));
        pnlBang.add(Box.createVerticalStrut(5));
        pnlBang.add(taoDongTaiKhoanMau("Bán hàng", "sale01", "Sale@1234"));
        pnlBang.add(Box.createVerticalStrut(5));
        pnlBang.add(taoDongTaiKhoanMau("CSKH", "cskh01", "Cskh@1234"));
        return pnlBang;
    }
    private JPanel taoDongTaiKhoanMau(String vaiTro, String tenDangNhap, String matKhau) {
        final Color mauChuThuong = new Color(51, 65, 85);
        final Color mauChuNenToi = new Color(203, 213, 225);
        JLabel lblVaiTro = new JLabel(vaiTro);
        lblVaiTro.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 11));
        lblVaiTro.setForeground(mauChuThuong);
        JLabel lblTenDangNhap = new JLabel(tenDangNhap);
        lblTenDangNhap.setFont(new Font("Monospaced", Font.PLAIN, 10));
        lblTenDangNhap.setForeground(HangSoGiaoDien.MUTED_LIGHT);
        lblTenDangNhap.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel lblMatKhau = new JLabel(matKhau);
        lblMatKhau.setFont(new Font("Monospaced", Font.PLAIN, 10));
        lblMatKhau.setForeground(HangSoGiaoDien.MUTED_LIGHT);
        lblMatKhau.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel pnlTaiKhoan = new JPanel(new GridLayout(1, 2, 14, 0));
        pnlTaiKhoan.setOpaque(false);
        pnlTaiKhoan.add(lblTenDangNhap);
        pnlTaiKhoan.add(lblMatKhau);
        KhungBoGoc pnlDong = new KhungBoGoc(9, Color.WHITE);
        pnlDong.setMauVien(HangSoGiaoDien.BORDER);
        pnlDong.setLayout(new BorderLayout());
        pnlDong.setBorder(new EmptyBorder(7, 10, 7, 10));
        pnlDong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        pnlDong.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pnlDong.add(lblVaiTro, BorderLayout.WEST);
        pnlDong.add(pnlTaiKhoan, BorderLayout.EAST);
        MouseAdapter xuLyChuot = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setThongTinDangNhap(tenDangNhap, matKhau);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                pnlDong.setMauNen(HangSoGiaoDien.NAVY);
                lblVaiTro.setForeground(Color.WHITE);
                lblTenDangNhap.setForeground(mauChuNenToi);
                lblMatKhau.setForeground(mauChuNenToi);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                pnlDong.setMauNen(Color.WHITE);
                lblVaiTro.setForeground(mauChuThuong);
                lblTenDangNhap.setForeground(HangSoGiaoDien.MUTED_LIGHT);
                lblMatKhau.setForeground(HangSoGiaoDien.MUTED_LIGHT);
            }
        };
        pnlDong.addMouseListener(xuLyChuot);
        lblVaiTro.addMouseListener(xuLyChuot);
        lblTenDangNhap.addMouseListener(xuLyChuot);
        return pnlDong;
    }
    public String getTenDangNhap() {
        return txtTenDangNhap.getText();
    }
    public String getMatKhau() {
        return new String(txtMatKhau.getPassword());
    }
    public void xoaMatKhau() {
        txtMatKhau.setText("");
        txtMatKhau.requestFocusInWindow();
    }
    public void setThongTinDangNhap(String tenDangNhap, String matKhau) {
        txtTenDangNhap.setText(tenDangNhap);
        txtMatKhau.setText(matKhau);
        xoaLoi();
        txtMatKhau.requestFocusInWindow();
    }
    public void hienLoi(String thongBao) {
        lblLoi.setText("<html>" + thongBao + "</html>");
        pnlLoi.setVisible(true);
        revalidate();
        repaint();
    }
    public void xoaLoi() {
        lblLoi.setText(" ");
        pnlLoi.setVisible(false);
        revalidate();
        repaint();
    }
    public void hienThongBaoPhien(String thongBao) {
        lblThongBaoPhien.setText("<html>" + thongBao + "</html>");
        pnlThongBaoPhien.setVisible(true);
        revalidate();
        repaint();
    }
    private void doiHienMatKhau() {
        dangHienMatKhau = !dangHienMatKhau;
        if (dangHienMatKhau) {
            txtMatKhau.setEchoChar((char) 0);
        } else {
            txtMatKhau.setEchoChar('•');
        }
    }
    private void dangNhap() {
        TaiKhoanService.KetQuaDangNhap ketQua = dichVuXacThuc.dangNhap(getTenDangNhap(), getMatKhau());
        if (!ketQua.isThanhCong()) {
            hienLoi(ketQua.getThongBao());
            xoaMatKhau();
            return;
        }
        xoaLoi();
        moManHinhChinh();
    }
    private void moManHinhChinh() {
        dispose();
        new KhungChinh(dichVuXacThuc).setVisible(true);
    }
}

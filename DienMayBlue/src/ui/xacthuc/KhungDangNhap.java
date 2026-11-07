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
    private final JLabel lblError = new JLabel(" ");
    private final JPanel khungLoi = new KhungBoGoc(10, new Color(254, 242, 242));
    private final JPanel thongBaoPhien = new KhungBoGoc(12, new Color(255, 251, 235));
    private final JLabel tieuDePhien = new JLabel("Phiên làm việc đã hết hạn");
    private final JLabel sessionText = new JLabel("Bạn đã không hoạt động trong 15 phút. Vui lòng đăng nhập lại.");
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
        JPanel khung = new JPanel(new GridBagLayout());
        khung.setBackground(Color.WHITE);
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
        khung.add(taoPhanGioiThieu(), trai);
        khung.add(taoPhanDangNhap(), phai);
        return khung;
    }
    private JPanel taoPhanGioiThieu() {
        JPanel khung = new JPanel(new BorderLayout());
        khung.setBackground(HangSoGiaoDien.NAVY);
        khung.setBorder(new EmptyBorder(34, 34, 34, 34));
        khung.add(taoLogo(), BorderLayout.NORTH);
        khung.add(taoNoiDungGioiThieu(), BorderLayout.CENTER);
        JLabel banQuyen = new JLabel("© 2025 Điện Máy Blue. Bảo lưu mọi quyền.");
        banQuyen.setForeground(new Color(107, 137, 168));
        banQuyen.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 11));
        khung.add(banQuyen, BorderLayout.SOUTH);
        return khung;
    }
    private JPanel taoLogo() {
        KhungBoGoc oVuong = new KhungBoGoc(12, HangSoGiaoDien.ORANGE);
        oVuong.setVeVien(false);
        oVuong.setPreferredSize(new Dimension(36, 36));
        oVuong.setMinimumSize(new Dimension(36, 36));
        oVuong.setMaximumSize(new Dimension(36, 36));
        oVuong.setLayout(new GridBagLayout());
        JLabel chuB = new JLabel("B");
        chuB.setForeground(Color.WHITE);
        chuB.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 16));
        oVuong.add(chuB);
        JLabel tenCuaHang = new JLabel("Điện Máy Blue");
        tenCuaHang.setForeground(Color.WHITE);
        tenCuaHang.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 15));
        tenCuaHang.setBorder(new EmptyBorder(0, 12, 0, 0));
        JPanel logo = new JPanel();
        logo.setOpaque(false);
        logo.setLayout(new BoxLayout(logo, BoxLayout.X_AXIS));
        logo.add(oVuong);
        logo.add(tenCuaHang);
        return logo;
    }
    private JPanel taoNoiDungGioiThieu() {
        JPanel noiDung = new JPanel();
        noiDung.setOpaque(false);
        noiDung.setLayout(new BoxLayout(noiDung, BoxLayout.Y_AXIS));
        noiDung.add(Box.createVerticalGlue());
        JLabel tieuDe = new JLabel("<html>Hệ thống quản lý<br>cửa hàng điện máy</html>");
        tieuDe.setForeground(Color.WHITE);
        tieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 31));
        tieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        noiDung.add(tieuDe);
        noiDung.add(Box.createVerticalStrut(18));
        JLabel moTa = new JLabel( "<html><div style='width:300px'>Quản lý toàn diện từ bán hàng, kho hàng, khách hàng " + "đến thống kê kinh doanh theo thời gian thực.</div></html>");
        moTa.setForeground(new Color(159, 184, 210));
        moTa.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 15));
        moTa.setAlignmentX(Component.LEFT_ALIGNMENT);
        noiDung.add(moTa);
        noiDung.add(Box.createVerticalStrut(46));
        noiDung.add(taoHangSoLieu());
        noiDung.add(Box.createVerticalGlue());
        return noiDung;
    }
    private JPanel taoHangSoLieu() {
        JPanel hang = new JPanel(new GridLayout(1, 3, 12, 0));
        hang.setOpaque(false);
        hang.setBorder(new EmptyBorder(25, 0, 0, 0));
        hang.add(taoSoLieu("12.500+", "Sản phẩm"));
        hang.add(taoSoLieu("48.000+", "Khách hàng"));
        hang.add(taoSoLieu("320+", "Nhân viên"));
        JPanel khung = new JPanel(new BorderLayout());
        khung.setOpaque(false);
        khung.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 25)));
        khung.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        khung.setAlignmentX(Component.LEFT_ALIGNMENT);
        khung.add(hang, BorderLayout.CENTER);
        return khung;
    }
    private JPanel taoSoLieu(String so, String ten) {
        JLabel lblSo = new JLabel(so);
        lblSo.setForeground(HangSoGiaoDien.ORANGE);
        lblSo.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 24));
        JLabel lblTen = new JLabel(ten);
        lblTen.setForeground(new Color(112, 151, 189));
        lblTen.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 12));
        JPanel khung = new JPanel();
        khung.setOpaque(false);
        khung.setLayout(new BoxLayout(khung, BoxLayout.Y_AXIS));
        khung.add(lblSo);
        khung.add(Box.createVerticalStrut(3));
        khung.add(lblTen);
        return khung;
    }
    private JPanel taoPhanDangNhap() {
        JPanel bieuMau = new JPanel();
        bieuMau.setOpaque(false);
        bieuMau.setPreferredSize(new Dimension(360, 610));
        bieuMau.setMaximumSize(new Dimension(360, Integer.MAX_VALUE));
        bieuMau.setLayout(new BoxLayout(bieuMau, BoxLayout.Y_AXIS));
        bieuMau.add(Box.createVerticalGlue());
        bieuMau.add(taoKhungHetPhien());
        bieuMau.add(Box.createVerticalStrut(18));
        JLabel tieuDe = new JLabel("Đăng nhập");
        tieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 26));
        tieuDe.setForeground(HangSoGiaoDien.TEXT);
        tieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        bieuMau.add(tieuDe);
        bieuMau.add(Box.createVerticalStrut(5));
        JLabel phuDe = new JLabel("Chào mừng trở lại! Vui lòng đăng nhập để tiếp tục.");
        phuDe.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 13));
        phuDe.setForeground(HangSoGiaoDien.MUTED_LIGHT);
        phuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        bieuMau.add(phuDe);
        bieuMau.add(Box.createVerticalStrut(25));
        bieuMau.add(taoNhan("Tên đăng nhập"));
        bieuMau.add(Box.createVerticalStrut(7));
        bieuMau.add(taoONhap(txtTenDangNhap, "user", false));
        bieuMau.add(Box.createVerticalStrut(16));
        bieuMau.add(taoNhan("Mật khẩu"));
        bieuMau.add(Box.createVerticalStrut(7));
        bieuMau.add(taoONhap(txtMatKhau, null, true));
        bieuMau.add(Box.createVerticalStrut(14));
        bieuMau.add(taoKhungLoi());
        bieuMau.add(Box.createVerticalStrut(14));
        btnDangNhap.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDangNhap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnDangNhap.setPreferredSize(new Dimension(360, 44));
        btnDangNhap.addActionListener(e -> dangNhap());
        bieuMau.add(btnDangNhap);
        bieuMau.add(Box.createVerticalStrut(16));
        JLabel hoTro = new JLabel("<html><div style='text-align:center'>Gặp sự cố? Liên hệ " + "<b><font color='#0f2744'>Bộ phận kỹ thuật</font></b></div></html>", SwingConstants.CENTER);
        hoTro.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 12));
        hoTro.setForeground(HangSoGiaoDien.MUTED_LIGHT);
        hoTro.setAlignmentX(Component.CENTER_ALIGNMENT);
        hoTro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        bieuMau.add(hoTro);
        bieuMau.add(Box.createVerticalStrut(22));
        bieuMau.add(taoBangTaiKhoanMau());
        bieuMau.add(Box.createVerticalGlue());
        JPanel vung = new JPanel(new GridBagLayout());
        vung.setBackground(Color.WHITE);
        vung.setBorder(new EmptyBorder(24, 24, 24, 24));
        GridBagConstraints giua = new GridBagConstraints();
        giua.gridx = 0;
        giua.gridy = 0;
        giua.weightx = 1;
        giua.weighty = 1;
        giua.anchor = GridBagConstraints.CENTER;
        vung.add(bieuMau, giua);
        return vung;
    }
    private JPanel taoKhungHetPhien() {
        JLabel bieuTuong = new JLabel("◷");
        bieuTuong.setForeground(new Color(245, 158, 11));
        bieuTuong.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 17));
        tieuDePhien.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 12));
        tieuDePhien.setForeground(new Color(146, 64, 14));
        sessionText.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 11));
        sessionText.setForeground(new Color(180, 83, 9));
        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        chu.add(tieuDePhien);
        chu.add(Box.createVerticalStrut(2));
        chu.add(sessionText);
        thongBaoPhien.setVisible(false);
        thongBaoPhien.setLayout(new BorderLayout(10, 0));
        thongBaoPhien.setBorder(new EmptyBorder(12, 13, 12, 13));
        thongBaoPhien.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        thongBaoPhien.add(bieuTuong, BorderLayout.WEST);
        thongBaoPhien.add(chu, BorderLayout.CENTER);
        return thongBaoPhien;
    }
    private JPanel taoKhungLoi() {
        JLabel dauChamThan = new JLabel("!");
        dauChamThan.setHorizontalAlignment(SwingConstants.CENTER);
        dauChamThan.setForeground(Color.WHITE);
        dauChamThan.setOpaque(true);
        dauChamThan.setBackground(new Color(239, 68, 68));
        dauChamThan.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 11));
        dauChamThan.setPreferredSize(new Dimension(18, 18));
        lblError.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 11));
        lblError.setForeground(new Color(220, 38, 38));
        khungLoi.setVisible(false);
        khungLoi.setLayout(new BorderLayout(8, 0));
        khungLoi.setBorder(new EmptyBorder(10, 11, 10, 11));
        khungLoi.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        khungLoi.add(dauChamThan, BorderLayout.WEST);
        khungLoi.add(lblError, BorderLayout.CENTER);
        return khungLoi;
    }
    private JLabel taoNhan(String chu) {
        JLabel nhan = new JLabel(chu);
        nhan.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 13));
        nhan.setForeground(HangSoGiaoDien.TEXT);
        nhan.setAlignmentX(Component.LEFT_ALIGNMENT);
        return nhan;
    }
    private JPanel taoONhap(JTextField oNhap, String tenIcon, boolean coNutXemMatKhau) {
        final Color mauVienThuong = new Color(203, 213, 225);
        KhungBoGoc vien = new KhungBoGoc(9, HangSoGiaoDien.INPUT_BG);
        vien.setMauVien(mauVienThuong);
        vien.setLayout(new BorderLayout());
        vien.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        vien.setPreferredSize(new Dimension(360, 46));
        vien.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (tenIcon != null) {
            JLabel bieuTuong = new JLabel(new BieuTuongUngDung(tenIcon, 16, HangSoGiaoDien.MUTED));
            bieuTuong.setBorder(new EmptyBorder(0, 12, 0, 8));
            vien.add(bieuTuong, BorderLayout.WEST);
        }
        oNhap.setBorder(new EmptyBorder(0, tenIcon == null ? 12 : 0, 0, 0));
        oNhap.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 14));
        oNhap.setForeground(HangSoGiaoDien.TEXT);
        oNhap.setCaretColor(HangSoGiaoDien.TEXT);
        oNhap.setBackground(HangSoGiaoDien.INPUT_BG);
        oNhap.setOpaque(false);
        oNhap.setPreferredSize(new Dimension(300, 44));
        vien.add(oNhap, BorderLayout.CENTER);
        if (coNutXemMatKhau) {
            JButton nutMat = new JButton(new BieuTuongUngDung("eye", 16, HangSoGiaoDien.MUTED));
            nutMat.setPreferredSize(new Dimension(42, 44));
            nutMat.setBorderPainted(false);
            nutMat.setContentAreaFilled(false);
            nutMat.setFocusPainted(false);
            nutMat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            nutMat.addActionListener(e -> doiHienMatKhau());
            vien.add(nutMat, BorderLayout.EAST);
        }
        oNhap.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                vien.setMauVien(HangSoGiaoDien.ORANGE);
                vien.setMauNen(Color.WHITE);
            }
            @Override
            public void focusLost(FocusEvent e) {
                vien.setMauVien(mauVienThuong);
                vien.setMauNen(HangSoGiaoDien.INPUT_BG);
            }
        });
        oNhap.addActionListener(e -> dangNhap());
        oNhap.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() != '\n' && e.getKeyChar() != '\r') xoaLoi();
            }
        });
        return vien;
    }
    private JPanel taoBangTaiKhoanMau() {
        JLabel tieuDe = new JLabel("TÀI KHOẢN MẪU — NHẤN ĐỂ ĐIỀN NHANH", SwingConstants.CENTER);
        tieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 10));
        tieuDe.setForeground(HangSoGiaoDien.MUTED);
        tieuDe.setAlignmentX(Component.CENTER_ALIGNMENT);
        tieuDe.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        KhungBoGoc bang = new KhungBoGoc(12, HangSoGiaoDien.INPUT_BG);
        bang.setMauVien(new Color(241, 245, 249));
        bang.setLayout(new BoxLayout(bang, BoxLayout.Y_AXIS));
        bang.setBorder(new EmptyBorder(13, 14, 13, 14));
        bang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        bang.setAlignmentX(Component.LEFT_ALIGNMENT);
        bang.add(tieuDe);
        bang.add(Box.createVerticalStrut(9));
        bang.add(Box.createVerticalStrut(5));
        bang.add(taoDongTaiKhoanMau("Quản trị hệ thống", "admin", "Admin@1234"));
        bang.add(Box.createVerticalStrut(5));
        bang.add(taoDongTaiKhoanMau("Quản lý", "admin01", "Admin@123"));
        bang.add(Box.createVerticalStrut(5));
        bang.add(taoDongTaiKhoanMau("Bán hàng", "sale01", "Sale@1234"));
        bang.add(Box.createVerticalStrut(5));
        bang.add(taoDongTaiKhoanMau("CSKH", "cskh01", "Cskh@1234"));
        return bang;
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
        JPanel phanTaiKhoan = new JPanel(new GridLayout(1, 2, 14, 0));
        phanTaiKhoan.setOpaque(false);
        phanTaiKhoan.add(lblTenDangNhap);
        phanTaiKhoan.add(lblMatKhau);
        KhungBoGoc dong = new KhungBoGoc(9, Color.WHITE);
        dong.setMauVien(HangSoGiaoDien.BORDER);
        dong.setLayout(new BorderLayout());
        dong.setBorder(new EmptyBorder(7, 10, 7, 10));
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        dong.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dong.add(lblVaiTro, BorderLayout.WEST);
        dong.add(phanTaiKhoan, BorderLayout.EAST);
        MouseAdapter xuLyChuot = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setThongTinDangNhap(tenDangNhap, matKhau);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                dong.setMauNen(HangSoGiaoDien.NAVY);
                lblVaiTro.setForeground(Color.WHITE);
                lblTenDangNhap.setForeground(mauChuNenToi);
                lblMatKhau.setForeground(mauChuNenToi);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                dong.setMauNen(Color.WHITE);
                lblVaiTro.setForeground(mauChuThuong);
                lblTenDangNhap.setForeground(HangSoGiaoDien.MUTED_LIGHT);
                lblMatKhau.setForeground(HangSoGiaoDien.MUTED_LIGHT);
            }
        };
        dong.addMouseListener(xuLyChuot);
        lblVaiTro.addMouseListener(xuLyChuot);
        lblTenDangNhap.addMouseListener(xuLyChuot);
        return dong;
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
        lblError.setText("<html>" + thongBao + "</html>");
        khungLoi.setVisible(true);
        revalidate();
        repaint();
    }
    public void xoaLoi() {
        lblError.setText(" ");
        khungLoi.setVisible(false);
        revalidate();
        repaint();
    }
    public void hienThongBaoPhien(String thongBao) {
        sessionText.setText("<html>" + thongBao + "</html>");
        thongBaoPhien.setVisible(true);
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

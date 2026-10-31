package ui.xacthuc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import service.AuthService;
import ui.chinh.MainFrame;
import ui.dungchung.AppIcon;
import ui.dungchung.RoundedButton;
import ui.dungchung.RoundedPanel;
import ui.dungchung.UIConstants;

/**
 * Màn hình đăng nhập. Chia làm 2 phần:
 * bên trái: logo và giới thiệu
 * bên phải: form đăng nhập
 */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTextField txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final RoundedButton btnLogin = new RoundedButton(
            "Đăng nhập", UIConstants.ORANGE, UIConstants.ORANGE_HOVER, 12);

    private final JLabel lblError = new JLabel(" ");
    private final JPanel errorBox = new RoundedPanel(10, new Color(254, 242, 242));

    private final JPanel sessionBanner = new RoundedPanel(12, new Color(255, 251, 235));
    private final JLabel sessionTitle = new JLabel("Phiên làm việc đã hết hạn");
    private final JLabel sessionText = new JLabel(
            "Bạn đã không hoạt động trong 15 phút. Vui lòng đăng nhập lại.");

    private final AuthService authService = new AuthService();
    private boolean dangHienMatKhau = false;

    public LoginFrame() {
        setTitle("Điện Máy Blue - Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1180, 720);
        setResizable(false);
        setLocationRelativeTo(null);
        setBackground(Color.WHITE);

        setContentPane(taoKhungChinh());
        getRootPane().setDefaultButton(btnLogin);

       
        SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.NAVY);
        panel.setBorder(new EmptyBorder(34, 34, 34, 34));

        panel.add(taoLogo(), BorderLayout.NORTH);
        panel.add(taoNoiDungGioiThieu(), BorderLayout.CENTER);

        JLabel banQuyen = new JLabel("© 2025 Điện Máy Blue. Bảo lưu mọi quyền.");
        banQuyen.setForeground(new Color(107, 137, 168));
        banQuyen.setFont(UIConstants.font(Font.PLAIN, 11));
        panel.add(banQuyen, BorderLayout.SOUTH);
        return panel;
    }

    /** Ô vuông cam có chữ B và tên cửa hàng. */
    private JPanel taoLogo() {
        RoundedPanel oVuong = new RoundedPanel(12, UIConstants.ORANGE);
        oVuong.setDrawBorder(false);
        oVuong.setPreferredSize(new Dimension(36, 36));
        oVuong.setMinimumSize(new Dimension(36, 36));
        oVuong.setMaximumSize(new Dimension(36, 36));
        oVuong.setLayout(new GridBagLayout());

        JLabel chuB = new JLabel("B");
        chuB.setForeground(Color.WHITE);
        chuB.setFont(UIConstants.font(Font.BOLD, 16));
        oVuong.add(chuB);

        JLabel tenCuaHang = new JLabel("Điện Máy Blue");
        tenCuaHang.setForeground(Color.WHITE);
        tenCuaHang.setFont(UIConstants.font(Font.BOLD, 15));
        tenCuaHang.setBorder(new EmptyBorder(0, 12, 0, 0));

        JPanel logo = new JPanel();
        logo.setOpaque(false);
        logo.setLayout(new BoxLayout(logo, BoxLayout.X_AXIS));
        logo.add(oVuong);
        logo.add(tenCuaHang);
        return logo;
    }

    /** Dòng chữ lớn, mô tả và 3 số liệu ở giữa nửa bên trái. */
    private JPanel taoNoiDungGioiThieu() {
        JPanel noiDung = new JPanel();
        noiDung.setOpaque(false);
        noiDung.setLayout(new BoxLayout(noiDung, BoxLayout.Y_AXIS));
        noiDung.add(Box.createVerticalGlue());

        JLabel tieuDe = new JLabel("<html>Hệ thống quản lý<br>cửa hàng điện máy</html>");
        tieuDe.setForeground(Color.WHITE);
        tieuDe.setFont(UIConstants.font(Font.BOLD, 31));
        tieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        noiDung.add(tieuDe);
        noiDung.add(Box.createVerticalStrut(18));

        JLabel moTa = new JLabel(
                "<html><div style='width:300px'>Quản lý toàn diện từ bán hàng, kho hàng, khách hàng "
                + "đến thống kê kinh doanh theo thời gian thực.</div></html>");
        moTa.setForeground(new Color(159, 184, 210));
        moTa.setFont(UIConstants.font(Font.PLAIN, 15));
        moTa.setAlignmentX(Component.LEFT_ALIGNMENT);
        noiDung.add(moTa);
        noiDung.add(Box.createVerticalStrut(46));

        noiDung.add(taoHangSoLieu());
        noiDung.add(Box.createVerticalGlue());
        return noiDung;
    }

    /** Hàng 3 số liệu: sản phẩm, khách hàng, nhân viên. */
    private JPanel taoHangSoLieu() {
        JPanel hang = new JPanel(new GridLayout(1, 3, 12, 0));
        hang.setOpaque(false);
        hang.setBorder(new EmptyBorder(25, 0, 0, 0));
        hang.add(taoSoLieu("12.500+", "Sản phẩm"));
        hang.add(taoSoLieu("48.000+", "Khách hàng"));
        hang.add(taoSoLieu("320+", "Nhân viên"));

        // Có đường kẻ mờ phía trên
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
        lblSo.setForeground(UIConstants.ORANGE);
        lblSo.setFont(UIConstants.font(Font.BOLD, 24));

        JLabel lblTen = new JLabel(ten);
        lblTen.setForeground(new Color(112, 151, 189));
        lblTen.setFont(UIConstants.font(Font.PLAIN, 12));

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(lblSo);
        panel.add(Box.createVerticalStrut(3));
        panel.add(lblTen);
        return panel;
    }

    // =====================================================================
    // 3. Nửa bên phải: form đăng nhập
    // =====================================================================

    private JPanel taoPhanDangNhap() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(360, 610));
        form.setMaximumSize(new Dimension(360, Integer.MAX_VALUE));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(Box.createVerticalGlue());

        // Báo hết phiên (mặc định ẩn)
        form.add(taoKhungHetPhien());
        form.add(Box.createVerticalStrut(18));

        // Tiêu đề
        JLabel tieuDe = new JLabel("Đăng nhập");
        tieuDe.setFont(UIConstants.font(Font.BOLD, 26));
        tieuDe.setForeground(UIConstants.TEXT);
        tieuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(tieuDe);
        form.add(Box.createVerticalStrut(5));

        JLabel phuDe = new JLabel("Chào mừng trở lại! Vui lòng đăng nhập để tiếp tục.");
        phuDe.setFont(UIConstants.font(Font.PLAIN, 13));
        phuDe.setForeground(UIConstants.MUTED_LIGHT);
        phuDe.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(phuDe);
        form.add(Box.createVerticalStrut(25));

        // Ô tên đăng nhập
        form.add(taoNhan("Tên đăng nhập"));
        form.add(Box.createVerticalStrut(7));
        form.add(taoONhap(txtUsername, "user", false));
        form.add(Box.createVerticalStrut(16));

        // Ô mật khẩu (có nút con mắt để hiện / ẩn)
        form.add(taoNhan("Mật khẩu"));
        form.add(Box.createVerticalStrut(7));
        form.add(taoONhap(txtPassword, null, true));
        form.add(Box.createVerticalStrut(14));

        // Khung báo lỗi (mặc định ẩn)
        form.add(taoKhungLoi());
        form.add(Box.createVerticalStrut(14));

        // Nút đăng nhập
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.setPreferredSize(new Dimension(360, 44));
        btnLogin.addActionListener(e -> dangNhap());
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(16));

        // Dòng hỗ trợ
        JLabel hoTro = new JLabel(
                "<html><div style='text-align:center'>Gặp sự cố? Liên hệ "
                + "<b><font color='#0f2744'>Bộ phận kỹ thuật</font></b></div></html>",
                SwingConstants.CENTER);
        hoTro.setFont(UIConstants.font(Font.PLAIN, 12));
        hoTro.setForeground(UIConstants.MUTED_LIGHT);
        hoTro.setAlignmentX(Component.CENTER_ALIGNMENT);
        hoTro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        form.add(hoTro);
        form.add(Box.createVerticalStrut(22));

        // Bảng tài khoản mẫu
        form.add(taoBangTaiKhoanMau());
        form.add(Box.createVerticalGlue());

        // Đặt form vào giữa nửa bên phải
        JPanel vung = new JPanel(new GridBagLayout());
        vung.setBackground(Color.WHITE);
        vung.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints giua = new GridBagConstraints();
        giua.gridx = 0;
        giua.gridy = 0;
        giua.weightx = 1;
        giua.weighty = 1;
        giua.anchor = GridBagConstraints.CENTER;
        vung.add(form, giua);
        return vung;
    }

    /** Khung vàng báo "phiên làm việc đã hết hạn" (ẩn cho tới khi gọi showSessionMessage). */
    private JPanel taoKhungHetPhien() {
        JLabel bieuTuong = new JLabel("◷");
        bieuTuong.setForeground(new Color(245, 158, 11));
        bieuTuong.setFont(UIConstants.font(Font.BOLD, 17));

        sessionTitle.setFont(UIConstants.font(Font.BOLD, 12));
        sessionTitle.setForeground(new Color(146, 64, 14));
        sessionText.setFont(UIConstants.font(Font.PLAIN, 11));
        sessionText.setForeground(new Color(180, 83, 9));

        JPanel chu = new JPanel();
        chu.setOpaque(false);
        chu.setLayout(new BoxLayout(chu, BoxLayout.Y_AXIS));
        chu.add(sessionTitle);
        chu.add(Box.createVerticalStrut(2));
        chu.add(sessionText);

        sessionBanner.setVisible(false);
        sessionBanner.setLayout(new BorderLayout(10, 0));
        sessionBanner.setBorder(new EmptyBorder(12, 13, 12, 13));
        sessionBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        sessionBanner.add(bieuTuong, BorderLayout.WEST);
        sessionBanner.add(chu, BorderLayout.CENTER);
        return sessionBanner;
    }

    /** Khung đỏ nhạt có dấu "!" để hiện lỗi đăng nhập (ẩn cho tới khi có lỗi). */
    private JPanel taoKhungLoi() {
        JLabel dauChamThan = new JLabel("!");
        dauChamThan.setHorizontalAlignment(SwingConstants.CENTER);
        dauChamThan.setForeground(Color.WHITE);
        dauChamThan.setOpaque(true);
        dauChamThan.setBackground(new Color(239, 68, 68));
        dauChamThan.setFont(UIConstants.font(Font.BOLD, 11));
        dauChamThan.setPreferredSize(new Dimension(18, 18));

        lblError.setFont(UIConstants.font(Font.PLAIN, 11));
        lblError.setForeground(new Color(220, 38, 38));

        errorBox.setVisible(false);
        errorBox.setLayout(new BorderLayout(8, 0));
        errorBox.setBorder(new EmptyBorder(10, 11, 10, 11));
        errorBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        errorBox.add(dauChamThan, BorderLayout.WEST);
        errorBox.add(lblError, BorderLayout.CENTER);
        return errorBox;
    }

    private JLabel taoNhan(String chu) {
        JLabel nhan = new JLabel(chu);
        nhan.setFont(UIConstants.font(Font.BOLD, 13));
        nhan.setForeground(UIConstants.TEXT);
        nhan.setAlignmentX(Component.LEFT_ALIGNMENT);
        return nhan;
    }

    /**
     * Ô nhập có viền bo tròn.
     * - tenIcon: biểu tượng bên trái (null thì không có)
     * - coNutXemMatKhau: true thì thêm nút con mắt bên phải
     * Viền đổi sang màu cam khi ô đang được chọn; nhấn Enter hoặc gõ chữ thì xóa thông báo lỗi.
     */
    private JPanel taoONhap(JTextField oNhap, String tenIcon, boolean coNutXemMatKhau) {
        final Color mauVienThuong = new Color(203, 213, 225);

        RoundedPanel vien = new RoundedPanel(9, UIConstants.INPUT_BG);
        vien.setBorderColor(mauVienThuong);
        vien.setLayout(new BorderLayout());
        vien.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        vien.setPreferredSize(new Dimension(360, 46));
        vien.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (tenIcon != null) {
            JLabel icon = new JLabel(new AppIcon(tenIcon, 16, UIConstants.MUTED));
            icon.setBorder(new EmptyBorder(0, 12, 0, 8));
            vien.add(icon, BorderLayout.WEST);
        }

        oNhap.setBorder(new EmptyBorder(0, tenIcon == null ? 12 : 0, 0, 0));
        oNhap.setFont(UIConstants.font(Font.PLAIN, 14));
        oNhap.setForeground(UIConstants.TEXT);
        oNhap.setCaretColor(UIConstants.TEXT);
        oNhap.setBackground(UIConstants.INPUT_BG);
        oNhap.setOpaque(false);
        oNhap.setPreferredSize(new Dimension(300, 44));
        vien.add(oNhap, BorderLayout.CENTER);

        if (coNutXemMatKhau) {
            JButton nutMat = new JButton(new AppIcon("eye", 16, UIConstants.MUTED));
            nutMat.setPreferredSize(new Dimension(42, 44));
            nutMat.setBorderPainted(false);
            nutMat.setContentAreaFilled(false);
            nutMat.setFocusPainted(false);
            nutMat.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            nutMat.addActionListener(e -> doiHienMatKhau());
            vien.add(nutMat, BorderLayout.EAST);
        }

        // Ô được chọn thì viền cam, nền trắng; rời ô thì trả về như cũ
        oNhap.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                vien.setBorderColor(UIConstants.ORANGE);
                vien.setFillColor(Color.WHITE);
            }

            @Override
            public void focusLost(FocusEvent e) {
                vien.setBorderColor(mauVienThuong);
                vien.setFillColor(UIConstants.INPUT_BG);
            }
        });

        oNhap.addActionListener(e -> dangNhap());
        oNhap.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                clearError();
            }
        });
        return vien;
    }

    // =====================================================================
    // 4. Bảng tài khoản mẫu (bấm vào dòng nào thì điền sẵn vào form)
    // =====================================================================

    private JPanel taoBangTaiKhoanMau() {
        JLabel tieuDe = new JLabel("TÀI KHOẢN MẪU — NHẤN ĐỂ ĐIỀN NHANH", SwingConstants.CENTER);
        tieuDe.setFont(UIConstants.font(Font.BOLD, 10));
        tieuDe.setForeground(UIConstants.MUTED);
        tieuDe.setAlignmentX(Component.CENTER_ALIGNMENT);
        tieuDe.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        RoundedPanel bang = new RoundedPanel(12, UIConstants.INPUT_BG);
        bang.setBorderColor(new Color(241, 245, 249));
        bang.setLayout(new BoxLayout(bang, BoxLayout.Y_AXIS));
        bang.setBorder(new EmptyBorder(13, 14, 13, 14));
        bang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 205));
        bang.setAlignmentX(Component.LEFT_ALIGNMENT);

        bang.add(tieuDe);
        bang.add(Box.createVerticalStrut(9));
        bang.add(taoDongTaiKhoanMau("Admin", "admin", "Admin@1234"));
        bang.add(Box.createVerticalStrut(5));
        bang.add(taoDongTaiKhoanMau("Quản lý", "admin01", "Admin@123"));
        bang.add(Box.createVerticalStrut(5));
        bang.add(taoDongTaiKhoanMau("Bán hàng", "sale01", "Sale@1234"));
        bang.add(Box.createVerticalStrut(5));
        bang.add(taoDongTaiKhoanMau("CSKH", "cskh01", "Cskh@1234"));
        return bang;
    }

    /** Một dòng tài khoản mẫu: đưa chuột vào thì đổi màu, bấm vào thì điền form. */
    private JPanel taoDongTaiKhoanMau(String vaiTro, String tenDangNhap, String matKhau) {
        final Color mauChuThuong = new Color(51, 65, 85);
        final Color mauChuNenToi = new Color(203, 213, 225);

        JLabel lblVaiTro = new JLabel(vaiTro);
        lblVaiTro.setFont(UIConstants.font(Font.BOLD, 11));
        lblVaiTro.setForeground(mauChuThuong);

        JLabel lblTenDangNhap = new JLabel(tenDangNhap);
        lblTenDangNhap.setFont(new Font("Monospaced", Font.PLAIN, 10));
        lblTenDangNhap.setForeground(UIConstants.MUTED_LIGHT);
        lblTenDangNhap.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel lblMatKhau = new JLabel(matKhau);
        lblMatKhau.setFont(new Font("Monospaced", Font.PLAIN, 10));
        lblMatKhau.setForeground(UIConstants.MUTED_LIGHT);
        lblMatKhau.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel phanTaiKhoan = new JPanel(new GridLayout(1, 2, 14, 0));
        phanTaiKhoan.setOpaque(false);
        phanTaiKhoan.add(lblTenDangNhap);
        phanTaiKhoan.add(lblMatKhau);

        RoundedPanel dong = new RoundedPanel(9, Color.WHITE);
        dong.setBorderColor(UIConstants.BORDER);
        dong.setLayout(new BorderLayout());
        dong.setBorder(new EmptyBorder(7, 10, 7, 10));
        dong.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        dong.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dong.add(lblVaiTro, BorderLayout.WEST);
        dong.add(phanTaiKhoan, BorderLayout.EAST);

        MouseAdapter xuLyChuot = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setCredentials(tenDangNhap, matKhau);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                dong.setFillColor(UIConstants.NAVY);
                lblVaiTro.setForeground(Color.WHITE);
                lblTenDangNhap.setForeground(mauChuNenToi);
                lblMatKhau.setForeground(mauChuNenToi);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                dong.setFillColor(Color.WHITE);
                lblVaiTro.setForeground(mauChuThuong);
                lblTenDangNhap.setForeground(UIConstants.MUTED_LIGHT);
                lblMatKhau.setForeground(UIConstants.MUTED_LIGHT);
            }
        };
        dong.addMouseListener(xuLyChuot);
        lblVaiTro.addMouseListener(xuLyChuot);
        lblTenDangNhap.addMouseListener(xuLyChuot);
        return dong;
    }

    // =====================================================================
    // 5. Các hàm công khai (màn hình khác cũng gọi tới)
    // =====================================================================

    public String getUsername() {
        return txtUsername.getText();
    }

    public String getPassword() {
        return new String(txtPassword.getPassword());
    }

    /** Xóa ô mật khẩu và đặt con trỏ vào đó. */
    public void clearPassword() {
        txtPassword.setText("");
        txtPassword.requestFocusInWindow();
    }

    /** Điền sẵn tên đăng nhập và mật khẩu (dùng cho bảng tài khoản mẫu). */
    public void setCredentials(String username, String password) {
        txtUsername.setText(username);
        txtPassword.setText(password);
        clearError();
        txtPassword.requestFocusInWindow();
    }

    public void showError(String message) {
        lblError.setText("<html>" + message + "</html>");
        errorBox.setVisible(true);
        revalidate();
        repaint();
    }

    public void clearError() {
        lblError.setText(" ");
        errorBox.setVisible(false);
        revalidate();
        repaint();
    }

    /** Hiện khung vàng với nội dung thông báo (ví dụ: hết phiên đăng nhập). */
    public void showSessionMessage(String message) {
        sessionText.setText("<html>" + message + "</html>");
        sessionBanner.setVisible(true);
        revalidate();
        repaint();
    }

    // =====================================================================
    // 6. Xử lý đăng nhập
    // =====================================================================

    private void doiHienMatKhau() {
        dangHienMatKhau = !dangHienMatKhau;

        if (dangHienMatKhau) {
            txtPassword.setEchoChar((char) 0); // hiện chữ thật
        } else {
            txtPassword.setEchoChar('•'); // che bằng dấu chấm
        }
    }

    private void dangNhap() {
        AuthService.LoginResult ketQua = authService.login(getUsername(), getPassword());

        // Sai: hiện lỗi và xóa ô mật khẩu
        if (!ketQua.isSuccess()) {
            showError(ketQua.getMessage());
            clearPassword();
            return;
        }

        clearError();

        // Đăng nhập lần đầu: bắt đổi mật khẩu rồi mới vào hệ thống
        if (ketQua.isRequiresPasswordChange()) {
            ChangePasswordDialog hopThoai = new ChangePasswordDialog(
                    this, authService, true, this::moManHinhChinh);
            hopThoai.setVisible(true);
            return;
        }

        moManHinhChinh();
    }

    private void moManHinhChinh() {
        dispose();
        new MainFrame(authService).setVisible(true);
    }
}

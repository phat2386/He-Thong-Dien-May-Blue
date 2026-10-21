package app;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;


public class UIDangNhap extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTextField txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JLabel lblError = new JLabel(" ");
    private final JPanel errorBox = new RoundedPanel(10, new Color(254,242,242));
    private final JPanel sessionBanner = new RoundedPanel(12, new Color(255,251,235));
    private final JLabel sessionTitle = new JLabel("Phiên làm việc đã hết hạn");
    private final JLabel sessionText = new JLabel("Bạn đã không hoạt động trong 15 phút. Vui lòng đăng nhập lại.");
    private final RoundedButton btnLogin = new RoundedButton(
            "Đăng nhập", UIConstants.ORANGE, UIConstants.ORANGE_HOVER, 12);
    private final DangNhapController controller;
    private boolean passwordVisible;

    public UIDangNhap() {
        setTitle("Điện Máy Blue - Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180,720));
        setSize(1180,720);
        setResizable(false);
        setLocationRelativeTo(null);
        setBackground(Color.WHITE);

        controller = new DangNhapController(this);

        setContentPane(createRoot());
        getRootPane().setDefaultButton(btnLogin);

        SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
    }

    private JPanel createRoot() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(Color.WHITE);

        GridBagConstraints left = new GridBagConstraints();
        left.gridx=0; left.gridy=0; left.weightx=.40; left.weighty=1;
        left.fill=GridBagConstraints.BOTH;

        GridBagConstraints right = new GridBagConstraints();
        right.gridx=1; right.gridy=0; right.weightx=.60; right.weighty=1;
        right.fill=GridBagConstraints.BOTH;

        root.add(createBrandPanel(), left);
        root.add(createFormArea(), right);
        return root;
    }

    private JPanel createBrandPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.NAVY);
        panel.setBorder(new EmptyBorder(34,34,34,34));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));

        RoundedPanel logo = new RoundedPanel(12, UIConstants.ORANGE);
        logo.setDrawBorder(false);
        logo.setPreferredSize(new Dimension(36,36));
        logo.setMinimumSize(new Dimension(36,36));
        logo.setMaximumSize(new Dimension(36,36));
        logo.setLayout(new GridBagLayout());

        JLabel b = new JLabel("B");
        b.setForeground(Color.WHITE);
        b.setFont(UIConstants.font(Font.BOLD,16));
        logo.add(b);

        JLabel name = new JLabel("Điện Máy Blue");
        name.setForeground(Color.WHITE);
        name.setFont(UIConstants.font(Font.BOLD,15));
        name.setBorder(new EmptyBorder(0,12,0,0));

        brand.add(logo);
        brand.add(name);
        panel.add(brand,BorderLayout.NORTH);

        JPanel hero = new JPanel();
        hero.setOpaque(false);
        hero.setLayout(new BoxLayout(hero,BoxLayout.Y_AXIS));

        hero.add(Box.createVerticalGlue());

        JLabel heading = new JLabel(
                "<html>Hệ thống quản lý<br>cửa hàng điện máy</html>");
        heading.setForeground(Color.WHITE);
        heading.setFont(UIConstants.font(Font.BOLD,31));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.add(heading);

        hero.add(Box.createVerticalStrut(18));

        JLabel desc = new JLabel(
                "<html><div style='width:300px'>Quản lý toàn diện từ bán hàng, kho hàng, khách hàng "
              + "đến thống kê kinh doanh theo thời gian thực.</div></html>");
        desc.setForeground(new Color(159,184,210));
        desc.setFont(UIConstants.font(Font.PLAIN,15));
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.add(desc);

        hero.add(Box.createVerticalStrut(46));

        JPanel metricWrap = new JPanel(new BorderLayout());
        metricWrap.setOpaque(false);
        metricWrap.setBorder(BorderFactory.createMatteBorder(
                1,0,0,0,new Color(255,255,255,25)));
        metricWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE,110));
        metricWrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel metrics = new JPanel(new GridLayout(1,3,12,0));
        metrics.setOpaque(false);
        metrics.setBorder(new EmptyBorder(25,0,0,0));
        metrics.add(metric("12.500+","Sản phẩm"));
        metrics.add(metric("48.000+","Khách hàng"));
        metrics.add(metric("320+","Nhân viên"));
        metricWrap.add(metrics,BorderLayout.CENTER);
        hero.add(metricWrap);

        hero.add(Box.createVerticalGlue());
        panel.add(hero,BorderLayout.CENTER);

        JLabel footer = new JLabel("© 2025 Điện Máy Blue. Bảo lưu mọi quyền.");
        footer.setForeground(new Color(107,137,168));
        footer.setFont(UIConstants.font(Font.PLAIN,11));
        panel.add(footer,BorderLayout.SOUTH);

        return panel;
    }

    private JPanel metric(String number,String label) {
        JPanel p=new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

        JLabel n=new JLabel(number);
        n.setForeground(UIConstants.ORANGE);
        n.setFont(UIConstants.font(Font.BOLD,24));

        JLabel l=new JLabel(label);
        l.setForeground(new Color(112,151,189));
        l.setFont(UIConstants.font(Font.PLAIN,12));

        p.add(n);
        p.add(Box.createVerticalStrut(3));
        p.add(l);
        return p;
    }

    private JPanel createFormArea() {
        JPanel area=new JPanel(new GridBagLayout());
        area.setBackground(Color.WHITE);
        area.setBorder(new EmptyBorder(24,24,24,24));

        JPanel form=new JPanel();
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(360,610));
        form.setMaximumSize(new Dimension(360,Integer.MAX_VALUE));
        form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));

        sessionBanner.setVisible(false);
        sessionBanner.setLayout(new BorderLayout(10,0));
        sessionBanner.setBorder(new EmptyBorder(12,13,12,13));
        JLabel clock=new JLabel("◷");
        clock.setForeground(new Color(245,158,11));
        clock.setFont(UIConstants.font(Font.BOLD,17));
        sessionTitle.setFont(UIConstants.font(Font.BOLD,12));
        sessionTitle.setForeground(new Color(146,64,14));
        sessionText.setFont(UIConstants.font(Font.PLAIN,11));
        sessionText.setForeground(new Color(180,83,9));
        JPanel sesTxt=new JPanel();
        sesTxt.setOpaque(false);
        sesTxt.setLayout(new BoxLayout(sesTxt,BoxLayout.Y_AXIS));
        sesTxt.add(sessionTitle); sesTxt.add(Box.createVerticalStrut(2)); sesTxt.add(sessionText);
        sessionBanner.add(clock,BorderLayout.WEST);
        sessionBanner.add(sesTxt,BorderLayout.CENTER);
        sessionBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE,74));

        form.add(Box.createVerticalGlue());
        form.add(sessionBanner);
        form.add(Box.createVerticalStrut(18));

        JLabel title=new JLabel("Đăng nhập");
        title.setFont(UIConstants.font(Font.BOLD,26));
        title.setForeground(UIConstants.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(title);

        form.add(Box.createVerticalStrut(5));

        JLabel subtitle=new JLabel("Chào mừng trở lại! Vui lòng đăng nhập để tiếp tục.");
        subtitle.setFont(UIConstants.font(Font.PLAIN,13));
        subtitle.setForeground(UIConstants.MUTED_LIGHT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(subtitle);

        form.add(Box.createVerticalStrut(25));

        JLabel userLabel=label("Tên đăng nhập");
        form.add(userLabel);
        form.add(Box.createVerticalStrut(7));
        form.add(inputShell(txtUsername,"user",null));

        form.add(Box.createVerticalStrut(16));

        JLabel passLabel=label("Mật khẩu");
        form.add(passLabel);
        form.add(Box.createVerticalStrut(7));
        form.add(inputShell(txtPassword,null,this::togglePassword));

        form.add(Box.createVerticalStrut(14));

        errorBox.setVisible(false);
        errorBox.setLayout(new BorderLayout(8,0));
        errorBox.setBorder(new EmptyBorder(10,11,10,11));
        JLabel errorIcon=new JLabel("!");
        errorIcon.setHorizontalAlignment(SwingConstants.CENTER);
        errorIcon.setForeground(Color.WHITE);
        errorIcon.setOpaque(true);
        errorIcon.setBackground(new Color(239,68,68));
        errorIcon.setFont(UIConstants.font(Font.BOLD,11));
        errorIcon.setPreferredSize(new Dimension(18,18));
        lblError.setFont(UIConstants.font(Font.PLAIN,11));
        lblError.setForeground(new Color(220,38,38));
        errorBox.add(errorIcon,BorderLayout.WEST);
        errorBox.add(lblError,BorderLayout.CENTER);
        errorBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,52));
        form.add(errorBox);

        form.add(Box.createVerticalStrut(14));

        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        btnLogin.setPreferredSize(new Dimension(360,44));
        btnLogin.addActionListener(e->controller.dangNhap());
        form.add(btnLogin);

        form.add(Box.createVerticalStrut(16));

        JLabel support=new JLabel(
                "<html><div style='text-align:center'>Gặp sự cố? Liên hệ "
              + "<b><font color='#0f2744'>Bộ phận kỹ thuật</font></b></div></html>",
                SwingConstants.CENTER);
        support.setFont(UIConstants.font(Font.PLAIN,12));
        support.setForeground(UIConstants.MUTED_LIGHT);
        support.setAlignmentX(Component.CENTER_ALIGNMENT);
        support.setMaximumSize(new Dimension(Integer.MAX_VALUE,26));
        form.add(support);

        form.add(Box.createVerticalStrut(22));
        form.add(createDemoPanel());

        form.add(Box.createVerticalGlue());

        GridBagConstraints gbc=new GridBagConstraints();
        gbc.gridx=0; gbc.gridy=0; gbc.weightx=1; gbc.weighty=1;
        gbc.anchor=GridBagConstraints.CENTER;
        area.add(form,gbc);
        return area;
    }

    private JLabel label(String text) {
        JLabel l=new JLabel(text);
        l.setFont(UIConstants.font(Font.BOLD,13));
        l.setForeground(UIConstants.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel inputShell(JTextField field,String iconKind,Runnable trailingAction) {
        final Color normalBorder = new Color(203,213,225);
        RoundedPanel shell=new RoundedPanel(9,UIConstants.INPUT_BG);
        shell.setBorderColor(normalBorder);
        shell.setLayout(new BorderLayout());
        shell.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));
        shell.setPreferredSize(new Dimension(360,46));
        shell.setAlignmentX(Component.LEFT_ALIGNMENT);

        if(iconKind!=null){
            JLabel icon=new JLabel(new AppIcon(iconKind,16,UIConstants.MUTED));
            icon.setBorder(new EmptyBorder(0,12,0,8));
            shell.add(icon,BorderLayout.WEST);
        }

        field.setBorder(new EmptyBorder(0, iconKind==null?12:0,0,0));
        field.setFont(UIConstants.font(Font.PLAIN,14));
        field.setForeground(UIConstants.TEXT);
        field.setCaretColor(UIConstants.TEXT);
        field.setBackground(UIConstants.INPUT_BG);
        field.setOpaque(false);
        field.setPreferredSize(new Dimension(300,44));

        shell.add(field,BorderLayout.CENTER);

        if(trailingAction!=null){
            JButton eye=new JButton(new AppIcon("eye",16,UIConstants.MUTED));
            eye.setPreferredSize(new Dimension(42,44));
            eye.setBorderPainted(false);
            eye.setContentAreaFilled(false);
            eye.setFocusPainted(false);
            eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            eye.addActionListener(e->trailingAction.run());
            shell.add(eye,BorderLayout.EAST);
        }

        field.addFocusListener(new FocusAdapter(){
            @Override public void focusGained(FocusEvent e){
                shell.setBorderColor(UIConstants.ORANGE);
                shell.setFillColor(Color.WHITE);
            }
            @Override public void focusLost(FocusEvent e){
                shell.setBorderColor(normalBorder);
                shell.setFillColor(UIConstants.INPUT_BG);
            }
        });

        field.addActionListener(e->controller.dangNhap());
        field.addKeyListener(new KeyAdapter(){
            @Override public void keyTyped(KeyEvent e){ clearError(); }
        });

        return shell;
    }

    private JPanel createDemoPanel() {
        RoundedPanel panel=new RoundedPanel(12,UIConstants.INPUT_BG);
        panel.setBorderColor(new Color(241,245,249));
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(13,14,13,14));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,205));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title=new JLabel("TÀI KHOẢN MẪU — NHẤN ĐỂ ĐIỀN NHANH", SwingConstants.CENTER);
        title.setFont(UIConstants.font(Font.BOLD,10));
        title.setForeground(UIConstants.MUTED);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
        panel.add(title);
        panel.add(Box.createVerticalStrut(9));

        panel.add(demoRow("Admin","admin","Admin@1234"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(demoRow("Quản lý","admin01","Admin@123"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(demoRow("Bán hàng","sale01","Sale@1234"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(demoRow("CSKH","cskh01","Cskh@1234"));

        return panel;
    }

    private JPanel demoRow(String role,String username,String password) {
        RoundedPanel row=new RoundedPanel(9,Color.WHITE);
        row.setBorderColor(UIConstants.BORDER);
        row.setLayout(new BorderLayout());
        row.setBorder(new EmptyBorder(7,10,7,10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel roleLabel=new JLabel(role);
        roleLabel.setFont(UIConstants.font(Font.BOLD,11));
        roleLabel.setForeground(new Color(51,65,85));

        JLabel userLabel=new JLabel(username);
        userLabel.setFont(new Font("Monospaced",Font.PLAIN,10));
        userLabel.setForeground(UIConstants.MUTED_LIGHT);

        JLabel passwordLabel=new JLabel(password);
        passwordLabel.setFont(new Font("Monospaced",Font.PLAIN,10));
        passwordLabel.setForeground(UIConstants.MUTED_LIGHT);

        JPanel credentials=new JPanel(new GridLayout(1,2,14,0));
        credentials.setOpaque(false);
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        passwordLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        credentials.add(userLabel);
        credentials.add(passwordLabel);

        row.add(roleLabel,BorderLayout.WEST);
        row.add(credentials,BorderLayout.EAST);

        MouseAdapter click=new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e){
                setCredentials(username,password);
            }
            @Override public void mouseEntered(MouseEvent e){
                row.setFillColor(UIConstants.NAVY);
                roleLabel.setForeground(Color.WHITE);
                userLabel.setForeground(new Color(203,213,225));
                passwordLabel.setForeground(new Color(203,213,225));
            }
            @Override public void mouseExited(MouseEvent e){
                row.setFillColor(Color.WHITE);
                roleLabel.setForeground(new Color(51,65,85));
                userLabel.setForeground(UIConstants.MUTED_LIGHT);
                passwordLabel.setForeground(UIConstants.MUTED_LIGHT);
            }
        };
        row.addMouseListener(click);
        roleLabel.addMouseListener(click);
        userLabel.addMouseListener(click);
        return row;
    }

    private void togglePassword(){
        passwordVisible=!passwordVisible;
        txtPassword.setEchoChar(passwordVisible?(char)0:'•');
    }

    public String getTenDangNhap(){ return txtUsername.getText(); }
    public String getPassword(){ return new String(txtPassword.getPassword()); }

    public void clearPassword(){
        txtPassword.setText("");
        txtPassword.requestFocusInWindow();
    }

    public void setCredentials(String username,String password){
        txtUsername.setText(username);
        txtPassword.setText(password);
        clearError();
        txtPassword.requestFocusInWindow();
    }

    public void showError(String message){
        lblError.setText("<html>"+message+"</html>");
        errorBox.setVisible(true);
        revalidate();
        repaint();
    }

    public void clearError(){
        lblError.setText(" ");
        errorBox.setVisible(false);
        revalidate();
        repaint();
    }

    public void showSessionMessage(String message){
        sessionText.setText("<html>"+message+"</html>");
        sessionBanner.setVisible(true);
        revalidate();
        repaint();
    }
}

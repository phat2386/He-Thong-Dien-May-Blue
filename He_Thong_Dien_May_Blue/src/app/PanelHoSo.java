package app;

import bus.DangNhap_BUS;
import entity.TaiKhoan;
import entity.NhanVien;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class PanelHoSo extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final DangNhap_BUS authService;
    private final java.awt.Frame owner;

    public PanelHoSo(
            java.awt.Frame owner,
            DangNhap_BUS authService) {

        this.owner = owner;
        this.authService = authService;

        setLayout(new BorderLayout(0, 22));
        setBackground(UIConstants.BACKGROUND);
        setBorder(
                BorderFactory.createEmptyBorder(
                        28, 28, 28, 28));

        refreshData();
    }

    @Override
    public void refreshData() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    private void buildUI() {
        JLabel title = new JLabel("Hồ sơ cá nhân");
        title.setFont(UIConstants.TITLE);
        title.setForeground(UIConstants.TEXT);
        add(title, BorderLayout.NORTH);

        TaiKhoan account = null;
        String loadError = null;

        try {
            account = authService.layHoSoHienTai();
        } catch (SQLException ex) {
            ex.printStackTrace();
            loadError = "Không thể tải thông tin cá nhân từ cơ sở dữ liệu.";
        }

        NhanVien employee = account == null ? null : account.getNhanVien();

        JPanel card = new JPanel(new GridLayout(0, 2, 16, 16));
        card.setBackground(Color.WHITE);
        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER),
                        BorderFactory.createEmptyBorder(24, 24, 24, 24)));

        if (loadError != null) {
            JLabel error = new JLabel(loadError);
            error.setForeground(new Color(220, 38, 38));
            card.add(error);
            card.add(new JLabel(""));
        } else if (account == null || employee == null) {
            JLabel error = new JLabel("Phiên đăng nhập không còn hiệu lực.");
            error.setForeground(new Color(220, 38, 38));
            card.add(error);
            card.add(new JLabel(""));
        } else {
            addRow(card, "Họ và tên", employee.getHoTen());
            addRow(card, "Mã nhân viên", employee.getMaNhanVien());
            addRow(card, "Tên đăng nhập", account.getTenDangNhap());
            addRow(card, "Vai trò",
                    account.getVaiTro() == null ? "" : account.getVaiTro().getTenVaiTro());
            addRow(card, "Số điện thoại", employee.getSoDienThoai());
            addRow(card, "Thư điện tử",
                    employee.getEmail() == null ? "" : employee.getEmail());
            addRow(card, "Phòng ban",
                    employee.getPhongBan() == null ? "" : employee.getPhongBan());
            addRow(card, "Ngày vào làm",
                    employee.getNgayVaoLam() == null
                            ? ""
                            : employee.getNgayVaoLam().format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            addRow(card, "Trạng thái tài khoản",
                    UiText.vi(account.getTrangThai()));
            addRow(card, "Trạng thái nhân viên",
                    UiText.vi(employee.getTrangThai()));
        }

        add(card, BorderLayout.CENTER);

        JButton btnPassword = new JButton("Đổi mật khẩu");
        btnPassword.setBackground(UIConstants.ORANGE);
        btnPassword.setForeground(Color.WHITE);
        btnPassword.setFocusPainted(false);
        btnPassword.setEnabled(account != null && employee != null && loadError == null);

        btnPassword.addActionListener(e -> {
            UIDoiMatKhau dialog =
                    new UIDoiMatKhau(
                            owner,
                            authService,
                            false,
                            null);
            dialog.setVisible(true);
        });

        JPanel actions = new JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        actions.setOpaque(false);
        actions.add(btnPassword);
        add(actions, BorderLayout.SOUTH);
    }

    private void addRow(
            JPanel panel,
            String label,
            String value) {

        JLabel l = new JLabel(label);
        l.setFont(UIConstants.NORMAL);
        l.setForeground(UIConstants.MUTED);

        JLabel v = new JLabel(value == null ? "" : value);
        v.setFont(UIConstants.NORMAL);
        v.setForeground(UIConstants.TEXT);

        panel.add(l);
        panel.add(v);
    }
}

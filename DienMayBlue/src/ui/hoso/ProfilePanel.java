package ui.hoso;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import entity.Account;
import entity.Employee;
import service.AuthService;
import ui.xacthuc.ChangePasswordDialog;
import ui.dungchung.Refreshable;
import ui.dungchung.UIConstants;
public class ProfilePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;
    private final AuthService authService;
    private final java.awt.Frame owner;
    public ProfilePanel(
            java.awt.Frame owner, AuthService authService) {
        this.owner = owner;
        this.authService = authService;
        setLayout(new BorderLayout(0, 22));
        setBackground(UIConstants.BACKGROUND);
        setBorder( BorderFactory.createEmptyBorder( 28, 28, 28, 28));
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
        Account account = null;
        String loadError = null;
        try {
            account = authService.getCurrentAccountSnapshot();
        } catch (SQLException ex) {
            ex.printStackTrace();
            loadError = "Không thể tải thông tin cá nhân từ cơ sở dữ liệu.";
        }
        Employee employee = account == null ? null : account.getEmployee();
        JPanel card = new JPanel(new GridLayout(0, 2, 16, 16));
        card.setBackground(Color.WHITE);
        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER), BorderFactory.createEmptyBorder(24, 24, 24, 24)));
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
            addRow(card, "Họ và tên", employee.getFullName());
            addRow(card, "Mã nhân viên", employee.getEmployeeId());
            addRow(card, "Tên đăng nhập", account.getUsername());
            addRow(card, "Vai trò", account.getRole() == null ? "" : account.getRole().getRoleName());
            addRow(card, "Số điện thoại", employee.getPhone());
            addRow(card, "Thư điện tử", employee.getEmail() == null ? "" : employee.getEmail());
            addRow(card, "Phòng ban", employee.getDepartment() == null ? "" : employee.getDepartment());
            addRow(card, "Ngày vào làm", employee.getJoinDate() == null ? "" : employee.getJoinDate().format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            addRow(card, "Trạng thái tài khoản", util.Formats.vi(account.getStatus()));
            addRow(card, "Trạng thái nhân viên", util.Formats.vi(employee.getStatus()));
        }
        add(card, BorderLayout.CENTER);
        JButton btnPassword = new JButton("Đổi mật khẩu");
        btnPassword.setBackground(UIConstants.ORANGE);
        btnPassword.setForeground(Color.WHITE);
        btnPassword.setFocusPainted(false);
        btnPassword.setEnabled(account != null && employee != null && loadError == null);
        btnPassword.addActionListener(e -> {
            ChangePasswordDialog dialog = new ChangePasswordDialog( owner, authService, false, null);
            dialog.setVisible(true);
        });
        JPanel actions = new JPanel( new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        actions.setOpaque(false);
        actions.add(btnPassword);
        add(actions, BorderLayout.SOUTH);
    }
    private void addRow(
            JPanel panel, String label, String value) {
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

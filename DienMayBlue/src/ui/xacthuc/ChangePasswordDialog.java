package ui.xacthuc;
import ui.dungchung.DialogUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import service.AuthService;
import ui.dungchung.UIConstants;
public class ChangePasswordDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final JPasswordField txtCurrent = new JPasswordField();
    private final JPasswordField txtNew = new JPasswordField();
    private final JPasswordField txtConfirm = new JPasswordField();
    private final AuthService authService;
    private final boolean firstLoginMode;
    private final Runnable onSuccess;
    public ChangePasswordDialog(
            Frame owner, AuthService authService, boolean firstLoginMode, Runnable onSuccess) {
        super(owner, firstLoginMode ? "Đổi mật khẩu lần đầu" : "Đổi mật khẩu", true);
        this.authService = authService;
        this.firstLoginMode = firstLoginMode;
        this.onSuccess = onSuccess;
        setSize(500, 410);
        setLocationRelativeTo(owner);
        setResizable(false);
        if (firstLoginMode) {
            setDefaultCloseOperation( JDialog.DO_NOTHING_ON_CLOSE);
        }
        buildUI();
    }
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(Color.WHITE);
        root.setBorder( BorderFactory.createEmptyBorder( 24, 28, 24, 28));
        JLabel title = new JLabel( firstLoginMode ? "Thiết lập mật khẩu mới" : "Đổi mật khẩu");
        title.setFont(UIConstants.HEADING);
        title.setForeground(UIConstants.TEXT);
        JLabel hint = new JLabel( "<html>Mật khẩu mới tối thiểu 8 ký tự, " + "có chữ và số, và khác mật khẩu hiện tại.</html>");
        hint.setForeground(UIConstants.MUTED);
        hint.setFont(UIConstants.SMALL);
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout( new javax.swing.BoxLayout( top, javax.swing.BoxLayout.Y_AXIS));
        top.add(title);
        top.add(javax.swing.Box.createVerticalStrut(6));
        top.add(hint);
        JPanel form = new JPanel( new GridLayout(6, 1, 8, 6));
        form.setOpaque(false);
        form.add(new JLabel(firstLoginMode ? "Mật khẩu tạm thời hiện tại" : "Mật khẩu hiện tại"));
        form.add(txtCurrent);
        form.add(new JLabel("Mật khẩu mới"));
        form.add(txtNew);
        form.add(new JLabel("Xác nhận mật khẩu mới"));
        form.add(txtConfirm);
        stylePassword(txtCurrent);
        stylePassword(txtNew);
        stylePassword(txtConfirm);
        JButton btnSave = new JButton("Cập nhật mật khẩu");
        btnSave.setBackground(UIConstants.ORANGE);
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        JButton btnCancel = new JButton(firstLoginMode ? "Quay lại đăng nhập" : "Hủy");
        btnCancel.setFocusPainted(false);
        JPanel actions = new JPanel( new java.awt.FlowLayout( java.awt.FlowLayout.RIGHT));
        actions.setOpaque(false);
        actions.add(btnCancel);
        actions.add(btnSave);
        btnSave.addActionListener(e -> save());
        getRootPane().setDefaultButton(btnSave);
        btnCancel.addActionListener(e -> {
            if (firstLoginMode) {
                authService.logout();
                dispose();
                if (getOwner() instanceof LoginFrame) {
                    ((LoginFrame) getOwner())
                            .showError( "Đăng nhập lần đầu chưa hoàn tất. " + "Bạn cần đổi mật khẩu trước khi sử dụng hệ thống.");
                }
            } else {
                dispose();
            }
        });
        root.add(top, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
    }
    private void stylePassword(JPasswordField field) {
        field.setPreferredSize( new Dimension(420, 38));
        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder( UIConstants.BORDER), BorderFactory.createEmptyBorder(
                                6, 10, 6, 10)));
    }
    private void save() {
        AuthService.Result result = authService.changePassword(
                        new String(txtCurrent.getPassword()), new String(txtNew.getPassword()), new String(txtConfirm.getPassword()));
        if (!result.isSuccess()) {
            JOptionPane.showMessageDialog(
                    DialogUtils.owner(this), result.getMessage(), "Không thể đổi mật khẩu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog( DialogUtils.owner(this), result.getMessage(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
        dispose();
        if (onSuccess != null) {
            onSuccess.run();
        }
    }
}

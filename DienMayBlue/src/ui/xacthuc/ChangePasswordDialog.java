package ui.xacthuc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import service.AuthService;
import ui.dungchung.DialogUtils;
import ui.dungchung.UIConstants;

/**
 * Hộp thoại đổi mật khẩu.
 * - Đăng nhập lần đầu: bắt buộc đổi, không đóng được bằng nút X.
 * - Đổi bình thường (từ màn hình hồ sơ): có thể hủy.
 */
public class ChangePasswordDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JPasswordField txtMatKhauCu = new JPasswordField();
    private final JPasswordField txtMatKhauMoi = new JPasswordField();
    private final JPasswordField txtXacNhan = new JPasswordField();

    private final AuthService authService;
    private final boolean firstLoginMode;
    private final Runnable onSuccess; // việc làm tiếp sau khi đổi xong (có thể null)

    public ChangePasswordDialog(Frame owner, AuthService authService,
            boolean firstLoginMode, Runnable onSuccess) {
        super(owner, firstLoginMode ? "Đổi mật khẩu lần đầu" : "Đổi mật khẩu", true);

        this.authService = authService;
        this.firstLoginMode = firstLoginMode;
        this.onSuccess = onSuccess;

        setSize(500, 410);
        setLocationRelativeTo(owner);
        setResizable(false);

        // Lần đầu thì không cho tắt hộp thoại bằng nút X
        if (firstLoginMode) {
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        }

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        root.add(taoPhanTieuDe(), BorderLayout.NORTH);
        root.add(taoPhanNhapLieu(), BorderLayout.CENTER);
        root.add(taoPhanNut(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    /** Tiêu đề và dòng nhắc quy tắc mật khẩu. */
    private JPanel taoPhanTieuDe() {
        JLabel title = new JLabel(firstLoginMode ? "Thiết lập mật khẩu mới" : "Đổi mật khẩu");
        title.setFont(UIConstants.HEADING);
        title.setForeground(UIConstants.TEXT);

        JLabel hint = new JLabel("<html>Mật khẩu mới tối thiểu 8 ký tự, "
                + "có chữ và số, và khác mật khẩu hiện tại.</html>");
        hint.setForeground(UIConstants.MUTED);
        hint.setFont(UIConstants.SMALL);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);
        top.add(Box.createVerticalStrut(6));
        top.add(hint);
        return top;
    }

    /** Ba ô nhập: mật khẩu hiện tại, mật khẩu mới, xác nhận. */
    private JPanel taoPhanNhapLieu() {
        JPanel form = new JPanel(new GridLayout(6, 1, 8, 6));
        form.setOpaque(false);

        form.add(new JLabel(firstLoginMode ? "Mật khẩu tạm thời hiện tại" : "Mật khẩu hiện tại"));
        form.add(txtMatKhauCu);
        form.add(new JLabel("Mật khẩu mới"));
        form.add(txtMatKhauMoi);
        form.add(new JLabel("Xác nhận mật khẩu mới"));
        form.add(txtXacNhan);

        kieuOMatKhau(txtMatKhauCu);
        kieuOMatKhau(txtMatKhauMoi);
        kieuOMatKhau(txtXacNhan);
        return form;
    }

    /** Hai nút ở dưới: Hủy (hoặc Quay lại đăng nhập) và Cập nhật. */
    private JPanel taoPhanNut() {
        JButton btnLuu = new JButton("Cập nhật mật khẩu");
        btnLuu.setBackground(UIConstants.ORANGE);
        btnLuu.setForeground(Color.WHITE);
        btnLuu.setFocusPainted(false);

        JButton btnHuy = new JButton(firstLoginMode ? "Quay lại đăng nhập" : "Hủy");
        btnHuy.setFocusPainted(false);

        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nut.setOpaque(false);
        nut.add(btnHuy);
        nut.add(btnLuu);

        btnLuu.addActionListener(e -> luuMatKhau());
        btnHuy.addActionListener(e -> huy());
        getRootPane().setDefaultButton(btnLuu);
        return nut;
    }

    /** Bấm Hủy: lần đầu thì đăng xuất và báo ở màn hình đăng nhập; còn lại chỉ đóng. */
    private void huy() {
        if (!firstLoginMode) {
            dispose();
            return;
        }

        authService.logout();
        dispose();

        if (getOwner() instanceof LoginFrame) {
            LoginFrame manHinhDangNhap = (LoginFrame) getOwner();
            manHinhDangNhap.showError("Đăng nhập lần đầu chưa hoàn tất. "
                    + "Bạn cần đổi mật khẩu trước khi sử dụng hệ thống.");
        }
    }

    private void kieuOMatKhau(JPasswordField field) {
        field.setPreferredSize(new Dimension(420, 38));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private void luuMatKhau() {
        String matKhauCu = new String(txtMatKhauCu.getPassword());
        String matKhauMoi = new String(txtMatKhauMoi.getPassword());
        String xacNhan = new String(txtXacNhan.getPassword());

        AuthService.Result ketQua = authService.changePassword(matKhauCu, matKhauMoi, xacNhan);

        if (!ketQua.isSuccess()) {
            JOptionPane.showMessageDialog(DialogUtils.owner(this), ketQua.getMessage(),
                    "Không thể đổi mật khẩu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(DialogUtils.owner(this), ketQua.getMessage(),
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        dispose();

        if (onSuccess != null) {
            onSuccess.run();
        }
    }
}

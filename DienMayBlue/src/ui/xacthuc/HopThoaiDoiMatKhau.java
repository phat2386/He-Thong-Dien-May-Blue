package ui.xacthuc;
import java.awt.*;
import javax.swing.*;
import service.TaiKhoanService;
import ui.dungchung.*;
public class HopThoaiDoiMatKhau extends JDialog {
    private static final long serialVersionUID = 1L;
    private final JPasswordField txtMatKhauCu = new JPasswordField();
    private final JPasswordField txtMatKhauMoi = new JPasswordField();
    private final JPasswordField txtXacNhan = new JPasswordField();
    private final TaiKhoanService dichVuXacThuc;
    private final Runnable onSuccess;
    public HopThoaiDoiMatKhau(Frame chuSoHuu, TaiKhoanService dichVuXacThuc, Runnable onSuccess) {
        super(chuSoHuu, "Đổi mật khẩu", true);
        this.dichVuXacThuc = dichVuXacThuc;
        this.onSuccess = onSuccess;
        setSize(500, 410);
        setLocationRelativeTo(chuSoHuu);
        setResizable(false);
        taoGiaoDien();
    }
    public HopThoaiDoiMatKhau(Frame chuSoHuu, TaiKhoanService dichVuXacThuc, boolean boQuaCheDoCu, Runnable onSuccess) {
        this(chuSoHuu, dichVuXacThuc, onSuccess);
    }
    private void taoGiaoDien() {
        JPanel pnlGoc = new JPanel(new BorderLayout(16, 16));
        pnlGoc.setBackground(Color.WHITE);
        pnlGoc.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        pnlGoc.add(taoPhanTieuDe(), BorderLayout.NORTH);
        pnlGoc.add(taoPhanNhapLieu(), BorderLayout.CENTER);
        pnlGoc.add(taoPhanNut(), BorderLayout.SOUTH);
        setContentPane(pnlGoc);
    }
    private JPanel taoPhanTieuDe() {
        JLabel lblTieuDe = new JLabel("Đổi mật khẩu");
        lblTieuDe.setFont(HangSoGiaoDien.HEADING);
        lblTieuDe.setForeground(HangSoGiaoDien.TEXT);
        JLabel lblGoiY = new JLabel("<html>Mật khẩu mới tối thiểu 8 ký tự, " + "có chữ và số, và khác mật khẩu hiện tại.</html>");
        lblGoiY.setForeground(HangSoGiaoDien.MUTED);
        lblGoiY.setFont(HangSoGiaoDien.SMALL);
        JPanel pnlTren = new JPanel();
        pnlTren.setOpaque(false);
        pnlTren.setLayout(new BoxLayout(pnlTren, BoxLayout.Y_AXIS));
        pnlTren.add(lblTieuDe);
        pnlTren.add(Box.createVerticalStrut(6));
        pnlTren.add(lblGoiY);
        return pnlTren;
    }
    private JPanel taoPhanNhapLieu() {
        JPanel pnlBieuMau = new JPanel(new GridLayout(6, 1, 8, 6));
        pnlBieuMau.setOpaque(false);
        pnlBieuMau.add(new JLabel("Mật khẩu hiện tại"));
        pnlBieuMau.add(txtMatKhauCu);
        pnlBieuMau.add(new JLabel("Mật khẩu mới"));
        pnlBieuMau.add(txtMatKhauMoi);
        pnlBieuMau.add(new JLabel("Xác nhận mật khẩu mới"));
        pnlBieuMau.add(txtXacNhan);
        kieuOMatKhau(txtMatKhauCu);
        kieuOMatKhau(txtMatKhauMoi);
        kieuOMatKhau(txtXacNhan);
        return pnlBieuMau;
    }
    private JPanel taoPhanNut() {
        JButton btnLuu = new JButton("Cập nhật mật khẩu");
        btnLuu.setBackground(HangSoGiaoDien.ORANGE);
        btnLuu.setForeground(Color.WHITE);
        btnLuu.setFocusPainted(false);
        JButton btnHuy = new JButton("Hủy");
        btnHuy.setFocusPainted(false);
        JPanel pnlNut = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlNut.setOpaque(false);
        pnlNut.add(btnHuy);
        pnlNut.add(btnLuu);
        btnLuu.addActionListener(e -> luuMatKhau());
        btnHuy.addActionListener(e -> huy());
        getRootPane().setDefaultButton(btnLuu);
        return pnlNut;
    }
    private void huy() {
        dispose();
    }
    private void kieuOMatKhau(JPasswordField txtMatKhau) {
        txtMatKhau.setPreferredSize(new Dimension(420, 38));
        txtMatKhau.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }
    private void luuMatKhau() {
        String matKhauCu = new String(txtMatKhauCu.getPassword());
        String matKhauMoi = new String(txtMatKhauMoi.getPassword());
        String xacNhan = new String(txtXacNhan.getPassword());
        TaiKhoanService.KetQua ketQua = dichVuXacThuc.doiMatKhau(matKhauCu, matKhauMoi, xacNhan);
        if (!ketQua.isThanhCong()) {
            JOptionPane.showMessageDialog(TienIchHopThoai.chuSoHuu(this), ketQua.getThongBao(), "Không thể đổi mật khẩu", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(TienIchHopThoai.chuSoHuu(this), ketQua.getThongBao(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
        dispose();
        if (onSuccess != null) {
            onSuccess.run();
        }
    }
}
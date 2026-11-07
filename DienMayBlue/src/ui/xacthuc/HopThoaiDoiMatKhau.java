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
        JPanel khungGoc = new JPanel(new BorderLayout(16, 16));
        khungGoc.setBackground(Color.WHITE);
        khungGoc.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        khungGoc.add(taoPhanTieuDe(), BorderLayout.NORTH);
        khungGoc.add(taoPhanNhapLieu(), BorderLayout.CENTER);
        khungGoc.add(taoPhanNut(), BorderLayout.SOUTH);
        setContentPane(khungGoc);
    }
    private JPanel taoPhanTieuDe() {
        JLabel tieuDe = new JLabel("Đổi mật khẩu");
        tieuDe.setFont(HangSoGiaoDien.HEADING);
        tieuDe.setForeground(HangSoGiaoDien.TEXT);
        JLabel goiY = new JLabel("<html>Mật khẩu mới tối thiểu 8 ký tự, " + "có chữ và số, và khác mật khẩu hiện tại.</html>");
        goiY.setForeground(HangSoGiaoDien.MUTED);
        goiY.setFont(HangSoGiaoDien.SMALL);
        JPanel khungTren = new JPanel();
        khungTren.setOpaque(false);
        khungTren.setLayout(new BoxLayout(khungTren, BoxLayout.Y_AXIS));
        khungTren.add(tieuDe);
        khungTren.add(Box.createVerticalStrut(6));
        khungTren.add(goiY);
        return khungTren;
    }
    private JPanel taoPhanNhapLieu() {
        JPanel bieuMau = new JPanel(new GridLayout(6, 1, 8, 6));
        bieuMau.setOpaque(false);
        bieuMau.add(new JLabel("Mật khẩu hiện tại"));
        bieuMau.add(txtMatKhauCu);
        bieuMau.add(new JLabel("Mật khẩu mới"));
        bieuMau.add(txtMatKhauMoi);
        bieuMau.add(new JLabel("Xác nhận mật khẩu mới"));
        bieuMau.add(txtXacNhan);
        kieuOMatKhau(txtMatKhauCu);
        kieuOMatKhau(txtMatKhauMoi);
        kieuOMatKhau(txtXacNhan);
        return bieuMau;
    }
    private JPanel taoPhanNut() {
        JButton btnLuu = new JButton("Cập nhật mật khẩu");
        btnLuu.setBackground(HangSoGiaoDien.ORANGE);
        btnLuu.setForeground(Color.WHITE);
        btnLuu.setFocusPainted(false);
        JButton btnHuy = new JButton("Hủy");
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
    private void huy() {
        dispose();
    }
    private void kieuOMatKhau(JPasswordField oNhap) {
        oNhap.setPreferredSize(new Dimension(420, 38));
        oNhap.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
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

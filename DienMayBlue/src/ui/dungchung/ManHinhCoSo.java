package ui.dungchung;
import java.awt.*;
import javax.swing.*;
public abstract class ManHinhCoSo extends JPanel implements CoTheLamMoi {
    private static final long serialVersionUID = 1L;
    protected final JPanel noiDung = new JPanel(new BorderLayout(0, 14));
    protected final JPanel thaoTac = new JPanel(new BoCucXuongDong(FlowLayout.LEFT, 8, 4));
    protected final JLabel lblTieuDe = new JLabel();
    protected final JLabel lblTieuDePhu = new JLabel();
    protected ManHinhCoSo(String tieuDe, String subtitle) {
        setLayout(new BorderLayout(0, 12));
        setBackground(HangSoGiaoDien.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        JPanel vanBan = new JPanel();
        vanBan.setOpaque(false);
        vanBan.setLayout(new BoxLayout(vanBan, BoxLayout.Y_AXIS));
        lblTieuDe.setText(tieuDe);
        lblTieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 20));
        lblTieuDe.setForeground(HangSoGiaoDien.TEXT);
        lblTieuDePhu.setText(subtitle);
        lblTieuDePhu.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 12));
        lblTieuDePhu.setForeground(HangSoGiaoDien.MUTED);
        vanBan.add(lblTieuDe);
        vanBan.add(Box.createVerticalStrut(3));
        vanBan.add(lblTieuDePhu);
        thaoTac.setOpaque(false);
        header.add(vanBan, BorderLayout.NORTH);
        header.add(thaoTac, BorderLayout.CENTER);
        noiDung.setOpaque(false);
        add(header, BorderLayout.NORTH);
        add(noiDung, BorderLayout.CENTER);
    }
    protected void hienLoi(Exception ex) {
        // Lỗi nghiệp vụ / nhập liệu (IllegalArgumentException, IllegalStateException không có nguyên nhân gốc)
        // chỉ cần báo cho người dùng; lỗi hệ thống thật (SQL, IO...) mới in chi tiết ra console để lập trình viên xem.
        boolean loiNghiepVu = (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) && ex.getCause() == null;
        if (!loiNghiepVu) ex.printStackTrace();
        String thongBao = ex.getMessage() == null || ex.getMessage().isBlank() ? "Đã xảy ra lỗi không xác định." : ex.getMessage();
        int loai = loiNghiepVu ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE;
        JOptionPane.showMessageDialog(TienIchHopThoai.chuSoHuu(this), thongBao, loiNghiepVu ? "Thông báo" : "Lỗi", loai);
    }
    protected void thongBao(String msg) {
        JOptionPane.showMessageDialog(TienIchHopThoai.chuSoHuu(this), msg, "Điện Máy Blue", JOptionPane.INFORMATION_MESSAGE);
    }
    protected boolean xacNhan(String msg) {
        return TienIchHopThoai.xacNhanHanhDong(this, msg, "Xác nhận", "Xác nhận");
    }
}

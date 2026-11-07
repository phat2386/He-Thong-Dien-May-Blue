package ui.chinh;
import java.awt.*;
import javax.swing.*;
import ui.dungchung.*;
public class ThanhTren extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel lblBreadcrumb = new JLabel("Điện Máy Blue");
    private final JLabel lblManHinh = new JLabel("Trang chủ");
    private final JButton btnQuayLai;
    private final JButton btnQuayToi;
    public ThanhTren(Runnable hanhDongQuayLai, Runnable hanhDongQuayToi) {
        setPreferredSize(new Dimension(100, 56));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)));
        setLayout(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 18));
        left.setOpaque(false);
        lblBreadcrumb.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 12));
        lblBreadcrumb.setForeground(HangSoGiaoDien.MUTED_LIGHT);
        JLabel slash = new JLabel("/");
        slash.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 12));
        slash.setForeground(new Color(203, 213, 225));
        lblManHinh.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 12));
        lblManHinh.setForeground(HangSoGiaoDien.TEXT);
        left.add(lblBreadcrumb);
        left.add(slash);
        left.add(lblManHinh);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        btnQuayLai = taoNutDieuHuong("Quay lại", "arrow-left");
        btnQuayToi = taoNutDieuHuong("Quay tới", "arrow-right");
        btnQuayLai.addActionListener(e -> {
            if (hanhDongQuayLai != null) hanhDongQuayLai.run();
        });
        btnQuayToi.addActionListener(e -> {
            if (hanhDongQuayToi != null) hanhDongQuayToi.run();
        });
        right.add(btnQuayLai);
        right.add(btnQuayToi);
        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
        setDieuHuongChoPhep(false, false);
    }
    private JButton taoNutDieuHuong(String vanBan, String iconKind) {
        JButton b = new JButton(vanBan, new BieuTuongUngDung(iconKind, 14, HangSoGiaoDien.TEXT));
        b.setPreferredSize(new Dimension(104, 35));
        b.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 11));
        b.setForeground(HangSoGiaoDien.TEXT);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true), BorderFactory.createEmptyBorder(0, 8, 0, 8)));
        b.setIconTextGap(7);
        return b;
    }
    public void setTieuDeManHinh(String tieuDe) {
        lblManHinh.setText(tieuDe);
    }
    public void setDieuHuongChoPhep(boolean canBack, boolean canForward) {
        btnQuayLai.setEnabled(canBack);
        btnQuayToi.setEnabled(canForward);
        btnQuayLai.setForeground(canBack ? HangSoGiaoDien.TEXT : HangSoGiaoDien.MUTED_LIGHT);
        btnQuayToi.setForeground(canForward ? HangSoGiaoDien.TEXT : HangSoGiaoDien.MUTED_LIGHT);
        btnQuayLai.setIcon(new BieuTuongUngDung("arrow-left", 14, canBack ? HangSoGiaoDien.TEXT : HangSoGiaoDien.MUTED_LIGHT));
        btnQuayToi.setIcon(new BieuTuongUngDung("arrow-right", 14, canForward ? HangSoGiaoDien.TEXT : HangSoGiaoDien.MUTED_LIGHT));
        btnQuayLai.setCursor(canBack ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        btnQuayToi.setCursor(canForward ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }
}

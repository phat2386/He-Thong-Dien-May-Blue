package ui.dungchung;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
public class NutDieuHuong extends JButton {
    private static final long serialVersionUID = 1L;
    private final String fullText;
    private final String iconKind;
    private boolean hoatDong;
    private boolean collapsed;
    public NutDieuHuong(String vanBan) {
        this(vanBan, null);
    }
    public NutDieuHuong(String vanBan, String iconKind) {
        super(vanBan);
        this.fullText = vanBan;
        this.iconKind = iconKind;
        setHorizontalAlignment(SwingConstants.LEFT);
        setPreferredSize(new Dimension(196, 38));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(new Color(189, 204, 220));
        setFont(HangSoGiaoDien.phongChu(Font.BOLD, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 10, 0, 10));
        setIconTextGap(10);
        if (iconKind != null) setIcon(new BieuTuongUngDung(iconKind, 17, new Color(189, 204, 220)));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!hoatDong) {
                    setForeground(Color.WHITE);
                    capNhatBieuTuong(Color.WHITE);
                    repaint();
                }
            }
            public void mouseExited(MouseEvent e) {
                if (!hoatDong) {
                    setForeground(new Color(189, 204, 220));
                    capNhatBieuTuong(new Color(189, 204, 220));
                    repaint();
                }
            }
        });
    }
    public void setDangChon(boolean hoatDong) {
        this.hoatDong = hoatDong;
        setForeground(hoatDong ? Color.WHITE : new Color(189, 204, 220));
        capNhatBieuTuong(hoatDong ? Color.WHITE : new Color(189, 204, 220));
        repaint();
    }
    public void setThuGon(boolean collapsed) {
        this.collapsed = collapsed;
        setText(collapsed ? "" : fullText);
        setToolTipText(collapsed ? fullText : null);
        setHorizontalAlignment(collapsed ? SwingConstants.CENTER : SwingConstants.LEFT);
        setMargin(collapsed ? new Insets(0, 0, 0, 0) : new Insets(0, 10, 0, 10));
        setIconTextGap(collapsed ? 0 : 10);
        setMaximumSize(new Dimension(collapsed ? 40 : Integer.MAX_VALUE, 38));
        setPreferredSize(new Dimension(collapsed ? 40 : 196, 38));
        revalidate();
        repaint();
    }
    private void capNhatBieuTuong(Color c) {
        if (iconKind != null) setIcon(new BieuTuongUngDung(iconKind, 17, c));
    }
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (hoatDong) {
            g2.setColor(HangSoGiaoDien.ORANGE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
        } else if (getModel().isRollover()) {
            g2.setColor(new Color(255, 255, 255, 24));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

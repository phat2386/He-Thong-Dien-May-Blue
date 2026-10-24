package ui.dungchung;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
public class RoundedButton extends JButton {
    private static final long serialVersionUID = 1L;
    private final int radius;
    private Color fill;
    private Color hoverFill;
    private Color disabledFill = new Color(255, 237, 213);
    private Color disabledText = new Color(154, 52, 18);
    private boolean hovered;
    public RoundedButton(String text, Color fill, Color hoverFill, int radius) {
        super(text);
        this.fill = fill;
        this.hoverFill = hoverFill;
        this.radius = radius;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(Color.WHITE);
        setFont(UIConstants.font(Font.BOLD, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }
    public void setDisabledColors(Color bg, Color fg) {
        disabledFill = bg;
        disabledText = fg;
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = isEnabled() ? (hovered ? hoverFill : fill) : disabledFill;
        g2.setColor(bg);
        g2.fillRoundRect(0,0,getWidth(),getHeight(),radius,radius);
        g2.dispose();
        Color old = getForeground();
        setForeground(isEnabled() ? old : disabledText);
        super.paintComponent(g);
        if (!isEnabled()) setForeground(old);
    }
}

package ui.dungchung;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
public class OutlineButton extends JButton {
    private static final long serialVersionUID = 1L;
    private final int radius;
    private Color fill = Color.WHITE;
    private Color hoverFill = new Color(248, 250, 252);
    private Color border = UIConstants.BORDER;
    private Color text = UIConstants.TEXT;
    private Color disabledFill = new Color(248, 250, 252);
    private Color disabledBorder = new Color(226, 232, 240);
    private Color disabledText = new Color(148, 163, 184);
    private boolean hovered;
    public OutlineButton(String label, int radius) {
        super(label);
        this.radius = radius;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(UIConstants.font(Font.BOLD, 13));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }
    public void setColors(Color fill, Color hoverFill, Color border, Color text) {
        this.fill = fill;
        this.hoverFill = hoverFill;
        this.border = border;
        this.text = text;
        repaint();
    }
    public void setDisabledColors(Color fill, Color border, Color text) {
        this.disabledFill = fill;
        this.disabledBorder = border;
        this.disabledText = text;
        repaint();
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = isEnabled() ? (hovered ? hoverFill : fill) : disabledFill;
        Color bd = isEnabled() ? border : disabledBorder;
        Color fg = isEnabled() ? text : disabledText;
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.setColor(bd);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.dispose();
        Color old = getForeground();
        setForeground(fg);
        super.paintComponent(g);
        setForeground(old);
    }
}

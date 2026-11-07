package ui.dungchung;
import java.awt.*;
import javax.swing.JPanel;
public class KhungBoGoc extends JPanel {
    private static final long serialVersionUID = 1L;
    private final int radius;
    private Color fillColor;
    private Color borderColor = HangSoGiaoDien.BORDER;
    private boolean drawBorder = true;
    public KhungBoGoc(int radius, Color fillColor) {
        this.radius = radius;
        this.fillColor = fillColor;
        setOpaque(false);
    }
    public void setMauNen(Color fillColor) {
        this.fillColor = fillColor;
        repaint();
    }
    public void setMauVien(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }
    public void setVeVien(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fillColor);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        if (drawBorder) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

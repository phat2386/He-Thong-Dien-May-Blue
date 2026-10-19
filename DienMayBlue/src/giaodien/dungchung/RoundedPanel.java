package giaodien.dungchung;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

public class RoundedPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final int radius;
    private Color fillColor;
    private Color borderColor = UIConstants.BORDER;
    private boolean drawBorder = true;

    public RoundedPanel(int radius, Color fillColor) {
        this.radius = radius;
        this.fillColor = fillColor;
        setOpaque(false);
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(fillColor);
        g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);

        if(drawBorder){
            g2.setColor(borderColor);
            g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}

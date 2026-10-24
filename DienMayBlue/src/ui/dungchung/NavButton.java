package ui.dungchung;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
public class NavButton extends JButton {
    private static final long serialVersionUID=1L;
    private final String fullText;
    private final String iconKind;
    private boolean active;
    private boolean collapsed;
    public NavButton(String text){ this(text,null); }
    public NavButton(String text,String iconKind){
        super(text);
        this.fullText=text;
        this.iconKind=iconKind;
        setHorizontalAlignment(SwingConstants.LEFT);
        setPreferredSize(new Dimension(196,38));
        setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(new Color(189,204,220));
        setFont(UIConstants.font(Font.BOLD,12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0,10,0,10));
        setIconTextGap(10);
        if(iconKind!=null)
            setIcon(new AppIcon(iconKind,17,new Color(189,204,220)));
        addMouseListener(new MouseAdapter(){
            public void mouseEntered(MouseEvent e){ if(!active){setForeground(Color.WHITE); updateIcon(Color.WHITE); repaint();}}
            public void mouseExited(MouseEvent e){ if(!active){setForeground(new Color(189,204,220)); updateIcon(new Color(189,204,220)); repaint();}}
        });
    }
    public void setActive(boolean active){
        this.active=active;
        setForeground(active?Color.WHITE:new Color(189,204,220));
        updateIcon(active?Color.WHITE:new Color(189,204,220));
        repaint();
    }
    public void setCollapsed(boolean collapsed){
        this.collapsed=collapsed;
        setText(collapsed?"":fullText);
        setToolTipText(collapsed?fullText:null);
        setHorizontalAlignment(collapsed?SwingConstants.CENTER:SwingConstants.LEFT);
        setMargin(collapsed?new Insets(0,0,0,0):new Insets(0,10,0,10));
        setIconTextGap(collapsed?0:10);
        setMaximumSize(new Dimension(collapsed?40:Integer.MAX_VALUE,38));
        setPreferredSize(new Dimension(collapsed?40:196,38));
        revalidate();repaint();
    }
    private void updateIcon(Color c){
        if(iconKind!=null)setIcon(new AppIcon(iconKind,17,c));
    }
    protected void paintComponent(Graphics g){
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        if(active){
            g2.setColor(UIConstants.ORANGE);
            g2.fillRoundRect(0,0,getWidth(),getHeight(),9,9);
        }else if(getModel().isRollover()){
            g2.setColor(new Color(255,255,255,24));
            g2.fillRoundRect(0,0,getWidth(),getHeight(),9,9);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

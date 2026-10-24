package ui.dungchung;
import java.awt.*;
import javax.swing.Icon;
public class AppIcon implements Icon {
    private final String kind;
    private final int size;
    private final Color color;
    public AppIcon(String kind, int size, Color color) {
        this.kind = kind;
        this.size = size;
        this.color = color;
    }
    public int getIconWidth(){ return size; }
    public int getIconHeight(){ return size; }
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D a=(Graphics2D)g.create();
        a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        a.setColor(color);
        a.setStroke(new BasicStroke(Math.max(1.4f,size/12f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        int s=size, x0=x, y0=y;
        switch(kind){
            case "user": a.drawOval(x0+s/3,y0+1,s/3,s/3);
                a.drawArc(x0+s/5,y0+s/2,s*3/5,s/2,0,180);
                break;
            case "search": a.drawOval(x0+2,y0+2,s*2/3,s*2/3);
                a.drawLine(x0+s*2/3,y0+s*2/3,x0+s-2,y0+s-2);
                break;
            case "eye": a.drawArc(x0+1,y0+s/4,s-2,s/2,0,180);
                a.drawArc(x0+1,y0+s/4,s-2,s/2,180,180);
                a.fillOval(x0+s/2-2,y0+s/2-2,4,4);
                break;
            case "dashboard": a.drawRect(x0+1,y0+1,s/2-3,s/2-3);
                a.drawRect(x0+s/2+2,y0+1,s/2-3,s/2-3);
                a.drawRect(x0+1,y0+s/2+2,s/2-3,s/2-3);
                a.drawRect(x0+s/2+2,y0+s/2+2,s/2-3,s/2-3);
                break;
            case "cart": a.drawLine(x0+1,y0+2,x0+4,y0+2);
                a.drawLine(x0+4,y0+2,x0+6,y0+s-5);
                a.drawLine(x0+6,y0+s-5,x0+s-3,y0+s-5);
                a.drawLine(x0+7,y0+5,x0+s-2,y0+5);
                a.drawLine(x0+s-2,y0+5,x0+s-4,y0+s-7);
                a.fillOval(x0+7,y0+s-3,3,3); a.fillOval(x0+s-6,y0+s-3,3,3);
                break;
            case "box": a.drawRect(x0+2,y0+4,s-4,s-6); a.drawLine(x0+2,y0+4,x0+s/2,y0+1); a.drawLine(x0+s-2,y0+4,x0+s/2,y0+1);
                break;
            case "users": a.drawOval(x0+2,y0+1,s/3,s/3); a.drawOval(x0+s/2,y0+2,s/3,s/3);
                a.drawArc(x0,y0+s/2,s*2/3,s/2,0,180); a.drawArc(x0+s/3,y0+s/2,s*2/3,s/2,0,180);
                break;
            case "tag": int[] xs={x0+2,x0+s/2,x0+s-2,x0+s/2,x0+2}; int[] ys={y0+2,y0+2,y0+s/2,y0+s-2,y0+s-2};
                a.drawPolygon(xs,ys,5); a.fillOval(x0+s/3,y0+s/3,3,3); break;
            case "truck": a.drawRect(x0+1,y0+4,s*2/3,s/2); a.drawRect(x0+s*2/3,y0+s/3,s/3-2,s/3);
                a.fillOval(x0+3,y0+s-5,4,4); a.fillOval(x0+s-7,y0+s-5,4,4); break;
            case "chart": a.drawLine(x0+2,y0+s-2,x0+2,y0+2); a.drawLine(x0+2,y0+s-2,x0+s-2,y0+s-2);
                a.drawLine(x0+5,y0+s-5,x0+8,y0+s/2); a.drawLine(x0+8,y0+s/2,x0+12,y0+s/3); a.drawLine(x0+12,y0+s/3,x0+s-2,y0+3); break;
            case "headset":
                a.drawArc(x0+2,y0+2,s-4,s-4,0,180); a.drawLine(x0+2,y0+s/2,x0+2,y0+s-4); a.drawLine(x0+s-2,y0+s/2,x0+s-2,y0+s-4); break;
            case "building":
                a.drawRect(x0+3,y0+2,s-6,s-4); for(int i=0;i<2;i++)for(int j=0;j<3;j++)a.drawRect(x0+6+i*6,y0+5+j*5,2,2); break;
            case "shield":
                a.drawArc(x0+3,y0+1,s-6,s/2,0,180); a.drawLine(x0+3,y0+s/4,x0+s/2,y0+s-2); a.drawLine(x0+s-3,y0+s/4,x0+s/2,y0+s-2); break;
            case "calendar":
                a.drawRect(x0+2,y0+4,s-4,s-6); a.drawLine(x0+2,y0+8,x0+s-2,y0+8); a.drawLine(x0+5,y0+1,x0+5,y0+6); a.drawLine(x0+s-5,y0+1,x0+s-5,y0+6); break;
            case "money": a.drawRoundRect(x0+1,y0+3,s-2,s-6,4,4); a.drawOval(x0+s/2-3,y0+s/2-3,6,6); break;
            case "logout":
                a.drawRect(x0+1,y0+2,s/2,s-4); a.drawLine(x0+s/2-1,y0+s/2,x0+s-2,y0+s/2); a.drawLine(x0+s-5,y0+s/2-3,x0+s-2,y0+s/2); a.drawLine(x0+s-5,y0+s/2+3,x0+s-2,y0+s/2); break;
            case "arrow-left": a.drawLine(x0+s-2,y0+s/2,x0+2,y0+s/2);
                a.drawLine(x0+2,y0+s/2,x0+6,y0+s/2-4);
                a.drawLine(x0+2,y0+s/2,x0+6,y0+s/2+4);
                break;
            case "arrow-right": a.drawLine(x0+2,y0+s/2,x0+s-2,y0+s/2);
                a.drawLine(x0+s-2,y0+s/2,x0+s-6,y0+s/2-4);
                a.drawLine(x0+s-2,y0+s/2,x0+s-6,y0+s/2+4);
                break;
            default: a.drawOval(x0+2,y0+2,s-4,s-4);
        }
        a.dispose();
    }
}

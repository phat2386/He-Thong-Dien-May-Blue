package ui.chinh;
import java.awt.*;
import javax.swing.*;
import ui.dungchung.*;
public class TopBarPanel extends JPanel {
    private static final long serialVersionUID=1L;
    private final JLabel lblBreadcrumb=new JLabel("Điện Máy Blue");
    private final JLabel lblScreen=new JLabel("Trang chủ");
    private final JButton btnBack;
    private final JButton btnForward;
    public TopBarPanel(Runnable backAction, Runnable forwardAction){
        setPreferredSize(new Dimension(100,56));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(241,245,249)));
        setLayout(new BorderLayout());
        JPanel left=new JPanel(new FlowLayout(FlowLayout.LEFT,8,18));
        left.setOpaque(false);
        lblBreadcrumb.setFont(UIConstants.font(Font.PLAIN,12));
        lblBreadcrumb.setForeground(UIConstants.MUTED_LIGHT);
        JLabel slash=new JLabel("/");
        slash.setFont(UIConstants.font(Font.PLAIN,12));
        slash.setForeground(new Color(203,213,225));
        lblScreen.setFont(UIConstants.font(Font.BOLD,12));
        lblScreen.setForeground(UIConstants.TEXT);
        left.add(lblBreadcrumb);
        left.add(slash);
        left.add(lblScreen);
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,10));
        right.setOpaque(false);
        right.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        btnBack=createNavButton("Quay lại", "arrow-left");
        btnForward=createNavButton("Quay tới", "arrow-right");
        btnBack.addActionListener(e -> {
            if(backAction!=null) backAction.run();
        });
        btnForward.addActionListener(e -> {
            if(forwardAction!=null) forwardAction.run();
        });
        right.add(btnBack);
        right.add(btnForward);
        add(left,BorderLayout.WEST);
        add(right,BorderLayout.EAST);
        setNavigationEnabled(false,false);
    }
    private JButton createNavButton(String text,String iconKind){
        JButton b=new JButton(text,new AppIcon(iconKind,14,UIConstants.TEXT));
        b.setPreferredSize(new Dimension(104,35));
        b.setFont(UIConstants.font(Font.BOLD,11));
        b.setForeground(UIConstants.TEXT);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226,232,240),1,true), BorderFactory.createEmptyBorder(0,8,0,8)));
        b.setIconTextGap(7);
        return b;
    }
    public void setScreenTitle(String title){
        lblScreen.setText(title);
    }
    public void setNavigationEnabled(boolean canBack,boolean canForward){
        btnBack.setEnabled(canBack);
        btnForward.setEnabled(canForward);
        btnBack.setForeground(canBack?UIConstants.TEXT:UIConstants.MUTED_LIGHT);
        btnForward.setForeground(canForward?UIConstants.TEXT:UIConstants.MUTED_LIGHT);
        btnBack.setIcon(new AppIcon("arrow-left",14,canBack?UIConstants.TEXT:UIConstants.MUTED_LIGHT));
        btnForward.setIcon(new AppIcon("arrow-right",14,canForward?UIConstants.TEXT:UIConstants.MUTED_LIGHT));
        btnBack.setCursor(canBack?Cursor.getPredefinedCursor(Cursor.HAND_CURSOR):Cursor.getDefaultCursor());
        btnForward.setCursor(canForward?Cursor.getPredefinedCursor(Cursor.HAND_CURSOR):Cursor.getDefaultCursor());
    }
}

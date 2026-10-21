package app;

import java.awt.*;
import javax.swing.*;

public abstract class PanelBaseScreen extends JPanel implements Refreshable {
    private static final long serialVersionUID=1L;

    protected final JPanel body=new JPanel(new BorderLayout(0,14));
    protected final JPanel actions=new JPanel(new WrapLayout(FlowLayout.LEFT,8,4));
    protected final JLabel titleLabel=new JLabel();
    protected final JLabel subtitleLabel=new JLabel();

    protected PanelBaseScreen(String title,String subtitle){
        setLayout(new BorderLayout(0,12));
        setBackground(UIConstants.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        JPanel header=new JPanel(new BorderLayout(0,8));
        header.setOpaque(false);

        JPanel text=new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text,BoxLayout.Y_AXIS));

        titleLabel.setText(title);
        titleLabel.setFont(UIConstants.font(Font.BOLD,20));
        titleLabel.setForeground(UIConstants.TEXT);

        subtitleLabel.setText(subtitle);
        subtitleLabel.setFont(UIConstants.font(Font.PLAIN,12));
        subtitleLabel.setForeground(UIConstants.MUTED);

        text.add(titleLabel);
        text.add(Box.createVerticalStrut(3));
        text.add(subtitleLabel);

        actions.setOpaque(false);

        header.add(text,BorderLayout.NORTH);
        header.add(actions,BorderLayout.CENTER);

        body.setOpaque(false);

        add(header,BorderLayout.NORTH);
        add(body,BorderLayout.CENTER);
    }

    protected void showError(Exception ex){
        ex.printStackTrace();
        JOptionPane.showMessageDialog(DialogUtils.owner(this),ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);
    }

    protected void info(String msg){
        JOptionPane.showMessageDialog(DialogUtils.owner(this),msg,"Điện Máy Blue",JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean confirm(String msg){
        return JOptionPane.showConfirmDialog(DialogUtils.owner(this),msg,"Xác nhận",
                JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)==JOptionPane.YES_OPTION;
    }
}

package app;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import ui.xacthuc.LoginFrame;
import ui.dungchung.FullTextTooltipSupport;
public class Main {
    public static void main(String[] args) {
        FullTextTooltipSupport.installGlobal();
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        new LoginFrame() .setVisible(true);
                    }
                });
    }
}

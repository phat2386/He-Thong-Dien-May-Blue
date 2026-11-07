package app;
import javax.swing.*;
import ui.dungchung.HoTroChuThichDayDu;
import ui.xacthuc.KhungDangNhap;
public class ChuongTrinh {
    public static void main(String[] args) {
        HoTroChuThichDayDu.caiDatToanCuc();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new KhungDangNhap().setVisible(true);
            }
        });
    }
}

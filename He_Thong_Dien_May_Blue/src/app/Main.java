package app;

import dao.SchemaCompatibility;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


public class Main {

    public static void main(String[] args) {
        FullTextTooltipSupport.installGlobal();

        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        try {
            // Đồng bộ các migration nhỏ khi mở project trên database cũ.
            // Permission Model V2 mở mô hình phân quyền linh hoạt; Permission Defaults V3
            // tick sẵn bộ quyền nghiệp vụ ban đầu đúng 1 lần, không ghi đè chỉnh sửa sau đó.
            SchemaCompatibility.ensureCompatible();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Không thể đồng bộ cấu trúc database trước khi chạy chương trình.\n\n"
                  + ex.getMessage()
                  + "\n\nHãy chạy lại bộ SQL.",
                    "Lỗi database",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        new UIDangNhap()
                                .setVisible(true);
                    }
                });
    }
}

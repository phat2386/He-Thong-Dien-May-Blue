package giaodien.dungchung;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

/**
 * Hiển thị tooltip chứa đầy đủ nội dung khi ô nhập/combobox bị cắt chữ.
 * Được cài một lần ở cấp ứng dụng nên tự áp dụng cho các màn hình tạo sau đó.
 */
public final class FullTextTooltipSupport {
    private static boolean installed;

    private FullTextTooltipSupport() {}

    public static synchronized void installGlobal() {
        if (installed) return;
        installed = true;

        ToolTipManager.sharedInstance().setInitialDelay(250);
        ToolTipManager.sharedInstance().setDismissDelay(12000);

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override public void eventDispatched(AWTEvent event) {
                if (!(event instanceof MouseEvent)) return;
                MouseEvent e = (MouseEvent) event;
                if (e.getID() != MouseEvent.MOUSE_ENTERED && e.getID() != MouseEvent.MOUSE_MOVED) return;
                Component source = e.getComponent();
                if (source == null) return;

                JTextField field = findTextField(source);
                if (field != null) {
                    updateTextFieldTooltip(field);
                    return;
                }

                JComboBox<?> combo = findComboBox(source);
                if (combo != null) updateComboTooltip(combo);
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    private static JTextField findTextField(Component source) {
        Component c = source;
        while (c != null) {
            if (c instanceof JPasswordField) return null;
            if (c instanceof JTextField) return (JTextField) c;
            c = c.getParent();
        }
        return null;
    }

    private static JComboBox<?> findComboBox(Component source) {
        if (source instanceof JComboBox<?>) return (JComboBox<?>) source;
        return (JComboBox<?>) SwingUtilities.getAncestorOfClass(JComboBox.class, source);
    }

    private static void updateTextFieldTooltip(JTextField field) {
        if (field instanceof JPasswordField) return;
        String text = field.getText();
        if (text == null || text.trim().isEmpty()) {
            field.setToolTipText(null);
            return;
        }
        Insets insets = field.getInsets();
        int available = Math.max(0, field.getWidth() - insets.left - insets.right - 4);
        FontMetrics fm = field.getFontMetrics(field.getFont());
        field.setToolTipText(fm.stringWidth(text) > available ? htmlTooltip(text) : null);
    }

    private static void updateComboTooltip(JComboBox<?> combo) {
        Object selected = combo.getSelectedItem();
        String text = selected == null ? "" : String.valueOf(selected);
        if (text.trim().isEmpty()) {
            combo.setToolTipText(null);
            return;
        }
        Insets insets = combo.getInsets();
        int available = Math.max(0, combo.getWidth() - insets.left - insets.right - 28);
        FontMetrics fm = combo.getFontMetrics(combo.getFont());
        combo.setToolTipText(fm.stringWidth(text) > available ? htmlTooltip(text) : null);
    }

    private static String htmlTooltip(String text) {
        if (text.length() <= 70) return text;
        String escaped = text.replace("&", "&amp;")
                             .replace("<", "&lt;")
                             .replace(">", "&gt;")
                             .replace("\"", "&quot;");
        return "<html><div style='width:420px;padding:2px 3px;'>" + escaped + "</div></html>";
    }
}

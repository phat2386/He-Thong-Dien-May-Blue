package ui.dungchung;
import java.awt.Component;
import java.awt.Window;
import javax.swing.SwingUtilities;
public final class DialogUtils {
    private DialogUtils() {}
    public static Component owner(Component source) {
        if (source == null) {
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && w.isActive()) return w;
            }
            for (Window w : Window.getWindows()) {
                if (w.isShowing()) return w;
            }
            return null;
        }
        if (source instanceof Window) return source;
        Window window = SwingUtilities.getWindowAncestor(source);
        return window != null ? window : source;
    }
}

package ui.dungchung;
import java.awt.*;
import java.util.Arrays;
public final class HangSoGiaoDien {
    private HangSoGiaoDien() {
    }
    public static final Color NAVY = new Color(15, 39, 68);
    public static final Color NAVY_2 = new Color(24, 53, 85);
    public static final Color ORANGE = new Color(249, 115, 22);
    public static final Color ORANGE_HOVER = new Color(234, 108, 10);
    public static final Color BACKGROUND = new Color(241, 245, 249);
    public static final Color WHITE = Color.WHITE;
    public static final Color TEXT = new Color(15, 39, 68);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color MUTED_LIGHT = new Color(148, 163, 184);
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Color INPUT_BG = new Color(248, 250, 252);
    public static final Color GREEN = new Color(16, 185, 129);
    public static final Color RED = new Color(220, 38, 38);
    public static final Color BLUE = new Color(29, 111, 164);
    public static final Color PURPLE = new Color(139, 92, 246);
    public static final String FONT_FAMILY = timHoPhongChu();
    public static final Font TITLE = phongChu(Font.BOLD, 28);
    public static final Font HEADING = phongChu(Font.BOLD, 18);
    public static final Font SUBHEADING = phongChu(Font.BOLD, 14);
    public static final Font NORMAL = phongChu(Font.PLAIN, 14);
    public static final Font MEDIUM = phongChu(Font.BOLD, 13);
    public static final Font SMALL = phongChu(Font.PLAIN, 12);
    public static Font phongChu(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }
    private static String timHoPhongChu() {
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        if (Arrays.asList(fonts).contains("Inter")) return "Inter";
        if (Arrays.asList(fonts).contains("Segoe UI")) return "Segoe UI";
        return "SansSerif";
    }
}

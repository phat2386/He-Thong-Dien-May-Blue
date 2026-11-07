package ui.dungchung;
import java.awt.*;
import javax.swing.*;
public final class TienIchHopThoai {
    private TienIchHopThoai() {
    }
    public static boolean xacNhanBieuMau(Component source, Object noiDung, String tieuDe, String nutXacNhan) {
        Object[] luaChon = {
            "Hủy", nutXacNhan
        };
        int ketQua = JOptionPane.showOptionDialog(chuSoHuu(source), noiDung, tieuDe, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, luaChon, luaChon[1]);
        return ketQua == 1;
    }
    public static boolean xacNhanHanhDong(Component source, String noiDung, String tieuDe, String nutXacNhan) {
        Object[] luaChon = {
            "Hủy", nutXacNhan
        };
        int ketQua = JOptionPane.showOptionDialog(chuSoHuu(source), noiDung, tieuDe, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, luaChon, luaChon[1]);
        return ketQua == 1;
    }
    public static String nhapVanBanDai(Component source, String tieuDe, String nhan, String giaTriBanDau, String nutXacNhan) {
        javax.swing.JTextArea a = TaoGiaoDien.vungNhap(4, 34);
        a.setText(giaTriBanDau == null ? "" : giaTriBanDau);
        javax.swing.JPanel p = new javax.swing.JPanel(new java.awt.BorderLayout(0, 6));
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new javax.swing.JLabel(nhan), java.awt.BorderLayout.NORTH);
        p.add(TaoGiaoDien.cuonVungNhap(a, 100), java.awt.BorderLayout.CENTER);
        return xacNhanBieuMau(source, p, tieuDe, nutXacNhan) ? a.getText() : null;
    }
    public static Component chuSoHuu(Component source) {
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
        Window cuaSo = SwingUtilities.getWindowAncestor(source);
        return cuaSo != null ? cuaSo : source;
    }
}

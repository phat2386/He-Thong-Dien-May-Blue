package app;

import bus.Formats;

import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;


public final class UIFactory {
    private UIFactory() {}

    public static JButton primaryButton(String text) {
        RoundedButton b = new RoundedButton(text, UIConstants.ORANGE, UIConstants.ORANGE_HOVER, 10);
        b.setFont(UIConstants.font(Font.BOLD, 13));
        b.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        b.setDisabledColors(new Color(255, 237, 213), new Color(154, 52, 18));
        return b;
    }

    public static JButton secondaryButton(String text) {
        OutlineButton b = new OutlineButton(text, 10);
        b.setColors(Color.WHITE, new Color(248, 250, 252), UIConstants.BORDER, UIConstants.TEXT);
        b.setDisabledColors(new Color(248, 250, 252), new Color(226, 232, 240), new Color(148, 163, 184));
        b.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        return b;
    }

    public static JTextField field(int width) {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(width, 36));
        f.setFont(UIConstants.NORMAL);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        return f;
    }

    public static JComboBox<String> combo(String... values) {
        JComboBox<String> c = new JComboBox<String>(values);
        c.setPreferredSize(new Dimension(170, 36));
        c.setFont(UIConstants.NORMAL);
        c.setBackground(Color.WHITE);
        return c;
    }

    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(36);
        t.setFont(UIConstants.SMALL);
        t.setForeground(UIConstants.TEXT);
        t.setSelectionBackground(new Color(255, 237, 213));
        t.setSelectionForeground(UIConstants.TEXT);
        t.setGridColor(UIConstants.BORDER);
        t.setShowVerticalLines(false);
        t.setAutoCreateRowSorter(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(UIConstants.MEDIUM);
        h.setBackground(new Color(248,250,252));
        h.setForeground(UIConstants.MUTED);
        h.setPreferredSize(new Dimension(100,38));
        return t;
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static RoundedPanel card(String title, String value, String sub, Color accent) {
        RoundedPanel card = new RoundedPanel(14, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));
        JPanel strip = new JPanel();
        strip.setBackground(accent);
        strip.setPreferredSize(new Dimension(4,80));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title); t.setFont(UIConstants.SMALL); t.setForeground(UIConstants.MUTED);
        JLabel v = new JLabel(value); v.setFont(new Font("Inter",Font.BOLD,22)); v.setForeground(UIConstants.TEXT);
        JLabel s = new JLabel(sub); s.setFont(UIConstants.SMALL); s.setForeground(accent);
        content.add(t); content.add(Box.createVerticalStrut(5)); content.add(v); content.add(Box.createVerticalStrut(4)); content.add(s);
        card.add(strip, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    public static JPanel toolbar(Component... comps) {
        JPanel p = new JPanel(new WrapLayout(FlowLayout.LEFT,8,6));
        p.setOpaque(false);
        for (Component c: comps) p.add(c);
        return p;
    }

    public static void exportCsv(Component parent, JTable table, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName.endsWith(".csv") ? defaultName : defaultName+".csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            out.write('\ufeff');
            TableModel m = table.getModel();
            for (int c=0;c<m.getColumnCount();c++) {
                if (c>0) out.print(",");
                out.print(csv(m.getColumnName(c)));
            }
            out.println();
            for (int r=0;r<m.getRowCount();r++) {
                for (int c=0;c<m.getColumnCount();c++) {
                    if (c>0) out.print(",");
                    out.print(csv(Formats.safe(m.getValueAt(r,c))));
                }
                out.println();
            }
            JOptionPane.showMessageDialog(DialogUtils.owner(parent), "Đã xuất tệp dữ liệu bảng:\n"+file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(DialogUtils.owner(parent), ex.getMessage(), "Lỗi xuất file", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void printPdf(Component parent, JTable table, String title) {
        try {
            boolean ok = table.print(JTable.PrintMode.FIT_WIDTH,
                    new java.text.MessageFormat(title),
                    new java.text.MessageFormat("Trang {0}"));
            if (!ok) return;
            JOptionPane.showMessageDialog(DialogUtils.owner(parent),
                    "Trong hộp thoại in, chọn chức năng lưu thành tệp tài liệu để lưu kết quả.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(DialogUtils.owner(parent), ex.getMessage(), "Lỗi in tài liệu", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"","\"\"") + "\"";
    }
}

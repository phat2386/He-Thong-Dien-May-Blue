package ui.dungchung;

import java.awt.*;
import java.awt.event.*;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;

public final class GoiYTimKiem {
    public static final class Muc {
        private final String giaTri;
        private final String hienThi;

        public Muc(String giaTri, String hienThi) {
            this.giaTri = giaTri == null ? "" : giaTri;
            this.hienThi = hienThi == null ? this.giaTri : hienThi;
        }

        public String getGiaTri() {
            return giaTri;
        }

        @Override
        public String toString() {
            return hienThi;
        }
    }

    private final JTextField oNhap;
    private final Function<String, List<Muc>> nguon;
    private final Consumer<Muc> khiChon;
    private final JPopupMenu popup = new JPopupMenu();
    private final DefaultListModel<Muc> moHinh = new DefaultListModel<>();
    private final JList<Muc> danhSach = new JList<Muc>(moHinh) {
        private static final long serialVersionUID = 1L;

        @Override
        public String getToolTipText(MouseEvent e) {
            int index = locationToIndex(e.getPoint());
            if (index < 0 || index >= getModel().getSize())
                return null;
            Muc muc = getModel().getElementAt(index);
            return muc == null ? null : goiYHtml(muc.toString());
        }
    };
    private final JScrollPane cuon;
    private final javax.swing.Timer henGio;
    private boolean dangChon;

    private GoiYTimKiem(JTextField oNhap, Function<String, List<Muc>> nguon, Consumer<Muc> khiChon) {
        this.oNhap = oNhap;
        this.nguon = nguon;
        this.khiChon = khiChon == null ? m -> {
        } : khiChon;
        danhSach.setFont(HangSoGiaoDien.NORMAL);
        danhSach.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        danhSach.setFixedCellHeight(32);
        // Popup chỉ dùng để gợi ý, không được cướp focus khỏi ô nhập.
        danhSach.setFocusable(false);
        cuon = new JScrollPane(danhSach);
        cuon.setBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER));
        cuon.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cuon.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        cuon.getHorizontalScrollBar().setUnitIncrement(24);
        popup.setBorder(BorderFactory.createEmptyBorder());
        popup.setFocusable(false);
        popup.setRequestFocusEnabled(false);
        popup.add(cuon);
        henGio = new javax.swing.Timer(140, e -> capNhat());
        henGio.setRepeats(false);
        oNhap.getDocument().addDocumentListener(new DocumentListener() {
            private void doi() {
                if (!dangChon)
                    henGio.restart();
            }

            public void insertUpdate(DocumentEvent e) {
                doi();
            }

            public void removeUpdate(DocumentEvent e) {
                doi();
            }

            public void changedUpdate(DocumentEvent e) {
                doi();
            }
        });
        danhSach.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1)
                    chonDangChon();
            }
        });
        oNhap.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popup.isVisible())
                    return;
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    danhSach.setSelectedIndex(
                            Math.min(danhSach.getModel().getSize() - 1, Math.max(0, danhSach.getSelectedIndex() + 1)));
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    danhSach.setSelectedIndex(Math.max(0, danhSach.getSelectedIndex() - 1));
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && danhSach.getSelectedIndex() >= 0) {
                    chonDangChon();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                    popup.setVisible(false);
            }
        });
    }

    public static void gan(JTextField oNhap, Function<String, List<Muc>> nguon, Consumer<Muc> khiChon) {
        new GoiYTimKiem(oNhap, nguon, khiChon);
    }

    public static void ganTuBang(JTextField oNhap, JTable bang, int cotGiaTri, int... cotHienThi) {
        ganTuBang(oNhap, bang, cotGiaTri, m -> {
        }, cotHienThi);
    }

    public static void ganTuBang(JTextField oNhap, JTable bang, int cotGiaTri, Consumer<Muc> khiChon,
            int... cotHienThi) {
        List<Muc> boNho = new ArrayList<>();
        Runnable capNhatBoNho = () -> {
            if (!oNhap.getText().trim().isEmpty())
                return;
            boNho.clear();
            boNho.addAll(docTuBang(bang.getModel(), cotGiaTri, cotHienThi));
        };
        capNhatBoNho.run();
        GoiYTimKiem boGoiY = new GoiYTimKiem(oNhap, q -> loc(boNho, q), khiChon);
        bang.getModel().addTableModelListener(e -> {
            if (oNhap.getText().trim().isEmpty())
                capNhatBoNho.run();
            if (!oNhap.getText().trim().isEmpty() && oNhap.isFocusOwner())
                boGoiY.henGio.restart();
        });
    }

    private static List<Muc> docTuBang(TableModel model, int cotGiaTri, int[] cotHienThi) {
        LinkedHashMap<String, Muc> ketQua = new LinkedHashMap<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            String giaTri = chuoi(model.getValueAt(r, cotGiaTri));
            if (giaTri.isEmpty())
                continue;
            StringBuilder hienThi = new StringBuilder();
            for (int c : cotHienThi) {
                if (c < 0 || c >= model.getColumnCount())
                    continue;
                String v = chuoi(model.getValueAt(r, c));
                if (v.isEmpty())
                    continue;
                if (hienThi.length() > 0)
                    hienThi.append(" - ");
                hienThi.append(v);
            }
            String display = hienThi.length() == 0 ? giaTri : hienThi.toString();
            ketQua.putIfAbsent(giaTri, new Muc(giaTri, display));
        }
        return new ArrayList<>(ketQua.values());
    }

    private static List<Muc> loc(List<Muc> nguon, String q) {
        String khoa = chuanHoa(q);
        List<Muc> ketQua = new ArrayList<>();
        for (Muc muc : nguon) {
            if (chuanHoa(muc.toString()).contains(khoa) || chuanHoa(muc.getGiaTri()).contains(khoa))
                ketQua.add(muc);
            if (ketQua.size() >= 12)
                break;
        }
        return ketQua;
    }

    private void capNhat() {
        String q = oNhap.getText().trim();
        if (q.isEmpty() || !oNhap.isShowing()) {
            popup.setVisible(false);
            return;
        }
        List<Muc> cacMuc;
        try {
            cacMuc = nguon.apply(q);
        } catch (Exception ex) {
            popup.setVisible(false);
            return;
        }
        moHinh.clear();
        if (cacMuc != null)
            for (Muc m : cacMuc)
                if (m != null && moHinh.size() < 12)
                    moHinh.addElement(m);
        if (moHinh.isEmpty()) {
            popup.setVisible(false);
            return;
        }
        danhSach.setVisibleRowCount(Math.min(8, moHinh.size()));
        int cao = Math.min(8, moHinh.size()) * 32 + 6;
        int rongNoiDung = doRongNoiDung();
        int rongToiDa = doRongToiDaTrenManHinh();
        int rong = Math.min(rongToiDa, Math.max(Math.max(oNhap.getWidth(), 360), rongNoiDung));
        // Khi chuỗi vẫn dài hơn giới hạn màn hình thì cho phép cuộn ngang thay vì cắt
        // mất dữ liệu.
        danhSach.setFixedCellWidth(Math.max(rong - 26, rongNoiDung - 10));
        popup.setPopupSize(rong, cao);
        popup.show(oNhap, 0, oNhap.getHeight());
    }

    private int doRongNoiDung() {
        FontMetrics fm = danhSach.getFontMetrics(danhSach.getFont());
        int max = 0;
        for (int i = 0; i < moHinh.size(); i++) {
            Muc muc = moHinh.get(i);
            if (muc != null)
                max = Math.max(max, fm.stringWidth(muc.toString()));
        }
        return max + 42;
    }

    private int doRongToiDaTrenManHinh() {
        try {
            Point p = oNhap.getLocationOnScreen();
            GraphicsConfiguration gc = oNhap.getGraphicsConfiguration();
            Rectangle bounds = gc == null ? GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
                    : gc.getBounds();
            Insets in = gc == null ? new Insets(0, 0, 0, 0) : Toolkit.getDefaultToolkit().getScreenInsets(gc);
            int phai = bounds.x + bounds.width - in.right;
            return Math.max(360, phai - p.x - 12);
        } catch (Exception ex) {
            return 820;
        }
    }

    private void chonDangChon() {
        Muc muc = danhSach.getSelectedValue();
        if (muc == null && moHinh.size() > 0)
            muc = moHinh.get(0);
        if (muc == null)
            return;
        dangChon = true;
        try {
            oNhap.setText(muc.getGiaTri());
        } finally {
            dangChon = false;
        }
        popup.setVisible(false);
        khiChon.accept(muc);
    }

    private static String goiYHtml(String vanBan) {
        if (vanBan == null)
            return null;
        String escaped = vanBan.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        return "<html><div style='width:620px;padding:2px 3px;'>" + escaped + "</div></html>";
    }

    private static String chuoi(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String chuanHoa(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return n.replace('đ', 'd');
    }
}

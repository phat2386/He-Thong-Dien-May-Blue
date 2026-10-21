package app;

import bus.Formats;
import bus.PhienDangNhap;
import dao.Db;

import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;


public class PanelPersonalSales extends PanelBaseScreen {
    private static final long serialVersionUID = 1L;

    private final JTextField from = UIFactory.field(110);
    private final JTextField to = UIFactory.field(110);
    private final HintTextField employeeSearch = new HintTextField("Nhập mã hoặc tên NV...");

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Ngày", "Mã NV", "Nhân viên", "Số hóa đơn", "Doanh số", "Hoàn trả", "Doanh số ròng", "Tiền thưởng"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable table = UIFactory.table(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);
    private final JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));

    public PanelPersonalSales() {
        super("Doanh số nhân viên", "Theo dõi doanh số, hoàn trả và tiền thưởng của nhân viên trong khoảng thời gian đã chọn.");

        from.setText(LocalDate.now().withDayOfMonth(1).toString());
        to.setText(LocalDate.now().toString());

        JButton apply = UIFactory.primaryButton("Áp dụng");
        body.add(UIFactory.toolbar(new JLabel("Từ ngày"), from, new JLabel("Đến ngày"), to, apply), BorderLayout.NORTH);

        cards.setOpaque(false);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);

        JPanel detail = new JPanel(new BorderLayout(0, 8));
        detail.setOpaque(false);
        detail.add(buildDetailHeader(), BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        detail.add(UIFactory.scroll(table), BorderLayout.CENTER);
        center.add(detail, BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);

        apply.addActionListener(e -> refreshData());
        employeeSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyEmployeeFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyEmployeeFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyEmployeeFilter(); }
        });

        refreshData();
    }

    private JComponent buildDetailHeader() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JLabel title = new JLabel("CHI TIẾT DOANH SỐ");
        title.setFont(UIConstants.font(Font.BOLD, 12));
        title.setForeground(UIConstants.TEXT);
        row.add(title, BorderLayout.WEST);

        employeeSearch.setPreferredSize(new Dimension(260, 36));
        employeeSearch.setMinimumSize(new Dimension(220, 36));
        employeeSearch.setFont(UIConstants.NORMAL);
        employeeSearch.setForeground(UIConstants.TEXT);
        employeeSearch.setToolTipText("Nhập mã nhân viên hoặc tên nhân viên để lọc bảng doanh số");
        employeeSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));

        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(employeeSearch);
        row.add(searchWrap, BorderLayout.EAST);
        return row;
    }

    @Override public void refreshData() {
        try {
            if (PhienDangNhap.getTaiKhoanHienTai() == null || PhienDangNhap.getTaiKhoanHienTai().getNhanVien() == null)
                throw new IllegalStateException("Phiên đăng nhập không hợp lệ.");

            LocalDate f = LocalDate.parse(from.getText().trim());
            LocalDate t = LocalDate.parse(to.getText().trim());
            if (f.isAfter(t)) throw new IllegalArgumentException("Từ ngày phải nhỏ hơn hoặc bằng đến ngày.");

            BigDecimal grossSales = decimal(Db.scalar(
                    "SELECT ISNULL(SUM(CASE WHEN sales_amount>0 THEN sales_amount ELSE 0 END),0) "
                  + "FROM sales_bonuses WHERE CAST(created_at AS DATE) BETWEEN ? AND ?", f, t));
            BigDecimal refundAmount = decimal(Db.scalar(
                    "SELECT ISNULL(ABS(SUM(CASE WHEN sales_amount<0 THEN sales_amount ELSE 0 END)),0) "
                  + "FROM sales_bonuses WHERE CAST(created_at AS DATE) BETWEEN ? AND ?", f, t));
            BigDecimal bonus = decimal(Db.scalar(
                    "SELECT ISNULL(SUM(bonus_amount),0) FROM sales_bonuses "
                  + "WHERE CAST(created_at AS DATE) BETWEEN ? AND ?", f, t));
            Object orders = Db.scalar(
                    "SELECT COUNT(DISTINCT CASE WHEN sales_amount>0 THEN invoice_id END) FROM sales_bonuses "
                  + "WHERE CAST(created_at AS DATE) BETWEEN ? AND ?", f, t);

            cards.removeAll();
            cards.add(summaryCard("Tổng doanh số", Formats.money(grossSales), UIConstants.ORANGE));
            cards.add(summaryCard("Số hóa đơn", String.valueOf(orders), UIConstants.BLUE));
            cards.add(summaryCard("Tiền hoàn trả", Formats.money(refundAmount), new Color(220, 38, 38)));
            cards.add(summaryCard("Tiền thưởng", Formats.money(bonus), UIConstants.GREEN));

            model.setRowCount(0);
            String sql = "SELECT CAST(sb.created_at AS DATE) d,e.employee_id,e.full_name,"
                    + "COUNT(DISTINCT CASE WHEN sb.sales_amount>0 THEN sb.invoice_id END) cnt,"
                    + "SUM(CASE WHEN sb.sales_amount>0 THEN sb.sales_amount ELSE 0 END) gross_sales,"
                    + "ABS(SUM(CASE WHEN sb.sales_amount<0 THEN sb.sales_amount ELSE 0 END)) refund_amount,"
                    + "SUM(sb.sales_amount) net_sales,SUM(sb.bonus_amount) bonus "
                    + "FROM sales_bonuses sb JOIN employees e ON e.employee_id=sb.employee_id "
                    + "WHERE CAST(sb.created_at AS DATE) BETWEEN ? AND ? "
                    + "GROUP BY CAST(sb.created_at AS DATE),e.employee_id,e.full_name "
                    + "ORDER BY d,e.employee_id";

            for (Map<String, Object> r : Db.query(sql, f, t)) {
                model.addRow(new Object[]{
                        Formats.date(r.get("d")),
                        r.get("employee_id"),
                        r.get("full_name"),
                        r.get("cnt"),
                        Formats.money(r.get("gross_sales")),
                        Formats.money(r.get("refund_amount")),
                        Formats.money(r.get("net_sales")),
                        Formats.money(r.get("bonus"))
                });
            }

            applyEmployeeFilter();
            configureColumnWidths();
            revalidate();
            repaint();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void applyEmployeeFilter() {
        final String q = employeeSearch.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String employeeId = String.valueOf(entry.getValue(1)).toLowerCase(Locale.ROOT);
                String employeeName = String.valueOf(entry.getValue(2)).toLowerCase(Locale.ROOT);
                return employeeId.contains(q) || employeeName.contains(q);
            }
        });
    }

    private void configureColumnWidths() {
        int[] widths = {90, 105, 150, 80, 115, 105, 120, 110};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private JPanel summaryCard(String title, String value, Color accent) {
        RoundedPanel card = new RoundedPanel(14, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel strip = new JPanel();
        strip.setBackground(accent);
        strip.setPreferredSize(new Dimension(4, 64));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(UIConstants.SMALL);
        t.setForeground(UIConstants.MUTED);
        JLabel v = new JLabel(value);
        v.setFont(UIConstants.font(Font.BOLD, 20));
        v.setForeground(UIConstants.TEXT);

        content.add(t);
        content.add(Box.createVerticalStrut(7));
        content.add(v);

        card.add(strip, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private static BigDecimal decimal(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }

    private static class HintTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String hint;

        HintTextField(String hint) {
            this.hint = hint;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setFont(getFont());
                g2.setColor(UIConstants.MUTED);
                Insets insets = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, insets.left, y);
                g2.dispose();
            }
        }
    }
}

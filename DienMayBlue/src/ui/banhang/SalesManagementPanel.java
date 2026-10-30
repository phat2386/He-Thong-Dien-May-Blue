package ui.banhang;

import java.awt.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import database.Db;
import util.Formats;
import tienichUtil.UiText;
import ui.dungchung.*;
import ui.chinh.MainFrame;

/** UC017 - Tổng quan quản lý bán hàng. */
public class SalesManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;

    private final JLabel lblRevenue = new JLabel("0đ");
    private final JLabel lblInvoices = new JLabel("0");
    private final JLabel lblDeliveries = new JLabel("0");
    private final JLabel lblReturns = new JLabel("0");

    private final DefaultTableModel recentModel = new DefaultTableModel(
            new Object[]{"Mã hóa đơn", "Khách hàng", "Ngày lập", "Số SP", "Thực thu", "Trạng thái"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable recentTable = UIFactory.table(recentModel);

    public SalesManagementPanel(MainFrame mainFrame) {
        super("Quản lý bán hàng", "Tổng quan hoạt động bán hàng tại quầy.");

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setOpaque(false);

        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setOpaque(false);
        kpis.setPreferredSize(new Dimension(100, 118));
        kpis.add(kpiCard("money", "Doanh thu hôm nay", lblRevenue,
                "Doanh thu thực thu sau hoàn tiền"));
        kpis.add(kpiCard("cart", "Hóa đơn hôm nay", lblInvoices,
                "Hóa đơn đã thanh toán"));
        kpis.add(kpiCard("truck", "Giao hàng chờ xử lý", lblDeliveries,
                "Phiếu đang chuẩn bị giao"));
        kpis.add(kpiCard("box", "Đổi trả đang chờ", lblReturns,
                "Yêu cầu chưa hoàn tất"));

        RoundedPanel recentCard = new RoundedPanel(14, Color.WHITE);
        recentCard.setLayout(new BorderLayout());
        recentCard.setBorderColor(UIConstants.BORDER);

        JPanel recentHead = new JPanel(new BorderLayout());
        recentHead.setOpaque(false);
        recentHead.setBorder(BorderFactory.createEmptyBorder(12, 16, 10, 16));

        JPanel recentText = new JPanel();
        recentText.setOpaque(false);
        recentText.setLayout(new BoxLayout(recentText, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Giao dịch gần đây");
        title.setFont(UIConstants.SUBHEADING);
        title.setForeground(UIConstants.TEXT);
        JLabel note = new JLabel("Các hóa đơn bán hàng mới nhất trong hệ thống");
        note.setFont(UIConstants.font(Font.PLAIN, 11));
        note.setForeground(UIConstants.MUTED);
        recentText.add(title);
        recentText.add(Box.createVerticalStrut(3));
        recentText.add(note);
        recentHead.add(recentText, BorderLayout.WEST);

        recentTable.setRowHeight(34);
        recentCard.add(recentHead, BorderLayout.NORTH);
        recentCard.add(UIFactory.scroll(recentTable), BorderLayout.CENTER);

        root.add(kpis, BorderLayout.NORTH);
        root.add(recentCard, BorderLayout.CENTER);
        body.add(root, BorderLayout.CENTER);

        refreshData();
    }

    private JPanel kpiCard(String iconKind, String label, JLabel valueLabel, String caption) {
        RoundedPanel card = new RoundedPanel(14, Color.WHITE);
        card.setBorderColor(UIConstants.BORDER);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(13, 15, 12, 15));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        top.setOpaque(false);

        valueLabel.setFont(UIConstants.font(Font.BOLD, 21));
        valueLabel.setForeground(UIConstants.TEXT);

        JLabel name = new JLabel(label);
        name.setFont(UIConstants.font(Font.BOLD, 11));
        name.setForeground(UIConstants.TEXT);

        JLabel sub = new JLabel(caption);
        sub.setFont(UIConstants.font(Font.PLAIN, 10));
        sub.setForeground(UIConstants.MUTED);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(valueLabel);
        bottom.add(Box.createVerticalStrut(2));
        bottom.add(name);
        bottom.add(Box.createVerticalStrut(1));
        bottom.add(sub);

        card.add(bottom, BorderLayout.CENTER);
        return card;
    }

    @Override public void refreshData() {
        try {
            Object revenue = Db.scalar(
                    "SELECT ISNULL(SUM(i.total_amount-ISNULL(r.refunded,0)),0) "
                  + "FROM invoices i "
                  + "LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests "
                  + "WHERE status='COMPLETED' GROUP BY invoice_id) r ON r.invoice_id=i.invoice_id "
                  + "WHERE CAST(i.created_at AS DATE)=CAST(GETDATE() AS DATE) "
                  + "AND i.status IN ('PAID','PARTIAL_REFUND','REFUNDED')");

            Object invoiceCount = Db.scalar(
                    "SELECT COUNT(*) FROM invoices "
                  + "WHERE CAST(created_at AS DATE)=CAST(GETDATE() AS DATE) "
                  + "AND status IN ('PAID','PARTIAL_REFUND','REFUNDED')");

            Object deliveryCount = Db.scalar(
                    "SELECT COUNT(*) FROM deliveries WHERE status='PREPARING'");

            Object returnCount = Db.scalar(
                    "SELECT COUNT(*) FROM return_requests "
                  + "WHERE status IN ('PENDING','WAITING_APPROVAL','APPROVED')");

            lblRevenue.setText(shortMoney(revenue));
            lblInvoices.setText(String.valueOf(invoiceCount));
            lblDeliveries.setText(String.valueOf(deliveryCount));
            lblReturns.setText(String.valueOf(returnCount));

            recentModel.setRowCount(0);
            for (Map<String,Object> r : Db.query(
                    "SELECT TOP 10 i.invoice_id,ISNULL(c.full_name,N'Khách vãng lai') customer,i.created_at,"
                  + "i.total_amount-ISNULL(rr.refunded,0) net_total,i.status,"
                  + "ISNULL((SELECT SUM(quantity) FROM invoice_details d WHERE d.invoice_id=i.invoice_id),0) item_count "
                  + "FROM invoices i "
                  + "LEFT JOIN customers c ON c.customer_id=i.customer_id "
                  + "LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests "
                  + "WHERE status='COMPLETED' GROUP BY invoice_id) rr ON rr.invoice_id=i.invoice_id "
                  + "WHERE i.status IN ('PAID','PARTIAL_REFUND','REFUNDED') "
                  + "ORDER BY i.created_at DESC")) {
                recentModel.addRow(new Object[]{
                        r.get("invoice_id"),
                        r.get("customer"),
                        Formats.date(r.get("created_at")),
                        r.get("item_count"),
                        Formats.money(r.get("net_total")),
                        Formats.vi(r.get("status"))
                });
            }

            revalidate();
            repaint();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private String shortMoney(Object value) {
        if (value == null) return "0đ";
        double n = ((Number) value).doubleValue();
        if (n >= 1_000_000_000d) return String.format(java.util.Locale.US, "%.1f tỷ đ", n / 1_000_000_000d);
        if (n >= 1_000_000d) return String.format(java.util.Locale.US, "%.1f triệu đ", n / 1_000_000d);
        if (n >= 1_000d) return String.format(java.util.Locale.US, "%.0f nghìn đ", n / 1_000d);
        return String.format(java.util.Locale.US, "%.0fđ", n);
    }
}

package ui.phieunhap;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import service.ProductService;
import ui.dungchung.DialogUtils;
import ui.dungchung.UIFactory;
import util.Formats;

/**
 * Xem danh sách phiếu nhập kho.
 * Mỗi lần nhập kho ghi một dòng vào bảng supplier_debts, trong đó reference_code là mã phiếu nhập.
 * Màn hình này đọc lại các dòng đó và hiển thị dưới dạng danh sách phiếu nhập.
 */
public class StockReceiptListPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final ProductService service = new ProductService();
    private final JTextField txtSearch = UIFactory.field(300);
    private final DefaultTableModel model = UIFactory.readOnlyModel(new Object[]{
            "Mã phiếu", "Nhà cung cấp", "Nội dung nhập", "Tổng tiền", "Còn nợ", "Trạng thái", "Ngày nhập"});
    private final JLabel lblSummary = new JLabel();

    public StockReceiptListPanel() {
        super(new BorderLayout(0, 10));
        JTable tblReceipt = UIFactory.table(model);
        JScrollPane scrReceipt = UIFactory.scroll(tblReceipt);
        scrReceipt.setPreferredSize(new Dimension(900, 340));

        JPanel pnlTop = new JPanel(new BorderLayout(12, 0));
        pnlTop.add(txtSearch, BorderLayout.WEST);
        pnlTop.add(lblSummary, BorderLayout.EAST);

        add(pnlTop, BorderLayout.NORTH);
        add(scrReceipt, BorderLayout.CENTER);

        UIFactory.onTextChange(txtSearch, this::refreshData);
        refreshData();
    }

    private void refreshData() {
        try {
            String keyword = "%" + txtSearch.getText().trim() + "%";
            model.setRowCount(0);
            BigDecimal totalRemaining = BigDecimal.ZERO;
            for (Map<String, Object> r : service.query(
                    "SELECT d.reference_code,s.supplier_name,d.description,d.original_amount,"
                  + "d.remaining_amount,d.status,d.created_at "
                  + "FROM supplier_debts d JOIN suppliers s ON s.supplier_id=d.supplier_id "
                  + "WHERE d.reference_code IS NOT NULL "
                  + "AND (d.reference_code LIKE ? OR s.supplier_name LIKE ?) "
                  + "ORDER BY d.created_at DESC, d.debt_id DESC",
                    keyword, keyword)) {
                BigDecimal remaining = (BigDecimal) r.get("remaining_amount");
                if (remaining != null) {
                    totalRemaining = totalRemaining.add(remaining);
                }
                model.addRow(new Object[]{
                        r.get("reference_code"),
                        r.get("supplier_name"),
                        Formats.safe(r.get("description")),
                        Formats.money(r.get("original_amount")),
                        Formats.money(remaining),
                        Formats.vi(r.get("status")),
                        Formats.date(r.get("created_at"))});
            }
            lblSummary.setText("Số phiếu: " + model.getRowCount() + "   |   Tổng còn nợ: " + Formats.money(totalRemaining));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    /** Mở danh sách phiếu nhập kho trong một hộp thoại. */
    public static void show(Component parent) {
        JOptionPane.showMessageDialog(DialogUtils.owner(parent), new StockReceiptListPanel(),
                "Phiếu nhập kho", JOptionPane.PLAIN_MESSAGE);
    }
}

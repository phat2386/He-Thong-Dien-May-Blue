package ui.banhang;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import service.OperationService;
import util.Formats;
import ui.dungchung.*;
public class ReturnRequestPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;
    private final OperationService service = new OperationService();
    private final JTextField txtSearch = UIFactory.field(260);
    private final JComboBox<String> cmbInvoice = new JComboBox<String>();
    private final JComboBox<String> cmbDetail = new JComboBox<String>();
    private final JSpinner spnQty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JTextField txtReason = UIFactory.field(280);
    private final JTextField txtCondition = UIFactory.field(280);
    private final JTextField txtRefund = UIFactory.field(180);
    private final JCheckBox chkSeal = new JCheckBox("Còn tem/mác", true);
    private final JCheckBox chkAccessories = new JCheckBox("Đủ phụ kiện", true);
    private final JTextField txtEvidence = UIFactory.field(280);
    private final JLabel lblRule = new JLabel("Giá trị hoàn do hệ thống tính theo số tiền thực trả của sản phẩm.");
    public ReturnRequestPanel() {
        super("Lập phiếu đổi trả / hoàn tiền", "Tra cứu hóa đơn, kiểm tra điều kiện và xử lý hoặc chuyển Quản lý duyệt.");
        JButton btnFind = UIFactory.secondaryButton("Tra cứu");
        JButton btnRefresh = UIFactory.secondaryButton("Làm mới");
        JButton btnSave = UIFactory.primaryButton("Lập phiếu đổi trả");
        pnlActions.add(btnSave); pnlActions.add(btnRefresh);
        txtRefund.setEditable(false);
        txtRefund.setBackground(new Color(248,250,252));
        makeFlexible(cmbInvoice, 150);
        makeFlexible(cmbDetail, 150);
        makeFlexible(txtReason, 130);
        makeFlexible(txtCondition, 130);
        makeFlexible(txtRefund, 110);
        makeFlexible(txtEvidence, 130);
        lblRule.setFont(UIConstants.SMALL); lblRule.setForeground(UIConstants.MUTED);
        JPanel pnlTop = UIFactory.toolbar(new JLabel("Mã hóa đơn / SĐT"), txtSearch, btnFind);
        pnlBody.add(pnlTop, BorderLayout.NORTH);
        RoundedPanel card = new RoundedPanel(12, Color.WHITE);
        card.setBorderColor(UIConstants.BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        pnlBody.add(card, BorderLayout.CENTER);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7,7,7,7); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int y = 0;
        y = row(card,c,y,"Hóa đơn",cmbInvoice,"Sản phẩm",cmbDetail);
        y = row(card,c,y,"Số lượng",spnQty,"Giá trị hoàn",txtRefund);
        y = row(card,c,y,"Lý do",txtReason,"Tình trạng vật lý",txtCondition);
        y = row(card,c,y,"Điều kiện",chkSeal,"Phụ kiện",chkAccessories);
        y = row(card,c,y,"Ảnh minh chứng (đường dẫn)",txtEvidence,"",new JLabel(""));
        JLabel lblNote = new JLabel("Điều kiện: trong 7 ngày, còn tem/mác, đủ phụ kiện. Trên 5.000.000đ sẽ chuyển Quản lý duyệt.");
        lblNote.setFont(UIConstants.SMALL); lblNote.setForeground(UIConstants.MUTED);
        c.gridx=0;c.gridy=y;c.gridwidth=4;card.add(lblNote,c); y++;
        c.gridy=y;card.add(lblRule,c);
        cmbInvoice.addActionListener(e -> loadDetails());
        cmbDetail.addActionListener(e -> updateRefundPreview());
        spnQty.addChangeListener((ChangeEvent e) -> updateRefundPreview());
        btnFind.addActionListener(e -> refreshData());
        txtSearch.addActionListener(e -> refreshData());
        btnRefresh.addActionListener(e -> { txtSearch.setText(""); refreshData(); });
        btnSave.addActionListener(e -> saveReturn());
        refreshData();
    }
    private int row(JPanel pnlP, GridBagConstraints c, int y, String l1, Component v1, String l2, Component v2) {
        c.gridwidth=1;c.gridy=y;c.weightx=0;c.gridx=0;pnlP.add(label(l1),c);
        c.weightx=.54;c.gridx=1;pnlP.add(v1,c);
        c.weightx=0;c.gridx=2;pnlP.add(label(l2),c);
        c.weightx=.46;c.gridx=3;pnlP.add(v2,c);
        return y+1;
    }
    private void makeFlexible(JComponent component, int minWidth) {
        Dimension pref = component.getPreferredSize();
        component.setMinimumSize(new Dimension(minWidth, Math.max(32, pref.height)));
    }
    private JLabel label(String s){JLabel lblL=new JLabel(s);lblL.setFont(UIConstants.MEDIUM);lblL.setForeground(UIConstants.MUTED);return lblL;}
    @Override public void refreshData() {
        try {
            cmbInvoice.removeAllItems();
            String key = txtSearch.getText().trim();
            String like = "%" + key + "%";
            for (Map<String,Object> r : service.query(
                    "SELECT i.invoice_id,ISNULL(c.full_name,N'Khách vãng lai') customer,ISNULL(c.phone,'') phone,i.created_at,i.total_amount "
                  + "FROM invoices i LEFT JOIN customers c ON c.customer_id=i.customer_id "
                  + "WHERE i.status='PAID' AND NOT EXISTS(SELECT 1 FROM return_requests rr WHERE rr.invoice_id=i.invoice_id) "
                  + "AND (?='' OR i.invoice_id LIKE ? OR ISNULL(c.phone,'') LIKE ?) ORDER BY i.created_at DESC", key,like,like))
                cmbInvoice.addItem(r.get("invoice_id")+" - "+r.get("customer")+" - "+r.get("phone")+" - "+Formats.date(r.get("created_at"))+" - "+Formats.money(r.get("total_amount")));
            loadDetails();
            if (cmbInvoice.getItemCount()==0) lblRule.setText("Không có hóa đơn đủ điều kiện theo từ khóa hiện tại hoặc hóa đơn đã có phiếu đổi trả.");
        } catch (Exception ex) { showError(ex); }
    }
    private void loadDetails() {
        cmbDetail.removeAllItems();
        String inv = String.valueOf(cmbInvoice.getSelectedItem());
        if (inv == null || inv.equals("null") || !inv.contains(" - ")) { txtRefund.setText(""); return; }
        String id = inv.substring(0, inv.indexOf(" - "));
        try {
            for (Map<String,Object> r : service.query(
                    "SELECT d.invoice_detail_id,d.product_id,p.product_name,d.quantity,d.line_total FROM invoice_details d "
                  + "JOIN products p ON p.product_id=d.product_id WHERE d.invoice_id=?", id))
                cmbDetail.addItem(r.get("invoice_detail_id")+" - "+r.get("product_id")+" - "+r.get("product_name")+" | SL mua "+r.get("quantity")+" | Đã trả "+Formats.money(r.get("line_total")));
            updateRefundPreview();
        } catch (Exception ex) { showError(ex); }
    }
    private Long selectedDetailId() {
        String ds = String.valueOf(cmbDetail.getSelectedItem());
        if (ds == null || ds.equals("null") || !ds.contains(" - ")) return null;
        return Long.valueOf(ds.substring(0, ds.indexOf(" - ")));
    }
    private void updateRefundPreview() {
        Long detailId = selectedDetailId();
        if (detailId == null) { txtRefund.setText(""); return; }
        try {
            Map<String,Object> row = service.one("SELECT quantity FROM invoice_details WHERE invoice_detail_id=?", detailId);
            int max = ((Number)row.get("quantity")).intValue();
            SpinnerNumberModel m = (SpinnerNumberModel)spnQty.getModel();
            m.setMaximum(Integer.valueOf(max));
            if (((Number)m.getNumber()).intValue() > max) m.setValue(Integer.valueOf(max));
            BigDecimal amount = service.calculateRefundAmount(detailId.longValue(), ((Number)spnQty.getValue()).intValue());
            txtRefund.setText(Formats.money(amount));
            lblRule.setText(amount.compareTo(OperationService.REFUND_APPROVAL_LIMIT)>0
                    ? "Giá trị hoàn vượt 5.000.000đ: phiếu sẽ chờ Quản lý duyệt, chưa hoàn tiền ngay."
                    : "Giá trị hoàn trong hạn mức: hệ thống sẽ xử lý hoàn tiền ngay sau khi xác nhận.");
        } catch (Exception ex) {
            txtRefund.setText("");
            lblRule.setText(ex.getMessage());
        }
    }
    private void saveReturn() {
        try {
            String inv = String.valueOf(cmbInvoice.getSelectedItem());
            Long detailId = selectedDetailId();
            if (inv == null || inv.equals("null") || detailId == null)
                throw new IllegalArgumentException("Chưa chọn hóa đơn/sản phẩm.");
            String invoiceId = inv.substring(0, inv.indexOf(" - "));
            int quantity = ((Number)spnQty.getValue()).intValue();
            BigDecimal amount = service.calculateRefundAmount(detailId.longValue(), quantity);
            String message = "Xác nhận lập phiếu đổi trả?\nGiá trị hoàn: " + Formats.money(amount)
                    + (amount.compareTo(OperationService.REFUND_APPROVAL_LIMIT)>0 ? "\nPhiếu vượt hạn mức và sẽ chuyển Quản lý duyệt."
                       : "\nPhiếu trong hạn mức và sẽ được hoàn tiền ngay.");
            if (!confirm(message)) return;
            String id = service.createReturn(invoiceId, detailId.longValue(), quantity,
                    txtReason.getText().trim(), txtCondition.getText().trim(), chkSeal.isSelected(), chkAccessories.isSelected(), txtEvidence.getText().trim());
            info("Đã tạo phiếu " + id + ". " + (amount.compareTo(OperationService.REFUND_APPROVAL_LIMIT)>0
                    ? "Phiếu đang chờ Quản lý duyệt." : "Đã xử lý hoàn tiền."));
            txtReason.setText("");txtCondition.setText("");txtEvidence.setText("");spnQty.setValue(1);refreshData();
        } catch (Exception ex) { showError(ex); }
    }
}

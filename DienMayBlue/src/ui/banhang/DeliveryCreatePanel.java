package ui.banhang;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import javax.swing.*;
import service.OperationService;
import util.Formats;
import ui.dungchung.*;
public class DeliveryCreatePanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;
    private final OperationService service = new OperationService();
    private final JComboBox<String> cmbInvoice = new JComboBox<String>();
    private final JTextField txtCustomer = UIFactory.field(210);
    private final JTextField txtAddress = UIFactory.field(310);
    private final JComboBox<String> cmbRegion = UIFactory.combo("Nội thành", "Tỉnh / thành khác");
    private final JTextField txtDate = UIFactory.field(140);
    private final JTextField txtFrom = UIFactory.field(100);
    private final JTextField txtTo = UIFactory.field(100);
    private final JCheckBox chkInstall = new JCheckBox("Cần lắp đặt");
    private final JTextField txtFee = UIFactory.field(140);
    private final JTextArea txaNote = new JTextArea(4, 20);
    public DeliveryCreatePanel() {
        super("Lập phiếu giao hàng - lắp đặt", "Tạo phiếu ở trạng thái Đang chuẩn bị; việc phân công thực hiện tại mục Quản lý giao hàng.");
        JButton btnSave = UIFactory.primaryButton("Tạo phiếu giao hàng");
        JButton btnRefresh = UIFactory.secondaryButton("Làm mới");
        pnlActions.add(btnSave); pnlActions.add(btnRefresh);
        RoundedPanel card = new RoundedPanel(12, Color.WHITE);
        card.setBorderColor(UIConstants.BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        pnlBody.add(card, BorderLayout.CENTER);
        txtCustomer.setEditable(false);
        txtFee.setEditable(false);
        makeFlexible(cmbInvoice, 150);
        makeFlexible(txtCustomer, 120);
        makeFlexible(txtAddress, 150);
        makeFlexible(cmbRegion, 120);
        makeFlexible(txtDate, 110);
        makeFlexible(txtFrom, 80);
        makeFlexible(txtTo, 80);
        makeFlexible(txtFee, 100);
        txtFee.setBackground(new Color(248, 250, 252));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 7, 7, 7); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int y = 0;
        y = row(card, c, y, "Hóa đơn đã thanh toán", cmbInvoice, "Khách hàng", txtCustomer);
        y = row(card, c, y, "Địa chỉ giao hàng", txtAddress, "Khu vực", cmbRegion);
        y = row(card, c, y, "Ngày giao (yyyy-MM-dd)", txtDate, "Phí giao/lắp (hệ thống tính)", txtFee);
        y = row(card, c, y, "Từ giờ (HH:mm)", txtFrom, "Đến giờ (HH:mm)", txtTo);
        y = row(card, c, y, "Yêu cầu lắp đặt", chkInstall, "Trạng thái sau khi tạo", new JLabel("<html>Đang chuẩn bị /<br>chưa phân công</html>"));
        c.gridx = 0; c.gridy = y; c.weightx = 0; card.add(label("Ghi chú"), c);
        c.gridx = 1; c.gridwidth = 3; c.weightx = 1;
        txaNote.setLineWrap(true); txaNote.setWrapStyleWord(true); txaNote.setFont(UIConstants.NORMAL);
        txaNote.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIConstants.BORDER), BorderFactory.createEmptyBorder(8,8,8,8)));
        card.add(new JScrollPane(txaNote), c);
        cmbInvoice.addActionListener(e -> loadInvoiceCustomer());
        cmbRegion.addActionListener(e -> updateFee());
        chkInstall.addActionListener(e -> updateFee());
        btnRefresh.addActionListener(e -> refreshData());
        btnSave.addActionListener(e -> saveDelivery());
        refreshData();
    }
    private int row(JPanel pnlP, GridBagConstraints c, int y, String l1, Component v1, String l2, Component v2) {
        c.gridwidth = 1; c.gridy = y; c.weightx = 0; c.gridx = 0; pnlP.add(label(l1), c);
        c.weightx = .58; c.gridx = 1; pnlP.add(v1, c);
        c.weightx = 0; c.gridx = 2; pnlP.add(label(l2), c);
        c.weightx = .42; c.gridx = 3; pnlP.add(v2, c);
        return y + 1;
    }
    private void makeFlexible(JComponent component, int minWidth) {
        Dimension pref = component.getPreferredSize();
        component.setMinimumSize(new Dimension(minWidth, Math.max(32, pref.height)));
    }
    private JLabel label(String s) { JLabel lblL = new JLabel(s); lblL.setFont(UIConstants.MEDIUM); lblL.setForeground(UIConstants.MUTED); return lblL; }
    @Override public void refreshData() {
        try {
            cmbInvoice.removeAllItems();
            for (Map<String,Object> r : service.query(
                    "SELECT i.invoice_id,ISNULL(c.full_name,N'Khách vãng lai') customer,i.total_amount "
                  + "FROM invoices i LEFT JOIN customers c ON c.customer_id=i.customer_id "
                  + "WHERE i.status='PAID' AND NOT EXISTS(SELECT 1 FROM deliveries d WHERE d.invoice_id=i.invoice_id) "
                  + "ORDER BY i.created_at DESC"))
                cmbInvoice.addItem(r.get("invoice_id") + " - " + r.get("customer") + " - " + Formats.money(r.get("total_amount")));
            txtDate.setText(LocalDate.now().plusDays(1).toString());
            txtFrom.setText("09:00"); txtTo.setText("11:00");
            chkInstall.setSelected(false);
            txaNote.setText("");
            loadInvoiceCustomer();
            updateFee();
        } catch (Exception ex) { showError(ex); }
    }
    private void loadInvoiceCustomer() {
        String s = String.valueOf(cmbInvoice.getSelectedItem());
        if (s == null || s.equals("null") || !s.contains(" - ")) {
            txtCustomer.setText(""); txtAddress.setText(""); return;
        }
        String invoiceId = s.substring(0, s.indexOf(" - "));
        try {
            Map<String,Object> r = service.one(
                    "SELECT ISNULL(c.full_name,N'Khách vãng lai') customer,ISNULL(c.address,N'') address "
                  + "FROM invoices i LEFT JOIN customers c ON c.customer_id=i.customer_id WHERE i.invoice_id=?", invoiceId);
            txtCustomer.setText(r == null ? "" : String.valueOf(r.get("customer")));
            txtAddress.setText(r == null ? "" : String.valueOf(r.get("address")));
        } catch (Exception ex) { showError(ex); }
    }
    private void updateFee() {
        try {
            String regionCode = "Nội thành".equals(String.valueOf(cmbRegion.getSelectedItem())) ? "INNER_CITY" : "OTHER_PROVINCE";
            BigDecimal value = service.calculateDeliveryFee(regionCode, chkInstall.isSelected());
            txtFee.setText(Formats.money(value));
        } catch (Exception ex) { txtFee.setText(""); }
    }
    private void saveDelivery() {
        try {
            String inv = String.valueOf(cmbInvoice.getSelectedItem());
            if (inv == null || inv.equals("null") || !inv.contains(" - ")) throw new IllegalArgumentException("Chưa chọn hóa đơn.");
            String invoiceId = inv.substring(0, inv.indexOf(" - "));
            String regionCode = "Nội thành".equals(String.valueOf(cmbRegion.getSelectedItem())) ? "INNER_CITY" : "OTHER_PROVINCE";
            String id = service.createDelivery(invoiceId, txtAddress.getText().trim(), regionCode,
                    LocalDate.parse(txtDate.getText().trim()), LocalTime.parse(txtFrom.getText().trim()), LocalTime.parse(txtTo.getText().trim()),
                    chkInstall.isSelected(), txaNote.getText().trim());
            info("Đã tạo phiếu giao hàng " + id + ". Phiếu đang chờ phân công tại mục Quản lý giao hàng.");
            txtAddress.setText(""); txaNote.setText(""); refreshData();
        } catch (Exception ex) { showError(ex); }
    }
}

package app;

import bus.BusinessService;
import bus.Formats;
import dao.Db;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import javax.swing.*;


/** UC020 - Lập phiếu giao hàng - lắp đặt. */
public class PanelDeliveryCreate extends PanelBaseScreen {
    private static final long serialVersionUID = 1L;
    private final BusinessService service = new BusinessService();

    private final JComboBox<String> invoice = new JComboBox<String>();
    private final JTextField customer = UIFactory.field(210);
    private final JTextField address = UIFactory.field(310);
    private final JComboBox<String> region = UIFactory.combo("Nội thành", "Tỉnh / thành khác");
    private final JTextField date = UIFactory.field(140);
    private final JTextField from = UIFactory.field(100);
    private final JTextField to = UIFactory.field(100);
    private final JCheckBox install = new JCheckBox("Cần lắp đặt");
    private final JTextField fee = UIFactory.field(140);
    private final JTextArea note = new JTextArea(4, 20);

    public PanelDeliveryCreate() {
        super("Lập phiếu giao hàng - lắp đặt", "Tạo phiếu ở trạng thái Đang chuẩn bị; việc phân công thực hiện tại mục Quản lý giao hàng.");
        JButton save = UIFactory.primaryButton("Tạo phiếu giao hàng");
        JButton refresh = UIFactory.secondaryButton("Làm mới");
        actions.add(save); actions.add(refresh);

        RoundedPanel card = new RoundedPanel(12, Color.WHITE);
        card.setBorderColor(UIConstants.BORDER);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        body.add(card, BorderLayout.CENTER);

        customer.setEditable(false);
        fee.setEditable(false);
        makeFlexible(invoice, 150);
        makeFlexible(customer, 120);
        makeFlexible(address, 150);
        makeFlexible(region, 120);
        makeFlexible(date, 110);
        makeFlexible(from, 80);
        makeFlexible(to, 80);
        makeFlexible(fee, 100);
        fee.setBackground(new Color(248, 250, 252));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 7, 7, 7); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int y = 0;
        y = row(card, c, y, "Hóa đơn đã thanh toán", invoice, "Khách hàng", customer);
        y = row(card, c, y, "Địa chỉ giao hàng", address, "Khu vực", region);
        y = row(card, c, y, "Ngày giao (yyyy-MM-dd)", date, "Phí giao/lắp (hệ thống tính)", fee);
        y = row(card, c, y, "Từ giờ (HH:mm)", from, "Đến giờ (HH:mm)", to);
        y = row(card, c, y, "Yêu cầu lắp đặt", install, "Trạng thái sau khi tạo", new JLabel("<html>Đang chuẩn bị /<br>chưa phân công</html>"));
        c.gridx = 0; c.gridy = y; c.weightx = 0; card.add(label("Ghi chú"), c);
        c.gridx = 1; c.gridwidth = 3; c.weightx = 1;
        note.setLineWrap(true); note.setWrapStyleWord(true); note.setFont(UIConstants.NORMAL);
        note.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIConstants.BORDER), BorderFactory.createEmptyBorder(8,8,8,8)));
        card.add(new JScrollPane(note), c);

        invoice.addActionListener(e -> loadInvoiceCustomer());
        region.addActionListener(e -> updateFee());
        install.addActionListener(e -> updateFee());
        refresh.addActionListener(e -> refreshData());
        save.addActionListener(e -> saveDelivery());
        refreshData();
    }

    private int row(JPanel p, GridBagConstraints c, int y, String l1, Component v1, String l2, Component v2) {
        c.gridwidth = 1; c.gridy = y; c.weightx = 0; c.gridx = 0; p.add(label(l1), c);
        c.weightx = .58; c.gridx = 1; p.add(v1, c);
        c.weightx = 0; c.gridx = 2; p.add(label(l2), c);
        c.weightx = .42; c.gridx = 3; p.add(v2, c);
        return y + 1;
    }

    private void makeFlexible(JComponent component, int minWidth) {
        Dimension pref = component.getPreferredSize();
        component.setMinimumSize(new Dimension(minWidth, Math.max(32, pref.height)));
    }

    private JLabel label(String s) { JLabel l = new JLabel(s); l.setFont(UIConstants.MEDIUM); l.setForeground(UIConstants.MUTED); return l; }

    @Override public void refreshData() {
        try {
            invoice.removeAllItems();
            for (Map<String,Object> r : Db.query(
                    "SELECT i.invoice_id,ISNULL(c.full_name,N'Khách vãng lai') customer,i.total_amount "
                  + "FROM invoices i LEFT JOIN customers c ON c.customer_id=i.customer_id "
                  + "WHERE i.status='PAID' AND NOT EXISTS(SELECT 1 FROM deliveries d WHERE d.invoice_id=i.invoice_id) "
                  + "ORDER BY i.created_at DESC"))
                invoice.addItem(r.get("invoice_id") + " - " + r.get("customer") + " - " + Formats.money(r.get("total_amount")));

            date.setText(LocalDate.now().plusDays(1).toString());
            from.setText("09:00"); to.setText("11:00");
            install.setSelected(false);
            note.setText("");
            loadInvoiceCustomer();
            updateFee();
        } catch (Exception ex) { showError(ex); }
    }

    private void loadInvoiceCustomer() {
        String s = String.valueOf(invoice.getSelectedItem());
        if (s == null || s.equals("null") || !s.contains(" - ")) {
            customer.setText(""); address.setText(""); return;
        }
        String invoiceId = s.substring(0, s.indexOf(" - "));
        try {
            Map<String,Object> r = Db.one(
                    "SELECT ISNULL(c.full_name,N'Khách vãng lai') customer,ISNULL(c.address,N'') address "
                  + "FROM invoices i LEFT JOIN customers c ON c.customer_id=i.customer_id WHERE i.invoice_id=?", invoiceId);
            customer.setText(r == null ? "" : String.valueOf(r.get("customer")));
            address.setText(r == null ? "" : String.valueOf(r.get("address")));
        } catch (Exception ex) { showError(ex); }
    }

    private void updateFee() {
        try {
            String regionCode = "Nội thành".equals(String.valueOf(region.getSelectedItem())) ? "INNER_CITY" : "OTHER_PROVINCE";
            BigDecimal value = service.calculateDeliveryFee(regionCode, install.isSelected());
            fee.setText(Formats.money(value));
        } catch (Exception ex) { fee.setText(""); }
    }

    private void saveDelivery() {
        try {
            String inv = String.valueOf(invoice.getSelectedItem());
            if (inv == null || inv.equals("null") || !inv.contains(" - ")) throw new IllegalArgumentException("Chưa chọn hóa đơn.");
            String invoiceId = inv.substring(0, inv.indexOf(" - "));
            String regionCode = "Nội thành".equals(String.valueOf(region.getSelectedItem())) ? "INNER_CITY" : "OTHER_PROVINCE";
            String id = service.createDelivery(invoiceId, address.getText().trim(), regionCode,
                    LocalDate.parse(date.getText().trim()), LocalTime.parse(from.getText().trim()), LocalTime.parse(to.getText().trim()),
                    install.isSelected(), note.getText().trim());
            info("Đã tạo phiếu giao hàng " + id + ". Phiếu đang chờ phân công tại mục Quản lý giao hàng.");
            address.setText(""); note.setText(""); refreshData();
        } catch (Exception ex) { showError(ex); }
    }
}

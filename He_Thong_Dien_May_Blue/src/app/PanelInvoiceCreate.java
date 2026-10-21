package app;

import bus.BusinessService;
import bus.Formats;
import bus.SalesDraftBridge;
import bus.PhienDangNhap;
import dao.Db;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.Normalizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


/** UC019 - Lập hóa đơn bán hàng. */
public class PanelInvoiceCreate extends PanelBaseScreen {
    private static final long serialVersionUID = 1L;

    private final BusinessService service = new BusinessService();
    private final JComboBox<String> customer = new JComboBox<String>();
    private final JComboBox<String> product = new JComboBox<String>();
    private final JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JComboBox<String> payment = UIFactory.combo(UiText.labels("CASH", "BANK_TRANSFER"));
    private final JTextField cashReceived = UIFactory.field(180);
    private final JTextField transactionRef = UIFactory.field(220);
    private final JTextArea note = new JTextArea(3, 20);

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã SP", "Sản phẩm", "SL", "Đơn giá", "Khuyến mãi / ưu đãi", "Thành tiền"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = UIFactory.table(model);
    private final List<Line> lines = new ArrayList<Line>();
    private final List<String> customerOptions = new ArrayList<String>();
    private final List<String> productOptions = new ArrayList<String>();
    private boolean updatingSearchCombo = false;

    private final JLabel lblSelectedPrice = valueLabel("0đ");
    private final JLabel lblSelectedStock = valueLabel("-");
    private final JLabel lblCustomerName = valueLabel("Khách vãng lai");
    private final JLabel lblCustomerInfo = valueLabel("Không tích điểm thành viên");
    private final JLabel lblEmployee = valueLabel("");
    private final JLabel lblStatus = valueLabel("Chưa thanh toán");
    private final JLabel lblSubtotal = valueLabel("0đ");
    private final JLabel lblDiscount = valueLabel("0đ");
    private final JLabel lblTotal = new JLabel("0đ");
    private final JLabel lblChange = valueLabel("0đ");
    private final JLabel lblCashReceived = new JLabel("Tiền khách đưa");
    private final JLabel lblTransactionRef = new JLabel("Mã giao dịch");
    private final JLabel paymentHint = new JLabel();
    private final JButton btnPay = UIFactory.primaryButton("Xác nhận thanh toán");
    private final JButton btnRemoveLine = UIFactory.secondaryButton("Xóa dòng");
    private boolean sellingAllowed = false;

    public PanelInvoiceCreate() {
        super("Lập hóa đơn bán hàng", "Quét sản phẩm, kiểm tra khách hàng, áp dụng khuyến mãi, ưu đãi thành viên và xác nhận thanh toán.");

        JButton refresh = UIFactory.secondaryButton("Làm mới");
        JButton clear = UIFactory.secondaryButton("Hủy hóa đơn tạm");
        actions.add(btnPay); actions.add(refresh); actions.add(clear);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setOpaque(false);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(buildAddProductRow());
        top.add(Box.createVerticalStrut(12));
        top.add(buildInfoRow());
        root.add(top, BorderLayout.NORTH);

        RoundedPanel detailCard = new RoundedPanel(12, Color.WHITE);
        detailCard.setBorderColor(UIConstants.BORDER);
        detailCard.setLayout(new BorderLayout(0, 8));
        detailCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JLabel detailTitle = new JLabel("Chi tiết hóa đơn");
        detailTitle.setFont(UIConstants.SUBHEADING);
        detailTitle.setForeground(UIConstants.TEXT);

        JPanel detailHeader = new JPanel(new BorderLayout(8, 0));
        detailHeader.setOpaque(false);
        detailHeader.add(detailTitle, BorderLayout.WEST);
        detailCard.add(detailHeader, BorderLayout.NORTH);
        detailCard.add(UIFactory.scroll(table), BorderLayout.CENTER);
        detailCard.add(buildLineActions(), BorderLayout.SOUTH);
        root.add(detailCard, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 14, 0));
        bottom.setOpaque(false);
        bottom.add(buildPaymentCard());
        bottom.add(buildSummaryCard());
        root.add(bottom, BorderLayout.SOUTH);

        body.add(root, BorderLayout.CENTER);

        installSearchableCombo(customer, customerOptions, "Nhập mã hoặc tên khách hàng...");
        installSearchableCombo(product, productOptions, "Nhập mã hoặc tên sản phẩm...");

        refresh.addActionListener(e -> refreshData());
        clear.addActionListener(e -> clearInvoice());
        btnPay.addActionListener(e -> checkout());
        btnRemoveLine.addActionListener(e -> removeSelected());
        table.getSelectionModel().addListSelectionListener(e -> updateLineActionState());
        customer.addActionListener(e -> { if (!updatingSearchCombo) customerChanged(); });
        product.addActionListener(e -> { if (!updatingSearchCombo) updateSelectedProductInfo(); });
        payment.addActionListener(e -> updatePaymentFields());
        cashReceived.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateChange(); }
        });

        btnPay.setEnabled(false);
        btnRemoveLine.setEnabled(false);
        updatePaymentFields();
        refreshData();
    }

    private JPanel buildAddProductRow() {
        RoundedPanel p = new RoundedPanel(10, Color.WHITE);
        p.setBorderColor(UIConstants.BORDER);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel productRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        productRow.setOpaque(false);
        customer.setPreferredSize(new Dimension(205, 36));
        customer.setMinimumSize(new Dimension(120, 36));
        product.setPreferredSize(new Dimension(280, 36));
        product.setMinimumSize(new Dimension(160, 36));
        qty.setPreferredSize(new Dimension(58, 36));
        qty.setMinimumSize(new Dimension(52, 36));
        JButton add = UIFactory.primaryButton("+ Thêm sản phẩm");
        productRow.add(new JLabel("Khách hàng")); productRow.add(customer);
        productRow.add(new JLabel("Sản phẩm")); productRow.add(product);
        productRow.add(new JLabel("SL")); productRow.add(qty); productRow.add(add);

        JPanel infoRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 2));
        infoRow.setOpaque(false);
        JLabel priceTitle = new JLabel("Giá bán:");
        priceTitle.setFont(UIConstants.MEDIUM);
        lblSelectedPrice.setFont(UIConstants.font(Font.BOLD, 13));
        lblSelectedPrice.setForeground(UIConstants.ORANGE);
        JLabel stockTitle = new JLabel("Tồn kho:");
        stockTitle.setFont(UIConstants.MEDIUM);
        JLabel paymentTitle = new JLabel("Phương thức thanh toán:");
        paymentTitle.setFont(UIConstants.MEDIUM);
        payment.setPreferredSize(new Dimension(170, 36));
        payment.setMinimumSize(new Dimension(140, 36));
        infoRow.add(priceTitle); infoRow.add(lblSelectedPrice);
        infoRow.add(Box.createHorizontalStrut(8));
        infoRow.add(stockTitle); infoRow.add(lblSelectedStock);
        infoRow.add(Box.createHorizontalStrut(18));
        infoRow.add(paymentTitle); infoRow.add(payment);

        p.add(productRow);
        p.add(infoRow);
        add.addActionListener(e -> addProduct());
        return p;
    }

    private JPanel buildInfoRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);

        RoundedPanel customerCard = card();
        customerCard.setLayout(new GridLayout(0, 1, 0, 4));
        customerCard.add(sectionTitle("Thông tin khách hàng"));
        customerCard.add(lblCustomerName);
        customerCard.add(lblCustomerInfo);

        RoundedPanel invoiceCard = card();
        invoiceCard.setLayout(new GridLayout(0, 1, 0, 4));
        invoiceCard.add(sectionTitle("Thông tin hóa đơn"));
        invoiceCard.add(valueLabel("Mã hóa đơn: tạo khi thanh toán"));
        invoiceCard.add(lblEmployee);
        invoiceCard.add(lblStatus);

        row.add(customerCard); row.add(invoiceCard);
        return row;
    }

    private JPanel buildLineActions() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JButton minus = UIFactory.secondaryButton("− SL");
        JButton plus = UIFactory.secondaryButton("+ SL");
        minus.addActionListener(e -> adjustSelected(-1));
        plus.addActionListener(e -> adjustSelected(1));
        p.add(plus); p.add(minus); p.add(btnRemoveLine);
        return p;
    }

    private JPanel buildPaymentCard() {
        RoundedPanel p = card();
        p.setLayout(new BorderLayout(0, 8));
        p.add(sectionTitle("Thông tin thanh toán"), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 4, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        c.gridwidth = 1; c.weightx = 0; c.gridy = 0; c.gridx = 0;
        center.add(lblCashReceived, c);
        c.gridx = 1; c.weightx = 1;
        center.add(cashReceived, c);

        c.gridy = 1; c.gridx = 0; c.weightx = 0;
        center.add(new JLabel("Tiền thừa"), c);
        c.gridx = 1; c.weightx = 1;
        center.add(lblChange, c);

        c.gridy = 2; c.gridx = 0; c.weightx = 0;
        center.add(lblTransactionRef, c);
        c.gridx = 1; c.weightx = 1;
        center.add(transactionRef, c);

        JLabel noteLabel = new JLabel("Ghi chú thanh toán");
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2; c.weightx = 1;
        center.add(noteLabel, c);

        note.setLineWrap(true); note.setWrapStyleWord(true); note.setFont(UIConstants.NORMAL);
        note.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JScrollPane noteScroll = new JScrollPane(note);
        noteScroll.setPreferredSize(new Dimension(260, 62));
        noteScroll.setMinimumSize(new Dimension(160, 62));
        c.gridy = 4;
        center.add(noteScroll, c);

        paymentHint.setFont(UIConstants.SMALL);
        paymentHint.setForeground(UIConstants.MUTED);
        c.gridy = 5;
        center.add(paymentHint, c);

        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSummaryCard() {
        RoundedPanel p = card();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 2, 5, 2); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; p.add(sectionTitle("Tóm tắt thanh toán"), c);
        c.gridwidth = 1;
        addSummaryRow(p, c, 1, "Tạm tính", lblSubtotal);
        addSummaryRow(p, c, 2, "Giảm KM / ưu đãi", lblDiscount);
        JLabel totalTitle = new JLabel("Tổng thanh toán"); totalTitle.setFont(UIConstants.MEDIUM); totalTitle.setForeground(UIConstants.TEXT);
        lblTotal.setFont(UIConstants.font(Font.BOLD, 22)); lblTotal.setForeground(UIConstants.ORANGE);
        addSummaryRow(p, c, 3, totalTitle, lblTotal);
        JLabel point = new JLabel("Điểm thưởng và thưởng doanh số chỉ được ghi nhận khi thanh toán thành công.");
        point.setFont(UIConstants.SMALL); point.setForeground(UIConstants.MUTED);
        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; p.add(point, c);
        return p;
    }

    private void addSummaryRow(JPanel p, GridBagConstraints c, int y, String label, JLabel value) {
        JLabel l = new JLabel(label); l.setFont(UIConstants.NORMAL); l.setForeground(UIConstants.MUTED);
        addSummaryRow(p, c, y, l, value);
    }

    private void addSummaryRow(JPanel p, GridBagConstraints c, int y, JLabel label, JLabel value) {
        c.gridwidth = 1; c.gridy = y; c.gridx = 0; c.weightx = 1; p.add(label, c);
        c.gridx = 1; c.weightx = 0; p.add(value, c);
    }

    private RoundedPanel card() {
        RoundedPanel p = new RoundedPanel(12, Color.WHITE);
        p.setBorderColor(UIConstants.BORDER);
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text); l.setFont(UIConstants.SUBHEADING); l.setForeground(UIConstants.TEXT); return l;
    }

    private static JLabel valueLabel(String text) {
        JLabel l = new JLabel(text); l.setFont(UIConstants.NORMAL); l.setForeground(UIConstants.TEXT); return l;
    }

    @Override public void refreshData() {
        try {
            String oldCustomer = comboText(customer);
            String oldCustomerId = extractId(oldCustomer);

            customerOptions.clear();
            customerOptions.add("Khách vãng lai");
            for (Map<String,Object> r : Db.query(
                    "SELECT customer_id,full_name,phone FROM customers WHERE status='ACTIVE' ORDER BY full_name"))
                customerOptions.add(r.get("customer_id") + " - " + r.get("full_name") + " - " + Formats.safe(r.get("phone")));
            setComboOptions(customer, customerOptions);
            if (oldCustomerId != null) {
                if (!selectStartsWith(customer, oldCustomerId)) customer.setSelectedItem("Khách vãng lai");
            } else if (oldCustomer != null && oldCustomer.startsWith("Khách vãng lai")) {
                customer.setSelectedItem("Khách vãng lai");
            } else {
                customer.setSelectedItem("Khách vãng lai");
            }

            productOptions.clear();
            for (Map<String,Object> r : Db.query(
                    "SELECT product_id,product_name,sale_price,stock_quantity FROM products "
                  + "WHERE status='ACTIVE' AND stock_quantity>0 ORDER BY product_name"))
                productOptions.add(r.get("product_id") + " - " + r.get("product_name") + " | "
                        + Formats.money(r.get("sale_price")) + " | tồn " + r.get("stock_quantity"));
            setComboOptions(product, productOptions);
            clearComboSelection(product);
            updateSelectedProductInfo();

            String emp = PhienDangNhap.getTaiKhoanHienTai() == null ? "" : PhienDangNhap.getTaiKhoanHienTai().getNhanVien().getHoTen();
            lblEmployee.setText("Nhân viên: " + emp);
            updateCustomerInfo();
            importPendingItemsFromSearch();
            updateTotals();

            try {
                service.ensureCanSell();
                sellingAllowed = true;
                updatePayButtonState();
                updatePaymentFields();
            } catch (Exception ex) {
                sellingAllowed = false;
                updatePayButtonState();
                lblStatus.setText("Tạm khóa thanh toán • " + ex.getMessage());
            }
        } catch (Exception ex) { showError(ex); }
    }

    private boolean selectStartsWith(JComboBox<String> c, String prefix) {
        for (int i = 0; i < c.getItemCount(); i++) {
            if (String.valueOf(c.getItemAt(i)).startsWith(prefix + " -")) {
                c.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    private void installSearchableCombo(final JComboBox<String> combo, final List<String> source, String tooltip) {
        combo.setEditable(true);
        combo.setMaximumRowCount(10);
        Component component = combo.getEditor().getEditorComponent();
        if (!(component instanceof JTextField)) return;

        final JTextField editor = (JTextField) component;
        editor.setToolTipText(tooltip);
        editor.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> editor.selectAll());
            }
        });
        editor.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_ENTER
                        || key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_TAB) return;
                filterCombo(combo, source, editor.getText());
            }
        });
    }

    private void filterCombo(JComboBox<String> combo, List<String> source, String typedText) {
        if (updatingSearchCombo) return;
        String text = typedText == null ? "" : typedText;
        String query = normalizeSearch(text);
        DefaultComboBoxModel<String> filtered = new DefaultComboBoxModel<String>();
        for (String option : source) {
            if (query.isEmpty() || normalizeSearch(option).contains(query)) filtered.addElement(option);
        }

        updatingSearchCombo = true;
        try {
            combo.setModel(filtered);
            combo.setSelectedIndex(-1);
            Component component = combo.getEditor().getEditorComponent();
            if (component instanceof JTextField) {
                JTextField editor = (JTextField) component;
                editor.setText(text);
                editor.setCaretPosition(editor.getText().length());
            }
        } finally {
            updatingSearchCombo = false;
        }

        if (combo == product) {
            lblSelectedPrice.setText("0đ");
            lblSelectedStock.setText("-");
        } else if (combo == customer) {
            if (text.trim().isEmpty()) {
                lblCustomerName.setText("Khách vãng lai");
                lblCustomerInfo.setText("Không tích điểm thành viên");
            } else {
                lblCustomerName.setText("Chưa chọn khách hàng");
                lblCustomerInfo.setText("Chọn một khách hàng trong danh sách gợi ý");
            }
            // Khi đang gõ tìm khách hàng, bỏ ngay ưu đãi của lựa chọn cũ để số tiền không bị giữ sai.
            try {
                for (Line line : lines) recalc(line, loadSellableProduct(line.productId));
                rebuildTable();
            } catch (Exception ex) {
                showError(ex);
            }
        }

        if (combo.isShowing() && !text.trim().isEmpty() && filtered.getSize() > 0) {
            combo.setPopupVisible(true);
        } else if (filtered.getSize() == 0) {
            combo.setPopupVisible(false);
        }
    }

    private void setComboOptions(JComboBox<String> combo, List<String> source) {
        updatingSearchCombo = true;
        try {
            combo.setModel(new DefaultComboBoxModel<String>(source.toArray(new String[0])));
        } finally {
            updatingSearchCombo = false;
        }
    }

    private void clearComboSelection(JComboBox<String> combo) {
        updatingSearchCombo = true;
        try {
            combo.setSelectedIndex(-1);
            combo.getEditor().setItem("");
        } finally {
            updatingSearchCombo = false;
        }
    }

    private String comboText(JComboBox<String> combo) {
        if (combo.isEditable() && combo.getEditor() != null) {
            Object item = combo.getEditor().getItem();
            return item == null ? "" : String.valueOf(item).trim();
        }
        Object item = combo.getSelectedItem();
        return item == null ? "" : String.valueOf(item).trim();
    }

    private String extractId(String value) {
        if (value == null) return null;
        int split = value.indexOf(" - ");
        if (split <= 0) return null;
        return value.substring(0, split).trim();
    }

    private String normalizeSearch(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    private void customerChanged() {
        updateCustomerInfo();
        try {
            for (Line line : lines) recalc(line, loadSellableProduct(line.productId));
            rebuildTable();
        } catch (Exception ex) { showError(ex); }
    }

    private void updateCustomerInfo() {
        String s = comboText(customer);
        if (s.isEmpty() || s.startsWith("Khách vãng lai")) {
            lblCustomerName.setText("Khách vãng lai");
            lblCustomerInfo.setText("Không tích điểm thành viên");
            return;
        }
        if (!s.contains(" - ")) {
            lblCustomerName.setText("Chưa chọn khách hàng");
            lblCustomerInfo.setText("Chọn một khách hàng trong danh sách gợi ý");
            return;
        }
        try {
            String customerId = s.substring(0, s.indexOf(" - "));
            Map<String,Object> r = Db.one(
                    "SELECT c.full_name,c.phone,c.current_points,ISNULL(t.tier_name,N'Tiêu chuẩn') tier_name,"
                  + "ISNULL(t.discount_rate,0) discount_rate,ISNULL(t.point_multiplier,1) point_multiplier "
                  + "FROM customers c LEFT JOIN membership_tiers t ON t.tier_id=c.tier_id WHERE c.customer_id=?",
                    customerId);
            if (r == null) return;
            lblCustomerName.setText(String.valueOf(r.get("full_name")));
            lblCustomerInfo.setText(Formats.safe(r.get("phone")) + "  •  Hạng: " + r.get("tier_name")
                    + "  •  Điểm: " + r.get("current_points") + "  •  Ưu đãi: " + r.get("discount_rate") + "%"
                    + "  •  Hệ số điểm: " + r.get("point_multiplier"));
        } catch (Exception ex) { showError(ex); }
    }

    private void updateSelectedProductInfo() {
        String s = comboText(product);
        if (s.isEmpty() || !s.contains(" - ")) {
            lblSelectedPrice.setText("0đ");
            lblSelectedStock.setText("-");
            return;
        }
        try {
            String id = s.substring(0, s.indexOf(" - "));
            Map<String,Object> p = loadSellableProduct(id);
            lblSelectedPrice.setText(Formats.money(p.get("sale_price")));
            lblSelectedStock.setText(String.valueOf(p.get("stock_quantity")));
        } catch (Exception ex) {
            lblSelectedPrice.setText("0đ");
            lblSelectedStock.setText("-");
        }
    }

    private void addProduct() {
        String s = comboText(product);
        if (s.isEmpty() || !s.contains(" - ")) return;
        String id = s.substring(0, s.indexOf(" - "));
        try {
            Map<String,Object> p = loadSellableProduct(id);
            int q = ((Number) qty.getValue()).intValue();
            int stock = ((Number) p.get("stock_quantity")).intValue();
            if (q > stock) throw new IllegalArgumentException("Số lượng vượt tồn kho hiện tại (" + stock + ").");
            for (Line l : lines) if (l.productId.equals(id)) {
                if (l.qty + q > stock) throw new IllegalArgumentException("Tổng số lượng vượt tồn kho hiện tại (" + stock + ").");
                l.qty += q; recalc(l, p); rebuildTable(); return;
            }
            Line line = new Line();
            line.productId = id; line.name = String.valueOf(p.get("product_name")); line.qty = q;
            recalc(line, p); lines.add(line); rebuildTable();
        } catch (Exception ex) { showError(ex); }
    }

    private Map<String,Object> loadSellableProduct(String id) throws Exception {
        Map<String,Object> p = Db.one(
                "SELECT product_id,product_name,category_id,sale_price,stock_quantity,status FROM products WHERE product_id=?", id);
        if (p == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        if (!"ACTIVE".equals(String.valueOf(p.get("status"))) || ((Number)p.get("stock_quantity")).intValue() <= 0)
            throw new IllegalArgumentException("Sản phẩm đã hết hàng hoặc ngừng kinh doanh.");
        return p;
    }

    private void importPendingItemsFromSearch() {
        Map<String, Integer> items = SalesDraftBridge.consumePendingInvoiceItems();
        if (items == null || items.isEmpty()) return;
        try {
            resetInvoice();
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                addExternalProduct(entry.getKey(), entry.getValue().intValue());
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void addExternalProduct(String productId, int quantity) throws Exception {
        if (productId == null || quantity <= 0) return;
        Map<String,Object> p = loadSellableProduct(productId);
        int stock = ((Number) p.get("stock_quantity")).intValue();
        int q = Math.min(quantity, stock);
        for (Line l : lines) if (l.productId.equals(productId)) {
            int mergedQty = l.qty + q;
            if (mergedQty > stock) mergedQty = stock;
            l.qty = mergedQty;
            recalc(l, p);
            rebuildTable();
            return;
        }
        Line line = new Line();
        line.productId = productId;
        line.name = String.valueOf(p.get("product_name"));
        line.qty = q;
        recalc(line, p);
        lines.add(line);
        rebuildTable();
    }

    private void adjustSelected(int delta) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { info("Hãy chọn dòng sản phẩm cần chỉnh."); return; }
        int row = table.convertRowIndexToModel(viewRow);
        try {
            Line line = lines.get(row);
            int newQty = line.qty + delta;
            if (newQty <= 0) { lines.remove(row); rebuildTable(); return; }
            Map<String,Object> p = loadSellableProduct(line.productId);
            int stock = ((Number)p.get("stock_quantity")).intValue();
            if (newQty > stock) throw new IllegalArgumentException("Số lượng vượt tồn kho hiện tại (" + stock + ").");
            line.qty = newQty;
            recalc(line, p);
            rebuildTable();
        } catch (Exception ex) { showError(ex); }
    }

    private void removeSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { info("Hãy chọn dòng sản phẩm cần xóa."); return; }
        lines.remove(table.convertRowIndexToModel(viewRow));
        rebuildTable();
    }

    private void recalc(Line line, Map<String,Object> productRow) throws Exception {
        line.unitPrice = (BigDecimal) productRow.get("sale_price");
        BigDecimal gross = line.unitPrice.multiply(BigDecimal.valueOf(line.qty));
        line.discount = bestDiscount(line.productId, ((Number)productRow.get("category_id")).intValue(), gross, line.qty);
        line.total = gross.subtract(line.discount).max(BigDecimal.ZERO);
    }

    private BigDecimal bestDiscount(String productId, int categoryId, BigDecimal gross, int qty) throws Exception {
        BigDecimal bestPromotion = BigDecimal.ZERO;
        for (Map<String,Object> r : Db.query(
                "SELECT DISTINCT p.discount_type,p.discount_value FROM promotions p "
              + "LEFT JOIN promotion_products pp ON pp.promotion_id=p.promotion_id "
              + "LEFT JOIN promotion_categories pc ON pc.promotion_id=p.promotion_id "
              + "WHERE p.status='ACTIVE' AND SYSDATETIME() BETWEEN p.start_at AND p.end_at "
              + "AND (pp.product_id=? OR pc.category_id=?)", productId, categoryId)) {
            String type = String.valueOf(r.get("discount_type"));
            BigDecimal v = (BigDecimal) r.get("discount_value");
            BigDecimal d = "PERCENT".equals(type)
                    ? gross.multiply(v).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    : v.multiply(BigDecimal.valueOf(qty));
            if (d.compareTo(bestPromotion) > 0) bestPromotion = d;
        }
        BigDecimal memberRate=currentMemberDiscountRate();
        BigDecimal memberDiscount=gross.multiply(memberRate).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        return bestPromotion.max(memberDiscount).min(gross);
    }

    private BigDecimal currentMemberDiscountRate() throws Exception {
        String s = comboText(customer);
        if (s.isEmpty() || s.startsWith("Khách vãng lai") || !s.contains(" - ")) return BigDecimal.ZERO;
        String customerId=s.substring(0,s.indexOf(" - "));
        Object rate=Db.scalar("SELECT ISNULL(t.discount_rate,0) FROM customers c LEFT JOIN membership_tiers t ON t.tier_id=c.tier_id WHERE c.customer_id=?",customerId);
        return rate instanceof BigDecimal ? (BigDecimal)rate : new BigDecimal(String.valueOf(rate==null?0:rate));
    }

    private void rebuildTable() {
        model.setRowCount(0);
        for (Line l : lines)
            model.addRow(new Object[]{l.productId, l.name, l.qty, Formats.money(l.unitPrice),
                    l.discount.compareTo(BigDecimal.ZERO) > 0 ? "-" + Formats.money(l.discount) : "Không có",
                    Formats.money(l.total)});
        updateTotals();
        updateLineActionState();
    }

    private BigDecimal currentTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Line l : lines) total = total.add(l.total);
        return total;
    }

    private void updateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO, discount = BigDecimal.ZERO, total = BigDecimal.ZERO;
        for (Line l : lines) {
            subtotal = subtotal.add(l.unitPrice.multiply(BigDecimal.valueOf(l.qty)));
            discount = discount.add(l.discount);
            total = total.add(l.total);
        }
        lblSubtotal.setText(Formats.money(subtotal));
        lblDiscount.setText(discount.signum() == 0 ? "0đ" : "-" + Formats.money(discount));
        lblDiscount.setForeground(new Color(22, 163, 74));
        lblTotal.setText(Formats.money(total));
        syncDemoPaymentData();
        updateChange();
        updatePayButtonState();
        updateLineActionState();
    }

    private void updatePayButtonState() {
        boolean hasItems = !lines.isEmpty();
        btnPay.setEnabled(sellingAllowed && hasItems);
        if (!sellingAllowed) {
            btnPay.setToolTipText("Thanh toán hiện đang bị khóa.");
        } else if (!hasItems) {
            btnPay.setToolTipText("Thêm ít nhất một sản phẩm vào chi tiết hóa đơn để thanh toán.");
        } else {
            btnPay.setToolTipText(null);
        }
    }

    private void updateLineActionState() {
        btnRemoveLine.setEnabled(!lines.isEmpty() && table.getSelectedRow() >= 0);
    }

    private void updatePaymentFields() {
        String method = UiText.code(payment.getSelectedItem());
        boolean cash = "CASH".equals(method);
        boolean transfer = "BANK_TRANSFER".equals(method);

        // DEMO MODE: giữ các trường để minh họa quy trình nhưng người dùng không phải nhập tay.
        cashReceived.setEnabled(cash);
        cashReceived.setEditable(false);
        cashReceived.setFocusable(false);
        lblCashReceived.setEnabled(cash);
        lblChange.setEnabled(cash);

        transactionRef.setEnabled(transfer);
        transactionRef.setEditable(false);
        transactionRef.setFocusable(false);
        lblTransactionRef.setEnabled(transfer);

        if (!cash) { cashReceived.setText(""); lblChange.setText("0đ"); }
        if (!transfer) transactionRef.setText("");
        syncDemoPaymentData();

        if (cash) {
            paymentHint.setText("Chế độ demo: hệ thống tự coi khách đã đưa đủ đúng số tiền cần thanh toán.");
        } else if (transfer) {
            paymentHint.setText("Chế độ demo: hệ thống tự sinh mã giao dịch và mô phỏng chuyển khoản thành công.");
        } else {
            paymentHint.setText("Chế độ demo thanh toán.");
        }

        if (sellingAllowed)
            lblStatus.setText("Chưa thanh toán • " + (cash ? "Tiền mặt" : "Chuyển khoản"));
    }

    private void syncDemoPaymentData() {
        String method = UiText.code(payment.getSelectedItem());
        if ("CASH".equals(method)) {
            BigDecimal total = currentTotal();
            cashReceived.setText(total.setScale(0, RoundingMode.HALF_UP).toPlainString());
            lblChange.setText("0đ");
        } else if ("BANK_TRANSFER".equals(method)) {
            String current = transactionRef.getText() == null ? "" : transactionRef.getText().trim();
            if (current.isEmpty() || !current.startsWith("DEMO-CK-"))
                transactionRef.setText(newDemoTransactionRef());
        }
    }

    private String newDemoTransactionRef() {
        return "DEMO-CK-" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private void updateChange() {
        if (!"CASH".equals(UiText.code(payment.getSelectedItem()))) { lblChange.setText("0đ"); return; }
        try {
            BigDecimal received = parseMoney(cashReceived.getText());
            BigDecimal change = received.subtract(currentTotal());
            lblChange.setText(change.signum() < 0 ? "Thiếu " + Formats.money(change.abs()) : Formats.money(change));
        } catch (Exception ex) {
            lblChange.setText("0đ");
        }
    }

    private BigDecimal parseMoney(String text) {
        String s = text == null ? "" : text.trim().replace(".", "").replace(",", "").replace(" ", "");
        if (s.isEmpty()) return BigDecimal.ZERO;
        if (!s.matches("\\d+")) throw new IllegalArgumentException("Số tiền phải là số nguyên dương.");
        return new BigDecimal(s);
    }

    private void clearInvoice() {
        if (!lines.isEmpty() && !confirm("Hủy toàn bộ hóa đơn tạm hiện tại?")) return;
        resetInvoice();
    }

    private void resetInvoice() {
        lines.clear();
        model.setRowCount(0);
        note.setText("");
        cashReceived.setText("");
        transactionRef.setText("");
        qty.setValue(1);
        updateTotals();
    }

    private void checkout() {
        if (lines.isEmpty() || !sellingAllowed) return;
        try {
            service.ensureCanSell();
            syncDemoPaymentData();
            showInvoicePreview();
        } catch (Exception ex) { showError(ex); }
    }

    /**
     * Bước xem trước hóa đơn. Chưa ghi dữ liệu vào database cho tới khi người dùng
     * bấm nút Xác nhận trong hộp thoại này.
     */
    private void showInvoicePreview() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        final JDialog dialog = new JDialog(owner, "Hóa đơn điện tử", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UIConstants.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        RoundedPanel invoice = new RoundedPanel(14, Color.WHITE);
        invoice.setBorderColor(UIConstants.BORDER);
        invoice.setLayout(new BorderLayout(0, 12));
        invoice.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        RoundedPanel logo = new RoundedPanel(10, UIConstants.ORANGE);
        logo.setDrawBorder(false);
        logo.setPreferredSize(new Dimension(34, 34));
        logo.setMinimumSize(new Dimension(34, 34));
        logo.setMaximumSize(new Dimension(34, 34));
        logo.setLayout(new GridBagLayout());
        JLabel b = new JLabel("B");
        b.setForeground(Color.WHITE);
        b.setFont(UIConstants.font(Font.BOLD, 15));
        logo.add(b);
        JLabel store = new JLabel("Điện Máy Blue");
        store.setFont(UIConstants.font(Font.BOLD, 15));
        store.setForeground(UIConstants.TEXT);
        store.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        brand.add(logo);
        brand.add(store);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("HÓA ĐƠN BÁN HÀNG ĐIỆN TỬ");
        title.setFont(UIConstants.font(Font.BOLD, 22));
        title.setForeground(UIConstants.TEXT);
        title.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel preview = new JLabel("BẢN XEM TRƯỚC • Chưa ghi nhận thanh toán");
        preview.setFont(UIConstants.SMALL);
        preview.setForeground(UIConstants.ORANGE);
        preview.setAlignmentX(Component.RIGHT_ALIGNMENT);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(preview);

        header.add(brand, BorderLayout.WEST);
        header.add(titleBox, BorderLayout.EAST);
        invoice.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel meta = new JPanel(new GridLayout(0, 2, 16, 8));
        meta.setOpaque(false);
        meta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));
        meta.add(invoiceInfo("Mã hóa đơn", "Tạo sau khi xác nhận"));
        meta.add(invoiceInfo("Ngày lập", java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        meta.add(invoiceInfo("Người mua", lblCustomerName.getText()));
        meta.add(invoiceInfo("Nhân viên bán hàng", currentEmployeeName()));
        meta.add(invoiceInfo("Thông tin khách hàng", lblCustomerInfo.getText()));
        meta.add(invoiceInfo("Phương thức thanh toán", String.valueOf(payment.getSelectedItem())));
        center.add(meta);
        center.add(Box.createVerticalStrut(12));

        DefaultTableModel previewModel = new DefaultTableModel(
                new Object[]{"STT", "Mã SP", "Tên hàng hóa", "SL", "Đơn giá", "Giảm", "Thành tiền"}, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        int stt = 1;
        for (Line l : lines) {
            previewModel.addRow(new Object[]{stt++, l.productId, l.name, l.qty,
                    Formats.money(l.unitPrice),
                    l.discount.signum() == 0 ? "0đ" : "-" + Formats.money(l.discount),
                    Formats.money(l.total)});
        }
        JTable previewTable = UIFactory.table(previewModel);
        previewTable.setAutoCreateRowSorter(false);
        previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        previewTable.setRowHeight(32);
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(38);
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(68);
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        previewTable.getColumnModel().getColumn(3).setPreferredWidth(42);
        previewTable.getColumnModel().getColumn(4).setPreferredWidth(92);
        previewTable.getColumnModel().getColumn(5).setPreferredWidth(78);
        previewTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        JScrollPane tableScroll = UIFactory.scroll(previewTable);
        tableScroll.setPreferredSize(new Dimension(680, Math.min(190, 44 + lines.size() * 32)));
        center.add(tableScroll);
        center.add(Box.createVerticalStrut(10));

        JPanel totals = new JPanel(new GridBagLayout());
        totals.setOpaque(false);
        GridBagConstraints tc = new GridBagConstraints();
        tc.insets = new Insets(3, 8, 3, 0);
        tc.anchor = GridBagConstraints.EAST;
        tc.gridx = 0; tc.gridy = 0; tc.weightx = 1;
        JLabel subTitle = new JLabel("Tạm tính:"); subTitle.setForeground(UIConstants.MUTED); subTitle.setFont(UIConstants.NORMAL);
        totals.add(subTitle, tc);
        tc.gridx = 1; tc.weightx = 0;
        JLabel subValue = new JLabel(Formats.money(currentSubtotal())); subValue.setFont(UIConstants.MEDIUM); subValue.setForeground(UIConstants.TEXT);
        totals.add(subValue, tc);

        tc.gridy = 1; tc.gridx = 0; tc.weightx = 1;
        JLabel disTitle = new JLabel("Khuyến mãi / ưu đãi:"); disTitle.setForeground(UIConstants.MUTED); disTitle.setFont(UIConstants.NORMAL);
        totals.add(disTitle, tc);
        tc.gridx = 1; tc.weightx = 0;
        JLabel disValue = new JLabel(currentDiscount().signum() == 0 ? "0đ" : "-" + Formats.money(currentDiscount()));
        disValue.setFont(UIConstants.MEDIUM); disValue.setForeground(new Color(22, 163, 74));
        totals.add(disValue, tc);

        tc.gridy = 2; tc.gridx = 0; tc.weightx = 1;
        JLabel totalTitle = new JLabel("TỔNG THANH TOÁN:"); totalTitle.setFont(UIConstants.font(Font.BOLD, 14)); totalTitle.setForeground(UIConstants.TEXT);
        totals.add(totalTitle, tc);
        tc.gridx = 1; tc.weightx = 0;
        JLabel totalValue = new JLabel(Formats.money(currentTotal())); totalValue.setFont(UIConstants.font(Font.BOLD, 20)); totalValue.setForeground(UIConstants.ORANGE);
        totals.add(totalValue, tc);
        center.add(totals);

        String noteText = note.getText() == null ? "" : note.getText().trim();
        if (!noteText.isEmpty()) {
            JLabel notePreview = new JLabel("<html><b>Ghi chú:</b> " + escapeHtml(noteText) + "</html>");
            notePreview.setFont(UIConstants.SMALL);
            notePreview.setForeground(UIConstants.MUTED);
            notePreview.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));
            center.add(notePreview);
        }

        invoice.add(center, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        JLabel message = new JLabel("Vui lòng kiểm tra thông tin trước khi xác nhận thanh toán.");
        message.setFont(UIConstants.SMALL);
        message.setForeground(UIConstants.MUTED);
        message.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        foot.add(message, BorderLayout.WEST);
        invoice.add(foot, BorderLayout.SOUTH);

        ViewportWidthPanel invoiceHost = new ViewportWidthPanel();
        invoiceHost.setLayout(new BorderLayout());
        invoiceHost.setBackground(UIConstants.BACKGROUND);
        invoiceHost.add(invoice, BorderLayout.NORTH);

        JScrollPane invoiceScroll = new JScrollPane(invoiceHost,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        invoiceScroll.setBorder(null);
        invoiceScroll.getViewport().setBackground(UIConstants.BACKGROUND);
        invoiceScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(invoiceScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = UIFactory.secondaryButton("Hủy");
        JButton confirm = UIFactory.primaryButton("Xác nhận");
        buttons.add(cancel);
        buttons.add(confirm);
        root.add(buttons, BorderLayout.SOUTH);

        cancel.addActionListener(e -> dialog.dispose());
        confirm.addActionListener(e -> completePayment(dialog));
        dialog.getRootPane().setDefaultButton(confirm);
        dialog.setContentPane(root);
        dialog.setSize(860, 610);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private JPanel invoiceInfo(String label, String value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(UIConstants.SMALL);
        l.setForeground(UIConstants.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        String display = value == null || value.trim().isEmpty() ? "-" : value;
        JLabel v = new JLabel("<html><div style='width:260px'>" + escapeHtml(display) + "</div></html>");
        v.setFont(UIConstants.NORMAL);
        v.setForeground(UIConstants.TEXT);
        v.setAlignmentX(Component.LEFT_ALIGNMENT);
        v.setToolTipText(display);

        p.add(l);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        return p;
    }

    /** Giữ hóa đơn luôn bám theo chiều rộng viewport để không bị tràn ngang. */
    private static class ViewportWidthPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 100; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private String currentEmployeeName() {
        try {
            if (PhienDangNhap.getTaiKhoanHienTai() != null
                    && PhienDangNhap.getTaiKhoanHienTai().getNhanVien() != null)
                return PhienDangNhap.getTaiKhoanHienTai().getNhanVien().getHoTen();
        } catch (Exception ignore) {}
        return "-";
    }

    private BigDecimal currentSubtotal() {
        BigDecimal value = BigDecimal.ZERO;
        for (Line l : lines) value = value.add(l.unitPrice.multiply(BigDecimal.valueOf(l.qty)));
        return value;
    }

    private BigDecimal currentDiscount() {
        BigDecimal value = BigDecimal.ZERO;
        for (Line l : lines) value = value.add(l.discount);
        return value;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Thực sự ghi hóa đơn/thanh toán sau khi người dùng xác nhận bản xem trước. */
    private void completePayment(JDialog previewDialog) {
        try {
            service.ensureCanSell();

            String method = UiText.code(payment.getSelectedItem());
            BigDecimal total = currentTotal();
            BigDecimal received = BigDecimal.ZERO;
            String ref = null;

            if ("CASH".equals(method)) {
                received = total;
                cashReceived.setText(total.setScale(0, RoundingMode.HALF_UP).toPlainString());
            } else if ("BANK_TRANSFER".equals(method)) {
                ref = transactionRef.getText() == null ? "" : transactionRef.getText().trim();
                if (ref.isEmpty()) {
                    ref = newDemoTransactionRef();
                    transactionRef.setText(ref);
                }
            }

            List<BusinessService.InvoiceItem> items = new ArrayList<BusinessService.InvoiceItem>();
            for (Line l : lines) items.add(new BusinessService.InvoiceItem(l.productId, l.qty));
            String cs = comboText(customer);
            String customerId = (cs.isEmpty() || cs.startsWith("Khách vãng lai") || !cs.contains(" - "))
                    ? null : cs.substring(0, cs.indexOf(" - "));

            String id = service.createInvoice(customerId, items, method, ref, note.getText().trim());
            previewDialog.dispose();

            String message = "Thanh toán demo thành công. Mã hóa đơn: " + id;
            if ("CASH".equals(method)) {
                message += "\nTiền mặt mô phỏng: " + Formats.money(received)
                        + "\nTiền thừa: " + Formats.money(received.subtract(total));
            } else if ("BANK_TRANSFER".equals(method)) {
                message += "\nMã giao dịch mô phỏng: " + ref;
            }
            info(message);
            resetInvoice();
            refreshData();
        } catch (Exception ex) { showError(ex); }
    }

    private static class Line {
        String productId, name;
        int qty;
        BigDecimal unitPrice = BigDecimal.ZERO, discount = BigDecimal.ZERO, total = BigDecimal.ZERO;
    }
}

package ui.sanpham;
import service.SalesService;
import service.ProductService;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import util.Formats;
import ui.dungchung.*;
import ui.chinh.MainFrame;
public class ProductSearchPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;
    private final ProductService service = new ProductService();
    private final MainFrame mainFrame;
    private final JTextField txtSearch = new JTextField();
    private final JPanel pnlCategoryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JPanel pnlProductGrid = new JPanel(new GridLayout(0, 2, 16, 16));
    private final JScrollPane scrProduct = new JScrollPane(pnlProductGrid);
    private final JButton btnCheckout = UIFactory.primaryButton("Thanh toán");
    private final JButton btnAddToDraft = UIFactory.secondaryButton("Giỏ hàng");
    private final JButton btnDraft = UIFactory.primaryButton("Đơn tạm  0");
    private Integer selectedCategoryId = null;
    private JToggleButton tglSelectedCategory;
    private String selectedProductId;
    private String selectedProductName;
    private Number selectedProductPrice;
    private int selectedProductStock;
    private RoundedPanel selectedCard;
    private final DefaultTableModel draftModel = UIFactory.readOnlyModel(new Object[]{"Mã SP", "Tên sản phẩm", "SL", "Đơn giá", "Thành tiền"});
    public ProductSearchPanel(MainFrame mainFrame) {
        super("Tìm sản phẩm", "0 sản phẩm trong danh mục");
        this.mainFrame = mainFrame;
        btnCheckout.setEnabled(false);
        btnAddToDraft.setEnabled(false);
        actions.add(btnCheckout);
        actions.add(btnAddToDraft);
        actions.add(btnDraft);
        body.setLayout(new BorderLayout(0, 15));
        JPanel pnlFilters = new JPanel();
        pnlFilters.setOpaque(false);
        pnlFilters.setLayout(new BoxLayout(pnlFilters, BoxLayout.Y_AXIS));
        pnlFilters.add(buildSearchBox());
        pnlFilters.add(Box.createVerticalStrut(15));
        pnlCategoryBar.setOpaque(false);
        pnlFilters.add(pnlCategoryBar);
        body.add(pnlFilters, BorderLayout.NORTH);
        pnlProductGrid.setOpaque(false);
        pnlProductGrid.setBorder(new EmptyBorder(0, 0, 8, 8));
        scrProduct.setBorder(null);
        scrProduct.setOpaque(false);
        scrProduct.getViewport().setOpaque(false);
        scrProduct.getVerticalScrollBar().setUnitIncrement(18);
        scrProduct.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        body.add(scrProduct, BorderLayout.CENTER);
        btnAddToDraft.addActionListener(e -> addSelectedToDraft());
        btnCheckout.addActionListener(e -> checkoutSelectedProduct());
        btnDraft.addActionListener(e -> showDraftDialog());
        UIFactory.onTextChange(txtSearch, this::refreshData);
        loadCategories();
        refreshData();
    }
    private JComponent buildSearchBox() {
        RoundedPanel wrap = new RoundedPanel(8, Color.WHITE);
        wrap.setLayout(new BorderLayout(8, 0));
        wrap.setPreferredSize(new Dimension(680, 38));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER), new EmptyBorder(0, 11, 0, 11)));
        JLabel lblSearchIcon = new JLabel("⌕");
        lblSearchIcon.setFont(UIConstants.font(Font.PLAIN, 22));
        lblSearchIcon.setForeground(UIConstants.MUTED);
        txtSearch.setBorder(null);
        txtSearch.setOpaque(false);
        txtSearch.setFont(UIConstants.font(Font.PLAIN, 12));
        txtSearch.setForeground(UIConstants.TEXT);
        txtSearch.setToolTipText("Nhập tên sản phẩm, mã SP, mã vạch hoặc danh mục để tìm kiếm");
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên sản phẩm, mã SP, danh mục để tìm kiếm...");
        wrap.add(lblSearchIcon, BorderLayout.WEST);
        wrap.add(txtSearch, BorderLayout.CENTER);
        JPanel pnlRow = new JPanel(new BorderLayout());
        pnlRow.setOpaque(false);
        pnlRow.add(wrap, BorderLayout.CENTER);
        return pnlRow;
    }
    private void loadCategories() {
        pnlCategoryBar.removeAll();
        ButtonGroup group = new ButtonGroup();
        JToggleButton tglAll = categoryChip("Tất cả", null);
        group.add(tglAll);
        pnlCategoryBar.add(tglAll);
        tglAll.setSelected(true);
        styleChip(tglAll, true, false);
        tglSelectedCategory = tglAll;
        try {
            for (Map<String, Object> r : service.query(
                    "SELECT category_id,category_name FROM categories WHERE active=1 ORDER BY category_name")) {
                Integer id = ((Number) r.get("category_id")).intValue();
                JToggleButton tglCategory = categoryChip(String.valueOf(r.get("category_name")), id);
                group.add(tglCategory);
                pnlCategoryBar.add(tglCategory);
            }
        } catch (Exception ex) {
            showError(ex);
        }
        pnlCategoryBar.revalidate();
        pnlCategoryBar.repaint();
    }
    private JToggleButton categoryChip(String text, Integer id) {
        final JToggleButton tglChip = new JToggleButton(text);
        tglChip.setFont(UIConstants.font(Font.BOLD, 11));
        tglChip.setFocusPainted(false);
        tglChip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tglChip.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI());
        tglChip.setRolloverEnabled(false);
        tglChip.setContentAreaFilled(true);
        tglChip.setBorderPainted(true);
        tglChip.setBorder(new EmptyBorder(8, 13, 8, 13));
        tglChip.setOpaque(true);
        styleChip(tglChip, false, false);
        tglChip.addActionListener(e -> {
            if (tglSelectedCategory != null) styleChip(tglSelectedCategory, false, false);
            tglSelectedCategory = tglChip;
            selectedCategoryId = id;
            styleChip(tglChip, true, false);
            refreshData();
        });
        tglChip.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (tglChip != tglSelectedCategory) styleChip(tglChip, false, true);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (tglChip != tglSelectedCategory) styleChip(tglChip, false, false);
            }
        });
        return tglChip;
    }
    private void styleChip(AbstractButton b, boolean selected, boolean hovered) {
        if (selected || hovered) {
            b.setBackground(UIConstants.ORANGE);
            b.setForeground(Color.WHITE);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.ORANGE), new EmptyBorder(7, 12, 7, 12)));
        } else {
            b.setBackground(Color.WHITE);
            b.setForeground(UIConstants.MUTED);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.BORDER), new EmptyBorder(7, 12, 7, 12)));
        }
    }
    @Override
    public void refreshData() {
        try {
            clearSelection();
            String kw = "%" + txtSearch.getText().trim() + "%";
            String sql = "SELECT p.product_id,p.product_name,p.sale_price,p.stock_quantity,p.category_id," +
                    "c.category_name,p.barcode,p.status " + "FROM products p JOIN categories c ON c.category_id=p.category_id " +
                    "WHERE p.status='ACTIVE' " +
                    "AND (p.product_id LIKE ? OR p.barcode LIKE ? OR p.product_name LIKE ? OR c.category_name LIKE ?) " +
                    (selectedCategoryId == null ? "" : "AND p.category_id=? ") + "ORDER BY p.product_name";
            List<Map<String, Object>> rows = selectedCategoryId == null ? service.query(sql, kw, kw, kw, kw)
                    : service.query(sql, kw, kw, kw, kw, selectedCategoryId);
            pnlProductGrid.removeAll();
            for (Map<String, Object> r : rows) pnlProductGrid.add(productCard(r));
            subtitleLabel.setText(rows.size() + " sản phẩm trong danh mục");
            pnlProductGrid.revalidate();
            pnlProductGrid.repaint();
            SwingUtilities.invokeLater(() -> scrProduct.getVerticalScrollBar().setValue(0));
        } catch (Exception ex) {
            showError(ex);
        }
    }
    private JComponent productCard(Map<String, Object> r) {
        final String id = String.valueOf(r.get("product_id"));
        final String name = String.valueOf(r.get("product_name"));
        final Number price = (Number) r.get("sale_price");
        final int stock = ((Number) r.get("stock_quantity")).intValue();
        final String categoryName = String.valueOf(r.get("category_name"));
        final RoundedPanel card = new RoundedPanel(12, Color.WHITE);
        card.setPreferredSize(new Dimension(350, 205));
        card.setMinimumSize(new Dimension(260, 205));
        card.setLayout(new BorderLayout());
        card.setBorderColor(UIConstants.BORDER);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Chọn sản phẩm để thêm vào giỏ hàng hoặc thanh toán");
        JPanel pnlVisual = new JPanel(new GridBagLayout());
        pnlVisual.setPreferredSize(new Dimension(10, 86));
        pnlVisual.setBackground(categoryColor(categoryName));
        JLabel lblIcon = new JLabel("▣");
        lblIcon.setFont(UIConstants.font(Font.BOLD, 26));
        lblIcon.setForeground(categoryAccent(categoryName));
        pnlVisual.add(lblIcon);
        JPanel pnlInfo = new JPanel(null);
        pnlInfo.setOpaque(false);
        pnlInfo.setPreferredSize(new Dimension(10, 119));
        JLabel lblCode = label(id, Font.BOLD, 10, UIConstants.MUTED);
        lblCode.setBounds(13, 12, 130, 15);
        pnlInfo.add(lblCode);
        JLabel lblName = label(name, Font.BOLD, 13, UIConstants.TEXT);
        lblName.setBounds(13, 31, 320, 22);
        pnlInfo.add(lblName);
        JLabel lblPrice = label(Formats.money(price), Font.BOLD, 14, UIConstants.ORANGE);
        lblPrice.setBounds(13, 73, 190, 20);
        pnlInfo.add(lblPrice);
        JLabel lblStock = label("Tồn: " + stock, Font.PLAIN, 11, UIConstants.MUTED);
        lblStock.setHorizontalAlignment(SwingConstants.RIGHT);
        lblStock.setBounds(245, 75, 88, 18);
        pnlInfo.add(lblStock);
        if (stock <= 0) {
            JLabel lblOutOfStock = label("Hết hàng", Font.BOLD, 11, new Color(220, 38, 38));
            lblOutOfStock.setBounds(245, 12, 88, 18);
            lblOutOfStock.setHorizontalAlignment(SwingConstants.RIGHT);
            pnlInfo.add(lblOutOfStock);
        }
        card.add(pnlVisual, BorderLayout.NORTH);
        card.add(pnlInfo, BorderLayout.CENTER);
        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectProduct(card, id, name, price, stock);
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (card != selectedCard) applyCardStyle(card, true, false);
            }
            @Override public void mouseExited(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (card != selectedCard && card.getMousePosition() == null)
                        applyCardStyle(card, false, false);
                });
            }
        };
        installMouseHandler(card, adapter);
        return card;
    }
    private void installMouseHandler(Component component, MouseAdapter a) {
        component.addMouseListener(a);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                installMouseHandler(child, a);
            }
        }
    }
    private void selectProduct(RoundedPanel card, String id, String name, Number price, int stock) {
        if (selectedCard != null && selectedCard != card) applyCardStyle(selectedCard, false, false);
        selectedCard = card;
        selectedProductId = id;
        selectedProductName = name;
        selectedProductPrice = price;
        selectedProductStock = stock;
        applyCardStyle(card, true, true);
        boolean canUse = stock > 0;
        btnAddToDraft.setEnabled(canUse);
        btnCheckout.setEnabled(canUse);
    }
    private void clearSelection() {
        if (selectedCard != null) applyCardStyle(selectedCard, false, false);
        selectedCard = null;
        selectedProductId = null;
        selectedProductName = null;
        selectedProductPrice = null;
        selectedProductStock = 0;
        btnAddToDraft.setEnabled(false);
        btnCheckout.setEnabled(false);
    }
    private void applyCardStyle(RoundedPanel card, boolean highlighted, boolean selected) {
        if (selected) {
            card.setFillColor(new Color(255, 247, 237));
            card.setBorderColor(UIConstants.ORANGE);
        } else if (highlighted) {
            card.setFillColor(new Color(255, 251, 245));
            card.setBorderColor(new Color(251, 146, 60));
        } else {
            card.setFillColor(Color.WHITE);
            card.setBorderColor(UIConstants.BORDER);
        }
        card.repaint();
    }
    private JLabel label(String text, int style, int size, Color color) {
        JLabel lblText = new JLabel(text);
        lblText.setFont(UIConstants.font(style, size));
        lblText.setForeground(color);
        return lblText;
    }
    private Color categoryColor(String category) {
        String s = category.toLowerCase(Locale.ROOT);
        if (s.contains("laptop")) return new Color(245, 242, 255);
        if (s.contains("tv") || s.contains("tivi")) return new Color(250, 242, 255);
        if (s.contains("điện lạnh") || s.contains("dien lanh")) return new Color(240, 247, 255);
        if (s.contains("gia dụng") || s.contains("gia dung")) return new Color(255, 247, 237);
        return new Color(240, 247, 255);
    }
    private Color categoryAccent(String category) {
        String s = category.toLowerCase(Locale.ROOT);
        if (s.contains("laptop")) return new Color(124, 58, 237);
        if (s.contains("tv") || s.contains("tivi")) return UIConstants.ORANGE;
        return new Color(37, 99, 235);
    }
    private void addSelectedToDraft() {
        if (selectedProductId == null) return;
        addToDraft(selectedProductId, selectedProductName, selectedProductPrice, selectedProductStock);
    }
    private void checkoutSelectedProduct() {
        if (selectedProductId == null) return;
        if (selectedProductStock <= 0) {
            info("Sản phẩm đã hết hàng.");
            return;
        }
        LinkedHashMap<String, Integer> items = new LinkedHashMap<String, Integer>();
        items.put(selectedProductId, Integer.valueOf(1));
        SalesService.setDraftItems(items);
        mainFrame.showScreen(MainFrame.SCREEN_INVOICE_CREATE, "Lập hóa đơn bán hàng");
    }
    private void addToDraft(String id, String name, Number price, int stock) {
        if (stock <= 0) {
            info("Sản phẩm đã hết hàng.");
            return;
        }
        for (int i = 0; i < draftModel.getRowCount(); i++) {
            if (id.equals(String.valueOf(draftModel.getValueAt(i, 0)))) {
                int q = ((Number) draftModel.getValueAt(i, 2)).intValue();
                if (q >= stock) {
                    info("Số lượng trong đơn tạm đã bằng tồn kho hiện tại.");
                    return;
                }
                int newQty = q + 1;
                draftModel.setValueAt(Integer.valueOf(newQty), i, 2);
                draftModel.setValueAt(Formats.money(price.doubleValue() * newQty), i, 4);
                updateDraftCount();
                info("Đã thêm sản phẩm vào đơn tạm.");
                return;
            }
        }
        draftModel.addRow(new Object[]{id, name, Integer.valueOf(1), Formats.money(price), Formats.money(price)});
        updateDraftCount();
        info("Đã thêm sản phẩm vào đơn tạm.");
    }
    private void updateDraftCount() {
        int count = 0;
        for (int i = 0; i < draftModel.getRowCount(); i++)
            count += ((Number) draftModel.getValueAt(i, 2)).intValue();
        btnDraft.setText("Đơn tạm  " + count);
    }
    private void showDraftDialog() {
        final JDialog winDraft = new JDialog(SwingUtilities.getWindowAncestor(this), "Đơn tạm", Dialog.ModalityType.APPLICATION_MODAL);
        winDraft.setSize(820, 500);
        winDraft.setLocationRelativeTo(this);
        winDraft.setLayout(new BorderLayout(0, 12));
        winDraft.getContentPane().setBackground(UIConstants.BACKGROUND);
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(new EmptyBorder(18, 18, 0, 18));
        JLabel lblTitle = new JLabel("Đơn tạm");
        lblTitle.setFont(UIConstants.font(Font.BOLD, 20));
        lblTitle.setForeground(UIConstants.TEXT);
        JLabel lblHint = new JLabel("Danh sách sản phẩm đang tư vấn cho khách hàng");
        lblHint.setFont(UIConstants.font(Font.PLAIN, 12));
        lblHint.setForeground(UIConstants.MUTED);
        JPanel pnlHeaderText = new JPanel(); pnlHeaderText.setOpaque(false); pnlHeaderText.setLayout(new BoxLayout(pnlHeaderText, BoxLayout.Y_AXIS));
        pnlHeaderText.add(lblTitle); pnlHeaderText.add(Box.createVerticalStrut(3)); pnlHeaderText.add(lblHint);
        pnlHeader.add(pnlHeaderText, BorderLayout.WEST);
        winDraft.add(pnlHeader, BorderLayout.NORTH);
        final JTable tblDraft = UIFactory.table(draftModel);
        winDraft.add(UIFactory.scroll(tblDraft), BorderLayout.CENTER);
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.setBorder(new EmptyBorder(0, 18, 18, 18));
        final JButton btnMinus = UIFactory.secondaryButton("− SL");
        final JButton btnPlus = UIFactory.secondaryButton("+ SL");
        final JButton btnRemove = UIFactory.secondaryButton("Xóa dòng");
        final JButton btnClear = UIFactory.secondaryButton("Xóa đơn tạm");
        final JButton btnPayment = UIFactory.primaryButton("Thanh toán");
        pnlButtons.add(btnMinus); pnlButtons.add(btnPlus); pnlButtons.add(btnRemove); pnlButtons.add(btnClear); pnlButtons.add(btnPayment);
        winDraft.add(pnlButtons, BorderLayout.SOUTH);
        Runnable updateDialogButtons = () -> {
            boolean hasRows = draftModel.getRowCount() > 0;
            boolean hasSelection = tblDraft.getSelectedRow() >= 0;
            btnMinus.setEnabled(hasSelection);
            btnPlus.setEnabled(hasSelection);
            btnRemove.setEnabled(hasSelection);
            btnClear.setEnabled(hasRows);
            btnPayment.setEnabled(hasRows);
        };
        updateDialogButtons.run();
        tblDraft.getSelectionModel().addListSelectionListener(e -> updateDialogButtons.run());
        btnMinus.addActionListener(e -> { adjustDraft(tblDraft, -1); updateDialogButtons.run(); });
        btnPlus.addActionListener(e -> { adjustDraft(tblDraft, 1); updateDialogButtons.run(); });
        btnRemove.addActionListener(e -> {
            int row = tblDraft.getSelectedRow();
            if (row >= 0) draftModel.removeRow(tblDraft.convertRowIndexToModel(row));
            updateDraftCount();
            updateDialogButtons.run();
        });
        btnClear.addActionListener(e -> {
            if (draftModel.getRowCount() > 0 && confirm("Xóa toàn bộ sản phẩm khỏi đơn tạm?")) {
                draftModel.setRowCount(0);
                updateDraftCount();
                updateDialogButtons.run();
            }
        });
        btnPayment.addActionListener(e -> {
            if (draftModel.getRowCount() <= 0) return;
            transferDraftToInvoice();
            winDraft.dispose();
        });
        winDraft.setVisible(true);
    }
    private void adjustDraft(JTable tblDraft, int delta) {
        int vr = tblDraft.getSelectedRow();
        if (vr < 0) return;
        int row = tblDraft.convertRowIndexToModel(vr);
        String productId = String.valueOf(draftModel.getValueAt(row, 0));
        try {
            List<Map<String, Object>> rs = service.query(
                    "SELECT stock_quantity,sale_price FROM products WHERE product_id=? AND status='ACTIVE'", productId);
            if (rs.isEmpty()) { info("Sản phẩm không còn khả dụng."); return; }
            int stock = ((Number) rs.get(0).get("stock_quantity")).intValue();
            Number price = (Number) rs.get(0).get("sale_price");
            int qty = ((Number) draftModel.getValueAt(row, 2)).intValue() + delta;
            if (qty <= 0) draftModel.removeRow(row);
            else if (qty > stock) info("Số lượng vượt tồn kho hiện tại.");
            else {
                draftModel.setValueAt(Integer.valueOf(qty), row, 2);
                draftModel.setValueAt(Formats.money(price.doubleValue() * qty), row, 4);
            }
            updateDraftCount();
        } catch (Exception ex) {
            showError(ex);
        }
    }
    private void transferDraftToInvoice() {
        LinkedHashMap<String, Integer> items = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < draftModel.getRowCount(); i++) {
            String productId = String.valueOf(draftModel.getValueAt(i, 0));
            int qty = ((Number) draftModel.getValueAt(i, 2)).intValue();
            items.put(productId, Integer.valueOf(qty));
        }
        SalesService.setDraftItems(items);
        mainFrame.showScreen(MainFrame.SCREEN_INVOICE_CREATE, "Lập hóa đơn bán hàng");
    }
}

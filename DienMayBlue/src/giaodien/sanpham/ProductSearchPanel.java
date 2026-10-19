package giaodien.sanpham;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import tienich.Db;
import tienich.Formats;
import tienich.SalesDraftBridge;
import giaodien.dungchung.*;
import giaodien.chinh.MainFrame;

/** UC018 - Tìm sản phẩm. Giao diện bám Figma: ô tìm kiếm lớn, chip danh mục,
 * lưới thẻ sản phẩm và nút Đơn tạm ở góc phải. */
public class ProductSearchPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;

    private final MainFrame mainFrame;

    private final JTextField txtSearch = new JTextField();
    private final JPanel categoryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JPanel productGrid = new JPanel(new GridLayout(0, 2, 16, 16));
    private final JScrollPane productScroll = new JScrollPane(productGrid);
    private final JButton btnCheckout = UIFactory.primaryButton("Thanh toán");
    private final JButton btnAddToDraft = UIFactory.secondaryButton("Giỏ hàng");
    private final JButton btnDraft = UIFactory.primaryButton("Đơn tạm  0");

    private Integer selectedCategoryId = null;
    private JToggleButton selectedCategoryButton;

    private String selectedProductId;
    private String selectedProductName;
    private Number selectedProductPrice;
    private int selectedProductStock;
    private RoundedPanel selectedCard;

    private final DefaultTableModel draftModel = new DefaultTableModel(
            new Object[]{"Mã SP", "Tên sản phẩm", "SL", "Đơn giá", "Thành tiền"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public ProductSearchPanel(MainFrame mainFrame) {
        super("Tìm sản phẩm", "0 sản phẩm trong danh mục");
        this.mainFrame = mainFrame;

        btnCheckout.setEnabled(false);
        btnAddToDraft.setEnabled(false);

        actions.add(btnCheckout);
        actions.add(btnAddToDraft);
        actions.add(btnDraft);
        body.setLayout(new BorderLayout(0, 15));

        JPanel filters = new JPanel();
        filters.setOpaque(false);
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        filters.add(buildSearchBox());
        filters.add(Box.createVerticalStrut(15));

        categoryBar.setOpaque(false);
        filters.add(categoryBar);
        body.add(filters, BorderLayout.NORTH);

        productGrid.setOpaque(false);
        productGrid.setBorder(new EmptyBorder(0, 0, 8, 8));
        productScroll.setBorder(null);
        productScroll.setOpaque(false);
        productScroll.getViewport().setOpaque(false);
        productScroll.getVerticalScrollBar().setUnitIncrement(18);
        productScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        body.add(productScroll, BorderLayout.CENTER);

        btnAddToDraft.addActionListener(e -> addSelectedToDraft());
        btnCheckout.addActionListener(e -> checkoutSelectedProduct());
        btnDraft.addActionListener(e -> showDraftDialog());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshData(); }
            public void removeUpdate(DocumentEvent e) { refreshData(); }
            public void changedUpdate(DocumentEvent e) { refreshData(); }
        });

        loadCategories();
        refreshData();
    }

    private JComponent buildSearchBox() {
        RoundedPanel wrap = new RoundedPanel(8, Color.WHITE);
        wrap.setLayout(new BorderLayout(8, 0));
        wrap.setPreferredSize(new Dimension(680, 38));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                new EmptyBorder(0, 11, 0, 11)));

        JLabel icon = new JLabel("⌕");
        icon.setFont(UIConstants.font(Font.PLAIN, 22));
        icon.setForeground(UIConstants.MUTED);

        txtSearch.setBorder(null);
        txtSearch.setOpaque(false);
        txtSearch.setFont(UIConstants.font(Font.PLAIN, 12));
        txtSearch.setForeground(UIConstants.TEXT);
        txtSearch.setToolTipText("Nhập tên sản phẩm, mã SP, mã vạch hoặc danh mục để tìm kiếm");
        txtSearch.putClientProperty("JTextField.placeholderText", "Nhập tên sản phẩm, mã SP, danh mục để tìm kiếm...");

        wrap.add(icon, BorderLayout.WEST);
        wrap.add(txtSearch, BorderLayout.CENTER);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(wrap, BorderLayout.CENTER);
        return row;
    }

    private void loadCategories() {
        categoryBar.removeAll();
        ButtonGroup group = new ButtonGroup();

        JToggleButton all = categoryChip("Tất cả", null);
        group.add(all);
        categoryBar.add(all);
        all.setSelected(true);
        styleChip(all, true, false);
        selectedCategoryButton = all;

        try {
            for (Map<String, Object> r : Db.query(
                    "SELECT category_id,category_name FROM categories WHERE active=1 ORDER BY category_name")) {
                Integer id = ((Number) r.get("category_id")).intValue();
                JToggleButton b = categoryChip(String.valueOf(r.get("category_name")), id);
                group.add(b);
                categoryBar.add(b);
            }
        } catch (Exception ex) {
            showError(ex);
        }
        categoryBar.revalidate();
        categoryBar.repaint();
    }

    private JToggleButton categoryChip(String text, Integer id) {
        final JToggleButton b = new JToggleButton(text);
        b.setFont(UIConstants.font(Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Dùng Basic UI + tắt rollover mặc định của Windows LAF để màu hover/chọn
        // luôn đúng: nền cam, chữ trắng như thiết kế.
        b.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI());
        b.setRolloverEnabled(false);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setBorder(new EmptyBorder(8, 13, 8, 13));
        b.setOpaque(true);
        styleChip(b, false, false);
        b.addActionListener(e -> {
            if (selectedCategoryButton != null) styleChip(selectedCategoryButton, false, false);
            selectedCategoryButton = b;
            selectedCategoryId = id;
            styleChip(b, true, false);
            refreshData();
        });
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (b != selectedCategoryButton) styleChip(b, false, true);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (b != selectedCategoryButton) styleChip(b, false, false);
            }
        });
        return b;
    }

    private void styleChip(AbstractButton b, boolean selected, boolean hovered) {
        if (selected || hovered) {
            b.setBackground(UIConstants.ORANGE);
            b.setForeground(Color.WHITE);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.ORANGE),
                    new EmptyBorder(7, 12, 7, 12)));
        } else {
            b.setBackground(Color.WHITE);
            b.setForeground(UIConstants.MUTED);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.BORDER),
                    new EmptyBorder(7, 12, 7, 12)));
        }
    }

    @Override
    public void refreshData() {
        try {
            clearSelection();
            String kw = "%" + txtSearch.getText().trim() + "%";
            String sql = "SELECT p.product_id,p.product_name,p.sale_price,p.stock_quantity,p.category_id," +
                    "c.category_name,p.barcode,p.status " +
                    "FROM products p JOIN categories c ON c.category_id=p.category_id " +
                    "WHERE p.status='ACTIVE' " +
                    "AND (p.product_id LIKE ? OR p.barcode LIKE ? OR p.product_name LIKE ? OR c.category_name LIKE ?) " +
                    (selectedCategoryId == null ? "" : "AND p.category_id=? ") +
                    "ORDER BY p.product_name";

            List<Map<String, Object>> rows = selectedCategoryId == null
                    ? Db.query(sql, kw, kw, kw, kw)
                    : Db.query(sql, kw, kw, kw, kw, selectedCategoryId);

            productGrid.removeAll();
            for (Map<String, Object> r : rows) productGrid.add(productCard(r));

            subtitleLabel.setText(rows.size() + " sản phẩm trong danh mục");
            productGrid.revalidate();
            productGrid.repaint();
            SwingUtilities.invokeLater(() -> productScroll.getVerticalScrollBar().setValue(0));
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

        JPanel visual = new JPanel(new GridBagLayout());
        visual.setPreferredSize(new Dimension(10, 86));
        visual.setBackground(categoryColor(categoryName));
        JLabel icon = new JLabel("▣");
        icon.setFont(UIConstants.font(Font.BOLD, 26));
        icon.setForeground(categoryAccent(categoryName));
        visual.add(icon);

        JPanel info = new JPanel(null);
        info.setOpaque(false);
        info.setPreferredSize(new Dimension(10, 119));

        JLabel code = label(id, Font.BOLD, 10, UIConstants.MUTED);
        code.setBounds(13, 12, 130, 15);
        info.add(code);

        JLabel title = label(name, Font.BOLD, 13, UIConstants.TEXT);
        title.setBounds(13, 31, 320, 22);
        info.add(title);

        JLabel priceLabel = label(Formats.money(price), Font.BOLD, 14, UIConstants.ORANGE);
        priceLabel.setBounds(13, 73, 190, 20);
        info.add(priceLabel);

        JLabel stockLabel = label("Tồn: " + stock, Font.PLAIN, 11, UIConstants.MUTED);
        stockLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        stockLabel.setBounds(245, 75, 88, 18);
        info.add(stockLabel);

        if (stock <= 0) {
            JLabel out = label("Hết hàng", Font.BOLD, 11, new Color(220, 38, 38));
            out.setBounds(245, 12, 88, 18);
            out.setHorizontalAlignment(SwingConstants.RIGHT);
            info.add(out);
        }

        card.add(visual, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);

        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectProduct(card, id, name, price, stock);
            }
            @Override public void mouseEntered(MouseEvent e) {
                if (card != selectedCard) applyCardStyle(card, true, false);
            }
            @Override public void mouseExited(MouseEvent e) {
                // Khi chuột đi từ vùng ảnh sang vùng thông tin, Swing phát mouseExited
                // cho component con. Chỉ bỏ hover khi con trỏ thật sự ra khỏi toàn card.
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
        // Chỉ gắn listener đúng một lần cho mỗi component. Bản trước gắn trùng
        // cho các JPanel con, dễ làm một click/hover phát sinh nhiều event.
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
        // RoundedPanel tự vẽ bằng fillColor/borderColor, vì vậy setBackground()
        // không làm đổi màu hiển thị. Phải thay trực tiếp fillColor.
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
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.font(style, size));
        l.setForeground(color);
        return l;
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
        SalesDraftBridge.setDraftItems(items);
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
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Đơn tạm", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(820, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 12));
        dialog.getContentPane().setBackground(UIConstants.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 18, 0, 18));
        JLabel title = new JLabel("Đơn tạm");
        title.setFont(UIConstants.font(Font.BOLD, 20));
        title.setForeground(UIConstants.TEXT);
        JLabel hint = new JLabel("Danh sách sản phẩm đang tư vấn cho khách hàng");
        hint.setFont(UIConstants.font(Font.PLAIN, 12));
        hint.setForeground(UIConstants.MUTED);
        JPanel ht = new JPanel(); ht.setOpaque(false); ht.setLayout(new BoxLayout(ht, BoxLayout.Y_AXIS));
        ht.add(title); ht.add(Box.createVerticalStrut(3)); ht.add(hint);
        header.add(ht, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        final JTable table = UIFactory.table(draftModel);
        dialog.add(UIFactory.scroll(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(0, 18, 18, 18));
        final JButton minus = UIFactory.secondaryButton("− SL");
        final JButton plus = UIFactory.secondaryButton("+ SL");
        final JButton remove = UIFactory.secondaryButton("Xóa dòng");
        final JButton clear = UIFactory.secondaryButton("Xóa đơn tạm");
        final JButton payment = UIFactory.primaryButton("Thanh toán");
        buttons.add(minus); buttons.add(plus); buttons.add(remove); buttons.add(clear); buttons.add(payment);
        dialog.add(buttons, BorderLayout.SOUTH);

        Runnable updateDialogButtons = () -> {
            boolean hasRows = draftModel.getRowCount() > 0;
            boolean hasSelection = table.getSelectedRow() >= 0;
            minus.setEnabled(hasSelection);
            plus.setEnabled(hasSelection);
            remove.setEnabled(hasSelection);
            clear.setEnabled(hasRows);
            payment.setEnabled(hasRows);
        };
        updateDialogButtons.run();
        table.getSelectionModel().addListSelectionListener(e -> updateDialogButtons.run());

        minus.addActionListener(e -> { adjustDraft(table, -1); updateDialogButtons.run(); });
        plus.addActionListener(e -> { adjustDraft(table, 1); updateDialogButtons.run(); });
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) draftModel.removeRow(table.convertRowIndexToModel(row));
            updateDraftCount();
            updateDialogButtons.run();
        });
        clear.addActionListener(e -> {
            if (draftModel.getRowCount() > 0 && confirm("Xóa toàn bộ sản phẩm khỏi đơn tạm?")) {
                draftModel.setRowCount(0);
                updateDraftCount();
                updateDialogButtons.run();
            }
        });
        payment.addActionListener(e -> {
            if (draftModel.getRowCount() <= 0) return;
            transferDraftToInvoice();
            dialog.dispose();
        });
        dialog.setVisible(true);
    }

    private void adjustDraft(JTable table, int delta) {
        int vr = table.getSelectedRow();
        if (vr < 0) return;
        int row = table.convertRowIndexToModel(vr);
        String productId = String.valueOf(draftModel.getValueAt(row, 0));
        try {
            List<Map<String, Object>> rs = Db.query(
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
        SalesDraftBridge.setDraftItems(items);
        mainFrame.showScreen(MainFrame.SCREEN_INVOICE_CREATE, "Lập hóa đơn bán hàng");
    }
}

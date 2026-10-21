package app;

import bus.PhienDangNhap;
import entity.TaiKhoan;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;


/**
 * Sidebar dùng chung cho toàn hệ thống, lọc động theo role_permissions.
 *
 * UI menu được tổ chức theo nhóm chức năng kiểu accordion:
 * - Nhấn tiêu đề nhóm để xổ/thu các chức năng con.
 * - Chỉ một nhóm mở tại một thời điểm.
 * - Chức năng đang mở sẽ tự mở đúng nhóm chứa nó.
 * - Quyền truy cập vẫn do UIMain.canAccess(...) kiểm soát, không thay đổi nghiệp vụ.
 */
public class PanelSidebar extends JPanel {
    private static final long serialVersionUID = 1L;

    // Sidebar compact hơn để không chiếm nhiều diện tích nội dung.
    private static final int SIDEBAR_WIDTH = 204;
    private static final int SIDEBAR_COLLAPSED_WIDTH = 58;
    private static final int MENU_CONTENT_WIDTH = 186;

    private final UIMain mainFrame;
    private final Map<String, NavButton> buttons = new LinkedHashMap<String, NavButton>();
    private final List<MenuGroupPanel> groupPanels = new ArrayList<MenuGroupPanel>();

    private final JPanel brandRow = new JPanel(new BorderLayout());
    private final JPanel userArea = new JPanel();
    private final JLabel brandText = new JLabel("Điện Máy Blue");
    private final JLabel userName = new JLabel();
    private final JLabel roleName = new JLabel();
    private final JLabel avatar = new JLabel("", SwingConstants.CENTER);

    private boolean collapsed = false;

    public PanelSidebar(UIMain mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(UIConstants.NAVY);
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 900));
        setLayout(new BorderLayout());
        buildBrand();
        buildNav();
        buildUserArea();
    }

    private void buildBrand() {
        brandRow.setOpaque(false);
        brandRow.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 52));
        brandRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 20)),
                new EmptyBorder(0, 10, 0, 10)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

        RoundedPanel logo = new RoundedPanel(9, UIConstants.ORANGE);
        logo.setDrawBorder(false);
        logo.setPreferredSize(new Dimension(28, 28));
        logo.setMaximumSize(new Dimension(28, 28));
        logo.setLayout(new GridBagLayout());
        JLabel b = new JLabel("B");
        b.setForeground(Color.WHITE);
        b.setFont(UIConstants.font(Font.BOLD, 13));
        logo.add(b);

        brandText.setForeground(Color.WHITE);
        brandText.setFont(UIConstants.font(Font.BOLD, 13));
        brandText.setBorder(new EmptyBorder(0, 9, 0, 0));

        left.add(logo);
        left.add(brandText);

        JButton collapse = new JButton("‹");
        collapse.setForeground(new Color(255, 255, 255, 105));
        collapse.setFont(UIConstants.font(Font.BOLD, 20));
        collapse.setContentAreaFilled(false);
        collapse.setBorderPainted(false);
        collapse.setFocusPainted(false);
        collapse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        collapse.addActionListener(e -> setCollapsed(!collapsed));

        brandRow.add(left, BorderLayout.CENTER);
        brandRow.add(collapse, BorderLayout.EAST);
        add(brandRow, BorderLayout.NORTH);
    }

    /**
     * Menu bên trái: Trang chủ đứng độc lập; các chức năng còn lại được gom
     * theo các đối tượng chính và xổ xuống giống mẫu accordion.
     */
    private void buildNav() {
        JPanel nav = new JPanel();
        nav.setBackground(UIConstants.NAVY);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(8, 6, 8, 6));

        // Trang chủ luôn đứng riêng ở đầu menu.
        if (mainFrame.canAccess(UIMain.SCREEN_DASHBOARD)) {
            NavButton dashboard = createNavButton(
                    "Trang chủ", UIMain.SCREEN_DASHBOARD, null, false);
            nav.add(dashboard);
            nav.add(Box.createVerticalStrut(4));
        }

        // Cùng một danh mục nhóm chức năng; canAccess() quyết định mục nào được hiện.
        for (MenuGroup group : createRoleGroups()) {
            MenuGroupPanel groupPanel = new MenuGroupPanel(group.title, group.icon);

            for (MenuItem item : group.items) {
                if (!mainFrame.canAccess(item.screenKey)) continue;
                NavButton child = createNavButton(item.label, item.screenKey, item.icon, true);
                groupPanel.addItem(item.screenKey, child);
            }

            // Không hiển thị nhóm rỗng sau khi lọc permission.
            if (groupPanel.getItemCount() > 0) {
                groupPanels.add(groupPanel);
                nav.add(groupPanel);
                nav.add(Box.createVerticalStrut(2));
            }
        }

        JScrollPane scroll = new JScrollPane(nav);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(UIConstants.NAVY);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        add(scroll, BorderLayout.CENTER);
    }

    private NavButton createNavButton(String label, String screenKey, String icon, boolean child) {
        // Menu con theo đúng mẫu: chỉ hiển thị chữ, không lặp icon ở từng dòng.
        // Cách này vừa gọn vừa tránh các nhãn dài bị cắt ở mép phải.
        NavButton button = new NavButton(label, child ? null : icon);
        buttons.put(screenKey, button);

        if (child) {
            button.setFont(UIConstants.font(Font.PLAIN, 10));
            button.setPreferredSize(new Dimension(MENU_CONTENT_WIDTH, 29));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
            // Menu con chỉ thụt nhẹ so với menu cha, giống mẫu tham chiếu.
            // Không đẩy chữ quá sâu sang phải vì sẽ làm nhãn dài bị cắt.
            button.setMargin(new Insets(0, 14, 0, 4));
            button.setIconTextGap(0);
            button.setToolTipText(label);
            // Bắt buộc neo submenu về mép trái. Nếu để alignment mặc định của Swing,
            // BoxLayout có thể đặt cả itemsPanel lệch sang phải khi nhóm được xổ ra.
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else {
            button.setFont(UIConstants.font(Font.BOLD, 11));
            button.setPreferredSize(new Dimension(MENU_CONTENT_WIDTH, 36));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            button.setMargin(new Insets(0, 9, 0, 7));
            button.setIconTextGap(8);
        }

        button.addActionListener(ev -> mainFrame.showScreen(screenKey, label));
        return button;
    }

    private void buildUserArea() {
        TaiKhoan a = PhienDangNhap.getTaiKhoanHienTai();

        userArea.setBackground(UIConstants.NAVY);
        userArea.setLayout(new BoxLayout(userArea, BoxLayout.Y_AXIS));
        userArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 20)),
                new EmptyBorder(7, 8, 8, 8)));

        RoundedPanel userButton = new RoundedPanel(12, new Color(255,255,255,18));
        userButton.setBorderColor(new Color(255,255,255,25));
        userButton.setLayout(new BorderLayout(9, 0));
        userButton.setBorder(new EmptyBorder(8, 8, 8, 8));
        userButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setOpaque(false);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(UIConstants.font(Font.BOLD, 11));
        String initials = a == null ? "" : initials(a.getNhanVien().getHoTen());
        avatar.setText(initials);
        avatar.setIcon(null);
        avatar.setHorizontalTextPosition(SwingConstants.CENTER);
        avatar.setVerticalTextPosition(SwingConstants.CENTER);

        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));

        userName.setText(a == null ? "" : a.getNhanVien().getHoTen());
        userName.setForeground(Color.WHITE);
        userName.setFont(UIConstants.font(Font.BOLD, 11));

        roleName.setText(a == null ? "" : roleLabel(a.getVaiTro().getCodeVaiTro()));
        roleName.setForeground(new Color(255, 255, 255, 110));
        roleName.setFont(UIConstants.font(Font.PLAIN, 10));

        txt.add(userName);
        txt.add(Box.createVerticalStrut(2));
        txt.add(roleName);

        userButton.add(avatar, BorderLayout.WEST);
        userButton.add(txt, BorderLayout.CENTER);

        MouseAdapter profileClick = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                mainFrame.showScreen(UIMain.SCREEN_PROFILE, "Hồ sơ cá nhân");
            }
        };
        userButton.addMouseListener(profileClick);
        avatar.addMouseListener(profileClick);
        txt.addMouseListener(profileClick);

        NavButton logout = new NavButton("Đăng xuất", null);
        logout.setForeground(new Color(255, 255, 255, 128));
        logout.addActionListener(e -> mainFrame.requestLogout());

        userArea.add(userButton);
        userArea.add(Box.createVerticalStrut(4));
        userArea.add(logout);
        add(userArea, BorderLayout.SOUTH);
    }

    public void setCollapsed(boolean c) {
        collapsed = c;
        setPreferredSize(new Dimension(c ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_WIDTH, getHeight()));
        brandText.setVisible(!c);

        Component[] comps = userArea.getComponents();
        if (comps.length > 0 && comps[0] instanceof JPanel) {
            JPanel user = (JPanel) comps[0];
            if (user.getLayout() instanceof BorderLayout) {
                Component center = ((BorderLayout) user.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                if (center != null) center.setVisible(!c);
            }
        }

        for (NavButton b : buttons.values()) b.setCollapsed(c);
        for (MenuGroupPanel group : groupPanels) group.setSidebarCollapsed(c);
        if (comps.length > 2 && comps[2] instanceof NavButton) ((NavButton) comps[2]).setCollapsed(c);

        revalidate();
        repaint();
        mainFrame.revalidate();
    }

    public boolean isCollapsed() { return collapsed; }

    /**
     * Giữ active state như cũ, đồng thời tự mở nhóm chứa chức năng hiện tại.
     */
    public void setActive(String key) {
        for (Map.Entry<String, NavButton> e : buttons.entrySet())
            e.getValue().setActive(e.getKey().equals(key));

        MenuGroupPanel owner = null;
        for (MenuGroupPanel group : groupPanels) {
            group.setGroupActive(group.containsScreen(key));
            if (group.containsScreen(key)) owner = group;
        }

        if (!collapsed) {
            if (owner != null) {
                openOnly(owner);
            } else if (UIMain.SCREEN_DASHBOARD.equals(key)) {
                closeAllGroups();
            }
        }
    }

    private void openOnly(MenuGroupPanel selected) {
        for (MenuGroupPanel group : groupPanels) group.setExpanded(group == selected);
    }

    private void closeAllGroups() {
        for (MenuGroupPanel group : groupPanels) group.setExpanded(false);
    }

    private String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, 1).toUpperCase();
        return (p[0].substring(0, 1) + p[p.length - 1].substring(0, 1)).toUpperCase();
    }

    private String roleLabel(String role) {
        if ("ADMIN".equals(role)) return "Admin";
        if ("MANAGER".equals(role)) return "Nhân viên quản lý";
        if ("SALES".equals(role)) return "Nhân viên bán hàng";
        return "Nhân viên CSKH";
    }

    /**
     * Menu tổ chức theo hướng đối tượng: mỗi nhóm cấp một đại diện cho một
     * đối tượng chính của hệ thống; các mục con là thao tác trên đối tượng đó.
     * Trang chủ đứng riêng vì là màn hình tổng hợp.
     */
    private List<MenuGroup> createRoleGroups() {
        List<MenuGroup> groups = new ArrayList<MenuGroup>();

        groups.add(new MenuGroup("Nhân viên", null)
                .add("Quản lý nhân viên", UIMain.SCREEN_EMPLOYEES, null)
                .add("Quản lý ca làm việc", UIMain.SCREEN_SHIFTS, null)
                .add("Quản lý tài khoản", UIMain.SCREEN_ACCOUNTS, null)
                .add("Xem doanh số nhân viên", UIMain.SCREEN_PERSONAL_SALES, null));

        groups.add(new MenuGroup("Khách hàng", null)
                .add("Quản lý khách hàng", UIMain.SCREEN_CUSTOMERS, null)
                .add("Quản lý hạng thành viên", UIMain.SCREEN_MEMBERSHIP, null)
                .add("Quản lý bảo hành", UIMain.SCREEN_WARRANTY_TRACK, null)
                .add("Quản lý khiếu nại", UIMain.SCREEN_COMPLAINT_PROCESS, null));

        groups.add(new MenuGroup("Sản phẩm", null)
                .add("Quản lý sản phẩm", UIMain.SCREEN_PRODUCTS, null)
                .add("Tìm kiếm sản phẩm", UIMain.SCREEN_PRODUCT_SEARCH, null)
                .add("Quản lý khuyến mãi", UIMain.SCREEN_PROMOTIONS, null)
                .add("Quản lý nhà cung cấp", UIMain.SCREEN_SUPPLIERS, null));

        groups.add(new MenuGroup("Hóa đơn", null)
                .add("Lập hóa đơn bán hàng", UIMain.SCREEN_INVOICE_CREATE, null)
                .add("Quản lý hóa đơn", UIMain.SCREEN_SALES, null)
                .add("Lập phiếu đổi trả", UIMain.SCREEN_RETURN_CREATE, null)
                .add("Duyệt hoàn tiền", UIMain.SCREEN_REFUNDS, null));

        groups.add(new MenuGroup("Phiếu giao hàng", null)
                .add("Lập phiếu giao hàng", UIMain.SCREEN_DELIVERY_CREATE, null)
                .add("Quản lý phiếu giao hàng", UIMain.SCREEN_DELIVERY, null));

        groups.add(new MenuGroup("Thống kê", null)
                .add("Thống kê kinh doanh", UIMain.SCREEN_STATISTICS, null));

        return groups;
    }

    private static class MenuItem {
        final String label;
        final String screenKey;
        final String icon;

        MenuItem(String label, String screenKey, String icon) {
            this.label = label;
            this.screenKey = screenKey;
            this.icon = icon;
        }
    }

    private static class MenuGroup {
        final String title;
        final String icon;
        final List<MenuItem> items = new ArrayList<MenuItem>();

        MenuGroup(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }

        MenuGroup add(String label, String screenKey, String icon) {
            items.add(new MenuItem(label, screenKey, icon));
            return this;
        }
    }

    /**
     * Một nhóm menu có header và vùng submenu có thể xổ/thu.
     */
    private class MenuGroupPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JPanel header = new JPanel(new BorderLayout(8, 0));
        private final JLabel groupIcon = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel arrowLabel = new JLabel("+", SwingConstants.CENTER);
        private final JPanel itemsPanel = new JPanel();
        private final Map<String, NavButton> itemButtons = new LinkedHashMap<String, NavButton>();

        private boolean expanded = false;
        private boolean sidebarCollapsed = false;
        private boolean groupActive = false;

        MenuGroupPanel(String title, String icon) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            // Không cho BoxLayout kéo giãn nhóm theo chiều dọc. Nếu để chiều cao
            // Integer.MAX_VALUE, mỗi nhóm sẽ chiếm một khoảng trống rất lớn và
            // đẩy các nhóm phía dưới ra khỏi vùng nhìn thấy.
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));

            header.setOpaque(false);
            // Header và vùng submenu phải cùng neo trái để việc xổ/thu không làm
            // thay đổi bề rộng/điểm bắt đầu của cả nhóm menu.
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.setBorder(new EmptyBorder(0, 8, 0, 5));
            header.setPreferredSize(new Dimension(MENU_CONTENT_WIDTH, 36));
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            groupIcon.putClientProperty("iconKind", icon);
            groupIcon.setPreferredSize(new Dimension(0, 0));
            groupIcon.setVisible(false);

            titleLabel.setText(title);
            titleLabel.setForeground(new Color(189, 204, 220));
            titleLabel.setFont(UIConstants.font(Font.BOLD, 11));

            arrowLabel.setForeground(new Color(189, 204, 220));
            arrowLabel.setFont(UIConstants.font(Font.BOLD, 16));
            arrowLabel.setPreferredSize(new Dimension(15, 18));

            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
            left.add(titleLabel);

            header.add(left, BorderLayout.CENTER);
            header.add(arrowLabel, BorderLayout.EAST);

            itemsPanel.setOpaque(false);
            itemsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
            // Giữ submenu cùng bề rộng với menu cha; độ thụt nằm ở margin của
            // từng nút con để khi xổ xuống không làm thay đổi "khung" menu.
            itemsPanel.setBorder(new EmptyBorder(0, 0, 2, 0));
            itemsPanel.setVisible(false);

            MouseAdapter headerMouse = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (sidebarCollapsed) {
                        // Giữ cơ chế sidebar thu gọn hiện có: nhấn icon nhóm sẽ mở
                        // sidebar ra rồi xổ nhóm đó.
                        PanelSidebar.this.setCollapsed(false);
                        openOnly(MenuGroupPanel.this);
                    } else if (expanded) {
                        setExpanded(false);
                    } else {
                        openOnly(MenuGroupPanel.this);
                    }
                }

                @Override public void mouseEntered(MouseEvent e) {
                    // Chỉ hover nhẹ; không đổi kích thước/bố cục của dòng menu cha.
                    header.setBackground(new Color(255, 255, 255, 14));
                    header.setOpaque(true);
                    repaint();
                }

                @Override public void mouseExited(MouseEvent e) {
                    // Khi rời chuột, menu cha trở về đúng nền sidebar dù nhóm đang mở
                    // hay đang chứa màn hình active. Như vậy xổ menu chỉ "thêm dòng con"
                    // chứ không làm menu cha biến thành một khối khác.
                    header.setOpaque(false);
                    repaint();
                }
            };
            header.addMouseListener(headerMouse);
            left.addMouseListener(headerMouse);
            groupIcon.addMouseListener(headerMouse);
            titleLabel.addMouseListener(headerMouse);
            arrowLabel.addMouseListener(headerMouse);

            add(header);
            add(itemsPanel);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension preferred = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, preferred.height);
        }

        void addItem(String screenKey, NavButton button) {
            itemButtons.put(screenKey, button);
            itemsPanel.add(button);
            itemsPanel.add(Box.createVerticalStrut(1));
        }

        int getItemCount() {
            return itemButtons.size();
        }

        boolean containsScreen(String screenKey) {
            return itemButtons.containsKey(screenKey);
        }

        void setGroupActive(boolean active) {
            groupActive = active;
            Color c = active ? Color.WHITE : new Color(189, 204, 220);
            titleLabel.setForeground(c);
            arrowLabel.setForeground(c);
            // Active chỉ đổi màu chữ, không đổi nền hay hình học của menu cha.
            header.setOpaque(false);
            repaint();
        }

        void setExpanded(boolean value) {
            expanded = value;
            arrowLabel.setText(value ? "-" : "+");
            itemsPanel.setVisible(value && !sidebarCollapsed);
            revalidate();
            repaint();
        }

        void setSidebarCollapsed(boolean value) {
            sidebarCollapsed = value;
            titleLabel.setVisible(!value);
            arrowLabel.setVisible(!value);
            itemsPanel.setVisible(expanded && !value);

            header.setBorder(value
                    ? new EmptyBorder(0, 8, 0, 8)
                    : new EmptyBorder(0, 8, 0, 5));
            header.setPreferredSize(new Dimension(value ? 40 : MENU_CONTENT_WIDTH, 36));
            header.setMaximumSize(new Dimension(value ? 40 : Integer.MAX_VALUE, 36));
            header.setOpaque(false);

            // NavButton.setCollapsed(false) đưa margin/height về mặc định của menu
            // cấp 1; khôi phục lại đúng kiểu submenu gọn khi sidebar mở ra.
            if (!value) {
                for (NavButton child : itemButtons.values()) {
                    child.setFont(UIConstants.font(Font.PLAIN, 10));
                    child.setPreferredSize(new Dimension(MENU_CONTENT_WIDTH, 29));
                    child.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
                    child.setMargin(new Insets(0, 14, 0, 4));
                    child.setIconTextGap(0);
                    child.setHorizontalAlignment(SwingConstants.LEFT);
                    child.setAlignmentX(Component.LEFT_ALIGNMENT);
                }
            }

            revalidate();
            repaint();
        }
    }
}

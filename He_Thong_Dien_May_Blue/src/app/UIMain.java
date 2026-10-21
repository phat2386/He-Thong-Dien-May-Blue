package app;

import bus.DangNhap_BUS;
import bus.BusinessService;
import bus.PhienDangNhap;
import dao.Db;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Rectangle;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.Scrollable;


public class UIMain extends JFrame {
    private static final long serialVersionUID = 1L;

    public static final String SCREEN_DASHBOARD = "DASHBOARD";
    public static final String SCREEN_EMPLOYEES = "EMPLOYEES";
    public static final String SCREEN_SALES = "SALES";
    public static final String SCREEN_PRODUCT_SEARCH = "PRODUCT_SEARCH";
    public static final String SCREEN_INVOICE_CREATE = "INVOICE_CREATE";
    public static final String SCREEN_DELIVERY_CREATE = "DELIVERY_CREATE";
    public static final String SCREEN_RETURN_CREATE = "RETURN_CREATE";
    public static final String SCREEN_PRODUCTS = "PRODUCTS";
    public static final String SCREEN_CUSTOMERS = "CUSTOMERS";
    public static final String SCREEN_MEMBERSHIP = "MEMBERSHIP";
    public static final String SCREEN_PROMOTIONS = "PROMOTIONS";
    public static final String SCREEN_DELIVERY = "DELIVERY";
    public static final String SCREEN_STATISTICS = "STATISTICS";
    public static final String SCREEN_PERSONAL_SALES = "PERSONAL_SALES";
    public static final String SCREEN_WARRANTY_TRACK = "WARRANTY_TRACK";
    public static final String SCREEN_COMPLAINT_PROCESS = "COMPLAINT_PROCESS";
    public static final String SCREEN_SUPPLIERS = "SUPPLIERS";
    public static final String SCREEN_ACCOUNTS = "ACCOUNTS";
    public static final String SCREEN_SHIFTS = "SHIFTS";
    public static final String SCREEN_REFUNDS = "REFUNDS";
    public static final String SCREEN_PROFILE = "PROFILE";

    private static final long SESSION_TIMEOUT_MS = 15L * 60L * 1000L;
    private static final int FRAME_WIDTH = 1180;
    private static final int FRAME_HEIGHT = 720;

    private final DangNhap_BUS authService;
    private final BusinessService backgroundService = new BusinessService();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final PanelTopBar topBar;
    private final Map<String,JPanel> screens = new HashMap<String,JPanel>();
    private PanelSidebar sidebar;
    private final List<NavigationEntry> navigationHistory = new ArrayList<NavigationEntry>();
    private int navigationIndex = -1;

    private long lastActivity = System.currentTimeMillis();
    private final AWTEventListener activityListener;
    private final Timer sessionTimer;

    public UIMain(DangNhap_BUS authService) {
        this.authService = authService;
        setTitle("Điện Máy Blue");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        topBar = new PanelTopBar(this::goBack, this::goForward);
        sidebar = new PanelSidebar(this);
        add(sidebar, BorderLayout.WEST);
        add(topBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { requestLogout(); }
        });

        activityListener = event -> {
            if (event instanceof MouseEvent || event instanceof KeyEvent) lastActivity = System.currentTimeMillis();
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        sessionTimer = new Timer(30_000, e -> checkSessionTimeout());
        sessionTimer.start();
        openInitialScreen();
    }

    private void addScreen(String key, JPanel panel) {
        screens.put(key, panel);
        contentPanel.add(createScrollableScreen(panel), key);
    }

    private JScrollPane createScrollableScreen(JPanel panel) {
        ScreenScrollHost host = new ScreenScrollHost(panel);

        JScrollPane scroll = new JScrollPane(
                host,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setBlockIncrement(120);
        // Thanh cuộn dọc mảnh, không chiếm nhiều chiều rộng nội dung.
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(9, Integer.MAX_VALUE));
        return scroll;
    }

    /**
     * Host cho từng màn hình: luôn bám đúng chiều rộng viewport để các form
     * GridBag/Flow/WrapLayout co giãn thay vì tràn sang phải. Chiều cao vẫn
     * dùng preferred size để JScrollPane chỉ cuộn dọc khi nội dung thực sự dài.
     */
    private static final class ScreenScrollHost extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        ScreenScrollHost(JPanel content) {
            super(new BorderLayout());
            setBackground(UIConstants.BACKGROUND);
            add(content, BorderLayout.NORTH);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private JPanel createScreen(String key) {
        if (SCREEN_DASHBOARD.equals(key)) return new PanelDashboard();
        if (SCREEN_EMPLOYEES.equals(key)) return new PanelNhanVien();
        if (SCREEN_SALES.equals(key)) return new PanelSalesManagement(this);
        if (SCREEN_PRODUCT_SEARCH.equals(key)) return new PanelProductSearch(this);
        if (SCREEN_INVOICE_CREATE.equals(key)) return new PanelInvoiceCreate();
        if (SCREEN_DELIVERY_CREATE.equals(key)) return new PanelDeliveryCreate();
        if (SCREEN_RETURN_CREATE.equals(key)) return new PanelReturnRequest();
        if (SCREEN_PRODUCTS.equals(key)) return new PanelProductManagement();
        if (SCREEN_CUSTOMERS.equals(key)) return new PanelCustomerManagement();
        if (SCREEN_MEMBERSHIP.equals(key)) return new PanelMembershipManagement();
        if (SCREEN_PROMOTIONS.equals(key)) return new PanelPromotionManagement();
        if (SCREEN_DELIVERY.equals(key)) return new PanelDeliveryManagement();
        if (SCREEN_STATISTICS.equals(key)) return new PanelStatistics();
        if (SCREEN_PERSONAL_SALES.equals(key)) return new PanelPersonalSales();
        if (SCREEN_WARRANTY_TRACK.equals(key)) return new PanelAfterSales(PanelAfterSales.Section.WARRANTY);
        if (SCREEN_COMPLAINT_PROCESS.equals(key)) return new PanelAfterSales(PanelAfterSales.Section.COMPLAINT);
        if (SCREEN_SUPPLIERS.equals(key)) return new PanelSupplierManagement();
        if (SCREEN_ACCOUNTS.equals(key)) return new PanelTaiKhoan();
        if (SCREEN_SHIFTS.equals(key)) return new PanelCaLamViec();
        if (SCREEN_REFUNDS.equals(key)) return new PanelRefundApproval();
        if (SCREEN_PROFILE.equals(key)) return new PanelHoSo(this, authService);
        return null;
    }

    public void showScreen(String screenKey, String title) {
        if (!displayScreen(screenKey, title)) return;

        if (navigationIndex >= 0
                && navigationHistory.get(navigationIndex).screenKey.equals(screenKey)) {
            navigationHistory.get(navigationIndex).title = title;
        } else {
            while (navigationHistory.size() > navigationIndex + 1) {
                navigationHistory.remove(navigationHistory.size() - 1);
            }
            navigationHistory.add(new NavigationEntry(screenKey, title));
            navigationIndex = navigationHistory.size() - 1;
        }
        updateNavigationButtons();
    }

    private boolean displayScreen(String screenKey, String title) {
        if (!canAccess(screenKey)) {
            JOptionPane.showMessageDialog(DialogUtils.owner(this),
                    "Tài khoản hiện tại không có quyền truy cập chức năng này.",
                    "Không có quyền", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        JPanel panel = screens.get(screenKey);
        if (panel == null) {
            try {
                panel = createScreen(screenKey);
                if (panel == null) return false;
                addScreen(screenKey, panel);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(DialogUtils.owner(this),
                        "Không thể mở chức năng này.\n" + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (panel instanceof Refreshable) ((Refreshable) panel).refreshData();
        cardLayout.show(contentPanel, screenKey);
        topBar.setScreenTitle(title);
        if (sidebar != null) sidebar.setActive(screenKey);
        return true;
    }

    private void goBack() {
        if (navigationIndex <= 0) return;
        int targetIndex = navigationIndex - 1;
        NavigationEntry target = navigationHistory.get(targetIndex);
        if (displayScreen(target.screenKey, target.title)) {
            navigationIndex = targetIndex;
            updateNavigationButtons();
        }
    }

    private void goForward() {
        if (navigationIndex < 0 || navigationIndex >= navigationHistory.size() - 1) return;
        int targetIndex = navigationIndex + 1;
        NavigationEntry target = navigationHistory.get(targetIndex);
        if (displayScreen(target.screenKey, target.title)) {
            navigationIndex = targetIndex;
            updateNavigationButtons();
        }
    }

    private void updateNavigationButtons() {
        topBar.setNavigationEnabled(
                navigationIndex > 0,
                navigationIndex >= 0 && navigationIndex < navigationHistory.size() - 1);
    }

    private static final class NavigationEntry {
        final String screenKey;
        String title;

        NavigationEntry(String screenKey, String title) {
            this.screenKey = screenKey;
            this.title = title;
        }
    }

    /**
     * Quyền truy cập màn hình được điều khiển trực tiếp bởi role_permissions.
     * Không còn giới hạn cứng MANAGER/SALES/CSKH: Admin có thể cấp bất kỳ
     * chức năng nghiệp vụ nào cho bất kỳ actor nào. Riêng ADMIN là superuser
     * để phát triển/kiểm thử nên luôn truy cập được toàn bộ màn hình.
     */
    public boolean canAccess(String key) {
        if (!PhienDangNhap.daDangNhap() || PhienDangNhap.getTaiKhoanHienTai().getVaiTro() == null) return false;
        String role = PhienDangNhap.getTaiKhoanHienTai().getVaiTro().getCodeVaiTro();

        if ("ADMIN".equals(role)) {
            return SCREEN_DASHBOARD.equals(key)
                    || SCREEN_EMPLOYEES.equals(key)
                    || SCREEN_SALES.equals(key)
                    || SCREEN_PRODUCT_SEARCH.equals(key)
                    || SCREEN_INVOICE_CREATE.equals(key)
                    || SCREEN_DELIVERY_CREATE.equals(key)
                    || SCREEN_RETURN_CREATE.equals(key)
                    || SCREEN_PRODUCTS.equals(key)
                    || SCREEN_CUSTOMERS.equals(key)
                    || SCREEN_MEMBERSHIP.equals(key)
                    || SCREEN_PROMOTIONS.equals(key)
                    || SCREEN_DELIVERY.equals(key)
                    || SCREEN_STATISTICS.equals(key)
                    || SCREEN_PERSONAL_SALES.equals(key)
                    || SCREEN_WARRANTY_TRACK.equals(key)
                    || SCREEN_COMPLAINT_PROCESS.equals(key)
                    || SCREEN_SUPPLIERS.equals(key)
                    || SCREEN_ACCOUNTS.equals(key)
                    || SCREEN_SHIFTS.equals(key)
                    || SCREEN_REFUNDS.equals(key)
                    || SCREEN_PROFILE.equals(key);
        }

        if (SCREEN_DASHBOARD.equals(key)) return PhienDangNhap.coQuyen("DASHBOARD_VIEW");
        if (SCREEN_EMPLOYEES.equals(key)) return PhienDangNhap.coQuyen("EMPLOYEE_MANAGE");
        if (SCREEN_ACCOUNTS.equals(key)) return PhienDangNhap.coQuyen("ACCOUNT_MANAGE");
        if (SCREEN_SHIFTS.equals(key)) return PhienDangNhap.coQuyen("SHIFT_MANAGE");
        if (SCREEN_PRODUCTS.equals(key)) return PhienDangNhap.coQuyen("PRODUCT_MANAGE");
        if (SCREEN_SUPPLIERS.equals(key)) return PhienDangNhap.coQuyen("SUPPLIER_MANAGE");
        if (SCREEN_PROMOTIONS.equals(key)) return PhienDangNhap.coQuyen("PROMOTION_MANAGE");
        if (SCREEN_REFUNDS.equals(key)) return PhienDangNhap.coQuyen("REFUND_APPROVE");
        if (SCREEN_STATISTICS.equals(key)) return PhienDangNhap.coQuyen("STATISTICS_VIEW");
        if (SCREEN_PERSONAL_SALES.equals(key)) return PhienDangNhap.coQuyen("PERSONAL_SALES_VIEW");
        if (SCREEN_SALES.equals(key)) return PhienDangNhap.coQuyen("SALES_MANAGE");
        if (SCREEN_PRODUCT_SEARCH.equals(key)) return PhienDangNhap.coQuyen("PRODUCT_SEARCH");
        if (SCREEN_INVOICE_CREATE.equals(key)) return PhienDangNhap.coQuyen("INVOICE_CREATE");
        if (SCREEN_DELIVERY_CREATE.equals(key)) return PhienDangNhap.coQuyen("DELIVERY_CREATE");
        if (SCREEN_RETURN_CREATE.equals(key)) return PhienDangNhap.coQuyen("RETURN_CREATE");
        if (SCREEN_CUSTOMERS.equals(key)) return PhienDangNhap.coQuyen("CUSTOMER_MANAGE");
        if (SCREEN_MEMBERSHIP.equals(key)) return PhienDangNhap.coQuyen("MEMBERSHIP_MANAGE");
        if (SCREEN_DELIVERY.equals(key)) return PhienDangNhap.coQuyen("DELIVERY_MANAGE");
        if (SCREEN_WARRANTY_TRACK.equals(key)) return PhienDangNhap.coQuyen("WARRANTY_MANAGE");
        if (SCREEN_COMPLAINT_PROCESS.equals(key)) return PhienDangNhap.coQuyen("COMPLAINT_MANAGE");
        if (SCREEN_PROFILE.equals(key)) return PhienDangNhap.coQuyen("PROFILE_VIEW");
        return false;
    }

    private void openInitialScreen() {
        if (!PhienDangNhap.daDangNhap() || PhienDangNhap.getTaiKhoanHienTai().getVaiTro() == null) return;
        String role = PhienDangNhap.getTaiKhoanHienTai().getVaiTro().getCodeVaiTro();

        if ("ADMIN".equals(role) && canAccess(SCREEN_DASHBOARD)) {
            showScreen(SCREEN_DASHBOARD, "Trang chủ");
            return;
        }

        String[][] candidates = {
            {SCREEN_DASHBOARD, "Trang chủ"},
            {SCREEN_EMPLOYEES, "Quản lý nhân viên"},
            {SCREEN_SALES, "Quản lý hóa đơn"},
            {SCREEN_PRODUCT_SEARCH, "Tìm kiếm sản phẩm"},
            {SCREEN_INVOICE_CREATE, "Lập hóa đơn bán hàng"},
            {SCREEN_CUSTOMERS, "Quản lý khách hàng"},
            {SCREEN_DELIVERY, "Quản lý phiếu giao hàng"},
            {SCREEN_WARRANTY_TRACK, "Quản lý bảo hành"},
            {SCREEN_COMPLAINT_PROCESS, "Quản lý khiếu nại"},
            {SCREEN_PRODUCTS, "Quản lý sản phẩm"},
            {SCREEN_SUPPLIERS, "Quản lý nhà cung cấp"},
            {SCREEN_PROMOTIONS, "Quản lý khuyến mãi"},
            {SCREEN_STATISTICS, "Thống kê kinh doanh"},
            {SCREEN_PERSONAL_SALES, "Xem doanh số nhân viên"},
            {SCREEN_ACCOUNTS, "Quản lý tài khoản"},
            {SCREEN_SHIFTS, "Quản lý ca làm việc"},
            {SCREEN_REFUNDS, "Duyệt hoàn tiền"},
            {SCREEN_PROFILE, "Hồ sơ cá nhân"}
        };
        for (String[] item : candidates) {
            if (canAccess(item[0])) {
                showScreen(item[0], item[1]);
                return;
            }
        }

        JOptionPane.showMessageDialog(DialogUtils.owner(this),
                "Tài khoản hiện chưa được Admin cấp chức năng nghiệp vụ nào.",
                "Chưa có quyền", JOptionPane.INFORMATION_MESSAGE);
    }

    public void requestLogout() {
        int result = JOptionPane.showConfirmDialog(DialogUtils.owner(this), "Bạn có chắc muốn đăng xuất?", "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION) return;
        logoutAndReturnToLogin(null);
    }

    private void checkSessionTimeout() {
        if (System.currentTimeMillis() - lastActivity >= SESSION_TIMEOUT_MS) {
            logoutAndReturnToLogin("Phiên đăng nhập đã hết hạn do không hoạt động trong 15 phút.");
            return;
        }
        // Kiểm tra định kỳ để khóa/vô hiệu hóa hoặc thay đổi quyền có hiệu lực với phiên đang mở.
        try {
            if (!PhienDangNhap.daDangNhap()) return;
            int accountId = PhienDangNhap.getTaiKhoanHienTai().getMaTaiKhoan();
            Map<String,Object> row = Db.one(
                    "SELECT a.status account_status,e.status employee_status,a.role_id FROM accounts a "
                  + "JOIN employees e ON e.employee_id=a.employee_id WHERE a.account_id=?", accountId);
            if (row == null || !"ACTIVE".equals(String.valueOf(row.get("account_status")))
                    || !"ACTIVE".equals(String.valueOf(row.get("employee_status")))) {
                logoutAndReturnToLogin("Tài khoản hoặc hồ sơ nhân viên đã bị khóa/ngừng hoạt động.");
                return;
            }
            int dbRoleId = ((Number)row.get("role_id")).intValue();
            if (dbRoleId != PhienDangNhap.getTaiKhoanHienTai().getVaiTro().getMaVaiTro()) {
                logoutAndReturnToLogin("Vai trò tài khoản đã được thay đổi. Vui lòng đăng nhập lại.");
                return;
            }
            Set<String> dbPermissions = new HashSet<String>();
            for (Map<String,Object> p : Db.query(
                    "SELECT p.permission_code FROM role_permissions rp JOIN permissions p ON p.permission_id=rp.permission_id "
                  + "WHERE rp.role_id=? AND rp.granted=1", dbRoleId))
                dbPermissions.add(String.valueOf(p.get("permission_code")));
            if (!dbPermissions.equals(PhienDangNhap.getDanhSachQuyen())) {
                logoutAndReturnToLogin("Quyền của vai trò đã được cập nhật. Vui lòng đăng nhập lại để áp dụng quyền mới.");
                return;
            }
            // Kiểm tra định kỳ để khiếu nại quá 48 giờ được leo thang ngay cả khi CSKH không mở tab hậu mãi.
            backgroundService.refreshComplaintEscalation();
        } catch (Exception ignored) {
            // Lỗi kiểm tra nền không làm mất phiên; các nghiệp vụ vẫn tự kiểm tra quyền khi thực hiện.
        }
    }

    private void logoutAndReturnToLogin(String message) {
        sessionTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        authService.dangXuat();
        super.dispose();
        UIDangNhap login = new UIDangNhap();
        if (message != null) login.showSessionMessage(message);
        login.setVisible(true);
    }

    @Override public void dispose() {
        if (sessionTimer != null) sessionTimer.stop();
        if (activityListener != null) Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        super.dispose();
    }
}

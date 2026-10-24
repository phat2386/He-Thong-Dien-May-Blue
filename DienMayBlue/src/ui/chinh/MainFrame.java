package ui.chinh;
import ui.dungchung.DialogUtils;
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
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.Scrollable;
import service.AuthService;
import service.CustomerService;
import util.SessionManager;
import ui.taikhoan.AccountManagementPanel;
import ui.sauban.AfterSalesPanel;
import ui.xacthuc.LoginFrame;
import ui.dungchung.Refreshable;
import ui.dungchung.UIConstants;
import ui.khachhang.CustomerManagementPanel;
import ui.khachhang.MembershipManagementPanel;
import ui.tongquan.DashboardPanel;
import ui.giaohang.DeliveryManagementPanel;
import ui.nhanvien.EmployeeManagementPanel;
import ui.sanpham.ProductManagementPanel;
import ui.sanpham.ProductSearchPanel;
import ui.hoso.ProfilePanel;
import ui.khuyenmai.PromotionManagementPanel;
import ui.hoantien.RefundApprovalPanel;
import ui.banhang.DeliveryCreatePanel;
import ui.banhang.InvoiceCreatePanel;
import ui.banhang.ReturnRequestPanel;
import ui.banhang.SalesManagementPanel;
import ui.calamviec.ShiftManagementPanel;
import ui.thongke.PersonalSalesPanel;
import ui.thongke.StatisticsPanel;
import ui.nhacungcap.SupplierManagementPanel;
public class MainFrame extends JFrame {
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
    private final AuthService authService;
    private final CustomerService backgroundService = new CustomerService();
    private final AuthService navigationService = new AuthService();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final TopBarPanel topBar;
    private final Map<String,JPanel> screens = new HashMap<String,JPanel>();
    private SidebarPanel sidebar;
    private final List<NavigationEntry> navigationHistory = new ArrayList<NavigationEntry>();
    private int navigationIndex = -1;
    private long lastActivity = System.currentTimeMillis();
    private final AWTEventListener activityListener;
    private final Timer sessionTimer;
    public MainFrame(AuthService authService) {
        this.authService = authService;
        setTitle("Điện Máy Blue");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        topBar = new TopBarPanel(this::goBack, this::goForward);
        sidebar = new SidebarPanel(this);
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
        JScrollPane scroll = new JScrollPane( host, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setBlockIncrement(120);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(9, Integer.MAX_VALUE));
        return scroll;
    }
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
        if (SCREEN_DASHBOARD.equals(key)) return new DashboardPanel();
        if (SCREEN_EMPLOYEES.equals(key)) return new EmployeeManagementPanel();
        if (SCREEN_SALES.equals(key)) return new SalesManagementPanel(this);
        if (SCREEN_PRODUCT_SEARCH.equals(key)) return new ProductSearchPanel(this);
        if (SCREEN_INVOICE_CREATE.equals(key)) return new InvoiceCreatePanel();
        if (SCREEN_DELIVERY_CREATE.equals(key)) return new DeliveryCreatePanel();
        if (SCREEN_RETURN_CREATE.equals(key)) return new ReturnRequestPanel();
        if (SCREEN_PRODUCTS.equals(key)) return new ProductManagementPanel();
        if (SCREEN_CUSTOMERS.equals(key)) return new CustomerManagementPanel();
        if (SCREEN_MEMBERSHIP.equals(key)) return new MembershipManagementPanel();
        if (SCREEN_PROMOTIONS.equals(key)) return new PromotionManagementPanel();
        if (SCREEN_DELIVERY.equals(key)) return new DeliveryManagementPanel();
        if (SCREEN_STATISTICS.equals(key)) return new StatisticsPanel();
        if (SCREEN_PERSONAL_SALES.equals(key)) return new PersonalSalesPanel();
        if (SCREEN_WARRANTY_TRACK.equals(key)) return new AfterSalesPanel(AfterSalesPanel.Section.WARRANTY);
        if (SCREEN_COMPLAINT_PROCESS.equals(key)) return new AfterSalesPanel(AfterSalesPanel.Section.COMPLAINT);
        if (SCREEN_SUPPLIERS.equals(key)) return new SupplierManagementPanel();
        if (SCREEN_ACCOUNTS.equals(key)) return new AccountManagementPanel();
        if (SCREEN_SHIFTS.equals(key)) return new ShiftManagementPanel();
        if (SCREEN_REFUNDS.equals(key)) return new RefundApprovalPanel();
        if (SCREEN_PROFILE.equals(key)) return new ProfilePanel(this, authService);
        return null;
    }
    public void showScreen(String screenKey, String title) {
        if (!displayScreen(screenKey, title)) return;
        if (navigationIndex >= 0 && navigationHistory.get(navigationIndex).screenKey.equals(screenKey)) {
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
            JOptionPane.showMessageDialog(DialogUtils.owner(this), "Tài khoản hiện tại không có quyền truy cập chức năng này.",
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
                JOptionPane.showMessageDialog(DialogUtils.owner(this), "Không thể mở chức năng này.\n" + ex.getMessage(),
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
        topBar.setNavigationEnabled( navigationIndex > 0, navigationIndex >= 0 && navigationIndex < navigationHistory.size() - 1);
    }
    private static final class NavigationEntry {
        final String screenKey;
        String title;
        NavigationEntry(String screenKey, String title) {
            this.screenKey = screenKey;
            this.title = title;
        }
    }
    public boolean canAccess(String key) {
        if (!SessionManager.isLoggedIn() || SessionManager.getCurrentAccount().getRole() == null) return false;
        String role = SessionManager.getCurrentAccount().getRole().getRoleCode();
        if ("ADMIN".equals(role)) {
            return SCREEN_DASHBOARD.equals(key) || SCREEN_EMPLOYEES.equals(key)
                    || SCREEN_SALES.equals(key) || SCREEN_PRODUCT_SEARCH.equals(key)
                    || SCREEN_INVOICE_CREATE.equals(key) || SCREEN_DELIVERY_CREATE.equals(key)
                    || SCREEN_RETURN_CREATE.equals(key) || SCREEN_PRODUCTS.equals(key)
                    || SCREEN_CUSTOMERS.equals(key) || SCREEN_MEMBERSHIP.equals(key)
                    || SCREEN_PROMOTIONS.equals(key) || SCREEN_DELIVERY.equals(key)
                    || SCREEN_STATISTICS.equals(key) || SCREEN_PERSONAL_SALES.equals(key)
                    || SCREEN_WARRANTY_TRACK.equals(key) || SCREEN_COMPLAINT_PROCESS.equals(key)
                    || SCREEN_SUPPLIERS.equals(key) || SCREEN_ACCOUNTS.equals(key)
                    || SCREEN_SHIFTS.equals(key) || SCREEN_REFUNDS.equals(key) || SCREEN_PROFILE.equals(key);
        }
        if (SCREEN_DASHBOARD.equals(key)) return SessionManager.hasPermission("DASHBOARD_VIEW");
        if (SCREEN_EMPLOYEES.equals(key)) return SessionManager.hasPermission("EMPLOYEE_MANAGE");
        if (SCREEN_ACCOUNTS.equals(key)) return SessionManager.hasPermission("ACCOUNT_MANAGE");
        if (SCREEN_SHIFTS.equals(key)) return SessionManager.hasPermission("SHIFT_MANAGE");
        if (SCREEN_PRODUCTS.equals(key)) return SessionManager.hasPermission("PRODUCT_MANAGE");
        if (SCREEN_SUPPLIERS.equals(key)) return SessionManager.hasPermission("SUPPLIER_MANAGE");
        if (SCREEN_PROMOTIONS.equals(key)) return SessionManager.hasPermission("PROMOTION_MANAGE");
        if (SCREEN_REFUNDS.equals(key)) return SessionManager.hasPermission("REFUND_APPROVE");
        if (SCREEN_STATISTICS.equals(key)) return SessionManager.hasPermission("STATISTICS_VIEW");
        if (SCREEN_PERSONAL_SALES.equals(key)) return SessionManager.hasPermission("PERSONAL_SALES_VIEW");
        if (SCREEN_SALES.equals(key)) return SessionManager.hasPermission("SALES_MANAGE");
        if (SCREEN_PRODUCT_SEARCH.equals(key)) return SessionManager.hasPermission("PRODUCT_SEARCH");
        if (SCREEN_INVOICE_CREATE.equals(key)) return SessionManager.hasPermission("INVOICE_CREATE");
        if (SCREEN_DELIVERY_CREATE.equals(key)) return SessionManager.hasPermission("DELIVERY_CREATE");
        if (SCREEN_RETURN_CREATE.equals(key)) return SessionManager.hasPermission("RETURN_CREATE");
        if (SCREEN_CUSTOMERS.equals(key)) return SessionManager.hasPermission("CUSTOMER_MANAGE");
        if (SCREEN_MEMBERSHIP.equals(key)) return SessionManager.hasPermission("MEMBERSHIP_MANAGE");
        if (SCREEN_DELIVERY.equals(key)) return SessionManager.hasPermission("DELIVERY_MANAGE");
        if (SCREEN_WARRANTY_TRACK.equals(key)) return SessionManager.hasPermission("WARRANTY_MANAGE");
        if (SCREEN_COMPLAINT_PROCESS.equals(key)) return SessionManager.hasPermission("COMPLAINT_MANAGE");
        if (SCREEN_PROFILE.equals(key)) return SessionManager.hasPermission("PROFILE_VIEW");
        return false;
    }
    private void openInitialScreen() {
        if (!SessionManager.isLoggedIn() || SessionManager.getCurrentAccount().getRole() == null) return;
        String role = SessionManager.getCurrentAccount().getRole().getRoleCode();
        if ("ADMIN".equals(role) && canAccess(SCREEN_DASHBOARD)) {
            showScreen(SCREEN_DASHBOARD, "Trang chủ");
            return;
        }
        String[][] candidates = {
            {SCREEN_DASHBOARD, "Trang chủ"}, {SCREEN_EMPLOYEES, "Quản lý nhân viên"},
            {SCREEN_SALES, "Quản lý hóa đơn"}, {SCREEN_PRODUCT_SEARCH, "Tìm kiếm sản phẩm"},
            {SCREEN_INVOICE_CREATE, "Lập hóa đơn bán hàng"}, {SCREEN_CUSTOMERS, "Quản lý khách hàng"},
            {SCREEN_DELIVERY, "Quản lý phiếu giao hàng"}, {SCREEN_WARRANTY_TRACK, "Quản lý bảo hành"},
            {SCREEN_COMPLAINT_PROCESS, "Quản lý khiếu nại"}, {SCREEN_PRODUCTS, "Quản lý sản phẩm"},
            {SCREEN_SUPPLIERS, "Quản lý nhà cung cấp"}, {SCREEN_PROMOTIONS, "Quản lý khuyến mãi"},
            {SCREEN_STATISTICS, "Thống kê kinh doanh"}, {SCREEN_PERSONAL_SALES, "Xem doanh số nhân viên"},
            {SCREEN_ACCOUNTS, "Quản lý tài khoản"}, {SCREEN_SHIFTS, "Quản lý ca làm việc"},
            {SCREEN_REFUNDS, "Duyệt hoàn tiền"}, {SCREEN_PROFILE, "Hồ sơ cá nhân"}
        };
        for (String[] item : candidates) {
            if (canAccess(item[0])) {
                showScreen(item[0], item[1]);
                return;
            }
        }
        JOptionPane.showMessageDialog(DialogUtils.owner(this), "Tài khoản hiện chưa được Admin cấp chức năng nghiệp vụ nào.",
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
        try {
            if (!SessionManager.isLoggedIn()) return;
            int accountId = SessionManager.getCurrentAccount().getAccountId();
            Map<String,Object> row = navigationService.sessionState(accountId);
            if (row == null || !"ACTIVE".equals(String.valueOf(row.get("account_status")))
                    || !"ACTIVE".equals(String.valueOf(row.get("employee_status")))) {
                logoutAndReturnToLogin("Tài khoản hoặc hồ sơ nhân viên đã bị khóa/ngừng hoạt động.");
                return;
            }
            int dbRoleId = ((Number)row.get("role_id")).intValue();
            if (dbRoleId != SessionManager.getCurrentAccount().getRole().getRoleId()) {
                logoutAndReturnToLogin("Vai trò tài khoản đã được thay đổi. Vui lòng đăng nhập lại.");
                return;
            }
            Set<String> dbPermissions = navigationService.permissionsForRole(dbRoleId);
            if (!dbPermissions.equals(SessionManager.getPermissions())) {
                logoutAndReturnToLogin("Quyền của vai trò đã được cập nhật. Vui lòng đăng nhập lại để áp dụng quyền mới.");
                return;
            }
            backgroundService.refreshComplaintEscalation();
        } catch (Exception ignored) {
        }
    }
    private void logoutAndReturnToLogin(String message) {
        sessionTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        authService.logout();
        super.dispose();
        LoginFrame login = new LoginFrame();
        if (message != null) login.showSessionMessage(message);
        login.setVisible(true);
    }
    @Override public void dispose() {
        if (sessionTimer != null) sessionTimer.stop();
        if (activityListener != null) Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        super.dispose();
    }
}

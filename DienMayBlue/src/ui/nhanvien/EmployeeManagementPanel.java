package ui.nhanvien;
import ui.dungchung.DialogUtils;
import java.awt.*;
import java.time.LocalDate;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.EmployeeService;
import util.Formats;
import ui.dungchung.*;
public class EmployeeManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField search = UIFactory.field(320);
    private final JComboBox<String> status = UIFactory.combo(
            "Tất cả trạng thái", "Đang hoạt động", "Đã ngừng làm việc", "Đang nghỉ phép");
    private final DefaultTableModel model = UIFactory.readOnlyModel(new Object[]{"Mã NV", "Họ tên", "Số điện thoại", "Thư điện tử", "Phòng ban", "Ngày vào làm", "Trạng thái", "Tài khoản"});
    private final JTable table = UIFactory.table(model);
    private final EmployeeService service = new EmployeeService();
    public EmployeeManagementPanel() {
        super("Quản lý nhân viên", "Quản lý hồ sơ nhân viên; tài khoản và ca làm việc được xử lý tại các mục tương ứng.");
        JButton add = UIFactory.primaryButton("+ Thêm nhân viên");
        JButton edit = UIFactory.secondaryButton("Cập nhật hồ sơ");
        JButton toggle = UIFactory.secondaryButton("Ngừng / Kích hoạt");
        actions.add(add); actions.add(edit); actions.add(toggle);
        body.add(UIFactory.toolbar(new JLabel("Tìm"), search, new JLabel("Trạng thái"), status), BorderLayout.NORTH);
        body.add(UIFactory.scroll(table), BorderLayout.CENTER);
        search.addActionListener(e -> refreshData());
        status.addActionListener(e -> refreshData());
        add.addActionListener(e -> createEmployee());
        edit.addActionListener(e -> editEmployee());
        toggle.addActionListener(e -> toggleStatus());
        refreshData();
    }
    @Override public void refreshData() {
        try {
            String kw = "%" + search.getText().trim() + "%";
            String selected = String.valueOf(status.getSelectedItem());
            String code = null;
            if ("Đang hoạt động".equals(selected)) code = "ACTIVE";
            else if ("Đã ngừng làm việc".equals(selected)) code = "INACTIVE";
            else if ("Đang nghỉ phép".equals(selected)) code = "ON_LEAVE";
            String sql = "SELECT e.employee_id,e.full_name,e.phone,e.email,e.department,e.join_date,e.status,"
                    + "a.status account_status FROM employees e LEFT JOIN accounts a ON a.employee_id=e.employee_id "
                    + "LEFT JOIN roles r ON r.role_id=a.role_id "
                    + "WHERE ISNULL(r.role_code,'')<>'ADMIN' AND (e.employee_id LIKE ? OR e.full_name LIKE ? OR e.phone LIKE ? OR ISNULL(e.email,'') LIKE ?) "
                    + (code == null ? "" : "AND e.status=? ") + "ORDER BY e.full_name";
            java.util.List<Map<String,Object>> rows = code == null ? service.query(sql, kw, kw, kw, kw)
                    : service.query(sql, kw, kw, kw, kw, code);
            model.setRowCount(0);
            for (Map<String,Object> r : rows) {
                model.addRow(new Object[]{
                    r.get("employee_id"), r.get("full_name"), r.get("phone"), Formats.safe(r.get("email")),
                    Formats.safe(r.get("department")), Formats.date(r.get("join_date")), employeeStatus(r.get("status")),
                    r.get("account_status") == null ? "Chưa cấp" : accountStatus(r.get("account_status"))
                });
            }
        } catch (Exception ex) { showError(ex); }
    }
    private String selectedEmployeeId() {
        int vr = table.getSelectedRow();
        return vr < 0 ? null : String.valueOf(model.getValueAt(table.convertRowIndexToModel(vr), 0));
    }
    private void createEmployee() {
        JTextField id = UIFactory.field(180), name = UIFactory.field(260), phone = UIFactory.field(180),
                email = UIFactory.field(260), dept = UIFactory.field(220), join = UIFactory.field(160);
        join.setText(LocalDate.now().toString());
        JPanel p = formPanel();
        addRow(p, "Mã nhân viên", id); addRow(p, "Họ tên", name); addRow(p, "Số điện thoại", phone);
        addRow(p, "Thư điện tử", email); addRow(p, "Phòng ban", dept); addRow(p, "Ngày vào làm (yyyy-MM-dd)", join);
        if (JOptionPane.showConfirmDialog(DialogUtils.owner(this), p, "Thêm nhân viên", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            service.createEmployee(id.getText(), name.getText(), phone.getText(), email.getText(), dept.getText(),
                    LocalDate.parse(join.getText().trim()));
            info("Đã tạo hồ sơ nhân viên. Tài khoản đăng nhập được cấp riêng tại mục Quản lý tài khoản.");
            refreshData();
        } catch (Exception ex) { showError(ex); }
    }
    private void editEmployee() {
        String id = selectedEmployeeId();
        if (id == null) { info("Hãy chọn nhân viên cần cập nhật."); return; }
        try {
            Map<String,Object> r = service.one("SELECT * FROM employees WHERE employee_id=?", id);
            JTextField name = UIFactory.field(260), phone = UIFactory.field(180), email = UIFactory.field(260),
                    dept = UIFactory.field(220), join = UIFactory.field(160);
            name.setText(Formats.safe(r.get("full_name"))); phone.setText(Formats.safe(r.get("phone")));
            email.setText(Formats.safe(r.get("email"))); dept.setText(Formats.safe(r.get("department")));
            join.setText(String.valueOf(r.get("join_date")));
            JPanel p = formPanel();
            addRow(p, "Mã nhân viên", new JLabel(id)); addRow(p, "Họ tên", name); addRow(p, "Số điện thoại", phone);
            addRow(p, "Thư điện tử", email); addRow(p, "Phòng ban", dept); addRow(p, "Ngày vào làm (yyyy-MM-dd)", join);
            if (JOptionPane.showConfirmDialog(DialogUtils.owner(this), p, "Cập nhật nhân viên", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            service.updateEmployee(id, name.getText(), phone.getText(), email.getText(), dept.getText(),
                    LocalDate.parse(join.getText().trim()));
            refreshData();
        } catch (Exception ex) { showError(ex); }
    }
    private void toggleStatus() {
        String id = selectedEmployeeId();
        if (id == null) { info("Hãy chọn nhân viên."); return; }
        try {
            String old = String.valueOf(service.scalar("SELECT status FROM employees WHERE employee_id=?", id));
            boolean activate = !"ACTIVE".equals(old);
            String message = activate
                    ? "Kích hoạt lại hồ sơ nhân viên? Tài khoản đăng nhập (nếu đang vô hiệu hóa) vẫn phải được kích hoạt riêng tại mục Quản lý tài khoản."
                    : "Ngừng hoạt động nhân viên? Tài khoản sẽ bị vô hiệu hóa và các ca từ hôm nay trở đi sẽ bị hủy.";
            if (!confirm(message)) return;
            service.setEmployeeActive(id, activate);
            refreshData();
        } catch (Exception ex) { showError(ex); }
    }
    private String employeeStatus(Object value) {
        String s = String.valueOf(value);
        if ("ACTIVE".equals(s)) return "Đang hoạt động";
        if ("INACTIVE".equals(s)) return "Đã ngừng làm việc";
        if ("ON_LEAVE".equals(s)) return "Đang nghỉ phép";
        return s;
    }
    private String accountStatus(Object value) {
        String s = String.valueOf(value);
        if ("ACTIVE".equals(s)) return "Đang hoạt động";
        if ("LOCKED".equals(s)) return "Đã khóa";
        if ("DISABLED".equals(s)) return "Ngừng hoạt động";
        if ("PENDING".equals(s)) return "Chờ xử lý";
        return s;
    }
    private JPanel formPanel() {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return p;
    }
    private void addRow(JPanel p, String label, Component c) { p.add(new JLabel(label)); p.add(c); }
}

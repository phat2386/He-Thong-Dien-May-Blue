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
    private final JTextField txtSearch = UIFactory.field(320);
    private final JComboBox<String> cmbStatus = UIFactory.combo(
            "Tất cả trạng thái", "Đang hoạt động", "Đã ngừng làm việc", "Đang nghỉ phép");
    private final DefaultTableModel model = UIFactory.readOnlyModel(new Object[]{"Mã NV", "Họ tên", "Số điện thoại", "Thư điện tử", "Phòng ban", "Ngày vào làm", "Trạng thái", "Tài khoản"});
    private final JTable tblTable = UIFactory.table(model);
    private final EmployeeService service = new EmployeeService();
    public EmployeeManagementPanel() {
        super("Quản lý nhân viên", "Quản lý hồ sơ nhân viên; tài khoản và ca làm việc được xử lý tại các mục tương ứng.");
        JButton add = UIFactory.primaryButton("+ Thêm nhân viên");
        JButton edit = UIFactory.secondaryButton("Cập nhật hồ sơ");
        JButton toggle = UIFactory.secondaryButton("Ngừng / Kích hoạt");
        actions.add(add); actions.add(edit); actions.add(toggle);
        body.add(UIFactory.toolbar(new JLabel("Tìm"), txtSearch, new JLabel("Trạng thái"), cmbStatus), BorderLayout.NORTH);
        body.add(UIFactory.scroll(tblTable), BorderLayout.CENTER);
        txtSearch.addActionListener(e -> refreshData());
        cmbStatus.addActionListener(e -> refreshData());
        add.addActionListener(e -> createEmployee());
        edit.addActionListener(e -> editEmployee());
        toggle.addActionListener(e -> toggleStatus());
        refreshData();
    }
    @Override public void refreshData() {
        try {
            String kw = "%" + txtSearch.getText().trim() + "%";
            String selected = String.valueOf(cmbStatus.getSelectedItem());
            String code = null;
            if ("Đang hoạt động".equals(selected)) code = "ACTIVE";
            else if ("Đã ngừng làm việc".equals(selected)) code = "INACTIVE";
            else if ("Đang nghỉ phép".equals(selected)) code = "ON_LEAVE";
            java.util.List<Map<String,Object>> rows = service.searchEmployees(kw, code);
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
        int vr = tblTable.getSelectedRow();
        return vr < 0 ? null : String.valueOf(model.getValueAt(tblTable.convertRowIndexToModel(vr), 0));
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
            Map<String,Object> r = service.findEmployee(id);
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
            String old = String.valueOf(service.employeeStatus(id));
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

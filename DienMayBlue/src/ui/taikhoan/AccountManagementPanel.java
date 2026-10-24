package ui.taikhoan;
import ui.dungchung.DialogUtils;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.AuthService;
import util.Formats;
import util.SessionManager;
import ui.dungchung.*;
public class AccountManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID=1L;
    private final JTextField search=UIFactory.field(300);
    private final JComboBox<String> roleFilter=UIFactory.combo("Tất cả vai trò","Quản lý","Bán hàng","Chăm sóc khách hàng");
    private final JComboBox<String> statusFilter=UIFactory.combo("Tất cả trạng thái","Đang hoạt động","Đã khóa","Ngừng hoạt động","Chờ xử lý");
    private final DefaultTableModel model=UIFactory.readOnlyModel(new Object[]{"Mã","Mã NV","Họ tên","Tên đăng nhập","Vai trò","SĐT","Trạng thái","Đăng nhập gần nhất"});
    private final JTable table=UIFactory.table(model);
    private final AuthService service=new AuthService();
    public AccountManagementPanel(){
        super("Quản lý tài khoản","Cấp, cập nhật, khóa hoặc vô hiệu hóa tài khoản nhân viên. Phân quyền chức năng do Admin thực hiện riêng.");
        JButton add=UIFactory.primaryButton("+ Thêm tài khoản");
        JButton edit=UIFactory.secondaryButton("Sửa tài khoản");
        JButton toggle=UIFactory.secondaryButton("Khóa / Kích hoạt");
        JButton disable=UIFactory.secondaryButton("Vô hiệu hóa");
        JButton delete=UIFactory.secondaryButton("Xóa tài khoản");
        actions.add(add);actions.add(edit);actions.add(toggle);actions.add(disable);actions.add(delete);
        body.add(UIFactory.toolbar(new JLabel("Tìm"),search,new JLabel("Vai trò"),roleFilter,new JLabel("Trạng thái"),statusFilter),BorderLayout.NORTH);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        search.addActionListener(e->refreshData());roleFilter.addActionListener(e->refreshData());statusFilter.addActionListener(e->refreshData());
        add.addActionListener(e->create());edit.addActionListener(e->editAccount());toggle.addActionListener(e->toggle());
        disable.addActionListener(e->disableAccount());delete.addActionListener(e->deleteAccount());
        refreshData();
    }
    @Override public void refreshData(){
        try{
            String kw="%"+search.getText().trim()+"%";
            String role="Tất cả vai trò".equals(roleFilter.getSelectedItem())?null:Formats.code(roleFilter.getSelectedItem());
            String status="Tất cả trạng thái".equals(statusFilter.getSelectedItem())?null:Formats.code(statusFilter.getSelectedItem());
            model.setRowCount(0);
            for(Map<String,Object> r:service.searchAccounts(kw,role,status))
                model.addRow(new Object[]{r.get("account_id"),r.get("employee_id"),r.get("full_name"),r.get("username"),Formats.vi(r.get("role_code")),r.get("phone"),Formats.vi(r.get("status")),Formats.date(r.get("last_login_at"))});
        }catch(Exception ex){showError(ex);}
    }
    private Integer selectedAccount(){int vr=table.getSelectedRow();return vr<0?null:((Number)model.getValueAt(table.convertRowIndexToModel(vr),0)).intValue();}
    private void create(){
        try{
            JComboBox<String> employee=new JComboBox<String>();
            for(Map<String,Object> r:service.availableEmployees())
                employee.addItem(r.get("employee_id")+" - "+r.get("full_name")+" - "+r.get("phone"));
            if(employee.getItemCount()==0){info("Không còn hồ sơ nhân viên đang hoạt động nào chưa có tài khoản. Hãy tạo hồ sơ nhân viên trước.");return;}
            JTextField user=UIFactory.field(180);
            JPasswordField pass=new JPasswordField();pass.setPreferredSize(new Dimension(200,36));
            JComboBox<String> role=roleCombo();
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.add(new JLabel("Nhân viên"));p.add(employee);p.add(new JLabel("Tên đăng nhập"));p.add(user);
            p.add(new JLabel("Mật khẩu ban đầu"));p.add(pass);p.add(new JLabel("Vai trò"));p.add(role);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),p,"Thêm tài khoản",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            String es=String.valueOf(employee.getSelectedItem());String empId=es.substring(0,es.indexOf(" - "));
            int roleId=parseId(role.getSelectedItem());
            service.createAccountForEmployee(empId,user.getText().trim(),new String(pass.getPassword()),roleId);
            info("Đã cấp tài khoản. Người dùng phải đổi mật khẩu ở lần đăng nhập đầu tiên.");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void editAccount(){
        Integer id=selectedAccount();if(id==null){info("Hãy chọn tài khoản.");return;}
        try{
            Map<String,Object> row=service.accountInfo(id);
            JTextField user=UIFactory.field(180);user.setText(Formats.safe(row.get("username")));
            JComboBox<String> role=roleCombo();
            for(int i=0;i<role.getItemCount();i++)if(String.valueOf(role.getItemAt(i)).startsWith(String.valueOf(row.get("role_id"))+" -"))role.setSelectedIndex(i);
            if(id==SessionManager.getCurrentAccount().getAccountId()) role.setEnabled(false);
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.add(new JLabel("Nhân viên"));p.add(new JLabel(Formats.safe(row.get("full_name"))+" ("+row.get("employee_id")+")"));
            p.add(new JLabel("Tên đăng nhập"));p.add(user);p.add(new JLabel("Vai trò"));p.add(role);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),p,"Sửa tài khoản",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            service.updateAccount(id,user.getText().trim(),parseId(role.getSelectedItem()));
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void toggle(){
        Integer id=selectedAccount();if(id==null){info("Hãy chọn tài khoản.");return;}
        try{
            String st=service.accountStatus(id);
            String next="ACTIVE".equals(st)?"LOCKED":"ACTIVE";
            String action="ACTIVE".equals(next)?"Kích hoạt":"Khóa";
            if(!"ACTIVE".equals(next) && !warnActiveSession(id)) return;
            if(!confirm(action+" tài khoản này?"))return;
            service.changeAccountStatus(id,next);
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void disableAccount(){
        Integer id=selectedAccount();if(id==null){info("Hãy chọn tài khoản.");return;}
        if(!warnActiveSession(id))return;
        if(!confirm("Vô hiệu hóa tài khoản? Tài khoản sẽ không thể đăng nhập nhưng dữ liệu lịch sử vẫn được giữ lại."))return;
        try{service.changeAccountStatus(id,"DISABLED");refreshData();}catch(Exception ex){showError(ex);}
    }
    private void deleteAccount(){
        Integer id=selectedAccount();if(id==null){info("Hãy chọn tài khoản.");return;}
        if(!warnActiveSession(id))return;
        if(!confirm("Xóa tài khoản đã chọn? Nếu tài khoản đã có lịch sử, hệ thống sẽ tự chuyển sang Vô hiệu hóa để bảo toàn dữ liệu."))return;
        try{
            boolean deleted=service.deleteAccountOrDisable(id);
            info(deleted?"Đã xóa tài khoản chưa phát sinh lịch sử. Hồ sơ nhân viên vẫn được giữ nguyên."
                    :"Tài khoản đã có lịch sử nên được Vô hiệu hóa thay vì xóa vật lý.");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private boolean warnActiveSession(int accountId){
        try{
            if(service.hasActiveSession(accountId))
                return confirm("Tài khoản đang có phiên đăng nhập. Nếu tiếp tục, phiên đó sẽ bị kết thúc và ứng dụng sẽ tự đưa người dùng về màn hình đăng nhập.");
            return true;
        }catch(Exception ex){showError(ex);return false;}
    }
    private JComboBox<String> roleCombo() throws Exception {
        JComboBox<String> role=new JComboBox<String>();
        for(Map<String,Object> r:service.manageableRoles())
            role.addItem(r.get("role_id")+" - "+r.get("role_name")+" ["+r.get("role_code")+"]");
        return role;
    }
    private int parseId(Object value){String s=String.valueOf(value);return Integer.parseInt(s.substring(0,s.indexOf(" - ")));}
}

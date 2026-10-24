package ui.calamviec;
import ui.dungchung.DialogUtils;
import java.awt.*;
import java.time.LocalDate;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.EmployeeService;
import util.Formats;
import ui.dungchung.*;
public class ShiftManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID=1L;
    private final JTextField date=UIFactory.field(110);
    private final DefaultTableModel model=UIFactory.readOnlyModel(new Object[]{"Mã","Ngày","Ca","Thời gian","Mã NV","Nhân viên","Vai trò","Trạng thái","Phủ ca"});
    private final JTable table=UIFactory.table(model);
    private final EmployeeService service=new EmployeeService();
    public ShiftManagementPanel(){
        super("Phân ca làm việc","Mỗi nhân viên tối đa một ca/ngày; kiểm tra nghỉ phép và đảm bảo ca có nhân viên bán hàng.");
        date.setText(LocalDate.now().toString());
        JButton assign=UIFactory.primaryButton("+ Phân ca"),remove=UIFactory.secondaryButton("Hủy phân ca"),refresh=UIFactory.secondaryButton("Làm mới");
        actions.add(assign);actions.add(remove);actions.add(refresh);
        body.add(UIFactory.toolbar(new JLabel("Ngày yyyy-MM-dd"),date),BorderLayout.NORTH);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        date.addActionListener(e->refreshData());assign.addActionListener(e->assign());remove.addActionListener(e->remove());refresh.addActionListener(e->refreshData());
        refreshData();
    }
    @Override public void refreshData(){
        try{
            LocalDate d=LocalDate.parse(date.getText().trim());model.setRowCount(0);
            String sql="SELECT sa.assignment_id,sa.work_date,s.shift_id,s.shift_name,s.start_time,s.end_time,e.employee_id,e.full_name,ro.role_code,sa.status,"
                    +"CASE WHEN EXISTS(SELECT 1 FROM shift_assignments sx JOIN accounts ax ON ax.employee_id=sx.employee_id "
                    +"JOIN roles rx ON rx.role_id=ax.role_id WHERE sx.work_date=sa.work_date AND sx.shift_id=sa.shift_id "
                    +"AND sx.status='ASSIGNED' AND rx.role_code='SALES') THEN N'Đủ' ELSE N'Thiếu NV bán hàng' END coverage "
                    +"FROM shift_assignments sa JOIN shifts s ON s.shift_id=sa.shift_id JOIN employees e ON e.employee_id=sa.employee_id "
                    +"LEFT JOIN accounts a ON a.employee_id=e.employee_id LEFT JOIN roles ro ON ro.role_id=a.role_id "
                    +"WHERE sa.work_date=? AND sa.status='ASSIGNED' ORDER BY s.start_time,e.full_name";
            for(Map<String,Object> r:service.query(sql,d))
                model.addRow(new Object[]{r.get("assignment_id"),Formats.date(r.get("work_date")),r.get("shift_name"),r.get("start_time")+" - "+r.get("end_time"),
                        r.get("employee_id"),r.get("full_name"),Formats.vi(r.get("role_code")),Formats.vi(r.get("status")),r.get("coverage")});
        }catch(Exception ex){showError(ex);}
    }
    private void assign(){
        try{
            LocalDate d=LocalDate.parse(date.getText().trim());
            JComboBox<String> emp=new JComboBox<String>(),shift=new JComboBox<String>();
            for(Map<String,Object> r:service.query(
                    "SELECT e.employee_id,e.full_name,r.role_name,r.role_code FROM employees e "
                  + "JOIN accounts a ON a.employee_id=e.employee_id JOIN roles r ON r.role_id=a.role_id "
                  + "WHERE e.status='ACTIVE' AND a.status='ACTIVE' AND r.role_code IN ('SALES','CUSTOMER_SERVICE') ORDER BY e.full_name"))
                emp.addItem(r.get("employee_id")+" - "+r.get("full_name")+" ("+r.get("role_name")+")");
            for(Map<String,Object> r:service.query("SELECT shift_id,shift_name,start_time,end_time FROM shifts WHERE active=1 ORDER BY start_time"))
                shift.addItem(r.get("shift_id")+" - "+r.get("shift_name")+" "+r.get("start_time")+"-"+r.get("end_time"));
            if(emp.getItemCount()==0 || shift.getItemCount()==0){info("Không có nhân viên/ca làm việc hợp lệ để phân công.");return;}
            JPanel p=new JPanel(new GridLayout(0,2,8,8));p.add(new JLabel("Nhân viên"));p.add(emp);p.add(new JLabel("Ca"));p.add(shift);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),p,"Phân ca "+d,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            String es=String.valueOf(emp.getSelectedItem()),ss=String.valueOf(shift.getSelectedItem());
            String employeeId=es.substring(0,es.indexOf(" - "));
            int shiftId=Integer.parseInt(ss.substring(0,ss.indexOf(" - ")));
            service.assignShift(employeeId,d,shiftId);
            refreshData();
            Object sales=service.scalar("SELECT COUNT(*) FROM shift_assignments sa JOIN accounts a ON a.employee_id=sa.employee_id "
                    +"JOIN roles r ON r.role_id=a.role_id WHERE sa.work_date=? AND sa.shift_id=? AND sa.status='ASSIGNED' AND r.role_code='SALES'",d,shiftId);
            if(((Number)sales).intValue()==0)
                info("Đã lưu phân ca, nhưng ca này đang thiếu Nhân viên bán hàng. Hãy bổ sung trước khi ca bắt đầu.");
        }catch(Exception ex){showError(ex);}
    }
    private void remove(){
        int vr=table.getSelectedRow();if(vr<0){info("Hãy chọn phân ca.");return;}
        long id=((Number)model.getValueAt(table.convertRowIndexToModel(vr),0)).longValue();
        if(!confirm("Hủy phân ca đã chọn? Nhân viên sẽ nhận thông báo hủy ca."))return;
        try{service.cancelShiftAssignment(id);refreshData();}catch(Exception ex){showError(ex);}
    }
}

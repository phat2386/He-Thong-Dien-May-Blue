package ui.giaohang;
import ui.dungchung.DialogUtils;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.OperationService;
import util.Formats;
import util.SessionManager;
import ui.dungchung.*;
public class DeliveryManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID=1L;
    private final JComboBox<String> status=UIFactory.combo("Tất cả","Đang chuẩn bị","Đang giao","Đã giao","Đã lắp đặt","Giao không thành công");
    private final JComboBox<String> viewMode=UIFactory.combo("Phiếu của tôi","Chưa phân công","Tất cả phiếu");
    private final JTextField deliveryDate=UIFactory.field(125);
    private final DefaultTableModel model=UIFactory.readOnlyModel(new Object[]{"Mã giao","Hóa đơn","Khách hàng","Địa chỉ","Ngày","Khung giờ","Phụ trách","Lắp đặt","Trạng thái"});
    private final JTable table=UIFactory.table(model);
    private final OperationService service=new OperationService();
    public DeliveryManagementPanel(){
        super("Quản lý giao hàng","Phân công, xem phiếu được giao và cập nhật đúng người phụ trách.");
        JButton assign=UIFactory.secondaryButton("Phân công giao hàng");
        JButton start=UIFactory.primaryButton("Bắt đầu giao");
        JButton done=UIFactory.secondaryButton("Hoàn tất");
        JButton fail=UIFactory.secondaryButton("Giao thất bại");
        JButton retry=UIFactory.secondaryButton("Lập lịch giao lại");
        actions.add(start);
        if(SessionManager.hasPermission("DELIVERY_ASSIGN")) actions.add(assign);
        actions.add(done);actions.add(retry);actions.add(fail);
        deliveryDate.setText(LocalDate.now().toString());
        status.setSelectedItem("Đang chuẩn bị"); // UC027 mặc định: phiếu của tôi đang chuẩn bị, trong ngày.
        JButton clearDate=UIFactory.secondaryButton("Tất cả ngày");
        JPanel filters=UIFactory.toolbar(new JLabel("Phạm vi"),viewMode,new JLabel("Ngày giao"),deliveryDate,clearDate,new JLabel("Trạng thái"),status);
        body.add(filters,BorderLayout.NORTH);body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        status.addActionListener(e->refreshData()); viewMode.addActionListener(e->refreshData());
        deliveryDate.addActionListener(e->refreshData());
        clearDate.addActionListener(e->{deliveryDate.setText("");refreshData();});
        assign.addActionListener(e->assignDelivery());
        start.addActionListener(e->change("DELIVERING"));
        done.addActionListener(e->complete());
        fail.addActionListener(e->fail());
        retry.addActionListener(e->retry());
        refreshData();
    }
    public void refreshData(){
        try{
            String st=String.valueOf(status.getSelectedItem());
            String statusCode="Tất cả".equals(st)?null:Formats.code(st);
            String emp=SessionManager.getCurrentAccount().getEmployee().getEmployeeId();
            String mode=String.valueOf(viewMode.getSelectedItem());
            String dateText=deliveryDate.getText().trim();
            if(!dateText.isEmpty()) LocalDate.parse(dateText); // báo lỗi sớm nếu nhập sai.
            StringBuilder sql=new StringBuilder(
                    "SELECT d.delivery_id,d.invoice_id,ISNULL(c.full_name,N'Khách vãng lai') customer,d.delivery_address,d.delivery_date,d.time_from,d.time_to,"+
                    "ISNULL(e.full_name,N'Chưa phân công') employee,d.install_required,d.status " +
                    "FROM deliveries d JOIN invoices i ON i.invoice_id=d.invoice_id LEFT JOIN customers c ON c.customer_id=i.customer_id " +
                    "LEFT JOIN employees e ON e.employee_id=d.assigned_employee_id WHERE 1=1 ");
            java.util.List<Object> params=new ArrayList<Object>();
            if("Phiếu của tôi".equals(mode)){sql.append("AND d.assigned_employee_id=? ");params.add(emp);}
            else if("Chưa phân công".equals(mode)){sql.append("AND d.assigned_employee_id IS NULL ");}
            if(statusCode!=null){sql.append("AND d.status=? ");params.add(statusCode);}
            if(!dateText.isEmpty()){sql.append("AND d.delivery_date=? ");params.add(LocalDate.parse(dateText));}
            sql.append("ORDER BY d.delivery_date,d.time_from,d.delivery_id");
            java.util.List<Map<String,Object>> rows=service.query(sql.toString(),params.toArray());
            model.setRowCount(0);
            for(Map<String,Object> r:rows) model.addRow(new Object[]{
                    r.get("delivery_id"),r.get("invoice_id"),r.get("customer"),r.get("delivery_address"),
                    Formats.date(r.get("delivery_date")),r.get("time_from")+" - "+r.get("time_to"),r.get("employee"),
                    Boolean.TRUE.equals(r.get("install_required"))?"Có":"Không",Formats.vi(r.get("status"))});
        }catch(Exception ex){showError(ex);}
    }
    private void assignDelivery(){
        String id=selected(); if(id==null){info("Hãy chọn phiếu giao hàng cần phân công.");return;}
        try{
            int r=table.convertRowIndexToModel(table.getSelectedRow());
            String currentAssignee=String.valueOf(model.getValueAt(r,6));
            String currentStatus=String.valueOf(model.getValueAt(r,8));
            if(!"Đang chuẩn bị".equals(currentStatus)){
                info("Chỉ phiếu đang chuẩn bị mới được phân công hoặc phân công lại.");return;
            }
            JComboBox<String> employee=new JComboBox<String>();
            for(Map<String,Object> row:service.getAvailableDeliveryEmployees(id))
                employee.addItem(row.get("employee_id")+" - "+row.get("full_name"));
            if(employee.getItemCount()==0){
                info("Không có nhân viên bán hàng đang hoạt động và có ca phù hợp với khung giờ giao này.");return;
            }
            boolean reassign=!"Chưa phân công".equals(currentAssignee);
            if(reassign){
                int ok=JOptionPane.showConfirmDialog(DialogUtils.owner(this),
                        "Phiếu hiện đang được phân công cho "+currentAssignee+".\nBạn có chắc muốn phân công lại?",
                        "Xác nhận phân công lại",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
                if(ok!=JOptionPane.YES_OPTION)return;
            }
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),employee,reassign?"Chọn người nhận phân công lại":"Phân công giao hàng",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            String s=String.valueOf(employee.getSelectedItem());
            String emp=s.substring(0,s.indexOf(" - "));
            service.assignDelivery(id,emp,reassign);
            info((reassign?"Đã phân công lại ":"Đã phân công ")+id+" cho "+s.substring(s.indexOf(" - ")+3)+".");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private String selected(){
        int vr=table.getSelectedRow(); return vr<0?null:String.valueOf(model.getValueAt(table.convertRowIndexToModel(vr),0));
    }
    private void change(String next){
        String id=selected(); if(id==null){info("Hãy chọn phiếu giao hàng.");return;}
        try{service.updateDeliveryStatus(id,next,"Bắt đầu giao hàng");refreshData();}catch(Exception ex){showError(ex);}
    }
    private void complete(){
        int vr=table.getSelectedRow(); if(vr<0){info("Hãy chọn phiếu.");return;}
        int r=table.convertRowIndexToModel(vr);
        String id=String.valueOf(model.getValueAt(r,0));
        JTextField proof=UIFactory.field(300),signature=UIFactory.field(300),note=UIFactory.field(300);
        JPanel p=new JPanel(new GridLayout(0,2,8,8));
        p.add(new JLabel("Ảnh bằng chứng (đường dẫn)"));p.add(proof);
        p.add(new JLabel("Chữ ký (đường dẫn)"));p.add(signature);
        p.add(new JLabel("Ghi chú"));p.add(note);
        if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),p,"Xác nhận giao hàng hoàn tất",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        try{
            service.completeDelivery(id,proof.getText(),signature.getText(),note.getText());
            info("Đã xác nhận hoàn tất phiếu "+id+".");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void fail(){
        String id=selected(); if(id==null){info("Hãy chọn phiếu.");return;}
        String reason=JOptionPane.showInputDialog(DialogUtils.owner(this),"Lý do giao không thành công:");
        if(reason==null)return;
        try{
            LocalDate next=service.failDelivery(id,reason);
            info("Đã ghi nhận giao không thành công. Hệ thống đề xuất giao lại ngày "+next+".");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void retry(){
        String id=selected(); if(id==null){info("Hãy chọn phiếu.");return;}
        try{
            LocalDate next=service.retryDelivery(id);
            info("Đã chuyển lịch giao sang "+next+". Phiếu được bỏ phân công để chọn lại người có ca phù hợp.");
            viewMode.setSelectedItem("Chưa phân công");
            deliveryDate.setText(next.toString());
            status.setSelectedItem("Đang chuẩn bị");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
}

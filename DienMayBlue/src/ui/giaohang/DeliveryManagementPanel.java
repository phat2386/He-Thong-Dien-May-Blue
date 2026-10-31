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
    private final JComboBox<String> cmbStatus=UIFactory.combo("Tất cả","Đang chuẩn bị","Đang giao","Đã giao","Đã lắp đặt","Giao không thành công");
    private final JComboBox<String> cmbViewMode=UIFactory.combo("Phiếu của tôi","Chưa phân công","Tất cả phiếu");
    private final JTextField txtDeliveryDate=UIFactory.field(125);
    private final DefaultTableModel model=UIFactory.readOnlyModel(new Object[]{"Mã giao","Hóa đơn","Khách hàng","Địa chỉ","Ngày","Khung giờ","Phụ trách","Lắp đặt","Trạng thái"});
    private final JTable tblTable=UIFactory.table(model);
    private final OperationService service=new OperationService();
    public DeliveryManagementPanel(){
        super("Quản lý giao hàng","Phân công, xem phiếu được giao và cập nhật đúng người phụ trách.");
        JButton btnAssign=UIFactory.secondaryButton("Phân công giao hàng");
        JButton btnStart=UIFactory.primaryButton("Bắt đầu giao");
        JButton btnDone=UIFactory.secondaryButton("Hoàn tất");
        JButton btnFail=UIFactory.secondaryButton("Giao thất bại");
        JButton btnRetry=UIFactory.secondaryButton("Lập lịch giao lại");
        pnlActions.add(btnStart);
        if(SessionManager.hasPermission("DELIVERY_ASSIGN")) pnlActions.add(btnAssign);
        pnlActions.add(btnDone);pnlActions.add(btnRetry);pnlActions.add(btnFail);
        txtDeliveryDate.setText(LocalDate.now().toString());
        cmbStatus.setSelectedItem("Đang chuẩn bị"); // UC027 mặc định: phiếu của tôi đang chuẩn bị, trong ngày.
        JButton btnClearDate=UIFactory.secondaryButton("Tất cả ngày");
        JPanel pnlFilters=UIFactory.toolbar(new JLabel("Phạm vi"),cmbViewMode,new JLabel("Ngày giao"),txtDeliveryDate,btnClearDate,new JLabel("Trạng thái"),cmbStatus);
        pnlBody.add(pnlFilters,BorderLayout.NORTH);pnlBody.add(UIFactory.scroll(tblTable),BorderLayout.CENTER);
        cmbStatus.addActionListener(e->refreshData()); cmbViewMode.addActionListener(e->refreshData());
        txtDeliveryDate.addActionListener(e->refreshData());
        btnClearDate.addActionListener(e->{txtDeliveryDate.setText("");refreshData();});
        btnAssign.addActionListener(e->assignDelivery());
        btnStart.addActionListener(e->change("DELIVERING"));
        btnDone.addActionListener(e->complete());
        btnFail.addActionListener(e->fail());
        btnRetry.addActionListener(e->retry());
        refreshData();
    }
    public void refreshData(){
        try{
            String st=String.valueOf(cmbStatus.getSelectedItem());
            String statusCode="Tất cả".equals(st)?null:Formats.code(st);
            String emp=SessionManager.getCurrentAccount().getEmployee().getEmployeeId();
            String mode=String.valueOf(cmbViewMode.getSelectedItem());
            String dateText=txtDeliveryDate.getText().trim();
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
            int r=tblTable.convertRowIndexToModel(tblTable.getSelectedRow());
            String currentAssignee=String.valueOf(model.getValueAt(r,6));
            String currentStatus=String.valueOf(model.getValueAt(r,8));
            if(!"Đang chuẩn bị".equals(currentStatus)){
                info("Chỉ phiếu đang chuẩn bị mới được phân công hoặc phân công lại.");return;
            }
            JComboBox<String> cmbEmployee=new JComboBox<String>();
            for(Map<String,Object> row:service.getAvailableDeliveryEmployees(id))
                cmbEmployee.addItem(row.get("employee_id")+" - "+row.get("full_name"));
            if(cmbEmployee.getItemCount()==0){
                info("Không có nhân viên bán hàng đang hoạt động và có ca phù hợp với khung giờ giao này.");return;
            }
            boolean reassign=!"Chưa phân công".equals(currentAssignee);
            if(reassign){
                int ok=JOptionPane.showConfirmDialog(DialogUtils.owner(this),
                        "Phiếu hiện đang được phân công cho "+currentAssignee+".\nBạn có chắc muốn phân công lại?",
                        "Xác nhận phân công lại",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
                if(ok!=JOptionPane.YES_OPTION)return;
            }
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),cmbEmployee,reassign?"Chọn người nhận phân công lại":"Phân công giao hàng",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            String s=String.valueOf(cmbEmployee.getSelectedItem());
            String emp=s.substring(0,s.indexOf(" - "));
            service.assignDelivery(id,emp,reassign);
            info((reassign?"Đã phân công lại ":"Đã phân công ")+id+" cho "+s.substring(s.indexOf(" - ")+3)+".");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private String selected(){
        int vr=tblTable.getSelectedRow(); return vr<0?null:String.valueOf(model.getValueAt(tblTable.convertRowIndexToModel(vr),0));
    }
    private void change(String next){
        String id=selected(); if(id==null){info("Hãy chọn phiếu giao hàng.");return;}
        try{service.updateDeliveryStatus(id,next,"Bắt đầu giao hàng");refreshData();}catch(Exception ex){showError(ex);}
    }
    private void complete(){
        int vr=tblTable.getSelectedRow(); if(vr<0){info("Hãy chọn phiếu.");return;}
        int r=tblTable.convertRowIndexToModel(vr);
        String id=String.valueOf(model.getValueAt(r,0));
        JTextField txtProof=UIFactory.field(300),txtSignature=UIFactory.field(300),txtNote=UIFactory.field(300);
        JPanel pnlP=new JPanel(new GridLayout(0,2,8,8));
        pnlP.add(new JLabel("Ảnh bằng chứng (đường dẫn)"));pnlP.add(txtProof);
        pnlP.add(new JLabel("Chữ ký (đường dẫn)"));pnlP.add(txtSignature);
        pnlP.add(new JLabel("Ghi chú"));pnlP.add(txtNote);
        if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),pnlP,"Xác nhận giao hàng hoàn tất",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        try{
            service.completeDelivery(id,txtProof.getText(),txtSignature.getText(),txtNote.getText());
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
            cmbViewMode.setSelectedItem("Chưa phân công");
            txtDeliveryDate.setText(next.toString());
            cmbStatus.setSelectedItem("Đang chuẩn bị");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
}

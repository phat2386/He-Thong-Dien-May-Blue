package ui.sauban;
import ui.dungchung.DialogUtils;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.CustomerService;
import util.Formats;
import util.SessionManager;
import ui.dungchung.*;
public class AfterSalesPanel extends BaseScreenPanel {
    public enum Section { WARRANTY, COMPLAINT }
    private static final long serialVersionUID=1L;
    private final Section section;
    private final CustomerService service=new CustomerService();
    private final DefaultTableModel warrantyModel=UIFactory.readOnlyModel(new Object[]{"Mã BH","Khách hàng","Sản phẩm","Ngày nhận","Hạn BH","Vấn đề","Trạng thái","Phí đổi máy"});
    private final JTable tblWarrantyTable=UIFactory.table(warrantyModel);
    private final DefaultTableModel complaintModel=UIFactory.readOnlyModel(new Object[]{"Mã KN","Khách hàng/SĐT","Nội dung","Ưu tiên","Trạng thái","Phụ trách","Hạn xử lý","Leo thang"});
    private final JTable tblComplaintTable=UIFactory.table(complaintModel);
    public AfterSalesPanel(Section section){
        super(section == Section.WARRANTY ? "Bảo hành" : "Khiếu nại",
              section == Section.WARRANTY ? "Tiếp nhận và theo dõi phiếu bảo hành." : "Tiếp nhận và xử lý khiếu nại khách hàng.");
        this.section = section;
        JButton btnNewWarranty=UIFactory.primaryButton("+ Tiếp nhận bảo hành");
        JButton btnUpdateWarranty=UIFactory.secondaryButton("Theo dõi / cập nhật");
        JButton btnNewComplaint=UIFactory.primaryButton("+ Tiếp nhận khiếu nại");
        JButton btnUpdateComplaint=UIFactory.secondaryButton("Xử lý khiếu nại");
        JButton btnRefresh=UIFactory.secondaryButton("Làm mới");
        boolean warrantyAllowed=SessionManager.hasPermission("WARRANTY_MANAGE");
        boolean complaintAllowed=SessionManager.hasPermission("COMPLAINT_MANAGE");
        if(section == Section.WARRANTY && warrantyAllowed){
            pnlActions.add(btnNewWarranty); pnlActions.add(btnUpdateWarranty);
            pnlBody.add(UIFactory.scroll(tblWarrantyTable), BorderLayout.CENTER);
            btnNewWarranty.addActionListener(e->createWarranty());
            btnUpdateWarranty.addActionListener(e->updateWarranty());
        }else if(section == Section.COMPLAINT && complaintAllowed){
            pnlActions.add(btnNewComplaint); pnlActions.add(btnUpdateComplaint);
            pnlBody.add(UIFactory.scroll(tblComplaintTable), BorderLayout.CENTER);
            btnNewComplaint.addActionListener(e->createComplaint());
            btnUpdateComplaint.addActionListener(e->updateComplaint());
        }
        pnlActions.add(btnRefresh);
        btnRefresh.addActionListener(e->refreshData());
        refreshData();
    }
    @Override public void refreshData(){
        try{
            service.refreshComplaintEscalation();
            if(section == Section.WARRANTY){
                warrantyModel.setRowCount(0);
                for(Map<String,Object> r:service.warranties())
                    warrantyModel.addRow(new Object[]{r.get("warranty_id"),r.get("full_name"),r.get("product_name"),Formats.date(r.get("received_at")),
                            Formats.date(r.get("warranty_end_date")),r.get("issue_description"),Formats.vi(r.get("status")),
                            r.get("replacement_charge")==null?"":Formats.money(r.get("replacement_charge"))});
            }else{
                complaintModel.setRowCount(0);
                for(Map<String,Object> r:service.complaints())
                    complaintModel.addRow(new Object[]{r.get("complaint_id"),r.get("customer"),r.get("content"),Formats.vi(r.get("priority")),
                            Formats.vi(r.get("status")),Formats.safe(r.get("assigned_name")),formatDateTime(r.get("due_at")),
                            Boolean.TRUE.equals(r.get("escalated"))?"Đã báo quản lý":""});
            }
        }catch(Exception ex){showError(ex);}
    }
    private void createWarranty(){
        try{
            String phone=JOptionPane.showInputDialog(DialogUtils.owner(this),"Số điện thoại khách hàng:");
            if(phone==null)return;
            phone=phone.trim();
            if(phone.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số điện thoại khách hàng.");
            java.util.List<Map<String,Object>> rows=service.warrantyOptions(phone);
            if(rows.isEmpty()) throw new IllegalArgumentException("Không tìm thấy sản phẩm còn thời hạn bảo hành cho số điện thoại này.");
            JComboBox<WarrantyOption> cmbCombo=new JComboBox<WarrantyOption>();
            for(Map<String,Object> r:rows) cmbCombo.addItem(new WarrantyOption(r));
            JTextArea txaIssue=new JTextArea(4,30);txaIssue.setLineWrap(true);txaIssue.setWrapStyleWord(true);
            JPanel pnlP=new JPanel(new BorderLayout(8,8));
            JPanel pnlTop=new JPanel(new GridLayout(0,2,8,8));
            pnlTop.add(new JLabel("Sản phẩm đã mua"));pnlTop.add(cmbCombo);
            pnlTop.add(new JLabel("Hạn bảo hành"));
            JLabel lblEndLabel=new JLabel();pnlTop.add(lblEndLabel);
            pnlP.add(pnlTop,BorderLayout.NORTH);
            JPanel pnlIssuePanel=new JPanel(new BorderLayout(6,6));
            pnlIssuePanel.add(new JLabel("Mô tả lỗi"),BorderLayout.NORTH);pnlIssuePanel.add(new JScrollPane(txaIssue),BorderLayout.CENTER);
            pnlP.add(pnlIssuePanel,BorderLayout.CENTER);
            Runnable updateEnd=()->{
                WarrantyOption o=(WarrantyOption)cmbCombo.getSelectedItem();
                lblEndLabel.setText(o==null?"":o.warrantyEnd.toString()+" ("+o.warrantyMonths+" tháng)");
            };
            cmbCombo.addActionListener(e->updateEnd.run());updateEnd.run();
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),pnlP,"Tiếp nhận bảo hành",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            WarrantyOption selected=(WarrantyOption)cmbCombo.getSelectedItem();
            if(selected==null)return;
            String id=service.createWarranty(selected.customerId,selected.detailId,txaIssue.getText().trim());
            info("Đã tạo phiếu bảo hành "+id+". Hạn bảo hành được hệ thống xác định tự động.");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void updateWarranty(){
        int vr=tblWarrantyTable.getSelectedRow();
        if(vr<0){info("Hãy chọn phiếu bảo hành.");return;}
        int r=tblWarrantyTable.convertRowIndexToModel(vr);
        String id=String.valueOf(warrantyModel.getValueAt(r,0));
        String oldCode=Formats.code(warrantyModel.getValueAt(r,6));
        String[] codes=warrantyNextStatuses(oldCode);
        if(codes.length==0){info("Phiếu bảo hành này đã kết thúc, không thể chuyển trạng thái tiếp.");return;}
        String[] labels=Formats.labels(codes);
        String st=(String)JOptionPane.showInputDialog(DialogUtils.owner(this),"Trạng thái mới:","Cập nhật bảo hành",JOptionPane.PLAIN_MESSAGE,null,labels,labels[0]);
        if(st==null)return;
        String statusCode=Formats.code(st);
        String note=JOptionPane.showInputDialog(DialogUtils.owner(this),"Ghi chú xử lý:","");
        if(note==null)return;
        try{
            BigDecimal accessoryFee=BigDecimal.ZERO;
            if("REPLACEMENT".equals(statusCode)){
                String acc=JOptionPane.showInputDialog(DialogUtils.owner(this),"Phí phụ kiện thiếu/hư hỏng (nếu không có nhập 0):","0");
                if(acc==null)return;
                accessoryFee=new BigDecimal(acc.trim().isEmpty()?"0":acc.trim());
                BigDecimal charge=service.calculateReplacementCharge(id,accessoryFee);
                if(!confirm("Phí khách cần thanh toán khi đổi máy: "+Formats.money(charge)+"\nXác nhận chuyển sang xử lý đổi máy?"))return;
            }
            service.updateWarrantyStatus(id,statusCode,note,accessoryFee);
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private String[] warrantyNextStatuses(String old){
        if("REPAIRING".equals(old)) return new String[]{"COMPLETED","REPLACEMENT","REJECTED"};
        if("REPLACEMENT".equals(old)) return new String[]{"COMPLETED"};
        if("COMPLETED".equals(old)) return new String[]{"RETURNED"};
        return new String[0];
    }
    private void createComplaint(){
        try{
            JTextField txtPhone=UIFactory.field(180);
            JTextArea txaContent=new JTextArea(5,32);txaContent.setLineWrap(true);txaContent.setWrapStyleWord(true);
            JPanel pnlP=new JPanel(new BorderLayout(8,8));
            JPanel pnlTop=new JPanel(new GridLayout(1,2,8,8));pnlTop.add(new JLabel("Số điện thoại"));pnlTop.add(txtPhone);pnlP.add(pnlTop,BorderLayout.NORTH);
            JPanel pnlBodyP=new JPanel(new BorderLayout(6,6));pnlBodyP.add(new JLabel("Nội dung khiếu nại"),BorderLayout.NORTH);pnlBodyP.add(new JScrollPane(txaContent),BorderLayout.CENTER);pnlP.add(pnlBodyP,BorderLayout.CENTER);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),pnlP,"Ghi nhận khiếu nại",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            String customerId=service.customerIdByPhone(txtPhone.getText().trim());
            String id=service.createComplaint(customerId,txtPhone.getText().trim(),txaContent.getText().trim());
            info("Đã tạo khiếu nại "+id+". Hệ thống tự phân loại ưu tiên và đặt hạn xử lý 48 giờ.");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void updateComplaint(){
        int vr=tblComplaintTable.getSelectedRow();
        if(vr<0){info("Hãy chọn khiếu nại.");return;}
        int r=tblComplaintTable.convertRowIndexToModel(vr);
        String id=String.valueOf(complaintModel.getValueAt(r,0));
        String oldCode=Formats.code(complaintModel.getValueAt(r,4));
        String[] codes=complaintNextStatuses(oldCode);
        if(codes.length==0){info("Khiếu nại này đã đóng, không thể cập nhật tiếp.");return;}
        String[] labels=Formats.labels(codes);
        String st=(String)JOptionPane.showInputDialog(DialogUtils.owner(this),"Trạng thái mới:","Cập nhật khiếu nại",JOptionPane.PLAIN_MESSAGE,null,labels,labels[0]);
        if(st==null)return;
        String note=JOptionPane.showInputDialog(DialogUtils.owner(this),"Ghi chú/kết quả xử lý:","");
        if(note==null)return;
        try{service.updateComplaintStatus(id,Formats.code(st),note);refreshData();}catch(Exception ex){showError(ex);}
    }
    private String[] complaintNextStatuses(String old){
        if("NEW".equals(old)) return new String[]{"IN_PROGRESS"};
        if("IN_PROGRESS".equals(old)) return new String[]{"WAITING_CUSTOMER","RESOLVED"};
        if("WAITING_CUSTOMER".equals(old)) return new String[]{"IN_PROGRESS","RESOLVED"};
        if("RESOLVED".equals(old)) return new String[]{"CLOSED"};
        return new String[0];
    }
    private String formatDateTime(Object value){
        if(value==null)return "";
        if(value instanceof Timestamp){
            return ((Timestamp)value).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        return String.valueOf(value);
    }
    private static class WarrantyOption {
        final String customerId;
        final long detailId;
        final String label;
        final LocalDate warrantyEnd;
        final int warrantyMonths;
        WarrantyOption(Map<String,Object> r){
            customerId=String.valueOf(r.get("customer_id"));
            detailId=((Number)r.get("invoice_detail_id")).longValue();
            warrantyMonths=((Number)r.get("warranty_months")).intValue();
            Object end=r.get("warranty_end_date");
            warrantyEnd=end instanceof java.sql.Date?((java.sql.Date)end).toLocalDate():LocalDate.parse(String.valueOf(end));
            label=r.get("full_name")+" - "+r.get("product_name")+" - mua "+Formats.date(r.get("created_at"))+" - còn "+r.get("remaining_qty");
        }
        @Override public String toString(){return label;}
    }
}

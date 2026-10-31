package ui.hoantien;
import ui.dungchung.DialogUtils;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.OperationService;
import util.Formats;
import ui.dungchung.*;
public class RefundApprovalPanel extends BaseScreenPanel {
    private static final long serialVersionUID=1L;
    private final JComboBox<String> cmbFilter=UIFactory.combo("Chờ xử lý","Đã duyệt","Đã từ chối","Tất cả");
    private final DefaultTableModel model=UIFactory.readOnlyModel(new Object[]{"Mã phiếu","Hóa đơn","Người lập","Khách hàng","Giá trị hoàn","Lý do","Quyết định","Ngày tạo"});
    private final JTable tblTable=UIFactory.table(model);
    private final OperationService service=new OperationService();
    public RefundApprovalPanel(){
        super("Duyệt hoàn tiền","Duyệt hoặc từ chối phiếu đổi trả vượt hạn mức 5.000.000 đồng.");
        JButton btnApprove=UIFactory.primaryButton("Duyệt hoàn tiền"),btnReject=UIFactory.secondaryButton("Từ chối");
        pnlActions.add(btnApprove);pnlActions.add(btnReject);
        pnlBody.add(UIFactory.toolbar(new JLabel("Quyết định"),cmbFilter),BorderLayout.NORTH);pnlBody.add(UIFactory.scroll(tblTable),BorderLayout.CENTER);
        cmbFilter.addActionListener(e->refreshData());btnApprove.addActionListener(e->decision(true));btnReject.addActionListener(e->decision(false));
        refreshData();
    }
    public void refreshData(){
        try{
            String f=String.valueOf(cmbFilter.getSelectedItem());String decisionCode="Tất cả".equals(f)?f:Formats.code(f);
            String sql="SELECT rr.return_id,rr.invoice_id,e.full_name employee,ISNULL(c.full_name,N'Khách vãng lai') customer,rr.refund_amount,rr.reason,"
                    +"ra.decision,ra.created_at FROM refund_approvals ra JOIN return_requests rr ON rr.return_id=ra.return_id "
                    +"JOIN employees e ON e.employee_id=rr.created_by JOIN invoices i ON i.invoice_id=rr.invoice_id LEFT JOIN customers c ON c.customer_id=i.customer_id "
                    +("Tất cả".equals(f)?"":"WHERE ra.decision=? ") +"ORDER BY rr.refund_amount DESC";
            java.util.List<Map<String,Object>> rows="Tất cả".equals(f)?service.query(sql):service.query(sql,decisionCode);
            model.setRowCount(0);for(Map<String,Object> r:rows)model.addRow(new Object[]{r.get("return_id"),r.get("invoice_id"),r.get("employee"),r.get("customer"),
                    Formats.money(r.get("refund_amount")),r.get("reason"),Formats.vi(r.get("decision")),Formats.date(r.get("created_at"))});
        }catch(Exception ex){showError(ex);}
    }
    private void decision(boolean ok){
        int vr=tblTable.getSelectedRow();if(vr<0){info("Hãy chọn phiếu.");return;}
        int r=tblTable.convertRowIndexToModel(vr);String id=String.valueOf(model.getValueAt(r,0));String current=String.valueOf(model.getValueAt(r,6));
        if(!"Chờ xử lý".equals(current)){info("Phiếu đã được xử lý.");return;}
        String reason=JOptionPane.showInputDialog(DialogUtils.owner(this),ok?"Ghi chú duyệt (có thể để trống):":"Nhập lý do từ chối:");
        if(!ok && (reason==null||reason.trim().isEmpty()))return;
        try{service.approveRefund(id,ok,reason);refreshData();info(ok?"Đã duyệt và hoàn tiền.":"Đã từ chối phiếu.");}catch(Exception ex){showError(ex);}
    }
}

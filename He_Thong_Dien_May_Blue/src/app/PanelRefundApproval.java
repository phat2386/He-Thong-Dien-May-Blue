package app;

import bus.BusinessService;
import bus.Formats;
import dao.Db;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


public class PanelRefundApproval extends PanelBaseScreen {
    private static final long serialVersionUID=1L;
    private final JComboBox<String> filter=UIFactory.combo("Chờ xử lý","Đã duyệt","Đã từ chối","Tất cả");
    private final DefaultTableModel model=new DefaultTableModel(
            new Object[]{"Mã phiếu","Hóa đơn","Người lập","Khách hàng","Giá trị hoàn","Lý do","Quyết định","Ngày tạo"},0){
        private static final long serialVersionUID=1L;public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable table=UIFactory.table(model);
    private final BusinessService service=new BusinessService();

    public PanelRefundApproval(){
        super("Duyệt hoàn tiền","Duyệt hoặc từ chối phiếu đổi trả vượt hạn mức 5.000.000 đồng.");
        JButton approve=UIFactory.primaryButton("Duyệt hoàn tiền"),reject=UIFactory.secondaryButton("Từ chối");
        actions.add(approve);actions.add(reject);
        body.add(UIFactory.toolbar(new JLabel("Quyết định"),filter),BorderLayout.NORTH);body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        filter.addActionListener(e->refreshData());approve.addActionListener(e->decision(true));reject.addActionListener(e->decision(false));
        refreshData();
    }

    public void refreshData(){
        try{
            String f=String.valueOf(filter.getSelectedItem());String decisionCode="Tất cả".equals(f)?f:UiText.code(f);
            String sql="SELECT rr.return_id,rr.invoice_id,e.full_name employee,ISNULL(c.full_name,N'Khách vãng lai') customer,rr.refund_amount,rr.reason,"
                    +"ra.decision,ra.created_at FROM refund_approvals ra JOIN return_requests rr ON rr.return_id=ra.return_id "
                    +"JOIN employees e ON e.employee_id=rr.created_by JOIN invoices i ON i.invoice_id=rr.invoice_id LEFT JOIN customers c ON c.customer_id=i.customer_id "
                    +("Tất cả".equals(f)?"":"WHERE ra.decision=? ")
                    +"ORDER BY rr.refund_amount DESC";
            java.util.List<Map<String,Object>> rows="Tất cả".equals(f)?Db.query(sql):Db.query(sql,decisionCode);
            model.setRowCount(0);for(Map<String,Object> r:rows)model.addRow(new Object[]{r.get("return_id"),r.get("invoice_id"),r.get("employee"),r.get("customer"),
                    Formats.money(r.get("refund_amount")),r.get("reason"),UiText.vi(r.get("decision")),Formats.date(r.get("created_at"))});
        }catch(Exception ex){showError(ex);}
    }

    private void decision(boolean ok){
        int vr=table.getSelectedRow();if(vr<0){info("Hãy chọn phiếu.");return;}
        int r=table.convertRowIndexToModel(vr);String id=String.valueOf(model.getValueAt(r,0));String current=String.valueOf(model.getValueAt(r,6));
        if(!"Chờ xử lý".equals(current)){info("Phiếu đã được xử lý.");return;}
        String reason=JOptionPane.showInputDialog(DialogUtils.owner(this),ok?"Ghi chú duyệt (có thể để trống):":"Nhập lý do từ chối:");
        if(!ok && (reason==null||reason.trim().isEmpty()))return;
        try{service.approveRefund(id,ok,reason);refreshData();info(ok?"Đã duyệt và hoàn tiền.":"Đã từ chối phiếu.");}catch(Exception ex){showError(ex);}
    }
}

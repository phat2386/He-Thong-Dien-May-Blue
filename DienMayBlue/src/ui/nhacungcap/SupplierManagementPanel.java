package ui.nhacungcap;
import ui.dungchung.DialogUtils;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.ProductService;
import util.Formats;
import util.SessionManager;
import ui.dungchung.*;
import ui.phieunhap.StockReceiptListPanel;
public class SupplierManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID=1L;
    private final JTextField txtSearch=new HintTextField("Nhập mã NCC, tên NCC hoặc số điện thoại...");
    private final java.util.List<Integer> supplierIds=new ArrayList<Integer>();
    private final DefaultTableModel model=UIFactory.readOnlyModel(new Object[]{"Mã NCC","Tên NCC","Điện thoại","Công nợ còn lại","Trạng thái"});
    private final JTable tblSupplier=UIFactory.table(model);
    private final ProductService service=new ProductService();
    public SupplierManagementPanel(){
        super("Nhà cung cấp","Quản lý nhà cung cấp, công nợ phát sinh từ phiếu nhập và thanh toán công nợ.");
        JButton btnAdd=UIFactory.primaryButton("+ Thêm NCC");
        JButton btnEdit=UIFactory.secondaryButton("Chỉnh sửa");
        JButton btnStatus=UIFactory.secondaryButton("Đổi trạng thái");
        JButton btnPay=UIFactory.secondaryButton("Thanh toán nợ");
        JButton btnHistory=UIFactory.secondaryButton("Lịch sử thanh toán");
        JButton btnReceipts=UIFactory.secondaryButton("Phiếu nhập kho");
        actions.add(btnAdd);actions.add(btnEdit);actions.add(btnPay);actions.add(btnHistory);actions.add(btnReceipts);actions.add(btnStatus);
        txtSearch.setPreferredSize(new Dimension(380,38));
        body.add(UIFactory.toolbar(txtSearch),BorderLayout.NORTH);
        body.add(UIFactory.scroll(tblSupplier),BorderLayout.CENTER);
        UIFactory.onTextChange(txtSearch, this::refreshData);
        btnAdd.addActionListener(e->form(null));
        btnEdit.addActionListener(e->edit());
        btnStatus.addActionListener(e->toggleStatus());
        btnPay.addActionListener(e->payDebt());
        btnHistory.addActionListener(e->showPaymentHistory());
        btnReceipts.addActionListener(e->StockReceiptListPanel.show(this));
        refreshData();
    }
    @Override public void refreshData(){
        try{
            String kw="%"+txtSearch.getText().trim()+"%";
            model.setRowCount(0);
            supplierIds.clear();
            for(Map<String,Object> r:service.query(
                    "SELECT s.supplier_id,s.supplier_code,s.supplier_name,s.phone,s.status,"
                  + "ISNULL((SELECT SUM(remaining_amount) FROM supplier_debts d WHERE d.supplier_id=s.supplier_id),0) debt "
                  + "FROM suppliers s WHERE s.supplier_code LIKE ? OR s.supplier_name LIKE ? OR ISNULL(s.phone,'') LIKE ? ORDER BY s.supplier_code",
                    kw,kw,kw)) {
                String status="ACTIVE".equals(String.valueOf(r.get("status")))?"Đang hợp tác":"Ngừng hợp tác";
                supplierIds.add(((Number)r.get("supplier_id")).intValue());
                model.addRow(new Object[]{r.get("supplier_code"),r.get("supplier_name"),r.get("phone"),Formats.money(r.get("debt")),status});
            }
            if(model.getRowCount()>0 && !txtSearch.getText().trim().isEmpty()){
                tblSupplier.setRowSelectionInterval(0,0);
                tblSupplier.scrollRectToVisible(tblSupplier.getCellRect(0,0,true));
            }
        }catch(Exception ex){showError(ex);}
    }
    private Integer selected(){
        int vr=tblSupplier.getSelectedRow();
        if(vr<0)return null;
        int mr=tblSupplier.convertRowIndexToModel(vr);
        return mr>=0 && mr<supplierIds.size()?supplierIds.get(mr):null;
    }
    private void edit(){
        Integer id=selected();
        if(id==null){info("Hãy chọn nhà cung cấp.");return;}
        form(id);
    }
    private void form(Integer id){
        try{
            Map<String,Object> old=id==null?null:service.one("SELECT * FROM suppliers WHERE supplier_id=?",id);
            JTextField txtCode=UIFactory.field(180),txtName=UIFactory.field(260),txtPhone=UIFactory.field(180),txtAddr=UIFactory.field(260);
            if(old!=null){
                txtCode.setText(Formats.safe(old.get("supplier_code")));txtCode.setEditable(false);
                txtName.setText(Formats.safe(old.get("supplier_name")));txtPhone.setText(Formats.safe(old.get("phone")));
                txtAddr.setText(Formats.safe(old.get("address")));
            }else{
                txtCode.setText(nextSupplierCode());
            }
            JPanel pnlForm=new JPanel(new GridBagLayout());
            pnlForm.setPreferredSize(new Dimension(460,170));
            GridBagConstraints gc=new GridBagConstraints();
            gc.insets=new Insets(5,6,5,6);gc.anchor=GridBagConstraints.WEST;gc.fill=GridBagConstraints.HORIZONTAL;
            gc.gridx=0;gc.gridy=0;gc.weightx=0;pnlForm.add(new JLabel("Mã NCC"),gc);
            gc.gridx=1;gc.weightx=1;pnlForm.add(txtCode,gc);
            gc.gridx=0;gc.gridy++;gc.weightx=0;pnlForm.add(new JLabel("Tên NCC"),gc);
            gc.gridx=1;gc.weightx=1;pnlForm.add(txtName,gc);
            gc.gridx=0;gc.gridy++;gc.weightx=0;pnlForm.add(new JLabel("Điện thoại"),gc);
            gc.gridx=1;gc.weightx=1;pnlForm.add(txtPhone,gc);
            gc.gridx=0;gc.gridy++;gc.weightx=0;pnlForm.add(new JLabel("Địa chỉ"),gc);
            gc.gridx=1;gc.weightx=1;pnlForm.add(txtAddr,gc);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),pnlForm,id==null?"Thêm nhà cung cấp":"Sửa nhà cung cấp",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            String c=txtCode.getText().trim(),n=txtName.getText().trim(),ph=txtPhone.getText().trim(),a=txtAddr.getText().trim();
            String t=old==null?"AUTO-"+c:Formats.safe(old.get("tax_code"));
            if(c.isEmpty() || c.length()>20) throw new IllegalArgumentException("Mã nhà cung cấp là bắt buộc và tối đa 20 ký tự.");
            if(n.isEmpty()) throw new IllegalArgumentException("Tên nhà cung cấp là bắt buộc.");
            if(!ph.matches("\\d{9,11}")) throw new IllegalArgumentException("Số điện thoại phải gồm 9-11 chữ số.");
            if(a.isEmpty()) throw new IllegalArgumentException("Địa chỉ là bắt buộc.");
            service.transaction(con->{
                Object dupCode=id==null?service.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE supplier_code=?",c):service.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE supplier_code=? AND supplier_id<>?",c,id);
                Object dupPhone=id==null?service.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE phone=?",ph):service.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE phone=? AND supplier_id<>?",ph,id);
                if(((Number)dupCode).intValue()>0) throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại.");
                if(((Number)dupPhone).intValue()>0) throw new IllegalArgumentException("Số điện thoại đã thuộc nhà cung cấp khác.");
                Object auditId=id;
                if(id==null){
                    long newId=service.insertAndKey(con,"INSERT INTO suppliers(supplier_code,supplier_name,phone,address,tax_code,status) VALUES(?,?,?,?,?,'ACTIVE')",c,n,ph,a,t);
                    auditId=newId;
                }else{
                    service.update(con,"UPDATE suppliers SET supplier_name=?,phone=?,address=?,tax_code=?,updated_at=SYSDATETIME() WHERE supplier_id=?",n,ph,a,t,id);
                }
                if(SessionManager.getCurrentAccount()!=null)
                    service.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                            SessionManager.getCurrentAccount().getAccountId(),id==null?"SUPPLIER_CREATED":"SUPPLIER_UPDATED","suppliers",String.valueOf(auditId),(id==null?"Tạo":"Cập nhật")+" nhà cung cấp "+n);
                return null;
            });
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private String nextSupplierCode(){
        try{
            int max=0;
            for(Map<String,Object> r:service.query("SELECT supplier_code FROM suppliers")){
                String code=Formats.safe(r.get("supplier_code")).trim().toUpperCase();
                if(!code.matches("NCC\\d+")) continue;
                try{
                    max=Math.max(max,Integer.parseInt(code.substring(3)));
                }catch(NumberFormatException ignored){}
            }
            return String.format("NCC%03d",max+1);
        }catch(Exception ex){
            return "NCC001";
        }
    }
    private void toggleStatus(){
        Integer sid=selected();
        if(sid==null){info("Hãy chọn nhà cung cấp.");return;}
        try{
            Map<String,Object> s=service.one("SELECT supplier_name,status FROM suppliers WHERE supplier_id=?",sid);
            boolean active="ACTIVE".equals(String.valueOf(s.get("status")));
            if(active){
                BigDecimal debt=(BigDecimal)service.scalar("SELECT ISNULL(SUM(remaining_amount),0) FROM supplier_debts WHERE supplier_id=?",sid);
                String msg="Ngừng hợp tác với "+s.get("supplier_name")+"?";
                if(debt.compareTo(BigDecimal.ZERO)>0) msg+="\nNhà cung cấp vẫn còn công nợ "+Formats.money(debt)+". Công nợ vẫn có thể thanh toán sau khi ngừng hợp tác.";
                if(!confirm(msg)) return;
            }else if(!confirm("Khôi phục hợp tác với "+s.get("supplier_name")+"?")) return;
            service.setSupplierActive(sid,!active);
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void payDebt(){
        Integer sid=selected();
        if(sid==null){info("Hãy chọn nhà cung cấp.");return;}
        try{
            Map<String,Object> s=service.one(
                    "SELECT supplier_code,supplier_name,"
                  + "ISNULL((SELECT SUM(remaining_amount) FROM supplier_debts d WHERE d.supplier_id=s.supplier_id),0) debt "
                  + "FROM suppliers s WHERE supplier_id=?",sid);
            if(s==null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp.");
            BigDecimal remaining=(BigDecimal)s.get("debt");
            if(remaining==null || remaining.compareTo(BigDecimal.ZERO)<=0){
                info("Nhà cung cấp này hiện không còn công nợ.");
                return;
            }
            JTextField txtAmount=UIFactory.field(220);
            txtAmount.setText(remaining.stripTrailingZeros().toPlainString());
            JButton btnPayAll=UIFactory.secondaryButton("Thanh toán toàn bộ");
            btnPayAll.addActionListener(e->txtAmount.setText(remaining.stripTrailingZeros().toPlainString()));
            JPanel pnlPay=new JPanel(new GridBagLayout());
            GridBagConstraints c=new GridBagConstraints();
            c.insets=new Insets(6,8,6,8);c.anchor=GridBagConstraints.WEST;c.fill=GridBagConstraints.HORIZONTAL;
            c.gridx=0;c.gridy=0;c.weightx=0;pnlPay.add(new JLabel("Nhà cung cấp"),c);
            c.gridx=1;c.weightx=1;pnlPay.add(new JLabel(String.valueOf(s.get("supplier_name"))),c);
            c.gridx=0;c.gridy++;c.weightx=0;pnlPay.add(new JLabel("Mã NCC"),c);
            c.gridx=1;c.weightx=1;pnlPay.add(new JLabel(String.valueOf(s.get("supplier_code"))),c);
            c.gridx=0;c.gridy++;c.weightx=0;pnlPay.add(new JLabel("Công nợ còn lại"),c);
            c.gridx=1;c.weightx=1;pnlPay.add(new JLabel(Formats.money(remaining)),c);
            c.gridx=0;c.gridy++;c.weightx=0;pnlPay.add(new JLabel("Số tiền thanh toán"),c);
            c.gridx=1;c.weightx=1;pnlPay.add(txtAmount,c);
            c.gridx=1;c.gridy++;c.weightx=0;c.fill=GridBagConstraints.NONE;pnlPay.add(btnPayAll,c);
            Object[] options={"Xác nhận","Hủy"};
            int option=JOptionPane.showOptionDialog(DialogUtils.owner(this),pnlPay,
                    "Thanh toán công nợ",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,options[0]);
            if(option!=0)return;
            String raw=txtAmount.getText()==null?"":txtAmount.getText().trim().replace(".","").replace(",","").replace(" ","");
            if(!raw.matches("\\d+")) throw new IllegalArgumentException("Số tiền thanh toán phải là số nguyên dương.");
            BigDecimal paid=new BigDecimal(raw);
            if(paid.compareTo(BigDecimal.ZERO)<=0) throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
            if(paid.compareTo(remaining)>0) throw new IllegalArgumentException("Số tiền thanh toán vượt quá công nợ còn lại.");
            service.paySupplierDebts(sid,paid);
            refreshData();
            BigDecimal after=remaining.subtract(paid);
            info(after.compareTo(BigDecimal.ZERO)==0
                    ?"Đã thanh toán toàn bộ công nợ cho "+s.get("supplier_name")+"."
                    :"Đã thanh toán "+Formats.money(paid)+". Công nợ còn lại: "+Formats.money(after)+".");
        }catch(Exception ex){showError(ex);}
    }
    private void showPaymentHistory(){
        Integer sid=selected();
        if(sid==null){info("Hãy chọn nhà cung cấp.");return;}
        try{
            Map<String,Object> supplier=service.one( "SELECT supplier_code,supplier_name FROM suppliers WHERE supplier_id=?",sid);
            if(supplier==null) throw new IllegalArgumentException("Không tìm thấy nhà cung cấp.");
            DefaultTableModel m=UIFactory.readOnlyModel(new Object[]{"Phiếu nhập","Số tiền thanh toán","Ngày thanh toán","Người thực hiện"});
            BigDecimal totalPaid=BigDecimal.ZERO;
            for(Map<String,Object> r:service.query(
                    "SELECT d.reference_code,sp.amount,sp.paid_at,e.full_name "
                  + "FROM supplier_payments sp LEFT JOIN supplier_debts d ON d.debt_id=sp.debt_id "
                  + "JOIN employees e ON e.employee_id=sp.paid_by " + "WHERE sp.supplier_id=? ORDER BY sp.paid_at DESC",sid)){
                BigDecimal paid=(BigDecimal)r.get("amount");
                if(paid!=null) totalPaid=totalPaid.add(paid);
                m.addRow(new Object[]{
                        Formats.safe(r.get("reference_code")), Formats.money(paid), Formats.date(r.get("paid_at")), r.get("full_name")
                });
            }
            JLabel lblSupplier=new JLabel("Nhà cung cấp: "+supplier.get("supplier_code")+" - "+supplier.get("supplier_name"));
            JLabel lblTotal=new JLabel("Tổng đã thanh toán: "+Formats.money(totalPaid));
            lblSupplier.setFont(lblSupplier.getFont().deriveFont(Font.BOLD));
            JPanel pnlHeader=new JPanel(new BorderLayout(12,0));
            pnlHeader.add(lblSupplier,BorderLayout.WEST);
            pnlHeader.add(lblTotal,BorderLayout.EAST);
            JTable tblHistory=UIFactory.table(m);
            JScrollPane scrHistory=UIFactory.scroll(tblHistory);
            scrHistory.setPreferredSize(new Dimension(760,300));
            JPanel pnlHistory=new JPanel(new BorderLayout(0,10));
            pnlHistory.add(pnlHeader,BorderLayout.NORTH);
            pnlHistory.add(scrHistory,BorderLayout.CENTER);
            JOptionPane.showMessageDialog(DialogUtils.owner(this),pnlHistory, "Lịch sử thanh toán",JOptionPane.PLAIN_MESSAGE);
        }catch(Exception ex){showError(ex);}
    }
    private static class HintTextField extends JTextField {
        private static final long serialVersionUID=1L;
        private final String hint;
        HintTextField(String hint){this.hint=hint;}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if(getText().isEmpty() && !hasFocus()){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setFont(getFont());
                g2.setColor(UIConstants.MUTED);
                Insets insets=getInsets();
                FontMetrics fm=g2.getFontMetrics();
                int y=(getHeight()-fm.getHeight())/2+fm.getAscent();
                g2.drawString(hint,insets.left,y);
                g2.dispose();
            }
        }
    }
}

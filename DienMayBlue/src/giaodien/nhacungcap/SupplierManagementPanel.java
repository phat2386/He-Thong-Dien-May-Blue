package giaodien.nhacungcap;

import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import dichvu.BusinessService;
import tienich.Db;
import tienich.Formats;
import tienich.SessionManager;
import tienich.UiText;
import giaodien.dungchung.*;

public class SupplierManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID=1L;
    private final JTextField search=UIFactory.field(280);
    private final DefaultTableModel model=new DefaultTableModel(
            new Object[]{"Mã","Mã NCC","Tên NCC","Điện thoại","Mã số thuế","Công nợ còn lại","Trạng thái"},0){
        private static final long serialVersionUID=1L;
        @Override public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable table=UIFactory.table(model);
    private final BusinessService service=new BusinessService();

    public SupplierManagementPanel(){
        super("Nhà cung cấp","UC014 • Quản lý nhà cung cấp, công nợ phát sinh từ phiếu nhập và thanh toán công nợ.");
        JButton add=UIFactory.primaryButton("+ Thêm NCC");
        JButton edit=UIFactory.secondaryButton("Chỉnh sửa");
        JButton status=UIFactory.secondaryButton("Đổi trạng thái");
        JButton debt=UIFactory.secondaryButton("Xem công nợ");
        JButton pay=UIFactory.secondaryButton("Thanh toán nợ");
        JButton history=UIFactory.secondaryButton("Lịch sử thanh toán");
        actions.add(add);actions.add(edit);actions.add(status);actions.add(debt);actions.add(pay);actions.add(history);
        body.add(UIFactory.toolbar(new JLabel("Tìm"),search),BorderLayout.NORTH);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);

        search.addActionListener(e->refreshData());
        add.addActionListener(e->form(null));
        edit.addActionListener(e->edit());
        status.addActionListener(e->toggleStatus());
        debt.addActionListener(e->showDebt(false));
        pay.addActionListener(e->showDebt(true));
        history.addActionListener(e->showPaymentHistory());
        refreshData();
    }

    @Override public void refreshData(){
        try{
            String kw="%"+search.getText().trim()+"%";
            model.setRowCount(0);
            for(Map<String,Object> r:Db.query(
                    "SELECT s.supplier_id,s.supplier_code,s.supplier_name,s.phone,s.tax_code,s.status,"
                  + "ISNULL((SELECT SUM(remaining_amount) FROM supplier_debts d WHERE d.supplier_id=s.supplier_id),0) debt "
                  + "FROM suppliers s WHERE s.supplier_code LIKE ? OR s.supplier_name LIKE ? OR s.phone LIKE ? OR s.tax_code LIKE ? ORDER BY s.supplier_name",
                    kw,kw,kw,kw)) {
                String status="ACTIVE".equals(String.valueOf(r.get("status")))?"Đang hợp tác":"Ngừng hợp tác";
                model.addRow(new Object[]{r.get("supplier_id"),r.get("supplier_code"),r.get("supplier_name"),r.get("phone"),r.get("tax_code"),Formats.money(r.get("debt")),status});
            }
        }catch(Exception ex){showError(ex);}
    }

    private Integer selected(){
        int vr=table.getSelectedRow();
        return vr<0?null:((Number)model.getValueAt(table.convertRowIndexToModel(vr),0)).intValue();
    }

    private void edit(){
        Integer id=selected();
        if(id==null){info("Hãy chọn nhà cung cấp.");return;}
        form(id);
    }

    private void form(Integer id){
        try{
            Map<String,Object> old=id==null?null:Db.one("SELECT * FROM suppliers WHERE supplier_id=?",id);
            JTextField code=UIFactory.field(150),name=UIFactory.field(280),phone=UIFactory.field(180),tax=UIFactory.field(180),addr=UIFactory.field(320);
            if(old!=null){
                code.setText(Formats.safe(old.get("supplier_code")));code.setEditable(false);
                name.setText(Formats.safe(old.get("supplier_name")));phone.setText(Formats.safe(old.get("phone")));
                tax.setText(Formats.safe(old.get("tax_code")));addr.setText(Formats.safe(old.get("address")));
            }
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.add(new JLabel("Mã NCC"));p.add(code);p.add(new JLabel("Tên NCC"));p.add(name);
            p.add(new JLabel("Điện thoại"));p.add(phone);p.add(new JLabel("Mã số thuế"));p.add(tax);
            p.add(new JLabel("Địa chỉ"));p.add(addr);
            if(JOptionPane.showConfirmDialog(giaodien.dungchung.DialogUtils.owner(this),p,id==null?"Thêm nhà cung cấp":"Sửa nhà cung cấp",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;

            String c=code.getText().trim(),n=name.getText().trim(),ph=phone.getText().trim(),t=tax.getText().trim(),a=addr.getText().trim();
            if(c.isEmpty() || c.length()>20) throw new IllegalArgumentException("Mã nhà cung cấp là bắt buộc và tối đa 20 ký tự.");
            if(n.isEmpty()) throw new IllegalArgumentException("Tên nhà cung cấp là bắt buộc.");
            if(!ph.matches("\\d{9,11}")) throw new IllegalArgumentException("Số điện thoại phải gồm 9-11 chữ số.");
            if(t.isEmpty() || t.length()>30) throw new IllegalArgumentException("Mã số thuế là bắt buộc và tối đa 30 ký tự.");
            if(a.isEmpty()) throw new IllegalArgumentException("Địa chỉ là bắt buộc.");

            Db.transaction(con->{
                Object dupCode=id==null?Db.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE supplier_code=?",c):Db.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE supplier_code=? AND supplier_id<>?",c,id);
                Object dupPhone=id==null?Db.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE phone=?",ph):Db.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE phone=? AND supplier_id<>?",ph,id);
                Object dupTax=id==null?Db.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE tax_code=?",t):Db.scalar(con,"SELECT COUNT(*) FROM suppliers WHERE tax_code=? AND supplier_id<>?",t,id);
                if(((Number)dupCode).intValue()>0) throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại.");
                if(((Number)dupPhone).intValue()>0) throw new IllegalArgumentException("Số điện thoại đã thuộc nhà cung cấp khác.");
                if(((Number)dupTax).intValue()>0) throw new IllegalArgumentException("Mã số thuế đã thuộc nhà cung cấp khác.");

                Object auditId=id;
                if(id==null){
                    long newId=Db.insertAndKey(con,"INSERT INTO suppliers(supplier_code,supplier_name,phone,address,tax_code,status) VALUES(?,?,?,?,?,'ACTIVE')",c,n,ph,a,t);
                    auditId=newId;
                }else{
                    Db.update(con,"UPDATE suppliers SET supplier_name=?,phone=?,address=?,tax_code=?,updated_at=SYSDATETIME() WHERE supplier_id=?",n,ph,a,t,id);
                }
                if(SessionManager.getCurrentAccount()!=null)
                    Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                            SessionManager.getCurrentAccount().getAccountId(),id==null?"SUPPLIER_CREATED":"SUPPLIER_UPDATED","suppliers",String.valueOf(auditId),(id==null?"Tạo":"Cập nhật")+" nhà cung cấp "+n);
                return null;
            });
            refreshData();
        }catch(Exception ex){showError(ex);}
    }

    private void toggleStatus(){
        Integer sid=selected();
        if(sid==null){info("Hãy chọn nhà cung cấp.");return;}
        try{
            Map<String,Object> s=Db.one("SELECT supplier_name,status FROM suppliers WHERE supplier_id=?",sid);
            boolean active="ACTIVE".equals(String.valueOf(s.get("status")));
            if(active){
                BigDecimal debt=(BigDecimal)Db.scalar("SELECT ISNULL(SUM(remaining_amount),0) FROM supplier_debts WHERE supplier_id=?",sid);
                String msg="Ngừng hợp tác với "+s.get("supplier_name")+"?";
                if(debt.compareTo(BigDecimal.ZERO)>0) msg+="\nNhà cung cấp vẫn còn công nợ "+Formats.money(debt)+". Công nợ vẫn có thể thanh toán sau khi ngừng hợp tác.";
                if(!confirm(msg)) return;
            }else if(!confirm("Khôi phục hợp tác với "+s.get("supplier_name")+"?")) return;
            service.setSupplierActive(sid,!active);
            refreshData();
        }catch(Exception ex){showError(ex);}
    }

    private void showDebt(boolean payment){
        Integer sid=selected();
        if(sid==null){info("Hãy chọn nhà cung cấp.");return;}
        try{
            DefaultTableModel m=new DefaultTableModel(new Object[]{"Mã công nợ","Phiếu nhập","Nội dung","Số ban đầu","Còn lại","Hạn trả","Trạng thái"},0){
                private static final long serialVersionUID=1L;
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            for(Map<String,Object> r:Db.query(
                    "SELECT debt_id,reference_code,description,original_amount,remaining_amount,due_date,"
                  + "CASE WHEN remaining_amount=0 THEN 'PAID' WHEN due_date IS NOT NULL AND due_date<CAST(GETDATE() AS DATE) THEN 'OVERDUE' "
                  + "WHEN remaining_amount<original_amount THEN 'PARTIAL' ELSE 'UNPAID' END display_status "
                  + "FROM supplier_debts WHERE supplier_id=? ORDER BY CASE WHEN remaining_amount>0 THEN 0 ELSE 1 END,due_date,created_at",sid))
                m.addRow(new Object[]{r.get("debt_id"),r.get("reference_code"),r.get("description"),Formats.money(r.get("original_amount")),Formats.money(r.get("remaining_amount")),Formats.date(r.get("due_date")),UiText.vi(r.get("display_status"))});
            JTable t=UIFactory.table(m);
            JScrollPane sp=UIFactory.scroll(t);sp.setPreferredSize(new Dimension(900,340));
            int option=JOptionPane.showConfirmDialog(giaodien.dungchung.DialogUtils.owner(this),sp,payment?"Chọn khoản nợ để thanh toán":"Công nợ nhà cung cấp",payment?JOptionPane.OK_CANCEL_OPTION:JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(!payment || option!=JOptionPane.OK_OPTION)return;
            int vr=t.getSelectedRow();if(vr<0){info("Hãy chọn khoản nợ.");return;}
            int row=t.convertRowIndexToModel(vr);
            long debtId=((Number)m.getValueAt(row,0)).longValue();
            Map<String,Object> debt=Db.one("SELECT remaining_amount FROM supplier_debts WHERE debt_id=?",debtId);
            BigDecimal remaining=(BigDecimal)debt.get("remaining_amount");
            if(remaining.compareTo(BigDecimal.ZERO)<=0){info("Khoản công nợ này đã được thanh toán.");return;}
            String amount=JOptionPane.showInputDialog(giaodien.dungchung.DialogUtils.owner(this),"Số tiền thanh toán (còn lại "+Formats.money(remaining)+"):",remaining.toPlainString());
            if(amount==null)return;
            String note=JOptionPane.showInputDialog(giaodien.dungchung.DialogUtils.owner(this),"Ghi chú:","");
            service.paySupplierDebt(debtId,new BigDecimal(amount.trim()),note);
            refreshData();info("Đã ghi nhận thanh toán công nợ.");
        }catch(Exception ex){showError(ex);}
    }

    private void showPaymentHistory(){
        Integer sid=selected();
        if(sid==null){info("Hãy chọn nhà cung cấp.");return;}
        try{
            DefaultTableModel m=new DefaultTableModel(new Object[]{"Mã TT","Mã nợ","Phiếu nhập","Số tiền","Thời gian","Người ghi nhận","Ghi chú"},0){
                private static final long serialVersionUID=1L;
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            for(Map<String,Object> r:Db.query(
                    "SELECT sp.supplier_payment_id,sp.debt_id,d.reference_code,sp.amount,sp.paid_at,e.full_name,sp.note "
                  + "FROM supplier_payments sp LEFT JOIN supplier_debts d ON d.debt_id=sp.debt_id "
                  + "JOIN employees e ON e.employee_id=sp.paid_by WHERE sp.supplier_id=? ORDER BY sp.paid_at DESC",sid))
                m.addRow(new Object[]{r.get("supplier_payment_id"),r.get("debt_id"),r.get("reference_code"),Formats.money(r.get("amount")),r.get("paid_at"),r.get("full_name"),r.get("note")});
            JTable t=UIFactory.table(m);JScrollPane sp=UIFactory.scroll(t);sp.setPreferredSize(new Dimension(900,340));
            JOptionPane.showMessageDialog(giaodien.dungchung.DialogUtils.owner(this),sp,"Lịch sử thanh toán công nợ",JOptionPane.PLAIN_MESSAGE);
        }catch(Exception ex){showError(ex);}
    }
}

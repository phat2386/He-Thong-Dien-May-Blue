package app;

import bus.Formats;
import dao.Db;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


/**
 * UC022 - Quản lý khách hàng.
 *
 * Nhân viên bán hàng chỉ quản lý hồ sơ khách hàng. Hạng, điểm và thẻ được
 * hiển thị để phục vụ giao dịch nhưng không được sửa tại màn hình này.
 * UC023/UC024 được xử lý trong nhóm Quản lý khuyến mãi của Nhân viên quản lý.
 */
public class PanelCustomerManagement extends PanelBaseScreen {
    private static final long serialVersionUID=1L;
    private final JTextField txt=UIFactory.field(320);
    private final JComboBox<String> tier=UIFactory.combo("Tất cả hạng","Tiêu chuẩn","Bạc","Vàng","Bạch kim");
    private final DefaultTableModel model=new DefaultTableModel(
            new Object[]{"Mã KH","Họ tên","Điện thoại","Thư điện tử","Hạng","Điểm","Tổng chi tiêu ròng","Thẻ đang dùng","Trạng thái"},0){
        private static final long serialVersionUID=1L;
        public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable table=UIFactory.table(model);

    public PanelCustomerManagement(){
        super("Quản lý khách hàng","Tra cứu, thêm mới và cập nhật hồ sơ khách hàng. Điểm, hạng và thẻ chỉ để tra cứu tại quầy.");
        JButton add=UIFactory.primaryButton("+ Thêm khách hàng");
        JButton edit=UIFactory.secondaryButton("Chỉnh sửa");
        actions.add(add);actions.add(edit);
        body.add(UIFactory.toolbar(new JLabel("Tìm theo mã/tên/SĐT"),txt,new JLabel("Hạng"),tier),BorderLayout.NORTH);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        txt.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            private void changed(){ SwingUtilities.invokeLater(() -> refreshData()); }
            public void insertUpdate(javax.swing.event.DocumentEvent e){ changed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e){ changed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e){ changed(); }
        });
        tier.addActionListener(e->refreshData());
        add.addActionListener(e->form(null));edit.addActionListener(e->editSelected());
        refreshData();
    }

    public void refreshData(){
        try{
            String kw="%"+txt.getText().trim()+"%";
            String t=String.valueOf(tier.getSelectedItem());
            String tierCode="Tất cả hạng".equals(t)?t:UiText.code(t);
            String sql=
                    "SELECT c.customer_id,c.full_name,c.phone,c.email,mt.tier_code,c.current_points,c.status,"
                  + "ISNULL(sp.spent,0) spent,"
                  + "(SELECT TOP 1 card_code FROM membership_cards mc WHERE mc.customer_id=c.customer_id AND mc.status='ACTIVE' ORDER BY issued_at DESC) card "
                  + "FROM customers c "
                  + "LEFT JOIN membership_tiers mt ON mt.tier_id=c.tier_id "
                  + "OUTER APPLY ("
                  + "  SELECT SUM(i.total_amount-ISNULL(r.refunded,0)) spent "
                  + "  FROM invoices i "
                  + "  LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests WHERE status='COMPLETED' GROUP BY invoice_id) r ON r.invoice_id=i.invoice_id "
                  + "  WHERE i.customer_id=c.customer_id AND i.status IN ('PAID','PARTIAL_REFUND','REFUNDED')"
                  + ") sp "
                  + "WHERE (c.customer_id LIKE ? OR c.full_name LIKE ? OR ISNULL(c.phone,'') LIKE ?) "
                  + ("Tất cả hạng".equals(t)?"":"AND mt.tier_code=? ")
                  + "ORDER BY c.full_name";
            java.util.List<Map<String,Object>> rows="Tất cả hạng".equals(t)?Db.query(sql,kw,kw,kw):Db.query(sql,kw,kw,kw,tierCode);
            model.setRowCount(0);
            for(Map<String,Object> r:rows)model.addRow(new Object[]{
                    r.get("customer_id"),r.get("full_name"),r.get("phone"),r.get("email"),UiText.vi(r.get("tier_code")),
                    r.get("current_points"),Formats.money(r.get("spent")),r.get("card"),UiText.vi(r.get("status"))});
            String keyword=txt.getText().trim();
            if(!keyword.isEmpty() && model.getRowCount()>0){
                int best=0;
                for(int i=0;i<model.getRowCount();i++){
                    String code=String.valueOf(model.getValueAt(i,0));
                    String name=String.valueOf(model.getValueAt(i,1));
                    String phone=String.valueOf(model.getValueAt(i,2));
                    if(code.equalsIgnoreCase(keyword) || name.equalsIgnoreCase(keyword) || phone.equalsIgnoreCase(keyword)){
                        best=i;break;
                    }
                }
                table.setRowSelectionInterval(best,best);
                table.scrollRectToVisible(table.getCellRect(best,0,true));
            }
        }catch(Exception ex){showError(ex);}
    }

    private String selectedId(){
        int vr=table.getSelectedRow();
        if(vr<0)return null;
        return String.valueOf(model.getValueAt(table.convertRowIndexToModel(vr),0));
    }

    private void editSelected(){
        String id=selectedId();
        if(id==null){info("Hãy chọn khách hàng.");return;}
        form(id);
    }

    private void form(String id){
        try{
            Map<String,Object> old=id==null?null:Db.one("SELECT * FROM customers WHERE customer_id=?",id);
            JTextField cid=UIFactory.field(180),name=UIFactory.field(260),phone=UIFactory.field(180),email=UIFactory.field(260),address=UIFactory.field(320);
            if(old==null){
                cid.setText(nextCustomerId());
                cid.setEditable(true);
            }else{
                cid.setText(id);cid.setEditable(false);
                name.setText(Formats.safe(old.get("full_name")));
                phone.setText(Formats.safe(old.get("phone")));
                email.setText(Formats.safe(old.get("email")));
                address.setText(Formats.safe(old.get("address")));
            }
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.add(new JLabel("Mã khách hàng"));p.add(cid);
            p.add(new JLabel("Họ tên"));p.add(name);
            p.add(new JLabel("Số điện thoại"));p.add(phone);
            p.add(new JLabel("Thư điện tử"));p.add(email);
            p.add(new JLabel("Địa chỉ"));p.add(address);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),p,id==null?"Thêm khách hàng":"Cập nhật khách hàng",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;

            String customerId=cid.getText().trim().toUpperCase(java.util.Locale.ROOT);
            String fullName=name.getText().trim();
            String phoneValue=phone.getText().trim();
            String emailValue=email.getText().trim();
            if(old==null){
                if(!customerId.matches("KH\\d{3,}"))
                    throw new IllegalArgumentException("Mã khách hàng phải có dạng KH001, KH002, ...");
                if(Db.scalar("SELECT COUNT(*) FROM customers WHERE customer_id=?",customerId) instanceof Number n && n.intValue()>0)
                    throw new IllegalArgumentException("Mã khách hàng đã tồn tại.");
            }
            if(fullName.isEmpty())throw new IllegalArgumentException("Họ tên không được rỗng.");
            if(!phoneValue.isEmpty() && !phoneValue.matches("[0-9]{9,11}"))
                throw new IllegalArgumentException("Số điện thoại phải gồm 9-11 chữ số.");
            if(!emailValue.isEmpty() && (!emailValue.contains("@") || emailValue.startsWith("@") || emailValue.endsWith("@")))
                throw new IllegalArgumentException("Thư điện tử không hợp lệ.");

            if(old==null){
                Integer standard=(Integer)Db.scalar("SELECT tier_id FROM membership_tiers WHERE tier_code='STANDARD'");
                if(standard==null)throw new IllegalStateException("Chưa cấu hình hạng thành viên Tiêu chuẩn.");
                Db.update("INSERT INTO customers(customer_id,full_name,phone,email,address,tier_id,current_points,status) VALUES(?,?,?,?,?,?,0,'ACTIVE')",
                        customerId,fullName,empty(phone),empty(email),empty(address),standard);
            }else{
                Db.update("UPDATE customers SET full_name=?,phone=?,email=?,address=?,updated_at=SYSDATETIME() WHERE customer_id=?",
                        fullName,empty(phone),empty(email),empty(address),id);
            }
            refreshData();
        }catch(Exception ex){showError(ex);}
    }

    private Object empty(JTextField f){String s=f.getText().trim();return s.isEmpty()?null:s;}

    private String nextCustomerId() throws Exception {
        Object value=Db.scalar(
                "SELECT ISNULL(MAX(TRY_CONVERT(int,SUBSTRING(customer_id,3,10))),0) "
              + "FROM customers WHERE customer_id LIKE 'KH%' AND LEN(customer_id)=5 "
              + "AND TRY_CONVERT(int,SUBSTRING(customer_id,3,10)) IS NOT NULL");
        int next=value instanceof Number ? ((Number)value).intValue()+1 : 1;
        return String.format("KH%03d",next);
    }
}

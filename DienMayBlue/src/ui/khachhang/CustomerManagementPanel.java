package ui.khachhang;
import ui.dungchung.DialogUtils;
import service.CustomerService;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import util.Formats;
import ui.dungchung.*;

public class CustomerManagementPanel extends BaseScreenPanel {
	
    private static final long serialVersionUID=1L;
    
    private final CustomerService service = new CustomerService();
    private final JTextField txt=UIFactory.field(320);
    
    private final JComboBox<String> tier=UIFactory.combo("Tất cả hạng","Tiêu chuẩn","Bạc","Vàng","Bạch kim");
    private final DefaultTableModel model=UIFactory.readOnlyModel(new Object[]{
    		"Mã KH","Họ tên","Điện thoại","Thư điện tử",
    		"Hạng","Điểm","Tổng chi tiêu ròng","Thẻ đang dùng","Trạng thái"});
    private final JTable table=UIFactory.table(model);
    
    public CustomerManagementPanel(){
        super("Quản lý khách hàng","Tra cứu, thêm mới và cập nhật hồ sơ khách hàng. Điểm, hạng và thẻ chỉ để tra cứu tại quầy.");
        JButton add=UIFactory.primaryButton("+ Thêm khách hàng");
        JButton edit=UIFactory.secondaryButton("Chỉnh sửa");
        actions.add(add);actions.add(edit);
        
        // Bố cục giao diện
        body.add(UIFactory.toolbar(new JLabel("Tìm theo mã/tên/SĐT"),txt,new JLabel("Hạng"),tier),BorderLayout.NORTH);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        
        // Gắn sự kiện (Events)
        UIFactory.onTextChange(txt, this::refreshData);
        tier.addActionListener(e->refreshData());
        add.addActionListener(e->form(null));
        edit.addActionListener(e->editSelected());
        
        refreshData(); // Tải dữ liệu lần đầu tiên khi mở màn hình
    }
    @Override
	public void refreshData() {
		try{
            String kw="%"+txt.getText().trim()+"%";
            String selected=String.valueOf(tier.getSelectedItem());
            String tierCode="Tất cả hạng".equals(selected)?null:Formats.code(selected);
            
            model.setRowCount(0); // Xóa dữ liệu cũ trên bảng
            for(Map<String,Object> r:service.searchCustomers(kw,tierCode))
                model.addRow(new Object[]{
                    r.get("customer_id"), r.get("full_name"), r.get("phone"), 
                    r.get("email"), Formats.vi(r.get("tier_code")), r.get("current_points"), 
                    Formats.money(r.get("spent")), r.get("card"), Formats.vi(r.get("status"))
                });
            
            // Tự động chọn lại dòng khớp nếu người dùng đang tìm kiếm
            String keyword=txt.getText().trim();
            if(!keyword.isEmpty()&&model.getRowCount()>0){
                int best=0;
                for(int i=0;i<model.getRowCount();i++)
                	if (keyword.equalsIgnoreCase(String.valueOf(model.getValueAt(i, 0)))
                            || keyword.equalsIgnoreCase(String.valueOf(model.getValueAt(i, 1)))
                            || keyword.equalsIgnoreCase(String.valueOf(model.getValueAt(i, 2)))) {
                        best = i;
                        break;
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

	private void form(String id) {
		try {
            Map<String, Object> old = id == null ? null : service.getCustomer(id);
            JTextField cid = UIFactory.field(180);
            JTextField name = UIFactory.field(260);
            JTextField phone = UIFactory.field(180);
            JTextField email = UIFactory.field(260);
            JTextField address = UIFactory.field(320);
            
            if (old == null) {
                cid.setText(service.nextCustomerId());
                cid.setEditable(true);
            } else {
                cid.setText(id);
                cid.setEditable(false);
                name.setText(Formats.safe(old.get("full_name")));
                phone.setText(Formats.safe(old.get("phone")));
                email.setText(Formats.safe(old.get("email")));
                address.setText(Formats.safe(old.get("address")));
            }
            
            JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
            p.add(new JLabel("Mã khách hàng")); p.add(cid);
            p.add(new JLabel("Họ tên")); p.add(name);
            p.add(new JLabel("Số điện thoại")); p.add(phone);
            p.add(new JLabel("Thư điện tử")); p.add(email);
            p.add(new JLabel("Địa chỉ")); p.add(address);
            
            if (JOptionPane.showConfirmDialog(
                    DialogUtils.owner(this), 
                    p, 
                    id == null ? "Thêm khách hàng" : "Cập nhật khách hàng", 
                    JOptionPane.OK_CANCEL_OPTION, 
                    JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            
            service.saveCustomer(cid.getText(), name.getText(), phone.getText(), email.getText(), address.getText(), old == null);
            refreshData();
        } catch (Exception ex) {
            showError(ex);
        }
    }
}


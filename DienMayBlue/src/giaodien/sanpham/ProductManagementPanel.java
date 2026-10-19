package giaodien.sanpham;

import dichvu.BusinessService;
import tienich.Db;
import tienich.Formats;
import tienich.SessionManager;
import tienich.UiText;
import giaodien.dungchung.*;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class ProductManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;

    private final JTextField txtSearch = UIFactory.field(300);
    private final JComboBox<String> cboCategory = UIFactory.combo("Tất cả danh mục");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Mã SP","Mã vạch","Tên sản phẩm","Danh mục","Giá vốn","Giá bán","Tồn","BH (tháng)","Trạng thái"},0) {
        private static final long serialVersionUID=1L;
        @Override public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable table=UIFactory.table(model);
    private final BusinessService service=new BusinessService();

    public ProductManagementPanel() {
        super("Quản lý sản phẩm","UC013 • Quản lý thông tin sản phẩm; tồn kho thay đổi qua Nhập kho / Điều chỉnh tồn.");

        JButton add=UIFactory.primaryButton("+ Thêm sản phẩm");
        JButton edit=UIFactory.secondaryButton("Chỉnh sửa");
        JButton receive=UIFactory.secondaryButton("Nhập kho");
        JButton adjust=UIFactory.secondaryButton("Điều chỉnh tồn");
        JButton status=UIFactory.secondaryButton("Đổi trạng thái");
        JButton refresh=UIFactory.secondaryButton("Làm mới");
        actions.add(add); actions.add(edit); actions.add(receive); actions.add(adjust); actions.add(status); actions.add(refresh);

        JPanel filters=UIFactory.toolbar(new JLabel("Tìm kiếm"),txtSearch,new JLabel("Danh mục"),cboCategory);
        body.add(filters,BorderLayout.NORTH);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);

        add.addActionListener(e->showForm(null));
        edit.addActionListener(e->editSelected());
        receive.addActionListener(e->receiveStock());
        adjust.addActionListener(e->adjustStock());
        status.addActionListener(e->toggleStatus());
        refresh.addActionListener(e->refreshData());
        txtSearch.addActionListener(e->refreshData());

        // Nạp dữ liệu bộ lọc trước khi gắn listener để tránh refresh lặp
        // trong lúc JComboBox đang remove/add item khi khởi tạo màn hình.
        loadCategories();
        cboCategory.addActionListener(e->refreshData());
        refreshData();
    }

    private void loadCategories() {
        try {
            cboCategory.removeAllItems();
            cboCategory.addItem("Tất cả danh mục");
            for(Map<String,Object> r:Db.query("SELECT category_id,category_name FROM categories WHERE active=1 ORDER BY category_name"))
                cboCategory.addItem(r.get("category_id")+" - "+r.get("category_name"));
        } catch(Exception ex){ showError(ex); }
    }

    @Override public void refreshData() {
        try {
            String kw="%"+txtSearch.getText().trim()+"%";
            Integer cat=null;
            String sel=String.valueOf(cboCategory.getSelectedItem());
            if(sel.contains(" - ")) cat=Integer.valueOf(sel.substring(0,sel.indexOf(" - ")));
            String sql="SELECT p.product_id,p.barcode,p.product_name,c.category_name,p.cost_price,p.sale_price,p.stock_quantity,p.warranty_months,p.status "
                    +"FROM products p JOIN categories c ON c.category_id=p.category_id "
                    +"WHERE (p.product_id LIKE ? OR p.barcode LIKE ? OR p.product_name LIKE ?) "
                    +(cat==null?"":"AND p.category_id=? ")
                    +"ORDER BY p.product_name";
            java.util.List<Map<String,Object>> rows=cat==null?Db.query(sql,kw,kw,kw):Db.query(sql,kw,kw,kw,cat);
            model.setRowCount(0);
            for(Map<String,Object> r:rows) model.addRow(new Object[]{
                    r.get("product_id"),r.get("barcode"),r.get("product_name"),r.get("category_name"),
                    Formats.money(r.get("cost_price")),Formats.money(r.get("sale_price")),r.get("stock_quantity"),r.get("warranty_months"),UiText.vi(r.get("status"))});
        } catch(Exception ex){showError(ex);}
    }

    private String selectedProductId(){
        int vr=table.getSelectedRow();
        if(vr<0) return null;
        return String.valueOf(model.getValueAt(table.convertRowIndexToModel(vr),0));
    }

    private void editSelected(){
        String id=selectedProductId();
        if(id==null){info("Hãy chọn sản phẩm cần sửa.");return;}
        showForm(id);
    }

    private void showForm(String productId){
        try{
            Map<String,Object> old=productId==null?null:Db.one("SELECT * FROM products WHERE product_id=?",productId);
            JTextField id=UIFactory.field(220),barcode=UIFactory.field(220),name=UIFactory.field(320),
                    cost=UIFactory.field(180),sale=UIFactory.field(180),stock=UIFactory.field(100),warranty=UIFactory.field(100),image=UIFactory.field(300);
            JComboBox<String> cat=UIFactory.combo(),sup=UIFactory.combo("Không chọn");
            warranty.setText("12");
            stock.setText("0"); stock.setEditable(false);

            for(Map<String,Object> x:Db.query("SELECT category_id,category_name FROM categories WHERE active=1 ORDER BY category_name"))
                cat.addItem(x.get("category_id")+" - "+x.get("category_name"));
            for(Map<String,Object> x:Db.query("SELECT supplier_id,supplier_name,status FROM suppliers ORDER BY supplier_name"))
                sup.addItem(x.get("supplier_id")+" - "+x.get("supplier_name")+("ACTIVE".equals(String.valueOf(x.get("status")))?"":" (Ngừng hợp tác)"));

            if(old!=null){
                id.setText(String.valueOf(old.get("product_id")));id.setEditable(false);
                barcode.setText(String.valueOf(old.get("barcode")));name.setText(String.valueOf(old.get("product_name")));
                cost.setText(String.valueOf(old.get("cost_price")));sale.setText(String.valueOf(old.get("sale_price")));
                stock.setText(String.valueOf(old.get("stock_quantity")));warranty.setText(String.valueOf(old.get("warranty_months")));image.setText(Formats.safe(old.get("image_path")));
                selectPrefix(cat,String.valueOf(old.get("category_id")));
                if(old.get("supplier_id")!=null) selectPrefix(sup,String.valueOf(old.get("supplier_id")));
            }

            JLabel stockHint=new JLabel("Tồn kho chỉ thay đổi qua Nhập kho / Điều chỉnh tồn");
            stockHint.setForeground(UIConstants.MUTED);
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.add(new JLabel("Mã sản phẩm"));p.add(id);p.add(new JLabel("Mã vạch"));p.add(barcode);
            p.add(new JLabel("Tên sản phẩm"));p.add(name);p.add(new JLabel("Danh mục"));p.add(cat);
            p.add(new JLabel("Nhà cung cấp chính"));p.add(sup);p.add(new JLabel("Giá vốn"));p.add(cost);
            p.add(new JLabel("Giá bán"));p.add(sale);p.add(new JLabel("Tồn kho hiện tại"));p.add(stock);
            p.add(new JLabel(""));p.add(stockHint);p.add(new JLabel("Bảo hành (tháng)"));p.add(warranty);
            p.add(new JLabel("Đường dẫn hình ảnh"));p.add(image);

            if(JOptionPane.showConfirmDialog(giaodien.dungchung.DialogUtils.owner(this),p,old==null?"Thêm sản phẩm":"Chỉnh sửa sản phẩm",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION) return;

            String pid=id.getText().trim(),bc=barcode.getText().trim(),pn=name.getText().trim();
            if(pid.isEmpty() || pid.length()>20) throw new IllegalArgumentException("Mã sản phẩm là bắt buộc và tối đa 20 ký tự.");
            if(bc.isEmpty() || bc.length()>50) throw new IllegalArgumentException("Mã vạch là bắt buộc và tối đa 50 ký tự.");
            if(pn.isEmpty()) throw new IllegalArgumentException("Tên sản phẩm là bắt buộc.");
            BigDecimal cp=new BigDecimal(cost.getText().trim()),sp=new BigDecimal(sale.getText().trim());
            if(cp.compareTo(BigDecimal.ZERO)<0 || sp.compareTo(cp)<=0) throw new IllegalArgumentException("Giá bán phải lớn hơn giá vốn và giá vốn không được âm.");
            int category=Integer.parseInt(prefix(cat));
            Integer supplier=sup.getSelectedIndex()==0?null:Integer.valueOf(prefix(sup));
            int warrantyMonths=Integer.parseInt(warranty.getText().trim());
            if(warrantyMonths<12) throw new IllegalArgumentException("Thời hạn bảo hành tối thiểu là 12 tháng.");

            Db.transaction(con->{
                Object dupBarcode=old==null
                        ?Db.scalar(con,"SELECT COUNT(*) FROM products WHERE barcode=?",bc)
                        :Db.scalar(con,"SELECT COUNT(*) FROM products WHERE barcode=? AND product_id<>?",bc,pid);
                if(((Number)dupBarcode).intValue()>0) throw new IllegalArgumentException("Mã vạch đã được dùng cho sản phẩm khác.");
                if(old==null && Db.one(con,"SELECT product_id FROM products WHERE product_id=?",pid)!=null)
                    throw new IllegalArgumentException("Mã sản phẩm đã tồn tại.");
                Map<String,Object> c=Db.one(con,"SELECT active FROM categories WHERE category_id=?",category);
                if(c==null || !Boolean.TRUE.equals(c.get("active"))) throw new IllegalArgumentException("Danh mục không còn hoạt động.");
                if(supplier!=null && Db.one(con,"SELECT supplier_id FROM suppliers WHERE supplier_id=?",supplier)==null)
                    throw new IllegalArgumentException("Nhà cung cấp không tồn tại.");

                if(old==null){
                    Db.update(con,"INSERT INTO products(product_id,barcode,product_name,category_id,supplier_id,cost_price,sale_price,stock_quantity,warranty_months,image_path,status) "
                            +"VALUES(?,?,?,?,?,?,?,0,?,?,'OUT_OF_STOCK')",
                            pid,bc,pn,category,supplier,cp,sp,warrantyMonths,image.getText().trim().isEmpty()?null:image.getText().trim());
                }else{
                    Db.update(con,"UPDATE products SET barcode=?,product_name=?,category_id=?,supplier_id=?,cost_price=?,sale_price=?,warranty_months=?,image_path=?,updated_at=SYSDATETIME() WHERE product_id=?",
                            bc,pn,category,supplier,cp,sp,warrantyMonths,image.getText().trim().isEmpty()?null:image.getText().trim(),pid);
                }
                if(SessionManager.getCurrentAccount()!=null)
                    Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                            SessionManager.getCurrentAccount().getAccountId(),old==null?"PRODUCT_CREATED":"PRODUCT_UPDATED","products",pid,
                            (old==null?"Tạo":"Cập nhật")+" sản phẩm "+pn+(old==null?"; tồn ban đầu = 0":""));
                return null;
            });
            refreshData();
        }catch(Exception ex){showError(ex);}
    }

    private void receiveStock(){
        String productId=selectedProductId();
        if(productId==null){info("Hãy chọn sản phẩm cần nhập kho.");return;}
        try{
            Map<String,Object> p=Db.one("SELECT product_name,cost_price,supplier_id,status FROM products WHERE product_id=?",productId);
            if(p==null) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
            if("DISCONTINUED".equals(String.valueOf(p.get("status")))) throw new IllegalStateException("Sản phẩm đã ngừng kinh doanh.");

            JComboBox<String> supplier=UIFactory.combo();
            for(Map<String,Object> x:Db.query("SELECT supplier_id,supplier_name FROM suppliers WHERE status='ACTIVE' ORDER BY supplier_name"))
                supplier.addItem(x.get("supplier_id")+" - "+x.get("supplier_name"));
            if(supplier.getItemCount()==0) throw new IllegalStateException("Chưa có nhà cung cấp đang hoạt động.");
            if(p.get("supplier_id")!=null) selectPrefix(supplier,String.valueOf(p.get("supplier_id")));

            JTextField qty=UIFactory.field(100),unitCost=UIFactory.field(160),ref=UIFactory.field(180),due=UIFactory.field(140),note=UIFactory.field(280);
            qty.setText("1"); unitCost.setText(String.valueOf(p.get("cost_price")));
            ref.setText("PN-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
            due.setText(LocalDate.now().plusDays(30).toString());
            JPanel form=new JPanel(new GridLayout(0,2,8,8));
            form.add(new JLabel("Sản phẩm"));form.add(new JLabel(productId+" - "+p.get("product_name")));
            form.add(new JLabel("Nhà cung cấp"));form.add(supplier);
            form.add(new JLabel("Số lượng nhập"));form.add(qty);
            form.add(new JLabel("Đơn giá nhập"));form.add(unitCost);
            form.add(new JLabel("Mã phiếu nhập"));form.add(ref);
            form.add(new JLabel("Hạn thanh toán (yyyy-MM-dd)"));form.add(due);
            form.add(new JLabel("Ghi chú"));form.add(note);
            if(JOptionPane.showConfirmDialog(giaodien.dungchung.DialogUtils.owner(this),form,"Nhập kho / phát sinh công nợ",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;

            int q=Integer.parseInt(qty.getText().trim());
            BigDecimal uc=new BigDecimal(unitCost.getText().trim());
            LocalDate dueDate=due.getText().trim().isEmpty()?null:LocalDate.parse(due.getText().trim());
            BigDecimal total=uc.multiply(BigDecimal.valueOf(q));
            if(!confirm("Xác nhận nhập "+q+" sản phẩm?\nGiá trị phiếu nhập: "+Formats.money(total)+"\nHệ thống sẽ tạo công nợ tương ứng cho nhà cung cấp.")) return;
            service.receiveProductStock(productId,Integer.parseInt(prefix(supplier)),q,uc,ref.getText().trim(),dueDate,note.getText().trim());
            refreshData();
            info("Đã nhập kho và ghi nhận công nợ nhà cung cấp.");
        }catch(Exception ex){showError(ex);}
    }

    private void adjustStock(){
        String productId=selectedProductId();
        if(productId==null){info("Hãy chọn sản phẩm cần điều chỉnh tồn.");return;}
        try{
            Map<String,Object> p=Db.one("SELECT product_name,stock_quantity FROM products WHERE product_id=?",productId);
            JTextField newQty=UIFactory.field(120),reason=UIFactory.field(300);
            newQty.setText(String.valueOf(p.get("stock_quantity")));
            JPanel form=new JPanel(new GridLayout(0,2,8,8));
            form.add(new JLabel("Sản phẩm"));form.add(new JLabel(productId+" - "+p.get("product_name")));
            form.add(new JLabel("Tồn hiện tại"));form.add(new JLabel(String.valueOf(p.get("stock_quantity"))));
            form.add(new JLabel("Tồn sau điều chỉnh"));form.add(newQty);
            form.add(new JLabel("Lý do"));form.add(reason);
            if(JOptionPane.showConfirmDialog(giaodien.dungchung.DialogUtils.owner(this),form,"Điều chỉnh tồn kho",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            service.adjustProductStock(productId,Integer.parseInt(newQty.getText().trim()),reason.getText().trim());
            refreshData();
            info("Đã điều chỉnh tồn kho và ghi nhật ký thao tác.");
        }catch(Exception ex){showError(ex);}
    }

    private void toggleStatus(){
        String productId=selectedProductId();
        if(productId==null){info("Hãy chọn sản phẩm.");return;}
        try{
            Map<String,Object> p=Db.one("SELECT product_name,stock_quantity,status FROM products WHERE product_id=?",productId);
            boolean discontinued="DISCONTINUED".equals(String.valueOf(p.get("status")));
            String msg=discontinued
                    ?"Khôi phục kinh doanh sản phẩm "+productId+"?"
                    :"Ngừng kinh doanh sản phẩm "+productId+"?\nTồn hiện tại: "+p.get("stock_quantity")+". Dữ liệu lịch sử vẫn được giữ nguyên.";
            if(!confirm(msg)) return;
            service.setProductDiscontinued(productId,!discontinued);
            refreshData();
        }catch(Exception ex){showError(ex);}
    }

    private String prefix(JComboBox<String> c){
        String s=String.valueOf(c.getSelectedItem());
        int i=s.indexOf(" - ");
        if(i<0) throw new IllegalArgumentException("Vui lòng chọn dữ liệu hợp lệ.");
        return s.substring(0,i);
    }
    private void selectPrefix(JComboBox<String> c,String p){
        for(int i=0;i<c.getItemCount();i++) if(String.valueOf(c.getItemAt(i)).startsWith(p+" -")){c.setSelectedIndex(i);return;}
    }
}

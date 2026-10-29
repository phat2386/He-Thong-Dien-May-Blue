package ui.sanpham;
import ui.dungchung.DialogUtils;
import service.ProductService;
import util.Formats;
import util.SessionManager;
import ui.dungchung.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
public class ProductManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField txtSearch = UIFactory.field(300);
    private final JComboBox<String> cboCategory = UIFactory.combo("Tất cả danh mục");
    private final Map<String,Integer> categoryIds = new LinkedHashMap<String,Integer>();
    private final DefaultTableModel model = UIFactory.readOnlyModel(new Object[]{"Mã SP","Tên sản phẩm","Danh mục","Giá vốn","Giá bán","Tồn","BH (tháng)","Trạng thái"});
    private final JTable table=UIFactory.table(model);
    private final ProductService service=new ProductService();
    public ProductManagementPanel() {
        super("Quản lý sản phẩm","Quản lý thông tin sản phẩm; tồn kho được hệ thống tự động cập nhật theo nhập hàng, bán hàng và đổi trả.");
        JButton add=UIFactory.primaryButton("+ Thêm sản phẩm");
        JButton edit=UIFactory.secondaryButton("Chỉnh sửa");
        JButton receive=UIFactory.secondaryButton("Nhập kho");
        JButton status=UIFactory.secondaryButton("Đổi trạng thái");
        JButton refresh=UIFactory.secondaryButton("Làm mới");
        actions.add(add); actions.add(edit); actions.add(receive); actions.add(status); actions.add(refresh);
        JPanel filters=UIFactory.toolbar(new JLabel("Tìm kiếm"),txtSearch,new JLabel("Danh mục"),cboCategory);
        body.add(filters,BorderLayout.NORTH);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        add.addActionListener(e->showForm(null));
        edit.addActionListener(e->editSelected());
        receive.addActionListener(e->receiveStock());
        status.addActionListener(e->toggleStatus());
        refresh.addActionListener(e->refreshData());
        UIFactory.onTextChange(txtSearch, this::refreshData);
        loadCategories();
        cboCategory.addActionListener(e->refreshData());
        refreshData();
    }
    private void loadCategories() {
        try {
            cboCategory.removeAllItems();
            categoryIds.clear();
            cboCategory.addItem("Tất cả danh mục");
            for(Map<String,Object> r:service.query("SELECT category_id,category_name FROM categories WHERE active=1 ORDER BY category_name")) {
                String name = String.valueOf(r.get("category_name"));
                cboCategory.addItem(name);
                categoryIds.put(name, ((Number)r.get("category_id")).intValue());
            }
        } catch(Exception ex){ showError(ex); }
    }
    @Override public void refreshData() {
        try {
            String kw="%"+txtSearch.getText().trim()+"%";
            Integer cat=null;
            String sel=String.valueOf(cboCategory.getSelectedItem());
            if(!"Tất cả danh mục".equals(sel)) cat=categoryIds.get(sel);
            String sql="SELECT p.product_id,p.product_name,c.category_name,p.cost_price,p.sale_price,p.stock_quantity,p.warranty_months,p.status "
                    +"FROM products p JOIN categories c ON c.category_id=p.category_id "
                    +"WHERE (p.product_id LIKE ? OR p.product_name LIKE ?) " +(cat==null?"":"AND p.category_id=? ")
                    +"ORDER BY p.product_name";
            java.util.List<Map<String,Object>> rows=cat==null?service.query(sql,kw,kw):service.query(sql,kw,kw,cat);
            model.setRowCount(0);
            for(Map<String,Object> r:rows) model.addRow(new Object[]{
                    r.get("product_id"),r.get("product_name"),r.get("category_name"),
                    Formats.money(r.get("cost_price")),Formats.money(r.get("sale_price")),r.get("stock_quantity"),r.get("warranty_months"),Formats.vi(r.get("status"))});
            if(model.getRowCount()>0 && !txtSearch.getText().trim().isEmpty()) {
                table.setRowSelectionInterval(0,0);
                table.scrollRectToVisible(table.getCellRect(0,0,true));
            }
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
            Map<String,Object> old=productId==null?null:service.one("SELECT * FROM products WHERE product_id=?",productId);
            JTextField id=UIFactory.field(220),name=UIFactory.field(320),
                    cost=UIFactory.field(180),sale=UIFactory.field(180),stock=UIFactory.field(100),warranty=UIFactory.field(100),image=UIFactory.field(300);
            JComboBox<String> cat=UIFactory.combo(),sup=UIFactory.combo();
            warranty.setText("12");
            stock.setText("0"); stock.setEditable(false);
            Map<String,Integer> formCategoryIds = new LinkedHashMap<String,Integer>();
            cat.addItem("");
            for(Map<String,Object> x:service.query("SELECT category_id,category_name FROM categories WHERE active=1 ORDER BY category_name")) {
                String categoryName = String.valueOf(x.get("category_name"));
                cat.addItem(categoryName);
                formCategoryIds.put(categoryName, ((Number)x.get("category_id")).intValue());
            }
            sup.addItem("");
            for(Map<String,Object> x:service.query("SELECT supplier_id,supplier_name,status FROM suppliers ORDER BY supplier_id"))
                sup.addItem(x.get("supplier_id")+" - "+x.get("supplier_name")+("ACTIVE".equals(String.valueOf(x.get("status")))?"":" (Ngừng hợp tác)"));
            if(old==null){
                id.setText(nextProductId());
                id.setEditable(true);
                cat.setSelectedIndex(0);
                sup.setSelectedIndex(0);
            }else{
                id.setText(String.valueOf(old.get("product_id")));id.setEditable(false);
                name.setText(String.valueOf(old.get("product_name")));
                cost.setText(String.valueOf(old.get("cost_price")));sale.setText(String.valueOf(old.get("sale_price")));
                stock.setText(String.valueOf(old.get("stock_quantity")));warranty.setText(String.valueOf(old.get("warranty_months")));image.setText(Formats.safe(old.get("image_path")));
                for(Map.Entry<String,Integer> entry : formCategoryIds.entrySet()) {
                    if(entry.getValue().intValue()==((Number)old.get("category_id")).intValue()) {
                        cat.setSelectedItem(entry.getKey());
                        break;
                    }
                }
                if(old.get("supplier_id")!=null) selectPrefix(sup,String.valueOf(old.get("supplier_id")));
            }
            JLabel stockHint=new JLabel("Tồn kho được tự động cập nhật theo nhập kho, bán hàng và đổi trả");
            stockHint.setForeground(UIConstants.MUTED);
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.add(new JLabel("Mã sản phẩm"));p.add(id);
            p.add(new JLabel("Tên sản phẩm"));p.add(name);p.add(new JLabel("Danh mục"));p.add(cat);
            p.add(new JLabel("Nhà cung cấp chính"));p.add(sup);p.add(new JLabel("Giá vốn"));p.add(cost);
            p.add(new JLabel("Giá bán"));p.add(sale);p.add(new JLabel("Tồn kho hiện tại"));p.add(stock);
            p.add(new JLabel(""));p.add(stockHint);p.add(new JLabel("Bảo hành (tháng)"));p.add(warranty);
            p.add(new JLabel("Đường dẫn hình ảnh"));p.add(image);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),p,old==null?"Thêm sản phẩm":"Chỉnh sửa sản phẩm",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION) return;
            String pid=id.getText().trim(),pn=name.getText().trim();
            if(pid.isEmpty() || pid.length()>20) throw new IllegalArgumentException("Mã sản phẩm là bắt buộc và tối đa 20 ký tự.");
            if(pn.isEmpty()) throw new IllegalArgumentException("Tên sản phẩm là bắt buộc.");
            BigDecimal cp=new BigDecimal(cost.getText().trim()),sp=new BigDecimal(sale.getText().trim());
            if(cp.compareTo(BigDecimal.ZERO)<0 || sp.compareTo(cp)<=0) throw new IllegalArgumentException("Giá bán phải lớn hơn giá vốn và giá vốn không được âm.");
            Integer category=formCategoryIds.get(String.valueOf(cat.getSelectedItem()));
            if(category==null) throw new IllegalArgumentException("Vui lòng chọn danh mục hợp lệ.");
            Integer supplier=sup.getSelectedIndex()<=0?null:Integer.valueOf(prefix(sup));
            int warrantyMonths=Integer.parseInt(warranty.getText().trim());
            if(warrantyMonths<12) throw new IllegalArgumentException("Thời hạn bảo hành tối thiểu là 12 tháng.");
            service.transaction(con->{
                if(old==null && service.one(con,"SELECT product_id FROM products WHERE product_id=?",pid)!=null)
                    throw new IllegalArgumentException("Mã sản phẩm đã tồn tại.");
                Map<String,Object> c=service.one(con,"SELECT active FROM categories WHERE category_id=?",category);
                if(c==null || !Boolean.TRUE.equals(c.get("active"))) throw new IllegalArgumentException("Danh mục không còn hoạt động.");
                if(supplier!=null && service.one(con,"SELECT supplier_id FROM suppliers WHERE supplier_id=?",supplier)==null)
                    throw new IllegalArgumentException("Nhà cung cấp không tồn tại.");
                if(old==null){
                    String internalBarcode = "INTERNAL-" + pid;
                    service.update(con,"INSERT INTO products(product_id,barcode,product_name,category_id,supplier_id,cost_price,sale_price,stock_quantity,warranty_months,image_path,status) "
                            +"VALUES(?,?,?,?,?,?,?,0,?,?,'OUT_OF_STOCK')",
                            pid,internalBarcode,pn,category,supplier,cp,sp,warrantyMonths,image.getText().trim().isEmpty()?null:image.getText().trim());
                }else{
                    service.update(con,"UPDATE products SET product_name=?,category_id=?,supplier_id=?,cost_price=?,sale_price=?,warranty_months=?,image_path=?,updated_at=SYSDATETIME() WHERE product_id=?",
                            pn,category,supplier,cp,sp,warrantyMonths,image.getText().trim().isEmpty()?null:image.getText().trim(),pid);
                }
                if(SessionManager.getCurrentAccount()!=null)
                    service.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
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
            Map<String,Object> p=service.one("SELECT product_name,cost_price,supplier_id,status FROM products WHERE product_id=?",productId);
            if(p==null) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
            if("DISCONTINUED".equals(String.valueOf(p.get("status")))) throw new IllegalStateException("Sản phẩm đã ngừng kinh doanh.");
            JComboBox<String> supplier=UIFactory.combo();
            for(Map<String,Object> x:service.query("SELECT supplier_id,supplier_name FROM suppliers WHERE status='ACTIVE' ORDER BY supplier_id"))
                supplier.addItem(x.get("supplier_id")+" - "+x.get("supplier_name"));
            if(supplier.getItemCount()==0) throw new IllegalStateException("Chưa có nhà cung cấp đang hoạt động.");
            if(p.get("supplier_id")!=null) selectPrefix(supplier,String.valueOf(p.get("supplier_id")));
            JTextField qty=UIFactory.field(100),unitCost=UIFactory.field(160),ref=UIFactory.field(180);
            qty.setText("1");
            unitCost.setText(String.valueOf(p.get("cost_price")));
            unitCost.setEditable(false);
            ref.setText(nextReceiptCode());
            JPanel form=new JPanel(new GridLayout(0,2,8,8));
            form.add(new JLabel("Sản phẩm"));form.add(new JLabel(productId+" - "+p.get("product_name")));
            form.add(new JLabel("Nhà cung cấp"));form.add(supplier);
            form.add(new JLabel("Số lượng nhập"));form.add(qty);
            form.add(new JLabel("Đơn giá nhập"));form.add(unitCost);
            form.add(new JLabel("Mã phiếu nhập"));form.add(ref);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),form,"Nhập kho",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            int q=Integer.parseInt(qty.getText().trim());
            BigDecimal uc=new BigDecimal(unitCost.getText().trim());
            BigDecimal total=uc.multiply(BigDecimal.valueOf(q));
            if(!confirm("Xác nhận nhập "+q+" sản phẩm?\nĐơn giá nhập: "+Formats.money(uc)+"\nTổng giá trị: "+Formats.money(total))) return;
            service.receiveProductStock(productId,Integer.parseInt(prefix(supplier)),q,ref.getText().trim());
            refreshData();
            info("Đã nhập kho. Tồn kho đã được cập nhật tự động.");
        }catch(Exception ex){showError(ex);}
    }
    private void toggleStatus(){
        String productId=selectedProductId();
        if(productId==null){info("Hãy chọn sản phẩm.");return;}
        try{
            Map<String,Object> p=service.one("SELECT product_name,stock_quantity,status FROM products WHERE product_id=?",productId);
            boolean discontinued="DISCONTINUED".equals(String.valueOf(p.get("status")));
            String msg=discontinued
                    ?"Khôi phục kinh doanh sản phẩm "+productId+"?"
                    :"Ngừng kinh doanh sản phẩm "+productId+"?\nTồn hiện tại: "+p.get("stock_quantity")+". Dữ liệu lịch sử vẫn được giữ nguyên.";
            if(!confirm(msg)) return;
            service.setProductDiscontinued(productId,!discontinued);
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private String nextReceiptCode() throws Exception {
        int max = 0;
        for (Map<String,Object> row : service.query("SELECT reference_code FROM supplier_debts WHERE reference_code IS NOT NULL")) {
            String code = String.valueOf(row.get("reference_code")).trim().toUpperCase(Locale.ROOT);
            if (!code.matches("PN\\d+")) continue;
            try {
                max = Math.max(max, Integer.parseInt(code.substring(2)));
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("PN%03d", max + 1);
    }
    private String nextProductId() throws Exception {
        int max = 0;
        for (Map<String,Object> row : service.query("SELECT product_id FROM products")) {
            String code = String.valueOf(row.get("product_id")).trim().toUpperCase(Locale.ROOT);
            if (!code.matches("SP\\d+")) continue;
            try {
                max = Math.max(max, Integer.parseInt(code.substring(2)));
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("SP%03d", max + 1);
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

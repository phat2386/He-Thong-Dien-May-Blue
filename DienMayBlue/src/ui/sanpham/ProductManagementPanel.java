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
    private final JComboBox<String> cmbCategory = UIFactory.combo("Tất cả danh mục");
    private final Map<String,Integer> categoryIds = new LinkedHashMap<String,Integer>();
    private final DefaultTableModel model = UIFactory.readOnlyModel(new Object[]{"Mã SP","Tên sản phẩm","Danh mục","Giá vốn","Giá bán","Tồn","BH (tháng)","Trạng thái"});
    private final JTable tblProduct=UIFactory.table(model);
    private final ProductService service=new ProductService();
    public ProductManagementPanel() {
        super("Quản lý sản phẩm","Quản lý thông tin sản phẩm; tồn kho được hệ thống tự động cập nhật theo nhập hàng, bán hàng và đổi trả.");
        JButton btnAdd=UIFactory.primaryButton("+ Thêm sản phẩm");
        JButton btnEdit=UIFactory.secondaryButton("Chỉnh sửa");
        JButton btnReceive=UIFactory.secondaryButton("Nhập kho");
        JButton btnStatus=UIFactory.secondaryButton("Đổi trạng thái");
        JButton btnRefresh=UIFactory.secondaryButton("Làm mới");
        actions.add(btnAdd); actions.add(btnEdit); actions.add(btnReceive); actions.add(btnStatus); actions.add(btnRefresh);
        JPanel pnlFilters=UIFactory.toolbar(new JLabel("Tìm kiếm"),txtSearch,new JLabel("Danh mục"),cmbCategory);
        body.add(pnlFilters,BorderLayout.NORTH);
        body.add(UIFactory.scroll(tblProduct),BorderLayout.CENTER);
        btnAdd.addActionListener(e->showForm(null));
        btnEdit.addActionListener(e->editSelected());
        btnReceive.addActionListener(e->receiveStock());
        btnStatus.addActionListener(e->toggleStatus());
        btnRefresh.addActionListener(e->refreshData());
        UIFactory.onTextChange(txtSearch, this::refreshData);
        loadCategories();
        cmbCategory.addActionListener(e->refreshData());
        refreshData();
    }
    private void loadCategories() {
        try {
            cmbCategory.removeAllItems();
            categoryIds.clear();
            cmbCategory.addItem("Tất cả danh mục");
            for(Map<String,Object> r:service.query("SELECT category_id,category_name FROM categories WHERE active=1 ORDER BY category_name")) {
                String name = String.valueOf(r.get("category_name"));
                cmbCategory.addItem(name);
                categoryIds.put(name, ((Number)r.get("category_id")).intValue());
            }
        } catch(Exception ex){ showError(ex); }
    }
    @Override public void refreshData() {
        try {
            String kw="%"+txtSearch.getText().trim()+"%";
            Integer cat=null;
            String sel=String.valueOf(cmbCategory.getSelectedItem());
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
                tblProduct.setRowSelectionInterval(0,0);
                tblProduct.scrollRectToVisible(tblProduct.getCellRect(0,0,true));
            }
        } catch(Exception ex){showError(ex);}
    }
    private String selectedProductId(){
        int vr=tblProduct.getSelectedRow();
        if(vr<0) return null;
        return String.valueOf(model.getValueAt(tblProduct.convertRowIndexToModel(vr),0));
    }
    private void editSelected(){
        String id=selectedProductId();
        if(id==null){info("Hãy chọn sản phẩm cần sửa.");return;}
        showForm(id);
    }
    private void showForm(String productId){
        try{
            Map<String,Object> old=productId==null?null:service.one("SELECT * FROM products WHERE product_id=?",productId);
            JTextField txtId=UIFactory.field(220),txtName=UIFactory.field(320),
                    txtCost=UIFactory.field(180),txtSale=UIFactory.field(180),txtStock=UIFactory.field(100),txtWarranty=UIFactory.field(100),txtImage=UIFactory.field(300);
            JComboBox<String> cmbFormCategory=UIFactory.combo(),cmbFormSupplier=UIFactory.combo();
            txtWarranty.setText("12");
            txtStock.setText("0"); txtStock.setEditable(false);
            Map<String,Integer> formCategoryIds = new LinkedHashMap<String,Integer>();
            cmbFormCategory.addItem("");
            for(Map<String,Object> x:service.query("SELECT category_id,category_name FROM categories WHERE active=1 ORDER BY category_name")) {
                String categoryName = String.valueOf(x.get("category_name"));
                cmbFormCategory.addItem(categoryName);
                formCategoryIds.put(categoryName, ((Number)x.get("category_id")).intValue());
            }
            cmbFormSupplier.addItem("");
            for(Map<String,Object> x:service.query("SELECT supplier_id,supplier_name,status FROM suppliers ORDER BY supplier_id"))
                cmbFormSupplier.addItem(x.get("supplier_id")+" - "+x.get("supplier_name")+("ACTIVE".equals(String.valueOf(x.get("status")))?"":" (Ngừng hợp tác)"));
            if(old==null){
                txtId.setText(nextProductId());
                txtId.setEditable(true);
                cmbFormCategory.setSelectedIndex(0);
                cmbFormSupplier.setSelectedIndex(0);
            }else{
                txtId.setText(String.valueOf(old.get("product_id")));txtId.setEditable(false);
                txtName.setText(String.valueOf(old.get("product_name")));
                txtCost.setText(String.valueOf(old.get("cost_price")));txtSale.setText(String.valueOf(old.get("sale_price")));
                txtStock.setText(String.valueOf(old.get("stock_quantity")));txtWarranty.setText(String.valueOf(old.get("warranty_months")));txtImage.setText(Formats.safe(old.get("image_path")));
                for(Map.Entry<String,Integer> entry : formCategoryIds.entrySet()) {
                    if(entry.getValue().intValue()==((Number)old.get("category_id")).intValue()) {
                        cmbFormCategory.setSelectedItem(entry.getKey());
                        break;
                    }
                }
                if(old.get("supplier_id")!=null) selectPrefix(cmbFormSupplier,String.valueOf(old.get("supplier_id")));
            }
            JLabel lblStockHint=new JLabel("Tồn kho được tự động cập nhật theo nhập kho, bán hàng và đổi trả");
            lblStockHint.setForeground(UIConstants.MUTED);
            JPanel pnlForm=new JPanel(new GridLayout(0,2,8,8));
            pnlForm.add(new JLabel("Mã sản phẩm"));pnlForm.add(txtId);
            pnlForm.add(new JLabel("Tên sản phẩm"));pnlForm.add(txtName);pnlForm.add(new JLabel("Danh mục"));pnlForm.add(cmbFormCategory);
            pnlForm.add(new JLabel("Nhà cung cấp chính"));pnlForm.add(cmbFormSupplier);pnlForm.add(new JLabel("Giá vốn"));pnlForm.add(txtCost);
            pnlForm.add(new JLabel("Giá bán"));pnlForm.add(txtSale);pnlForm.add(new JLabel("Tồn kho hiện tại"));pnlForm.add(txtStock);
            pnlForm.add(new JLabel(""));pnlForm.add(lblStockHint);pnlForm.add(new JLabel("Bảo hành (tháng)"));pnlForm.add(txtWarranty);
            pnlForm.add(new JLabel("Đường dẫn hình ảnh"));pnlForm.add(txtImage);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),pnlForm,old==null?"Thêm sản phẩm":"Chỉnh sửa sản phẩm",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION) return;
            String pid=txtId.getText().trim(),pn=txtName.getText().trim();
            if(pid.isEmpty() || pid.length()>20) throw new IllegalArgumentException("Mã sản phẩm là bắt buộc và tối đa 20 ký tự.");
            if(pn.isEmpty()) throw new IllegalArgumentException("Tên sản phẩm là bắt buộc.");
            BigDecimal cp=new BigDecimal(txtCost.getText().trim()),sp=new BigDecimal(txtSale.getText().trim());
            if(cp.compareTo(BigDecimal.ZERO)<0 || sp.compareTo(cp)<=0) throw new IllegalArgumentException("Giá bán phải lớn hơn giá vốn và giá vốn không được âm.");
            Integer category=formCategoryIds.get(String.valueOf(cmbFormCategory.getSelectedItem()));
            if(category==null) throw new IllegalArgumentException("Vui lòng chọn danh mục hợp lệ.");
            Integer supplier=cmbFormSupplier.getSelectedIndex()<=0?null:Integer.valueOf(prefix(cmbFormSupplier));
            int warrantyMonths=Integer.parseInt(txtWarranty.getText().trim());
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
                            pid,internalBarcode,pn,category,supplier,cp,sp,warrantyMonths,txtImage.getText().trim().isEmpty()?null:txtImage.getText().trim());
                }else{
                    service.update(con,"UPDATE products SET product_name=?,category_id=?,supplier_id=?,cost_price=?,sale_price=?,warranty_months=?,image_path=?,updated_at=SYSDATETIME() WHERE product_id=?",
                            pn,category,supplier,cp,sp,warrantyMonths,txtImage.getText().trim().isEmpty()?null:txtImage.getText().trim(),pid);
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
            JComboBox<String> cmbSupplier=UIFactory.combo();
            for(Map<String,Object> x:service.query("SELECT supplier_id,supplier_name FROM suppliers WHERE status='ACTIVE' ORDER BY supplier_id"))
                cmbSupplier.addItem(x.get("supplier_id")+" - "+x.get("supplier_name"));
            if(cmbSupplier.getItemCount()==0) throw new IllegalStateException("Chưa có nhà cung cấp đang hoạt động.");
            if(p.get("supplier_id")!=null) selectPrefix(cmbSupplier,String.valueOf(p.get("supplier_id")));
            JTextField txtQty=UIFactory.field(100),txtUnitCost=UIFactory.field(160),txtRef=UIFactory.field(180);
            txtQty.setText("1");
            txtUnitCost.setText(String.valueOf(p.get("cost_price")));
            txtUnitCost.setEditable(false);
            txtRef.setText(nextReceiptCode());
            JPanel pnlReceive=new JPanel(new GridLayout(0,2,8,8));
            pnlReceive.add(new JLabel("Sản phẩm"));pnlReceive.add(new JLabel(productId+" - "+p.get("product_name")));
            pnlReceive.add(new JLabel("Nhà cung cấp"));pnlReceive.add(cmbSupplier);
            pnlReceive.add(new JLabel("Số lượng nhập"));pnlReceive.add(txtQty);
            pnlReceive.add(new JLabel("Đơn giá nhập"));pnlReceive.add(txtUnitCost);
            pnlReceive.add(new JLabel("Mã phiếu nhập"));pnlReceive.add(txtRef);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),pnlReceive,"Nhập kho",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            int q=Integer.parseInt(txtQty.getText().trim());
            BigDecimal uc=new BigDecimal(txtUnitCost.getText().trim());
            BigDecimal total=uc.multiply(BigDecimal.valueOf(q));
            if(!confirm("Xác nhận nhập "+q+" sản phẩm?\nĐơn giá nhập: "+Formats.money(uc)+"\nTổng giá trị: "+Formats.money(total))) return;
            service.receiveProductStock(productId,Integer.parseInt(prefix(cmbSupplier)),q,txtRef.getText().trim());
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
    private String prefix(JComboBox<String> cmbSource){
        String s=String.valueOf(cmbSource.getSelectedItem());
        int i=s.indexOf(" - ");
        if(i<0) throw new IllegalArgumentException("Vui lòng chọn dữ liệu hợp lệ.");
        return s.substring(0,i);
    }
    private void selectPrefix(JComboBox<String> cmbTarget,String p){
        for(int i=0;i<cmbTarget.getItemCount();i++) if(String.valueOf(cmbTarget.getItemAt(i)).startsWith(p+" -")){cmbTarget.setSelectedIndex(i);return;}
    }
}

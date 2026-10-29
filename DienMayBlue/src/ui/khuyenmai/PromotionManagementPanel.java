package ui.khuyenmai;
import ui.dungchung.DialogUtils;
import service.ProductService;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import util.Formats;
import util.SessionManager;
import ui.dungchung.*;
public class PromotionManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID=1L;
    private final ProductService service = new ProductService();
    private final DefaultTableModel model=UIFactory.readOnlyModel(new Object[]{"Mã KM","Chương trình","Loại","Mức giảm","Bắt đầu","Kết thúc","Trạng thái"});
    private final JTable table=UIFactory.table(model);
    public PromotionManagementPanel(){
        super("Quản lý khuyến mãi","Quản lý chương trình khuyến mãi và phạm vi áp dụng cho sản phẩm.");
        JButton add=UIFactory.primaryButton("+ Tạo khuyến mãi");
        JButton scope=UIFactory.secondaryButton("Xem phạm vi");
        JButton end=UIFactory.secondaryButton("Kết thúc sớm");
        JButton delete=UIFactory.secondaryButton("Xóa khuyến mãi");
        JButton refresh=UIFactory.secondaryButton("Làm mới");
        delete.setEnabled(false);
        actions.add(add);actions.add(scope);actions.add(end);actions.add(delete);actions.add(refresh);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        add.addActionListener(e->create());
        scope.addActionListener(e->viewScope());
        end.addActionListener(e->endEarly());
        delete.addActionListener(e->deleteEndedPromotion());
        refresh.addActionListener(e->refreshData());
        table.getSelectionModel().addListSelectionListener(e->{
            if(e.getValueIsAdjusting())return;
            int vr=table.getSelectedRow();
            if(vr<0){delete.setEnabled(false);return;}
            int r=table.convertRowIndexToModel(vr);
            delete.setEnabled("ENDED".equals(Formats.code(model.getValueAt(r,6))));
        });
        refreshData();
    }
    public void refreshData(){
        try{
            service.update("UPDATE promotions SET status=CASE WHEN end_at<SYSDATETIME() THEN 'ENDED' WHEN start_at<=SYSDATETIME() AND end_at>=SYSDATETIME() AND status<>'CANCELLED' THEN 'ACTIVE' ELSE status END WHERE status IN ('SCHEDULED','ACTIVE')");
            model.setRowCount(0);
            for(Map<String,Object> r:service.query("SELECT * FROM promotions ORDER BY start_at DESC"))model.addRow(new Object[]{
                    r.get("promotion_code"),r.get("promotion_name"),Formats.vi(r.get("discount_type")),r.get("discount_value"),
                    Formats.date(r.get("start_at")),Formats.date(r.get("end_at")),Formats.vi(r.get("status"))});
        }catch(Exception ex){showError(ex);}
    }
    private void create(){
        try{
            JTextField code=UIFactory.field(160),name=UIFactory.field(280),value=UIFactory.field(120),
                    start=UIFactory.field(160),end=UIFactory.field(160);
            JComboBox<String> type=UIFactory.combo("Phần trăm","Số tiền cố định");
            JComboBox<String> scopeType=UIFactory.combo("Theo sản phẩm","Theo danh mục","Toàn bộ sản phẩm");
            JButton chooseScope=UIFactory.secondaryButton("Chọn sản phẩm...");
            final LinkedHashSet<String> products=new LinkedHashSet<String>();
            final LinkedHashSet<Integer> categories=new LinkedHashSet<Integer>();
            code.setText(nextPromotionCode());
            start.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            end.setText(LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            JPanel scopeBox=new JPanel(new BorderLayout(8,0));
            scopeBox.setOpaque(false);
            scopeBox.add(chooseScope,BorderLayout.WEST);
            Runnable updateScopeUi=()->{
                String selected=String.valueOf(scopeType.getSelectedItem());
                if("Theo sản phẩm".equals(selected)){
                    chooseScope.setEnabled(true);
                    chooseScope.setText("Chọn sản phẩm...");
                }else if("Theo danh mục".equals(selected)){
                    chooseScope.setEnabled(true);
                    chooseScope.setText("Chọn danh mục...");
                }else{
                    chooseScope.setEnabled(false);
                    chooseScope.setText("Toàn bộ sản phẩm");
                }
            };
            scopeType.addActionListener(e->updateScopeUi.run());
            chooseScope.addActionListener(e->{
                try{
                    String selected=String.valueOf(scopeType.getSelectedItem());
                    if("Theo sản phẩm".equals(selected)){
                        LinkedHashSet<String> chosen=chooseProductScope(products);
                        if(chosen!=null){
                            products.clear();
                            products.addAll(chosen);
                        }
                    }else if("Theo danh mục".equals(selected)){
                        LinkedHashSet<Integer> chosen=chooseCategoryScope(categories);
                        if(chosen!=null){
                            categories.clear();
                            categories.addAll(chosen);
                        }
                    }
                }catch(Exception ex){showError(ex);}
            });
            updateScopeUi.run();
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.add(new JLabel("Mã chương trình"));p.add(code);
            p.add(new JLabel("Tên chương trình"));p.add(name);
            p.add(new JLabel("Loại giảm"));p.add(type);
            p.add(new JLabel("Giá trị"));p.add(value);
            p.add(new JLabel("Bắt đầu yyyy-MM-dd HH:mm"));p.add(start);
            p.add(new JLabel("Kết thúc"));p.add(end);
            p.add(new JLabel("Phạm vi áp dụng"));p.add(scopeType);
            p.add(new JLabel("Đối tượng áp dụng"));p.add(scopeBox);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),p,"Tạo chương trình khuyến mãi",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            String promotionCode=code.getText().trim().toUpperCase(Locale.ROOT);
            String promotionName=name.getText().trim();
            if(promotionCode.isEmpty() || promotionName.isEmpty())
                throw new IllegalArgumentException("Mã và tên chương trình không được để trống.");
            if(!promotionCode.matches("[A-Z0-9_-]{2,20}"))
                throw new IllegalArgumentException("Mã chương trình chỉ gồm chữ in hoa, số, dấu - hoặc _, tối đa 20 ký tự.");
            Object duplicated=service.scalar("SELECT COUNT(*) FROM promotions WHERE promotion_code=?",promotionCode);
            if(duplicated!=null && ((Number)duplicated).intValue()>0)
                throw new IllegalArgumentException("Mã chương trình đã tồn tại.");
            LocalDateTime s=LocalDateTime.parse(start.getText().trim(),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime e=LocalDateTime.parse(end.getText().trim(),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            if(!s.isBefore(e))throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc.");
            if(!e.isAfter(LocalDateTime.now()))throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai.");
            BigDecimal v=new BigDecimal(value.getText().trim());
            String discountType=Formats.code(type.getSelectedItem());
            if(v.compareTo(BigDecimal.ZERO)<=0)throw new IllegalArgumentException("Mức giảm phải lớn hơn 0.");
            if("PERCENT".equals(discountType) && v.compareTo(new BigDecimal("100"))>=0)
                throw new IllegalArgumentException("Giảm phần trăm phải nhỏ hơn 100%. Hệ thống chưa dùng loại khuyến mãi tặng miễn phí.");
            String scope=String.valueOf(scopeType.getSelectedItem());
            if("Theo sản phẩm".equals(scope)){
                categories.clear();
                if(products.isEmpty())throw new IllegalArgumentException("Hãy chọn ít nhất một sản phẩm áp dụng.");
            }else if("Theo danh mục".equals(scope)){
                products.clear();
                if(categories.isEmpty())throw new IllegalArgumentException("Hãy chọn ít nhất một danh mục áp dụng.");
            }else{
                products.clear();categories.clear();
                for(Map<String,Object> r:service.query("SELECT category_id FROM categories ORDER BY category_id"))
                    categories.add(((Number)r.get("category_id")).intValue());
                if(categories.isEmpty())throw new IllegalArgumentException("Chưa có danh mục sản phẩm để áp dụng khuyến mãi.");
            }
            validateTargets(products,categories);
            java.util.List<Map<String,Object>> affected=loadAffectedProducts(products,categories);
            if(affected.isEmpty())throw new IllegalArgumentException("Không có sản phẩm hợp lệ trong phạm vi đã chọn.");
            for(Map<String,Object> product:affected){
                String pid=String.valueOf(product.get("product_id"));
                int cid=((Number)product.get("category_id")).intValue();
                Map<String,Object> overlap=service.one(
                        "SELECT TOP 1 pr.promotion_code,pr.promotion_name FROM promotions pr "
                      + "LEFT JOIN promotion_products pp ON pp.promotion_id=pr.promotion_id "
                      + "LEFT JOIN promotion_categories pc ON pc.promotion_id=pr.promotion_id "
                      + "WHERE pr.status IN ('SCHEDULED','ACTIVE') AND ? < pr.end_at AND ? > pr.start_at "
                      + "AND (pp.product_id=? OR pc.category_id=?) ORDER BY pr.start_at",s,e,pid,cid);
                if(overlap!=null)
                    throw new IllegalArgumentException("Sản phẩm "+pid+" đã thuộc chương trình "
                            +overlap.get("promotion_code")+" - "+overlap.get("promotion_name")+" trong thời gian bị trùng.");
            }
            DefaultTableModel previewModel=UIFactory.readOnlyModel(new Object[]{"Mã SP","Sản phẩm","Danh mục","Giá gốc","Mức giảm","Giá sau KM"});
            for(Map<String,Object> product:affected){
                BigDecimal sale=(BigDecimal)product.get("sale_price");
                BigDecimal discount="PERCENT".equals(discountType)
                        ? sale.multiply(v).divide(new BigDecimal("100"),2,java.math.RoundingMode.HALF_UP) : v;
                if(discount.compareTo(sale)>=0)
                    throw new IllegalArgumentException("Mức giảm làm giá sản phẩm "+product.get("product_id")+" về 0 hoặc âm. Hãy giảm mức ưu đãi.");
                BigDecimal after=sale.subtract(discount);
                previewModel.addRow(new Object[]{product.get("product_id"),product.get("product_name"),product.get("category_name"),
                        Formats.money(sale),Formats.money(discount),Formats.money(after)});
            }
            JTable preview=UIFactory.table(previewModel);
            JScrollPane previewScroll=UIFactory.scroll(preview);previewScroll.setPreferredSize(new Dimension(760,280));
            JPanel previewPanel=new JPanel(new BorderLayout(0,8));
            previewPanel.add(new JLabel("Xem trước giá sau khuyến mãi cho "+affected.size()+" sản phẩm. Khuyến mãi không được cộng dồn với chương trình khác."),BorderLayout.NORTH);
            previewPanel.add(previewScroll,BorderLayout.CENTER);
            if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),previewPanel,"Xác nhận chương trình",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            final LinkedHashSet<String> fproducts=new LinkedHashSet<String>(products);
            final LinkedHashSet<Integer> fcategories=new LinkedHashSet<Integer>(categories);
            final LocalDateTime fs=s,fe=e;final BigDecimal fv=v;final String fcode=promotionCode,fname=promotionName,ftype=discountType;
            service.transaction(con->{
                for(Map<String,Object> product:affected){
                    String pid=String.valueOf(product.get("product_id"));
                    int cid=((Number)product.get("category_id")).intValue();
                    Map<String,Object> overlap=service.one(con, "SELECT TOP 1 pr.promotion_code,pr.promotion_name FROM promotions pr "
                          + "LEFT JOIN promotion_products pp ON pp.promotion_id=pr.promotion_id "
                          + "LEFT JOIN promotion_categories pc ON pc.promotion_id=pr.promotion_id "
                          + "WHERE pr.status IN ('SCHEDULED','ACTIVE') AND ? < pr.end_at AND ? > pr.start_at "
                          + "AND (pp.product_id=? OR pc.category_id=?) ORDER BY pr.start_at",fs,fe,pid,cid);
                    if(overlap!=null)throw new IllegalArgumentException("Sản phẩm "+pid+" vừa phát sinh khuyến mãi trùng thời gian. Hãy tải lại và kiểm tra.");
                }
                long promotionId=service.insertAndKey(con,"INSERT INTO promotions(promotion_code,promotion_name,discount_type,discount_value,start_at,end_at,status,created_by) "
                        +"VALUES(?,?,?,?,?,?,CASE WHEN ?<=SYSDATETIME() THEN 'ACTIVE' ELSE 'SCHEDULED' END,?)",
                        fcode,fname,ftype,fv,fs,fe,fs,SessionManager.getCurrentAccount().getEmployee().getEmployeeId());
                for(String pid:fproducts)service.update(con,"INSERT INTO promotion_products(promotion_id,product_id) VALUES(?,?)",promotionId,pid);
                for(Integer cid:fcategories)service.update(con,"INSERT INTO promotion_categories(promotion_id,category_id) VALUES(?,?)",promotionId,cid);
                service.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                        SessionManager.getCurrentAccount().getAccountId(),"PROMOTION_CREATED","promotions",String.valueOf(promotionId),
                        "Tạo chương trình "+fcode+"; phạm vi="+affected.size()+" sản phẩm");
                return null;
            });
            refreshData();
            info("Đã tạo chương trình khuyến mãi. Hệ thống sẽ tự áp dụng theo thời gian hiệu lực.");
        }catch(Exception ex){showError(ex);}
    }
    private LinkedHashSet<String> chooseProductScope(Set<String> selectedProductIds) throws Exception{
        final java.util.List<ScopeChoice> choices=new ArrayList<ScopeChoice>();
        for(Map<String,Object> r:service.query(
                "SELECT product_id,product_name FROM products WHERE status<>'DISCONTINUED' ORDER BY product_id")){
            choices.add(new ScopeChoice(String.valueOf(r.get("product_id")), r.get("product_id")+" - "+r.get("product_name")));
        }
        final LinkedHashSet<String> selected=new LinkedHashSet<String>();
        if(selectedProductIds!=null)selected.addAll(selectedProductIds);
        JTextField search=new HintTextField("Nhập mã sản phẩm...");
        search.setPreferredSize(new Dimension(420,38));
        JPanel listPanel=new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel,BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        JScrollPane scroll=new JScrollPane(listPanel);
        scroll.setPreferredSize(new Dimension(540,300));
        final Runnable rebuild=()->{
            listPanel.removeAll();
            String kw=search.getText().trim().toUpperCase(Locale.ROOT);
            int visible=0;
            for(ScopeChoice c:choices){
                String code=String.valueOf(c.value).toUpperCase(Locale.ROOT);
                if(!kw.isEmpty() && !code.contains(kw))continue;
                JCheckBox check=new JCheckBox(c.label);
                check.setBackground(Color.WHITE);
                check.setFont(UIConstants.NORMAL);
                check.setSelected(selected.contains(String.valueOf(c.value)));
                check.addItemListener(ev->{
                    String id=String.valueOf(c.value);
                    if(check.isSelected())selected.add(id);else selected.remove(id);
                });
                listPanel.add(check);
                listPanel.add(Box.createVerticalStrut(5));
                visible++;
            }
            if(visible==0){
                JLabel empty=new JLabel("Không tìm thấy sản phẩm theo mã đã nhập.");
                empty.setForeground(UIConstants.MUTED);
                listPanel.add(empty);
            }
            listPanel.revalidate();
            listPanel.repaint();
        };
        UIFactory.onTextChange(search, rebuild);
        rebuild.run();
        JPanel chooser=new JPanel(new BorderLayout(0,8));
        chooser.add(new JLabel("Có thể chọn nhiều sản phẩm. Nhập mã sản phẩm để lọc nhanh."),BorderLayout.NORTH);
        JPanel center=new JPanel(new BorderLayout(0,8));
        center.add(search,BorderLayout.NORTH);
        center.add(scroll,BorderLayout.CENTER);
        chooser.add(center,BorderLayout.CENTER);
        if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),chooser,"Chọn sản phẩm áp dụng",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return null;
        if(selected.isEmpty()){info("Hãy chọn ít nhất một sản phẩm áp dụng.");return null;}
        return new LinkedHashSet<String>(selected);
    }
    private LinkedHashSet<Integer> chooseCategoryScope(Set<Integer> selectedCategoryIds) throws Exception{
        final java.util.List<ScopeChoice> choices=new ArrayList<ScopeChoice>();
        for(Map<String,Object> r:service.query("SELECT category_id,category_name FROM categories ORDER BY category_name")){
            choices.add(new ScopeChoice(((Number)r.get("category_id")).intValue(),String.valueOf(r.get("category_name"))));
        }
        final LinkedHashSet<Integer> selected=new LinkedHashSet<Integer>();
        if(selectedCategoryIds!=null)selected.addAll(selectedCategoryIds);
        JPanel listPanel=new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel,BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        for(ScopeChoice c:choices){
            Integer id=(Integer)c.value;
            JCheckBox check=new JCheckBox(c.label);
            check.setBackground(Color.WHITE);
            check.setFont(UIConstants.NORMAL);
            check.setSelected(selected.contains(id));
            check.addItemListener(ev->{if(check.isSelected())selected.add(id);else selected.remove(id);});
            listPanel.add(check);
            listPanel.add(Box.createVerticalStrut(5));
        }
        JScrollPane scroll=new JScrollPane(listPanel);
        scroll.setPreferredSize(new Dimension(440,260));
        JPanel chooser=new JPanel(new BorderLayout(0,8));
        chooser.add(new JLabel("Có thể chọn một hoặc nhiều danh mục."),BorderLayout.NORTH);
        chooser.add(scroll,BorderLayout.CENTER);
        if(JOptionPane.showConfirmDialog(DialogUtils.owner(this),chooser,"Chọn danh mục áp dụng",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return null;
        if(selected.isEmpty()){info("Hãy chọn ít nhất một danh mục áp dụng.");return null;}
        return new LinkedHashSet<Integer>(selected);
    }
    private String nextPromotionCode() throws Exception{
        int max=0;
        for(Map<String,Object> r:service.query("SELECT promotion_code FROM promotions")){
            String c=r.get("promotion_code")==null?"":String.valueOf(r.get("promotion_code")).trim().toUpperCase(Locale.ROOT);
            if(c.matches("KM\\d+")){
                try{max=Math.max(max,Integer.parseInt(c.substring(2)));}catch(NumberFormatException ignored){}
            }
        }
        return String.format("KM%03d",max+1);
    }
    private static final class ScopeChoice{
        final Object value;final String label;
        ScopeChoice(Object value,String label){this.value=value;this.label=label;}
        @Override public String toString(){return label;}
    }
    private void validateTargets(Set<String> products,Set<Integer> categories) throws Exception{
        for(String pid:products){
            Map<String,Object> p=service.one("SELECT product_id,status FROM products WHERE product_id=?",pid);
            if(p==null)throw new IllegalArgumentException("Không tồn tại sản phẩm "+pid+".");
            if("DISCONTINUED".equals(String.valueOf(p.get("status"))))
                throw new IllegalArgumentException("Sản phẩm "+pid+" đã ngừng kinh doanh, không thể đưa vào khuyến mãi mới.");
        }
        for(Integer cid:categories){
            Object count=service.scalar("SELECT COUNT(*) FROM categories WHERE category_id=?",cid);
            if(count==null||((Number)count).intValue()==0)throw new IllegalArgumentException("Không tồn tại danh mục mã "+cid+".");
        }
    }
    private java.util.List<Map<String,Object>> loadAffectedProducts(Set<String> products,Set<Integer> categories) throws Exception{
        java.util.List<Map<String,Object>> out=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> r:service.query("SELECT p.product_id,p.product_name,p.category_id,p.sale_price,p.status,c.category_name "
                +"FROM products p JOIN categories c ON c.category_id=p.category_id WHERE p.status<>'DISCONTINUED' ORDER BY p.product_name")){
            String pid=String.valueOf(r.get("product_id"));int cid=((Number)r.get("category_id")).intValue();
            if(products.contains(pid)||categories.contains(cid))out.add(r);
        }
        return out;
    }
    private void viewScope(){
        int vr=table.getSelectedRow();if(vr<0){info("Hãy chọn chương trình cần xem.");return;}
        int row=table.convertRowIndexToModel(vr);String promotionCode=String.valueOf(model.getValueAt(row,0));
        try{
            Object idValue=service.scalar("SELECT promotion_id FROM promotions WHERE promotion_code=?",promotionCode);
            if(idValue==null){info("Chương trình không còn tồn tại. Hãy làm mới danh sách.");refreshData();return;}
            int promotionId=((Number)idValue).intValue();
            DefaultTableModel m=UIFactory.readOnlyModel(new Object[]{"Mã SP","Sản phẩm","Danh mục","Giá bán","Trạng thái"});
            for(Map<String,Object> r:service.query(
                    "SELECT DISTINCT p.product_id,p.product_name,c.category_name,p.sale_price,p.status FROM products p "
                  + "JOIN categories c ON c.category_id=p.category_id "
                  + "WHERE EXISTS(SELECT 1 FROM promotion_products pp WHERE pp.promotion_id=? AND pp.product_id=p.product_id) "
                  + "OR EXISTS(SELECT 1 FROM promotion_categories pc WHERE pc.promotion_id=? AND pc.category_id=p.category_id) "
                  + "ORDER BY p.product_name",promotionId,promotionId))
                m.addRow(new Object[]{r.get("product_id"),r.get("product_name"),r.get("category_name"),Formats.money(r.get("sale_price")),Formats.vi(r.get("status"))});
            JTable t=UIFactory.table(m);JScrollPane sp=UIFactory.scroll(t);sp.setPreferredSize(new Dimension(760,320));
            JOptionPane.showMessageDialog(DialogUtils.owner(this),sp,"Phạm vi áp dụng - "+model.getValueAt(row,0),JOptionPane.PLAIN_MESSAGE);
        }catch(Exception ex){showError(ex);}
    }
    private void endEarly(){
        int vr=table.getSelectedRow();if(vr<0){info("Hãy chọn chương trình.");return;}
        int r=table.convertRowIndexToModel(vr);
        String code=String.valueOf(model.getValueAt(r,0));
        String status=Formats.code(model.getValueAt(r,6));
        if(!"ACTIVE".equals(status) && !"SCHEDULED".equals(status)){info("Chỉ chương trình đang hoạt động hoặc đã lên lịch mới có thể kết thúc sớm.");return;}
        if(!confirm("Kết thúc sớm chương trình "+code+"? Sau thao tác này chương trình sẽ không còn được áp dụng."))return;
        try{
            Object idValue=service.scalar("SELECT promotion_id FROM promotions WHERE promotion_code=?",code);
            if(idValue==null){info("Chương trình không còn tồn tại. Hãy làm mới danh sách.");refreshData();return;}
            int id=((Number)idValue).intValue();
            int updated=service.update("UPDATE promotions SET status='ENDED',ended_early_at=SYSDATETIME(),end_at=CASE WHEN end_at>SYSDATETIME() THEN SYSDATETIME() ELSE end_at END "
                    +"WHERE promotion_id=? AND status IN ('ACTIVE','SCHEDULED')",id);
            if(updated==0){info("Chương trình đã thay đổi trạng thái. Hãy làm mới danh sách.");refreshData();return;}
            service.update("INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    SessionManager.getCurrentAccount().getAccountId(),"PROMOTION_ENDED_EARLY","promotions",String.valueOf(id),"Kết thúc sớm chương trình khuyến mãi: "+code);
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
    private void deleteEndedPromotion(){
        int vr=table.getSelectedRow();if(vr<0){info("Hãy chọn chương trình cần xóa.");return;}
        int r=table.convertRowIndexToModel(vr);
        String code=String.valueOf(model.getValueAt(r,0));
        String name=String.valueOf(model.getValueAt(r,1));
        String status=Formats.code(model.getValueAt(r,6));
        if(!"ENDED".equals(status)){
            info("Chỉ được xóa chương trình đã kết thúc.");
            return;
        }
        if(!confirm("Xóa chương trình "+code+" - "+name+"? Dữ liệu phạm vi áp dụng của chương trình cũng sẽ được xóa."))return;
        try{
            service.transaction(con->{
                Map<String,Object> row=service.one(con,"SELECT promotion_id,status FROM promotions WHERE promotion_code=?",code);
                if(row==null)throw new IllegalStateException("Chương trình không còn tồn tại.");
                if(!"ENDED".equals(String.valueOf(row.get("status"))))
                    throw new IllegalStateException("Chỉ được xóa chương trình đã kết thúc.");
                int id=((Number)row.get("promotion_id")).intValue();
                service.update(con,"DELETE FROM promotion_products WHERE promotion_id=?",id);
                service.update(con,"DELETE FROM promotion_categories WHERE promotion_id=?",id);
                service.update(con,"DELETE FROM promotions WHERE promotion_id=? AND status='ENDED'",id);
                service.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                        SessionManager.getCurrentAccount().getAccountId(),"PROMOTION_DELETED","promotions",String.valueOf(id),"Xóa chương trình khuyến mãi đã kết thúc: "+code+" - "+name);
                return null;
            });
            refreshData();
            info("Đã xóa chương trình "+code+".");
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

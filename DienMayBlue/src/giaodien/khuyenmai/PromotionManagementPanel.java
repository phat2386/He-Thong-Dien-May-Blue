package giaodien.khuyenmai;

import tienich.UiText;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import dichvu.BusinessService;
import tienich.Db;
import tienich.Formats;
import tienich.SessionManager;
import giaodien.dungchung.*;

public class PromotionManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID=1L;
    private final DefaultTableModel model=new DefaultTableModel(
            new Object[]{"Mã","Mã KM","Chương trình","Loại","Mức giảm","Bắt đầu","Kết thúc","Trạng thái"},0){
        private static final long serialVersionUID=1L;public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable table=UIFactory.table(model);
    private final BusinessService service=new BusinessService();

    public PromotionManagementPanel(){
        super("Quản lý khuyến mãi","UC015–UC016, UC023–UC024 • Khuyến mãi, thẻ/hạng thành viên và chính sách tích điểm.");
        JButton add=UIFactory.primaryButton("+ Tạo khuyến mãi");
        JButton scope=UIFactory.secondaryButton("Xem phạm vi");
        JButton member=UIFactory.secondaryButton("Thẻ & hạng thành viên");
        JButton pointPolicy=UIFactory.secondaryButton("Chính sách tích điểm");
        boolean canMembership=SessionManager.hasPermission("MEMBERSHIP_MANAGE");
        boolean canPointPolicy=SessionManager.hasPermission("POINT_MANAGE");
        JButton end=UIFactory.secondaryButton("Kết thúc sớm");
        JButton refresh=UIFactory.secondaryButton("Làm mới");
        actions.add(add);actions.add(scope);
        if(canMembership) actions.add(member);
        if(canPointPolicy) actions.add(pointPolicy);
        actions.add(end);actions.add(refresh);
        body.add(UIFactory.scroll(table),BorderLayout.CENTER);
        add.addActionListener(e->create());
        scope.addActionListener(e->viewScope());
        member.addActionListener(e->openMembershipManagement());
        pointPolicy.addActionListener(e->openPointPolicy());
        end.addActionListener(e->endEarly());
        refresh.addActionListener(e->refreshData());
        refreshData();
    }

    public void refreshData(){
        try{
            Db.update("UPDATE promotions SET status=CASE WHEN end_at<SYSDATETIME() THEN 'ENDED' WHEN start_at<=SYSDATETIME() AND end_at>=SYSDATETIME() AND status<>'CANCELLED' THEN 'ACTIVE' ELSE status END WHERE status IN ('SCHEDULED','ACTIVE')");
            model.setRowCount(0);
            for(Map<String,Object> r:Db.query("SELECT * FROM promotions ORDER BY start_at DESC"))model.addRow(new Object[]{
                    r.get("promotion_id"),r.get("promotion_code"),r.get("promotion_name"),UiText.vi(r.get("discount_type")),r.get("discount_value"),
                    Formats.date(r.get("start_at")),Formats.date(r.get("end_at")),UiText.vi(r.get("status"))});
        }catch(Exception ex){showError(ex);}
    }

    private void create(){
        try{
            JTextField code=UIFactory.field(160),name=UIFactory.field(280),value=UIFactory.field(120),
                    start=UIFactory.field(160),end=UIFactory.field(160),ids=UIFactory.field(300),catIds=UIFactory.field(300);
            JComboBox<String> type=UIFactory.combo("Phần trăm","Số tiền cố định");
            start.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            end.setText(LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.add(new JLabel("Mã chương trình"));p.add(code);
            p.add(new JLabel("Tên chương trình"));p.add(name);
            p.add(new JLabel("Loại giảm"));p.add(type);
            p.add(new JLabel("Giá trị"));p.add(value);
            p.add(new JLabel("Bắt đầu yyyy-MM-dd HH:mm"));p.add(start);
            p.add(new JLabel("Kết thúc"));p.add(end);
            p.add(new JLabel("Mã SP áp dụng (SP001,SP002)"));p.add(ids);
            p.add(new JLabel("Mã danh mục áp dụng (1,2)"));p.add(catIds);
            if(JOptionPane.showConfirmDialog(giaodien.dungchung.DialogUtils.owner(this),p,"Tạo chương trình khuyến mãi",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;

            String promotionCode=code.getText().trim().toUpperCase(Locale.ROOT);
            String promotionName=name.getText().trim();
            if(promotionCode.isEmpty() || promotionName.isEmpty())
                throw new IllegalArgumentException("Mã và tên chương trình không được để trống.");
            if(!promotionCode.matches("[A-Z0-9_-]{2,20}"))
                throw new IllegalArgumentException("Mã chương trình chỉ gồm chữ in hoa, số, dấu - hoặc _, tối đa 20 ký tự.");
            Object duplicated=Db.scalar("SELECT COUNT(*) FROM promotions WHERE promotion_code=?",promotionCode);
            if(duplicated!=null && ((Number)duplicated).intValue()>0)
                throw new IllegalArgumentException("Mã chương trình đã tồn tại.");

            LocalDateTime s=LocalDateTime.parse(start.getText().trim(),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime e=LocalDateTime.parse(end.getText().trim(),DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            if(!s.isBefore(e))throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc.");
            if(!e.isAfter(LocalDateTime.now()))throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai.");

            BigDecimal v=new BigDecimal(value.getText().trim());
            String discountType=UiText.code(type.getSelectedItem());
            if(v.compareTo(BigDecimal.ZERO)<=0)throw new IllegalArgumentException("Mức giảm phải lớn hơn 0.");
            if("PERCENT".equals(discountType) && v.compareTo(new BigDecimal("100"))>=0)
                throw new IllegalArgumentException("Giảm phần trăm phải nhỏ hơn 100%. Hệ thống chưa dùng loại khuyến mãi tặng miễn phí.");

            LinkedHashSet<String> products=parseProductIds(ids.getText());
            LinkedHashSet<Integer> categories=parseCategoryIds(catIds.getText());
            if(products.isEmpty() && categories.isEmpty())
                throw new IllegalArgumentException("Phải chọn ít nhất một sản phẩm hoặc một danh mục áp dụng.");

            validateTargets(products,categories);
            java.util.List<Map<String,Object>> affected=loadAffectedProducts(products,categories);
            if(affected.isEmpty())throw new IllegalArgumentException("Không có sản phẩm hợp lệ trong phạm vi đã chọn.");

            for(Map<String,Object> product:affected){
                String pid=String.valueOf(product.get("product_id"));
                int cid=((Number)product.get("category_id")).intValue();
                Map<String,Object> overlap=Db.one(
                        "SELECT TOP 1 pr.promotion_code,pr.promotion_name FROM promotions pr "
                      + "LEFT JOIN promotion_products pp ON pp.promotion_id=pr.promotion_id "
                      + "LEFT JOIN promotion_categories pc ON pc.promotion_id=pr.promotion_id "
                      + "WHERE pr.status IN ('SCHEDULED','ACTIVE') AND ? < pr.end_at AND ? > pr.start_at "
                      + "AND (pp.product_id=? OR pc.category_id=?) ORDER BY pr.start_at",s,e,pid,cid);
                if(overlap!=null)
                    throw new IllegalArgumentException("Sản phẩm "+pid+" đã thuộc chương trình "
                            +overlap.get("promotion_code")+" - "+overlap.get("promotion_name")+" trong thời gian bị trùng.");
            }

            DefaultTableModel previewModel=new DefaultTableModel(new Object[]{"Mã SP","Sản phẩm","Danh mục","Giá gốc","Mức giảm","Giá sau KM"},0){
                private static final long serialVersionUID=1L;public boolean isCellEditable(int r,int c){return false;}
            };
            for(Map<String,Object> product:affected){
                BigDecimal sale=(BigDecimal)product.get("sale_price");
                BigDecimal discount="PERCENT".equals(discountType)
                        ? sale.multiply(v).divide(new BigDecimal("100"),2,java.math.RoundingMode.HALF_UP)
                        : v;
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
            if(JOptionPane.showConfirmDialog(giaodien.dungchung.DialogUtils.owner(this),previewPanel,"Xác nhận chương trình",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;

            final LinkedHashSet<String> fproducts=products;final LinkedHashSet<Integer> fcategories=categories;
            final LocalDateTime fs=s,fe=e;final BigDecimal fv=v;final String fcode=promotionCode,fname=promotionName,ftype=discountType;
            Db.transaction(con->{
                // Kiểm tra lại ngay trước khi lưu để tránh dữ liệu đã thay đổi trong lúc Manager xem preview.
                for(Map<String,Object> product:affected){
                    String pid=String.valueOf(product.get("product_id"));
                    int cid=((Number)product.get("category_id")).intValue();
                    Map<String,Object> overlap=Db.one(con,
                            "SELECT TOP 1 pr.promotion_code,pr.promotion_name FROM promotions pr "
                          + "LEFT JOIN promotion_products pp ON pp.promotion_id=pr.promotion_id "
                          + "LEFT JOIN promotion_categories pc ON pc.promotion_id=pr.promotion_id "
                          + "WHERE pr.status IN ('SCHEDULED','ACTIVE') AND ? < pr.end_at AND ? > pr.start_at "
                          + "AND (pp.product_id=? OR pc.category_id=?) ORDER BY pr.start_at",fs,fe,pid,cid);
                    if(overlap!=null)throw new IllegalArgumentException("Sản phẩm "+pid+" vừa phát sinh khuyến mãi trùng thời gian. Hãy tải lại và kiểm tra.");
                }
                long promotionId=Db.insertAndKey(con,"INSERT INTO promotions(promotion_code,promotion_name,discount_type,discount_value,start_at,end_at,status,created_by) "
                        +"VALUES(?,?,?,?,?,?,CASE WHEN ?<=SYSDATETIME() THEN 'ACTIVE' ELSE 'SCHEDULED' END,?)",
                        fcode,fname,ftype,fv,fs,fe,fs,SessionManager.getCurrentAccount().getEmployee().getEmployeeId());
                for(String pid:fproducts)Db.update(con,"INSERT INTO promotion_products(promotion_id,product_id) VALUES(?,?)",promotionId,pid);
                for(Integer cid:fcategories)Db.update(con,"INSERT INTO promotion_categories(promotion_id,category_id) VALUES(?,?)",promotionId,cid);
                Db.update(con,"INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                        SessionManager.getCurrentAccount().getAccountId(),"PROMOTION_CREATED","promotions",String.valueOf(promotionId),
                        "Tạo chương trình "+fcode+"; phạm vi="+affected.size()+" sản phẩm");
                return null;
            });
            refreshData();
            info("Đã tạo chương trình khuyến mãi. Hệ thống sẽ tự áp dụng theo thời gian hiệu lực.");
        }catch(Exception ex){showError(ex);}
    }

    private LinkedHashSet<String> parseProductIds(String text){
        LinkedHashSet<String> out=new LinkedHashSet<String>();
        if(text==null||text.trim().isEmpty())return out;
        for(String s:text.split(",")){String id=s.trim().toUpperCase(Locale.ROOT);if(!id.isEmpty())out.add(id);}
        return out;
    }

    private LinkedHashSet<Integer> parseCategoryIds(String text){
        LinkedHashSet<Integer> out=new LinkedHashSet<Integer>();
        if(text==null||text.trim().isEmpty())return out;
        for(String s:text.split(",")){String id=s.trim();if(id.isEmpty())continue;try{out.add(Integer.valueOf(id));}catch(NumberFormatException ex){throw new IllegalArgumentException("Mã danh mục phải là số nguyên: "+id);}}
        return out;
    }

    private void validateTargets(Set<String> products,Set<Integer> categories) throws Exception{
        for(String pid:products){
            Map<String,Object> p=Db.one("SELECT product_id,status FROM products WHERE product_id=?",pid);
            if(p==null)throw new IllegalArgumentException("Không tồn tại sản phẩm "+pid+".");
            if("DISCONTINUED".equals(String.valueOf(p.get("status"))))
                throw new IllegalArgumentException("Sản phẩm "+pid+" đã ngừng kinh doanh, không thể đưa vào khuyến mãi mới.");
        }
        for(Integer cid:categories){
            Object count=Db.scalar("SELECT COUNT(*) FROM categories WHERE category_id=?",cid);
            if(count==null||((Number)count).intValue()==0)throw new IllegalArgumentException("Không tồn tại danh mục mã "+cid+".");
        }
    }

    private java.util.List<Map<String,Object>> loadAffectedProducts(Set<String> products,Set<Integer> categories) throws Exception{
        java.util.List<Map<String,Object>> out=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> r:Db.query("SELECT p.product_id,p.product_name,p.category_id,p.sale_price,p.status,c.category_name "
                +"FROM products p JOIN categories c ON c.category_id=p.category_id WHERE p.status<>'DISCONTINUED' ORDER BY p.product_name")){
            String pid=String.valueOf(r.get("product_id"));int cid=((Number)r.get("category_id")).intValue();
            if(products.contains(pid)||categories.contains(cid))out.add(r);
        }
        return out;
    }

    private void viewScope(){
        int vr=table.getSelectedRow();if(vr<0){info("Hãy chọn chương trình cần xem.");return;}
        int row=table.convertRowIndexToModel(vr);int promotionId=((Number)model.getValueAt(row,0)).intValue();
        try{
            DefaultTableModel m=new DefaultTableModel(new Object[]{"Mã SP","Sản phẩm","Danh mục","Giá bán","Trạng thái"},0){
                private static final long serialVersionUID=1L;public boolean isCellEditable(int r,int c){return false;}
            };
            for(Map<String,Object> r:Db.query(
                    "SELECT DISTINCT p.product_id,p.product_name,c.category_name,p.sale_price,p.status FROM products p "
                  + "JOIN categories c ON c.category_id=p.category_id "
                  + "WHERE EXISTS(SELECT 1 FROM promotion_products pp WHERE pp.promotion_id=? AND pp.product_id=p.product_id) "
                  + "OR EXISTS(SELECT 1 FROM promotion_categories pc WHERE pc.promotion_id=? AND pc.category_id=p.category_id) "
                  + "ORDER BY p.product_name",promotionId,promotionId))
                m.addRow(new Object[]{r.get("product_id"),r.get("product_name"),r.get("category_name"),Formats.money(r.get("sale_price")),UiText.vi(r.get("status"))});
            JTable t=UIFactory.table(m);JScrollPane sp=UIFactory.scroll(t);sp.setPreferredSize(new Dimension(760,320));
            JOptionPane.showMessageDialog(giaodien.dungchung.DialogUtils.owner(this),sp,"Phạm vi áp dụng - "+model.getValueAt(row,1),JOptionPane.PLAIN_MESSAGE);
        }catch(Exception ex){showError(ex);}
    }

    /** UC023 - thao tác thành viên nằm trong nhóm Quản lý khuyến mãi của Manager. */
    private void openMembershipManagement(){
        final JTextField search=UIFactory.field(260);
        final DefaultTableModel m=new DefaultTableModel(new Object[]{"Mã KH","Họ tên","SĐT","Hạng","Điểm","Thẻ đang dùng"},0){
            private static final long serialVersionUID=1L;public boolean isCellEditable(int r,int c){return false;}
        };
        final JTable t=UIFactory.table(m);
        final Runnable reload=()->{
            try{
                String kw="%"+search.getText().trim()+"%";
                m.setRowCount(0);
                for(Map<String,Object> r:Db.query(
                        "SELECT c.customer_id,c.full_name,c.phone,mt.tier_name,c.current_points,"
                      + "(SELECT TOP 1 mc.card_code FROM membership_cards mc WHERE mc.customer_id=c.customer_id AND mc.status='ACTIVE' ORDER BY mc.issued_at DESC) card_code "
                      + "FROM customers c LEFT JOIN membership_tiers mt ON mt.tier_id=c.tier_id "
                      + "WHERE c.status='ACTIVE' AND (c.customer_id LIKE ? OR c.full_name LIKE ? OR ISNULL(c.phone,'') LIKE ?) ORDER BY c.full_name",
                        kw,kw,kw)){
                    m.addRow(new Object[]{r.get("customer_id"),r.get("full_name"),r.get("phone"),r.get("tier_name"),r.get("current_points"),r.get("card_code")});
                }
            }catch(Exception ex){showError(ex);}
        };
        search.addActionListener(e->reload.run());
        reload.run();

        JButton reissue=UIFactory.primaryButton("Cấp / cấp lại thẻ");
        JButton sync=UIFactory.secondaryButton("Đồng bộ hạng theo điểm");
        JButton refresh=UIFactory.secondaryButton("Làm mới");
        JPanel top=UIFactory.toolbar(new JLabel("Tìm khách hàng"),search,refresh);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));buttons.add(sync);buttons.add(reissue);
        JPanel panel=new JPanel(new BorderLayout(0,10));panel.setPreferredSize(new Dimension(850,430));
        panel.add(top,BorderLayout.NORTH);panel.add(UIFactory.scroll(t),BorderLayout.CENTER);panel.add(buttons,BorderLayout.SOUTH);

        refresh.addActionListener(e->reload.run());
        reissue.addActionListener(e->{
            int vr=t.getSelectedRow();if(vr<0){info("Hãy chọn khách hàng.");return;}
            int rr=t.convertRowIndexToModel(vr);String customerId=String.valueOf(m.getValueAt(rr,0));
            String oldCard=Formats.safe(m.getValueAt(rr,5));
            String question=oldCard.isEmpty()?"Cấp thẻ thành viên cho "+customerId+"?":"Vô hiệu hóa thẻ "+oldCard+" và cấp thẻ mới cho "+customerId+"?";
            if(!confirm(question))return;
            try{String card=service.reissueMembershipCard(customerId);reload.run();info("Thẻ đang hoạt động: "+card);}catch(Exception ex){showError(ex);}
        });
        sync.addActionListener(e->{
            int vr=t.getSelectedRow();if(vr<0){info("Hãy chọn khách hàng.");return;}
            int rr=t.convertRowIndexToModel(vr);String customerId=String.valueOf(m.getValueAt(rr,0));
            try{String tierName=service.syncCustomerTier(customerId);reload.run();info("Hạng phù hợp theo điểm hiện tại: "+tierName);}catch(Exception ex){showError(ex);}
        });

        JOptionPane.showMessageDialog(giaodien.dungchung.DialogUtils.owner(this),panel,"UC023 - Quản lý thẻ & hạng thành viên",JOptionPane.PLAIN_MESSAGE);
    }

    /** UC024 - Manager quản lý chính sách; điểm khách hàng vẫn do hệ thống tự cộng/trừ. */
    private void openPointPolicy(){
        final DefaultTableModel m=new DefaultTableModel(new Object[]{"ID","Mã hạng","Tên hạng","Mốc điểm","Ưu đãi (%)","Hệ số tích điểm"},0){
            private static final long serialVersionUID=1L;public boolean isCellEditable(int r,int c){return false;}
        };
        final JTable t=UIFactory.table(m);
        final Runnable reload=()->{
            try{
                m.setRowCount(0);
                for(Map<String,Object> r:Db.query("SELECT tier_id,tier_code,tier_name,min_points,discount_rate,point_multiplier FROM membership_tiers ORDER BY min_points"))
                    m.addRow(new Object[]{r.get("tier_id"),r.get("tier_code"),r.get("tier_name"),r.get("min_points"),r.get("discount_rate"),r.get("point_multiplier")});
            }catch(Exception ex){showError(ex);}
        };
        reload.run();

        JLabel note=new JLabel("UC019: 1 điểm / 10.000đ × hệ số hạng. Ưu đãi hạng không cộng dồn với khuyến mãi; hệ thống lấy mức giảm có lợi hơn.");
        note.setForeground(UIConstants.MUTED);
        JButton edit=UIFactory.primaryButton("Sửa chính sách hạng");
        JPanel panel=new JPanel(new BorderLayout(0,10));panel.setPreferredSize(new Dimension(820,390));
        panel.add(note,BorderLayout.NORTH);panel.add(UIFactory.scroll(t),BorderLayout.CENTER);
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT));bottom.add(edit);panel.add(bottom,BorderLayout.SOUTH);

        edit.addActionListener(e->{
            int vr=t.getSelectedRow();if(vr<0){info("Hãy chọn hạng cần sửa.");return;}
            int rr=t.convertRowIndexToModel(vr);
            int tierId=((Number)m.getValueAt(rr,0)).intValue();
            String tierName=String.valueOf(m.getValueAt(rr,2));
            JTextField min=UIFactory.field(120),discount=UIFactory.field(120),multiplier=UIFactory.field(120);
            min.setText(String.valueOf(m.getValueAt(rr,3)));discount.setText(String.valueOf(m.getValueAt(rr,4)));multiplier.setText(String.valueOf(m.getValueAt(rr,5)));
            JPanel f=new JPanel(new GridLayout(0,2,8,8));
            f.add(new JLabel("Hạng"));f.add(new JLabel(tierName));
            f.add(new JLabel("Mốc điểm tối thiểu"));f.add(min);
            f.add(new JLabel("Ưu đãi hạng (%)"));f.add(discount);
            f.add(new JLabel("Hệ số tích điểm"));f.add(multiplier);
            if(JOptionPane.showConfirmDialog(giaodien.dungchung.DialogUtils.owner(panel),f,"Cập nhật chính sách - "+tierName,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
            try{
                service.updateMembershipTierPolicy(tierId,Integer.parseInt(min.getText().trim()),new BigDecimal(discount.getText().trim()),new BigDecimal(multiplier.getText().trim()));
                reload.run();info("Đã cập nhật chính sách và đồng bộ lại hạng khách hàng theo điểm hiện tại.");
            }catch(Exception ex){showError(ex);}
        });

        JOptionPane.showMessageDialog(giaodien.dungchung.DialogUtils.owner(this),panel,"UC024 - Chính sách tích điểm",JOptionPane.PLAIN_MESSAGE);
    }

    private void endEarly(){
        int vr=table.getSelectedRow();if(vr<0){info("Hãy chọn chương trình.");return;}
        int r=table.convertRowIndexToModel(vr);int id=((Number)model.getValueAt(r,0)).intValue();
        String status=UiText.code(model.getValueAt(r,7));
        if(!"ACTIVE".equals(status) && !"SCHEDULED".equals(status)){info("Chỉ chương trình đang hoạt động hoặc đã lên lịch mới có thể kết thúc sớm.");return;}
        if(!confirm("Kết thúc sớm chương trình đang chọn? Sau thao tác này chương trình sẽ không còn được áp dụng."))return;
        try{
            int updated=Db.update("UPDATE promotions SET status='ENDED',ended_early_at=SYSDATETIME(),end_at=CASE WHEN end_at>SYSDATETIME() THEN SYSDATETIME() ELSE end_at END "
                    +"WHERE promotion_id=? AND status IN ('ACTIVE','SCHEDULED')",id);
            if(updated==0){info("Chương trình đã thay đổi trạng thái. Hãy làm mới danh sách.");return;}
            Db.update("INSERT INTO audit_logs(account_id,action_code,entity_name,entity_id,detail) VALUES(?,?,?,?,?)",
                    SessionManager.getCurrentAccount().getAccountId(),"PROMOTION_ENDED_EARLY","promotions",String.valueOf(id),"Kết thúc sớm chương trình khuyến mãi");
            refreshData();
        }catch(Exception ex){showError(ex);}
    }
}

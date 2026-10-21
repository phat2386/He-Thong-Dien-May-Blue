package app;

import bus.BusinessService;
import bus.Formats;
import bus.PhienDangNhap;
import dao.Db;

import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


public class PanelStatistics extends PanelBaseScreen {
    private static final long serialVersionUID=1L;

    private final JTextField from=UIFactory.field(110),to=UIFactory.field(110);
    private final JComboBox<String> customerRank=UIFactory.combo("Theo tổng giá trị","Theo số lần mua");
    private final JComboBox<String> revenuePeriod=UIFactory.combo("Theo ngày","Theo tháng","Theo năm");
    private final JTabbedPane tabs=new JTabbedPane();

    private final DefaultTableModel topCustomerModel=model("Mã KH","Khách hàng","SĐT","Số lần mua","Chi tiêu ròng");
    private final DefaultTableModel revenueModel=model("Kỳ","Doanh thu ròng","Lợi nhuận","Số đơn ròng");
    private final DefaultTableModel productModel=model("Sản phẩm","SL bán ròng","Doanh thu ròng","Xếp hạng");
    private final DefaultTableModel bonusModel=model("Nhân viên","Doanh số gốc","Điều chỉnh hoàn","Doanh số tính thưởng","Tiền thưởng");

    private final JTable topCustomerTable=UIFactory.table(topCustomerModel);
    private final JTable revenueTable=UIFactory.table(revenueModel);
    private final JTable productTable=UIFactory.table(productModel);
    private final JTable bonusTable=UIFactory.table(bonusModel);
    private final RevenueChart revenueChart=new RevenueChart();
    private final BusinessService service=new BusinessService();
    private final boolean canFinalizeBonus=PhienDangNhap.coQuyen("BONUS_FINALIZE");

    public PanelStatistics(){
        super("Thống kê kinh doanh","Số liệu ròng đã khấu trừ các phiếu hoàn tiền hoàn tất.");
        from.setText(LocalDate.now().withDayOfMonth(1).toString());
        to.setText(LocalDate.now().toString());

        JButton apply=UIFactory.primaryButton("Áp dụng");
        JButton excel=UIFactory.secondaryButton("Xuất bảng dữ liệu");
        JButton finalize=UIFactory.secondaryButton("Chốt thưởng");
        actions.add(excel);
        if(canFinalizeBonus) actions.add(finalize);

        JPanel filters=UIFactory.toolbar(new JLabel("Từ ngày"),from,new JLabel("Đến ngày"),to,
                new JLabel("Xếp KH"),customerRank,new JLabel("Nhóm doanh thu"),revenuePeriod,apply);

        tabs.addTab("Khách hàng mua nhiều",UIFactory.scroll(topCustomerTable));

        JPanel revenuePane=new JPanel(new BorderLayout(0,10));
        revenuePane.setOpaque(false);
        revenueChart.setPreferredSize(new Dimension(100,180));
        revenuePane.add(revenueChart,BorderLayout.NORTH);
        revenuePane.add(UIFactory.scroll(revenueTable),BorderLayout.CENTER);
        tabs.addTab("Doanh thu / lợi nhuận",revenuePane);

        tabs.addTab("Sản phẩm bán chạy / bán chậm",UIFactory.scroll(productTable));
        if(canFinalizeBonus) tabs.addTab("Thưởng doanh số",UIFactory.scroll(bonusTable));
        tabs.setFont(UIConstants.NORMAL);
        styleStatisticTabs();
        tabs.addChangeListener(e -> updateStatisticTabStyles());

        body.add(filters,BorderLayout.NORTH);
        body.add(tabs,BorderLayout.CENTER);

        apply.addActionListener(e->refreshData());
        customerRank.addActionListener(e->refreshData());
        revenuePeriod.addActionListener(e->refreshData());
        excel.addActionListener(e->UIFactory.exportCsv(this,currentTable(),"thong_ke.csv"));
        finalize.addActionListener(e->finalizeBonus());
        refreshData();
    }


    private void styleStatisticTabs(){
        tabs.setOpaque(false);
        tabs.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        for(int i=0;i<tabs.getTabCount();i++){
            JLabel label=new JLabel(tabs.getTitleAt(i),SwingConstants.CENTER);
            label.setOpaque(true);
            label.setFont(UIConstants.font(Font.BOLD,13));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIConstants.BORDER),
                    BorderFactory.createEmptyBorder(9,14,9,14)));
            tabs.setTabComponentAt(i,label);
        }
        updateStatisticTabStyles();
    }

    private void updateStatisticTabStyles(){
        int selected=tabs.getSelectedIndex();
        for(int i=0;i<tabs.getTabCount();i++){
            Component c=tabs.getTabComponentAt(i);
            if(!(c instanceof JLabel)) continue;
            JLabel label=(JLabel)c;
            boolean active=i==selected;
            label.setBackground(active?UIConstants.ORANGE:UIConstants.WHITE);
            label.setForeground(active?UIConstants.WHITE:UIConstants.TEXT);
            label.setFont(UIConstants.font(Font.BOLD,active?14:13));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(active?UIConstants.ORANGE:UIConstants.BORDER),
                    BorderFactory.createEmptyBorder(active?9:8,14,active?9:8,14)));
        }
    }

    private DefaultTableModel model(String... cols){
        return new DefaultTableModel(cols,0){
            private static final long serialVersionUID=1L;
            public boolean isCellEditable(int r,int c){return false;}
        };
    }

    private JTable currentTable(){
        switch(tabs.getSelectedIndex()){
            case 0:return topCustomerTable;
            case 1:return revenueTable;
            case 2:return productTable;
            default:return bonusTable;
        }
    }

    private LocalDate f(){return LocalDate.parse(from.getText().trim());}
    private LocalDate t(){return LocalDate.parse(to.getText().trim());}

    @Override public void refreshData(){
        try{
            LocalDate f=f(),t=t();
            if(f.isAfter(t)) throw new IllegalArgumentException("Từ ngày phải nhỏ hơn hoặc bằng đến ngày.");
            loadCustomers(f,t);
            loadRevenue(f,t);
            loadProducts(f,t);
            if(canFinalizeBonus) loadBonus(f,t);
        }catch(Exception ex){showError(ex);}
    }

    private void loadCustomers(LocalDate f,LocalDate t) throws Exception {
        topCustomerModel.setRowCount(0);
        String order="Theo số lần mua".equals(customerRank.getSelectedItem()) ? "purchase_count DESC, spent DESC" : "spent DESC, purchase_count DESC";
        String sql=
                "WITH rf AS ("
              + " SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests WHERE status='COMPLETED' GROUP BY invoice_id"
              + "), ni AS ("
              + " SELECT i.invoice_id,i.customer_id,CASE WHEN i.total_amount-ISNULL(rf.refunded,0)>0 "
              + " THEN i.total_amount-ISNULL(rf.refunded,0) ELSE 0 END net_amount "
              + " FROM invoices i LEFT JOIN rf ON rf.invoice_id=i.invoice_id "
              + " WHERE CAST(i.created_at AS DATE) BETWEEN ? AND ? AND i.status IN ('PAID','PARTIAL_REFUND','REFUNDED')"
              + ") "
              + "SELECT c.customer_id,c.full_name,c.phone,"
              + "SUM(CASE WHEN ni.net_amount>0 THEN 1 ELSE 0 END) purchase_count,SUM(ni.net_amount) spent "
              + "FROM customers c JOIN ni ON ni.customer_id=c.customer_id "
              + "GROUP BY c.customer_id,c.full_name,c.phone HAVING SUM(ni.net_amount)>0 "
              + "ORDER BY "+order;
        for(Map<String,Object> r:Db.query(sql,f,t))
            topCustomerModel.addRow(new Object[]{r.get("customer_id"),r.get("full_name"),r.get("phone"),
                    r.get("purchase_count"),Formats.money(r.get("spent"))});
    }

    private void loadRevenue(LocalDate f,LocalDate t) throws Exception {
        revenueModel.setRowCount(0);
        String periodExpr;
        Object p=revenuePeriod.getSelectedItem();
        if("Theo tháng".equals(p)) periodExpr="CONVERT(char(7),created_at,120)";
        else if("Theo năm".equals(p)) periodExpr="CONVERT(char(4),YEAR(created_at))";
        else periodExpr="CONVERT(char(10),CAST(created_at AS DATE),23)";

        String sql=
                "WITH ret AS ("
              + " SELECT rd.invoice_detail_id,SUM(rd.quantity) returned_qty,SUM(rd.refund_amount) refunded "
              + " FROM return_details rd JOIN return_requests rr ON rr.return_id=rd.return_id "
              + " WHERE rr.status='COMPLETED' GROUP BY rd.invoice_detail_id"
              + "), inv AS ("
              + " SELECT i.invoice_id,i.created_at,"
              + " SUM(CASE WHEN d.line_total-ISNULL(ret.refunded,0)>0 THEN d.line_total-ISNULL(ret.refunded,0) ELSE 0 END) revenue,"
              + " SUM(CASE WHEN d.quantity-ISNULL(ret.returned_qty,0)>0 THEN (d.quantity-ISNULL(ret.returned_qty,0))*d.cost_price_snapshot ELSE 0 END) cost "
              + " FROM invoices i JOIN invoice_details d ON d.invoice_id=i.invoice_id "
              + " LEFT JOIN ret ON ret.invoice_detail_id=d.invoice_detail_id "
              + " WHERE CAST(i.created_at AS DATE) BETWEEN ? AND ? AND i.status IN ('PAID','PARTIAL_REFUND','REFUNDED') "
              + " GROUP BY i.invoice_id,i.created_at"
              + ") "
              + "SELECT "+periodExpr+" period,SUM(revenue) revenue,SUM(revenue-cost) profit,"
              + "SUM(CASE WHEN revenue>0 THEN 1 ELSE 0 END) orders "
              + "FROM inv GROUP BY "+periodExpr+" ORDER BY MIN(created_at)";
        java.util.List<Map<String,Object>> rows=Db.query(sql,f,t);
        for(Map<String,Object> r:rows)
            revenueModel.addRow(new Object[]{r.get("period"),Formats.money(r.get("revenue")),Formats.money(r.get("profit")),r.get("orders")});
        revenueChart.setData(rows);
    }

    private void loadProducts(LocalDate f,LocalDate t) throws Exception {
        productModel.setRowCount(0);
        String sql=
                "WITH ret AS ("
              + " SELECT rd.invoice_detail_id,SUM(rd.quantity) returned_qty,SUM(rd.refund_amount) refunded "
              + " FROM return_details rd JOIN return_requests rr ON rr.return_id=rd.return_id "
              + " WHERE rr.status='COMPLETED' GROUP BY rd.invoice_detail_id"
              + ") "
              + "SELECT p.product_name,"
              + "ISNULL(SUM(CASE WHEN i.invoice_id IS NULL THEN 0 WHEN d.quantity-ISNULL(ret.returned_qty,0)>0 THEN d.quantity-ISNULL(ret.returned_qty,0) ELSE 0 END),0) qty,"
              + "ISNULL(SUM(CASE WHEN i.invoice_id IS NULL THEN 0 WHEN d.line_total-ISNULL(ret.refunded,0)>0 THEN d.line_total-ISNULL(ret.refunded,0) ELSE 0 END),0) revenue "
              + "FROM products p "
              + "LEFT JOIN invoice_details d ON d.product_id=p.product_id "
              + "LEFT JOIN invoices i ON i.invoice_id=d.invoice_id AND CAST(i.created_at AS DATE) BETWEEN ? AND ? "
              + " AND i.status IN ('PAID','PARTIAL_REFUND','REFUNDED') "
              + "LEFT JOIN ret ON ret.invoice_detail_id=d.invoice_detail_id "
              + "GROUP BY p.product_id,p.product_name ORDER BY qty DESC,revenue DESC,p.product_name";
        java.util.List<Map<String,Object>> rows=Db.query(sql,f,t);
        int n=rows.size();
        int group=Math.min(5,Math.max(1,n/2));
        for(int i=0;i<n;i++){
            Map<String,Object> r=rows.get(i);
            String rank=i<group?"Bán chạy":(i>=n-group?"Bán chậm":"");
            productModel.addRow(new Object[]{r.get("product_name"),r.get("qty"),Formats.money(r.get("revenue")),rank});
        }
    }

    private void loadBonus(LocalDate f,LocalDate t) throws Exception {
        bonusModel.setRowCount(0);
        String sql=
                "SELECT e.full_name,"
              + "SUM(CASE WHEN sb.sales_amount>0 THEN sb.sales_amount ELSE 0 END) gross_sales,"
              + "SUM(CASE WHEN sb.sales_amount<0 THEN sb.sales_amount ELSE 0 END) adjustments,"
              + "SUM(sb.sales_amount) net_sales,SUM(sb.bonus_amount) bonus "
              + "FROM sales_bonuses sb JOIN employees e ON e.employee_id=sb.employee_id "
              + "WHERE CAST(sb.created_at AS DATE) BETWEEN ? AND ? AND sb.status='OPEN' "
              + "GROUP BY e.employee_id,e.full_name ORDER BY net_sales DESC";
        for(Map<String,Object> r:Db.query(sql,f,t))
            bonusModel.addRow(new Object[]{r.get("full_name"),Formats.money(r.get("gross_sales")),
                    Formats.money(r.get("adjustments")),Formats.money(r.get("net_sales")),Formats.money(r.get("bonus"))});
    }

    private void finalizeBonus(){
        if(!canFinalizeBonus){info("Tài khoản chưa được Admin cấp quyền chốt bảng kê thưởng.");return;}
        if(!confirm("Chốt bảng kê thưởng từ "+from.getText()+" đến "+to.getText()+"?\nDữ liệu đã chốt sẽ được khóa theo kỳ."))return;
        try{
            service.finalizeBonusStatement(f(),t());
            refreshData();
            info("Đã chốt bảng kê thưởng doanh số.");
        }catch(Exception ex){showError(ex);}
    }

    private static class RevenueChart extends JPanel {
        private static final long serialVersionUID=1L;
        private java.util.List<String> labels=Collections.emptyList();
        private java.util.List<BigDecimal> values=Collections.emptyList();

        RevenueChart(){setOpaque(true);setBackground(Color.WHITE);setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));}

        void setData(java.util.List<Map<String,Object>> rows){
            java.util.List<String> l=new ArrayList<String>();
            java.util.List<BigDecimal> v=new ArrayList<BigDecimal>();
            for(Map<String,Object> r:rows){
                l.add(String.valueOf(r.get("period")));
                Object x=r.get("revenue");
                v.add(x==null?BigDecimal.ZERO:new BigDecimal(String.valueOf(x)));
            }
            labels=l;values=v;repaint();
        }

        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),left=45,right=18,top=18,bottom=30;
            g2.setColor(UIConstants.MUTED);g2.setFont(UIConstants.font(Font.PLAIN,10));
            g2.drawString("Doanh thu ròng",12,14);
            if(values.isEmpty()){g2.drawString("Chưa có dữ liệu trong khoảng thời gian đã chọn.",left,Math.max(50,h/2));g2.dispose();return;}
            BigDecimal max=BigDecimal.ZERO;for(BigDecimal v:values)if(v.compareTo(max)>0)max=v;
            if(max.signum()==0)max=BigDecimal.ONE;
            int plotW=Math.max(1,w-left-right),plotH=Math.max(1,h-top-bottom);
            int count=values.size(),gap=6,barW=Math.max(4,(plotW-gap*(count+1))/Math.max(1,count));
            for(int i=0;i<count;i++){
                double ratio=values.get(i).divide(max,6,java.math.RoundingMode.HALF_UP).doubleValue();
                int bh=(int)Math.round(plotH*ratio);
                int x=left+gap+i*(barW+gap),y=top+plotH-bh;
                g2.setColor(UIConstants.ORANGE);g2.fillRoundRect(x,y,barW,bh,6,6);
                if(count<=12){
                    g2.setColor(UIConstants.MUTED);
                    String s=labels.get(i);if(s.length()>10)s=s.substring(0,10);
                    g2.drawString(s,x, h-10);
                }
            }
            g2.setColor(UIConstants.BORDER);g2.drawLine(left,top+plotH,w-right,top+plotH);
            g2.dispose();
        }
    }
}

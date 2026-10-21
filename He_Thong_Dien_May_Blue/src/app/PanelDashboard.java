package app;

import bus.Formats;
import dao.Db;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


public class PanelDashboard extends PanelBaseScreen {
    private static final long serialVersionUID=1L;

    private final JPanel cards=new JPanel(new GridLayout(1,4,16,0));
    private final RevenueChart chart=new RevenueChart();

    private final DefaultTableModel recentModel=new DefaultTableModel(
            new Object[]{"Mã hóa đơn","Khách hàng","Số lượng","Thực thu","Nhân viên","Ngày","Trạng thái"},0){
        private static final long serialVersionUID=1L;
        public boolean isCellEditable(int r,int c){return false;}
    };
    private final JTable recent=UIFactory.table(recentModel);
    private final JPanel categoryList=new JPanel();

    public PanelDashboard(){
        super("Trang chủ","Cập nhật tình hình hoạt động hôm nay.");

        RoundedPanel chartCard=new RoundedPanel(14,Color.WHITE);
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new javax.swing.border.EmptyBorder(15,16,14,16));

        JPanel chartHead=new JPanel(new BorderLayout());
        chartHead.setOpaque(false);
        JLabel ct=new JLabel("Doanh thu 7 ngày gần nhất");
        ct.setFont(UIConstants.font(Font.BOLD,13));
        ct.setForeground(UIConstants.TEXT);
        JLabel unit=new JLabel("Đơn vị: đồng");
        unit.setFont(UIConstants.font(Font.PLAIN,10));
        unit.setForeground(UIConstants.MUTED_LIGHT);
        chartHead.add(ct,BorderLayout.WEST);
        chartHead.add(unit,BorderLayout.EAST);
        chartCard.add(chartHead,BorderLayout.NORTH);
        chartCard.add(chart,BorderLayout.CENTER);

        RoundedPanel catCard=new RoundedPanel(14,Color.WHITE);
        catCard.setLayout(new BorderLayout());
        catCard.setBorder(new javax.swing.border.EmptyBorder(15,16,14,16));
        JLabel catTitle=new JLabel("Danh mục bán chạy");
        catTitle.setFont(UIConstants.font(Font.BOLD,13));
        catTitle.setForeground(UIConstants.TEXT);
        categoryList.setOpaque(false);
        categoryList.setLayout(new BoxLayout(categoryList,BoxLayout.Y_AXIS));
        catCard.add(catTitle,BorderLayout.NORTH);
        catCard.add(categoryList,BorderLayout.CENTER);

        GridBagLayout gbl=new GridBagLayout();
        JPanel middleGrid=new JPanel(gbl);
        middleGrid.setOpaque(false);

        GridBagConstraints gc1=new GridBagConstraints();
        gc1.gridx=0;gc1.gridy=0;gc1.weightx=.67;gc1.weighty=1;gc1.fill=GridBagConstraints.BOTH;gc1.insets=new Insets(0,0,0,8);
        GridBagConstraints gc2=new GridBagConstraints();
        gc2.gridx=1;gc2.gridy=0;gc2.weightx=.33;gc2.weighty=1;gc2.fill=GridBagConstraints.BOTH;gc2.insets=new Insets(0,8,0,0);
        middleGrid.add(chartCard,gc1);
        middleGrid.add(catCard,gc2);

        RoundedPanel recentCard=new RoundedPanel(14,Color.WHITE);
        recentCard.setLayout(new BorderLayout());
        recentCard.setBorder(new javax.swing.border.EmptyBorder(0,0,0,0));

        JPanel rh=new JPanel(new BorderLayout());
        rh.setOpaque(false);
        rh.setBorder(new javax.swing.border.EmptyBorder(12,16,11,16));
        JLabel rt=new JLabel("Đơn hàng gần đây");
        rt.setFont(UIConstants.font(Font.BOLD,13));
        rt.setForeground(UIConstants.TEXT);
        JLabel more=new JLabel("Xem tất cả →");
        more.setFont(UIConstants.font(Font.BOLD,10));
        more.setForeground(UIConstants.ORANGE);
        rh.add(rt,BorderLayout.WEST);
        rh.add(more,BorderLayout.EAST);
        recentCard.add(rh,BorderLayout.NORTH);
        recentCard.add(UIFactory.scroll(recent),BorderLayout.CENTER);

        JPanel center=new JPanel(new BorderLayout(0,16));
        center.setOpaque(false);
        center.add(middleGrid,BorderLayout.NORTH);
        center.add(recentCard,BorderLayout.CENTER);

        cards.setOpaque(false);
        cards.setPreferredSize(new Dimension(100,118));

        body.add(cards,BorderLayout.NORTH);
        body.add(center,BorderLayout.CENTER);

        refreshData();
    }

    @Override public void refreshData(){
        try{
            Object revenue=Db.scalar(
                    "SELECT ISNULL(SUM(i.total_amount-ISNULL(r.refunded,0)),0) "
                  + "FROM invoices i LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests WHERE status='COMPLETED' GROUP BY invoice_id) r ON r.invoice_id=i.invoice_id "
                  + "WHERE CAST(i.created_at AS DATE)=CAST(GETDATE() AS DATE) AND i.status IN ('PAID','PARTIAL_REFUND','REFUNDED')");
            Object orders=Db.scalar("SELECT COUNT(*) FROM invoices WHERE CAST(created_at AS DATE)=CAST(GETDATE() AS DATE) AND status IN ('PAID','PARTIAL_REFUND','REFUNDED')");
            Object customers=Db.scalar("SELECT COUNT(*) FROM customers WHERE CAST(created_at AS DATE)=CAST(GETDATE() AS DATE)");
            Object low=Db.scalar("SELECT COUNT(*) FROM products WHERE status IN ('ACTIVE','OUT_OF_STOCK') AND stock_quantity<=5");

            cards.removeAll();
            cards.add(kpi("money","Doanh thu hôm nay",shortMoney(revenue)));
            cards.add(kpi("cart","Đơn hàng hôm nay",String.valueOf(orders)));
            cards.add(kpi("users","Khách hàng mới",String.valueOf(customers)));
            cards.add(kpi("box","Sắp hết / hết hàng",String.valueOf(low)));

            java.util.List<Map<String,Object>> daily=Db.query(
                    "WITH d AS (SELECT CAST(GETDATE() AS DATE) dt UNION ALL SELECT DATEADD(DAY,-1,CAST(GETDATE() AS DATE)) "
                  + "UNION ALL SELECT DATEADD(DAY,-2,CAST(GETDATE() AS DATE)) UNION ALL SELECT DATEADD(DAY,-3,CAST(GETDATE() AS DATE)) "
                  + "UNION ALL SELECT DATEADD(DAY,-4,CAST(GETDATE() AS DATE)) UNION ALL SELECT DATEADD(DAY,-5,CAST(GETDATE() AS DATE)) "
                  + "UNION ALL SELECT DATEADD(DAY,-6,CAST(GETDATE() AS DATE))), "
                  + "net AS (SELECT CAST(i.created_at AS DATE) dt,SUM(i.total_amount-ISNULL(r.refunded,0)) revenue "
                  + "FROM invoices i LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests WHERE status='COMPLETED' GROUP BY invoice_id) r ON r.invoice_id=i.invoice_id "
                  + "WHERE i.status IN ('PAID','PARTIAL_REFUND','REFUNDED') GROUP BY CAST(i.created_at AS DATE)) "
                  + "SELECT d.dt,ISNULL(net.revenue,0) revenue FROM d LEFT JOIN net ON net.dt=d.dt ORDER BY d.dt");
            chart.setData(daily);

            categoryList.removeAll();
            java.util.List<Map<String,Object>> cats=Db.query(
                    "WITH ret AS (SELECT rd.invoice_detail_id,SUM(rd.quantity) returned_qty,SUM(rd.refund_amount) refunded "
                  + "FROM return_details rd JOIN return_requests rr ON rr.return_id=rd.return_id "
                  + "WHERE rr.status='COMPLETED' GROUP BY rd.invoice_detail_id) "
                  + "SELECT TOP 5 c.category_name, "
                  + "SUM(CASE WHEN d.quantity-ISNULL(ret.returned_qty,0)>0 THEN d.quantity-ISNULL(ret.returned_qty,0) ELSE 0 END) qty, "
                  + "SUM(CASE WHEN d.line_total-ISNULL(ret.refunded,0)>0 THEN d.line_total-ISNULL(ret.refunded,0) ELSE 0 END) val "
                  + "FROM invoice_details d JOIN products p ON p.product_id=d.product_id "
                  + "JOIN categories c ON c.category_id=p.category_id JOIN invoices i ON i.invoice_id=d.invoice_id "
                  + "LEFT JOIN ret ON ret.invoice_detail_id=d.invoice_detail_id "
                  + "WHERE i.status IN ('PAID','PARTIAL_REFUND','REFUNDED') "
                  + "GROUP BY c.category_name "
                  + "HAVING SUM(CASE WHEN d.quantity-ISNULL(ret.returned_qty,0)>0 THEN d.quantity-ISNULL(ret.returned_qty,0) ELSE 0 END)>0 "
                  + "ORDER BY qty DESC");
            double total=0;for(Map<String,Object> r:cats) total+=((Number)r.get("val")).doubleValue();
            for(Map<String,Object> r:cats){
                double v=((Number)r.get("val")).doubleValue();
                int pct=total<=0?0:(int)Math.round(v*100/total);
                categoryList.add(categoryRow(String.valueOf(r.get("category_name")),pct,shortMoney(r.get("val"))));
                categoryList.add(Box.createVerticalStrut(8));
            }
            if(cats.isEmpty()){
                JLabel empty=new JLabel("Chưa có dữ liệu bán hàng.");
                empty.setForeground(UIConstants.MUTED_LIGHT);
                empty.setFont(UIConstants.SMALL);
                categoryList.add(empty);
            }

            recentModel.setRowCount(0);
            for(Map<String,Object> r:Db.query(
                    "SELECT TOP 8 i.invoice_id,ISNULL(c.full_name,N'Khách vãng lai') customer,"
                  + "(SELECT SUM(quantity) FROM invoice_details d WHERE d.invoice_id=i.invoice_id) items,"
                  + "i.total_amount-ISNULL(rr.refunded,0) net_total,e.full_name employee,i.created_at,i.status "
                  + "FROM invoices i LEFT JOIN customers c ON c.customer_id=i.customer_id "
                  + "LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests WHERE status='COMPLETED' GROUP BY invoice_id) rr ON rr.invoice_id=i.invoice_id "
                  + "JOIN employees e ON e.employee_id=i.employee_id ORDER BY i.created_at DESC")){
                recentModel.addRow(new Object[]{
                        r.get("invoice_id"),r.get("customer"),r.get("items"),
                        Formats.money(r.get("net_total")),r.get("employee"),
                        Formats.date(r.get("created_at")),UiText.vi(r.get("status"))
                });
            }

            revalidate();
            repaint();
        }catch(Exception ex){showError(ex);}
    }

    private JPanel kpi(String iconKind,String label,String value){
        RoundedPanel card=new RoundedPanel(14,Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new javax.swing.border.EmptyBorder(18,15,16,15));

        JLabel val=new JLabel(value);
        val.setFont(UIConstants.font(Font.BOLD,23));
        val.setForeground(UIConstants.TEXT);

        JLabel lab=new JLabel(label);
        lab.setFont(UIConstants.font(Font.PLAIN,11));
        lab.setForeground(UIConstants.MUTED);

        JPanel content=new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));
        content.add(Box.createVerticalGlue());
        content.add(val);
        content.add(Box.createVerticalStrut(2));
        content.add(lab);

        card.add(content,BorderLayout.CENTER);
        return card;
    }

    private JPanel categoryRow(String name,int pct,String val){
        JPanel row=new JPanel(new BorderLayout(0,4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));

        JPanel top=new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel n=new JLabel(name);
        n.setFont(UIConstants.font(Font.PLAIN,11));
        n.setForeground(new Color(71,85,105));
        JLabel v=new JLabel(val);
        v.setFont(new Font("Monospaced",Font.PLAIN,10));
        v.setForeground(UIConstants.MUTED_LIGHT);
        top.add(n,BorderLayout.WEST);
        top.add(v,BorderLayout.EAST);

        JPanel bg=new JPanel(new BorderLayout());
        bg.setBackground(new Color(241,245,249));
        bg.setPreferredSize(new Dimension(100,6));

        JPanel fill=new JPanel();
        fill.setBackground(UIConstants.ORANGE);
        fill.setPreferredSize(new Dimension(Math.max(8,pct*2),6));
        bg.add(fill,BorderLayout.WEST);

        row.add(top,BorderLayout.NORTH);
        row.add(bg,BorderLayout.CENTER);
        return row;
    }

    private String shortMoney(Object value){
        if(value==null)return "0";
        double n=((Number)value).doubleValue();
        if(n>=1_000_000_000)return String.format("%.1f tỷ đ",n/1_000_000_000d);
        if(n>=1_000_000)return String.format("%.1f triệu đ",n/1_000_000d);
        if(n>=1_000)return String.format("%.0f nghìn đ",n/1_000d);
        return String.format("%.0fđ",n);
    }

    private static class RevenueChart extends JPanel{
        private static final long serialVersionUID=1L;
        private java.util.List<Map<String,Object>> data=Collections.emptyList();

        RevenueChart(){
            setOpaque(false);
            setPreferredSize(new Dimension(520,196));
        }

        void setData(java.util.List<Map<String,Object>> d){data=d;repaint();}

        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

            int w=getWidth(),h=getHeight();
            int left=34,right=w-10,top=18,bottom=h-25;
            double max=1;
            for(Map<String,Object> r:data)max=Math.max(max,((Number)r.get("revenue")).doubleValue());

            g2.setColor(new Color(241,245,249));
            for(int i=0;i<4;i++){
                int y=top+(bottom-top)*i/4;
                g2.drawLine(left,y,right,y);
            }

            int n=Math.max(1,data.size());
            int gap=18;
            int bw=Math.max(22,(right-left-gap*(n+1))/n);

            for(int i=0;i<data.size();i++){
                Map<String,Object> r=data.get(i);
                double v=((Number)r.get("revenue")).doubleValue();
                int bh=(int)((bottom-top-8)*v/max);
                int x=left+gap+i*(bw+gap);
                int y=bottom-bh;

                g2.setColor(UIConstants.NAVY);
                g2.fillRoundRect(x,y,bw,bh,5,5);

                g2.setColor(UIConstants.MUTED_LIGHT);
                g2.setFont(UIConstants.font(Font.PLAIN,10));
                String label=Formats.date(r.get("dt")).substring(0,5);
                g2.drawString(label,x+1,h-7);
            }

            g2.dispose();
        }
    }
}

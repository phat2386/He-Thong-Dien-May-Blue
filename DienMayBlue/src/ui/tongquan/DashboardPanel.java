package ui.tongquan;

import service.SalesService;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import util.Formats;
import ui.dungchung.*;

public class DashboardPanel extends BaseScreenPanel {
	
	private static final long serialVersionUID=1L;
	
	private final SalesService service = new SalesService();
	private final JPanel cardsContainer = new JPanel(new GridLayout(1,4,16,0));
	private final RevenueChart revenueChart = new RevenueChart();
	private final JPanel categoryListPanel = new JPanel();
	
	private final DefaultTableModel recentTableModel = UIFactory.readOnlyModel(
			new Object[] {"Mã hóa đơn","Khách hàng","Số lượng","Thực thu","Nhân viên","Ngày","Trạng thái"
	});
	
	private final JTable recentTable = UIFactory.table(recentTableModel);
	
	public DashboardPanel() {
	    super("Trang Chủ", "Cập nhật tình hình hoạt động");
	    initLayout();
	    refreshData();
	}
	
	private void initLayout() {
		cardsContainer.setOpaque(false);
		cardsContainer.setPreferredSize(new Dimension(100,118));
		
		JPanel middleSection = createMiddleSection();
		JPanel recentSection = createRecentOrderSection();
		
		JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);
        centerPanel.add(middleSection, BorderLayout.NORTH);
        centerPanel.add(recentSection, BorderLayout.CENTER);
		
        body.add(cardsContainer,BorderLayout.NORTH);
        body.add(centerPanel,BorderLayout.CENTER);
        
	}

	private JPanel createRecentSection() {
		 JPanel panel = new JPanel(new BorderLayout());
		    panel.add(recentTable.getTableHeader(), BorderLayout.NORTH);
		    panel.add(recentTable, BorderLayout.CENTER);
		    return panel;
	}

	private JPanel createMiddleSection() {
        RoundedPanel chartCard = new RoundedPanel(14, Color.WHITE);
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new javax.swing.border.EmptyBorder(15, 16, 14, 16));
        
        JPanel chartHead = new JPanel(new BorderLayout());
        chartHead.setOpaque(false);
        JLabel titleChart = new JLabel("Doanh thu 7 ngày gần nhất");
        titleChart.setFont(UIConstants.font(Font.BOLD, 13));
        titleChart.setForeground(UIConstants.TEXT);
        JLabel unitLabel = new JLabel("Đơn vị: đồng");
        unitLabel.setFont(UIConstants.font(Font.PLAIN, 10));
        unitLabel.setForeground(UIConstants.MUTED_LIGHT);
        chartHead.add(titleChart, BorderLayout.WEST);
        chartHead.add(unitLabel, BorderLayout.EAST);
        
        chartCard.add(chartHead, BorderLayout.NORTH);
        chartCard.add(revenueChart, BorderLayout.CENTER);

        RoundedPanel catCard = new RoundedPanel(14, Color.WHITE);
        catCard.setLayout(new BorderLayout());
        catCard.setBorder(new javax.swing.border.EmptyBorder(15, 16, 14, 16));
        
        JLabel catTitle = new JLabel("Danh mục bán chạy");
        catTitle.setFont(UIConstants.font(Font.BOLD, 13));
        catTitle.setForeground(UIConstants.TEXT);
        
        categoryListPanel.setOpaque(false);
        categoryListPanel.setLayout(new BoxLayout(categoryListPanel, BoxLayout.Y_AXIS));
        
        catCard.add(catTitle, BorderLayout.NORTH);
        catCard.add(categoryListPanel, BorderLayout.CENTER);

        JPanel middleGrid = new JPanel(new GridBagLayout());
        middleGrid.setOpaque(false);
        
        GridBagConstraints gc1 = new GridBagConstraints();
        gc1.gridx = 0; gc1.gridy = 0; gc1.weightx = .67; gc1.weighty = 1; 
        gc1.fill = GridBagConstraints.BOTH; gc1.insets = new Insets(0, 0, 0, 8);
        
        GridBagConstraints gc2 = new GridBagConstraints();
        gc2.gridx = 1; gc2.gridy = 0; gc2.weightx = .33; gc2.weighty = 1; 
        gc2.fill = GridBagConstraints.BOTH; gc2.insets = new Insets(0, 8, 0, 0);
        
        middleGrid.add(chartCard, gc1);
        middleGrid.add(catCard, gc2);
        
        return middleGrid;
    }

	private JPanel createRecentOrderSection() {
	    RoundedPanel recentCard = new RoundedPanel(14, Color.WHITE);
	    recentCard.setLayout(new BorderLayout());

	    JLabel title = new JLabel("Đơn hàng gần đây");
	    title.setFont(UIConstants.font(Font.BOLD, 13));
	    title.setForeground(UIConstants.TEXT);
	    title.setBorder(new javax.swing.border.EmptyBorder(12, 16, 11, 16));

	    recentCard.add(title, BorderLayout.NORTH);
	    recentCard.add(UIFactory.scroll(recentTable), BorderLayout.CENTER);

	    return recentCard;
	}

	@Override
	public void refreshData() {
		try {
            Object revenue = service.scalar(
                "SELECT ISNULL(SUM(i.total_amount-ISNULL(r.refunded,0)),0) "
              + "FROM invoices i LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests WHERE status='COMPLETED' GROUP BY invoice_id) r ON r.invoice_id=i.invoice_id "
              + "WHERE CAST(i.created_at AS DATE)=CAST(GETDATE() AS DATE) AND i.status IN ('PAID','PARTIAL_REFUND','REFUNDED')");
            Object orders = service.scalar("SELECT COUNT(*) FROM invoices WHERE CAST(created_at AS DATE)=CAST(GETDATE() AS DATE) AND status IN ('PAID','PARTIAL_REFUND','REFUNDED')");
            Object customers = service.scalar("SELECT COUNT(*) FROM customers WHERE CAST(created_at AS DATE)=CAST(GETDATE() AS DATE)");
            Object lowStock = service.scalar("SELECT COUNT(*) FROM products WHERE status IN ('ACTIVE','OUT_OF_STOCK') AND stock_quantity<=5");
            
            cardsContainer.removeAll();
            cardsContainer.add(createKpiCard("Doanh thu hôm nay", shortMoney(revenue)));
            cardsContainer.add(createKpiCard("Đơn hàng hôm nay", String.valueOf(orders)));
            cardsContainer.add(createKpiCard("Khách hàng mới", String.valueOf(customers)));
            cardsContainer.add(createKpiCard("Sắp hết / hết hàng", String.valueOf(lowStock)));

            java.util.List<Map<String, Object>> dailyRevenue = service.query(
                "WITH d AS (SELECT CAST(GETDATE() AS DATE) dt UNION ALL SELECT DATEADD(DAY,-1,CAST(GETDATE() AS DATE)) "
              + "UNION ALL SELECT DATEADD(DAY,-2,CAST(GETDATE() AS DATE)) UNION ALL SELECT DATEADD(DAY,-3,CAST(GETDATE() AS DATE)) "
              + "UNION ALL SELECT DATEADD(DAY,-4,CAST(GETDATE() AS DATE)) UNION ALL SELECT DATEADD(DAY,-5,CAST(GETDATE() AS DATE)) "
              + "UNION ALL SELECT DATEADD(DAY,-6,CAST(GETDATE() AS DATE))), "
              + "net AS (SELECT CAST(i.created_at AS DATE) dt,SUM(i.total_amount-ISNULL(r.refunded,0)) revenue "
              + "FROM invoices i LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests WHERE status='COMPLETED' GROUP BY invoice_id) r ON r.invoice_id=i.invoice_id "
              + "WHERE i.status IN ('PAID','PARTIAL_REFUND','REFUNDED') GROUP BY CAST(i.created_at AS DATE)) "
              + "SELECT d.dt,ISNULL(net.revenue,0) revenue FROM d LEFT JOIN net ON net.dt=d.dt ORDER BY d.dt");
            revenueChart.setData(dailyRevenue);

            categoryListPanel.removeAll();
            java.util.List<Map<String, Object>> topCategories = service.query(
                "WITH ret AS (SELECT rd.invoice_detail_id,SUM(rd.quantity) returned_qty,SUM(rd.refund_amount) refunded "
              + "FROM return_details rd JOIN return_requests rr ON rr.return_id=rd.return_id "
              + "WHERE rr.status='COMPLETED' GROUP BY rd.invoice_detail_id) " + "SELECT TOP 5 c.category_name, "
              + "SUM(CASE WHEN d.quantity-ISNULL(ret.returned_qty,0)>0 THEN d.quantity-ISNULL(ret.returned_qty,0) ELSE 0 END) qty, "
              + "SUM(CASE WHEN d.line_total-ISNULL(ret.refunded,0)>0 THEN d.line_total-ISNULL(ret.refunded,0) ELSE 0 END) val "
              + "FROM invoice_details d JOIN products p ON p.product_id=d.product_id "
              + "JOIN categories c ON c.category_id=p.category_id JOIN invoices i ON i.invoice_id=d.invoice_id "
              + "LEFT JOIN ret ON ret.invoice_detail_id=d.invoice_detail_id "
              + "WHERE i.status IN ('PAID','PARTIAL_REFUND','REFUNDED') " + "GROUP BY c.category_name "
              + "HAVING SUM(CASE WHEN d.quantity-ISNULL(ret.returned_qty,0)>0 THEN d.quantity-ISNULL(ret.returned_qty,0) ELSE 0 END)>0 "
              + "ORDER BY qty DESC");
            
            double totalValue = 0;
            for (Map<String, Object> r : topCategories) totalValue += ((Number) r.get("val")).doubleValue();
            
            for (Map<String, Object> r : topCategories) {
                double val = ((Number) r.get("val")).doubleValue();
                int percent = totalValue <= 0 ? 0 : (int) Math.round(val * 100 / totalValue);
                categoryListPanel.add(createCategoryRow(String.valueOf(r.get("category_name")), percent, shortMoney(r.get("val"))));
                categoryListPanel.add(Box.createVerticalStrut(8));
            }
            
            if (topCategories.isEmpty()) {
                JLabel emptyLabel = new JLabel("Chưa có dữ liệu bán hàng.");
                emptyLabel.setForeground(UIConstants.MUTED_LIGHT);
                emptyLabel.setFont(UIConstants.SMALL);
                categoryListPanel.add(emptyLabel);
            }

            recentTableModel.setRowCount(0);
            for (Map<String, Object> r : service.query(
                "SELECT TOP 8 i.invoice_id,ISNULL(c.full_name,N'Khách vãng lai') customer,"
              + "(SELECT SUM(quantity) FROM invoice_details d WHERE d.invoice_id=i.invoice_id) items,"
              + "i.total_amount-ISNULL(rr.refunded,0) net_total,e.full_name employee,i.created_at,i.status "
              + "FROM invoices i LEFT JOIN customers c ON c.customer_id=i.customer_id "
              + "LEFT JOIN (SELECT invoice_id,SUM(refund_amount) refunded FROM return_requests WHERE status='COMPLETED' GROUP BY invoice_id) rr ON rr.invoice_id=i.invoice_id "
              + "JOIN employees e ON e.employee_id=i.employee_id ORDER BY i.created_at DESC")) {
                recentTableModel.addRow(new Object[]{
                    r.get("invoice_id"), r.get("customer"), r.get("items"), 
                    Formats.money(r.get("net_total")), r.get("employee"),
                    Formats.date(r.get("created_at")), Formats.vi(r.get("status"))
                });
            }
            
            body.revalidate();
            body.repaint();
        }catch (Exception ex) {
            ex.printStackTrace();
            showError(ex);
        }
	}
	
	private JPanel createKpiCard(String label, String value) {
        RoundedPanel card = new RoundedPanel(14, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new javax.swing.border.EmptyBorder(18, 15, 16, 15));
        
        JLabel valLabel = new JLabel(value);
        valLabel.setFont(UIConstants.font(Font.BOLD, 23));
        valLabel.setForeground(UIConstants.TEXT);
        
        JLabel labLabel = new JLabel(label);
        labLabel.setFont(UIConstants.font(Font.PLAIN, 11));
        labLabel.setForeground(UIConstants.MUTED);
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(Box.createVerticalGlue());
        content.add(valLabel);
        content.add(Box.createVerticalStrut(2));
        content.add(labLabel);
        
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCategoryRow(String name, int pct, String val) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(UIConstants.font(Font.PLAIN, 11));
        nameLabel.setForeground(new Color(71, 85, 105));
        
        JLabel valLabel = new JLabel(val);
        valLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
        valLabel.setForeground(UIConstants.MUTED_LIGHT);
        
        top.add(nameLabel, BorderLayout.WEST);
        top.add(valLabel, BorderLayout.EAST);
        
        JPanel bg = new JPanel(new BorderLayout());
        bg.setBackground(new Color(241, 245, 249));
        bg.setPreferredSize(new Dimension(100, 6));
        
        JPanel fill = new JPanel();
        fill.setBackground(UIConstants.ORANGE);
        fill.setPreferredSize(new Dimension(Math.max(8, pct * 2), 6));
        bg.add(fill, BorderLayout.WEST);
        
        row.add(top, BorderLayout.NORTH);
        row.add(bg, BorderLayout.CENTER);
        return row;
    }

    private String shortMoney(Object value) {
        if (value == null) return "0";
        double n = ((Number) value).doubleValue();
        if (n >= 1_000_000_000) return String.format("%.1f tỷ đ", n / 1_000_000_000d);
        if (n >= 1_000_000) return String.format("%.1f triệu đ", n / 1_000_000d);
        if (n >= 1_000) return String.format("%.0f nghìn đ", n / 1_000d);
        return String.format("%.0fđ", n);
    }
    
    private static class RevenueChart extends JPanel {
        private static final long serialVersionUID = 1L;
        private java.util.List<Map<String, Object>> data = Collections.emptyList();

        RevenueChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(520, 196));
        }

        void setData(java.util.List<Map<String, Object>> d) {
            data = (d == null) ? Collections.emptyList() : d;
            repaint();
        }

        @Override 
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int left = 34, right = w - 10, top = 18, bottom = h - 25;
            
            double max = 1;
            for (Map<String, Object> r : data) {
                max = Math.max(max, ((Number) r.get("revenue")).doubleValue());
            }

            g2.setColor(new Color(241, 245, 249));
            for (int i = 0; i < 4; i++) {
                int y = top + (bottom - top) * i / 4;
                g2.drawLine(left, y, right, y);
            }

            int n = Math.max(1, data.size());
            int gap = 18;
            int bw = Math.max(22, (right - left - gap * (n + 1)) / n);

            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> r = data.get(i);
                double v = ((Number) r.get("revenue")).doubleValue();
                int bh = (int) ((bottom - top - 8) * v / max);
                int x = left + gap + i * (bw + gap);
                int y = bottom - bh;

                g2.setColor(UIConstants.NAVY);
                g2.fillRoundRect(x, y, bw, bh, 5, 5);

                g2.setColor(UIConstants.MUTED_LIGHT);
                g2.setFont(UIConstants.font(Font.PLAIN, 10));
                String label = Formats.date(r.get("dt"));
                if (label.length() >= 5) label = label.substring(0, 5);
                g2.drawString(label, x + 1, h - 7);
            }
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Dashboard Tổng Quan");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new DashboardPanel());
            f.setMinimumSize(new Dimension(1000, 680));
            f.setSize(1260, 780);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}

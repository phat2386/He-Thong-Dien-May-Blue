package ui.khachhang;

import java.awt.*;
import java.util.Map;
import java.math.BigDecimal;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.CustomerService;
import util.Formats;	
import util.SessionManager;
import ui.dungchung.*;

public class MembershipManagementPanel extends BaseScreenPanel {
	private static final long serialVersionUID = 1L;
	
	private final CustomerService service = new CustomerService();
	private final JTextField search = UIFactory.field(260);
	private final DefaultTableModel model = UIFactory.readOnlyModel(new Object[]{
			"Mã KH","Họ tên","SĐT","Hạng hiện tại","Điểm tích lũy","Mã thẻ đang dùng"});
	private final JTable table = UIFactory.table(model);
	
	public MembershipManagementPanel() {
		super("Thẻ / hạng thành viên", "Quản lý thẻ, điểm tích lũy và hạng thành viên. Hạng khách hàng được tự động cập nhật theo điểm hiện có.");
        JButton refresh = UIFactory.secondaryButton("Làm mới");
        JButton reissue = UIFactory.primaryButton("Cấp thẻ / Cấp lại thẻ");
        JButton policy = UIFactory.secondaryButton("Chính sách hạng & điểm");
        
        reissue.setToolTipText("Chọn một khách hàng trong bảng để cấp thẻ lần đầu hoặc vô hiệu hóa thẻ cũ và cấp thẻ mới.");
        policy.setToolTipText("Xem và điều chỉnh mốc điểm, ưu đãi và hệ số tích điểm của từng hạng thành viên.");
        refresh.setToolTipText("Tải lại danh sách thẻ, điểm và hạng thành viên.");
        
        actions.add(reissue);
//        if (SessionManager.hasPermission("POINT_MANAGE")) 
        actions.add(policy);
        actions.add(refresh);
        
        search.putClientProperty("JTextField.placeholderText", "Nhập mã, tên hoặc SĐT khách hàng...");
        search.setToolTipText("Nhập mã khách hàng, họ tên hoặc số điện thoại để lọc danh sách.");
        
        JLabel guide = new JLabel("Chọn khách hàng trong bảng để cấp/cấp lại thẻ. Hạng hiện tại được hệ thống tự xác định từ điểm tích lũy.");
        guide.setForeground(UIConstants.MUTED);
        
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(UIFactory.toolbar(new JLabel("Tìm khách hàng"), search), BorderLayout.NORTH);
        north.add(guide, BorderLayout.SOUTH);
        
        body.add(north, BorderLayout.NORTH);
        body.add(UIFactory.scroll(table), BorderLayout.CENTER);
        
        UIFactory.onTextChange(search, this::refreshData);
        search.addActionListener(e -> refreshData());
        refresh.addActionListener(e -> refreshData());
        reissue.addActionListener(e -> reissueCard());
        policy.addActionListener(e -> openPointPolicy());
        
        refreshData();
	}
	@Override
	public void refreshData() {
	        try {
	            String kw = "%" + search.getText().trim() + "%";
	            model.setRowCount(0);
	            for (Map<String,Object> r : service.membershipCustomers(kw)) {
	                model.addRow(new Object[]{
	                    r.get("customer_id"), 
	                    r.get("full_name"), 
	                    r.get("phone"),
	                    r.get("tier_name"), 
	                    r.get("current_points"), 
	                    r.get("card_code")
	                });
	            }
	        } catch (Exception ex) {
	            showError(ex);
	        }
	    }
	
	private String selectedCustomerId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            info("Hãy chọn khách hàng.");
            return null;
        }
        int row = table.convertRowIndexToModel(viewRow);
        return String.valueOf(model.getValueAt(row, 0));
    }

	private void reissueCard() {
        String customerId = selectedCustomerId();
        if (customerId == null) return;
        
        int viewRow = table.getSelectedRow();
        int row = table.convertRowIndexToModel(viewRow);
        String oldCard = Formats.safe(model.getValueAt(row, 5));
        
        String question = oldCard.isEmpty() ? "Cấp thẻ thành viên lần đầu cho " + customerId + "?"
                : "Cấp lại thẻ cho " + customerId + "? Thẻ hiện tại " + oldCard + " sẽ bị vô hiệu hóa.";
        
        if (!confirm(question)) return;
        try {
            String card = service.reissueMembershipCard(customerId);
            refreshData();
            info("Đã cấp thẻ thành công. Mã thẻ đang hoạt động: " + card);
        } catch (Exception ex) {
            showError(ex);
        }
    }
	private void openPointPolicy() {
        final DefaultTableModel m = UIFactory.readOnlyModel(new Object[]{"ID","Mã hạng","Tên hạng","Điểm tối thiểu","Ưu đãi (%)","Hệ số tích điểm"});
        final JTable t = UIFactory.table(m);
        
        final Runnable reload = () -> {
            try {
                m.setRowCount(0);
                for (Map<String,Object> r : service.membershipTiers()) {
                    m.addRow(new Object[]{
                        r.get("tier_id"), r.get("tier_code"), r.get("tier_name"),
                        r.get("min_points"), r.get("discount_rate"), r.get("point_multiplier")
                    });
                }
            } catch (Exception ex) {
                showError(ex);
            }
        };
        reload.run();
        
        JLabel note = new JLabel("Hạng được xác định theo điểm tích lũy. Quy đổi: 1 điểm / 10.000đ × hệ số hạng; ưu đãi hạng không cộng dồn với khuyến mãi.");
        note.setForeground(UIConstants.MUTED);
        
        JButton edit = UIFactory.primaryButton("Cập nhật chính sách hạng");
        edit.setToolTipText("Chọn một hạng trong bảng rồi cập nhật mốc điểm, ưu đãi hoặc hệ số tích điểm.");
        
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setPreferredSize(new Dimension(820, 390));
        panel.add(note, BorderLayout.NORTH);
        panel.add(UIFactory.scroll(t), BorderLayout.CENTER);
        
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(edit);
        panel.add(bottom, BorderLayout.SOUTH);
        
        edit.addActionListener(e -> {
            int vr = t.getSelectedRow();
            if (vr < 0) { info("Hãy chọn hạng cần sửa."); return; }
            int rr = t.convertRowIndexToModel(vr);
            
            int tierId = ((Number)m.getValueAt(rr,0)).intValue();
            String tierName = String.valueOf(m.getValueAt(rr,2));
            
            JTextField min = UIFactory.field(120);
            JTextField discount = UIFactory.field(120);
            JTextField multiplier = UIFactory.field(120);
            
            min.setText(String.valueOf(m.getValueAt(rr,3)));
            discount.setText(String.valueOf(m.getValueAt(rr,4)));
            multiplier.setText(String.valueOf(m.getValueAt(rr,5)));
            
            JPanel f = new JPanel(new GridLayout(0,2,8,8));
            f.add(new JLabel("Hạng")); f.add(new JLabel(tierName));
            f.add(new JLabel("Mốc điểm tối thiểu")); f.add(min);
            f.add(new JLabel("Ưu đãi hạng (%)")); f.add(discount);
            f.add(new JLabel("Hệ số tích điểm")); f.add(multiplier);
            
            if (JOptionPane.showConfirmDialog(DialogUtils.owner(panel), f, "Cập nhật chính sách - " + tierName,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            try {
                service.updateMembershipTierPolicy(tierId, Integer.parseInt(min.getText().trim()),
                        new BigDecimal(discount.getText().trim()), new BigDecimal(multiplier.getText().trim()));
                reload.run();
                refreshData();
                info("Đã cập nhật chính sách; hạng khách hàng đã được cập nhật tự động theo điểm.");
            } catch (Exception ex) {
                showError(ex);
            }
        });
        
        JOptionPane.showMessageDialog(DialogUtils.owner(this), panel, "Chính sách hạng thành viên & tích điểm", JOptionPane.PLAIN_MESSAGE);
    }
	
}

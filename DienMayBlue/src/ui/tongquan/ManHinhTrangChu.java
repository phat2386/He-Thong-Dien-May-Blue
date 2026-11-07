package ui.tongquan;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.*;
import ui.dungchung.*;
import java.util.List;
public class ManHinhTrangChu extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final ThongKeService dichVu = new ThongKeService();
    private final JPanel khungCacThe = new JPanel(new GridLayout(1, 4, 12, 0));
    private final BieuDoDoanhThu bieuDoDoanhThu = new BieuDoDoanhThu();
    private final DefaultTableModel moHinhDanhMuc = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Danh mục", "Doanh thu"
    });
    private final JTable bangDanhMuc = TaoGiaoDien.bang(moHinhDanhMuc);
    private final DefaultTableModel moHinhBangGanDay = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã hóa đơn", "Khách hàng", "SL", "Thực thu", "Nhân viên", "Ngày", "Trạng thái"
    });
    private final JTable bangGanDay = TaoGiaoDien.bang(moHinhBangGanDay);
    public ManHinhTrangChu() {
        super("Trang chủ", "Tổng quan hoạt động kinh doanh");
        khoiTaoBoCuc();
    }
    private void khoiTaoBoCuc() {
        khungCacThe.setOpaque(false);
        khungCacThe.setPreferredSize(new Dimension(100, 96));
        JPanel khuVucTongQuan = taoKhuVucTongQuan();
        JPanel khuVucGanDay = taoKhuVucDonGanDay();
        JPanel trungTam = new JPanel(new BorderLayout(0, 12));
        trungTam.setOpaque(false);
        trungTam.add(khuVucTongQuan, BorderLayout.NORTH);
        trungTam.add(khuVucGanDay, BorderLayout.CENTER);
        noiDung.add(khungCacThe, BorderLayout.NORTH);
        noiDung.add(trungTam, BorderLayout.CENTER);
    }
    private JPanel taoKhuVucTongQuan() {
        KhungBoGoc theBieuDo = new KhungBoGoc(12, Color.WHITE);
        theBieuDo.setLayout(new BorderLayout(0, 8));
        theBieuDo.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        JLabel tieuDeBieuDo = taoTieuDeKhuVuc("Doanh thu 7 ngày gần nhất");
        theBieuDo.add(tieuDeBieuDo, BorderLayout.NORTH);
        theBieuDo.add(bieuDoDoanhThu, BorderLayout.CENTER);
        KhungBoGoc theDanhMuc = new KhungBoGoc(12, Color.WHITE);
        theDanhMuc.setLayout(new BorderLayout(0, 8));
        theDanhMuc.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        theDanhMuc.add(taoTieuDeKhuVuc("Danh mục bán chạy"), BorderLayout.NORTH);
        theDanhMuc.add(TaoGiaoDien.bangPhanTrang(bangDanhMuc), BorderLayout.CENTER);
        JPanel luoi = new JPanel(new GridBagLayout());
        luoi.setOpaque(false);
        GridBagConstraints trai = new GridBagConstraints();
        trai.gridx = 0;
        trai.gridy = 0;
        trai.weightx = 0.68;
        trai.weighty = 1;
        trai.fill = GridBagConstraints.BOTH;
        trai.insets = new Insets(0, 0, 0, 6);
        GridBagConstraints phai = new GridBagConstraints();
        phai.gridx = 1;
        phai.gridy = 0;
        phai.weightx = 0.32;
        phai.weighty = 1;
        phai.fill = GridBagConstraints.BOTH;
        phai.insets = new Insets(0, 6, 0, 0);
        luoi.add(theBieuDo, trai);
        luoi.add(theDanhMuc, phai);
        luoi.setPreferredSize(new Dimension(100, 340));
        return luoi;
    }
    private JPanel taoKhuVucDonGanDay() {
        KhungBoGoc the = new KhungBoGoc(12, Color.WHITE);
        the.setLayout(new BorderLayout(0, 8));
        the.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        the.add(taoTieuDeKhuVuc("Hóa đơn gần đây"), BorderLayout.NORTH);
        the.add(TaoGiaoDien.bangPhanTrang(bangGanDay), BorderLayout.CENTER);
        return the;
    }
    private JLabel taoTieuDeKhuVuc(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 13));
        label.setForeground(HangSoGiaoDien.TEXT);
        return label;
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            Map<String, Object> tongQuan = dichVu.layTongQuanHomNay();
            khungCacThe.removeAll();
            khungCacThe.add(taoTheKpi("Doanh thu hôm nay", tienRutGon(tongQuan.get("revenue"))));
            khungCacThe.add(taoTheKpi("Hóa đơn hôm nay", String.valueOf(tongQuan.get("orders"))));
            khungCacThe.add(taoTheKpi("Khách hàng mới", String.valueOf(tongQuan.get("customers"))));
            khungCacThe.add(taoTheKpi("Sắp hết / hết hàng", String.valueOf(tongQuan.get("low_stock"))));
            java.util.List<Map<String, Object>> doanhThuNgay = dichVu.layDoanhThuBayNgayGanNhat();
            bieuDoDoanhThu.setData(doanhThuNgay);
            moHinhDanhMuc.setRowCount(0);
            for (Map<String, Object> r : dichVu.layTopDanhMucBanChay()) {
                moHinhDanhMuc.addRow(new Object[] {
                    String.valueOf(r.get("category_name")), DinhDangHienThi.tien(r.get("val"))
                });
            }
            moHinhBangGanDay.setRowCount(0);
            for (Map<String, Object> r : dichVu.layHoaDonGanDay()) {
                moHinhBangGanDay.addRow(new Object[] {
                    r.get("invoice_id"), r.get("customer"), r.get("items"), DinhDangHienThi.tien(r.get("net_total")), r.get("employee"), DinhDangHienThi.ngay(r.get("created_at")), DinhDangHienThi.tiengViet(r.get( "status"))
                });
            }
            noiDung.revalidate();
            noiDung.repaint();
        } catch (Exception ex) {
            ex.printStackTrace();
            hienLoi(ex);
        }
    }
    private JPanel taoTheKpi(String nhan, String giaTri) {
        KhungBoGoc the = new KhungBoGoc(12, Color.WHITE);
        the.setLayout(new BorderLayout());
        the.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        JLabel lblMoTa = new JLabel(nhan);
        lblMoTa.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 11));
        lblMoTa.setForeground(HangSoGiaoDien.MUTED);
        JLabel lblGiaTri = new JLabel(giaTri);
        lblGiaTri.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 20));
        lblGiaTri.setForeground(HangSoGiaoDien.TEXT);
        JPanel noiDungThe = new JPanel();
        noiDungThe.setOpaque(false);
        noiDungThe.setLayout(new BoxLayout(noiDungThe, BoxLayout.Y_AXIS));
        noiDungThe.add(lblMoTa);
        noiDungThe.add(Box.createVerticalStrut(7));
        noiDungThe.add(lblGiaTri);
        noiDungThe.add(Box.createVerticalGlue());
        the.add(noiDungThe, BorderLayout.CENTER);
        return the;
    }
    private String tienRutGon(Object giaTri) {
        if (giaTri == null) return "0 đ";
        double n = ((Number) giaTri).doubleValue();
        if (n >= 1_000_000_000) return String.format("%.1f tỷ đ", n / 1_000_000_000d);
        if (n >= 1_000_000) return String.format("%.1f triệu đ", n / 1_000_000d);
        if (n >= 1_000) return String.format("%.0f nghìn đ", n / 1_000d);
        return String.format("%.0f đ", n);
    }
    private static class BieuDoDoanhThu extends JPanel {
        private static final long serialVersionUID = 1L;
        private java.util.List<Map<String, Object>> duLieu = Collections.emptyList();
        BieuDoDoanhThu() {
            setOpaque(false);
            setPreferredSize(new Dimension(520, 190));
        }
        void setData(java.util.List<Map<String, Object>> data) {
            duLieu = data == null ? Collections.emptyList() : data;
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int left = 38;
            int right = w - 12;
            int top = 12;
            int bottom = h - 28;
            g2.setColor(HangSoGiaoDien.BORDER);
            for (int i = 0; i <= 4; i++) {
                int y = top + (bottom - top) * i / 4;
                g2.drawLine(left, y, right, y);
            }
            double max = 1;
            for (Map<String, Object> r : duLieu) {
                max = Math.max(max, ((Number) r.get("revenue")).doubleValue());
            }
            int n = Math.max(1, duLieu.size());
            int khoang = 16;
            int doRongCot = Math.max(22, (right - left - khoang * (n + 1)) / n);
            for (int i = 0; i < duLieu.size(); i++) {
                Map<String, Object> r = duLieu.get(i);
                double value = ((Number) r.get("revenue")).doubleValue();
                int chieuCao = (int)((bottom - top - 6) * value / max);
                int x = left + khoang + i * (doRongCot + khoang);
                int y = bottom - chieuCao;
                g2.setColor(HangSoGiaoDien.NAVY_2);
                g2.fillRoundRect(x, y, doRongCot, chieuCao, 4, 4);
                g2.setColor(HangSoGiaoDien.MUTED);
                g2.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 10));
                String nhan = DinhDangHienThi.ngay(r.get("dt"));
                if (nhan.length() >= 5) nhan = nhan.substring(0, 5);
                g2.drawString(nhan, x, h - 8);
            }
            g2.dispose();
        }
    }
}

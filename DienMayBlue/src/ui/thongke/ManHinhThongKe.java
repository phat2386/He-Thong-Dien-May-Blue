package ui.thongke;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import service.*;
import ui.dungchung.*;
import java.util.List;
public class ManHinhThongKe extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final JTextField txtTuNgay = TaoGiaoDien.oNhap(110);
    private final JTextField txtDenNgay = TaoGiaoDien.oNhap(110);
    private final JComboBox<String> cmbNhomDoanhThu = TaoGiaoDien.oChon("Theo ngày", "Theo tháng", "Theo năm");
    private final JComboBox<String> cmbXepKhachHang = TaoGiaoDien.oChon("Theo tổng giá trị", "Theo số lần mua");
    private final JTextField txtTimNhanVien = TaoGiaoDien.oNhap(260);
    private final JLabel lblDoanhThu = taoGiaTriTongHop("0 đ");
    private final JLabel lblLoiNhuan = taoGiaTriTongHop("0 đ");
    private final JLabel lblSoHoaDon = taoGiaTriTongHop("0");
    private final JLabel lblSanPhamBanRa = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinhDoanhThu = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Kỳ", "Doanh thu ròng", "Lợi nhuận", "Số hóa đơn ròng"
    });
    private final DefaultTableModel moHinhSanPham = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Sản phẩm", "SL bán ròng", "Doanh thu ròng", "Xếp hạng"
    });
    private final DefaultTableModel moHinhKhachHang = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã KH", "Khách hàng", "SĐT", "Số lần mua", "Chi tiêu ròng"
    });
    private final DefaultTableModel moHinhThuong = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Ngày", "Mã NV", "Nhân viên", "Số hóa đơn", "Doanh số", "Hoàn trả", "Doanh số ròng", "Tiền thưởng"
    });
    private final JTable tblDoanhThu = TaoGiaoDien.bang(moHinhDoanhThu);
    private final JTable tblSanPham = TaoGiaoDien.bang(moHinhSanPham);
    private final JTable tblKhachHang = TaoGiaoDien.bang(moHinhKhachHang);
    private final JTable tblThuong = TaoGiaoDien.bang(moHinhThuong);
    private final TableRowSorter<DefaultTableModel> sapXepDoanhSoNhanVien = new TableRowSorter<DefaultTableModel>( moHinhThuong);
    private final BieuDoDoanhThu bieuDo = new BieuDoDoanhThu();
    private final JTabbedPane tabs = new JTabbedPane();
    private final ThongKeService dichVuThongKe = new ThongKeService();
    private final HoaDonService dichVuBanHang = new HoaDonService();
    public ManHinhThongKe() {
        super("Thống kê", "Tổng hợp doanh thu, lợi nhuận, sản phẩm, khách hàng và thưởng doanh số theo khoảng thời gian.");
        TaoGiaoDien.datChuGoiY(txtTimNhanVien, "Mã NV hoặc tên nhân viên");
        txtTuNgay.setText(LocalDate.now().withDayOfMonth(1).toString());
        txtDenNgay.setText(LocalDate.now().toString());
        JButton btnApDung = TaoGiaoDien.nutChinh("Áp dụng");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        JButton btnXuat = TaoGiaoDien.nutPhu("Xuất CSV");
        JButton btnChotThuong = TaoGiaoDien.nutPhu("Chốt thưởng");
        thaoTac.add(btnXuat);
        thaoTac.add(btnChotThuong);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        JPanel tren = new JPanel(new BorderLayout(0, 10));
        tren.setOpaque(false);
        tren.add(TaoGiaoDien.thanhCongCu(new JLabel("Từ ngày"), txtTuNgay, new JLabel("Đến ngày"), txtDenNgay, new JLabel("Nhóm doanh thu"), cmbNhomDoanhThu, new JLabel("Xếp khách hàng"), cmbXepKhachHang, btnApDung, btnLamMoi), BorderLayout.NORTH);
        tren.add(taoKhungTongHop(), BorderLayout.CENTER);
        khung.add(tren, BorderLayout.NORTH);
        tblThuong.setRowSorter(sapXepDoanhSoNhanVien);
        taoCacTab();
        khung.add(tabs, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        btnApDung.addActionListener(e -> lamMoiDuLieu());
        btnLamMoi.addActionListener(e -> {
            txtTuNgay.setText(LocalDate.now().withDayOfMonth(1).toString());
            txtDenNgay.setText(LocalDate.now().toString());
            cmbNhomDoanhThu.setSelectedItem("Theo ngày");
            cmbXepKhachHang.setSelectedItem("Theo tổng giá trị");
            lamMoiDuLieu();
        });
        cmbNhomDoanhThu.addActionListener(e -> lamMoiDuLieu());
        cmbXepKhachHang.addActionListener(e -> lamMoiDuLieu());
        btnXuat.addActionListener(e -> TaoGiaoDien.xuatCsv(this, bangHienTai(), "thong_ke.csv"));
        btnChotThuong.addActionListener(e -> chotThuong());
        txtTimNhanVien.addActionListener(e -> apDungLocNhanVien());
        GoiYTimKiem.ganTuBang(txtTimNhanVien, tblThuong, 1, m -> apDungLocNhanVien(), 1, 2);
    }
    private JPanel taoKhungTongHop() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        p.add(taoTheTongHop("Doanh thu ròng", lblDoanhThu));
        p.add(taoTheTongHop("Lợi nhuận", lblLoiNhuan));
        p.add(taoTheTongHop("Số hóa đơn ròng", lblSoHoaDon));
        p.add(taoTheTongHop("Sản phẩm bán ra", lblSanPhamBanRa));
        return p;
    }
    private JPanel taoTheTongHop(String tieuDe, JLabel giaTri) {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel t = new JLabel(tieuDe);
        t.setFont(HangSoGiaoDien.MEDIUM);
        t.setForeground(HangSoGiaoDien.MUTED);
        p.add(t, BorderLayout.NORTH);
        p.add(giaTri, BorderLayout.CENTER);
        return p;
    }
    private static JLabel taoGiaTriTongHop(String vanBan) {
        JLabel l = new JLabel(vanBan);
        l.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 22));
        l.setForeground(HangSoGiaoDien.TEXT);
        return l;
    }
    private void taoCacTab() {
        JPanel doanhThu = new JPanel(new BorderLayout(0, 10));
        doanhThu.setOpaque(false);
        bieuDo.setPreferredSize(new Dimension(100, 190));
        doanhThu.add(bieuDo, BorderLayout.NORTH);
        doanhThu.add(TaoGiaoDien.bangPhanTrang(tblDoanhThu), BorderLayout.CENTER);
        tabs.addTab("Doanh thu", doanhThu);
        tabs.addTab("Sản phẩm", TaoGiaoDien.bangPhanTrang(tblSanPham));
        tabs.addTab("Khách hàng", TaoGiaoDien.bangPhanTrang(tblKhachHang));
        JPanel doanhSoNhanVien = new JPanel(new BorderLayout(0, 8));
        doanhSoNhanVien.setOpaque(false);
        JPanel thanhTimNhanVien = TaoGiaoDien.thanhCongCu(new JLabel("Tìm nhân viên"), txtTimNhanVien);
        doanhSoNhanVien.add(thanhTimNhanVien, BorderLayout.NORTH);
        doanhSoNhanVien.add(TaoGiaoDien.bangPhanTrang(tblThuong), BorderLayout.CENTER);
        tabs.addTab("Doanh số nhân viên", doanhSoNhanVien);
        tabs.setFont(HangSoGiaoDien.NORMAL);
        tabs.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        tabs.addChangeListener(e -> capNhatKieuTab());
        taoKieuTab();
    }
    private void taoKieuTab() {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            JLabel nhan = new JLabel(tabs.getTitleAt(i), SwingConstants.CENTER);
            nhan.setOpaque(true);
            nhan.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 13));
            nhan.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            tabs.setTabComponentAt(i, nhan);
        }
        capNhatKieuTab();
    }
    private void capNhatKieuTab() {
        int dangChon = tabs.getSelectedIndex();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (!(tabs.getTabComponentAt(i) instanceof JLabel)) continue;
            JLabel nhan = (JLabel) tabs.getTabComponentAt(i);
            boolean active = i == dangChon;
            nhan.setBackground(active ? HangSoGiaoDien.ORANGE : Color.WHITE);
            nhan.setForeground(active ? Color.WHITE : HangSoGiaoDien.TEXT);
        }
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            LocalDate tu = tuNgay();
            LocalDate den = denNgay();
            if (tu.isAfter(den)) {
                throw new IllegalArgumentException("Từ ngày phải nhỏ hơn hoặc bằng đến ngày.");
            }
            List<Map<String, Object>> doanhThu = taiThongKeDoanhThu(tu, den);
            List<Map<String, Object>> sanPham = taiThongKeSanPham(tu, den);
            taiThongKeKhachHang(tu, den);
            taiThongKeThuong(tu, den);
            capNhatTongHop(doanhThu, sanPham);
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private List<Map<String, Object>> taiThongKeDoanhThu(LocalDate tu, LocalDate den) throws Exception {
        moHinhDoanhThu.setRowCount(0);
        String nhomTheo = "Theo tháng".equals(cmbNhomDoanhThu.getSelectedItem()) ? "MONTH" : ("Theo năm".equals( cmbNhomDoanhThu.getSelectedItem()) ? "YEAR" : "DAY");
        List<Map<String, Object>> cacDong = dichVuThongKe.layThongKeDoanhThu(tu, den, nhomTheo);
        for (Map<String, Object> r : cacDong) {
            moHinhDoanhThu.addRow(new Object[] {
                r.get("period"), DinhDangHienThi.tien(r.get("revenue")), DinhDangHienThi.tien(r.get("profit")), r.get("orders")
            });
        }
        bieuDo.setData(cacDong);
        return cacDong;
    }
    private List<Map<String, Object>> taiThongKeSanPham(LocalDate tu, LocalDate den) throws Exception {
        moHinhSanPham.setRowCount(0);
        List<Map<String, Object>> cacDong = dichVuThongKe.layThongKeSanPham(tu, den);
        int n = cacDong.size();
        int nhom = Math.min(5, Math.max(1, n / 2));
        for (int i = 0; i < n; i++) {
            Map<String, Object> r = cacDong.get(i);
            String xepHang = i < nhom ? "Bán chạy" : (i >= n - nhom ? "Bán chậm" : "");
            moHinhSanPham.addRow(new Object[] {
                r.get("product_name"), r.get("qty"), DinhDangHienThi.tien(r.get("revenue")), xepHang
            });
        }
        return cacDong;
    }
    private void taiThongKeKhachHang(LocalDate tu, LocalDate den) throws Exception {
        moHinhKhachHang.setRowCount(0);
        boolean xepTheoSoLan = "Theo số lần mua".equals(cmbXepKhachHang.getSelectedItem());
        for (Map<String, Object> r : dichVuThongKe.layThongKeKhachHang(tu, den, xepTheoSoLan)) {
            moHinhKhachHang.addRow(new Object[] {
                r.get("customer_id"), r.get("full_name"), r.get("phone"), r.get("purchase_count"), DinhDangHienThi.tien(r.get("spent"))
            });
        }
    }
    private void taiThongKeThuong(LocalDate tu, LocalDate den) throws Exception {
        moHinhThuong.setRowCount(0);
        for (Map<String, Object> r : dichVuThongKe.layChiTietDoanhSoNhanVien(tu, den)) {
            moHinhThuong.addRow(new Object[] {
                DinhDangHienThi.ngay(r.get("d")), r.get("employee_id"), r.get("full_name"), r.get("cnt"), DinhDangHienThi.tien(r.get("gross_sales")), DinhDangHienThi.tien(r.get("refund_amount")),
                    DinhDangHienThi.tien(r.get("net_sales")), DinhDangHienThi.tien(r.get("bonus"))
            });
        }
        apDungLocNhanVien();
        int[] rong = {
            90, 90, 160, 85, 115, 110, 120, 110
        };
        for (int i = 0; i < rong.length && i < tblThuong.getColumnModel().getColumnCount(); i++) {
            tblThuong.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
    }
    private void apDungLocNhanVien() {
        final String q = txtTimNhanVien.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            sapXepDoanhSoNhanVien.setRowFilter(null);
            return;
        }
        sapXepDoanhSoNhanVien.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel,? extends Integer> entry) {
                String ma = String.valueOf(entry.getValue(1)).toLowerCase(Locale.ROOT);
                String ten = String.valueOf(entry.getValue(2)).toLowerCase(Locale.ROOT);
                return ma.contains(q) || ten.contains(q);
            }
        });
    }
    private void capNhatTongHop(List<Map<String, Object>> doanhThu, List<Map<String, Object>> sanPham) {
        BigDecimal tongDoanhThu = BigDecimal.ZERO;
        BigDecimal tongLoiNhuan = BigDecimal.ZERO;
        long soHoaDon = 0;
        long soSanPham = 0;
        for (Map<String, Object> r : doanhThu) {
            tongDoanhThu = tongDoanhThu.add(soTien(r.get("revenue")));
            tongLoiNhuan = tongLoiNhuan.add(soTien(r.get("profit")));
            Object orders = r.get("orders");
            if (orders instanceof Number) soHoaDon += ((Number) orders).longValue();
            else if (orders != null)
                soHoaDon += Long.parseLong(String.valueOf(orders));
        }
        for (Map<String, Object> r : sanPham) {
            Object qty = r.get("qty");
            if (qty instanceof Number) soSanPham += ((Number) qty).longValue();
            else if (qty != null)
                soSanPham += Long.parseLong(String.valueOf(qty));
        }
        lblDoanhThu.setText(DinhDangHienThi.tien(tongDoanhThu));
        lblLoiNhuan.setText(DinhDangHienThi.tien(tongLoiNhuan));
        lblSoHoaDon.setText(String.valueOf(soHoaDon));
        lblSanPhamBanRa.setText(String.valueOf(soSanPham));
    }
    private BigDecimal soTien(Object giaTri) {
        if (giaTri == null) return BigDecimal.ZERO;
        if (giaTri instanceof BigDecimal) return(BigDecimal) giaTri;
        return new BigDecimal(String.valueOf(giaTri));
    }
    private JTable bangHienTai() {
        switch (tabs.getSelectedIndex()) {
            case 0:
            return tblDoanhThu;
            case 1:
            return tblSanPham;
            case 2:
            return tblKhachHang;
            default:
            return tblThuong;
        }
    }
    private LocalDate tuNgay() {
        return LocalDate.parse(txtTuNgay.getText().trim());
    }
    private LocalDate denNgay() {
        return LocalDate.parse(txtDenNgay.getText().trim());
    }
    private void chotThuong() {
        if (!xacNhan("Chốt bảng kê thưởng từ " + txtTuNgay.getText() + " đến " + txtDenNgay.getText() + "?")) {
            return;
        }
        try {
            dichVuBanHang.chotBangKeThuong(tuNgay(), denNgay());
            lamMoiDuLieu();
            thongBao("Đã chốt bảng kê thưởng doanh số.");
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private static class BieuDoDoanhThu extends JPanel {
        private static final long serialVersionUID = 1L;
        private List<String> nhan = Collections.emptyList();
        private List<BigDecimal> giaTri = Collections.emptyList();
        BieuDoDoanhThu() {
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER));
        }
        void setData(List<Map<String, Object>> cacDong) {
            List<String> l = new ArrayList<String>();
            List<BigDecimal> v = new ArrayList<BigDecimal>();
            for (Map<String, Object> r : cacDong) {
                l.add(String.valueOf(r.get("period")));
                Object x = r.get("revenue");
                v.add(x == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(x)));
            }
            nhan = l;
            giaTri = v;
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int left = 45;
            int right = 18;
            int top = 22;
            int bottom = 30;
            g2.setColor(HangSoGiaoDien.MUTED);
            g2.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 10));
            g2.drawString("Doanh thu ròng", 12, 15);
            if (giaTri.isEmpty()) {
                g2.drawString("Chưa có dữ liệu trong khoảng thời gian đã chọn.", left, Math.max(55, h / 2));
                g2.dispose();
                return;
            }
            BigDecimal max = BigDecimal.ZERO;
            for (BigDecimal v : giaTri) if (v.compareTo(max) > 0)
                max = v;
            if (max.signum() == 0) max = BigDecimal.ONE;
            int plotW = Math.max(1, w - left - right);
            int plotH = Math.max(1, h - top - bottom);
            int n = giaTri.size();
            int gap = 6;
            int barW = Math.max(4, (plotW - gap * (n + 1)) / Math.max(1, n));
            for (int i = 0; i < n; i++) {
                double ratio = giaTri.get(i).divide(max, 6, RoundingMode.HALF_UP).doubleValue();
                int bh = (int) Math.round(plotH * ratio);
                int x = left + gap + i * (barW + gap);
                int y = top + plotH - bh;
                g2.setColor(HangSoGiaoDien.ORANGE);
                g2.fillRect(x, y, barW, bh);
                if (n <= 12) {
                    g2.setColor(HangSoGiaoDien.MUTED);
                    String s = nhan.get(i);
                    if (s.length() > 10) s = s.substring(0, 10);
                    g2.drawString(s, x, h - 10);
                }
            }
            g2.setColor(HangSoGiaoDien.BORDER);
            g2.drawLine(left, top + plotH, w - right, top + plotH);
            g2.dispose();
        }
    }
}

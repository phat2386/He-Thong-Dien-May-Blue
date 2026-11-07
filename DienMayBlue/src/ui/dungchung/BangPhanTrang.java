package ui.dungchung;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
public final class BangPhanTrang extends JPanel {
    private static final long serialVersionUID = 1L;
    public static final int SO_DONG_NHIN_THAY = 7;
    private static final Integer[] CAC_MUC_DONG = { 10, 25, 50, 100 };
    private static final int SO_NUT_TRANG_TOI_DA = 7;
    private final JTable bang;
    private final JScrollPane cuon;
    private final JComboBox<Integer> cmbDongMoiTrang = new JComboBox<Integer>(CAC_MUC_DONG);
    private final JLabel lblPhamVi = new JLabel();
    private final JPanel pnlTrang = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
    private final JTextField txtTrang = new JTextField(3);
    private final JLabel lblTongTrang = new JLabel();
    private final JButton btnDi = taoNutNho("Đi");
    private final TableRowSorter<TableModel> boSapXep;
    private final boolean choPhepSapXep;
    private RowFilter<TableModel, Integer> boLocNgoai;
    private RowFilter<TableModel, Integer> boLocPhanTrangDangDung;
    private int trangHienTai = 1;
    private int dongMoiTrang = 10;
    private boolean dangCapNhat;
    @SuppressWarnings("unchecked")
    public BangPhanTrang(JTable bang) {
        super(new BorderLayout());
        this.bang = bang;
        setOpaque(false);
        if (bang.getRowSorter() instanceof TableRowSorter<?>) {
            boSapXep = (TableRowSorter<TableModel>) bang.getRowSorter();
            choPhepSapXep = true;
        } else {
            boSapXep = new TableRowSorter<TableModel>(bang.getModel());
            choPhepSapXep = false;
            for (int i = 0; i < bang.getColumnCount(); i++) boSapXep.setSortable(i, false);
            bang.setRowSorter(boSapXep);
        }
        cuon = TaoGiaoDien.cuon(bang);
        cuon.getViewport().setPreferredSize(new Dimension(400, bang.getRowHeight() * SO_DONG_NHIN_THAY));
        Dimension kichThuocCuon = cuon.getPreferredSize();
        cuon.setMinimumSize(new Dimension(120, kichThuocCuon.height));
        cuon.setMaximumSize(new Dimension(Integer.MAX_VALUE, kichThuocCuon.height));
        JPanel noiDungCoDinh = new JPanel(new BorderLayout(0, 6));
        noiDungCoDinh.setOpaque(false);
        noiDungCoDinh.add(cuon, BorderLayout.CENTER);
        noiDungCoDinh.add(taoThanhPhanTrang(), BorderLayout.SOUTH);
        noiDungCoDinh.setPreferredSize(new Dimension(400, kichThuocCuon.height + 42));
        add(noiDungCoDinh, BorderLayout.NORTH);
        cmbDongMoiTrang.setSelectedItem(Integer.valueOf(10));
        cmbDongMoiTrang.setPreferredSize(new Dimension(66, 30));
        cmbDongMoiTrang.setFont(HangSoGiaoDien.SMALL);
        txtTrang.setHorizontalAlignment(SwingConstants.CENTER);
        txtTrang.setPreferredSize(new Dimension(42, 30));
        txtTrang.setFont(HangSoGiaoDien.SMALL);
        btnDi.setPreferredSize(new Dimension(42, 30));
        cmbDongMoiTrang.addActionListener(e -> {
            if (dangCapNhat) return;
            Object chon = cmbDongMoiTrang.getSelectedItem();
            if (!(chon instanceof Integer)) return;
            dongMoiTrang = ((Integer) chon).intValue();
            trangHienTai = 1;
            capNhatPhanTrang();
        });
        txtTrang.addActionListener(e -> diDenTrangNhap());
        btnDi.addActionListener(e -> diDenTrangNhap());
        boSapXep.addRowSorterListener(e -> {
            @SuppressWarnings("unchecked")
            RowFilter<TableModel, Integer> hienTai = (RowFilter<TableModel, Integer>) boSapXep.getRowFilter();
            if (hienTai == boLocPhanTrangDangDung) return;
            boLocNgoai = hienTai;
            trangHienTai = 1;
            SwingUtilities.invokeLater(() -> capNhatPhanTrang());
        });
        bang.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (bang.getModel().getRowCount() == 0) trangHienTai = 1;
                SwingUtilities.invokeLater(() -> capNhatPhanTrang());
            }
        });
        capNhatPhanTrang();
    }
    private JPanel taoThanhPhanTrang() {
        JPanel thanh = new JPanel(new BorderLayout(8, 0));
        thanh.setOpaque(false);
        thanh.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
        JPanel trai = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        trai.setOpaque(false);
        JLabel lblDong = new JLabel("Dòng/trang");
        lblDong.setFont(HangSoGiaoDien.SMALL);
        lblPhamVi.setFont(HangSoGiaoDien.SMALL);
        lblPhamVi.setForeground(HangSoGiaoDien.MUTED);
        trai.add(lblDong);
        trai.add(cmbDongMoiTrang);
        trai.add(lblPhamVi);
        pnlTrang.setOpaque(false);
        JPanel phai = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        phai.setOpaque(false);
        JLabel lblTrang = new JLabel("Trang");
        lblTrang.setFont(HangSoGiaoDien.SMALL);
        lblTongTrang.setFont(HangSoGiaoDien.SMALL);
        phai.add(lblTrang);
        phai.add(txtTrang);
        phai.add(lblTongTrang);
        phai.add(btnDi);
        thanh.add(trai, BorderLayout.WEST);
        thanh.add(pnlTrang, BorderLayout.CENTER);
        thanh.add(phai, BorderLayout.EAST);
        return thanh;
    }
    private void capNhatPhanTrang() {
        java.util.List<Integer> cacDongSauLoc = layCacDongSauBoLocNgoai();
        int tongDong = cacDongSauLoc.size();
        int tongTrang = tinhTongTrang(tongDong);
        if (trangHienTai > tongTrang) trangHienTai = tongTrang;
        if (trangHienTai < 1) trangHienTai = 1;
        final int tu = (trangHienTai - 1) * dongMoiTrang;
        final int den = Math.min(tu + dongMoiTrang, tongDong);
        final java.util.Set<Integer> cacDongCuaTrang = new java.util.HashSet<Integer>();
        for (int i = tu; i < den; i++) cacDongCuaTrang.add(cacDongSauLoc.get(i));
        boLocPhanTrangDangDung = new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                return cacDongCuaTrang.contains(entry.getIdentifier());
            }
        };
        boSapXep.setRowFilter(boLocPhanTrangDangDung);
        if (!choPhepSapXep) {
            for (int i = 0; i < bang.getColumnCount(); i++) boSapXep.setSortable(i, false);
        }
        lblPhamVi.setText(tongDong == 0 ? "0 dòng" : (tu + 1) + " - " + den + " / " + tongDong + " dòng");
        txtTrang.setText(String.valueOf(trangHienTai));
        lblTongTrang.setText("/ " + tongTrang);
        taoLaiNutTrang(tongTrang);
        cuon.getVerticalScrollBar().setValue(0);
        revalidate();
        repaint();
    }
    private java.util.List<Integer> layCacDongSauBoLocNgoai() {
        java.util.List<Integer> ketQua = new java.util.ArrayList<Integer>();
        TableModel model = bang.getModel();
        for (int dong = 0; dong < model.getRowCount(); dong++) {
            if (boLocNgoai == null || boLocNgoai.include(taoEntry(model, dong))) ketQua.add(Integer.valueOf(dong));
        }
        return ketQua;
    }
    private static RowFilter.Entry<TableModel, Integer> taoEntry(final TableModel model, final int dong) {
        return new RowFilter.Entry<TableModel, Integer>() {
            @Override
            public TableModel getModel() {
                return model;
            }
            @Override
            public int getValueCount() {
                return model.getColumnCount();
            }
            @Override
            public Object getValue(int index) {
                return model.getValueAt(dong, index);
            }
            @Override
            public String getStringValue(int index) {
                Object value = getValue(index);
                return value == null ? "" : String.valueOf(value);
            }
            @Override
            public Integer getIdentifier() {
                return Integer.valueOf(dong);
            }
        };
    }
    private int tinhTongTrang(int tongDong) {
        return Math.max(1, (int) Math.ceil(tongDong / (double) dongMoiTrang));
    }
    private void taoLaiNutTrang(int tongTrang) {
        pnlTrang.removeAll();
        JButton truoc = taoNutNho("‹");
        truoc.setEnabled(trangHienTai > 1);
        truoc.addActionListener(e -> chuyenTrang(trangHienTai - 1));
        pnlTrang.add(truoc);
        if (tongTrang <= SO_NUT_TRANG_TOI_DA) {
            for (int i = 1; i <= tongTrang; i++) pnlTrang.add(taoNutTrang(i));
        } else if (trangHienTai <= 4) {
            for (int i = 1; i <= 5; i++) pnlTrang.add(taoNutTrang(i));
            pnlTrang.add(taoDauBaCham());
            pnlTrang.add(taoNutTrang(tongTrang));
        } else if (trangHienTai >= tongTrang - 3) {
            pnlTrang.add(taoNutTrang(1));
            pnlTrang.add(taoDauBaCham());
            for (int i = tongTrang - 4; i <= tongTrang; i++) pnlTrang.add(taoNutTrang(i));
        } else {
            pnlTrang.add(taoNutTrang(1));
            pnlTrang.add(taoDauBaCham());
            pnlTrang.add(taoNutTrang(trangHienTai - 1));
            pnlTrang.add(taoNutTrang(trangHienTai));
            pnlTrang.add(taoNutTrang(trangHienTai + 1));
            pnlTrang.add(taoDauBaCham());
            pnlTrang.add(taoNutTrang(tongTrang));
        }
        JButton sau = taoNutNho("›");
        sau.setEnabled(trangHienTai < tongTrang);
        sau.addActionListener(e -> chuyenTrang(trangHienTai + 1));
        pnlTrang.add(sau);
        pnlTrang.revalidate();
        pnlTrang.repaint();
    }
    private JButton taoNutTrang(int soTrang) {
        JButton b = taoNutNho(String.valueOf(soTrang));
        boolean dangChon = soTrang == trangHienTai;
        if (dangChon) {
            b.setForeground(Color.WHITE);
            b.setBackground(HangSoGiaoDien.ORANGE);
            b.setOpaque(true);
            b.setBorder(BorderFactory.createLineBorder(HangSoGiaoDien.ORANGE));
        }
        b.addActionListener(e -> chuyenTrang(soTrang));
        return b;
    }
    private static JLabel taoDauBaCham() {
        JLabel l = new JLabel("…");
        l.setFont(HangSoGiaoDien.SMALL);
        l.setForeground(HangSoGiaoDien.MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        return l;
    }
    private static JButton taoNutNho(String text) {
        JButton b = new JButton(text);
        b.setFont(HangSoGiaoDien.SMALL);
        b.setForeground(HangSoGiaoDien.TEXT);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setMargin(new Insets(2, 8, 2, 8));
        b.setPreferredSize(new Dimension(34, 30));
        b.setBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER));
        return b;
    }
    private void chuyenTrang(int trang) {
        int tongTrang = tinhTongTrang(layCacDongSauBoLocNgoai().size());
        if (trang < 1 || trang > tongTrang) return;
        trangHienTai = trang;
        bang.clearSelection();
        capNhatPhanTrang();
    }
    private void diDenTrangNhap() {
        try {
            int trang = Integer.parseInt(txtTrang.getText().trim());
            int tongTrang = tinhTongTrang(layCacDongSauBoLocNgoai().size());
            if (trang < 1 || trang > tongTrang) throw new NumberFormatException();
            chuyenTrang(trang);
        } catch (NumberFormatException ex) {
            Toolkit.getDefaultToolkit().beep();
            txtTrang.setText(String.valueOf(trangHienTai));
            txtTrang.selectAll();
        }
    }
    public JScrollPane getCuon() {
        return cuon;
    }
    public int getDongMoiTrang() {
        return dongMoiTrang;
    }
    public int getTrangHienTai() {
        return trangHienTai;
    }
    public void veTrangDau() {
        trangHienTai = 1;
        capNhatPhanTrang();
    }
}

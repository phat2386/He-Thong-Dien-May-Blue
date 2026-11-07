package ui.chinh;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import entity.TaiKhoan;
import service.TaiKhoanService;
import ui.dungchung.*;
import java.util.List;
public class ThanhBen extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int SIDEBAR_WIDTH = 204;
    private static final int SIDEBAR_COLLAPSED_WIDTH = 58;
    private static final int MENU_CONTENT_WIDTH = 186;
    private final KhungChinh khungChinh;
    private final Map<String, NutDieuHuong> cacNut = new LinkedHashMap<String, NutDieuHuong>();
    private final List<KhungNhomMenu> cacKhungNhom = new ArrayList<KhungNhomMenu>();
    private final JPanel dongThuongHieu = new JPanel(new BorderLayout());
    private final JPanel userArea = new JPanel();
    private final JLabel brandText = new JLabel("Điện Máy Blue");
    private final JLabel tenNguoiDung = new JLabel();
    private final JLabel tenVaiTro = new JLabel();
    private final JLabel avatar = new JLabel("", SwingConstants.CENTER);
    private boolean collapsed = false;
    public ThanhBen(KhungChinh khungChinh) {
        this.khungChinh = khungChinh;
        setBackground(HangSoGiaoDien.NAVY);
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 900));
        setLayout(new BorderLayout());
        taoThuongHieu();
        taoDieuHuong();
        taoKhuVucNguoiDung();
    }
    private void taoThuongHieu() {
        dongThuongHieu.setOpaque(false);
        dongThuongHieu.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 52));
        dongThuongHieu.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 20)), new EmptyBorder(0, 10, 0, 10)));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        KhungBoGoc logo = new KhungBoGoc(9, HangSoGiaoDien.ORANGE);
        logo.setVeVien(false);
        logo.setPreferredSize(new Dimension(28, 28));
        logo.setMaximumSize(new Dimension(28, 28));
        logo.setLayout(new GridBagLayout());
        JLabel b = new JLabel("B");
        b.setForeground(Color.WHITE);
        b.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 13));
        logo.add(b);
        brandText.setForeground(Color.WHITE);
        brandText.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 13));
        brandText.setBorder(new EmptyBorder(0, 9, 0, 0));
        left.add(logo);
        left.add(brandText);
        JButton collapse = new JButton("‹");
        collapse.setForeground(new Color(255, 255, 255, 105));
        collapse.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 20));
        collapse.setContentAreaFilled(false);
        collapse.setBorderPainted(false);
        collapse.setFocusPainted(false);
        collapse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        collapse.addActionListener(e -> setThuGon(!collapsed));
        dongThuongHieu.add(left, BorderLayout.CENTER);
        dongThuongHieu.add(collapse, BorderLayout.EAST);
        add(dongThuongHieu, BorderLayout.NORTH);
    }
    private void taoDieuHuong() {
        JPanel nav = new JPanel();
        nav.setBackground(HangSoGiaoDien.NAVY);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(8, 6, 8, 6));
        if (khungChinh.coTheTruyCap(KhungChinh.MAN_HINH_TRANG_CHU)) {
            NutDieuHuong dashboard = taoNutDieuHuong("Trang chủ", KhungChinh.MAN_HINH_TRANG_CHU, null, false);
            nav.add(dashboard);
            nav.add(Box.createVerticalStrut(4));
        }
        for (NhomMenu nhom : taoNhomTheoVaiTro()) {
            KhungNhomMenu khungNhom = new KhungNhomMenu(nhom.tieuDe, nhom.bieuTuong);
            for (MucMenu muc : nhom.cacMuc) {
                if (!khungChinh.coTheTruyCap(muc.maManHinh)) continue;
                NutDieuHuong mucCon = taoNutDieuHuong(muc.nhan, muc.maManHinh, muc.bieuTuong, true);
                khungNhom.addItem(muc.maManHinh, mucCon);
            }
            if (khungNhom.getItemCount() > 0) {
                cacKhungNhom.add(khungNhom);
                nav.add(khungNhom);
                nav.add(Box.createVerticalStrut(2));
            }
        }
        JScrollPane cuon = new JScrollPane(nav);
        cuon.setBorder(null);
        cuon.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cuon.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        cuon.getViewport().setBackground(HangSoGiaoDien.NAVY);
        cuon.getVerticalScrollBar().setUnitIncrement(12);
        add(cuon, BorderLayout.CENTER);
    }
    private NutDieuHuong taoNutDieuHuong(String nhan, String maManHinh, String bieuTuong, boolean mucCon) {
        NutDieuHuong nut = new NutDieuHuong(nhan, mucCon ? null : bieuTuong);
        cacNut.put(maManHinh, nut);
        if (mucCon) {
            nut.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 10));
            nut.setPreferredSize(new Dimension(MENU_CONTENT_WIDTH, 29));
            nut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
            nut.setMargin(new Insets(0, 14, 0, 4));
            nut.setIconTextGap(0);
            nut.setToolTipText(nhan);
            nut.setAlignmentX(Component.LEFT_ALIGNMENT);
        } else {
            nut.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 11));
            nut.setPreferredSize(new Dimension(MENU_CONTENT_WIDTH, 36));
            nut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            nut.setMargin(new Insets(0, 9, 0, 7));
            nut.setIconTextGap(8);
        }
        nut.addActionListener(ev -> khungChinh.hienManHinh(maManHinh, nhan));
        return nut;
    }
    private void taoKhuVucNguoiDung() {
        TaiKhoan a = TaiKhoanService.getTaiKhoanHienTai();
        userArea.setBackground(HangSoGiaoDien.NAVY);
        userArea.setLayout(new BoxLayout(userArea, BoxLayout.Y_AXIS));
        userArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 20)), new EmptyBorder(7, 8, 8, 8)));
        KhungBoGoc nutNguoiDung = new KhungBoGoc(12, new Color(255, 255, 255, 18));
        nutNguoiDung.setMauVien(new Color(255, 255, 255, 25));
        nutNguoiDung.setLayout(new BorderLayout(9, 0));
        nutNguoiDung.setBorder(new EmptyBorder(8, 8, 8, 8));
        nutNguoiDung.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatar.setPreferredSize(new Dimension(38, 38));
        avatar.setOpaque(false);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 11));
        String vietTat = a == null ? "" : vietTat(a.getNhanVien().getHoTen());
        avatar.setText(vietTat);
        avatar.setIcon(null);
        avatar.setHorizontalTextPosition(SwingConstants.CENTER);
        avatar.setVerticalTextPosition(SwingConstants.CENTER);
        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        tenNguoiDung.setText(a == null ? "" : a.getNhanVien().getHoTen());
        tenNguoiDung.setForeground(Color.WHITE);
        tenNguoiDung.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 11));
        tenVaiTro.setText(a == null ? "" : nhanVaiTro(a.getVaiTro().getMaQuyenVaiTro()));
        tenVaiTro.setForeground(new Color(255, 255, 255, 110));
        tenVaiTro.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 10));
        txt.add(tenNguoiDung);
        txt.add(Box.createVerticalStrut(2));
        txt.add(tenVaiTro);
        nutNguoiDung.add(avatar, BorderLayout.WEST);
        nutNguoiDung.add(txt, BorderLayout.CENTER);
        MouseAdapter profileClick = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                khungChinh.hienManHinh(KhungChinh.MAN_HINH_HO_SO, "Hồ sơ cá nhân");
            }
        };
        nutNguoiDung.addMouseListener(profileClick);
        avatar.addMouseListener(profileClick);
        txt.addMouseListener(profileClick);
        NutDieuHuong dangXuat = new NutDieuHuong("Đăng xuất", null);
        dangXuat.setForeground(new Color(255, 255, 255, 128));
        dangXuat.addActionListener(e -> khungChinh.yeuCauDangXuat());
        userArea.add(nutNguoiDung);
        userArea.add(Box.createVerticalStrut(4));
        userArea.add(dangXuat);
        add(userArea, BorderLayout.SOUTH);
    }
    public void setThuGon(boolean c) {
        collapsed = c;
        setPreferredSize(new Dimension(c ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_WIDTH, getHeight()));
        brandText.setVisible(!c);
        Component[] comps = userArea.getComponents();
        if (comps.length > 0 && comps[0] instanceof JPanel) {
            JPanel user = (JPanel) comps[0];
            if (user.getLayout() instanceof BorderLayout) {
                Component trungTam = ((BorderLayout) user.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                if (trungTam != null) trungTam.setVisible(!c);
            }
        }
        for (NutDieuHuong b : cacNut.values()) b.setThuGon(c);
        for (KhungNhomMenu nhom : cacKhungNhom) nhom.setSidebarCollapsed(c);
        if (comps.length > 2 && comps[2] instanceof NutDieuHuong) ((NutDieuHuong) comps[2]).setThuGon(c);
        revalidate();
        repaint();
        khungChinh.revalidate();
    }
    public boolean isThuGon() {
        return collapsed;
    }
    public void setDangChon(String maManHinh) {
        for (Map.Entry<String, NutDieuHuong> e : cacNut.entrySet()) e.getValue().setDangChon(e.getKey().equals(maManHinh));
        KhungNhomMenu nhomChua = null;
        for (KhungNhomMenu nhom : cacKhungNhom) {
            nhom.setNhomHoatDong(nhom.containsScreen(maManHinh));
            if (nhom.containsScreen(maManHinh)) nhomChua = nhom;
        }
        if (!collapsed) {
            if (nhomChua != null) {
                chiMo(nhomChua);
            } else if (KhungChinh.MAN_HINH_TRANG_CHU.equals(maManHinh)) {
                dongTatCaNhom();
            }
        }
    }
    private void chiMo(KhungNhomMenu maDangChon) {
        for (KhungNhomMenu nhom : cacKhungNhom) nhom.setExpanded(nhom == maDangChon);
    }
    private void dongTatCaNhom() {
        for (KhungNhomMenu nhom : cacKhungNhom) nhom.setExpanded(false);
    }
    private String vietTat(String ten) {
        if (ten == null || ten.trim().isEmpty()) return "";
        String[] p = ten.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, 1).toUpperCase();
        return(p[0].substring(0, 1) + p[p.length - 1].substring(0, 1)).toUpperCase();
    }
    private String nhanVaiTro(String vaiTro) {
        if ("ADMIN".equals(vaiTro)) return "Quản trị hệ thống";
        if ("MANAGER".equals(vaiTro)) return "Nhân viên quản lý";
        if ("SALES".equals(vaiTro)) return "Nhân viên bán hàng";
        return "Nhân viên CSKH";
    }
    private List<NhomMenu> taoNhomTheoVaiTro() {
        List<NhomMenu> groups = new ArrayList<NhomMenu>();
        groups.add(new NhomMenu("Nhân viên", null).add("Quản lý nhân viên", KhungChinh.MAN_HINH_NHAN_VIEN, null).add("Quản lý ca làm việc", KhungChinh.MAN_HINH_CA_LAM, null).add("Quản lý tài khoản", KhungChinh.MAN_HINH_TAI_KHOAN, null));
        groups.add(new NhomMenu("Khách hàng", null).add("Quản lý khách hàng", KhungChinh.MAN_HINH_KHACH_HANG, null).add("Quản lý hạng thành viên", KhungChinh.MAN_HINH_THANH_VIEN, null).add("Quản lý bảo hành",
            KhungChinh.MAN_HINH_BAO_HANH, null).add("Quản lý khiếu nại", KhungChinh.MAN_HINH_KHIEU_NAI, null));
        groups.add(new NhomMenu("Sản phẩm", null).add("Quản lý sản phẩm", KhungChinh.MAN_HINH_SAN_PHAM, null).add("Quản lý khuyến mãi", KhungChinh.MAN_HINH_KHUYEN_MAI, null).add("Quản lý nhà cung cấp", KhungChinh.MAN_HINH_NHA_CUNG_CAP, null));
        groups.add(new NhomMenu("Hóa đơn", null).add("Lập hóa đơn bán hàng", KhungChinh.MAN_HINH_LAP_HOA_DON, null).add("Quản lý hóa đơn", KhungChinh.MAN_HINH_BAN_HANG, null).add("Lập phiếu đổi trả",
            KhungChinh.MAN_HINH_LAP_DOI_TRA, null).add("Duyệt hoàn tiền", KhungChinh.MAN_HINH_HOAN_TIEN, null));
        groups.add(new NhomMenu("Phiếu giao hàng", null).add("Lập phiếu giao hàng", KhungChinh.MAN_HINH_LAP_GIAO_HANG, null).add("Quản lý phiếu giao hàng", KhungChinh.MAN_HINH_GIAO_HANG, null));
        groups.add(new NhomMenu("Thống kê", null).add("Thống kê kinh doanh", KhungChinh.MAN_HINH_THONG_KE, null));
        return groups;
    }
    private static class MucMenu {
        final String nhan;
        final String maManHinh;
        final String bieuTuong;
        MucMenu(String nhan, String maManHinh, String bieuTuong) {
            this.nhan = nhan;
            this.maManHinh = maManHinh;
            this.bieuTuong = bieuTuong;
        }
    }
    private static class NhomMenu {
        final String tieuDe;
        final String bieuTuong;
        final List<MucMenu> cacMuc = new ArrayList<MucMenu>();
        NhomMenu(String tieuDe, String bieuTuong) {
            this.tieuDe = tieuDe;
            this.bieuTuong = bieuTuong;
        }
        NhomMenu add(String nhan, String maManHinh, String bieuTuong) {
            cacMuc.add(new MucMenu(nhan, maManHinh, bieuTuong));
            return this;
        }
    }
    private class KhungNhomMenu extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JPanel header = new JPanel(new BorderLayout(8, 0));
        private final JLabel groupIcon = new JLabel();
        private final JLabel lblTieuDe = new JLabel();
        private final JLabel lblMuiTen = new JLabel("+", SwingConstants.CENTER);
        private final JPanel pnlDanhSach = new JPanel();
        private final Map<String, NutDieuHuong> cacNutMuc = new LinkedHashMap<String, NutDieuHuong>();
        private boolean expanded = false;
        private boolean thanhBenThuGon = false;
        private boolean nhomHoatDong = false;
        KhungNhomMenu(String tieuDe, String bieuTuong) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
            header.setOpaque(false);
            header.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.setBorder(new EmptyBorder(0, 8, 0, 5));
            header.setPreferredSize(new Dimension(MENU_CONTENT_WIDTH, 36));
            header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            groupIcon.putClientProperty("iconKind", bieuTuong);
            groupIcon.setPreferredSize(new Dimension(0, 0));
            groupIcon.setVisible(false);
            lblTieuDe.setText(tieuDe);
            lblTieuDe.setForeground(new Color(189, 204, 220));
            lblTieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 11));
            lblMuiTen.setForeground(new Color(189, 204, 220));
            lblMuiTen.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 16));
            lblMuiTen.setPreferredSize(new Dimension(15, 18));
            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
            left.add(lblTieuDe);
            header.add(left, BorderLayout.CENTER);
            header.add(lblMuiTen, BorderLayout.EAST);
            pnlDanhSach.setOpaque(false);
            pnlDanhSach.setAlignmentX(Component.LEFT_ALIGNMENT);
            pnlDanhSach.setLayout(new BoxLayout(pnlDanhSach, BoxLayout.Y_AXIS));
            pnlDanhSach.setBorder(new EmptyBorder(0, 0, 2, 0));
            pnlDanhSach.setVisible(false);
            MouseAdapter headerMouse = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (thanhBenThuGon) {
                        ThanhBen.this.setThuGon(false);
                        chiMo(KhungNhomMenu.this);
                    } else if (expanded) {
                        setExpanded(false);
                    } else {
                        chiMo(KhungNhomMenu.this);
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    header.setBackground(new Color(255, 255, 255, 14));
                    header.setOpaque(true);
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    header.setOpaque(false);
                    repaint();
                }
            };
            header.addMouseListener(headerMouse);
            left.addMouseListener(headerMouse);
            groupIcon.addMouseListener(headerMouse);
            lblTieuDe.addMouseListener(headerMouse);
            lblMuiTen.addMouseListener(headerMouse);
            add(header);
            add(pnlDanhSach);
        }
        @Override
        public Dimension getMaximumSize() {
            Dimension kichThuocUuTien = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, kichThuocUuTien.height);
        }
        void addItem(String maManHinh, NutDieuHuong nut) {
            cacNutMuc.put(maManHinh, nut);
            pnlDanhSach.add(nut);
            pnlDanhSach.add(Box.createVerticalStrut(1));
        }
        int getItemCount() {
            return cacNutMuc.size();
        }
        boolean containsScreen(String maManHinh) {
            return cacNutMuc.containsKey(maManHinh);
        }
        void setNhomHoatDong(boolean hoatDong) {
            nhomHoatDong = hoatDong;
            Color c = hoatDong ? Color.WHITE : new Color(189, 204, 220);
            lblTieuDe.setForeground(c);
            lblMuiTen.setForeground(c);
            header.setOpaque(false);
            repaint();
        }
        void setExpanded(boolean giaTri) {
            expanded = giaTri;
            lblMuiTen.setText(giaTri ? "-" : "+");
            pnlDanhSach.setVisible(giaTri && !thanhBenThuGon);
            revalidate();
            repaint();
        }
        void setSidebarCollapsed(boolean giaTri) {
            thanhBenThuGon = giaTri;
            lblTieuDe.setVisible(!giaTri);
            lblMuiTen.setVisible(!giaTri);
            pnlDanhSach.setVisible(expanded && !giaTri);
            header.setBorder(giaTri ? new EmptyBorder(0, 8, 0, 8) : new EmptyBorder(0, 8, 0, 5));
            header.setPreferredSize(new Dimension(giaTri ? 40 : MENU_CONTENT_WIDTH, 36));
            header.setMaximumSize(new Dimension(giaTri ? 40 : Integer.MAX_VALUE, 36));
            header.setOpaque(false);
            if (!giaTri) {
                for (NutDieuHuong mucCon : cacNutMuc.values()) {
                    mucCon.setFont(HangSoGiaoDien.phongChu(Font.PLAIN, 10));
                    mucCon.setPreferredSize(new Dimension(MENU_CONTENT_WIDTH, 29));
                    mucCon.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
                    mucCon.setMargin(new Insets(0, 14, 0, 4));
                    mucCon.setIconTextGap(0);
                    mucCon.setHorizontalAlignment(SwingConstants.LEFT);
                    mucCon.setAlignmentX(Component.LEFT_ALIGNMENT);
                }
            }
            revalidate();
            repaint();
        }
    }
}

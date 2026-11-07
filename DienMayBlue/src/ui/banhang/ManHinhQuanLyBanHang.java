package ui.banhang;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.util.*;
import java.time.*;
import java.time.format.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import entity.*;
import service.*;
import ui.chinh.KhungChinh;
import ui.dungchung.*;
import java.util.List;
public class ManHinhQuanLyBanHang extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final HoaDonService dichVu = new HoaDonService();
    private final JTextField txtTimKiem = TaoGiaoDien.oNhap(260);
    private final JTextField txtTuNgay = TaoGiaoDien.oNhap(105);
    private final JTextField txtDenNgay = TaoGiaoDien.oNhap(105);
    private final JLabel lblTongHoaDon = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinh = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã hóa đơn", "Mã KH", "Khách hàng", "Mã NV bán", "Nhân viên", "Ngày lập", "Phương thức thanh toán", "Tổng tiền"
    });
    private final JTable tblHoaDon = TaoGiaoDien.bang(moHinh);
    public ManHinhQuanLyBanHang(KhungChinh khungChinh) {
        super("Quản lý hóa đơn", "Tra cứu, theo dõi và xem chi tiết các hóa đơn bán hàng.");
        TaoGiaoDien.datChuGoiY(txtTimKiem, "Mã hóa đơn, khách hàng hoặc nhân viên");
        JButton btnXemChiTiet = TaoGiaoDien.nutChinh("Xem chi tiết");
        JButton btnXuatCsv = TaoGiaoDien.nutPhu("Xuất CSV");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        JButton btnLocNgay = TaoGiaoDien.nutPhu("Lọc ngày");
        TaoGiaoDien.datChuGoiY(txtTuNgay, "dd/MM/yyyy");
        TaoGiaoDien.datChuGoiY(txtDenNgay, "dd/MM/yyyy");
        thaoTac.add(btnXemChiTiet);
        thaoTac.add(btnXuatCsv);
        thaoTac.add(btnLamMoi);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(taoKhungTongHop(), BorderLayout.NORTH);
        JPanel trungTam = new JPanel(new BorderLayout(0, 10));
        trungTam.setOpaque(false);
        JPanel boLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        boLoc.setOpaque(false);
        boLoc.add(nhomLoc("Tìm kiếm", txtTimKiem));
        boLoc.add(nhomLoc("Từ ngày", txtTuNgay));
        boLoc.add(nhomLoc("Đến ngày", txtDenNgay));
        boLoc.add(btnLocNgay);
        trungTam.add(boLoc, BorderLayout.NORTH);
        BangPhanTrang cuon = TaoGiaoDien.bangPhanTrang(tblHoaDon);
        tblHoaDon.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        datDoRongCot();
        trungTam.add(cuon, BorderLayout.CENTER);
        khung.add(trungTam, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimKiem, tblHoaDon, 0, m -> lamMoiDuLieu(), 0, 1, 2, 3, 4);
        btnXemChiTiet.addActionListener(e -> xemChiTietDangChon());
        btnLocNgay.addActionListener(e -> lamMoiDuLieu());
        txtTuNgay.addActionListener(e -> lamMoiDuLieu());
        txtDenNgay.addActionListener(e -> lamMoiDuLieu());
        btnXuatCsv.addActionListener(e -> TaoGiaoDien.xuatCsv(this, tblHoaDon, "danh_sach_hoa_don.csv"));
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            txtTuNgay.setText("");
            txtDenNgay.setText("");
            lamMoiDuLieu();
        });
        tblHoaDon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) xemChiTietDangChon();
            }
        });
    }
    private JPanel taoKhungTongHop() {
        JPanel p = new JPanel(new GridLayout(1, 1, 10, 0));
        p.setOpaque(false);
        p.add(taoTheTongHop("Tổng số hóa đơn đã thanh toán", lblTongHoaDon));
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
        l.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 24));
        l.setForeground(HangSoGiaoDien.TEXT);
        return l;
    }
    private void datDoRongCot() {
        int[] rong = {
            105, 85, 170, 95, 165, 150, 170, 130
        };
        for (int i = 0; i < rong.length && i < tblHoaDon.getColumnCount(); i++) {
            tblHoaDon.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            List<HoaDon> tatCa = dichVu.layDanhSachHoaDonQuanLy();
            capNhatTongHop(tatCa);
            String tuKhoa = txtTimKiem.getText().trim().toLowerCase();
            LocalDate tuNgay = docNgayLoc(txtTuNgay.getText());
            LocalDate denNgay = docNgayLoc(txtDenNgay.getText());
            moHinh.setRowCount(0);
            for (HoaDon r : tatCa) {
                LocalDate ngayLap = r.getNgayLap() == null ? null : r.getNgayLap().toLocalDate();
                if (tuNgay != null && (ngayLap == null || ngayLap.isBefore(tuNgay))) continue;
                if (denNgay != null && (ngayLap == null || ngayLap.isAfter(denNgay))) continue;
                String ma = r.getMaHoaDon();
                String khachHang = r.getKhachHang() == null ? "" : r.getKhachHang().getHoTen();
                String nhanVien = r.getNhanVien() == null ? "" : r.getNhanVien().getHoTen();
                if (!tuKhoa.isEmpty() && !ma.toLowerCase().contains(tuKhoa) && !khachHang.toLowerCase().contains( tuKhoa) && !nhanVien.toLowerCase().contains(tuKhoa)) {
                    continue;
                }
                moHinh.addRow(new Object[] {
                    ma, r.getKhachHang() == null ? "" : r.getKhachHang().getMaKhachHang(), khachHang,
                    r.getNhanVien() == null ? "" : r.getNhanVien().getMaNhanVien(), nhanVien, DinhDangHienThi.ngay(r.getNgayLap()),
                    nhanPhuongThuc(r.getPhuongThucThanhToan()), DinhDangHienThi.tien(r.getTongTienThuan())
                });
            }
            if (!tuKhoa.isEmpty() && moHinh.getRowCount() > 0) {
                TaoGiaoDien.chonDongDauTienSauKhiCapNhat(tblHoaDon);
            }
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private JPanel nhomLoc(String nhan, JComponent truong) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setOpaque(false);
        p.add(new JLabel(nhan));
        p.add(truong);
        return p;
    }
    private LocalDate docNgayLoc(String vanBan) {
        String s = vanBan == null ? "" : vanBan.trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
    private void capNhatTongHop(List<HoaDon> tatCa) {
        lblTongHoaDon.setText(String.valueOf(tatCa.size()));
    }
    private String maHoaDonDangChon() {
        int vr = tblHoaDon.getSelectedRow();
        if (vr < 0) return null;
        return String.valueOf(moHinh.getValueAt(tblHoaDon.convertRowIndexToModel(vr), 0));
    }
    private void xemChiTietDangChon() {
        String maHoaDon = maHoaDonDangChon();
        if (maHoaDon == null) {
            thongBao("Hãy chọn hóa đơn cần xem.");
            return;
        }
        try {
            HoaDon hoaDon = dichVu.layChiTietHoaDonQuanLy(maHoaDon);
            if (hoaDon == null) {
                thongBao("Không tìm thấy hóa đơn.");
                return;
            }
            JPanel thongTin = new JPanel(new GridBagLayout());
            thongTin.setBackground(Color.WHITE);
            thongTin.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            themDong(thongTin, 0, "Mã hóa đơn", hoaDon.getMaHoaDon());
            themDong(thongTin, 1, "Ngày lập", DinhDangHienThi.ngay(hoaDon.getNgayLap()));
            themDong(thongTin, 2, "Khách hàng", hoaDon.getKhachHang() == null ? "" : hoaDon.getKhachHang().getHoTen());
            themDong(thongTin, 3, "Số điện thoại", hoaDon.getKhachHang() == null ? "" : hoaDon.getKhachHang().getSoDienThoai());
            themDong(thongTin, 4, "Nhân viên", hoaDon.getNhanVien() == null ? "" : hoaDon.getNhanVien().getHoTen());
            themDong(thongTin, 5, "Phương thức thanh toán", nhanPhuongThuc(hoaDon.getPhuongThucThanhToan()));
            themDong(thongTin, 6, "Tạm tính", DinhDangHienThi.tien(hoaDon.getTamTinh()));
            themDong(thongTin, 7, "Tiền giảm", DinhDangHienThi.tien(hoaDon.getTienGiam()));
            themDong(thongTin, 8, "Tổng tiền", DinhDangHienThi.tien(hoaDon.getTongTien()));
            DefaultTableModel chiTietModel = TaoGiaoDien.moHinhChiDoc(new Object[] {
                "Mã SP", "Sản phẩm", "SL", "Đơn giá", "Giảm", "Thành tiền"
            });
            JTable chiTietTable = TaoGiaoDien.bang(chiTietModel);
            chiTietTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (ChiTietHoaDon r : dichVu.layDongChiTietHoaDonQuanLy(maHoaDon)) {
                chiTietModel.addRow(new Object[] {
                    r.getSanPham().getMaSanPham(), r.getSanPham().getTenSanPham(), r.getSoLuong(), DinhDangHienThi.tien(r.getDonGia()), DinhDangHienThi.tien(r.getTienGiam()), DinhDangHienThi.tien(r.getThanhTien())
                });
            }
            int[] rongCotChiTiet = {100, 300, 70, 130, 120, 140};
            for (int i = 0; i < rongCotChiTiet.length && i < chiTietTable.getColumnCount(); i++) {
                chiTietTable.getColumnModel().getColumn(i).setPreferredWidth(rongCotChiTiet[i]);
            }
            JScrollPane cuonChiTiet = TaoGiaoDien.cuon(chiTietTable);
            int soDongHienThi = Math.max(4, Math.min(8, chiTietTable.getRowCount()));
            int caoBang = chiTietTable.getTableHeader().getPreferredSize().height + soDongHienThi * chiTietTable.getRowHeight() + 4;
            cuonChiTiet.setPreferredSize(new Dimension(880, caoBang));

            JPanel tong = new JPanel(new BorderLayout(0, 10));
            tong.setBackground(Color.WHITE);
            tong.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            tong.setPreferredSize(new Dimension(900, Math.min(600, 245 + caoBang)));
            tong.add(thongTin, BorderLayout.NORTH);
            // Chỉ hiển thị bảng chi tiết. Không dùng BangPhanTrang để tránh các ô chọn số/trang xuất hiện trong dialog.
            tong.add(cuonChiTiet, BorderLayout.CENTER);
            JOptionPane.showMessageDialog(TienIchHopThoai.chuSoHuu(this), tong, "Chi tiết hóa đơn " + maHoaDon, JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void themDong(JPanel p, int dong, String nhan, Object giaTri) {
        GridBagConstraints gbcNhan = new GridBagConstraints();
        gbcNhan.gridx = 0;
        gbcNhan.gridy = dong;
        gbcNhan.weightx = 0;
        gbcNhan.anchor = GridBagConstraints.WEST;
        gbcNhan.fill = GridBagConstraints.NONE;
        gbcNhan.insets = new Insets(3, 0, 3, 18);
        JLabel l = new JLabel(nhan);
        l.setForeground(HangSoGiaoDien.MUTED);
        l.setPreferredSize(new Dimension(180, 22));
        p.add(l, gbcNhan);

        GridBagConstraints gbcGiaTri = new GridBagConstraints();
        gbcGiaTri.gridx = 1;
        gbcGiaTri.gridy = dong;
        gbcGiaTri.weightx = 1;
        gbcGiaTri.anchor = GridBagConstraints.WEST;
        gbcGiaTri.fill = GridBagConstraints.HORIZONTAL;
        gbcGiaTri.insets = new Insets(3, 0, 3, 0);
        JLabel v = new JLabel(DinhDangHienThi.anToan(giaTri));
        v.setForeground(HangSoGiaoDien.TEXT);
        v.setToolTipText(v.getText());
        p.add(v, gbcGiaTri);
    }
    private String nhanPhuongThuc(Object giaTri) {
        String s = DinhDangHienThi.anToan(giaTri);
        if (s.isEmpty()) return "-";
        if ("Tiền mặt".equalsIgnoreCase(s)) return "Tiền mặt";
        if ("TRANSFER".equalsIgnoreCase(s)) return "Chuyển khoản";
        return s;
    }
}

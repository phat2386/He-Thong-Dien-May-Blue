package ui.khachhang;

import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.*;
import entity.*;
import ui.dungchung.*;
import java.util.List;

public class ManHinhQuanLyThanhVien extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final TheThanhVienService dichVu = new TheThanhVienService();
    private final KhachHangService dichVuKhachHang = new KhachHangService();
    private final JTextField txtTimKiem = TaoGiaoDien.oNhap(320);
    private final JComboBox<String> cmbHang = TaoGiaoDien.oChon("Tất cả hạng", "Tiêu chuẩn", "Bạc", "Vàng", "Bạch kim");
    private final JComboBox<String> cmbTrangThai = TaoGiaoDien.oChon("Tất cả trạng thái", "Đang hoạt động",
            "Đã vô hiệu hóa", "Đã mất");
    private final JLabel lblTongThe = taoGiaTriTongHop("0");
    private final JLabel lblDangHoatDong = taoGiaTriTongHop("0");
    private final JLabel lblDaVoHieu = taoGiaTriTongHop("0");
    private final JLabel lblBachKim = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinh = TaoGiaoDien.moHinhChiDoc(new Object[] {
            "Mã thẻ", "Mã KH", "Khách hàng", "Số điện thoại", "Hạng thành viên", "Điểm tích lũy", "Ngày cấp",
            "Ngày vô hiệu", "Trạng thái"
    });
    private final JTable tblThanhVien = TaoGiaoDien.bang(moHinh);

    public ManHinhQuanLyThanhVien() {
        super("Quản lý thẻ thành viên",
                "Quản lý thẻ, điểm tích lũy, hạng thành viên và trạng thái thẻ của khách hàng.");
        TaoGiaoDien.datChuGoiY(txtTimKiem, "Mã thẻ, mã KH, họ tên hoặc SĐT");
        JButton btnCapThe = TaoGiaoDien.nutChinh("Cấp thẻ / Cấp lại thẻ");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        thaoTac.add(btnCapThe);
        thaoTac.add(btnLamMoi);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(taoKhungTongHop(), BorderLayout.NORTH);
        JPanel trungTam = new JPanel(new BorderLayout(0, 10));
        trungTam.setOpaque(false);
        trungTam.add(TaoGiaoDien.thanhCongCu(new JLabel("Tìm kiếm"), txtTimKiem, new JLabel("Hạng"), cmbHang,
                new JLabel("Trạng thái"), cmbTrangThai), BorderLayout.NORTH);
        BangPhanTrang cuon = TaoGiaoDien.bangPhanTrang(tblThanhVien);
        tblThanhVien.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        datDoRongCot();
        trungTam.add(cuon, BorderLayout.CENTER);
        khung.add(trungTam, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimKiem, tblThanhVien, 0, m -> lamMoiDuLieu(), 0, 1, 2, 3);
        cmbHang.addActionListener(e -> lamMoiDuLieu());
        cmbTrangThai.addActionListener(e -> lamMoiDuLieu());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu());
        btnCapThe.addActionListener(e -> capTheHoacCapLai());
    }

    private JPanel taoKhungTongHop() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        p.add(taoTheTongHop("Tổng số thẻ", lblTongThe));
        p.add(taoTheTongHop("Đang hoạt động", lblDangHoatDong));
        p.add(taoTheTongHop("Đã vô hiệu hóa", lblDaVoHieu));
        p.add(taoTheTongHop("Bạch kim đang hoạt động", lblBachKim));
        return p;
    }

    private JPanel taoTheTongHop(String tieuDe, JLabel giaTri) {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
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
                150, 85, 190, 120, 130, 105, 105, 110, 135
        };
        for (int i = 0; i < rong.length && i < tblThanhVien.getColumnCount(); i++) {
            tblThanhVien.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
    }

    @Override
    public void lamMoiDuLieu() {
        try {
            List<TheThanhVien> tatCa = dichVu.khachHangThanhVien("%");
            capNhatTongHop(tatCa);
            String tuKhoa = txtTimKiem.getText().trim().toLowerCase();
            String hangLoc = "Tất cả hạng".equals(String.valueOf(cmbHang.getSelectedItem())) ? null
                    : String.valueOf(cmbHang.getSelectedItem());
            String trangThaiLoc = maTrangThaiDangChon();
            moHinh.setRowCount(0);
            for (TheThanhVien r : tatCa) {
                String maThe = r.getMaTheHienThi();
                String maKhachHang = r.getKhachHang().getMaKhachHang();
                String hoTen = r.getKhachHang().getHoTen();
                String dienThoai = DinhDangHienThi.anToan(r.getKhachHang().getSoDienThoai());
                String hang = DinhDangHienThi.anToan(r.getHangThanhVien());
                String trangThai = r.getTrangThai();
                if (!tuKhoa.isEmpty()) {
                    String haystack = (maThe + " " + maKhachHang + " " + hoTen + " " + dienThoai).toLowerCase();
                    if (!haystack.contains(tuKhoa))
                        continue;
                }
                if (hangLoc != null && !hangLoc.equals(hang))
                    continue;
                if (trangThaiLoc != null && !trangThaiLoc.equals(trangThai))
                    continue;
                moHinh.addRow(new Object[] {
                        maThe, maKhachHang, hoTen, dienThoai, hang, r.getDiemTichLuy(),
                        DinhDangHienThi.ngay(r.getNgayCap()), DinhDangHienThi.ngay(r.getNgayVoHieu()),
                        nhanTrangThai(trangThai)
                });
            }
            if (!tuKhoa.isEmpty() && moHinh.getRowCount() > 0) {
                TaoGiaoDien.chonDongDauTienSauKhiCapNhat(tblThanhVien);
            }
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }

    private void capNhatTongHop(List<TheThanhVien> cacDong) {
        int active = 0;
        int invalidated = 0;
        int platinum = 0;
        for (TheThanhVien r : cacDong) {
            String trangThai = r.getTrangThai();
            String hang = r.getHangThanhVien();
            if ("Hoạt động".equals(trangThai)) {
                active++;
                if ("Bạch kim".equals(hang))
                    platinum++;
            } else if ("Đã vô hiệu hóa".equals(trangThai)) {
                invalidated++;
            }
        }
        lblTongThe.setText(String.valueOf(cacDong.size()));
        lblDangHoatDong.setText(String.valueOf(active));
        lblDaVoHieu.setText(String.valueOf(invalidated));
        lblBachKim.setText(String.valueOf(platinum));
    }

    private String maTrangThaiDangChon() {
        String chon = String.valueOf(cmbTrangThai.getSelectedItem());
        if ("Đang hoạt động".equals(chon))
            return "Hoạt động";
        if ("Đã vô hiệu hóa".equals(chon))
            return "Đã vô hiệu hóa";
        if ("Đã mất".equals(chon))
            return "Đã mất";
        return null;
    }

    private void capTheHoacCapLai() {
        try {
            JComboBox<String> cboKhachHang = TaoGiaoDien.oChon();
            Map<String, String> maKhachTheoDong = new LinkedHashMap<String, String>();
            Map<String, KhachHang> duLieuTheoMa = new LinkedHashMap<String, KhachHang>();
            for (KhachHang r : dichVuKhachHang.timKiemKhachHang("%", null)) {
                if (!"Hoạt động".equals(r.getTrangThai()))
                    continue;
                String ma = r.getMaKhachHang();
                String dong = ma + " - " + r.getHoTen();
                cboKhachHang.addItem(dong);
                maKhachTheoDong.put(dong, ma);
                duLieuTheoMa.put(ma, r);
            }
            if (cboKhachHang.getItemCount() == 0) {
                thongBao("Không có khách hàng đang hoạt động để cấp thẻ.");
                return;
            }
            JTextField txtMaThe = TaoGiaoDien.oNhap(220);
            JTextField txtDiem = TaoGiaoDien.oNhap(160);
            JTextField txtHang = TaoGiaoDien.oNhap(160);
            JTextField txtTheHienTai = TaoGiaoDien.oNhap(220);
            TaoGiaoDien.datChiDoc(txtDiem, "0");
            TaoGiaoDien.datChiDoc(txtHang, "Tiêu chuẩn");
            TaoGiaoDien.datChiDoc(txtTheHienTai, "Chưa có thẻ hoạt động");
            int vr = tblThanhVien.getSelectedRow();
            String theDangChon = "";
            String maKhachDangChon = null;
            if (vr >= 0) {
                int row = tblThanhVien.convertRowIndexToModel(vr);
                maKhachDangChon = String.valueOf(moHinh.getValueAt(row, 1));
                theDangChon = String.valueOf(moHinh.getValueAt(row, 0));
                for (int i = 0; i < cboKhachHang.getItemCount(); i++) {
                    String item = String.valueOf(cboKhachHang.getItemAt(i));
                    if (maKhachDangChon.equals(maKhachTheoDong.get(item))) {
                        cboKhachHang.setSelectedIndex(i);
                        break;
                    }
                }
            }
            Runnable capNhatThongTin = () -> {
                String item = String.valueOf(cboKhachHang.getSelectedItem());
                String ma = maKhachTheoDong.get(item);
                KhachHang r = duLieuTheoMa.get(ma);
                if (r == null)
                    return;
                TaoGiaoDien.datChiDoc(txtDiem, String.valueOf(r.getDiemHienTai()));
                String tier = r.getMaHangThanhVien() == null ? "STANDARD"
                        : r.getMaHangThanhVien() == 2 ? "SILVER"
                                : r.getMaHangThanhVien() == 3 ? "GOLD"
                                        : r.getMaHangThanhVien() == 4 ? "PLATINUM" : "STANDARD";
                String hang = "STANDARD".equals(tier) ? "Tiêu chuẩn"
                        : "SILVER".equals(tier) ? "Bạc"
                                : "GOLD".equals(tier) ? "Vàng" : "PLATINUM".equals(tier) ? "Bạch kim" : tier;
                TaoGiaoDien.datChiDoc(txtHang, hang);
                String card = "";
                try {
                    for (TheThanhVien the : dichVu.khachHangThanhVien("%")) {
                        if (ma.equals(the.getKhachHang().getMaKhachHang()) && "Hoạt động".equals(the.getTrangThai())) {
                            card = the.getMaTheHienThi();
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
                TaoGiaoDien.datChiDoc(txtTheHienTai, card.isBlank() ? "Chưa có thẻ hoạt động" : card);
                try {
                    TaoGiaoDien.datGoiYMa(txtMaThe, dichVu.goiYMaThe(ma));
                } catch (Exception ignored) {
                }
            };
            cboKhachHang.addActionListener(e -> capNhatThongTin.run());
            capNhatThongTin.run();
            JPanel p = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Khách hàng", cboKhachHang),
                    TaoGiaoDien.truongBieuMau("Mã thẻ mới", txtMaThe), TaoGiaoDien.truongBieuMau(
                            "Điểm tích lũy hiện tại", txtDiem),
                    TaoGiaoDien.truongBieuMau("Hạng thành viên hiện tại", txtHang),
                    TaoGiaoDien.truongBieuMau("Thẻ đang hoạt động", txtTheHienTai));
            if (!TienIchHopThoai.xacNhanBieuMau(this, p, "Cấp thẻ / Cấp lại thẻ", "Cấp thẻ"))
                return;
            String item = String.valueOf(cboKhachHang.getSelectedItem());
            String maKhachHang = maKhachTheoDong.get(item);
            String maTheMoi = txtMaThe.getText().trim();
            if (!xacNhan("Cấp thẻ " + maTheMoi + " cho " + item
                    + "? Nếu khách hàng đang có thẻ hoạt động, thẻ cũ sẽ bị vô hiệu hóa."))
                return;
            String maThe = dichVu.capLaiTheThanhVien(maKhachHang, maTheMoi);
            lamMoiDuLieu();
            thongBao("Đã cấp thẻ thành viên: " + maThe);
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }

    private String nhanTrangThai(String trangThai) {
        if ("Hoạt động".equals(trangThai))
            return "Đang hoạt động";
        if ("Đã vô hiệu hóa".equals(trangThai))
            return "Đã vô hiệu hóa";
        if ("Đã mất".equals(trangThai))
            return "Đã mất";
        return trangThai;
    }
}

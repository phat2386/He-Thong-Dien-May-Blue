package ui.calamviec;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.*;
import entity.*;
import ui.dungchung.*;
import java.util.List;
public class ManHinhQuanLyCaLam extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final PhanCongService dichVu = new PhanCongService();
    private final JTextField txtNgay = TaoGiaoDien.oNhap(120);
    private final JComboBox<String> cmbLoaiCa = TaoGiaoDien.oChon("Tất cả ca", "Sáng", "Chiều", "Tối");
    private final JComboBox<String> cmbLoaiNhanVien = TaoGiaoDien.oChon("Tất cả loại nhân viên", "Bán hàng", "CSKH");
    private final JLabel lblTongPhanCong = taoGiaTriTongHop("0");
    private final JLabel lblSang = taoGiaTriTongHop("0");
    private final JLabel lblChieu = taoGiaTriTongHop("0");
    private final JLabel lblToi = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinh = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã ca", "Ngày", "Loại ca", "Thời gian", "Mã NV", "Nhân viên", "Loại nhân viên", "Người phân công", "Trạng thái"
    });
    private final JTable tblPhanCa = TaoGiaoDien.bang(moHinh);
    public ManHinhQuanLyCaLam() {
        super("Phân ca làm việc", "Phân công hoặc thay nhân viên theo ngày cho các loại nhân viên được cấu hình cho phép phân ca.");
        txtNgay.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        JButton btnPhanCa = TaoGiaoDien.nutChinh("Phân ca");
        JButton btnThay = TaoGiaoDien.nutPhu("Thay nhân viên");
        JButton btnHuy = TaoGiaoDien.nutPhu("Hủy phân ca");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        thaoTac.add(btnPhanCa);
        thaoTac.add(btnThay);
        thaoTac.add(btnHuy);
        thaoTac.add(btnLamMoi);
        JPanel pnlKhung = new JPanel(new BorderLayout(0, 12));
        pnlKhung.setOpaque(false);
        pnlKhung.add(taoKhungTongHop(), BorderLayout.NORTH);
        JPanel pnlTrungTam = new JPanel(new BorderLayout(0, 10));
        pnlTrungTam.setOpaque(false);
        pnlTrungTam.add(TaoGiaoDien.thanhCongCu(new JLabel("Ngày (dd/MM/yyyy)"), txtNgay, new JLabel("Loại ca"), cmbLoaiCa, new JLabel("Loại nhân viên"), cmbLoaiNhanVien), BorderLayout.NORTH);
        BangPhanTrang pnlCuon = TaoGiaoDien.bangPhanTrang(tblPhanCa);
        tblPhanCa.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        datDoRongCot();
        pnlTrungTam.add(pnlCuon, BorderLayout.CENTER);
        pnlKhung.add(pnlTrungTam, BorderLayout.CENTER);
        noiDung.add(pnlKhung, BorderLayout.CENTER);
        txtNgay.addActionListener(e -> lamMoiDuLieu());
        cmbLoaiCa.addActionListener(e -> lamMoiDuLieu());
        cmbLoaiNhanVien.addActionListener(e -> lamMoiDuLieu());
        btnPhanCa.addActionListener(e -> phanCa());
        btnThay.addActionListener(e -> thayNhanVien());
        btnHuy.addActionListener(e -> huyPhanCa());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu());
    }
    private JPanel taoKhungTongHop() {
        JPanel pnlTongHop = new JPanel(new GridLayout(1, 4, 10, 0));
        pnlTongHop.setOpaque(false);
        pnlTongHop.add(taoTheTongHop("Tổng phân công", lblTongPhanCong));
        pnlTongHop.add(taoTheTongHop("Ca sáng", lblSang));
        pnlTongHop.add(taoTheTongHop("Ca chiều", lblChieu));
        pnlTongHop.add(taoTheTongHop("Ca tối", lblToi));
        return pnlTongHop;
    }
    private JPanel taoTheTongHop(String tieuDe, JLabel lblGiaTri) {
        KhungBoGoc pnlThe = new KhungBoGoc(12, Color.WHITE);
        pnlThe.setLayout(new BorderLayout(0, 6));
        pnlThe.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel lblTieuDe = new JLabel(tieuDe);
        lblTieuDe.setFont(HangSoGiaoDien.MEDIUM);
        lblTieuDe.setForeground(HangSoGiaoDien.MUTED);
        pnlThe.add(lblTieuDe, BorderLayout.NORTH);
        pnlThe.add(lblGiaTri, BorderLayout.CENTER);
        return pnlThe;
    }
    private static JLabel taoGiaTriTongHop(String vanBan) {
        JLabel lblGiaTri = new JLabel(vanBan);
        lblGiaTri.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 24));
        lblGiaTri.setForeground(HangSoGiaoDien.TEXT);
        return lblGiaTri;
    }
    private void datDoRongCot() {
        int[] rong = {
            80, 100, 90, 125, 80, 180, 145, 120, 110
        };
        for (int i = 0; i < rong.length && i < tblPhanCa.getColumnCount(); i++) {
            tblPhanCa.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            LocalDate ngay = LocalDate.parse(txtNgay.getText().trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String loaiCaLoc = String.valueOf(cmbLoaiCa.getSelectedItem());
            String loaiNhanVienLoc = String.valueOf(cmbLoaiNhanVien.getSelectedItem());
            List<PhanCong> danhSach = dichVu.phanCongTheoNgay(ngay);
            int tong = 0;
            int sang = 0;
            int chieu = 0;
            int toi = 0;
            moHinh.setRowCount(0);
            for (PhanCong dong : danhSach) {
                String loaiCa = dong.getCaLamViec().getTenCa();
                if (!"Tất cả ca".equals(loaiCaLoc) && !loaiCaLoc.equals(loaiCa)) continue;
                String loaiNhanVien = DinhDangHienThi.anToan(dong.getNhanVien().getPhongBan());
                if (!"Tất cả loại nhân viên".equals(loaiNhanVienLoc) && !loaiNhanVienLoc.equals(loaiNhanVien)) continue;
                tong++;
                if ("Sáng".equals(loaiCa)) sang++;
                else if ("Chiều".equals(loaiCa)) chieu++;
                else if ("Tối".equals(loaiCa)) toi++;
                String thoiGian = dong.getCaLamViec().getGioBatDau() + " - " + dong.getCaLamViec().getGioKetThuc();
                moHinh.addRow(new Object[] {
                    dong.getMaPhanCong(), DinhDangHienThi.ngay(dong.getNgayLamViec()), loaiCa, thoiGian, dong.getNhanVien().getMaNhanVien(), dong.getNhanVien().getHoTen(), loaiNhanVien,
                    DinhDangHienThi.anToan(dong.getMaNguoiPhanCong()), nhanTrangThai(dong.getTrangThai())
                });
            }
            lblTongPhanCong.setText(String.valueOf(tong));
            lblSang.setText(String.valueOf(sang));
            lblChieu.setText(String.valueOf(chieu));
            lblToi.setText(String.valueOf(toi));
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void phanCa() {
        try {
            LocalDate ngay = LocalDate.parse(txtNgay.getText().trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            JTextField txtMaCa = TaoGiaoDien.oNhap(160);
            TaoGiaoDien.datGoiYMa(txtMaCa, dichVu.goiYMaCa());
            JComboBox<String> cmbLoaiNhanVienChon = TaoGiaoDien.oChon("Bán hàng", "CSKH");
            JComboBox<String> cmbNhanVien = TaoGiaoDien.oChon();
            JComboBox<String> cmbCa = TaoGiaoDien.oChon();
            List<NhanVien> nhanVienHopLe = dichVu.nhanVienCoThePhanCong();
            Runnable napNhanVien = () -> {
                cmbNhanVien.removeAllItems();
                String loai = String.valueOf(cmbLoaiNhanVienChon.getSelectedItem());
                for (NhanVien nv : nhanVienHopLe) {
                    if (loai.equals(nv.getPhongBan())) cmbNhanVien.addItem(nv.getMaNhanVien() + " - " + nv.getHoTen());
                }
            };
            cmbLoaiNhanVienChon.addActionListener(e -> napNhanVien.run());
            napNhanVien.run();
            for (CaLamViec ca : dichVu.caDangHoatDong()) {
                cmbCa.addItem(ca.getMaCa() + " - " + ca.getTenCa() + " (" + ca.getGioBatDau() + " - " + ca.getGioKetThuc() + ")");
            }
            if (cmbCa.getItemCount() == 0) {
                thongBao("Không có ca làm việc hợp lệ để phân công.");
                return;
            }
            JTextField txtNgayChiDoc = TaoGiaoDien.oNhap(160);
            TaoGiaoDien.datChiDoc(txtNgayChiDoc, ngay.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            JPanel pnlBieuMau = TaoGiaoDien.bieuMauHaiCot(
                TaoGiaoDien.truongBieuMau("Mã ca", txtMaCa),
                TaoGiaoDien.truongBieuMau("Ngày làm việc", txtNgayChiDoc),
                TaoGiaoDien.truongBieuMau("Loại nhân viên", cmbLoaiNhanVienChon),
                TaoGiaoDien.truongBieuMau("Nhân viên", cmbNhanVien),
                TaoGiaoDien.truongBieuMau("Ca làm việc", cmbCa));
            if (!TienIchHopThoai.xacNhanBieuMau(this, pnlBieuMau, "Phân ca làm việc", "Lưu phân ca")) return;
            if (cmbNhanVien.getSelectedItem() == null) throw new IllegalArgumentException("Không có nhân viên phù hợp với loại đã chọn.");
            String dongNhanVien = String.valueOf(cmbNhanVien.getSelectedItem());
            String dongCa = String.valueOf(cmbCa.getSelectedItem());
            String maNhanVien = dongNhanVien.substring(0, dongNhanVien.indexOf(" - "));
            String maMauCa = dongCa.substring(0, dongCa.indexOf(" - "));
            int maLoaiCa = "MORNING".equals(maMauCa) ? 1 : "AFTERNOON".equals(maMauCa) ? 2 : 3;
            dichVu.phanCa(txtMaCa.getText(), maNhanVien, ngay, maLoaiCa, null);
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void thayNhanVien() {
        int dongChon = tblPhanCa.getSelectedRow();
        if (dongChon < 0) { thongBao("Hãy chọn phân ca cần thay nhân viên."); return; }
        int dongThat = tblPhanCa.convertRowIndexToModel(dongChon);
        String maCa = String.valueOf(moHinh.getValueAt(dongThat, 0));
        String maPhanCong = maCa;
        try {
            JComboBox<String> cmbNhanVienThay = TaoGiaoDien.oChon();
            String maHienTai = String.valueOf(moHinh.getValueAt(dongThat, 4));
            for (NhanVien nv : dichVu.nhanVienCoThePhanCong()) if (!maHienTai.equals(nv.getMaNhanVien())) cmbNhanVienThay.addItem(nv.getMaNhanVien() + " - " + nv.getHoTen());
            if (cmbNhanVienThay.getItemCount() == 0) { thongBao("Không có nhân viên phù hợp để thay thế."); return; }
            JPanel pnlBieuMau = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Phân ca", new JLabel(maCa)), TaoGiaoDien.truongBieuMau("Nhân viên thay thế", cmbNhanVienThay));
            if (!TienIchHopThoai.xacNhanBieuMau(this, pnlBieuMau, "Thay nhân viên trong ca", "Xác nhận thay")) return;
            String dong = String.valueOf(cmbNhanVienThay.getSelectedItem());
            dichVu.thayNhanVienPhanCa(maPhanCong, dong.substring(0, dong.indexOf(" - ")));
            lamMoiDuLieu();
        } catch (Exception ex) { hienLoi(ex); }
    }
    private void huyPhanCa() {
        int dongChon = tblPhanCa.getSelectedRow();
        if (dongChon < 0) {
            thongBao("Hãy chọn phân ca cần hủy.");
            return;
        }
        int dongThat = tblPhanCa.convertRowIndexToModel(dongChon);
        String maCa = String.valueOf(moHinh.getValueAt(dongThat, 0));
        if (!maCa.matches("CA\\d{3}")) {
            thongBao("Mã ca không đúng định dạng CA001, CA002, ...");
            return;
        }
        String maPhanCong = maCa;
        if (!xacNhan("Hủy phân ca " + maCa + "?")) return;
        try {
            dichVu.huyPhanCa(maPhanCong);
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private String nhanTrangThai(String trangThai) {
        if ("Đã phân công".equals(trangThai)) return "Đã phân công";
        if ("Hoàn tất".equals(trangThai)) return "Đã hoàn thành";
        if ("Đã hủy".equals(trangThai)) return "Đã hủy";
        return trangThai;
    }
}
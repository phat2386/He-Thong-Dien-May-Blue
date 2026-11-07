package ui.hoantien;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import service.*;
import entity.*;
import ui.dungchung.*;
import java.util.List;
public class ManHinhDuyetHoanTien extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final PhieuDoiTraHoanTienService dichVu = new PhieuDoiTraHoanTienService();
    private final JTextField txtTimKiem = TaoGiaoDien.oNhap(320);
    private final JComboBox<String> cmbTrangThai = TaoGiaoDien.oChon("Tất cả trạng thái", "Chờ duyệt", "Đã duyệt", "Đã từ chối");
    private final JLabel lblChoDuyet = taoGiaTriTongHop("0");
    private final JLabel lblDaDuyet = taoGiaTriTongHop("0");
    private final JLabel lblTuChoi = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinh = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã phiếu", "Hóa đơn", "Mã KH", "Khách hàng", "Mã NV bán", "Mã NV QL", "Giá trị hoàn", "Trạng thái", "Ngày lập"
    });
    private final JTable tblDanhSach = TaoGiaoDien.bang(moHinh);
    private final List<PhieuDoiTraHoanTien> duLieuHienThi = new ArrayList<>();
    private final JLabel lblMaPhieu = new JLabel("-");
    private final JLabel lblHoaDon = new JLabel("-");
    private final JLabel lblMaKhachHang = new JLabel("-");
    private final JLabel lblKhachHang = new JLabel("-");
    private final JLabel lblSoDienThoai = new JLabel("-");
    private final JLabel lblMaNhanVienBanHang = new JLabel("-");
    private final JLabel lblMaNhanVienQuanLy = new JLabel("-");
    private final JLabel lblNgayLap = new JLabel("-");
    private final JLabel lblSanPham = new JLabel("-");
    private final JLabel lblGiaTriHoan = new JLabel("-");
    private final JLabel lblTrangThai = new JLabel("-");
    private final JLabel lblTemMac = new JLabel("-");
    private final JLabel lblHop = new JLabel("-");
    private final JLabel lblPhuKien = new JLabel("-");
    private final JLabel lblDieuKien = new JLabel("-");
    private final JTextArea txtLyDo = taoVungVanBan();
    private final JTextArea txtTinhTrang = taoVungVanBan();
    private final JTextArea txtKetQua = taoVungVanBan();
    private final JButton btnDuyet = TaoGiaoDien.nutChinh("Duyệt hoàn tiền");
    private final JButton btnTuChoi = TaoGiaoDien.nutPhu("Từ chối");
    public ManHinhDuyetHoanTien() {
        super("Duyệt hoàn tiền", "Duyệt hoặc từ chối các phiếu đổi trả vượt hạn mức cần Quản lý phê duyệt.");
        TaoGiaoDien.datChuGoiY(txtTimKiem, "Mã phiếu, hóa đơn, khách hàng, nhân viên hoặc sản phẩm");
        thaoTac.add(btnDuyet);
        thaoTac.add(btnTuChoi);
        thaoTac.add(TaoGiaoDien.nutPhu("Làm mới"));
        ((JButton) thaoTac.getComponent(2)).addActionListener(e -> lamMoiDuLieu());
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(taoKhungTongHop(), BorderLayout.NORTH);
        JPanel trungTam = new JPanel(new BorderLayout(0, 10));
        trungTam.setOpaque(false);
        trungTam.add(TaoGiaoDien.thanhCongCu(new JLabel("Tìm kiếm"), txtTimKiem, new JLabel("Trạng thái"), cmbTrangThai), BorderLayout.NORTH);
        BangPhanTrang cuonBang = TaoGiaoDien.bangPhanTrang(tblDanhSach);
        tblDanhSach.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDanhSach.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        datDoRongCot();
        JSplitPane chia = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cuonBang, taoKhungChiTiet());
        chia.setResizeWeight(0.58);
        chia.setDividerSize(8);
        chia.setBorder(null);
        chia.setOpaque(false);
        trungTam.add(chia, BorderLayout.CENTER);
        khung.add(trungTam, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimKiem, tblDanhSach, 0, m -> lamMoiDuLieu(), 0, 1, 2, 3, 4, 5);
        cmbTrangThai.addActionListener(e -> lamMoiDuLieu());
        tblDanhSach.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) capNhatChiTietDangChon();
        });
        btnDuyet.addActionListener(e -> quyetDinh(true));
        btnTuChoi.addActionListener(e -> quyetDinh(false));
    }
    private JPanel taoKhungTongHop() {
        JPanel p = new JPanel(new GridLayout(1, 3, 10, 0));
        p.setOpaque(false);
        p.add(taoTheTongHop("Chờ duyệt", lblChoDuyet));
        p.add(taoTheTongHop("Đã duyệt", lblDaDuyet));
        p.add(taoTheTongHop("Đã từ chối", lblTuChoi));
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
    private JPanel taoKhungChiTiet() {
        KhungBoGoc p = new KhungBoGoc(12, Color.WHITE);
        p.setLayout(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        p.setPreferredSize(new Dimension(430, 430));
        JLabel tieuDe = new JLabel("Chi tiết phiếu");
        tieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 16));
        tieuDe.setForeground(HangSoGiaoDien.TEXT);
        p.add(tieuDe, BorderLayout.NORTH);
        JPanel noi = new JPanel(new BorderLayout(0, 8));
        noi.setOpaque(false);
        JPanel thongTin = new JPanel(new GridLayout(0, 2, 10, 4));
        thongTin.setOpaque(false);
        thongTin.add(taoDongChiTiet("Mã phiếu", lblMaPhieu));
        thongTin.add(taoDongChiTiet("Hóa đơn", lblHoaDon));
        thongTin.add(taoDongChiTiet("Mã khách hàng", lblMaKhachHang));
        thongTin.add(taoDongChiTiet("Khách hàng", lblKhachHang));
        thongTin.add(taoDongChiTiet("Số điện thoại", lblSoDienThoai));
        thongTin.add(taoDongChiTiet("Mã NV bán hàng", lblMaNhanVienBanHang));
        thongTin.add(taoDongChiTiet("Mã NV quản lý", lblMaNhanVienQuanLy));
        thongTin.add(taoDongChiTiet("Ngày lập", lblNgayLap));
        thongTin.add(taoDongChiTiet("Sản phẩm", lblSanPham));
        thongTin.add(taoDongChiTiet("Giá trị hoàn", lblGiaTriHoan));
        thongTin.add(taoDongChiTiet("Trạng thái", lblTrangThai));
        thongTin.add(taoDongChiTiet("Điều kiện", lblDieuKien));
        noi.add(thongTin, BorderLayout.NORTH);
        JPanel vanBan = new JPanel(new GridLayout(1, 2, 8, 0));
        vanBan.setOpaque(false);
        vanBan.add(taoNhanVung("Lý do đổi trả", txtLyDo));
        vanBan.add(taoNhanVung("Tình trạng thực tế", txtTinhTrang));
        noi.add(vanBan, BorderLayout.CENTER);
        JPanel ketQua = taoNhanVung("Kết quả xử lý", txtKetQua);
        ketQua.setPreferredSize(new Dimension(400, 66));
        noi.add(ketQua, BorderLayout.SOUTH);
        p.add(noi, BorderLayout.CENTER);
        return p;
    }
    private JPanel taoDongChiTiet(String ten, JLabel giaTri) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        JLabel lbl = new JLabel(ten + ":");
        lbl.setPreferredSize(new Dimension(92, 22));
        lbl.setForeground(HangSoGiaoDien.MUTED);
        giaTri.setForeground(HangSoGiaoDien.TEXT);
        p.add(lbl, BorderLayout.WEST);
        p.add(giaTri, BorderLayout.CENTER);
        return p;
    }
    private JPanel taoNhanVung(String ten, JTextArea vung) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        JLabel lbl = new JLabel(ten);
        lbl.setForeground(HangSoGiaoDien.MUTED);
        p.add(lbl, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(vung);
        sp.setPreferredSize(new Dimension(185, 54));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }
    private static JTextArea taoVungVanBan() {
        JTextArea a = new JTextArea(2, 18);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setFont(HangSoGiaoDien.NORMAL);
        a.setBackground(new Color(248, 250, 252));
        a.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return a;
    }
    private void datDoRongCot() {
        int[] rong = {
            85, 90, 80, 145, 90, 90, 115, 105, 110
        };
        for (int i = 0; i < rong.length && i < tblDanhSach.getColumnCount(); i++) {
            tblDanhSach.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        }
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            capNhatTongHop();
            String trangThai = maTrangThaiDangChon();
            String tuKhoa = txtTimKiem.getText().trim().toLowerCase();
            List<PhieuDoiTraHoanTien> cacDong = dichVu.layDanhSachDuyetHoanTien(trangThai);
            duLieuHienThi.clear();
            moHinh.setRowCount(0);
            for (PhieuDoiTraHoanTien dong : cacDong) {
                if (!khopTuKhoa(dong, tuKhoa)) continue;
                duLieuHienThi.add(dong);
                String maQuanLy = dong.getNhanVienQuanLy() == null ? "-" : dong.getNhanVienQuanLy().getMaNhanVien();
                moHinh.addRow(new Object[] {
                    dong.getMaPhieu(), dong.getHoaDon().getMaHoaDon(), dong.getHoaDon().getKhachHang().getMaKhachHang(), dong.getHoaDon().getKhachHang().getHoTen(),
                    dong.getNhanVienBanHang().getMaNhanVien(), maQuanLy, DinhDangHienThi.tien(dong.getGiaTriHoan()), nhanTrangThai(dong.getTrangThai()), DinhDangHienThi.ngay(dong.getNgayLap())
                });
            }
            if (moHinh.getRowCount() > 0) TaoGiaoDien.chonDongDauTienSauKhiCapNhat(tblDanhSach);
            else xoaChiTiet();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void capNhatTongHop() throws Exception {
        int cho = dichVu.layDanhSachDuyetHoanTien("WAITING_APPROVAL").size();
        int duyet = dichVu.layDanhSachDuyetHoanTien("APPROVED").size();
        int tuChoi = dichVu.layDanhSachDuyetHoanTien("REJECTED").size();
        lblChoDuyet.setText(String.valueOf(cho));
        lblDaDuyet.setText(String.valueOf(duyet));
        lblTuChoi.setText(String.valueOf(tuChoi));
    }
    private boolean khopTuKhoa(PhieuDoiTraHoanTien dong, String tuKhoa) {
        if (tuKhoa.isEmpty()) return true;
        String sanPham = moTaSanPham(dong);
        String maQuanLy = dong.getNhanVienQuanLy() == null ? "" : dong.getNhanVienQuanLy().getMaNhanVien();
        String tong = (dong.getMaPhieu() + " " + dong.getHoaDon().getMaHoaDon() + " " + dong.getHoaDon().getKhachHang().getMaKhachHang() + " " +
            dong.getHoaDon().getKhachHang().getHoTen() + " " + dong.getNhanVienBanHang().getMaNhanVien() + " " + maQuanLy + " " + sanPham).toLowerCase();
        return tong.contains(tuKhoa);
    }
    private String moTaSanPham(PhieuDoiTraHoanTien phieu) {
        if (phieu == null || phieu.getChiTiet().isEmpty()) return "-";
        StringBuilder b = new StringBuilder();
        for (ChiTietPhieuDoiTra d : phieu.getChiTiet()) {
            if (b.length() > 0) b.append("; ");
            b.append(d.getSanPham().getMaSanPham()).append(" x").append(d.getSoLuongTra());
        }
        return b.toString();
    }
    private String maTrangThaiDangChon() {
        String chon = String.valueOf(cmbTrangThai.getSelectedItem());
        if ("Chờ duyệt".equals(chon)) return "WAITING_APPROVAL";
        if ("Đã duyệt".equals(chon)) return "APPROVED";
        if ("Đã từ chối".equals(chon)) return "REJECTED";
        return null;
    }
    private void capNhatChiTietDangChon() {
        int viewRow = tblDanhSach.getSelectedRow();
        if (viewRow < 0) {
            xoaChiTiet();
            return;
        }
        int modelRow = tblDanhSach.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= duLieuHienThi.size()) {
            xoaChiTiet();
            return;
        }
        PhieuDoiTraHoanTien d = duLieuHienThi.get(modelRow);
        lblMaPhieu.setText(d.getMaPhieu());
        lblHoaDon.setText(d.getHoaDon().getMaHoaDon());
        lblMaKhachHang.setText(d.getHoaDon().getKhachHang().getMaKhachHang());
        lblKhachHang.setText(d.getHoaDon().getKhachHang().getHoTen());
        lblSoDienThoai.setText(d.getHoaDon().getKhachHang().getSoDienThoai());
        lblMaNhanVienBanHang.setText(d.getNhanVienBanHang().getMaNhanVien());
        lblMaNhanVienQuanLy.setText(d.getNhanVienQuanLy() == null ? "Chưa duyệt" : d.getNhanVienQuanLy().getMaNhanVien());
        lblNgayLap.setText(DinhDangHienThi.ngay(d.getNgayLap()));
        lblSanPham.setText(moTaSanPham(d));
        lblGiaTriHoan.setText(DinhDangHienThi.tien(d.getGiaTriHoan()));
        String status = d.getTrangThai();
        lblTrangThai.setText(nhanTrangThai(status));
        lblTemMac.setText(d.isConTemMac() ? "Có" : "Không");
        lblHop.setText(d.isDayDuHop() ? "Có" : "Không");
        lblPhuKien.setText(d.isDuPhuKien() ? "Có" : "Không");
        lblDieuKien.setText("Tem " + lblTemMac.getText() + " • Hộp " + lblHop.getText() + " • Phụ kiện " + lblPhuKien.getText());
        txtLyDo.setText(d.getLyDo() == null ? "" : d.getLyDo());
        txtTinhTrang.setText(d.getTinhTrangVatLy() == null ? "" : d.getTinhTrangVatLy());
        txtKetQua.setText("REJECTED".equals(status) ? (d.getLyDoTuChoi() == null ? "" : d.getLyDoTuChoi()) :
            ("APPROVED".equals(status) || "Hoàn tất".equals(status)) ? "Đã được duyệt và xử lý hoàn tiền." : "Chưa xử lý.");
        boolean choDuyet = "WAITING_APPROVAL".equals(status);
        btnDuyet.setEnabled(choDuyet);
        btnTuChoi.setEnabled(choDuyet);
    }
    private String coKhong(Object giaTri) {
        if (giaTri instanceof Boolean) return((Boolean) giaTri).booleanValue() ? "Có" : "Không";
        if (giaTri instanceof Number) return((Number) giaTri).intValue() != 0 ? "Có" : "Không";
        return "1".equals(String.valueOf(giaTri)) || "true".equalsIgnoreCase(String.valueOf(giaTri)) ? "Có" : "Không";
    }
    private void xoaChiTiet() {
        lblMaPhieu.setText("-");
        lblHoaDon.setText("-");
        lblMaKhachHang.setText("-");
        lblKhachHang.setText("-");
        lblSoDienThoai.setText("-");
        lblMaNhanVienBanHang.setText("-");
        lblMaNhanVienQuanLy.setText("-");
        lblNgayLap.setText("-");
        lblSanPham.setText("-");
        lblGiaTriHoan.setText("-");
        lblTrangThai.setText("-");
        lblTemMac.setText("-");
        lblHop.setText("-");
        lblPhuKien.setText("-");
        lblDieuKien.setText("-");
        txtLyDo.setText("");
        txtTinhTrang.setText("");
        txtKetQua.setText("");
        btnDuyet.setEnabled(false);
        btnTuChoi.setEnabled(false);
    }
    private String nhanTrangThai(String ma) {
        if ("WAITING_APPROVAL".equals(ma)) return "Chờ duyệt";
        if ("APPROVED".equals(ma)) return "Đã duyệt";
        if ("REJECTED".equals(ma)) return "Đã từ chối";
        if ("Hoàn tất".equals(ma)) return "Đã duyệt";
        return ma;
    }
    private void quyetDinh(boolean dongY) {
        int vr = tblDanhSach.getSelectedRow();
        if (vr < 0) {
            thongBao("Hãy chọn phiếu cần xử lý.");
            return;
        }
        int r = tblDanhSach.convertRowIndexToModel(vr);
        String id = String.valueOf(moHinh.getValueAt(r, 0));
        String hienTai = String.valueOf(moHinh.getValueAt(r, 7));
        if (!"Chờ duyệt".equals(hienTai)) {
            thongBao("Phiếu đã được xử lý.");
            return;
        }
        String lyDo = null;
        if (!dongY) {
            lyDo = TienIchHopThoai.nhapVanBanDai(this, "Từ chối hoàn tiền", "Lý do từ chối", "", "Tiếp tục từ chối");
            if (lyDo == null) return;
            if (lyDo.trim().isEmpty()) {
                thongBao("Cần nhập lý do từ chối.");
                return;
            }
        }
        if (!xacNhan(dongY ? "Xác nhận duyệt phiếu " + id + "?" : "Xác nhận từ chối phiếu " + id + "?")) return;
        try {
            dichVu.duyetHoanTien(id, dongY, lyDo);
            cmbTrangThai.setSelectedItem("Tất cả trạng thái");
            lamMoiDuLieu();
            thongBao(dongY ? "Đã duyệt và hoàn tiền." : "Đã từ chối phiếu.");
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
}

package ui.sauban;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import service.*;
import entity.*;
import ui.dungchung.*;
import java.util.List;
public class ManHinhSauBan extends ManHinhCoSo {
    public enum PhanSauBan {
        BAO_HANH, KHIEU_NAI
    }
    private static final long serialVersionUID = 1L;
    private final PhanSauBan phanSauBan;
    private final KhachHangService dichVuKhachHang = new KhachHangService();
    private final PhieuBaoHanhService dichVuBaoHanh = new PhieuBaoHanhService();
    private final KhieuNaiService dichVuKhieuNai = new KhieuNaiService();
    private final JTextField txtTimBaoHanh = TaoGiaoDien.oNhap(320);
    private final JComboBox<String> cmbTrangThaiBaoHanh = TaoGiaoDien.oChon("Tất cả trạng thái", "Đang sửa chữa", "Đổi máy", "Hoàn tất", "Đã trả khách", "Từ chối");
    private final JLabel lblTongBaoHanh = taoGiaTriTongHop("0");
    private final JLabel lblDangSua = taoGiaTriTongHop("0");
    private final JLabel lblDoiMay = taoGiaTriTongHop("0");
    private final JLabel lblDaKetThuc = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinhBaoHanh = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã phiếu", "Khách hàng", "Sản phẩm", "Ngày tiếp nhận", "Hạn bảo hành", "Trạng thái", "Phí đổi máy"
    });
    private final JTable tblBaoHanh = TaoGiaoDien.bang(moHinhBaoHanh);
    private final List<PhieuBaoHanh> duLieuBaoHanh = new ArrayList<PhieuBaoHanh>();
    private final JLabel lblChiTietMaPhieu = new JLabel("-");
    private final JLabel lblChiTietKhachHang = new JLabel("-");
    private final JLabel lblChiTietSoDienThoai = new JLabel("-");
    private final JLabel lblChiTietSanPham = new JLabel("-");
    private final JLabel lblChiTietNhanVien = new JLabel("-");
    private final JLabel lblChiTietNgayNhan = new JLabel("-");
    private final JLabel lblChiTietHanBaoHanh = new JLabel("-");
    private final JLabel lblChiTietTrangThai = new JLabel("-");
    private final JLabel lblChiTietPhiDoiMay = new JLabel("-");
    private final JLabel lblChiTietNgayHoanThanh = new JLabel("-");
    private final JLabel lblChiTietNgayTraKhach = new JLabel("-");
    private final JTextArea txaChiTietMoTaLoi = taoVungVanBanChiDoc();
    private JButton btnCapNhatBaoHanh;
    private final JTextField txtTimKhieuNai = TaoGiaoDien.oNhap(320);
    private final JComboBox<String> cmbTrangThaiKhieuNai = TaoGiaoDien.oChon("Tất cả trạng thái", "Chưa xử lý", "Đang xử lý", "Chờ khách hàng", "Đã xử lý", "Đã đóng");
    private final JComboBox<String> cmbUuTienKhieuNai = TaoGiaoDien.oChon("Tất cả mức ưu tiên", "Bình thường", "Khẩn cấp");
    private final JLabel lblTongKhieuNai = taoGiaTriTongHop("0");
    private final JLabel lblDangXuLyKhieuNai = taoGiaTriTongHop("0");
    private final JLabel lblDaGiaiQuyetKhieuNai = taoGiaTriTongHop("0");
    private final JLabel lblLeoThangKhieuNai = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinhKhieuNai = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã KN", "Khách hàng", "SĐT", "Ngày ghi nhận", "Ưu tiên", "Trạng thái", "Phụ trách", "Hạn xử lý"
    });
    private final JTable tblKhieuNai = TaoGiaoDien.bang(moHinhKhieuNai);
    private final List<KhieuNai> duLieuKhieuNai = new ArrayList<KhieuNai>();
    private final JLabel lblKhieuNaiMa = new JLabel("-");
    private final JLabel lblKhieuNaiKhachHang = new JLabel("-");
    private final JLabel lblKhieuNaiSoDienThoai = new JLabel("-");
    private final JLabel lblKhieuNaiNguoiTao = new JLabel("-");
    private final JLabel lblKhieuNaiPhuTrach = new JLabel("-");
    private final JLabel lblKhieuNaiNgayGhiNhan = new JLabel("-");
    private final JLabel lblKhieuNaiHanXuLy = new JLabel("-");
    private final JLabel lblKhieuNaiUuTien = new JLabel("-");
    private final JLabel lblKhieuNaiTrangThai = new JLabel("-");
    private final JLabel lblKhieuNaiLeoThang = new JLabel("-");
    private final JLabel lblKhieuNaiNgayGiaiQuyet = new JLabel("-");
    private final JTextArea txaKhieuNaiNoiDung = taoVungVanBanChiDoc();
    private JButton btnCapNhatKhieuNai;
    public ManHinhSauBan(PhanSauBan phanSauBan) {
        super(phanSauBan == PhanSauBan.BAO_HANH ? "Bảo hành" : "Khiếu nại", phanSauBan == PhanSauBan.BAO_HANH ? "Tiếp nhận, theo dõi và cập nhật xử lý bảo hành sản phẩm." : "Tiếp nhận và xử lý khiếu nại khách hàng.");
        this.phanSauBan = phanSauBan;
        TaoGiaoDien.datChuGoiY(txtTimBaoHanh, "Mã phiếu, khách hàng, SĐT, sản phẩm hoặc nhân viên");
        TaoGiaoDien.datChuGoiY(txtTimKhieuNai, "Mã KN, khách hàng, SĐT, nội dung hoặc phụ trách");
        if (phanSauBan == PhanSauBan.BAO_HANH) {
            taoGiaoDienBaoHanh();
        } else {
            taoGiaoDienKhieuNai();
        }
    }
    private void taoGiaoDienBaoHanh() {
        boolean duocQuanLy = TaiKhoanService.coQuyen("WARRANTY_MANAGE");
        JButton btnTiepNhan = TaoGiaoDien.nutChinh("Tiếp nhận bảo hành");
        btnCapNhatBaoHanh = TaoGiaoDien.nutPhu("Cập nhật xử lý");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        btnTiepNhan.setEnabled(duocQuanLy);
        btnCapNhatBaoHanh.setEnabled(false);
        thaoTac.add(btnTiepNhan);
        thaoTac.add(btnCapNhatBaoHanh);
        thaoTac.add(btnLamMoi);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(taoKhungTongHopBaoHanh(), BorderLayout.NORTH);
        JPanel trungTam = new JPanel(new BorderLayout(0, 10));
        trungTam.setOpaque(false);
        trungTam.add(TaoGiaoDien.thanhCongCu(new JLabel("Tìm kiếm"), txtTimBaoHanh, new JLabel("Trạng thái"), cmbTrangThaiBaoHanh), BorderLayout.NORTH);
        JPanel danhSachVaChiTiet = new JPanel(new BorderLayout(0, 10));
        danhSachVaChiTiet.setOpaque(false);
        tblBaoHanh.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        danhSachVaChiTiet.add(TaoGiaoDien.bangPhanTrang(tblBaoHanh), BorderLayout.CENTER);
        danhSachVaChiTiet.add(taoKhungChiTietBaoHanh(), BorderLayout.SOUTH);
        trungTam.add(danhSachVaChiTiet, BorderLayout.CENTER);
        khung.add(trungTam, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        txtTimBaoHanh.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimBaoHanh, tblBaoHanh, 0, m -> lamMoiDuLieu(), 0, 1, 2);
        cmbTrangThaiBaoHanh.addActionListener(e -> lamMoiDuLieu());
        btnTiepNhan.addActionListener(e -> taoBaoHanh());
        btnCapNhatBaoHanh.addActionListener(e -> capNhatBaoHanh());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu());
        tblBaoHanh.getSelectionModel().addListSelectionListener(this::khiChonBaoHanh);
    }
    private JPanel taoKhungTongHopBaoHanh() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        p.add(taoTheTongHop("Tổng phiếu", lblTongBaoHanh));
        p.add(taoTheTongHop("Đang sửa chữa", lblDangSua));
        p.add(taoTheTongHop("Đổi máy", lblDoiMay));
        p.add(taoTheTongHop("Đã kết thúc", lblDaKetThuc));
        return p;
    }
    private JPanel taoKhungChiTietBaoHanh() {
        KhungBoGoc khung = new KhungBoGoc(12, Color.WHITE);
        khung.setLayout(new BorderLayout(12, 8));
        khung.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel tieuDe = new JLabel("Chi tiết phiếu bảo hành");
        tieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 15));
        tieuDe.setForeground(HangSoGiaoDien.TEXT);
        khung.add(tieuDe, BorderLayout.NORTH);
        JPanel cotTrai = new JPanel(new GridLayout(0, 2, 8, 5));
        cotTrai.setOpaque(false);
        themDong(cotTrai, "Mã phiếu", lblChiTietMaPhieu);
        themDong(cotTrai, "Khách hàng", lblChiTietKhachHang);
        themDong(cotTrai, "Số điện thoại", lblChiTietSoDienThoai);
        themDong(cotTrai, "Sản phẩm", lblChiTietSanPham);
        themDong(cotTrai, "Nhân viên CSKH", lblChiTietNhanVien);
        themDong(cotTrai, "Ngày tiếp nhận", lblChiTietNgayNhan);
        JPanel cotPhai = new JPanel(new GridLayout(0, 2, 8, 5));
        cotPhai.setOpaque(false);
        themDong(cotPhai, "Hạn bảo hành", lblChiTietHanBaoHanh);
        themDong(cotPhai, "Trạng thái", lblChiTietTrangThai);
        themDong(cotPhai, "Phí đổi máy", lblChiTietPhiDoiMay);
        themDong(cotPhai, "Ngày hoàn thành", lblChiTietNgayHoanThanh);
        themDong(cotPhai, "Ngày trả khách", lblChiTietNgayTraKhach);
        JPanel thongTin = new JPanel(new GridLayout(1, 2, 20, 0));
        thongTin.setOpaque(false);
        thongTin.add(cotTrai);
        thongTin.add(cotPhai);
        JPanel noiDungChiTiet = new JPanel(new BorderLayout(12, 8));
        noiDungChiTiet.setOpaque(false);
        noiDungChiTiet.add(thongTin, BorderLayout.CENTER);
        JPanel moTa = new JPanel(new BorderLayout(6, 4));
        moTa.setOpaque(false);
        JLabel lbl = new JLabel("Mô tả lỗi");
        lbl.setFont(HangSoGiaoDien.MEDIUM);
        moTa.add(lbl, BorderLayout.NORTH);
        JScrollPane cuon = new JScrollPane(txaChiTietMoTaLoi);
        cuon.setBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER));
        cuon.setPreferredSize(new java.awt.Dimension(100, 62));
        moTa.add(cuon, BorderLayout.CENTER);
        noiDungChiTiet.add(moTa, BorderLayout.SOUTH);
        khung.add(noiDungChiTiet, BorderLayout.CENTER);
        return khung;
    }
    private void taoGiaoDienKhieuNai() {
        boolean duocQuanLy = TaiKhoanService.coQuyen("COMPLAINT_MANAGE");
        JButton btnTiepNhan = TaoGiaoDien.nutChinh("Tiếp nhận khiếu nại");
        btnCapNhatKhieuNai = TaoGiaoDien.nutPhu("Cập nhật xử lý");
        JButton btnLamMoi = TaoGiaoDien.nutPhu("Làm mới");
        btnTiepNhan.setEnabled(duocQuanLy);
        btnCapNhatKhieuNai.setEnabled(false);
        thaoTac.add(btnTiepNhan);
        thaoTac.add(btnCapNhatKhieuNai);
        thaoTac.add(btnLamMoi);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(taoKhungTongHopKhieuNai(), BorderLayout.NORTH);
        JPanel trungTam = new JPanel(new BorderLayout(0, 10));
        trungTam.setOpaque(false);
        trungTam.add(TaoGiaoDien.thanhCongCu(new JLabel("Tìm kiếm"), txtTimKhieuNai, new JLabel("Trạng thái"), cmbTrangThaiKhieuNai, new JLabel("Mức ưu tiên"), cmbUuTienKhieuNai), BorderLayout.NORTH);
        JPanel danhSachVaChiTiet = new JPanel(new BorderLayout(0, 10));
        danhSachVaChiTiet.setOpaque(false);
        tblKhieuNai.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        danhSachVaChiTiet.add(TaoGiaoDien.bangPhanTrang(tblKhieuNai), BorderLayout.CENTER);
        danhSachVaChiTiet.add(taoKhungChiTietKhieuNai(), BorderLayout.SOUTH);
        trungTam.add(danhSachVaChiTiet, BorderLayout.CENTER);
        khung.add(trungTam, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        txtTimKhieuNai.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimKhieuNai, tblKhieuNai, 0, m -> lamMoiDuLieu(), 0, 1, 2);
        cmbTrangThaiKhieuNai.addActionListener(e -> lamMoiDuLieu());
        cmbUuTienKhieuNai.addActionListener(e -> lamMoiDuLieu());
        btnTiepNhan.addActionListener(e -> taoKhieuNai());
        btnCapNhatKhieuNai.addActionListener(e -> capNhatKhieuNai());
        btnLamMoi.addActionListener(e -> lamMoiDuLieu());
        tblKhieuNai.getSelectionModel().addListSelectionListener(this::khiChonKhieuNai);
    }
    private JPanel taoKhungTongHopKhieuNai() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setOpaque(false);
        p.add(taoTheTongHop("Tổng khiếu nại", lblTongKhieuNai));
        p.add(taoTheTongHop("Đang xử lý", lblDangXuLyKhieuNai));
        p.add(taoTheTongHop("Đã giải quyết", lblDaGiaiQuyetKhieuNai));
        p.add(taoTheTongHop("Đã leo thang", lblLeoThangKhieuNai));
        return p;
    }
    private JPanel taoKhungChiTietKhieuNai() {
        KhungBoGoc khung = new KhungBoGoc(12, Color.WHITE);
        khung.setLayout(new BorderLayout(12, 8));
        khung.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel tieuDe = new JLabel("Chi tiết khiếu nại");
        tieuDe.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 15));
        tieuDe.setForeground(HangSoGiaoDien.TEXT);
        khung.add(tieuDe, BorderLayout.NORTH);
        JPanel cotTrai = new JPanel(new GridLayout(0, 2, 8, 5));
        cotTrai.setOpaque(false);
        themDong(cotTrai, "Mã khiếu nại", lblKhieuNaiMa);
        themDong(cotTrai, "Khách hàng", lblKhieuNaiKhachHang);
        themDong(cotTrai, "Số điện thoại", lblKhieuNaiSoDienThoai);
        themDong(cotTrai, "Người ghi nhận", lblKhieuNaiNguoiTao);
        themDong(cotTrai, "Người phụ trách", lblKhieuNaiPhuTrach);
        themDong(cotTrai, "Ngày ghi nhận", lblKhieuNaiNgayGhiNhan);
        JPanel cotPhai = new JPanel(new GridLayout(0, 2, 8, 5));
        cotPhai.setOpaque(false);
        themDong(cotPhai, "Mức ưu tiên", lblKhieuNaiUuTien);
        themDong(cotPhai, "Trạng thái", lblKhieuNaiTrangThai);
        themDong(cotPhai, "Hạn xử lý", lblKhieuNaiHanXuLy);
        themDong(cotPhai, "Leo thang", lblKhieuNaiLeoThang);
        themDong(cotPhai, "Ngày giải quyết", lblKhieuNaiNgayGiaiQuyet);
        JPanel thongTin = new JPanel(new GridLayout(1, 2, 20, 0));
        thongTin.setOpaque(false);
        thongTin.add(cotTrai);
        thongTin.add(cotPhai);
        JPanel noiDungChiTiet = new JPanel(new BorderLayout(12, 8));
        noiDungChiTiet.setOpaque(false);
        noiDungChiTiet.add(thongTin, BorderLayout.CENTER);
        JPanel noiDungKn = new JPanel(new BorderLayout(6, 4));
        noiDungKn.setOpaque(false);
        JLabel lbl = new JLabel("Nội dung khiếu nại");
        lbl.setFont(HangSoGiaoDien.MEDIUM);
        noiDungKn.add(lbl, BorderLayout.NORTH);
        JScrollPane cuon = new JScrollPane(txaKhieuNaiNoiDung);
        cuon.setBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER));
        cuon.setPreferredSize(new java.awt.Dimension(100, 62));
        noiDungKn.add(cuon, BorderLayout.CENTER);
        noiDungChiTiet.add(noiDungKn, BorderLayout.SOUTH);
        khung.add(noiDungChiTiet, BorderLayout.CENTER);
        return khung;
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
    private static JTextArea taoVungVanBanChiDoc() {
        JTextArea a = new JTextArea(3, 30);
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(HangSoGiaoDien.NORMAL);
        a.setBackground(Color.WHITE);
        return a;
    }
    private void themDong(JPanel p, String ten, JLabel giaTri) {
        JLabel lbl = new JLabel(ten);
        lbl.setForeground(HangSoGiaoDien.MUTED);
        p.add(lbl);
        p.add(giaTri);
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            if (phanSauBan == PhanSauBan.BAO_HANH) {
                lamMoiBaoHanh();
            } else {
                lamMoiKhieuNai();
            }
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void lamMoiBaoHanh() throws Exception {
        List<PhieuBaoHanh> tatCa = dichVuBaoHanh.danhSachBaoHanh();
        capNhatTongHopBaoHanh(tatCa);
        duLieuBaoHanh.clear();
        moHinhBaoHanh.setRowCount(0);
        String tuKhoa = txtTimBaoHanh.getText().trim().toLowerCase(Locale.ROOT);
        String trangThaiLoc = maTrangThaiBaoHanhDangChon();
        for (PhieuBaoHanh r : tatCa) {
            String trangThai = r.getTrangThai();
            if (trangThaiLoc != null && !trangThaiLoc.equals(trangThai)) continue;
            String noiDungTim = (r.getMaPhieu() + " " + r.getKhachHang().getHoTen() + " " + DinhDangHienThi.anToan(r.getKhachHang().getSoDienThoai()) + " " + r.getSanPham().getTenSanPham() + " " + r.getNhanVien().getHoTen()).toLowerCase(Locale.ROOT);
            if (!tuKhoa.isEmpty() && !noiDungTim.contains(tuKhoa)) continue;
            duLieuBaoHanh.add(r);
            moHinhBaoHanh.addRow(new Object[] {
                r.getMaPhieu(), r.getKhachHang().getHoTen(), r.getSanPham().getTenSanPham(), DinhDangHienThi.ngay(r.getNgayTiepNhan()), DinhDangHienThi.ngay(r.getHanBaoHanh()), nhanTrangThaiBaoHanh(trangThai),
                r.getPhiDoiMay() == null ? "" : DinhDangHienThi.tien(r.getPhiDoiMay())
            });
        }
        if (moHinhBaoHanh.getRowCount() > 0) {
            TaoGiaoDien.chonDongDauTienSauKhiCapNhat(tblBaoHanh);
        } else {
            xoaChiTietBaoHanh();
        }
    }
    private void capNhatTongHopBaoHanh(List<PhieuBaoHanh> tatCa) {
        int repairing = 0, replacement = 0, ended = 0;
        for (PhieuBaoHanh r : tatCa) {
            String st = r.getTrangThai();
            if ("REPAIRING".equals(st)) repairing++;
            else if ("REPLACEMENT".equals(st))
                replacement++;
            else if ("Hoàn tất".equals(st) || "RETURNED".equals(st) || "REJECTED".equals(st))
                ended++;
        }
        lblTongBaoHanh.setText(String.valueOf(tatCa.size()));
        lblDangSua.setText(String.valueOf(repairing));
        lblDoiMay.setText(String.valueOf(replacement));
        lblDaKetThuc.setText(String.valueOf(ended));
    }
    private String maTrangThaiBaoHanhDangChon() {
        String s = String.valueOf(cmbTrangThaiBaoHanh.getSelectedItem());
        if ("Đang sửa chữa".equals(s)) return "REPAIRING";
        if ("Đổi máy".equals(s)) return "REPLACEMENT";
        if ("Hoàn tất".equals(s)) return "Hoàn tất";
        if ("Đã trả khách".equals(s)) return "RETURNED";
        if ("Từ chối".equals(s)) return "REJECTED";
        return null;
    }
    private void khiChonBaoHanh(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int vr = tblBaoHanh.getSelectedRow();
        if (vr < 0) {
            xoaChiTietBaoHanh();
            return;
        }
        int mr = tblBaoHanh.convertRowIndexToModel(vr);
        if (mr < 0 || mr >= duLieuBaoHanh.size()) {
            xoaChiTietBaoHanh();
            return;
        }
        PhieuBaoHanh r = duLieuBaoHanh.get(mr);
        lblChiTietMaPhieu.setText(r.getMaPhieu());
        lblChiTietKhachHang.setText(r.getKhachHang().getHoTen());
        lblChiTietSoDienThoai.setText(DinhDangHienThi.anToan(r.getKhachHang().getSoDienThoai()));
        lblChiTietSanPham.setText(r.getSanPham().getMaSanPham() + " - " + r.getSanPham().getTenSanPham());
        lblChiTietNhanVien.setText(r.getNhanVien().getMaNhanVien() + " - " + r.getNhanVien().getHoTen());
        lblChiTietNgayNhan.setText(DinhDangHienThi.ngay(r.getNgayTiepNhan()));
        lblChiTietHanBaoHanh.setText(DinhDangHienThi.ngay(r.getHanBaoHanh()));
        String st = r.getTrangThai();
        lblChiTietTrangThai.setText(nhanTrangThaiBaoHanh(st));
        lblChiTietPhiDoiMay.setText(r.getPhiDoiMay() == null ? "0 đ" : DinhDangHienThi.tien(r.getPhiDoiMay()));
        lblChiTietNgayHoanThanh.setText(dinhDangNgayGio(r.getNgayHoanThanh()));
        lblChiTietNgayTraKhach.setText(dinhDangNgayGio(r.getNgayTraKhach()));
        txaChiTietMoTaLoi.setText(DinhDangHienThi.anToan(r.getMoTaLoi()));
        btnCapNhatBaoHanh.setEnabled(trangThaiBaoHanhTiepTheo(st).length > 0 && TaiKhoanService.coQuyen( "WARRANTY_MANAGE"));
    }
    private void xoaChiTietBaoHanh() {
        lblChiTietMaPhieu.setText("-");
        lblChiTietKhachHang.setText("-");
        lblChiTietSoDienThoai.setText("-");
        lblChiTietSanPham.setText("-");
        lblChiTietNhanVien.setText("-");
        lblChiTietNgayNhan.setText("-");
        lblChiTietHanBaoHanh.setText("-");
        lblChiTietTrangThai.setText("-");
        lblChiTietPhiDoiMay.setText("-");
        lblChiTietNgayHoanThanh.setText("-");
        lblChiTietNgayTraKhach.setText("-");
        txaChiTietMoTaLoi.setText("");
        if (btnCapNhatBaoHanh != null) btnCapNhatBaoHanh.setEnabled(false);
    }
    private void taoBaoHanh() {
        try {
            JComboBox<String> cboKhachHang = TaoGiaoDien.oChon();
            Map<String, String> soDienThoaiTheoDong = new LinkedHashMap<String, String>();
            for (KhachHang r : dichVuKhachHang.timKiemKhachHang("%", null)) {
                if (!"Hoạt động".equals(r.getTrangThai())) continue;
                String dong = r.getMaKhachHang() + " - " + r.getHoTen();
                cboKhachHang.addItem(dong);
                soDienThoaiTheoDong.put(dong, DinhDangHienThi.anToan(r.getSoDienThoai()));
            }
            if (cboKhachHang.getItemCount() == 0) {
                thongBao("Không có khách hàng đang hoạt động.");
                return;
            }
            JTextField txtMaPhieu = TaoGiaoDien.oNhap(180);
            TaoGiaoDien.datGoiYMa(txtMaPhieu, dichVuBaoHanh.goiYMaBaoHanh());
            JTextField txtSoDienThoai = TaoGiaoDien.oNhap(180);
            JTextField txtHanBaoHanh = TaoGiaoDien.oNhap(220);
            TaoGiaoDien.datChiDoc(txtSoDienThoai, "");
            TaoGiaoDien.datChiDoc(txtHanBaoHanh, "-");
            JComboBox<LuaChonBaoHanh> cmbLuaChon = new JComboBox<LuaChonBaoHanh>();
            JTextArea txaIssue = TaoGiaoDien.vungNhap(4, 34);
            Runnable taiSanPham = () -> {
                try {
                    cmbLuaChon.removeAllItems();
                    String dong = String.valueOf(cboKhachHang.getSelectedItem());
                    String phone = soDienThoaiTheoDong.get(dong);
                    TaoGiaoDien.datChiDoc(txtSoDienThoai, phone);
                    if (phone == null || phone.isBlank()) {
                        TaoGiaoDien.datChiDoc(txtHanBaoHanh, "Khách hàng chưa có số điện thoại");
                        return;
                    }
                    for (Map<String, Object> r : dichVuBaoHanh.luaChonBaoHanh(phone)) cmbLuaChon.addItem(new LuaChonBaoHanh(r));
                    LuaChonBaoHanh o = (LuaChonBaoHanh) cmbLuaChon.getSelectedItem();
                    TaoGiaoDien.datChiDoc(txtHanBaoHanh, o == null ? "Không có sản phẩm còn bảo hành" : o.hanBaoHanh.format( DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (" + o.soThangBaoHanh + " tháng)");
                } catch (Exception ex) {
                    cmbLuaChon.removeAllItems();
                    TaoGiaoDien.datChiDoc(txtHanBaoHanh, "Không tải được dữ liệu bảo hành");
                }
            };
            cboKhachHang.addActionListener(e -> taiSanPham.run());
            cmbLuaChon.addActionListener(e -> {
                LuaChonBaoHanh o = (LuaChonBaoHanh) cmbLuaChon.getSelectedItem();
                TaoGiaoDien.datChiDoc(txtHanBaoHanh, o == null ? "Không có sản phẩm còn bảo hành" : o.hanBaoHanh.format( DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " (" + o.soThangBaoHanh + " tháng)");
            });
            taiSanPham.run();
            JPanel thongTin = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Mã phiếu bảo hành", txtMaPhieu), TaoGiaoDien.truongBieuMau("Khách hàng", cboKhachHang), TaoGiaoDien.truongBieuMau(
                "Số điện thoại", txtSoDienThoai), TaoGiaoDien.truongBieuMau("Sản phẩm đã mua", cmbLuaChon), TaoGiaoDien.truongBieuMau("Hạn bảo hành", txtHanBaoHanh));
            JPanel p = new JPanel(new BorderLayout(0, 10));
            p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            p.add(thongTin, BorderLayout.NORTH);
            p.add(TaoGiaoDien.truongBieuMau("Mô tả lỗi", TaoGiaoDien.cuonVungNhap(txaIssue, 100)), BorderLayout.CENTER);
            if (!TienIchHopThoai.xacNhanBieuMau(this, p, "Tiếp nhận bảo hành", "Tạo phiếu bảo hành")) return;
            LuaChonBaoHanh luaChon = (LuaChonBaoHanh) cmbLuaChon.getSelectedItem();
            if (luaChon == null) throw new IllegalArgumentException("Khách hàng không có sản phẩm còn thời hạn bảo hành.");
            String id = dichVuBaoHanh.taoBaoHanh(txtMaPhieu.getText(), luaChon.maKhachHang, luaChon.maHoaDon, luaChon.maSanPham, txaIssue.getText().trim());
            thongBao("Đã tạo phiếu bảo hành " + id + ".");
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void capNhatBaoHanh() {
        int vr = tblBaoHanh.getSelectedRow();
        if (vr < 0) {
            thongBao("Hãy chọn phiếu bảo hành.");
            return;
        }
        int r = tblBaoHanh.convertRowIndexToModel(vr);
        String id = String.valueOf(moHinhBaoHanh.getValueAt(r, 0));
        String maTrangThaiCu = duLieuBaoHanh.get(r).getTrangThai();
        String[] cacMa = trangThaiBaoHanhTiepTheo(maTrangThaiCu);
        if (cacMa.length == 0) {
            thongBao("Phiếu bảo hành này đã kết thúc, không thể chuyển trạng thái tiếp.");
            return;
        }
        String[] nhan = new String[cacMa.length];
        for (int i = 0; i < cacMa.length; i++) nhan[i] = nhanTrangThaiBaoHanh(cacMa[i]);
        JComboBox<String> cboTrangThai = TaoGiaoDien.oChon(nhan);
        JPanel chonTrangThai = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Trạng thái mới", cboTrangThai));
        if (!TienIchHopThoai.xacNhanBieuMau(this, chonTrangThai, "Cập nhật bảo hành", "Tiếp tục")) return;
        String maTrangThai = maTrangThaiBaoHanh(String.valueOf(cboTrangThai.getSelectedItem()));
        try {
            BigDecimal phiPhuKien = BigDecimal.ZERO;
            if ("REPLACEMENT".equals(maTrangThai)) {
                JTextField txtPhi = TaoGiaoDien.oNhap(180);
                txtPhi.setText("0");
                JPanel p = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Phí phụ kiện thiếu/hư hỏng (VNĐ)", txtPhi));
                if (!TienIchHopThoai.xacNhanBieuMau(this, p, "Phí đổi máy", "Tính phí")) return;
                phiPhuKien = new BigDecimal(txtPhi.getText().trim().isEmpty() ? "0" : txtPhi.getText().trim());
                BigDecimal charge = dichVuBaoHanh.tinhPhiDoiMay(id, phiPhuKien);
                if (!xacNhan("Phí khách cần thanh toán khi đổi máy: " + DinhDangHienThi.tien(charge) + "\nXác nhận chuyển sang xử lý đổi máy?")) return;
            }
            dichVuBaoHanh.capNhatTrangThaiBaoHanh(id, maTrangThai, null, phiPhuKien);
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private String[] trangThaiBaoHanhTiepTheo(String cu) {
        if ("REPAIRING".equals(cu)) return new String[] {
            "Hoàn tất", "REPLACEMENT", "REJECTED"
        };
        if ("REPLACEMENT".equals(cu)) return new String[] {
            "Hoàn tất"
        };
        if ("Hoàn tất".equals(cu)) return new String[] {
            "RETURNED"
        };
        return new String[0];
    }
    private String nhanTrangThaiBaoHanh(String ma) {
        if ("REPAIRING".equals(ma)) return "Đang sửa chữa";
        if ("REPLACEMENT".equals(ma)) return "Đổi máy";
        if ("Hoàn tất".equals(ma)) return "Hoàn tất";
        if ("RETURNED".equals(ma)) return "Đã trả khách";
        if ("REJECTED".equals(ma)) return "Từ chối";
        return ma;
    }
    private String maTrangThaiBaoHanh(String nhan) {
        if ("Đang sửa chữa".equals(nhan)) return "REPAIRING";
        if ("Đổi máy".equals(nhan)) return "REPLACEMENT";
        if ("Hoàn tất".equals(nhan)) return "Hoàn tất";
        if ("Đã trả khách".equals(nhan)) return "RETURNED";
        if ("Từ chối".equals(nhan)) return "REJECTED";
        return nhan;
    }
    private void lamMoiKhieuNai() throws Exception {
        dichVuKhieuNai.capNhatLeoThangKhieuNai();
        List<KhieuNai> tatCa = dichVuKhieuNai.danhSachKhieuNai();
        capNhatTongHopKhieuNai(tatCa);
        duLieuKhieuNai.clear();
        moHinhKhieuNai.setRowCount(0);
        String tuKhoa = txtTimKhieuNai.getText().trim().toLowerCase(Locale.ROOT);
        String trangThaiLoc = maTrangThaiKhieuNaiDangChon();
        String uuTienLoc = maUuTienKhieuNaiDangChon();
        for (KhieuNai r : tatCa) {
            String st = r.getTrangThai();
            String priority = r.getMucUuTien();
            if (trangThaiLoc != null && !trangThaiLoc.equals(st)) continue;
            if (uuTienLoc != null && !uuTienLoc.equals(priority)) continue;
            String noiDungTim = (r.getMaKhieuNai() + " " + r.getKhachHang().getHoTen() + " " + DinhDangHienThi.anToan(r.getSoDienThoaiKhach()) + " " + r.getNoiDung() + " " + (r.getNguoiPhuTrach() == null ? "" : r.getNguoiPhuTrach().getHoTen())).toLowerCase(Locale.ROOT);
            if (!tuKhoa.isEmpty() && !noiDungTim.contains(tuKhoa)) continue;
            duLieuKhieuNai.add(r);
            moHinhKhieuNai.addRow(new Object[] {
                r.getMaKhieuNai(), r.getKhachHang().getHoTen(), DinhDangHienThi.anToan(r.getSoDienThoaiKhach()), dinhDangNgayGio(r.getNgayGhiNhan()), nhanUuTienKhieuNai(priority), nhanTrangThaiKhieuNai(st),
                r.getNguoiPhuTrach() == null ? "" : r.getNguoiPhuTrach().getHoTen(), dinhDangNgayGio(r.getHanXuLy())
            });
        }
        if (moHinhKhieuNai.getRowCount() > 0) TaoGiaoDien.chonDongDauTienSauKhiCapNhat(tblKhieuNai);
        else xoaChiTietKhieuNai();
    }
    private void capNhatTongHopKhieuNai(List<KhieuNai> tatCa) {
        int dangXuLy = 0, daGiaiQuyet = 0, leoThang = 0;
        for (KhieuNai r : tatCa) {
            String st = r.getTrangThai();
            if ("NEW".equals(st) || "IN_PROGRESS".equals(st) || "WAITING_CUSTOMER".equals(st)) dangXuLy++;
            if ("RESOLVED".equals(st) || "CLOSED".equals(st)) daGiaiQuyet++;
            if (r.isDaLeoThang()) leoThang++;
        }
        lblTongKhieuNai.setText(String.valueOf(tatCa.size()));
        lblDangXuLyKhieuNai.setText(String.valueOf(dangXuLy));
        lblDaGiaiQuyetKhieuNai.setText(String.valueOf(daGiaiQuyet));
        lblLeoThangKhieuNai.setText(String.valueOf(leoThang));
    }
    private void khiChonKhieuNai(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int vr = tblKhieuNai.getSelectedRow();
        if (vr < 0) {
            xoaChiTietKhieuNai();
            return;
        }
        int mr = tblKhieuNai.convertRowIndexToModel(vr);
        if (mr < 0 || mr >= duLieuKhieuNai.size()) {
            xoaChiTietKhieuNai();
            return;
        }
        KhieuNai r = duLieuKhieuNai.get(mr);
        String st = r.getTrangThai();
        lblKhieuNaiMa.setText(r.getMaKhieuNai());
        lblKhieuNaiKhachHang.setText(r.getKhachHang().getMaKhachHang() + " - " + r.getKhachHang().getHoTen());
        lblKhieuNaiSoDienThoai.setText(DinhDangHienThi.anToan(r.getSoDienThoaiKhach()));
        lblKhieuNaiNguoiTao.setText(r.getNguoiTao() == null ? "" : r.getNguoiTao().getMaNhanVien() + " - " + r.getNguoiTao().getHoTen());
        lblKhieuNaiPhuTrach.setText(r.getNguoiPhuTrach() == null ? "" : r.getNguoiPhuTrach().getMaNhanVien() + " - " + r.getNguoiPhuTrach().getHoTen());
        lblKhieuNaiNgayGhiNhan.setText(dinhDangNgayGio(r.getNgayGhiNhan()));
        lblKhieuNaiHanXuLy.setText(dinhDangNgayGio(r.getHanXuLy()));
        lblKhieuNaiUuTien.setText(nhanUuTienKhieuNai(r.getMucUuTien()));
        lblKhieuNaiTrangThai.setText(nhanTrangThaiKhieuNai(st));
        lblKhieuNaiLeoThang.setText(r.isDaLeoThang() ? "Đã leo thang" : "Chưa leo thang");
        lblKhieuNaiNgayGiaiQuyet.setText(dinhDangNgayGio(r.getNgayGiaiQuyet()));
        txaKhieuNaiNoiDung.setText(DinhDangHienThi.anToan(r.getNoiDung()));
        btnCapNhatKhieuNai.setEnabled(trangThaiKhieuNaiTiepTheo(st).length > 0 && TaiKhoanService.coQuyen( "COMPLAINT_MANAGE"));
    }
    private void xoaChiTietKhieuNai() {
        lblKhieuNaiMa.setText("-");
        lblKhieuNaiKhachHang.setText("-");
        lblKhieuNaiSoDienThoai.setText("-");
        lblKhieuNaiNguoiTao.setText("-");
        lblKhieuNaiPhuTrach.setText("-");
        lblKhieuNaiNgayGhiNhan.setText("-");
        lblKhieuNaiHanXuLy.setText("-");
        lblKhieuNaiUuTien.setText("-");
        lblKhieuNaiTrangThai.setText("-");
        lblKhieuNaiLeoThang.setText("-");
        lblKhieuNaiNgayGiaiQuyet.setText("-");
        txaKhieuNaiNoiDung.setText("");
        if (btnCapNhatKhieuNai != null) btnCapNhatKhieuNai.setEnabled(false);
    }
    private String maTrangThaiKhieuNaiDangChon() {
        String s = String.valueOf(cmbTrangThaiKhieuNai.getSelectedItem());
        if ("Chưa xử lý".equals(s)) return "NEW";
        if ("Đang xử lý".equals(s)) return "IN_PROGRESS";
        if ("Chờ khách hàng".equals(s)) return "WAITING_CUSTOMER";
        if ("Đã xử lý".equals(s)) return "RESOLVED";
        if ("Đã đóng".equals(s)) return "CLOSED";
        return null;
    }
    private String maUuTienKhieuNaiDangChon() {
        String s = String.valueOf(cmbUuTienKhieuNai.getSelectedItem());
        if ("Bình thường".equals(s)) return "Bình thường";
        if ("Khẩn cấp".equals(s)) return "Khẩn cấp";
        return null;
    }
    private String nhanTrangThaiKhieuNai(String ma) {
        if ("NEW".equals(ma)) return "Chưa xử lý";
        if ("IN_PROGRESS".equals(ma)) return "Đang xử lý";
        if ("WAITING_CUSTOMER".equals(ma)) return "Chờ khách hàng";
        if ("RESOLVED".equals(ma)) return "Đã xử lý";
        if ("CLOSED".equals(ma)) return "Đã đóng";
        return ma;
    }
    private String maTrangThaiKhieuNai(String nhan) {
        if ("Chưa xử lý".equals(nhan)) return "NEW";
        if ("Đang xử lý".equals(nhan)) return "IN_PROGRESS";
        if ("Chờ khách hàng".equals(nhan)) return "WAITING_CUSTOMER";
        if ("Đã xử lý".equals(nhan)) return "RESOLVED";
        if ("Đã đóng".equals(nhan)) return "CLOSED";
        return nhan;
    }
    private String nhanUuTienKhieuNai(String ma) {
        if ("Khẩn cấp".equals(ma)) return "Khẩn cấp";
        return "Bình thường";
    }
    private void taoKhieuNai() {
        try {
            JTextField txtMaKhieuNai = TaoGiaoDien.oNhap(160);
            TaoGiaoDien.datGoiYMa(txtMaKhieuNai, dichVuKhieuNai.goiYMaKhieuNai());
            JComboBox<String> cboKhachHang = TaoGiaoDien.oChon();
            JTextField txtSoDienThoai = TaoGiaoDien.oNhap(180);
            TaoGiaoDien.datChiDoc(txtSoDienThoai, "");
            Map<String, String> maKhachTheoDong = new LinkedHashMap<String, String>();
            Map<String, String> sdtTheoDong = new LinkedHashMap<String, String>();
            for (KhachHang r : dichVuKhachHang.timKiemKhachHang("%", null)) {
                if (!"Hoạt động".equals(r.getTrangThai())) continue;
                String dong = r.getMaKhachHang() + " - " + r.getHoTen();
                cboKhachHang.addItem(dong);
                maKhachTheoDong.put(dong, r.getMaKhachHang());
                sdtTheoDong.put(dong, DinhDangHienThi.anToan(r.getSoDienThoai()));
            }
            if (cboKhachHang.getItemCount() == 0) {
                thongBao("Không có khách hàng đang hoạt động để tiếp nhận khiếu nại.");
                return;
            }
            Runnable capNhatSdt = () -> TaoGiaoDien.datChiDoc(txtSoDienThoai, sdtTheoDong.get(String.valueOf( cboKhachHang.getSelectedItem())));
            cboKhachHang.addActionListener(e -> capNhatSdt.run());
            capNhatSdt.run();
            JComboBox<String> cmbUuTien = TaoGiaoDien.oChon("Bình thường", "Khẩn cấp");
            JTextArea txaContent = TaoGiaoDien.vungNhap(5, 34);
            JPanel thongTin = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Mã khiếu nại", txtMaKhieuNai), TaoGiaoDien.truongBieuMau("Khách hàng", cboKhachHang), TaoGiaoDien.truongBieuMau(
                "Số điện thoại", txtSoDienThoai), TaoGiaoDien.truongBieuMau("Mức ưu tiên", cmbUuTien));
            JPanel p = new JPanel(new BorderLayout(0, 10));
            p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            p.add(thongTin, BorderLayout.NORTH);
            p.add(TaoGiaoDien.truongBieuMau("Nội dung khiếu nại", TaoGiaoDien.cuonVungNhap(txaContent, 110)), BorderLayout.CENTER);
            if (!TienIchHopThoai.xacNhanBieuMau(this, p, "Tiếp nhận khiếu nại", "Tạo khiếu nại")) return;
            String dong = String.valueOf(cboKhachHang.getSelectedItem());
            String maKhachHang = maKhachTheoDong.get(dong);
            String soDienThoai = sdtTheoDong.get(dong);
            String uuTien = "Khẩn cấp".equals(String.valueOf(cmbUuTien.getSelectedItem())) ? "Khẩn cấp" : "Bình thường";
            String id = dichVuKhieuNai.taoKhieuNai(txtMaKhieuNai.getText(), maKhachHang, soDienThoai, txaContent.getText(), uuTien);
            thongBao("Đã tạo khiếu nại " + id + ". Hạn xử lý được đặt sau 48 giờ theo nghiệp vụ hiện tại.");
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private void capNhatKhieuNai() {
        int vr = tblKhieuNai.getSelectedRow();
        if (vr < 0) {
            thongBao("Hãy chọn khiếu nại.");
            return;
        }
        int r = tblKhieuNai.convertRowIndexToModel(vr);
        if (r < 0 || r >= duLieuKhieuNai.size()) return;
        KhieuNai duLieu = duLieuKhieuNai.get(r);
        String id = duLieu.getMaKhieuNai();
        String maTrangThaiCu = duLieu.getTrangThai();
        String[] cacMa = trangThaiKhieuNaiTiepTheo(maTrangThaiCu);
        if (cacMa.length == 0) {
            thongBao("Khiếu nại này đã đóng, không thể cập nhật tiếp.");
            return;
        }
        String[] nhan = new String[cacMa.length];
        for (int i = 0; i < cacMa.length; i++) nhan[i] = nhanTrangThaiKhieuNai(cacMa[i]);
        JComboBox<String> cboTrangThai = TaoGiaoDien.oChon(nhan);
        JPanel p = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Trạng thái mới", cboTrangThai));
        if (!TienIchHopThoai.xacNhanBieuMau(this, p, "Cập nhật khiếu nại", "Cập nhật")) return;
        try {
            dichVuKhieuNai.capNhatTrangThaiKhieuNai(id, maTrangThaiKhieuNai(String.valueOf(cboTrangThai.getSelectedItem())), null);
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
    private String[] trangThaiKhieuNaiTiepTheo(String cu) {
        if ("NEW".equals(cu)) return new String[] {
            "IN_PROGRESS"
        };
        if ("IN_PROGRESS".equals(cu)) return new String[] {
            "WAITING_CUSTOMER", "RESOLVED"
        };
        if ("WAITING_CUSTOMER".equals(cu)) return new String[] {
            "IN_PROGRESS", "RESOLVED"
        };
        if ("RESOLVED".equals(cu)) return new String[] {
            "CLOSED"
        };
        return new String[0];
    }
    private String dinhDangNgayGio(Object giaTri) {
        if (giaTri == null) return "";
        if (giaTri instanceof Timestamp) {
            return((Timestamp) giaTri).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        if (giaTri instanceof java.sql.Date) return((java.sql.Date) giaTri).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return String.valueOf(giaTri);
    }
    private static class LuaChonBaoHanh {
        final String maKhachHang;
        final String tenKhachHang;
        final String maHoaDon;
        final String maSanPham;
        final String nhan;
        final LocalDate hanBaoHanh;
        final int soThangBaoHanh;
        LuaChonBaoHanh(Map<String, Object> r) {
            maKhachHang = String.valueOf(r.get("customer_id"));
            tenKhachHang = String.valueOf(r.get("full_name"));
            maHoaDon = String.valueOf(r.get("invoice_id"));
            maSanPham = String.valueOf(r.get("product_id"));
            soThangBaoHanh = ((Number) r.get("warranty_months")).intValue();
            Object ketThuc = r.get("warranty_end_date");
            hanBaoHanh = ketThuc instanceof java.sql.Date ? ((java.sql.Date) ketThuc).toLocalDate() : LocalDate .parse(String.valueOf(ketThuc));
            nhan = r.get("product_name") + " - mua " + DinhDangHienThi.ngay(r.get("created_at")) + " - còn " + r.get("remaining_qty");
        }
        @Override
        public String toString() {
            return nhan;
        }
    }
}

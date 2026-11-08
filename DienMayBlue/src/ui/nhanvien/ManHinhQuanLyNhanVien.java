package ui.nhanvien;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import entity.*;
import service.*;
import ui.dungchung.*;
public class ManHinhQuanLyNhanVien extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final JTextField txtTimKiem = TaoGiaoDien.oNhap(330);
    private final JComboBox<String> cmbLoaiNhanVien = TaoGiaoDien.oChon("Tất cả loại nhân viên", "Quản lý", "Bán hàng", "CSKH");
    private final JLabel lblTongNhanVien = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinh = TaoGiaoDien.moHinhChiDoc(new Object[] {
        "Mã NV", "Họ tên", "Số điện thoại", "Thư điện tử", "Loại nhân viên", "Trình độ", "Hệ số lương", "Lương cơ bản", "Ngày vào làm"
    });
    private final JTable tblDanhSach = TaoGiaoDien.bang(moHinh);
    private final NhanVienService dichVu = new NhanVienService();
    public ManHinhQuanLyNhanVien() {
        super("Quản lý nhân viên", "Quản lý hồ sơ nhân viên đang thuộc hệ thống.");
        TaoGiaoDien.datChuGoiY(txtTimKiem, "Mã NV, họ tên, SĐT hoặc email");
        JButton btnThem = TaoGiaoDien.nutChinh("Thêm nhân viên");
        JButton btnSua = TaoGiaoDien.nutPhu("Cập nhật hồ sơ");
        JButton btnXoa = TaoGiaoDien.nutPhu("Xóa nhân viên");
        thaoTac.add(btnThem); thaoTac.add(btnSua); thaoTac.add(btnXoa);
        JPanel pnlKhung = new JPanel(new BorderLayout(0, 12));
        pnlKhung.setOpaque(false);
        pnlKhung.add(taoKhungTongHop(), BorderLayout.NORTH);
        JPanel pnlTrungTam = new JPanel(new BorderLayout(0, 10));
        pnlTrungTam.setOpaque(false);
        pnlTrungTam.add(TaoGiaoDien.thanhCongCu(new JLabel("Tìm kiếm"), txtTimKiem, new JLabel("Loại nhân viên"), cmbLoaiNhanVien), BorderLayout.NORTH);
        BangPhanTrang pnlCuon = TaoGiaoDien.bangPhanTrang(tblDanhSach);
        tblDanhSach.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        datDoRongCot();
        pnlTrungTam.add(pnlCuon, BorderLayout.CENTER);
        pnlKhung.add(pnlTrungTam, BorderLayout.CENTER);
        noiDung.add(pnlKhung, BorderLayout.CENTER);
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimKiem, tblDanhSach, 0, m -> lamMoiDuLieu(), 0, 1, 2, 3);
        cmbLoaiNhanVien.addActionListener(e -> lamMoiDuLieu());
        btnThem.addActionListener(e -> taoNhanVien());
        btnSua.addActionListener(e -> suaNhanVien());
        btnXoa.addActionListener(e -> xoaNhanVien());
    }
    private JPanel taoKhungTongHop() {
        JPanel pnlTongHop = new JPanel(new GridLayout(1, 1, 10, 0));
        pnlTongHop.setOpaque(false);
        pnlTongHop.add(taoTheTongHop("Nhân viên đang quản lý", lblTongNhanVien));
        return pnlTongHop;
    }
    private JPanel taoTheTongHop(String tieuDe, JLabel lblGiaTri) {
        KhungBoGoc pnlThe = new KhungBoGoc(12, Color.WHITE);
        pnlThe.setLayout(new BorderLayout(0, 6));
        pnlThe.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel lblTieuDe = new JLabel(tieuDe); lblTieuDe.setFont(HangSoGiaoDien.MEDIUM); lblTieuDe.setForeground(HangSoGiaoDien.MUTED);
        pnlThe.add(lblTieuDe, BorderLayout.NORTH); pnlThe.add(lblGiaTri, BorderLayout.CENTER);
        return pnlThe;
    }
    private static JLabel taoGiaTriTongHop(String vanBan) {
        JLabel lblGiaTri = new JLabel(vanBan); lblGiaTri.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 24)); lblGiaTri.setForeground(HangSoGiaoDien.TEXT); return lblGiaTri;
    }
    private void datDoRongCot() {
        int[] rong = {75, 155, 110, 200, 125, 105, 90, 120, 105};
        for (int i = 0; i < rong.length && i < tblDanhSach.getColumnCount(); i++) tblDanhSach.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
    }
    private JComboBox<String> taoComboLoai(String giaTri) {
        JComboBox<String> cmbLoai = TaoGiaoDien.oChon("Quản lý", "Bán hàng", "CSKH");
        if (giaTri != null) cmbLoai.setSelectedItem(giaTri);
        return cmbLoai;
    }
    @Override
    public void lamMoiDuLieu() {
        try {
            String kw = "%" + txtTimKiem.getText().trim() + "%";
            String loaiDangChon = String.valueOf(cmbLoaiNhanVien.getSelectedItem());
            List<NhanVien> cacDong = dichVu.timKiemNhanVien(kw, "Hoạt động");
            moHinh.setRowCount(0);
            for (NhanVien r : cacDong) {
                String loai = DinhDangHienThi.anToan(r.getPhongBan());
                if (!"Tất cả loại nhân viên".equals(loaiDangChon) && !loaiDangChon.equals(loai)) continue;
                moHinh.addRow(new Object[] {r.getMaNhanVien(), r.getHoTen(), r.getSoDienThoai(), DinhDangHienThi.anToan(r.getEmail()), loai,
                    DinhDangHienThi.anToan(r.getTrinhDo()), r.getHeSoLuong() == null ? "" : r.getHeSoLuong(), dinhDangLuong(r.getLuongCoBan()), DinhDangHienThi.ngay(r.getNgayVaoLam())});
            }
            lblTongNhanVien.setText(String.valueOf(moHinh.getRowCount()));
        } catch (Exception ex) { hienLoi(ex); }
    }
    private String maNhanVienDangChon() {
        int vr = tblDanhSach.getSelectedRow();
        return vr < 0 ? null : String.valueOf(moHinh.getValueAt(tblDanhSach.convertRowIndexToModel(vr), 0));
    }
    private void taoNhanVien() {
        JTextField txtMa = TaoGiaoDien.oNhap(180);
        try { TaoGiaoDien.datGoiYMa(txtMa, dichVu.goiYMaNhanVien()); } catch (Exception ex) { hienLoi(ex); return; }
        hienBieuMauNhanVien(null, txtMa);
    }
    private void suaNhanVien() {
        String ma = maNhanVienDangChon();
        if (ma == null) { thongBao("Hãy chọn nhân viên cần cập nhật."); return; }
        JTextField txtMaChiDoc = TaoGiaoDien.oNhap(180); TaoGiaoDien.datChiDoc(txtMaChiDoc, ma);
        hienBieuMauNhanVien(ma, txtMaChiDoc);
    }
    private void hienBieuMauNhanVien(String maCu, JTextField txtMa) {
        try {
            NhanVien nv = maCu == null ? null : dichVu.timNhanVien(maCu);
            JTextField txtTen = TaoGiaoDien.oNhap(260), txtSoDienThoai = TaoGiaoDien.oNhap(180), txtEmail = TaoGiaoDien.oNhap(260), txtNgay = TaoGiaoDien.oNhap(160), txtHeSo = TaoGiaoDien.oNhap(120), txtLuong = TaoGiaoDien.oNhap(180);
            JComboBox<String> cmbLoai = taoComboLoai(nv == null ? null : nv.getPhongBan());
            JComboBox<String> cmbTrinhDo = TaoGiaoDien.oChon("Trung cấp", "Cao đẳng", "Đại học", "Sau đại học");
            if (nv == null) txtNgay.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            else {
                txtTen.setText(nv.getHoTen()); txtSoDienThoai.setText(nv.getSoDienThoai()); txtEmail.setText(DinhDangHienThi.anToan(nv.getEmail()));
                txtNgay.setText(nv.getNgayVaoLam().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))); cmbTrinhDo.setSelectedItem(nv.getTrinhDo());
                txtHeSo.setText(String.valueOf(nv.getHeSoLuong())); txtLuong.setText(nv.getLuongCoBan().toPlainString());
            }
            JPanel pnlBieuMau = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Mã nhân viên", txtMa), TaoGiaoDien.truongBieuMau("Họ tên", txtTen), TaoGiaoDien.truongBieuMau("Số điện thoại", txtSoDienThoai),
                TaoGiaoDien.truongBieuMau("Thư điện tử", txtEmail), TaoGiaoDien.truongBieuMau("Loại nhân viên", cmbLoai), TaoGiaoDien.truongBieuMau("Ngày vào làm (dd/MM/yyyy)", txtNgay),
                TaoGiaoDien.truongBieuMau("Trình độ", cmbTrinhDo), TaoGiaoDien.truongBieuMau("Hệ số lương", txtHeSo), TaoGiaoDien.truongBieuMau("Lương cơ bản (VNĐ)", txtLuong));
            while (true) {
                if (!TienIchHopThoai.xacNhanBieuMau(this, pnlBieuMau, nv == null ? "Thêm nhân viên" : "Cập nhật nhân viên", nv == null ? "Thêm nhân viên" : "Lưu thay đổi")) return;
                try {
                    String maNhap = txtMa.getText().trim().toUpperCase(Locale.ROOT);
                    String tenNhap = txtTen.getText().trim();
                    String sdtNhap = txtSoDienThoai.getText().trim();
                    String emailNhap = txtEmail.getText().trim();
                    String ngayNhap = txtNgay.getText().trim();
                    String heSoNhap = txtHeSo.getText().trim();
                    String luongNhap = txtLuong.getText().trim();
                    if (maNhap.isEmpty() || tenNhap.isEmpty() || sdtNhap.isEmpty() || emailNhap.isEmpty() || ngayNhap.isEmpty() || heSoNhap.isEmpty() || luongNhap.isEmpty()) {
                        throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin.");
                    }
                    if (!maNhap.matches("NV\\d{3}")) throw new IllegalArgumentException("Mã nhân viên phải theo dạng NV001.");
                    if (!sdtNhap.matches("0\\d{9}")) throw new IllegalArgumentException("Số điện thoại phải gồm 10 số, bắt đầu bằng 0.");
                    LocalDate ngayVaoLam;
                    try {
                        ngayVaoLam = LocalDate.parse(ngayNhap, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT));
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Ngày vào làm phải theo dạng dd/MM/yyyy.");
                    }
                    NhanVien moi = new NhanVien(maNhap, tenNhap, sdtNhap, emailNhap, ngayVaoLam,
                        String.valueOf(cmbTrinhDo.getSelectedItem()), Double.valueOf(heSoNhap), new BigDecimal(luongNhap), String.valueOf(cmbLoai.getSelectedItem()), "Hoạt động");
                    if (nv == null) dichVu.taoNhanVien(moi); else dichVu.capNhatNhanVien(moi);
                    lamMoiDuLieu();
                    return;
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Thông tin chưa hợp lệ", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) { hienLoi(ex); }
    }
    private void xoaNhanVien() {
        String ma = maNhanVienDangChon();
        if (ma == null) { thongBao("Hãy chọn nhân viên cần xóa."); return; }
        if (!xacNhan("Xóa nhân viên " + ma + " khỏi hệ thống? Nhân viên đã phát sinh nghiệp vụ sẽ không được phép xóa.")) return;
        try { dichVu.xoaNhanVien(ma); lamMoiDuLieu(); } catch (Exception ex) { hienLoi(ex); }
    }
    private String dinhDangLuong(Object giaTri) {
        if (giaTri == null) return "";
        try { return DinhDangHienThi.tien(new BigDecimal(String.valueOf(giaTri))); } catch (Exception ex) { return String.valueOf(giaTri); }
    }
}
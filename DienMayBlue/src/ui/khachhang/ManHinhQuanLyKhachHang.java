package ui.khachhang;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import entity.KhachHang;
import service.KhachHangService;
import ui.dungchung.*;

public class ManHinhQuanLyKhachHang extends ManHinhCoSo {
    private static final long serialVersionUID = 1L;
    private final KhachHangService dichVu = new KhachHangService();
    private final JTextField txtTimKiem = TaoGiaoDien.oNhap(360);
    private final JLabel lblTongKhachHang = taoGiaTriTongHop("0");
    private final DefaultTableModel moHinh = TaoGiaoDien.moHinhChiDoc(new Object[] {
            "Mã KH", "Họ tên", "Số điện thoại", "Thư điện tử", "Địa chỉ"
    });
    private final JTable tblDanhSach = TaoGiaoDien.bang(moHinh);

    public ManHinhQuanLyKhachHang() {
        super("Quản lý khách hàng",
                "Quản lý hồ sơ khách hàng. Khách hàng đã phát sinh nghiệp vụ được giữ lại để bảo toàn lịch sử.");
        TaoGiaoDien.datChuGoiY(txtTimKiem, "Mã KH, họ tên hoặc SĐT");
        JButton btnThem = TaoGiaoDien.nutChinh("Thêm khách hàng");
        JButton btnSua = TaoGiaoDien.nutPhu("Cập nhật hồ sơ");
        JButton btnXoa = TaoGiaoDien.nutPhu("Xóa khách hàng");
        thaoTac.add(btnThem);
        thaoTac.add(btnSua);
        thaoTac.add(btnXoa);
        JPanel khung = new JPanel(new BorderLayout(0, 12));
        khung.setOpaque(false);
        khung.add(taoKhungTongHop(), BorderLayout.NORTH);
        JPanel trungTam = new JPanel(new BorderLayout(0, 10));
        trungTam.setOpaque(false);
        trungTam.add(TaoGiaoDien.thanhCongCu(new JLabel("Tìm kiếm"), txtTimKiem), BorderLayout.NORTH);
        BangPhanTrang cuon = TaoGiaoDien.bangPhanTrang(tblDanhSach);
        tblDanhSach.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] rong = { 85, 190, 120, 220, 420 };
        for (int i = 0; i < rong.length && i < tblDanhSach.getColumnCount(); i++)
            tblDanhSach.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        trungTam.add(cuon, BorderLayout.CENTER);
        khung.add(trungTam, BorderLayout.CENTER);
        noiDung.add(khung, BorderLayout.CENTER);
        txtTimKiem.addActionListener(e -> lamMoiDuLieu());
        GoiYTimKiem.ganTuBang(txtTimKiem, tblDanhSach, 0, m -> lamMoiDuLieu(), 0, 1, 2);
        btnThem.addActionListener(e -> bieuMau(null));
        btnSua.addActionListener(e -> suaDangChon());
        btnXoa.addActionListener(e -> xoaDangChon());
    }

    private JPanel taoKhungTongHop() {
        JPanel p = new JPanel(new GridLayout(1, 1, 10, 0));
        p.setOpaque(false);
        KhungBoGoc the = new KhungBoGoc(12, Color.WHITE);
        the.setLayout(new BorderLayout(0, 6));
        the.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel t = new JLabel("Khách hàng đang quản lý");
        t.setFont(HangSoGiaoDien.MEDIUM);
        t.setForeground(HangSoGiaoDien.MUTED);
        the.add(t, BorderLayout.NORTH);
        the.add(lblTongKhachHang, BorderLayout.CENTER);
        p.add(the);
        return p;
    }

    private static JLabel taoGiaTriTongHop(String vanBan) {
        JLabel l = new JLabel(vanBan);
        l.setFont(HangSoGiaoDien.phongChu(Font.BOLD, 24));
        l.setForeground(HangSoGiaoDien.TEXT);
        return l;
    }

    @Override
    public void lamMoiDuLieu() {
        try {
            String kw = "%" + txtTimKiem.getText().trim() + "%";
            List<KhachHang> cacDong = dichVu.timKiemKhachHang(kw, null);
            moHinh.setRowCount(0);
            for (KhachHang r : cacDong) {
                if (!"Hoạt động".equals(r.getTrangThai()))
                    continue;
                moHinh.addRow(
                        new Object[] { r.getMaKhachHang(), r.getHoTen(), DinhDangHienThi.anToan(r.getSoDienThoai()),
                                DinhDangHienThi.anToan(r.getEmail()), DinhDangHienThi.anToan(r.getDiaChi()) });
            }
            lblTongKhachHang.setText(String.valueOf(moHinh.getRowCount()));
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }

    private String maDangChon() {
        int vr = tblDanhSach.getSelectedRow();
        return vr < 0 ? null : String.valueOf(moHinh.getValueAt(tblDanhSach.convertRowIndexToModel(vr), 0));
    }

    private void suaDangChon() {
        String id = maDangChon();
        if (id == null) {
            thongBao("Hãy chọn khách hàng cần cập nhật.");
            return;
        }
        bieuMau(id);
    }

    private void xoaDangChon() {
        String id = maDangChon();
        if (id == null) {
            thongBao("Hãy chọn khách hàng cần xóa.");
            return;
        }
        if (!xacNhan("Xóa khách hàng " + id + "? Khách hàng đã có lịch sử giao dịch sẽ không được phép xóa."))
            return;
        try {
            dichVu.xoaKhachHang(id);
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }

    private void bieuMau(String id) {
        try {
            KhachHang cu = id == null ? null : dichVu.layKhachHang(id);
            JTextField ma = TaoGiaoDien.oNhap(180), ten = TaoGiaoDien.oNhap(260), sdt = TaoGiaoDien.oNhap(180),
                    email = TaoGiaoDien.oNhap(260);
            JTextArea diaChi = TaoGiaoDien.vungNhap(3, 34);
            if (cu == null)
                TaoGiaoDien.datGoiYMa(ma, dichVu.goiYMaKhachHang());
            else {
                TaoGiaoDien.datChiDoc(ma, cu.getMaKhachHang());
                ten.setText(cu.getHoTen());
                sdt.setText(DinhDangHienThi.anToan(cu.getSoDienThoai()));
                email.setText(DinhDangHienThi.anToan(cu.getEmail()));
                diaChi.setText(DinhDangHienThi.anToan(cu.getDiaChi()));
            }
            JPanel thongTin = TaoGiaoDien.bieuMauHaiCot(TaoGiaoDien.truongBieuMau("Mã khách hàng", ma),
                    TaoGiaoDien.truongBieuMau("Họ tên", ten), TaoGiaoDien.truongBieuMau("Số điện thoại", sdt),
                    TaoGiaoDien.truongBieuMau("Thư điện tử", email));
            JPanel p = new JPanel(new BorderLayout(0, 10));
            p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
            p.add(thongTin, BorderLayout.NORTH);
            p.add(TaoGiaoDien.truongBieuMau("Địa chỉ", TaoGiaoDien.cuonVungNhap(diaChi, 88)), BorderLayout.CENTER);
            if (!TienIchHopThoai.xacNhanBieuMau(this, p, cu == null ? "Thêm khách hàng" : "Cập nhật khách hàng",
                    cu == null ? "Thêm khách hàng" : "Lưu thay đổi"))
                return;
            boolean taoMoi = cu == null;
            KhachHang kh = new KhachHang(ma.getText(), ten.getText(), sdt.getText(), email.getText(), diaChi.getText(),
                    taoMoi ? null : cu.getMaHangThanhVien(), taoMoi ? 0 : cu.getDiemHienTai(), "Hoạt động");
            dichVu.luuKhachHang(kh, taoMoi);
            lamMoiDuLieu();
        } catch (Exception ex) {
            hienLoi(ex);
        }
    }
}

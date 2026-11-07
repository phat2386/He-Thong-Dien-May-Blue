package ui.chinh;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import entity.TaiKhoan;
import service.*;
import ui.banhang.*;
import ui.calamviec.ManHinhQuanLyCaLam;
import ui.dungchung.*;
import ui.giaohang.ManHinhQuanLyGiaoHang;
import ui.hoantien.ManHinhDuyetHoanTien;
import ui.hoso.ManHinhHoSoCaNhan;
import ui.khachhang.*;
import ui.khuyenmai.ManHinhQuanLyKhuyenMai;
import ui.nhacungcap.ManHinhQuanLyNhaCungCap;
import ui.nhanvien.ManHinhQuanLyNhanVien;
import ui.sanpham.ManHinhQuanLySanPham;
import ui.sauban.ManHinhSauBan;
import ui.taikhoan.ManHinhQuanLyTaiKhoan;
import ui.thongke.ManHinhThongKe;
import ui.tongquan.ManHinhTrangChu;
import ui.xacthuc.KhungDangNhap;
import java.util.List;
import javax.swing.Timer;

public class KhungChinh extends JFrame {
    private static final long serialVersionUID = 1L;
    public static final String MAN_HINH_TRANG_CHU = "DASHBOARD";
    public static final String MAN_HINH_NHAN_VIEN = "EMPLOYEES";
    public static final String MAN_HINH_BAN_HANG = "SALES";
    public static final String MAN_HINH_LAP_HOA_DON = "INVOICE_CREATE";
    public static final String MAN_HINH_LAP_GIAO_HANG = "DELIVERY_CREATE";
    public static final String MAN_HINH_LAP_DOI_TRA = "RETURN_CREATE";
    public static final String MAN_HINH_SAN_PHAM = "PRODUCTS";
    public static final String MAN_HINH_KHACH_HANG = "CUSTOMERS";
    public static final String MAN_HINH_THANH_VIEN = "MEMBERSHIP";
    public static final String MAN_HINH_KHUYEN_MAI = "PROMOTIONS";
    public static final String MAN_HINH_GIAO_HANG = "DELIVERY";
    public static final String MAN_HINH_THONG_KE = "STATISTICS";
    public static final String MAN_HINH_BAO_HANH = "WARRANTY_TRACK";
    public static final String MAN_HINH_KHIEU_NAI = "COMPLAINT_PROCESS";
    public static final String MAN_HINH_NHA_CUNG_CAP = "SUPPLIERS";
    public static final String MAN_HINH_TAI_KHOAN = "ACCOUNTS";
    public static final String MAN_HINH_CA_LAM = "SHIFTS";
    public static final String MAN_HINH_HOAN_TIEN = "REFUNDS";
    public static final String MAN_HINH_HO_SO = "PROFILE";
    private static final long THOI_GIAN_HET_PHIEN_MS = 15L * 60L * 1000L;
    private static final int CHIEU_RONG_KHUNG = 1180;
    private static final int CHIEU_CAO_KHUNG = 720;
    private final TaiKhoanService dichVuXacThuc;
    private final KhieuNaiService dichVuNen = new KhieuNaiService();
    private final TaiKhoanService dichVuDieuHuong = new TaiKhoanService();
    private final CardLayout boCucThe = new CardLayout();
    private final JPanel pnlNoiDung = new JPanel(boCucThe);
    private final ThanhTren thanhTren;
    private final Map<String, JPanel> cacManHinh = new HashMap<String, JPanel>();
    private ThanhBen thanhBen;
    private final List<MucDieuHuong> lichSuDieuHuong = new ArrayList<MucDieuHuong>();
    private int chiSoDieuHuong = -1;
    private long lanHoatDongCuoi = System.currentTimeMillis();
    private final AWTEventListener boLangNgheHoatDong;
    private final Timer boDemPhien;

    public KhungChinh(TaiKhoanService dichVuXacThuc) {
        this.dichVuXacThuc = dichVuXacThuc;
        setTitle("Điện Máy Blue");
        setSize(CHIEU_RONG_KHUNG, CHIEU_CAO_KHUNG);
        setMinimumSize(new Dimension(CHIEU_RONG_KHUNG, CHIEU_CAO_KHUNG));
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        thanhTren = new ThanhTren(this::quayLai, this::quayToi);
        thanhBen = new ThanhBen(this);
        add(thanhBen, BorderLayout.WEST);
        add(thanhTren, BorderLayout.NORTH);
        add(pnlNoiDung, BorderLayout.CENTER);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                yeuCauDangXuat();
            }
        });
        boLangNgheHoatDong = event -> {
            if (event instanceof MouseEvent || event instanceof KeyEvent)
                lanHoatDongCuoi = System.currentTimeMillis();
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(boLangNgheHoatDong,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        boDemPhien = new Timer(30_000, e -> kiemTraHetPhien());
        boDemPhien.start();
        moManHinhDau();
    }

    private void themManHinh(String maManHinh, JPanel khung) {
        cacManHinh.put(maManHinh, khung);
        pnlNoiDung.add(taoManHinhCuon(khung), maManHinh);
    }

    private JScrollPane taoManHinhCuon(JPanel khung) {
        KhungCuonManHinh host = new KhungCuonManHinh(khung);
        JScrollPane cuon = new JScrollPane(host, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        cuon.setBorder(null);
        cuon.getViewport().setBackground(HangSoGiaoDien.BACKGROUND);
        cuon.getVerticalScrollBar().setUnitIncrement(18);
        cuon.getVerticalScrollBar().setBlockIncrement(120);
        cuon.getVerticalScrollBar().setPreferredSize(new Dimension(13, Integer.MAX_VALUE));
        return cuon;
    }

    private static final class KhungCuonManHinh extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        KhungCuonManHinh(JPanel noiDungCuon) {
            super(new BorderLayout());
            setBackground(HangSoGiaoDien.BACKGROUND);
            add(noiDungCuon, BorderLayout.NORTH);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private JPanel taoManHinh(String maManHinh) {
        if (MAN_HINH_TRANG_CHU.equals(maManHinh))
            return new ManHinhTrangChu();
        if (MAN_HINH_NHAN_VIEN.equals(maManHinh))
            return new ManHinhQuanLyNhanVien();
        if (MAN_HINH_BAN_HANG.equals(maManHinh))
            return new ManHinhQuanLyBanHang(this);
        if (MAN_HINH_LAP_HOA_DON.equals(maManHinh))
            return new ManHinhLapHoaDon();
        if (MAN_HINH_LAP_GIAO_HANG.equals(maManHinh))
            return new ManHinhLapPhieuGiaoHang();
        if (MAN_HINH_LAP_DOI_TRA.equals(maManHinh))
            return new ManHinhLapPhieuDoiTra();
        if (MAN_HINH_SAN_PHAM.equals(maManHinh))
            return new ManHinhQuanLySanPham();
        if (MAN_HINH_KHACH_HANG.equals(maManHinh))
            return new ManHinhQuanLyKhachHang();
        if (MAN_HINH_THANH_VIEN.equals(maManHinh))
            return new ManHinhQuanLyThanhVien();
        if (MAN_HINH_KHUYEN_MAI.equals(maManHinh))
            return new ManHinhQuanLyKhuyenMai();
        if (MAN_HINH_GIAO_HANG.equals(maManHinh))
            return new ManHinhQuanLyGiaoHang();
        if (MAN_HINH_THONG_KE.equals(maManHinh))
            return new ManHinhThongKe();
        if (MAN_HINH_BAO_HANH.equals(maManHinh))
            return new ManHinhSauBan(ManHinhSauBan.PhanSauBan.BAO_HANH);
        if (MAN_HINH_KHIEU_NAI.equals(maManHinh))
            return new ManHinhSauBan(ManHinhSauBan.PhanSauBan.KHIEU_NAI);
        if (MAN_HINH_NHA_CUNG_CAP.equals(maManHinh))
            return new ManHinhQuanLyNhaCungCap();
        if (MAN_HINH_TAI_KHOAN.equals(maManHinh))
            return new ManHinhQuanLyTaiKhoan();
        if (MAN_HINH_CA_LAM.equals(maManHinh))
            return new ManHinhQuanLyCaLam();
        if (MAN_HINH_HOAN_TIEN.equals(maManHinh))
            return new ManHinhDuyetHoanTien();
        if (MAN_HINH_HO_SO.equals(maManHinh))
            return new ManHinhHoSoCaNhan(this, dichVuXacThuc);
        return null;
    }

    public void hienManHinh(String maManHinh, String tieuDe) {
        if (!hienThiManHinh(maManHinh, tieuDe))
            return;
        if (chiSoDieuHuong >= 0 && lichSuDieuHuong.get(chiSoDieuHuong).maManHinh.equals(maManHinh)) {
            lichSuDieuHuong.get(chiSoDieuHuong).tieuDe = tieuDe;
        } else {
            while (lichSuDieuHuong.size() > chiSoDieuHuong + 1) {
                lichSuDieuHuong.remove(lichSuDieuHuong.size() - 1);
            }
            lichSuDieuHuong.add(new MucDieuHuong(maManHinh, tieuDe));
            chiSoDieuHuong = lichSuDieuHuong.size() - 1;
        }
        capNhatNutDieuHuong();
    }

    private boolean hienThiManHinh(String maManHinh, String tieuDe) {
        if (!coTheTruyCap(maManHinh)) {
            JOptionPane.showMessageDialog(TienIchHopThoai.chuSoHuu(this),
                    "Tài khoản hiện tại không có quyền truy cập chức năng này.", "Không có quyền",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        JPanel khung = cacManHinh.get(maManHinh);
        if (khung == null) {
            try {
                khung = taoManHinh(maManHinh);
                if (khung == null)
                    return false;
                themManHinh(maManHinh, khung);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(TienIchHopThoai.chuSoHuu(this),
                        "Không thể mở chức năng này.\n" + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (khung instanceof CoTheLamMoi)
            ((CoTheLamMoi) khung).lamMoiDuLieu();
        boCucThe.show(pnlNoiDung, maManHinh);
        thanhTren.setTieuDeManHinh(tieuDe);
        if (thanhBen != null)
            thanhBen.setDangChon(maManHinh);
        return true;
    }

    private void quayLai() {
        if (chiSoDieuHuong <= 0)
            return;
        int targetIndex = chiSoDieuHuong - 1;
        MucDieuHuong target = lichSuDieuHuong.get(targetIndex);
        if (hienThiManHinh(target.maManHinh, target.tieuDe)) {
            chiSoDieuHuong = targetIndex;
            capNhatNutDieuHuong();
        }
    }

    private void quayToi() {
        if (chiSoDieuHuong < 0 || chiSoDieuHuong >= lichSuDieuHuong.size() - 1)
            return;
        int targetIndex = chiSoDieuHuong + 1;
        MucDieuHuong target = lichSuDieuHuong.get(targetIndex);
        if (hienThiManHinh(target.maManHinh, target.tieuDe)) {
            chiSoDieuHuong = targetIndex;
            capNhatNutDieuHuong();
        }
    }

    private void capNhatNutDieuHuong() {
        thanhTren.setDieuHuongChoPhep(chiSoDieuHuong > 0,
                chiSoDieuHuong >= 0 && chiSoDieuHuong < lichSuDieuHuong.size() - 1);
    }

    private static final class MucDieuHuong {
        final String maManHinh;
        String tieuDe;

        MucDieuHuong(String maManHinh, String tieuDe) {
            this.maManHinh = maManHinh;
            this.tieuDe = tieuDe;
        }
    }

    public boolean coTheTruyCap(String maManHinh) {
        if (!TaiKhoanService.daDangNhap() || TaiKhoanService.getTaiKhoanHienTai().getVaiTro() == null)
            return false;
        if (MAN_HINH_TRANG_CHU.equals(maManHinh))
            return TaiKhoanService.coQuyen("DASHBOARD_VIEW");
        if (MAN_HINH_NHAN_VIEN.equals(maManHinh))
            return TaiKhoanService.coQuyen("EMPLOYEE_MANAGE");
        if (MAN_HINH_TAI_KHOAN.equals(maManHinh))
            return TaiKhoanService.coQuyen("ACCOUNT_MANAGE");
        if (MAN_HINH_CA_LAM.equals(maManHinh))
            return TaiKhoanService.coQuyen("SHIFT_MANAGE");
        if (MAN_HINH_SAN_PHAM.equals(maManHinh))
            return TaiKhoanService.coQuyen("PRODUCT_MANAGE");
        if (MAN_HINH_NHA_CUNG_CAP.equals(maManHinh))
            return TaiKhoanService.coQuyen("SUPPLIER_MANAGE");
        if (MAN_HINH_KHUYEN_MAI.equals(maManHinh))
            return TaiKhoanService.coQuyen("PROMOTION_MANAGE");
        if (MAN_HINH_HOAN_TIEN.equals(maManHinh))
            return TaiKhoanService.coQuyen("REFUND_APPROVE");
        if (MAN_HINH_THONG_KE.equals(maManHinh))
            return TaiKhoanService.coQuyen("STATISTICS_VIEW");
        if (MAN_HINH_BAN_HANG.equals(maManHinh))
            return TaiKhoanService.coQuyen("SALES_MANAGE");
        if (MAN_HINH_LAP_HOA_DON.equals(maManHinh))
            return TaiKhoanService.coQuyen("INVOICE_CREATE");
        if (MAN_HINH_LAP_GIAO_HANG.equals(maManHinh))
            return TaiKhoanService.coQuyen("DELIVERY_CREATE");
        if (MAN_HINH_LAP_DOI_TRA.equals(maManHinh))
            return TaiKhoanService.coQuyen("RETURN_CREATE");
        if (MAN_HINH_KHACH_HANG.equals(maManHinh))
            return TaiKhoanService.coQuyen("CUSTOMER_MANAGE");
        if (MAN_HINH_THANH_VIEN.equals(maManHinh))
            return TaiKhoanService.coQuyen("MEMBERSHIP_MANAGE");
        if (MAN_HINH_GIAO_HANG.equals(maManHinh))
            return TaiKhoanService.coQuyen("DELIVERY_MANAGE");
        if (MAN_HINH_BAO_HANH.equals(maManHinh))
            return TaiKhoanService.coQuyen("WARRANTY_MANAGE");
        if (MAN_HINH_KHIEU_NAI.equals(maManHinh))
            return TaiKhoanService.coQuyen("COMPLAINT_MANAGE");
        if (MAN_HINH_HO_SO.equals(maManHinh))
            return TaiKhoanService.coQuyen("PROFILE_VIEW");
        return false;
    }

    private void moManHinhDau() {
        if (!TaiKhoanService.daDangNhap() || TaiKhoanService.getTaiKhoanHienTai().getVaiTro() == null)
            return;
        String[][] cacUngVien = {
                {
                        MAN_HINH_TRANG_CHU, "Trang chủ"
                },
                {
                        MAN_HINH_NHAN_VIEN, "Quản lý nhân viên"
                },
                {
                        MAN_HINH_BAN_HANG, "Quản lý hóa đơn"
                },
                {
                        MAN_HINH_LAP_HOA_DON, "Lập hóa đơn bán hàng"
                },
                {
                        MAN_HINH_KHACH_HANG, "Quản lý khách hàng"
                },
                {
                        MAN_HINH_GIAO_HANG, "Quản lý phiếu giao hàng"
                },
                {
                        MAN_HINH_BAO_HANH, "Quản lý bảo hành"
                },
                {
                        MAN_HINH_KHIEU_NAI, "Quản lý khiếu nại"
                },
                {
                        MAN_HINH_SAN_PHAM, "Quản lý sản phẩm"
                },
                {
                        MAN_HINH_NHA_CUNG_CAP, "Quản lý nhà cung cấp"
                },
                {
                        MAN_HINH_KHUYEN_MAI, "Quản lý khuyến mãi"
                },
                {
                        MAN_HINH_THONG_KE, "Thống kê kinh doanh"
                },
                {
                        MAN_HINH_TAI_KHOAN, "Quản lý tài khoản"
                },
                {
                        MAN_HINH_CA_LAM, "Quản lý ca làm việc"
                },
                {
                        MAN_HINH_HOAN_TIEN, "Duyệt hoàn tiền"
                },
                {
                        MAN_HINH_HO_SO, "Hồ sơ cá nhân"
                }
        };
        for (String[] muc : cacUngVien) {
            if (coTheTruyCap(muc[0])) {
                hienManHinh(muc[0], muc[1]);
                return;
            }
        }
        JOptionPane.showMessageDialog(TienIchHopThoai.chuSoHuu(this),
                "Tài khoản hiện chưa được Admin cấp chức năng nghiệp vụ nào.", "Chưa có quyền",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void yeuCauDangXuat() {
        if (!TienIchHopThoai.xacNhanHanhDong(this, "Bạn có chắc muốn đăng xuất?", "Xác nhận đăng xuất", "Đăng xuất"))
            return;
        dangXuatVaVeDangNhap(null);
    }

    private void kiemTraHetPhien() {
        if (System.currentTimeMillis() - lanHoatDongCuoi >= THOI_GIAN_HET_PHIEN_MS) {
            dangXuatVaVeDangNhap("Phiên đăng nhập đã hết hạn do không hoạt động trong 15 phút.");
            return;
        }
        try {
            if (!TaiKhoanService.daDangNhap())
                return;
            String maTaiKhoan = TaiKhoanService.getTaiKhoanHienTai().getMaTaiKhoan();
            TaiKhoan dong = dichVuDieuHuong.trangThaiPhien(maTaiKhoan);
            if (dong == null || !"Hoạt động".equals(dong.getTrangThai()) || dong.getNhanVien() == null
                    || !"Hoạt động".equals(dong.getNhanVien().getTrangThai())) {
                dangXuatVaVeDangNhap("Tài khoản hoặc hồ sơ nhân viên đã bị khóa/ngừng hoạt động.");
                return;
            }
            int maVaiTroCSDL = dong.getVaiTro() == null ? 0 : dong.getVaiTro().getMaVaiTro();
            if (maVaiTroCSDL != TaiKhoanService.getTaiKhoanHienTai().getVaiTro().getMaVaiTro()) {
                dangXuatVaVeDangNhap("Vai trò tài khoản đã được thay đổi. Vui lòng đăng nhập lại.");
                return;
            }
            Set<String> dbPermissions = dichVuDieuHuong.quyenTheoVaiTro(maVaiTroCSDL);
            if (!dbPermissions.equals(TaiKhoanService.getDanhSachQuyen())) {
                dangXuatVaVeDangNhap(
                        "Quyền của vai trò đã được cập nhật. Vui lòng đăng nhập lại để áp dụng quyền mới.");
                return;
            }
            dichVuNen.capNhatLeoThangKhieuNai();
        } catch (Exception ignored) {
        }
    }

    private void dangXuatVaVeDangNhap(String thongBao) {
        boDemPhien.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(boLangNgheHoatDong);
        dichVuXacThuc.dangXuat();
        super.dispose();
        KhungDangNhap dangNhap = new KhungDangNhap();
        if (thongBao != null)
            dangNhap.hienThongBaoPhien(thongBao);
        dangNhap.setVisible(true);
    }

    @Override
    public void dispose() {
        if (boDemPhien != null)
            boDemPhien.stop();
        if (boLangNgheHoatDong != null)
            Toolkit.getDefaultToolkit().removeAWTEventListener(boLangNgheHoatDong);
        super.dispose();
    }
}

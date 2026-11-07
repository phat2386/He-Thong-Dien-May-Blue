package ui.hoso;
import ui.dungchung.DinhDangHienThi;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import entity.*;
import service.*;
import ui.dungchung.*;
import ui.xacthuc.HopThoaiDoiMatKhau;
public class ManHinhHoSoCaNhan extends JPanel implements CoTheLamMoi {
    private static final long serialVersionUID = 1L;
    private final TaiKhoanService dichVuXacThuc;
    private final java.awt.Frame chuSoHuu;
    public ManHinhHoSoCaNhan(java.awt.Frame chuSoHuu, TaiKhoanService dichVuXacThuc) {
        this.chuSoHuu = chuSoHuu;
        this.dichVuXacThuc = dichVuXacThuc;
        setLayout(new BorderLayout(0, 22));
        setBackground(HangSoGiaoDien.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
    }
    @Override
    public void lamMoiDuLieu() {
        removeAll();
        taoGiaoDien();
        revalidate();
        repaint();
    }
    private void taoGiaoDien() {
        JLabel tieuDe = new JLabel("Hồ sơ cá nhân");
        tieuDe.setFont(HangSoGiaoDien.TITLE);
        tieuDe.setForeground(HangSoGiaoDien.TEXT);
        add(tieuDe, BorderLayout.NORTH);
        TaiKhoan taiKhoan = null;
        String loadError = null;
        try {
            taiKhoan = dichVuXacThuc.layTaiKhoanHienTaiTuCSDL();
        } catch (SQLException ex) {
            ex.printStackTrace();
            loadError = "Không thể tải thông tin cá nhân từ cơ sở dữ liệu.";
        }
        NhanVien nhanVien = taiKhoan == null ? null : taiKhoan.getNhanVien();
        JPanel the = new JPanel(new GridLayout(0, 2, 16, 16));
        the.setBackground(Color.WHITE);
        the.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(HangSoGiaoDien.BORDER), BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        if (loadError != null) {
            JLabel error = new JLabel(loadError);
            error.setForeground(new Color(220, 38, 38));
            the.add(error);
            the.add(new JLabel(""));
        } else if (taiKhoan == null || nhanVien == null) {
            JLabel error = new JLabel("Phiên đăng nhập không còn hiệu lực.");
            error.setForeground(new Color(220, 38, 38));
            the.add(error);
            the.add(new JLabel(""));
        } else {
            themDong(the, "Họ và tên", nhanVien.getHoTen());
            themDong(the, "Mã nhân viên", nhanVien.getMaNhanVien());
            themDong(the, "Tên đăng nhập", taiKhoan.getTenDangNhap());
            themDong(the, "Vai trò", taiKhoan.getVaiTro() == null ? "" : taiKhoan.getVaiTro().getTenVaiTro());
            themDong(the, "Số điện thoại", nhanVien.getSoDienThoai());
            themDong(the, "Thư điện tử", nhanVien.getEmail() == null ? "" : nhanVien.getEmail());
            themDong(the, "Phòng ban", nhanVien.getPhongBan() == null ? "" : nhanVien.getPhongBan());
            themDong(the, "Ngày vào làm", nhanVien.getNgayVaoLam() == null ? "" : nhanVien.getNgayVaoLam().format( DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            themDong(the, "Trạng thái tài khoản", DinhDangHienThi.tiengViet(taiKhoan.getTrangThai()));
            themDong(the, "Trạng thái nhân viên", DinhDangHienThi.tiengViet(nhanVien.getTrangThai()));
        }
        add(the, BorderLayout.CENTER);
        JButton nutDoiMatKhau = new JButton("Đổi mật khẩu");
        nutDoiMatKhau.setBackground(HangSoGiaoDien.ORANGE);
        nutDoiMatKhau.setForeground(Color.WHITE);
        nutDoiMatKhau.setFocusPainted(false);
        nutDoiMatKhau.setEnabled(taiKhoan != null && nhanVien != null && loadError == null);
        nutDoiMatKhau.addActionListener(e -> {
            HopThoaiDoiMatKhau hopThoai = new HopThoaiDoiMatKhau(chuSoHuu, dichVuXacThuc, false, null);
            hopThoai.setVisible(true);
        });
        JPanel thaoTac = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        thaoTac.setOpaque(false);
        thaoTac.add(nutDoiMatKhau);
        add(thaoTac, BorderLayout.SOUTH);
    }
    private void themDong(JPanel khung, String nhan, String giaTri) {
        JLabel l = new JLabel(nhan);
        l.setFont(HangSoGiaoDien.NORMAL);
        l.setForeground(HangSoGiaoDien.MUTED);
        JLabel v = new JLabel(giaTri == null ? "" : giaTri);
        v.setFont(HangSoGiaoDien.NORMAL);
        v.setForeground(HangSoGiaoDien.TEXT);
        khung.add(l);
        khung.add(v);
    }
}

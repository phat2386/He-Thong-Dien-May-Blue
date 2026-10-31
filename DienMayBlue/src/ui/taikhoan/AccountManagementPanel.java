package ui.taikhoan;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import service.AuthService;
import ui.dungchung.BaseScreenPanel;
import ui.dungchung.DialogUtils;
import ui.dungchung.UIFactory;
import util.Formats;
import util.SessionManager;

/** Màn hình quản lý tài khoản: cấp, sửa, khóa / kích hoạt, vô hiệu hóa, xóa. */
public class AccountManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;

    private final JTextField txtTim = UIFactory.field(300);
    private final JComboBox<String> cboVaiTro = UIFactory.combo(
            "Tất cả vai trò", "Quản lý", "Bán hàng", "Chăm sóc khách hàng");
    private final JComboBox<String> cboTrangThai = UIFactory.combo(
            "Tất cả trạng thái", "Đang hoạt động", "Đã khóa", "Ngừng hoạt động", "Chờ xử lý");

    private final DefaultTableModel model = UIFactory.readOnlyModel(new Object[] {
            "Mã", "Mã NV", "Họ tên", "Tên đăng nhập",
            "Vai trò", "SĐT", "Trạng thái", "Đăng nhập gần nhất" });
    private final JTable table = UIFactory.table(model);

    private final AuthService service = new AuthService();

    public AccountManagementPanel() {
        super("Quản lý tài khoản",
                "Cấp, cập nhật, khóa hoặc vô hiệu hóa tài khoản nhân viên. "
                + "Phân quyền chức năng do Admin thực hiện riêng.");

        JButton btnThem = UIFactory.primaryButton("+ Thêm tài khoản");
        JButton btnSua = UIFactory.secondaryButton("Sửa tài khoản");
        JButton btnKhoa = UIFactory.secondaryButton("Khóa / Kích hoạt");
        JButton btnVoHieu = UIFactory.secondaryButton("Vô hiệu hóa");
        JButton btnXoa = UIFactory.secondaryButton("Xóa tài khoản");

        actions.add(btnThem);
        actions.add(btnSua);
        actions.add(btnKhoa);
        actions.add(btnVoHieu);
        actions.add(btnXoa);

        body.add(UIFactory.toolbar(new JLabel("Tìm"), txtTim,
                new JLabel("Vai trò"), cboVaiTro,
                new JLabel("Trạng thái"), cboTrangThai), BorderLayout.NORTH);
        body.add(UIFactory.scroll(table), BorderLayout.CENTER);

        txtTim.addActionListener(e -> refreshData());
        cboVaiTro.addActionListener(e -> refreshData());
        cboTrangThai.addActionListener(e -> refreshData());

        btnThem.addActionListener(e -> themTaiKhoan());
        btnSua.addActionListener(e -> suaTaiKhoan());
        btnKhoa.addActionListener(e -> khoaHoacKichHoat());
        btnVoHieu.addActionListener(e -> voHieuHoa());
        btnXoa.addActionListener(e -> xoaTaiKhoan());

        refreshData();
    }

    /** Tải lại bảng theo từ khóa, vai trò và trạng thái đang chọn. */
    @Override
    public void refreshData() {
        try {
            String tuKhoa = "%" + txtTim.getText().trim() + "%";

            // "Tất cả ..." thì không lọc (null), còn lại đổi chữ tiếng Việt thành mã
            String maVaiTro = null;
            if (!"Tất cả vai trò".equals(cboVaiTro.getSelectedItem())) {
                maVaiTro = Formats.code(cboVaiTro.getSelectedItem());
            }

            String maTrangThai = null;
            if (!"Tất cả trạng thái".equals(cboTrangThai.getSelectedItem())) {
                maTrangThai = Formats.code(cboTrangThai.getSelectedItem());
            }

            List<Map<String, Object>> danhSach = service.searchAccounts(tuKhoa, maVaiTro, maTrangThai);

            model.setRowCount(0);
            for (Map<String, Object> dong : danhSach) {
                model.addRow(new Object[] {
                        dong.get("account_id"),
                        dong.get("employee_id"),
                        dong.get("full_name"),
                        dong.get("username"),
                        Formats.vi(dong.get("role_code")),
                        dong.get("phone"),
                        Formats.vi(dong.get("status")),
                        Formats.date(dong.get("last_login_at")) });
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Mã tài khoản của dòng đang chọn, chưa chọn thì trả về null. */
    private Integer layMaTaiKhoanDangChon() {
        int dongChon = table.getSelectedRow();
        if (dongChon < 0) {
            return null;
        }
        int dongThat = table.convertRowIndexToModel(dongChon);
        return ((Number) model.getValueAt(dongThat, 0)).intValue();
    }

    /** Cấp tài khoản cho một nhân viên chưa có tài khoản. */
    private void themTaiKhoan() {
        try {
            JComboBox<String> cboNhanVien = new JComboBox<String>();
            for (Map<String, Object> nv : service.availableEmployees()) {
                cboNhanVien.addItem(nv.get("employee_id") + " - "
                        + nv.get("full_name") + " - " + nv.get("phone"));
            }

            if (cboNhanVien.getItemCount() == 0) {
                info("Không còn hồ sơ nhân viên đang hoạt động nào chưa có tài khoản. "
                        + "Hãy tạo hồ sơ nhân viên trước.");
                return;
            }

            JTextField txtTenDangNhap = UIFactory.field(180);
            JPasswordField txtMatKhau = new JPasswordField();
            txtMatKhau.setPreferredSize(new Dimension(200, 36));
            JComboBox<String> cboChonVaiTro = taoOChonVaiTro();

            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Nhân viên"));
            form.add(cboNhanVien);
            form.add(new JLabel("Tên đăng nhập"));
            form.add(txtTenDangNhap);
            form.add(new JLabel("Mật khẩu ban đầu"));
            form.add(txtMatKhau);
            form.add(new JLabel("Vai trò"));
            form.add(cboChonVaiTro);

            int chon = JOptionPane.showConfirmDialog(DialogUtils.owner(this), form,
                    "Thêm tài khoản", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (chon != JOptionPane.OK_OPTION) {
                return;
            }

            // Dòng nhân viên có dạng "mã - tên - sđt", lấy phần mã đứng trước chữ " - "
            String dongNhanVien = String.valueOf(cboNhanVien.getSelectedItem());
            String maNhanVien = dongNhanVien.substring(0, dongNhanVien.indexOf(" - "));
            int maVaiTro = layMaTuDong(cboChonVaiTro.getSelectedItem());

            service.createAccountForEmployee(maNhanVien, txtTenDangNhap.getText().trim(),
                    new String(txtMatKhau.getPassword()), maVaiTro);

            info("Đã cấp tài khoản. Người dùng phải đổi mật khẩu ở lần đăng nhập đầu tiên.");
            refreshData();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Sửa tên đăng nhập và vai trò của tài khoản đang chọn. */
    private void suaTaiKhoan() {
        Integer maTaiKhoan = layMaTaiKhoanDangChon();
        if (maTaiKhoan == null) {
            info("Hãy chọn tài khoản.");
            return;
        }

        try {
            Map<String, Object> thongTin = service.accountInfo(maTaiKhoan);

            JTextField txtTenDangNhap = UIFactory.field(180);
            txtTenDangNhap.setText(Formats.safe(thongTin.get("username")));

            // Chọn sẵn vai trò hiện tại trong ô chọn
            JComboBox<String> cboChonVaiTro = taoOChonVaiTro();
            String dauDong = String.valueOf(thongTin.get("role_id")) + " -";
            for (int i = 0; i < cboChonVaiTro.getItemCount(); i++) {
                if (String.valueOf(cboChonVaiTro.getItemAt(i)).startsWith(dauDong)) {
                    cboChonVaiTro.setSelectedIndex(i);
                }
            }

            // Không cho tự đổi vai trò của chính mình
            if (maTaiKhoan == SessionManager.getCurrentAccount().getAccountId()) {
                cboChonVaiTro.setEnabled(false);
            }

            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Nhân viên"));
            form.add(new JLabel(Formats.safe(thongTin.get("full_name"))
                    + " (" + thongTin.get("employee_id") + ")"));
            form.add(new JLabel("Tên đăng nhập"));
            form.add(txtTenDangNhap);
            form.add(new JLabel("Vai trò"));
            form.add(cboChonVaiTro);

            int chon = JOptionPane.showConfirmDialog(DialogUtils.owner(this), form,
                    "Sửa tài khoản", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (chon != JOptionPane.OK_OPTION) {
                return;
            }

            service.updateAccount(maTaiKhoan, txtTenDangNhap.getText().trim(),
                    layMaTuDong(cboChonVaiTro.getSelectedItem()));
            refreshData();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Đang hoạt động thì khóa, ngược lại thì kích hoạt. */
    private void khoaHoacKichHoat() {
        Integer maTaiKhoan = layMaTaiKhoanDangChon();
        if (maTaiKhoan == null) {
            info("Hãy chọn tài khoản.");
            return;
        }

        try {
            String trangThaiHienTai = service.accountStatus(maTaiKhoan);
            String trangThaiMoi = "ACTIVE".equals(trangThaiHienTai) ? "LOCKED" : "ACTIVE";
            String tenHanhDong = "ACTIVE".equals(trangThaiMoi) ? "Kích hoạt" : "Khóa";

            // Khóa tài khoản thì phải hỏi trước nếu người đó đang đăng nhập
            if (!"ACTIVE".equals(trangThaiMoi) && !canhBaoDangDangNhap(maTaiKhoan)) {
                return;
            }
            if (!confirm(tenHanhDong + " tài khoản này?")) {
                return;
            }

            service.changeAccountStatus(maTaiKhoan, trangThaiMoi);
            refreshData();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Vô hiệu hóa: không đăng nhập được nhưng vẫn giữ lịch sử. */
    private void voHieuHoa() {
        Integer maTaiKhoan = layMaTaiKhoanDangChon();
        if (maTaiKhoan == null) {
            info("Hãy chọn tài khoản.");
            return;
        }
        if (!canhBaoDangDangNhap(maTaiKhoan)) {
            return;
        }
        if (!confirm("Vô hiệu hóa tài khoản? Tài khoản sẽ không thể đăng nhập "
                + "nhưng dữ liệu lịch sử vẫn được giữ lại.")) {
            return;
        }

        try {
            service.changeAccountStatus(maTaiKhoan, "DISABLED");
            refreshData();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Xóa tài khoản; nếu đã có lịch sử thì hệ thống tự chuyển sang vô hiệu hóa. */
    private void xoaTaiKhoan() {
        Integer maTaiKhoan = layMaTaiKhoanDangChon();
        if (maTaiKhoan == null) {
            info("Hãy chọn tài khoản.");
            return;
        }
        if (!canhBaoDangDangNhap(maTaiKhoan)) {
            return;
        }
        if (!confirm("Xóa tài khoản đã chọn? Nếu tài khoản đã có lịch sử, "
                + "hệ thống sẽ tự chuyển sang Vô hiệu hóa để bảo toàn dữ liệu.")) {
            return;
        }

        try {
            boolean daXoa = service.deleteAccountOrDisable(maTaiKhoan);

            if (daXoa) {
                info("Đã xóa tài khoản chưa phát sinh lịch sử. Hồ sơ nhân viên vẫn được giữ nguyên.");
            } else {
                info("Tài khoản đã có lịch sử nên được Vô hiệu hóa thay vì xóa vật lý.");
            }
            refreshData();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /**
     * Nếu tài khoản đang có người đăng nhập thì hỏi lại.
     * Trả về true nếu được phép tiếp tục.
     */
    private boolean canhBaoDangDangNhap(int maTaiKhoan) {
        try {
            if (service.hasActiveSession(maTaiKhoan)) {
                return confirm("Tài khoản đang có phiên đăng nhập. Nếu tiếp tục, phiên đó sẽ bị kết thúc "
                        + "và ứng dụng sẽ tự đưa người dùng về màn hình đăng nhập.");
            }
            return true;
        } catch (Exception ex) {
            showError(ex);
            return false;
        }
    }

    /** Ô chọn vai trò; mỗi dòng có dạng "mã - tên [mã vai trò]". */
    private JComboBox<String> taoOChonVaiTro() throws Exception {
        JComboBox<String> cbo = new JComboBox<String>();

        for (Map<String, Object> vaiTro : service.manageableRoles()) {
            cbo.addItem(vaiTro.get("role_id") + " - " + vaiTro.get("role_name")
                    + " [" + vaiTro.get("role_code") + "]");
        }
        return cbo;
    }

    /** Lấy số đứng trước chữ " - " của dòng đã chọn trong ô chọn. */
    private int layMaTuDong(Object dongDaChon) {
        String dong = String.valueOf(dongDaChon);
        return Integer.parseInt(dong.substring(0, dong.indexOf(" - ")));
    }
}

package app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import bus.Formats;
import bus.PhienDangNhap;
import bus.TaiKhoan_BUS;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.VaiTro;

/** UC005 - Quản lý tài khoản nhân viên. Phân quyền chức năng do Admin thực hiện riêng. */
public class PanelTaiKhoan extends PanelBaseScreen {
	private static final long serialVersionUID = 1L;

	private final JTextField txtTim = UIFactory.field(300);
	private final JComboBox<String> cboVaiTro = UIFactory.combo("Tất cả vai trò", "Quản lý", "Bán hàng",
			"Chăm sóc khách hàng");
	private final JComboBox<String> cboTrangThai = UIFactory.combo("Tất cả trạng thái", "Đang hoạt động", "Đã khóa",
			"Ngừng hoạt động", "Chờ xử lý");
	private final DefaultTableModel model = new DefaultTableModel(new Object[] { "Mã", "Mã NV", "Họ tên",
			"Tên đăng nhập", "Vai trò", "SĐT", "Trạng thái", "Đăng nhập gần nhất" }, 0) {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int r, int c) {
			return false;
		}
	};
	private final JTable table = UIFactory.table(model);
	private final TaiKhoan_BUS taiKhoanBUS = new TaiKhoan_BUS();

	public PanelTaiKhoan() {
		super("Quản lý tài khoản",
				"Cấp, cập nhật, khóa hoặc vô hiệu hóa tài khoản nhân viên. Phân quyền chức năng do Admin thực hiện riêng.");

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

		body.add(UIFactory.toolbar(new JLabel("Tìm"), txtTim, new JLabel("Vai trò"), cboVaiTro,
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

	/** Tải lại bảng theo từ khóa, vai trò, trạng thái đang chọn. */
	@Override
	public void refreshData() {
		try {
			String vaiTro = String.valueOf(cboVaiTro.getSelectedItem());
			String trangThai = String.valueOf(cboTrangThai.getSelectedItem());
			String codeVaiTro = "Tất cả vai trò".equals(vaiTro) ? null : UiText.code(vaiTro);
			String codeTrangThai = "Tất cả trạng thái".equals(trangThai) ? null : UiText.code(trangThai);

			List<TaiKhoan> ds = taiKhoanBUS.timKiem(txtTim.getText(), codeVaiTro, codeTrangThai);
			model.setRowCount(0);
			for (TaiKhoan tk : ds) {
				model.addRow(new Object[] { tk.getMaTaiKhoan(), tk.getNhanVien().getMaNhanVien(),
						tk.getNhanVien().getHoTen(), tk.getTenDangNhap(), UiText.vi(tk.getVaiTro().getCodeVaiTro()),
						tk.getNhanVien().getSoDienThoai(), UiText.vi(tk.getTrangThai()),
						Formats.date(tk.getDangNhapCuoi()) });
			}
		} catch (Exception ex) {
			showError(ex);
		}
	}

	/** Mã tài khoản của dòng đang chọn (null nếu chưa chọn). */
	private Integer maTaiKhoanDangChon() {
		int dong = table.getSelectedRow();
		return dong < 0 ? null : ((Number) model.getValueAt(table.convertRowIndexToModel(dong), 0)).intValue();
	}

	private void themTaiKhoan() {
		try {
			List<NhanVien> dsNhanVien = taiKhoanBUS.layNhanVienChuaCoTaiKhoan();
			if (dsNhanVien.isEmpty()) {
				info("Không còn hồ sơ nhân viên đang hoạt động nào chưa có tài khoản. Hãy tạo hồ sơ nhân viên trước.");
				return;
			}
			JComboBox<String> cboNhanVien = new JComboBox<String>();
			for (NhanVien nv : dsNhanVien) {
				cboNhanVien.addItem(nv.getMaNhanVien() + " - " + nv.getHoTen() + " - " + nv.getSoDienThoai());
			}
			JTextField txtTen = UIFactory.field(180);
			JPasswordField txtMatKhau = new JPasswordField();
			txtMatKhau.setPreferredSize(new Dimension(200, 36));
			List<VaiTro> dsVaiTro = taiKhoanBUS.layVaiTroGanDuoc();
			JComboBox<String> cboVaiTroForm = taoComboVaiTro(dsVaiTro);

			JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
			form.add(new JLabel("Nhân viên"));
			form.add(cboNhanVien);
			form.add(new JLabel("Tên đăng nhập"));
			form.add(txtTen);
			form.add(new JLabel("Mật khẩu ban đầu"));
			form.add(txtMatKhau);
			form.add(new JLabel("Vai trò"));
			form.add(cboVaiTroForm);
			if (JOptionPane.showConfirmDialog(DialogUtils.owner(this), form, "Thêm tài khoản",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
				return;
			}

			String maNhanVien = dsNhanVien.get(cboNhanVien.getSelectedIndex()).getMaNhanVien();
			int maVaiTro = dsVaiTro.get(cboVaiTroForm.getSelectedIndex()).getMaVaiTro();
			taiKhoanBUS.capTaiKhoan(maNhanVien, txtTen.getText().trim(), new String(txtMatKhau.getPassword()),
					maVaiTro);
			info("Đã cấp tài khoản. Người dùng phải đổi mật khẩu ở lần đăng nhập đầu tiên.");
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}

	private void suaTaiKhoan() {
		Integer ma = maTaiKhoanDangChon();
		if (ma == null) {
			info("Hãy chọn tài khoản.");
			return;
		}
		try {
			TaiKhoan tk = taiKhoanBUS.timTheoMa(ma);
			if (tk == null) {
				info("Không tìm thấy tài khoản.");
				return;
			}
			JTextField txtTen = UIFactory.field(180);
			txtTen.setText(Formats.safe(tk.getTenDangNhap()));

			List<VaiTro> dsVaiTro = taiKhoanBUS.layVaiTroGanDuoc();
			JComboBox<String> cboVaiTroForm = taoComboVaiTro(dsVaiTro);
			for (int i = 0; i < dsVaiTro.size(); i++) {
				if (dsVaiTro.get(i).getMaVaiTro() == tk.getVaiTro().getMaVaiTro()) {
					cboVaiTroForm.setSelectedIndex(i);
				}
			}
			// Không cho tự đổi vai trò của chính mình
			if (ma == PhienDangNhap.getTaiKhoanHienTai().getMaTaiKhoan()) {
				cboVaiTroForm.setEnabled(false);
			}

			JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
			form.add(new JLabel("Nhân viên"));
			form.add(new JLabel(
					Formats.safe(tk.getNhanVien().getHoTen()) + " (" + tk.getNhanVien().getMaNhanVien() + ")"));
			form.add(new JLabel("Tên đăng nhập"));
			form.add(txtTen);
			form.add(new JLabel("Vai trò"));
			form.add(cboVaiTroForm);
			if (JOptionPane.showConfirmDialog(DialogUtils.owner(this), form, "Sửa tài khoản",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
				return;
			}

			int maVaiTro = dsVaiTro.get(cboVaiTroForm.getSelectedIndex()).getMaVaiTro();
			taiKhoanBUS.capNhatTaiKhoan(ma, txtTen.getText().trim(), maVaiTro);
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}

	private void khoaHoacKichHoat() {
		Integer ma = maTaiKhoanDangChon();
		if (ma == null) {
			info("Hãy chọn tài khoản.");
			return;
		}
		try {
			TaiKhoan tk = taiKhoanBUS.timTheoMa(ma);
			if (tk == null) {
				info("Không tìm thấy tài khoản.");
				return;
			}
			String trangThaiMoi = "ACTIVE".equals(tk.getTrangThai()) ? "LOCKED" : "ACTIVE";
			String hanhDong = "ACTIVE".equals(trangThaiMoi) ? "Kích hoạt" : "Khóa";
			if (!"ACTIVE".equals(trangThaiMoi) && !canhBaoDangDangNhap(ma)) {
				return;
			}
			if (!confirm(hanhDong + " tài khoản này?")) {
				return;
			}
			taiKhoanBUS.doiTrangThai(ma, trangThaiMoi);
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}

	private void voHieuHoa() {
		Integer ma = maTaiKhoanDangChon();
		if (ma == null) {
			info("Hãy chọn tài khoản.");
			return;
		}
		if (!canhBaoDangDangNhap(ma)) {
			return;
		}
		if (!confirm("Vô hiệu hóa tài khoản? Tài khoản sẽ không thể đăng nhập nhưng dữ liệu lịch sử vẫn được giữ lại.")) {
			return;
		}
		try {
			taiKhoanBUS.doiTrangThai(ma, "DISABLED");
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}

	private void xoaTaiKhoan() {
		Integer ma = maTaiKhoanDangChon();
		if (ma == null) {
			info("Hãy chọn tài khoản.");
			return;
		}
		if (!canhBaoDangDangNhap(ma)) {
			return;
		}
		if (!confirm(
				"Xóa tài khoản đã chọn? Nếu tài khoản đã có lịch sử, hệ thống sẽ tự chuyển sang Vô hiệu hóa để bảo toàn dữ liệu.")) {
			return;
		}
		try {
			boolean daXoa = taiKhoanBUS.xoaHoacVoHieuHoa(ma);
			info(daXoa ? "Đã xóa tài khoản chưa phát sinh lịch sử. Hồ sơ nhân viên vẫn được giữ nguyên."
					: "Tài khoản đã có lịch sử nên được Vô hiệu hóa thay vì xóa vật lý.");
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}

	// ===================== HÀM PHỤ =====================

	/** Nếu tài khoản đang đăng nhập thì hỏi lại trước khi tiếp tục. */
	private boolean canhBaoDangDangNhap(int maTaiKhoan) {
		try {
			if (taiKhoanBUS.dangCoPhienDangNhap(maTaiKhoan)) {
				return confirm(
						"Tài khoản đang có phiên đăng nhập. Nếu tiếp tục, phiên đó sẽ bị kết thúc và ứng dụng sẽ tự đưa người dùng về màn hình đăng nhập.");
			}
			return true;
		} catch (Exception ex) {
			showError(ex);
			return false;
		}
	}

	private JComboBox<String> taoComboVaiTro(List<VaiTro> dsVaiTro) {
		JComboBox<String> cbo = new JComboBox<String>();
		for (VaiTro vt : dsVaiTro) {
			cbo.addItem(vt.getTenVaiTro() + " [" + vt.getCodeVaiTro() + "]");
		}
		return cbo;
	}
}

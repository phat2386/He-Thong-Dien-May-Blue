package app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import bus.Formats;
import bus.NhanVien_BUS;
import entity.NhanVien;

/** UC032 - Quản lý hồ sơ nhân viên (thêm, sửa, ngừng / kích hoạt). */
public class PanelNhanVien extends PanelBaseScreen {
	private static final long serialVersionUID = 1L;

	private final JTextField txtTim = UIFactory.field(320);
	private final JComboBox<String> cboTrangThai = UIFactory.combo("Tất cả trạng thái", "Đang hoạt động",
			"Đã ngừng làm việc", "Đang nghỉ phép");
	private final DefaultTableModel model = new DefaultTableModel(new Object[] { "Mã NV", "Họ tên", "Số điện thoại",
			"Thư điện tử", "Phòng ban", "Ngày vào làm", "Trạng thái", "Tài khoản" }, 0) {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final JTable table = UIFactory.table(model);
	private final NhanVien_BUS nhanVienBUS = new NhanVien_BUS();

	public PanelNhanVien() {
		super("Quản lý nhân viên",
				"Quản lý hồ sơ nhân viên; tài khoản và ca làm việc được xử lý tại các mục tương ứng.");

		JButton btnThem = UIFactory.primaryButton("+ Thêm nhân viên");
		JButton btnSua = UIFactory.secondaryButton("Cập nhật hồ sơ");
		JButton btnDoiTrangThai = UIFactory.secondaryButton("Ngừng / Kích hoạt");
		actions.add(btnThem);
		actions.add(btnSua);
		actions.add(btnDoiTrangThai);

		body.add(UIFactory.toolbar(new JLabel("Tìm"), txtTim, new JLabel("Trạng thái"), cboTrangThai),
				BorderLayout.NORTH);
		body.add(UIFactory.scroll(table), BorderLayout.CENTER);

		txtTim.addActionListener(e -> refreshData());
		cboTrangThai.addActionListener(e -> refreshData());
		btnThem.addActionListener(e -> themNhanVien());
		btnSua.addActionListener(e -> suaNhanVien());
		btnDoiTrangThai.addActionListener(e -> doiTrangThai());
		refreshData();
	}

	/** Tải lại bảng theo từ khóa và trạng thái đang chọn. */
	@Override
	public void refreshData() {
		try {
			String trangThai = null;
			String chon = String.valueOf(cboTrangThai.getSelectedItem());
			if ("Đang hoạt động".equals(chon)) {
				trangThai = "ACTIVE";
			} else if ("Đã ngừng làm việc".equals(chon)) {
				trangThai = "INACTIVE";
			} else if ("Đang nghỉ phép".equals(chon)) {
				trangThai = "ON_LEAVE";
			}

			List<NhanVien> ds = nhanVienBUS.timKiem(txtTim.getText(), trangThai);
			model.setRowCount(0);
			for (NhanVien nv : ds) {
				model.addRow(new Object[] { nv.getMaNhanVien(), nv.getHoTen(), nv.getSoDienThoai(),
						Formats.safe(nv.getEmail()), Formats.safe(nv.getPhongBan()), Formats.date(nv.getNgayVaoLam()),
						trangThaiNhanVien(nv.getTrangThai()),
						nv.getTrangThaiTaiKhoan() == null ? "Chưa cấp" : trangThaiTaiKhoan(nv.getTrangThaiTaiKhoan()) });
			}
		} catch (Exception ex) {
			showError(ex);
		}
	}

	/** Mã nhân viên của dòng đang chọn (null nếu chưa chọn). */
	private String maNhanVienDangChon() {
		int dong = table.getSelectedRow();
		return dong < 0 ? null : String.valueOf(model.getValueAt(table.convertRowIndexToModel(dong), 0));
	}

	private void themNhanVien() {
		JTextField txtMa = UIFactory.field(180), txtTen = UIFactory.field(260), txtSdt = UIFactory.field(180);
		JTextField txtEmail = UIFactory.field(260), txtPhongBan = UIFactory.field(220), txtNgay = UIFactory.field(160);
		txtNgay.setText(LocalDate.now().toString());

		JPanel form = taoForm();
		themDong(form, "Mã nhân viên", txtMa);
		themDong(form, "Họ tên", txtTen);
		themDong(form, "Số điện thoại", txtSdt);
		themDong(form, "Thư điện tử", txtEmail);
		themDong(form, "Phòng ban", txtPhongBan);
		themDong(form, "Ngày vào làm (yyyy-MM-dd)", txtNgay);

		if (JOptionPane.showConfirmDialog(DialogUtils.owner(this), form, "Thêm nhân viên",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
			return;
		}
		try {
			NhanVien nv = new NhanVien(txtMa.getText(), txtTen.getText(), txtSdt.getText(), txtEmail.getText(),
					txtPhongBan.getText(), LocalDate.parse(txtNgay.getText().trim()), "ACTIVE");
			nhanVienBUS.themNhanVien(nv);
			info("Đã tạo hồ sơ nhân viên. Tài khoản đăng nhập được cấp riêng tại mục Quản lý tài khoản.");
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}

	private void suaNhanVien() {
		String ma = maNhanVienDangChon();
		if (ma == null) {
			info("Hãy chọn nhân viên cần cập nhật.");
			return;
		}
		try {
			NhanVien cu = nhanVienBUS.timTheoMa(ma);
			if (cu == null) {
				info("Không tìm thấy nhân viên.");
				return;
			}
			JTextField txtTen = UIFactory.field(260), txtSdt = UIFactory.field(180), txtEmail = UIFactory.field(260);
			JTextField txtPhongBan = UIFactory.field(220), txtNgay = UIFactory.field(160);
			txtTen.setText(Formats.safe(cu.getHoTen()));
			txtSdt.setText(Formats.safe(cu.getSoDienThoai()));
			txtEmail.setText(Formats.safe(cu.getEmail()));
			txtPhongBan.setText(Formats.safe(cu.getPhongBan()));
			txtNgay.setText(String.valueOf(cu.getNgayVaoLam()));

			JPanel form = taoForm();
			themDong(form, "Mã nhân viên", new JLabel(ma));
			themDong(form, "Họ tên", txtTen);
			themDong(form, "Số điện thoại", txtSdt);
			themDong(form, "Thư điện tử", txtEmail);
			themDong(form, "Phòng ban", txtPhongBan);
			themDong(form, "Ngày vào làm (yyyy-MM-dd)", txtNgay);

			if (JOptionPane.showConfirmDialog(DialogUtils.owner(this), form, "Cập nhật nhân viên",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
				return;
			}
			NhanVien moi = new NhanVien(ma, txtTen.getText(), txtSdt.getText(), txtEmail.getText(),
					txtPhongBan.getText(), LocalDate.parse(txtNgay.getText().trim()), cu.getTrangThai());
			nhanVienBUS.capNhatNhanVien(moi);
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}

	private void doiTrangThai() {
		String ma = maNhanVienDangChon();
		if (ma == null) {
			info("Hãy chọn nhân viên.");
			return;
		}
		try {
			NhanVien nv = nhanVienBUS.timTheoMa(ma);
			if (nv == null) {
				info("Không tìm thấy nhân viên.");
				return;
			}
			boolean kichHoat = !"ACTIVE".equals(nv.getTrangThai());
			String thongBao = kichHoat
					? "Kích hoạt lại hồ sơ nhân viên? Tài khoản đăng nhập (nếu đang vô hiệu hóa) vẫn phải được kích hoạt riêng tại mục Quản lý tài khoản."
					: "Ngừng hoạt động nhân viên? Tài khoản sẽ bị vô hiệu hóa và các ca từ hôm nay trở đi sẽ bị hủy.";
			if (!confirm(thongBao)) {
				return;
			}
			nhanVienBUS.doiTrangThaiHoatDong(ma, kichHoat);
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}

	// ===================== HÀM PHỤ =====================

	private String trangThaiNhanVien(String ma) {
		if ("ACTIVE".equals(ma))
			return "Đang hoạt động";
		if ("INACTIVE".equals(ma))
			return "Đã ngừng làm việc";
		if ("ON_LEAVE".equals(ma))
			return "Đang nghỉ phép";
		return ma;
	}

	private String trangThaiTaiKhoan(String ma) {
		if ("ACTIVE".equals(ma))
			return "Đang hoạt động";
		if ("LOCKED".equals(ma))
			return "Đã khóa";
		if ("DISABLED".equals(ma))
			return "Ngừng hoạt động";
		if ("PENDING".equals(ma))
			return "Chờ xử lý";
		return ma;
	}

	private JPanel taoForm() {
		JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		return p;
	}

	private void themDong(JPanel p, String nhan, Component c) {
		p.add(new JLabel(nhan));
		p.add(c);
	}
}

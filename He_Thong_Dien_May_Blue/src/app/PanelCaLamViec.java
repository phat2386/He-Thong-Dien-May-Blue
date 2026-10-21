package app;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import bus.CaLamViec_BUS;
import bus.Formats;
import entity.CaLamViec;
import entity.PhanCa;
import entity.TaiKhoan;

/** UC006 - Phân ca cho nhân viên bán hàng và CSKH. */
public class PanelCaLamViec extends PanelBaseScreen {
	private static final long serialVersionUID = 1L;

	private final JTextField txtNgay = UIFactory.field(110);
	private final DefaultTableModel model = new DefaultTableModel(
			new Object[] { "Mã", "Ngày", "Ca", "Thời gian", "Mã NV", "Nhân viên", "Vai trò", "Trạng thái", "Phủ ca" },
			0) {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int r, int c) {
			return false;
		}
	};
	private final JTable table = UIFactory.table(model);
	private final CaLamViec_BUS caBUS = new CaLamViec_BUS();

	public PanelCaLamViec() {
		super("Phân ca làm việc",
				"Mỗi nhân viên tối đa một ca/ngày; kiểm tra nghỉ phép và đảm bảo ca có nhân viên bán hàng.");
		txtNgay.setText(LocalDate.now().toString());

		JButton btnPhanCa = UIFactory.primaryButton("+ Phân ca");
		JButton btnHuy = UIFactory.secondaryButton("Hủy phân ca");
		JButton btnLamMoi = UIFactory.secondaryButton("Làm mới");
		actions.add(btnPhanCa);
		actions.add(btnHuy);
		actions.add(btnLamMoi);

		body.add(UIFactory.toolbar(new JLabel("Ngày yyyy-MM-dd"), txtNgay), BorderLayout.NORTH);
		body.add(UIFactory.scroll(table), BorderLayout.CENTER);

		txtNgay.addActionListener(e -> refreshData());
		btnPhanCa.addActionListener(e -> phanCa());
		btnHuy.addActionListener(e -> huyPhanCa());
		btnLamMoi.addActionListener(e -> refreshData());
		refreshData();
	}

	/** Tải danh sách phân ca của ngày đang nhập. */
	@Override
	public void refreshData() {
		try {
			LocalDate ngay = LocalDate.parse(txtNgay.getText().trim());
			List<PhanCa> ds = caBUS.layPhanCaTheoNgay(ngay);
			model.setRowCount(0);
			for (PhanCa pc : ds) {
				model.addRow(new Object[] { pc.getMaPhanCa(), Formats.date(pc.getNgayLam()), pc.getTenCa(),
						pc.getGioBatDau() + " - " + pc.getGioKetThuc(), pc.getMaNhanVien(), pc.getHoTenNhanVien(),
						UiText.vi(pc.getCodeVaiTro()), UiText.vi(pc.getTrangThai()),
						pc.isDuNhanVienBanHang() ? "Đủ" : "Thiếu NV bán hàng" });
			}
		} catch (Exception ex) {
			showError(ex);
		}
	}

	private void phanCa() {
		try {
			LocalDate ngay = LocalDate.parse(txtNgay.getText().trim());

			List<TaiKhoan> dsNhanVien = caBUS.layNhanVienCoTheXepCa();
			List<CaLamViec> dsCa = caBUS.layCaDangDung();
			if (dsNhanVien.isEmpty() || dsCa.isEmpty()) {
				info("Không có nhân viên/ca làm việc hợp lệ để phân công.");
				return;
			}

			JComboBox<String> cboNhanVien = new JComboBox<String>();
			for (TaiKhoan tk : dsNhanVien) {
				cboNhanVien.addItem(tk.getNhanVien().getMaNhanVien() + " - " + tk.getNhanVien().getHoTen() + " ("
						+ tk.getVaiTro().getTenVaiTro() + ")");
			}
			JComboBox<String> cboCa = new JComboBox<String>();
			for (CaLamViec ca : dsCa) {
				cboCa.addItem(ca.getMaCa() + " - " + ca.getTenCa() + " " + ca.getGioBatDau() + "-" + ca.getGioKetThuc());
			}

			JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
			form.add(new JLabel("Nhân viên"));
			form.add(cboNhanVien);
			form.add(new JLabel("Ca"));
			form.add(cboCa);
			if (JOptionPane.showConfirmDialog(DialogUtils.owner(this), form, "Phân ca " + ngay,
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
				return;
			}

			String maNhanVien = dsNhanVien.get(cboNhanVien.getSelectedIndex()).getNhanVien().getMaNhanVien();
			int maCa = dsCa.get(cboCa.getSelectedIndex()).getMaCa();

			caBUS.phanCa(maNhanVien, ngay, maCa);
			refreshData();

			if (!caBUS.caDaCoNhanVienBanHang(ngay, maCa)) {
				info("Đã lưu phân ca, nhưng ca này đang thiếu Nhân viên bán hàng. Hãy bổ sung trước khi ca bắt đầu.");
			}
		} catch (Exception ex) {
			showError(ex);
		}
	}

	private void huyPhanCa() {
		int dong = table.getSelectedRow();
		if (dong < 0) {
			info("Hãy chọn phân ca.");
			return;
		}
		long ma = ((Number) model.getValueAt(table.convertRowIndexToModel(dong), 0)).longValue();
		if (!confirm("Hủy phân ca đã chọn? Nhân viên sẽ nhận thông báo hủy ca.")) {
			return;
		}
		try {
			caBUS.huyPhanCa(ma);
			refreshData();
		} catch (Exception ex) {
			showError(ex);
		}
	}
}

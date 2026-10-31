package ui.calamviec;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import service.EmployeeService;
import ui.dungchung.BaseScreenPanel;
import ui.dungchung.DialogUtils;
import ui.dungchung.UIFactory;
import util.Formats;

/** Màn hình phân ca: xem ca theo ngày, phân ca cho nhân viên, hủy phân ca. */
public class ShiftManagementPanel extends BaseScreenPanel {
    private static final long serialVersionUID = 1L;

    private final JTextField txtNgay = UIFactory.field(110);

    private final DefaultTableModel model = UIFactory.readOnlyModel(new Object[] {
            "Mã", "Ngày", "Ca", "Thời gian", "Mã NV",
            "Nhân viên", "Vai trò", "Trạng thái", "Phủ ca" });
    private final JTable table = UIFactory.table(model);

    private final EmployeeService service = new EmployeeService();

    public ShiftManagementPanel() {
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

    /** Tải lại bảng: hiện các ca đã phân trong ngày đang nhập. */
    @Override
    public void refreshData() {
        try {
            LocalDate ngay = LocalDate.parse(txtNgay.getText().trim());
            model.setRowCount(0);

            List<Map<String, Object>> danhSach = service.assignmentsByDate(ngay);

            for (Map<String, Object> dong : danhSach) {
                String thoiGian = dong.get("start_time") + " - " + dong.get("end_time");

                model.addRow(new Object[] {
                        dong.get("assignment_id"),
                        Formats.date(dong.get("work_date")),
                        dong.get("shift_name"),
                        thoiGian,
                        dong.get("employee_id"),
                        dong.get("full_name"),
                        Formats.vi(dong.get("role_code")),
                        Formats.vi(dong.get("status")),
                        dong.get("coverage") });
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Phân một nhân viên vào một ca trong ngày đang chọn. */
    private void phanCa() {
        try {
            LocalDate ngay = LocalDate.parse(txtNgay.getText().trim());

            JComboBox<String> cboNhanVien = new JComboBox<String>();
            JComboBox<String> cboCa = new JComboBox<String>();

            // Mỗi dòng trong ô chọn có dạng: "mã - tên ..."
            for (Map<String, Object> nv : service.assignableStaff()) {
                cboNhanVien.addItem(nv.get("employee_id") + " - " + nv.get("full_name")
                        + " (" + nv.get("role_name") + ")");
            }
            for (Map<String, Object> ca : service.activeShifts()) {
                cboCa.addItem(ca.get("shift_id") + " - " + ca.get("shift_name") + " "
                        + ca.get("start_time") + "-" + ca.get("end_time"));
            }

            if (cboNhanVien.getItemCount() == 0 || cboCa.getItemCount() == 0) {
                info("Không có nhân viên/ca làm việc hợp lệ để phân công.");
                return;
            }

            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Nhân viên"));
            form.add(cboNhanVien);
            form.add(new JLabel("Ca"));
            form.add(cboCa);

            int chon = JOptionPane.showConfirmDialog(DialogUtils.owner(this), form,
                    "Phân ca " + ngay, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (chon != JOptionPane.OK_OPTION) {
                return;
            }

            // Lấy phần mã đứng trước chữ " - " của dòng đã chọn
            String dongNhanVien = String.valueOf(cboNhanVien.getSelectedItem());
            String dongCa = String.valueOf(cboCa.getSelectedItem());
            String maNhanVien = dongNhanVien.substring(0, dongNhanVien.indexOf(" - "));
            int maCa = Integer.parseInt(dongCa.substring(0, dongCa.indexOf(" - ")));

            service.assignShift(maNhanVien, ngay, maCa);
            refreshData();

            // Nhắc nếu ca này vẫn chưa có nhân viên bán hàng nào
            int soNhanVienBanHang = service.countAssignedSales(ngay, maCa);
            if (soNhanVienBanHang == 0) {
                info("Đã lưu phân ca, nhưng ca này đang thiếu Nhân viên bán hàng. "
                        + "Hãy bổ sung trước khi ca bắt đầu.");
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    /** Hủy dòng phân ca đang chọn trong bảng. */
    private void huyPhanCa() {
        int dongChon = table.getSelectedRow();
        if (dongChon < 0) {
            info("Hãy chọn phân ca.");
            return;
        }

        int dongThat = table.convertRowIndexToModel(dongChon);
        long maPhanCa = ((Number) model.getValueAt(dongThat, 0)).longValue();

        if (!confirm("Hủy phân ca đã chọn? Nhân viên sẽ nhận thông báo hủy ca.")) {
            return;
        }

        try {
            service.cancelShiftAssignment(maPhanCa);
            refreshData();
        } catch (Exception ex) {
            showError(ex);
        }
    }
}

package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import connectDB.ConnectDB;
import entity.CaLamViec;
import entity.PhanCa;

/** Các bảng shifts, shift_assignments, leave_requests, notifications. */
public class CaLamViec_DAO {

	// ===================== DANH SÁCH CA =====================

	/** Các ca đang sử dụng, sắp theo giờ bắt đầu. */
	public List<CaLamViec> layCaDangDung() throws SQLException {
		String sql = "SELECT shift_id, shift_name, start_time, end_time, active FROM shifts WHERE active = 1 ORDER BY start_time";
		List<CaLamViec> ds = new ArrayList<CaLamViec>();
		try (Connection con = ConnectDB.getConnection();
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				ds.add(docCa(rs));
			}
		}
		return ds;
	}

	public CaLamViec timCaTheoMa(Connection con, int maCa) throws SQLException {
		String sql = "SELECT shift_id, shift_name, start_time, end_time, active FROM shifts WHERE shift_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, maCa);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? docCa(rs) : null;
			}
		}
	}

	private CaLamViec docCa(ResultSet rs) throws SQLException {
		CaLamViec ca = new CaLamViec();
		ca.setMaCa(rs.getInt("shift_id"));
		ca.setTenCa(rs.getString("shift_name"));
		ca.setGioBatDau(String.valueOf(rs.getObject("start_time")));
		ca.setGioKetThuc(String.valueOf(rs.getObject("end_time")));
		ca.setHoatDong(rs.getBoolean("active"));
		return ca;
	}

	// ===================== PHÂN CA =====================

	/** Các lượt phân ca đang hiệu lực trong một ngày, kèm thông tin ca đã đủ nhân viên bán hàng chưa. */
	public List<PhanCa> layPhanCaTheoNgay(LocalDate ngay) throws SQLException {
		String sql = "SELECT sa.assignment_id, sa.work_date, s.shift_id, s.shift_name, s.start_time, s.end_time, "
				+ "e.employee_id, e.full_name, ro.role_code, sa.status, "
				+ "CASE WHEN EXISTS (SELECT 1 FROM shift_assignments sx "
				+ "JOIN accounts ax ON ax.employee_id = sx.employee_id JOIN roles rx ON rx.role_id = ax.role_id "
				+ "WHERE sx.work_date = sa.work_date AND sx.shift_id = sa.shift_id AND sx.status = 'ASSIGNED' "
				+ "AND rx.role_code = 'SALES') THEN 1 ELSE 0 END AS du_ban_hang "
				+ "FROM shift_assignments sa JOIN shifts s ON s.shift_id = sa.shift_id "
				+ "JOIN employees e ON e.employee_id = sa.employee_id "
				+ "LEFT JOIN accounts a ON a.employee_id = e.employee_id LEFT JOIN roles ro ON ro.role_id = a.role_id "
				+ "WHERE sa.work_date = ? AND sa.status = 'ASSIGNED' ORDER BY s.start_time, e.full_name";
		List<PhanCa> ds = new ArrayList<PhanCa>();
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setDate(1, Date.valueOf(ngay));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					PhanCa pc = new PhanCa();
					pc.setMaPhanCa(rs.getLong("assignment_id"));
					pc.setNgayLam(rs.getDate("work_date").toLocalDate());
					pc.setMaCa(rs.getInt("shift_id"));
					pc.setTenCa(rs.getString("shift_name"));
					pc.setGioBatDau(String.valueOf(rs.getObject("start_time")));
					pc.setGioKetThuc(String.valueOf(rs.getObject("end_time")));
					pc.setMaNhanVien(rs.getString("employee_id"));
					pc.setHoTenNhanVien(rs.getString("full_name"));
					pc.setCodeVaiTro(rs.getString("role_code"));
					pc.setTrangThai(rs.getString("status"));
					pc.setDuNhanVienBanHang(rs.getInt("du_ban_hang") == 1);
					ds.add(pc);
				}
			}
		}
		return ds;
	}

	/** Tìm một lượt phân ca theo mã (kèm tên ca, vai trò nhân viên). Trả về null nếu không có. */
	public PhanCa timPhanCaTheoMa(Connection con, long maPhanCa) throws SQLException {
		String sql = "SELECT sa.assignment_id, sa.employee_id, sa.work_date, sa.shift_id, sa.status, "
				+ "e.full_name, r.role_code, s.shift_name FROM shift_assignments sa "
				+ "JOIN employees e ON e.employee_id = sa.employee_id JOIN shifts s ON s.shift_id = sa.shift_id "
				+ "LEFT JOIN accounts a ON a.employee_id = e.employee_id LEFT JOIN roles r ON r.role_id = a.role_id "
				+ "WHERE sa.assignment_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setLong(1, maPhanCa);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				PhanCa pc = new PhanCa();
				pc.setMaPhanCa(rs.getLong("assignment_id"));
				pc.setMaNhanVien(rs.getString("employee_id"));
				pc.setNgayLam(rs.getDate("work_date").toLocalDate());
				pc.setMaCa(rs.getInt("shift_id"));
				pc.setTrangThai(rs.getString("status"));
				pc.setHoTenNhanVien(rs.getString("full_name"));
				pc.setCodeVaiTro(rs.getString("role_code"));
				pc.setTenCa(rs.getString("shift_name"));
				return pc;
			}
		}
	}

	/** Nhân viên đã được duyệt nghỉ vào ngày này? */
	public boolean daDuyetNghi(Connection con, String maNhanVien, LocalDate ngay) throws SQLException {
		String sql = "SELECT COUNT(*) FROM leave_requests WHERE employee_id = ? AND leave_date = ? AND status = 'APPROVED'";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.setDate(2, Date.valueOf(ngay));
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		}
	}

	/** Nhân viên đã có ca trong ngày này? */
	public boolean daCoCaTrongNgay(Connection con, String maNhanVien, LocalDate ngay) throws SQLException {
		String sql = "SELECT COUNT(*) FROM shift_assignments WHERE employee_id = ? AND work_date = ? AND status = 'ASSIGNED'";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.setDate(2, Date.valueOf(ngay));
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		}
	}

	/** Mã lượt phân ca đã bị hủy trước đó (cùng nhân viên, ngày, ca) để dùng lại; null nếu không có. */
	public Long timPhanCaDaHuy(Connection con, String maNhanVien, LocalDate ngay, int maCa) throws SQLException {
		String sql = "SELECT TOP 1 assignment_id FROM shift_assignments "
				+ "WHERE employee_id = ? AND work_date = ? AND shift_id = ? AND status = 'CANCELLED' "
				+ "ORDER BY assignment_id DESC";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.setDate(2, Date.valueOf(ngay));
			ps.setInt(3, maCa);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? Long.valueOf(rs.getLong(1)) : null;
			}
		}
	}

	/** Kích hoạt lại một lượt phân ca đã hủy. */
	public void kichHoatLai(Connection con, long maPhanCa, String nguoiPhan) throws SQLException {
		String sql = "UPDATE shift_assignments SET assigned_by = ?, status = 'ASSIGNED', created_at = SYSDATETIME() "
				+ "WHERE assignment_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, nguoiPhan);
			ps.setLong(2, maPhanCa);
			ps.executeUpdate();
		}
	}

	public void them(Connection con, String maNhanVien, LocalDate ngay, int maCa, String nguoiPhan)
			throws SQLException {
		String sql = "INSERT INTO shift_assignments(employee_id, work_date, shift_id, assigned_by, status) "
				+ "VALUES(?, ?, ?, ?, 'ASSIGNED')";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.setDate(2, Date.valueOf(ngay));
			ps.setInt(3, maCa);
			ps.setString(4, nguoiPhan);
			ps.executeUpdate();
		}
	}

	public void huy(Connection con, long maPhanCa) throws SQLException {
		try (PreparedStatement ps = con
				.prepareStatement("UPDATE shift_assignments SET status = 'CANCELLED' WHERE assignment_id = ?")) {
			ps.setLong(1, maPhanCa);
			ps.executeUpdate();
		}
	}

	/** Số nhân viên khác (không tính lượt phân ca này) còn làm trong cùng ngày + ca. */
	public int demNhanVienKhac(Connection con, LocalDate ngay, int maCa, long loaiTruMaPhanCa) throws SQLException {
		String sql = "SELECT COUNT(*) FROM shift_assignments "
				+ "WHERE work_date = ? AND shift_id = ? AND status = 'ASSIGNED' AND assignment_id <> ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setDate(1, Date.valueOf(ngay));
			ps.setInt(2, maCa);
			ps.setLong(3, loaiTruMaPhanCa);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	/** Số nhân viên bán hàng khác (không tính lượt phân ca này) trong cùng ngày + ca. */
	public int demNhanVienBanHangKhac(Connection con, LocalDate ngay, int maCa, long loaiTruMaPhanCa)
			throws SQLException {
		String sql = "SELECT COUNT(*) FROM shift_assignments sa JOIN accounts a ON a.employee_id = sa.employee_id "
				+ "JOIN roles r ON r.role_id = a.role_id WHERE sa.work_date = ? AND sa.shift_id = ? "
				+ "AND sa.status = 'ASSIGNED' AND sa.assignment_id <> ? AND r.role_code = 'SALES'";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setDate(1, Date.valueOf(ngay));
			ps.setInt(2, maCa);
			ps.setLong(3, loaiTruMaPhanCa);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	/** Số nhân viên bán hàng đang được phân vào ngày + ca (để cảnh báo thiếu người). */
	public int demNhanVienBanHang(LocalDate ngay, int maCa) throws SQLException {
		String sql = "SELECT COUNT(*) FROM shift_assignments sa JOIN accounts a ON a.employee_id = sa.employee_id "
				+ "JOIN roles r ON r.role_id = a.role_id WHERE sa.work_date = ? AND sa.shift_id = ? "
				+ "AND sa.status = 'ASSIGNED' AND r.role_code = 'SALES'";
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setDate(1, Date.valueOf(ngay));
			ps.setInt(2, maCa);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	// ===================== THÔNG BÁO =====================

	public void themThongBao(Connection con, String maNhanVien, String tieuDe, String noiDung) throws SQLException {
		String sql = "INSERT INTO notifications(employee_id, title, content) VALUES(?, ?, ?)";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.setNString(2, tieuDe);
			ps.setString(3, noiDung);
			ps.executeUpdate();
		}
	}
}

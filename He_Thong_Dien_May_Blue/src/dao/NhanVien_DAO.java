package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import connectDB.ConnectDB;
import entity.NhanVien;

/** Bảng employees. */
public class NhanVien_DAO {

	private NhanVien docNhanVien(ResultSet rs) throws SQLException {
		NhanVien nv = new NhanVien();
		nv.setMaNhanVien(rs.getString("employee_id"));
		nv.setHoTen(rs.getString("full_name"));
		nv.setSoDienThoai(rs.getString("phone"));
		nv.setEmail(rs.getString("email"));
		nv.setPhongBan(rs.getString("department"));
		if (rs.getDate("join_date") != null) {
			nv.setNgayVaoLam(rs.getDate("join_date").toLocalDate());
		}
		nv.setTrangThai(rs.getString("status"));
		return nv;
	}

	/** Danh sách hiển thị (trừ hồ sơ ADMIN), kèm trạng thái tài khoản. trangThai = null thì không lọc. */
	public List<NhanVien> timKiem(String tuKhoa, String trangThai) throws SQLException {
		String sql = "SELECT e.employee_id, e.full_name, e.phone, e.email, e.department, e.join_date, e.status, "
				+ "a.status AS account_status FROM employees e "
				+ "LEFT JOIN accounts a ON a.employee_id = e.employee_id "
				+ "LEFT JOIN roles r ON r.role_id = a.role_id "
				+ "WHERE ISNULL(r.role_code,'') <> 'ADMIN' "
				+ "AND (e.employee_id LIKE ? OR e.full_name LIKE ? OR e.phone LIKE ? OR ISNULL(e.email,'') LIKE ?) "
				+ (trangThai == null ? "" : "AND e.status = ? ") + "ORDER BY e.full_name";
		String kw = "%" + tuKhoa + "%";
		List<NhanVien> ds = new ArrayList<NhanVien>();
		try (Connection con = ConnectDB.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, kw);
			ps.setString(2, kw);
			ps.setString(3, kw);
			ps.setString(4, kw);
			if (trangThai != null) {
				ps.setString(5, trangThai);
			}
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					NhanVien nv = docNhanVien(rs);
					nv.setTrangThaiTaiKhoan(rs.getString("account_status"));
					ds.add(nv);
				}
			}
		}
		return ds;
	}

	public NhanVien timTheoMa(String maNhanVien) throws SQLException {
		try (Connection con = ConnectDB.getConnection()) {
			return timTheoMa(con, maNhanVien);
		}
	}

	public NhanVien timTheoMa(Connection con, String maNhanVien) throws SQLException {
		String sql = "SELECT employee_id, full_name, phone, email, department, join_date, status "
				+ "FROM employees WHERE employee_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? docNhanVien(rs) : null;
			}
		}
	}

	/** Mã nhân viên hoặc số điện thoại đã có người dùng? */
	public boolean trungMaHoacSdt(Connection con, String maNhanVien, String soDienThoai) throws SQLException {
		String sql = "SELECT COUNT(*) FROM employees WHERE employee_id = ? OR phone = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.setString(2, soDienThoai);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		}
	}

	/** Số điện thoại đã thuộc nhân viên khác? */
	public boolean sdtThuocNguoiKhac(Connection con, String soDienThoai, String maNhanVien) throws SQLException {
		String sql = "SELECT COUNT(*) FROM employees WHERE phone = ? AND employee_id <> ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, soDienThoai);
			ps.setString(2, maNhanVien);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		}
	}

	/** Thêm nhân viên mới (trạng thái ACTIVE). */
	public void them(Connection con, NhanVien nv) throws SQLException {
		String sql = "INSERT INTO employees(employee_id, full_name, phone, email, department, join_date, status) "
				+ "VALUES(?, ?, ?, ?, ?, ?, 'ACTIVE')";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, nv.getMaNhanVien());
			ps.setString(2, nv.getHoTen());
			ps.setString(3, nv.getSoDienThoai());
			ps.setString(4, nv.getEmail());
			ps.setString(5, nv.getPhongBan());
			ps.setDate(6, Date.valueOf(nv.getNgayVaoLam()));
			ps.executeUpdate();
		}
	}

	public void capNhat(Connection con, NhanVien nv) throws SQLException {
		String sql = "UPDATE employees SET full_name = ?, phone = ?, email = ?, department = ?, join_date = ?, "
				+ "updated_at = SYSDATETIME() WHERE employee_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, nv.getHoTen());
			ps.setString(2, nv.getSoDienThoai());
			ps.setString(3, nv.getEmail());
			ps.setString(4, nv.getPhongBan());
			ps.setDate(5, Date.valueOf(nv.getNgayVaoLam()));
			ps.setString(6, nv.getMaNhanVien());
			ps.executeUpdate();
		}
	}

	public void doiTrangThai(Connection con, String maNhanVien, String trangThaiMoi) throws SQLException {
		String sql = "UPDATE employees SET status = ?, updated_at = SYSDATETIME() WHERE employee_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, trangThaiMoi);
			ps.setString(2, maNhanVien);
			ps.executeUpdate();
		}
	}

	/** Khi nhân viên ngừng làm việc: vô hiệu hóa tài khoản của họ. */
	public void voHieuHoaTaiKhoan(Connection con, String maNhanVien) throws SQLException {
		String sql = "UPDATE accounts SET status = 'DISABLED', locked_until = NULL, updated_at = SYSDATETIME() "
				+ "WHERE employee_id = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.executeUpdate();
		}
	}

	/** Khi nhân viên ngừng làm việc: hủy các ca từ hôm nay trở đi. */
	public void huyCaTuongLai(Connection con, String maNhanVien) throws SQLException {
		String sql = "UPDATE shift_assignments SET status = 'CANCELLED' "
				+ "WHERE employee_id = ? AND status = 'ASSIGNED' AND work_date >= CAST(GETDATE() AS DATE)";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, maNhanVien);
			ps.executeUpdate();
		}
	}

	/** Nhân viên đang hoạt động nhưng chưa có tài khoản (để cấp tài khoản). */
	public List<NhanVien> layNhanVienChuaCoTaiKhoan() throws SQLException {
		String sql = "SELECT e.employee_id, e.full_name, e.phone, e.email, e.department, e.join_date, e.status "
				+ "FROM employees e WHERE e.status = 'ACTIVE' "
				+ "AND NOT EXISTS (SELECT 1 FROM accounts a WHERE a.employee_id = e.employee_id) "
				+ "ORDER BY e.full_name";
		List<NhanVien> ds = new ArrayList<NhanVien>();
		try (Connection con = ConnectDB.getConnection();
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				ds.add(docNhanVien(rs));
			}
		}
		return ds;
	}
}

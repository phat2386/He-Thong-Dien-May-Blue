package entity;

import java.time.LocalDate;

public class NhanVien {
	private String maNhanVien;   // employee_id
	private String hoTen;        // full_name
	private String soDienThoai;  // phone
	private String email;
	private String phongBan;     // department
	private LocalDate ngayVaoLam; // join_date
	private String trangThai;    // ACTIVE / INACTIVE / ON_LEAVE
	private String trangThaiTaiKhoan; // chỉ để hiển thị trên bảng (null = chưa cấp tài khoản)

	public NhanVien() {
	}

	public NhanVien(String maNhanVien, String hoTen, String soDienThoai, String email, String phongBan,
			LocalDate ngayVaoLam, String trangThai) {
		this.maNhanVien = maNhanVien;
		this.hoTen = hoTen;
		this.soDienThoai = soDienThoai;
		this.email = email;
		this.phongBan = phongBan;
		this.ngayVaoLam = ngayVaoLam;
		this.trangThai = trangThai;
	}

	public String getMaNhanVien() {
		return maNhanVien;
	}

	public void setMaNhanVien(String maNhanVien) {
		this.maNhanVien = maNhanVien;
	}

	public String getHoTen() {
		return hoTen;
	}

	public void setHoTen(String hoTen) {
		this.hoTen = hoTen;
	}

	public String getSoDienThoai() {
		return soDienThoai;
	}

	public void setSoDienThoai(String soDienThoai) {
		this.soDienThoai = soDienThoai;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhongBan() {
		return phongBan;
	}

	public void setPhongBan(String phongBan) {
		this.phongBan = phongBan;
	}

	public LocalDate getNgayVaoLam() {
		return ngayVaoLam;
	}

	public void setNgayVaoLam(LocalDate ngayVaoLam) {
		this.ngayVaoLam = ngayVaoLam;
	}

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}

	public String getTrangThaiTaiKhoan() {
		return trangThaiTaiKhoan;
	}

	public void setTrangThaiTaiKhoan(String trangThaiTaiKhoan) {
		this.trangThaiTaiKhoan = trangThaiTaiKhoan;
	}
}

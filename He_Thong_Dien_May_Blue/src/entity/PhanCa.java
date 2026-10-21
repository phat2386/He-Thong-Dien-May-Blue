package entity;

import java.time.LocalDate;

/** Một lượt phân ca: nhân viên nào làm ca nào, ngày nào. */
public class PhanCa {
	private long maPhanCa;        // assignment_id
	private LocalDate ngayLam;    // work_date
	private int maCa;             // shift_id
	private String tenCa;
	private String gioBatDau;
	private String gioKetThuc;
	private String maNhanVien;
	private String hoTenNhanVien;
	private String codeVaiTro;    // SALES / CUSTOMER_SERVICE ...
	private String trangThai;     // ASSIGNED / CANCELLED
	private boolean duNhanVienBanHang; // ca này đã có nhân viên bán hàng chưa

	public PhanCa() {
	}

	public long getMaPhanCa() {
		return maPhanCa;
	}

	public void setMaPhanCa(long maPhanCa) {
		this.maPhanCa = maPhanCa;
	}

	public LocalDate getNgayLam() {
		return ngayLam;
	}

	public void setNgayLam(LocalDate ngayLam) {
		this.ngayLam = ngayLam;
	}

	public int getMaCa() {
		return maCa;
	}

	public void setMaCa(int maCa) {
		this.maCa = maCa;
	}

	public String getTenCa() {
		return tenCa;
	}

	public void setTenCa(String tenCa) {
		this.tenCa = tenCa;
	}

	public String getGioBatDau() {
		return gioBatDau;
	}

	public void setGioBatDau(String gioBatDau) {
		this.gioBatDau = gioBatDau;
	}

	public String getGioKetThuc() {
		return gioKetThuc;
	}

	public void setGioKetThuc(String gioKetThuc) {
		this.gioKetThuc = gioKetThuc;
	}

	public String getMaNhanVien() {
		return maNhanVien;
	}

	public void setMaNhanVien(String maNhanVien) {
		this.maNhanVien = maNhanVien;
	}

	public String getHoTenNhanVien() {
		return hoTenNhanVien;
	}

	public void setHoTenNhanVien(String hoTenNhanVien) {
		this.hoTenNhanVien = hoTenNhanVien;
	}

	public String getCodeVaiTro() {
		return codeVaiTro;
	}

	public void setCodeVaiTro(String codeVaiTro) {
		this.codeVaiTro = codeVaiTro;
	}

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}

	public boolean isDuNhanVienBanHang() {
		return duNhanVienBanHang;
	}

	public void setDuNhanVienBanHang(boolean duNhanVienBanHang) {
		this.duNhanVienBanHang = duNhanVienBanHang;
	}
}

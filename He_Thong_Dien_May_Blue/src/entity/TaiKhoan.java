package entity;

import java.time.LocalDateTime;

public class TaiKhoan {
	private int maTaiKhoan;            // account_id
	private String tenDangNhap;        // username
	private String matKhauHash;        // password_hash (SHA-256)
	private String trangThai;          // ACTIVE / LOCKED / DISABLED / PENDING
	private boolean dangNhapLanDau;    // first_login
	private int soLanSai;              // failed_attempts
	private LocalDateTime khoaDen;     // locked_until
	private LocalDateTime dangNhapCuoi; // last_login_at
	private NhanVien nhanVien;
	private VaiTro vaiTro;

	public TaiKhoan() {
	}

	public int getMaTaiKhoan() {
		return maTaiKhoan;
	}

	public void setMaTaiKhoan(int maTaiKhoan) {
		this.maTaiKhoan = maTaiKhoan;
	}

	public String getTenDangNhap() {
		return tenDangNhap;
	}

	public void setTenDangNhap(String tenDangNhap) {
		this.tenDangNhap = tenDangNhap;
	}

	public String getMatKhauHash() {
		return matKhauHash;
	}

	public void setMatKhauHash(String matKhauHash) {
		this.matKhauHash = matKhauHash;
	}

	public String getTrangThai() {
		return trangThai;
	}

	public void setTrangThai(String trangThai) {
		this.trangThai = trangThai;
	}

	public boolean isDangNhapLanDau() {
		return dangNhapLanDau;
	}

	public void setDangNhapLanDau(boolean dangNhapLanDau) {
		this.dangNhapLanDau = dangNhapLanDau;
	}

	public int getSoLanSai() {
		return soLanSai;
	}

	public void setSoLanSai(int soLanSai) {
		this.soLanSai = soLanSai;
	}

	public LocalDateTime getKhoaDen() {
		return khoaDen;
	}

	public void setKhoaDen(LocalDateTime khoaDen) {
		this.khoaDen = khoaDen;
	}

	public LocalDateTime getDangNhapCuoi() {
		return dangNhapCuoi;
	}

	public void setDangNhapCuoi(LocalDateTime dangNhapCuoi) {
		this.dangNhapCuoi = dangNhapCuoi;
	}

	public NhanVien getNhanVien() {
		return nhanVien;
	}

	public void setNhanVien(NhanVien nhanVien) {
		this.nhanVien = nhanVien;
	}

	public VaiTro getVaiTro() {
		return vaiTro;
	}

	public void setVaiTro(VaiTro vaiTro) {
		this.vaiTro = vaiTro;
	}
}

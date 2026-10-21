package entity;

/** Kết quả trả về sau khi đăng nhập. */
public class KetQuaDangNhap {
	private final boolean thanhCong;
	private final String thongBao;
	private final TaiKhoan taiKhoan;
	private final boolean canDoiMatKhau;

	private KetQuaDangNhap(boolean thanhCong, String thongBao, TaiKhoan taiKhoan, boolean canDoiMatKhau) {
		this.thanhCong = thanhCong;
		this.thongBao = thongBao;
		this.taiKhoan = taiKhoan;
		this.canDoiMatKhau = canDoiMatKhau;
	}

	public static KetQuaDangNhap thanhCong(TaiKhoan taiKhoan, boolean canDoiMatKhau) {
		return new KetQuaDangNhap(true, "Đăng nhập thành công.", taiKhoan, canDoiMatKhau);
	}

	public static KetQuaDangNhap thatBai(String thongBao) {
		return new KetQuaDangNhap(false, thongBao, null, false);
	}

	public boolean isThanhCong() {
		return thanhCong;
	}

	public String getThongBao() {
		return thongBao;
	}

	public TaiKhoan getTaiKhoan() {
		return taiKhoan;
	}

	public boolean isCanDoiMatKhau() {
		return canDoiMatKhau;
	}
}

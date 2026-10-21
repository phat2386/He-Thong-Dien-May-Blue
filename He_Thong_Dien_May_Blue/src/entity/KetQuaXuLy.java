package entity;

/** Kết quả chung của một thao tác (thành công / thất bại kèm thông báo). */
public class KetQuaXuLy {
	private final boolean thanhCong;
	private final String thongBao;

	private KetQuaXuLy(boolean thanhCong, String thongBao) {
		this.thanhCong = thanhCong;
		this.thongBao = thongBao;
	}

	public static KetQuaXuLy thanhCong(String thongBao) {
		return new KetQuaXuLy(true, thongBao);
	}

	public static KetQuaXuLy thatBai(String thongBao) {
		return new KetQuaXuLy(false, thongBao);
	}

	public boolean isThanhCong() {
		return thanhCong;
	}

	public String getThongBao() {
		return thongBao;
	}
}

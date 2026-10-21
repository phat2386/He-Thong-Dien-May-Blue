package entity;

public class CaLamViec {
	private int maCa;            // shift_id
	private String tenCa;        // shift_name
	private String gioBatDau;    // start_time
	private String gioKetThuc;   // end_time
	private boolean hoatDong;    // active

	public CaLamViec() {
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

	public boolean isHoatDong() {
		return hoatDong;
	}

	public void setHoatDong(boolean hoatDong) {
		this.hoatDong = hoatDong;
	}
}

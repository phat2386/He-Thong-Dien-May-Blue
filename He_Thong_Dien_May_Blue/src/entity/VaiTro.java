package entity;

public class VaiTro {
	private int maVaiTro;        // role_id
	private String codeVaiTro;   // role_code: ADMIN, MANAGER, SALES, CUSTOMER_SERVICE
	private String tenVaiTro;    // role_name
	private String moTa;         // description

	public VaiTro() {
	}

	public VaiTro(int maVaiTro, String codeVaiTro, String tenVaiTro, String moTa) {
		this.maVaiTro = maVaiTro;
		this.codeVaiTro = codeVaiTro;
		this.tenVaiTro = tenVaiTro;
		this.moTa = moTa;
	}

	public int getMaVaiTro() {
		return maVaiTro;
	}

	public void setMaVaiTro(int maVaiTro) {
		this.maVaiTro = maVaiTro;
	}

	public String getCodeVaiTro() {
		return codeVaiTro;
	}

	public void setCodeVaiTro(String codeVaiTro) {
		this.codeVaiTro = codeVaiTro;
	}

	public String getTenVaiTro() {
		return tenVaiTro;
	}

	public void setTenVaiTro(String tenVaiTro) {
		this.tenVaiTro = tenVaiTro;
	}

	public String getMoTa() {
		return moTa;
	}

	public void setMoTa(String moTa) {
		this.moTa = moTa;
	}
}

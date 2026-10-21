package app;

import bus.DangNhap_BUS;
import entity.KetQuaDangNhap;

/** Nối màn hình đăng nhập (UIDangNhap) với phần xử lý đăng nhập (DangNhap_BUS). */
public class DangNhapController {

	private final UIDangNhap view;
	private final DangNhap_BUS dangNhapBUS;

	public DangNhapController(UIDangNhap view) {
		this.view = view;
		this.dangNhapBUS = new DangNhap_BUS();
	}

	public void dangNhap() {
		KetQuaDangNhap ketQua = dangNhapBUS.dangNhap(view.getTenDangNhap(), view.getPassword());

		if (!ketQua.isThanhCong()) {
			view.showError(ketQua.getThongBao());
			view.clearPassword();
			return;
		}

		view.clearError();

		// Đăng nhập lần đầu bắt buộc đổi mật khẩu trước khi vào hệ thống
		if (ketQua.isCanDoiMatKhau()) {
			UIDoiMatKhau hopThoai = new UIDoiMatKhau(view, dangNhapBUS, true, new Runnable() {
				@Override
				public void run() {
					moManHinhChinh();
				}
			});
			hopThoai.setVisible(true);
			return;
		}

		moManHinhChinh();
	}

	private void moManHinhChinh() {
		view.dispose();
		new UIMain(dangNhapBUS).setVisible(true);
	}
}

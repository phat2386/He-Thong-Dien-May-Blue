package bus;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import entity.TaiKhoan;

/** Lưu thông tin người đang đăng nhập (tài khoản, quyền) trong suốt phiên làm việc. */
public final class PhienDangNhap {
	private static TaiKhoan taiKhoanHienTai;
	private static Long maNhatKy;
	private static Set<String> danhSachQuyen = Collections.emptySet();

	private PhienDangNhap() {
	}

	public static void dangNhap(TaiKhoan taiKhoan, long maNhatKyDangNhap, Set<String> quyen) {
		taiKhoanHienTai = taiKhoan;
		maNhatKy = maNhatKyDangNhap;
		danhSachQuyen = Collections.unmodifiableSet(new HashSet<String>(quyen));
	}

	public static TaiKhoan getTaiKhoanHienTai() {
		return taiKhoanHienTai;
	}

	public static Long getMaNhatKy() {
		return maNhatKy;
	}

	public static Set<String> getDanhSachQuyen() {
		return danhSachQuyen;
	}

	/** ADMIN được dùng mọi chức năng; vai trò khác phải được cấp quyền trong CSDL. */
	public static boolean coQuyen(String maQuyen) {
		if (taiKhoanHienTai != null && taiKhoanHienTai.getVaiTro() != null
				&& "ADMIN".equals(taiKhoanHienTai.getVaiTro().getCodeVaiTro())) {
			return true;
		}
		return danhSachQuyen.contains(maQuyen);
	}

	public static boolean daDangNhap() {
		return taiKhoanHienTai != null;
	}

	/** Kiểm tra quyền; không đủ quyền thì ném lỗi. Trả về tài khoản đang đăng nhập. */
	public static TaiKhoan yeuCauQuyen(String maQuyen) {
		if (taiKhoanHienTai == null || taiKhoanHienTai.getNhanVien() == null || taiKhoanHienTai.getVaiTro() == null)
			throw new IllegalStateException("Phiên đăng nhập không hợp lệ.");
		if (!coQuyen(maQuyen))
			throw new IllegalStateException("Tài khoản chưa được Admin cấp quyền thực hiện chức năng này.");
		return taiKhoanHienTai;
	}

	public static void xoaPhien() {
		taiKhoanHienTai = null;
		maNhatKy = null;
		danhSachQuyen = Collections.emptySet();
	}
}

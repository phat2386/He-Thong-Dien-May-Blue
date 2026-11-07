package service;
import java.math.BigDecimal;
import java.util.*;
public final class ChinhSachThanhVien {
    public static final BigDecimal SO_TIEN_MOT_DIEM = new BigDecimal("10000");
    public static final String TIEU_CHUAN = "Tiêu chuẩn";
    public static final String BAC = "Bạc";
    public static final String VANG = "Vàng";
    public static final String BACH_KIM = "Bạch kim";
    private ChinhSachThanhVien() {
    }
    public static String hangTheoDiem(int diem) {
        if (diem >= 10000) return BACH_KIM;
        if (diem >= 4000) return VANG;
        if (diem >= 1000) return BAC;
        return TIEU_CHUAN;
    }
    public static int maHang(String hang) {
        if (BAC.equals(hang)) return 2;
        if (VANG.equals(hang)) return 3;
        if (BACH_KIM.equals(hang)) return 4;
        return 1;
    }
    public static String maHangKyHieu(String hang) {
        if (BAC.equals(hang)) return "SILVER";
        if (VANG.equals(hang)) return "GOLD";
        if (BACH_KIM.equals(hang)) return "PLATINUM";
        return "STANDARD";
    }
    public static BigDecimal tyLeGiam(String hang) {
        if (BAC.equals(hang)) return new BigDecimal("2");
        if (VANG.equals(hang)) return new BigDecimal("5");
        if (BACH_KIM.equals(hang)) return new BigDecimal("8");
        return BigDecimal.ZERO;
    }
    public static BigDecimal heSoDiem(String hang) {
        if (BAC.equals(hang)) return new BigDecimal("1.10");
        if (VANG.equals(hang)) return new BigDecimal("1.25");
        if (BACH_KIM.equals(hang)) return new BigDecimal("1.50");
        return BigDecimal.ONE;
    }
    public static List<Map<String, Object>> danhSachMacDinh() {
        List<Map<String, Object>> ketQua = new ArrayList<>();
        ketQua.add(hang(1, "STANDARD", TIEU_CHUAN, 0));
        ketQua.add(hang(2, "SILVER", BAC, 1000));
        ketQua.add(hang(3, "GOLD", VANG, 4000));
        ketQua.add(hang(4, "PLATINUM", BACH_KIM, 10000));
        return ketQua;
    }
    private static Map<String, Object> hang(int id, String code, String name, int min) {
        Map<String, Object> dong = new LinkedHashMap<>();
        dong.put("tier_id", id);
        dong.put("tier_code", code);
        dong.put("tier_name", name);
        dong.put("min_points", min);
        dong.put("discount_rate", tyLeGiam(name));
        dong.put("point_multiplier", heSoDiem(name));
        return dong;
    }
}

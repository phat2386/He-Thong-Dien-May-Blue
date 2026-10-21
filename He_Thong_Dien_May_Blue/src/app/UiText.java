package app;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UiText {
    private UiText() {}

    private static final Map<String,String> TO_VI = new LinkedHashMap<String,String>();
    private static final Map<String,String> TO_CODE = new LinkedHashMap<String,String>();

    static {
        // Trạng thái chung
        put("ACTIVE", "Đang hoạt động");
        put("INACTIVE", "Đã ngừng làm việc");
        put("ON_LEAVE", "Đang nghỉ phép");
        put("LOCKED", "Đã khóa");
        put("DISABLED", "Ngừng hoạt động");
        put("PENDING", "Chờ xử lý");
        put("CANCELLED", "Đã hủy");
        put("COMPLETED", "Hoàn tất");
        put("APPROVED", "Đã duyệt");
        put("REJECTED", "Đã từ chối");
        put("WAITING_APPROVAL", "Chờ duyệt");
        put("OPEN", "Chưa chốt");
        put("FINALIZED", "Đã chốt");
        put("REVERSED", "Đã khấu trừ");
        put("ASSIGNED", "Đã phân công");
        put("SUCCESS", "Thành công");

        // Hóa đơn / thanh toán
        put("PAID", "Đã thanh toán");
        put("PARTIAL_REFUND", "Hoàn tiền một phần");
        put("REFUNDED", "Đã hoàn tiền");
        put("CASH", "Tiền mặt");
        put("CARD", "Thẻ");
        put("BANK_TRANSFER", "Chuyển khoản");
        put("INSTALLMENT", "Trả góp");

        // Sản phẩm
        put("OUT_OF_STOCK", "Hết hàng");
        put("DISCONTINUED", "Ngừng kinh doanh");

        // Khuyến mãi
        put("SCHEDULED", "Sắp diễn ra");
        put("ENDED", "Đã kết thúc");
        put("PERCENT", "Phần trăm");
        put("FIXED", "Số tiền cố định");

        // Giao hàng
        put("PREPARING", "Đang chuẩn bị");
        put("DELIVERING", "Đang giao");
        put("DELIVERED", "Đã giao");
        put("INSTALLED", "Đã lắp đặt");
        put("FAILED", "Giao không thành công");
        put("INNER_CITY", "Nội thành");
        put("OTHER_PROVINCE", "Tỉnh / thành khác");

        // Thành viên
        put("STANDARD", "Tiêu chuẩn");
        put("SILVER", "Bạc");
        put("GOLD", "Vàng");
        put("PLATINUM", "Bạch kim");
        put("EARN", "Cộng điểm");
        put("REVERSAL", "Trừ điểm");
        put("INVALIDATED", "Đã vô hiệu hóa");

        // Công nợ
        put("UNPAID", "Chưa thanh toán");
        put("PARTIAL", "Thanh toán một phần");
        put("OVERDUE", "Quá hạn");

        // Bảo hành / khiếu nại
        put("REPAIRING", "Đang sửa chữa");
        put("RETURNED", "Đã trả khách");
        put("REPLACEMENT", "Đổi máy");
        put("NEW", "Mới tiếp nhận");
        put("IN_PROGRESS", "Đang xử lý");
        put("WAITING_CUSTOMER", "Chờ khách hàng");
        put("RESOLVED", "Đã giải quyết");
        put("CLOSED", "Đã đóng");
        put("NORMAL", "Thông thường");
        put("URGENT", "Khẩn cấp");

        // Vai trò
        put("ADMIN", "Admin");
        put("MANAGER", "Quản lý");
        put("SALES", "Bán hàng");
        put("CUSTOMER_SERVICE", "Chăm sóc khách hàng");
    }

    private static void put(String code, String vi) {
        TO_VI.put(code, vi);
        TO_CODE.put(vi, code);
    }

    public static String vi(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        String translated = TO_VI.get(s);
        return translated == null ? s : translated;
    }

    public static String code(Object labelOrCode) {
        if (labelOrCode == null) return "";
        String s = String.valueOf(labelOrCode);
        String code = TO_CODE.get(s);
        return code == null ? s : code;
    }

    public static String[] labels(String... codes) {
        String[] out = new String[codes.length];
        for (int i = 0; i < codes.length; i++) out[i] = vi(codes[i]);
        return out;
    }
}

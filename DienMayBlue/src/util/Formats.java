package util;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
public final class Formats {
    private Formats(){}
    private static final NumberFormat VND=NumberFormat.getNumberInstance(new Locale("vi","VN"));
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Map<String,String> TO_VI=new LinkedHashMap<>(), TO_CODE=new LinkedHashMap<>();
    static {
        String[][] values={
            {"ACTIVE","Đang hoạt động"},{"INACTIVE","Đã ngừng làm việc"},{"ON_LEAVE","Đang nghỉ phép"},{"LOCKED","Đã khóa"},
            {"DISABLED","Ngừng hoạt động"},{"PENDING","Chờ xử lý"},{"CANCELLED","Đã hủy"},{"COMPLETED","Hoàn tất"},
            {"APPROVED","Đã duyệt"},{"REJECTED","Đã từ chối"},{"WAITING_APPROVAL","Chờ duyệt"},{"OPEN","Chưa chốt"},
            {"FINALIZED","Đã chốt"},{"REVERSED","Đã khấu trừ"},{"ASSIGNED","Đã phân công"},{"SUCCESS","Thành công"},
            {"PAID","Đã thanh toán"},{"PARTIAL_REFUND","Hoàn tiền một phần"},{"REFUNDED","Đã hoàn tiền"},{"CASH","Tiền mặt"},
            {"CARD","Thẻ"},{"BANK_TRANSFER","Chuyển khoản"},{"INSTALLMENT","Trả góp"},{"OUT_OF_STOCK","Hết hàng"},
            {"DISCONTINUED","Ngừng kinh doanh"},{"SCHEDULED","Sắp diễn ra"},{"ENDED","Đã kết thúc"},{"PERCENT","Phần trăm"},
            {"FIXED","Số tiền cố định"},{"PREPARING","Đang chuẩn bị"},{"DELIVERING","Đang giao"},{"DELIVERED","Đã giao"},
            {"INSTALLED","Đã lắp đặt"},{"FAILED","Giao không thành công"},{"INNER_CITY","Nội thành"},{"OTHER_PROVINCE","Tỉnh / thành khác"},
            {"STANDARD","Tiêu chuẩn"},{"SILVER","Bạc"},{"GOLD","Vàng"},{"PLATINUM","Bạch kim"},
            {"EARN","Cộng điểm"},{"REVERSAL","Trừ điểm"},{"INVALIDATED","Đã vô hiệu hóa"},{"UNPAID","Chưa thanh toán"},
            {"PARTIAL","Thanh toán một phần"},{"OVERDUE","Quá hạn"},{"REPAIRING","Đang sửa chữa"},{"RETURNED","Đã trả khách"},
            {"REPLACEMENT","Đổi máy"},{"NEW","Mới tiếp nhận"},{"IN_PROGRESS","Đang xử lý"},{"WAITING_CUSTOMER","Chờ khách hàng"},
            {"RESOLVED","Đã giải quyết"},{"CLOSED","Đã đóng"},{"NORMAL","Thông thường"},{"URGENT","Khẩn cấp"},
            {"ADMIN","Admin"},{"MANAGER","Quản lý"},{"SALES","Bán hàng"},{"CUSTOMER_SERVICE","Chăm sóc khách hàng"}
        };
        for(String[] p:values){TO_VI.put(p[0],p[1]);TO_CODE.put(p[1],p[0]);}
    }
    public static String money(Object value){
        if(value==null)return "0đ";
        BigDecimal n=value instanceof BigDecimal b?b:value instanceof Number x?BigDecimal.valueOf(x.doubleValue()):new BigDecimal(String.valueOf(value));
        return VND.format(n)+"đ";
    }
    public static String date(Object value){
        if(value instanceof java.sql.Date d)return d.toLocalDate().format(DATE);
        if(value instanceof java.sql.Timestamp t)return t.toLocalDateTime().format(DATE_TIME);
        return safe(value);
    }
    public static String safe(Object value){return value==null?"":String.valueOf(value);}
    public static String vi(Object value){String s=safe(value);return TO_VI.getOrDefault(s,s);}
    public static String code(Object value){String s=safe(value);return TO_CODE.getOrDefault(s,s);}
    public static String[] labels(String... codes){String[] out=new String[codes.length];for(int i=0;i<codes.length;i++)out[i]=vi(codes[i]);return out;}
}

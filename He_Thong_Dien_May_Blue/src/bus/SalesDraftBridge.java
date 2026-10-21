package bus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cầu nối dữ liệu tạm thời giữa UC018 (Tìm sản phẩm) và UC019 (Lập hóa đơn).
 * Mục tiêu là giữ code đơn giản: chỉ chuyển danh sách mã sản phẩm + số lượng
 * khi người dùng bấm Thanh toán từ màn tìm sản phẩm hoặc từ Đơn tạm.
 */
public final class SalesDraftBridge {
    private static final LinkedHashMap<String, Integer> pendingInvoiceItems = new LinkedHashMap<String, Integer>();

    private SalesDraftBridge() {}

    public static synchronized void setDraftItems(Map<String, Integer> items) {
        pendingInvoiceItems.clear();
        if (items == null) return;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue().intValue() > 0) {
                pendingInvoiceItems.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static synchronized Map<String, Integer> consumePendingInvoiceItems() {
        LinkedHashMap<String, Integer> copy = new LinkedHashMap<String, Integer>(pendingInvoiceItems);
        pendingInvoiceItems.clear();
        return copy;
    }
}

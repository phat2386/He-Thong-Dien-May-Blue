package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dao.CustomerDAO;
import entity.Account;
import util.Formats;
import util.Ids;

public class CustomerService extends BaseService<CustomerDAO> {

    public CustomerService() {
        super(new CustomerDAO());
    }

    public List<Map<String, Object>> searchCustomers(String keyword, String tierCode)
            throws SQLException {
        return dao.searchCustomers(keyword, tierCode);
    }

    public Map<String, Object> getCustomer(String id) throws SQLException {
        return dao.findCustomer(id);
    }

    public String nextCustomerId() throws SQLException {
        return String.format("KH%03d", dao.maxShortCustomerNumber() + 1);
    }

    public void saveCustomer(
            String id, String name, String phone, String email, String address, boolean create) throws Exception {
        String customerId = id == null ? "" : id.trim().toUpperCase(Locale.ROOT);
        String fullName = name == null ? "" : name.trim();
        String tel = phone == null ? "" : phone.trim();
        String mail = email == null ? "" : email.trim();
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được rỗng.");
        }
        if (!tel.isEmpty() && !tel.matches("[0-9]{9,11}")) {
            throw new IllegalArgumentException("Số điện thoại phải gồm 9-11 chữ số.");
        }
        if (!mail.isEmpty() && (!mail.contains("@") || mail.startsWith("@") || mail.endsWith("@"))) {
            throw new IllegalArgumentException("Thư điện tử không hợp lệ.");
        }
        if (create) {
            if (!customerId.matches("KH\\d{3,}")) {
                throw new IllegalArgumentException("Mã khách hàng phải có dạng KH001, KH002, ...");
            }
            if (dao.customerExists(customerId)) {
                throw new IllegalArgumentException("Mã khách hàng đã tồn tại.");
            }
            Object tierId = dao.standardTierId();
            if (tierId == null) {
                throw new IllegalStateException("Chưa cấu hình hạng thành viên Tiêu chuẩn.");
            }
            dao.insertCustomer( customerId, fullName, emptyToNull(tel), emptyToNull(mail), emptyToNull(address), tierId);
        } else {
            dao.updateCustomer( customerId, fullName, emptyToNull(tel), emptyToNull(mail), emptyToNull(address));
        }
    }

    public List<Map<String, Object>> membershipCustomers(String keyword) throws SQLException {
        return dao.findMembershipCustomers(keyword);
    }

    public List<Map<String, Object>> membershipTiers() throws SQLException {
        return dao.findMembershipTiers();
    }

    public List<Map<String, Object>> warranties() throws SQLException {
        return dao.findWarranties();
    }

    public List<Map<String, Object>> complaints() throws SQLException {
        return dao.findComplaints();
    }

    public List<Map<String, Object>> warrantyOptions(String phone) throws SQLException {
        return dao.findWarrantyOptions(phone);
    }

    public String customerIdByPhone(String phone) throws SQLException {
        List<Map<String, Object>> rows = dao.findCustomerIdsByPhone(phone);
        return rows.size() == 1 ? String.valueOf(rows.get(0).get("customer_id")) : null;
    }

    public String reissueMembershipCard(String customerId) throws Exception {
        Account account = requirePermission("MEMBERSHIP_MANAGE");
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn khách hàng.");
        }
        String normalizedId = customerId.trim();
        return dao.transaction(con -> {
            Map<String, Object> customer = dao.findCustomerStatus(con, normalizedId);
            if (customer == null) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng.");
            }
            if (!"ACTIVE".equals(String.valueOf(customer.get("status")))) {
                throw new IllegalStateException( "Khách hàng đang ngừng hoạt động nên không thể cấp thẻ mới.");
            }
            dao.invalidateActiveMembershipCards(con, normalizedId);
            int nextSequence = 1;
            String prefix = "BLUE-" + normalizedId + "-";
            for (Map<String, Object> row : dao.findMembershipCardCodes(con, normalizedId)) {
                String code = row.get("card_code") == null ? "" : String.valueOf(row.get("card_code")).trim();
                if (!code.startsWith(prefix)) {
                    continue;
                }
                String suffix = code.substring(prefix.length());
                if (!suffix.matches("\\d+")) {
                    continue;
                }
                try {
                    nextSequence = Math.max(nextSequence, Integer.parseInt(suffix) + 1);
                } catch (NumberFormatException ignored) {
                    // Bỏ qua mã thẻ cũ không còn đúng định dạng số.
                }
            }
            String cardCode = String.format("BLUE-%s-%02d", normalizedId, nextSequence);
            dao.insertMembershipCard(con, normalizedId, cardCode);
            dao.insertAuditLog(
                    con, account.getAccountId(), "MEMBERSHIP_CARD_REISSUED", "customers",
                    normalizedId, "Cấp/cấp lại thẻ thành viên: " + cardCode);
            return cardCode;
        });
    }

    public void updateMembershipTierPolicy(
            int tierId, int minPoints, BigDecimal discountRate, BigDecimal pointMultiplier) throws Exception {
        Account account = requirePermission("POINT_MANAGE");
        if (minPoints < 0) {
            throw new IllegalArgumentException("Mốc điểm không được âm.");
        }
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) < 0 || discountRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Mức ưu đãi phải từ 0 đến 100%.");
        }
        if (pointMultiplier == null || pointMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hệ số tích điểm phải lớn hơn 0.");
        }
        dao.transaction(con -> {
            Map<String, Object> tier = dao.findTier(con, tierId);
            if (tier == null) {
                throw new IllegalArgumentException("Không tìm thấy hạng thành viên.");
            }
            String tierCode = String.valueOf(tier.get("tier_code"));
            if ("STANDARD".equals(tierCode) && minPoints != 0) {
                throw new IllegalArgumentException("Hạng Tiêu chuẩn phải bắt đầu từ 0 điểm.");
            }
            if (dao.tierThresholdUsedByAnother(con, tierId, minPoints)) {
                throw new IllegalArgumentException("Mốc điểm này đã được dùng cho hạng khác.");
            }
            Map<String, Integer> proposed = new LinkedHashMap<>();
            for (Map<String, Object> row : dao.findTierThresholds(con)) {
                int id = ((Number) row.get("tier_id")).intValue();
                int threshold = id == tierId ? minPoints : ((Number) row.get("min_points")).intValue();
                proposed.put(String.valueOf(row.get("tier_code")), threshold);
            }
            String[] order = {"STANDARD", "SILVER", "GOLD", "PLATINUM"};
            Integer previous = null;
            for (String code : order) {
                Integer current = proposed.get(code);
                if (current == null) {
                    continue;
                }
                if (previous != null && current <= previous) {
                    throw new IllegalArgumentException( "Mốc điểm phải tăng dần theo thứ tự Tiêu chuẩn → Bạc → Vàng → Bạch kim.");
                }
                previous = current;
            }
            dao.updateTierPolicy(con, tierId, minPoints, discountRate, pointMultiplier);
            dao.recalculateAllCustomerTiers(con);
            dao.insertAuditLog(
                    con, account.getAccountId(), "MEMBERSHIP_POLICY_UPDATED", "membership_tiers",
                    String.valueOf(tierId), "Cập nhật chính sách " + tier.get("tier_name")
                            + ": mốc=" + minPoints + ", ưu đãi=" + discountRate + "%" + ", hệ số điểm=" + pointMultiplier);
            return null;
        });
    }

    public String createWarranty(String customerId, long invoiceDetailId, String issue)
            throws Exception {
        Account account = requireCustomerServicePermission("WARRANTY_MANAGE");
        if (issue == null || issue.trim().isEmpty()) {
            throw new IllegalArgumentException("Mô tả lỗi không được để trống.");
        }
        return dao.transaction(con -> {
            Map<String, Object> purchase = dao.findWarrantyPurchase(con, invoiceDetailId);
            if (purchase == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm đã mua.");
            }
            if (!String.valueOf(purchase.get("customer_id")).equals(customerId)) {
                throw new IllegalArgumentException("Sản phẩm không thuộc khách hàng đã chọn.");
            }
            String invoiceStatus = String.valueOf(purchase.get("status"));
            if (!"PAID".equals(invoiceStatus) && !"PARTIAL_REFUND".equals(invoiceStatus)) {
                throw new IllegalArgumentException( "Hóa đơn không còn đủ điều kiện tiếp nhận bảo hành.");
            }
            int remaining = ((Number) purchase.get("remaining_qty")).intValue();
            if (remaining <= 0) {
                throw new IllegalArgumentException( "Sản phẩm này đã được trả/hoàn toàn bộ, không thể tiếp nhận bảo hành.");
            }
            LocalDate purchaseDate = ((java.sql.Timestamp) purchase.get("created_at")) .toLocalDateTime() .toLocalDate();
            int months = ((Number) purchase.get("warranty_months")).intValue();
            LocalDate warrantyEnd = purchaseDate.plusMonths(months);
            if (warrantyEnd.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException( "Sản phẩm đã hết hạn bảo hành từ " + warrantyEnd + ".");
            }
            if (dao.activeWarrantyCount(con, invoiceDetailId) >= remaining) {
                throw new IllegalArgumentException( "Sản phẩm này đang có phiếu bảo hành chưa kết thúc.");
            }
            String warrantyId = Ids.next("BH");
            dao.insertWarranty(
                    con, warrantyId, customerId, invoiceDetailId,
                    purchase.get("product_id"), issue.trim(), warrantyEnd, account.getEmployee().getEmployeeId());
            dao.insertWarrantyHistory( con, warrantyId, null, "REPAIRING", account.getEmployee().getEmployeeId(), "Tiếp nhận bảo hành");
            dao.insertAuditLog(
                    con, account.getAccountId(), "WARRANTY_CREATED", "warranties",
                    warrantyId, "Tiếp nhận bảo hành cho invoice_detail=" + invoiceDetailId);
            return warrantyId;
        });
    }

    public BigDecimal calculateReplacementCharge(String warrantyId, BigDecimal accessoryFee)
            throws Exception {
        requireCustomerServicePermission("WARRANTY_MANAGE");
        BigDecimal fee = accessoryFee == null ? BigDecimal.ZERO : accessoryFee;
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí phụ kiện không được âm.");
        }
        Map<String, Object> warranty = dao.findWarrantyChargeData(warrantyId);
        if (warranty == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu bảo hành.");
        }
        if (!"REPAIRING".equals(String.valueOf(warranty.get("status")))) {
            throw new IllegalArgumentException( "Chỉ phiếu đang sửa chữa mới được chuyển sang đổi máy.");
        }
        return calculateReplacementCharge(warranty, fee);
    }

    public void updateWarrantyStatus(String id, String newStatus, String note) throws Exception {
        updateWarrantyStatus(id, newStatus, note, BigDecimal.ZERO);
    }

    public void updateWarrantyStatus(
            String id, String newStatus, String note, BigDecimal accessoryFee) throws Exception {
        Account account = requireCustomerServicePermission("WARRANTY_MANAGE");
        String status = newStatus == null ? "" : newStatus.trim();
        BigDecimal fee = accessoryFee == null ? BigDecimal.ZERO : accessoryFee;
        dao.transaction(con -> {
            Map<String, Object> row = dao.findWarrantyStatus(con, id);
            if (row == null) {
                throw new IllegalArgumentException("Không tìm thấy phiếu bảo hành.");
            }
            String oldStatus = String.valueOf(row.get("status"));
            if (oldStatus.equals(status)) {
                throw new IllegalArgumentException("Phiếu đã ở trạng thái này.");
            }
            if (!isValidWarrantyTransition(oldStatus, status)) {
                throw new IllegalArgumentException( "Không thể chuyển bảo hành từ '" + oldStatus + "' sang '" + status + "'.");
            }
            if (("REJECTED".equals(status) || "REPLACEMENT".equals(status)) && (note == null || note.trim().isEmpty())) {
                throw new IllegalArgumentException( "Vui lòng nhập ghi chú/lý do cho trạng thái này.");
            }
            BigDecimal replacementCharge = null;
            if ("REPLACEMENT".equals(status)) {
                if (fee.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Phí phụ kiện không được âm.");
                }
                Map<String, Object> warranty = dao.findWarrantyChargeData(con, id);
                replacementCharge = calculateReplacementCharge(warranty, fee);
            }
            dao.updateWarrantyState(con, id, status, replacementCharge);
            dao.insertWarrantyHistory( con, id, oldStatus, status, account.getEmployee().getEmployeeId(), emptyToNull(note));
            dao.insertAuditLog( con, account.getAccountId(), "WARRANTY_STATUS_CHANGED", "warranties", id, oldStatus + " -> " + status);
            return null;
        });
    }

    private BigDecimal calculateReplacementCharge(
            Map<String, Object> warranty, BigDecimal accessoryFee) {
        LocalDate purchaseDate = ((java.sql.Timestamp) warranty.get("created_at")) .toLocalDateTime() .toLocalDate();
        long months = Math.max(0, ChronoUnit.MONTHS.between(purchaseDate, LocalDate.now()));
        int percent = (int) Math.min(60, months * 10);
        BigDecimal rate = new BigDecimal(percent).divide(new BigDecimal("100"));
        return ((BigDecimal) warranty.get("unit_price")) .multiply(rate) .add(accessoryFee) .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isValidWarrantyTransition(String oldStatus, String newStatus) {
        if ("REPAIRING".equals(oldStatus)) {
            return "COMPLETED".equals(newStatus) || "REPLACEMENT".equals(newStatus) || "REJECTED".equals(newStatus);
        }
        if ("REPLACEMENT".equals(oldStatus)) {
            return "COMPLETED".equals(newStatus);
        }
        if ("COMPLETED".equals(oldStatus)) {
            return "RETURNED".equals(newStatus);
        }
        return false;
    }

    public String createComplaint(String customerId, String phone, String content)
            throws Exception {
        Account account = requireCustomerServicePermission("COMPLAINT_MANAGE");
        String tel = phone == null ? "" : phone.trim();
        String body = content == null ? "" : content.trim();
        if (tel.isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại khách hàng không được để trống.");
        }
        if (!tel.matches("[0-9+]{9,15}")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (body.length() < 5) {
            throw new IllegalArgumentException("Nội dung khiếu nại quá ngắn.");
        }
        String priority = classifyComplaint(body);
        return dao.transaction(con -> {
            String complaintId = Ids.next("KN");
            String employeeId = account.getEmployee().getEmployeeId();
            dao.insertComplaint( con, complaintId, emptyToNull(customerId), tel, body, priority, employeeId);
            dao.insertComplaintHistory( con, complaintId, null, "NEW", employeeId, "Tiếp nhận khiếu nại");
            dao.insertAuditLog(
                    con, account.getAccountId(), "COMPLAINT_CREATED", "complaints",
                    complaintId, "Tiếp nhận khiếu nại - ưu tiên " + priority);
            return complaintId;
        });
    }

    public void refreshComplaintEscalation() throws Exception {
        dao.transaction(con -> {
            List<Map<String, Object>> overdue = dao.findOverdueComplaints(con);
            if (overdue.isEmpty()) {
                return null;
            }
            List<Map<String, Object>> managers = dao.findActiveManagers(con);
            for (Map<String, Object> complaint : overdue) {
                String complaintId = String.valueOf(complaint.get("complaint_id"));
                dao.markComplaintEscalated(con, complaintId);
                for (Map<String, Object> manager : managers) {
                    dao.insertNotification(
                            con, manager.get("employee_id"), "Khiếu nại quá 48 giờ", "Khiếu nại " + complaintId
                                    + " (" + Formats.safe(complaint.get("customer_phone")) + ")" + " chưa được xử lý quá 48 giờ.");
                }
                dao.insertAuditLog( con, null, "COMPLAINT_ESCALATED", "complaints", complaintId, "Tự động leo thang do quá hạn 48 giờ");
            }
            return null;
        });
    }

    private String classifyComplaint(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String[] urgentKeywords = {
                "cháy", "nổ", "điện giật", "nguy hiểm", "thương tích", "khẩn cấp", "rò điện"
        };
        for (String keyword : urgentKeywords) {
            if (normalized.contains(keyword)) {
                return "URGENT";
            }
        }
        return "NORMAL";
    }

    public void updateComplaintStatus(String id, String newStatus, String note) throws Exception {
        Account account = requireCustomerServicePermission("COMPLAINT_MANAGE");
        String status = newStatus == null ? "" : newStatus.trim();
        dao.transaction(con -> {
            Map<String, Object> row = dao.findComplaintStatus(con, id);
            if (row == null) {
                throw new IllegalArgumentException("Không tìm thấy khiếu nại.");
            }
            String oldStatus = String.valueOf(row.get("status"));
            if (oldStatus.equals(status)) {
                throw new IllegalArgumentException("Khiếu nại đã ở trạng thái này.");
            }
            if (!isValidComplaintTransition(oldStatus, status)) {
                throw new IllegalArgumentException( "Không thể chuyển khiếu nại từ '" + oldStatus + "' sang '" + status + "'.");
            }
            if (("RESOLVED".equals(status) || "CLOSED".equals(status)) && (note == null || note.trim().isEmpty())) {
                throw new IllegalArgumentException( "Vui lòng ghi rõ kết quả xử lý trước khi hoàn tất/đóng khiếu nại.");
            }
            String employeeId = account.getEmployee().getEmployeeId();
            dao.updateComplaintStatus(con, id, status, employeeId);
            dao.insertComplaintHistory( con, id, oldStatus, status, employeeId, emptyToNull(note));
            dao.insertAuditLog( con, account.getAccountId(), "COMPLAINT_STATUS_CHANGED", "complaints", id, oldStatus + " -> " + status);
            return null;
        });
    }

    private boolean isValidComplaintTransition(String oldStatus, String newStatus) {
        if ("NEW".equals(oldStatus)) {
            return "IN_PROGRESS".equals(newStatus);
        }
        if ("IN_PROGRESS".equals(oldStatus)) {
            return "WAITING_CUSTOMER".equals(newStatus) || "RESOLVED".equals(newStatus);
        }
        if ("WAITING_CUSTOMER".equals(oldStatus)) {
            return "IN_PROGRESS".equals(newStatus) || "RESOLVED".equals(newStatus);
        }
        if ("RESOLVED".equals(oldStatus)) {
            return "CLOSED".equals(newStatus);
        }
        return false;
    }
}

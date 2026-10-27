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
            dao.insertCustomer(customerId, fullName, emptyToNull(tel), emptyToNull(mail), emptyToNull(address), tierId);
        } else {

            dao.updateCustomer(customerId, fullName, emptyToNull(tel), emptyToNull(mail), emptyToNull(address));
        }
    }
}
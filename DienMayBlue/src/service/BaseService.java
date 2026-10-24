package service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import dao.BaseDAO;
import database.Db;
import entity.Account;
import util.SessionManager;

/**
 * Phần dùng chung giữa các service.
 * Giữ API truy vấn hiện tại để các màn hình cũ không bị phá vỡ.
 */
public abstract class BaseService<D extends BaseDAO> {

    protected final D dao;

    protected BaseService(D dao) {
        this.dao = dao;
    }

    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        return dao.query(sql, params);
    }

    public List<Map<String, Object>> query(Connection connection, String sql, Object... params)
            throws SQLException {
        return dao.query(connection, sql, params);
    }

    public Map<String, Object> one(String sql, Object... params) throws SQLException {
        return dao.one(sql, params);
    }

    public Map<String, Object> one(Connection connection, String sql, Object... params)
            throws SQLException {
        return dao.one(connection, sql, params);
    }

    public Object scalar(String sql, Object... params) throws SQLException {
        return dao.scalar(sql, params);
    }

    public Object scalar(Connection connection, String sql, Object... params) throws SQLException {
        return dao.scalar(connection, sql, params);
    }

    public int update(String sql, Object... params) throws SQLException {
        return dao.update(sql, params);
    }

    public int update(Connection connection, String sql, Object... params) throws SQLException {
        return dao.update(connection, sql, params);
    }

    public long insertAndKey(Connection connection, String sql, Object... params) throws SQLException {
        return dao.insertAndKey(connection, sql, params);
    }

    public <T> T transaction(Db.TransactionWork<T> work) throws Exception {
        return dao.transaction(work);
    }

    protected Account requirePermission(String permission) {
        Account account = SessionManager.getCurrentAccount();
        if (account == null || account.getEmployee() == null || account.getRole() == null) {
            throw new IllegalStateException("Phiên đăng nhập không hợp lệ.");
        }
        if (!SessionManager.hasPermission(permission)) {
            throw new IllegalStateException("Tài khoản chưa được cấp quyền thực hiện chức năng này.");
        }
        return account;
    }

    protected Account requireManagerPermission(String permission) {
        return requirePermission(permission);
    }

    protected Account requireCustomerServicePermission(String permission) {
        return requirePermission(permission);
    }

    protected static Object emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

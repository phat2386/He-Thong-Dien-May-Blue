package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import database.Db;

/**
 * Chứa các thao tác JDBC dùng chung cho các DAO.
 * Các DAO nghiệp vụ kế thừa lớp này thay vì lặp lại cùng một nhóm wrapper.
 */
public abstract class BaseDAO {

    public <T> T transaction(Db.TransactionWork<T> work) throws Exception {
        return Db.transaction(work);
    }

    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        return Db.query(sql, params);
    }

    public List<Map<String, Object>> query(Connection connection, String sql, Object... params)
            throws SQLException {
        return Db.query(connection, sql, params);
    }

    public Map<String, Object> one(String sql, Object... params) throws SQLException {
        return Db.one(sql, params);
    }

    public Map<String, Object> one(Connection connection, String sql, Object... params)
            throws SQLException {
        return Db.one(connection, sql, params);
    }

    public Object scalar(String sql, Object... params) throws SQLException {
        return Db.scalar(sql, params);
    }

    public Object scalar(Connection connection, String sql, Object... params) throws SQLException {
        return Db.scalar(connection, sql, params);
    }

    public int update(String sql, Object... params) throws SQLException {
        return Db.update(sql, params);
    }

    public int update(Connection connection, String sql, Object... params) throws SQLException {
        return Db.update(connection, sql, params);
    }

    public long insertAndKey(String sql, Object... params) throws SQLException {
        return Db.insertAndKey(sql, params);
    }

    public long insertAndKey(Connection connection, String sql, Object... params) throws SQLException {
        return Db.insertAndKey(connection, sql, params);
    }
}

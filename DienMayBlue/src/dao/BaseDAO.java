package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import connectDB.ConnectDB;

/**
 * Lớp cơ sở của tầng DAO.
 * Mọi thao tác JDBC dùng chung nằm ở tầng DAO; ConnectDB chỉ mở kết nối.
 * Các DAO nghiệp vụ chứa câu SQL và gọi các hàm hỗ trợ này khi cần.
 */
public abstract class BaseDAO {

    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(Connection connection) throws Exception;
    }

    public <T> T transaction(TransactionWork<T> work) throws Exception {
        try (Connection connection = ConnectDB.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (Exception ex) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
                throw ex;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        try (Connection connection = ConnectDB.getConnection()) {
            return query(connection, sql, params);
        }
    }

    public List<Map<String, Object>> query(Connection connection, String sql, Object... params)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    public Map<String, Object> one(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> rows = query(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> one(Connection connection, String sql, Object... params)
            throws SQLException {
        List<Map<String, Object>> rows = query(connection, sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Object scalar(String sql, Object... params) throws SQLException {
        try (Connection connection = ConnectDB.getConnection()) {
            return scalar(connection, sql, params);
        }
    }

    public Object scalar(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getObject(1) : null;
            }
        }
    }

    public int update(String sql, Object... params) throws SQLException {
        try (Connection connection = ConnectDB.getConnection()) {
            return update(connection, sql, params);
        }
    }

    public int update(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        }
    }

    public long insertAndKey(String sql, Object... params) throws SQLException {
        try (Connection connection = ConnectDB.getConnection()) {
            return insertAndKey(connection, sql, params);
        }
    }

    public long insertAndKey(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được khóa tự tăng.");
    }

    private void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof java.time.LocalDate date) {
                statement.setDate(i + 1, java.sql.Date.valueOf(date));
            } else if (value instanceof java.time.LocalDateTime dateTime) {
                statement.setTimestamp(i + 1, java.sql.Timestamp.valueOf(dateTime));
            } else if (value instanceof java.time.LocalTime time) {
                statement.setTime(i + 1, java.sql.Time.valueOf(time));
            } else {
                statement.setObject(i + 1, value);
            }
        }
    }
}

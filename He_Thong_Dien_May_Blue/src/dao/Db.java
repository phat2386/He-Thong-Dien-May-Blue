package dao;

import connectDB.ConnectDB;

import java.sql.*;
import java.util.*;

public final class Db {
    private Db() {}

    public interface TransactionWork<T> {
        T execute(Connection con) throws Exception;
    }

    public static <T> T transaction(TransactionWork<T> work) throws Exception {
        try (Connection con = ConnectDB.getConnection()) {
            con.setAutoCommit(false);
            try {
                T result = work.execute(con);
                con.commit();
                return result;
            } catch (Exception ex) {
                try { con.rollback(); } catch (SQLException ignored) {}
                throw ex;
            }
        }
    }

    public static List<Map<String,Object>> query(String sql, Object... params) throws SQLException {
        try (Connection con = ConnectDB.getConnection()) {
            return query(con, sql, params);
        }
    }

    public static List<Map<String,Object>> query(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();
                ResultSetMetaData md = rs.getMetaData();
                int count = md.getColumnCount();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<String,Object>();
                    for (int i=1;i<=count;i++) {
                        row.put(md.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    public static Map<String,Object> one(String sql, Object... params) throws SQLException {
        List<Map<String,Object>> rows = query(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static Map<String,Object> one(Connection con, String sql, Object... params) throws SQLException {
        List<Map<String,Object>> rows = query(con, sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static Object scalar(String sql, Object... params) throws SQLException {
        try (Connection con = ConnectDB.getConnection()) {
            return scalar(con, sql, params);
        }
    }

    public static Object scalar(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    public static int update(String sql, Object... params) throws SQLException {
        try (Connection con = ConnectDB.getConnection()) {
            return update(con, sql, params);
        }
    }

    public static int update(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    public static long insertAndKey(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Không lấy được khóa tự tăng.");
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i=0;i<params.length;i++) {
            Object p = params[i];
            if (p instanceof java.time.LocalDate) {
                ps.setDate(i+1, java.sql.Date.valueOf((java.time.LocalDate)p));
            } else if (p instanceof java.time.LocalDateTime) {
                ps.setTimestamp(i+1, java.sql.Timestamp.valueOf((java.time.LocalDateTime)p));
            } else if (p instanceof java.time.LocalTime) {
                ps.setTime(i+1, java.sql.Time.valueOf((java.time.LocalTime)p));
            } else {
                ps.setObject(i+1, p);
            }
        }
    }
}

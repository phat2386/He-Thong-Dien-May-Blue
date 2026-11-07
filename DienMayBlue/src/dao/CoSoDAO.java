package dao;
import java.sql.*;
import java.util.*;
import connectDB.KetNoiCSDL;
import java.sql.Date;
public abstract class CoSoDAO {
    @FunctionalInterface public interface CongViecGiaoDich<T> {
        T execute(Connection connection) throws Exception;
    }
    public<T> T giaoDich(CongViecGiaoDich<T> congViec) throws Exception {
        try (Connection connection = KetNoiCSDL.layKetNoi()) {
            connection.setAutoCommit(false);
            try {
                T ketQua = congViec.execute(connection);
                connection.commit();
                return ketQua;
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
    public List<Map<String, Object>> truyVan(String cauLenhSql, Object ...thamSo) throws SQLException {
        try (Connection connection = KetNoiCSDL.layKetNoi()) {
            return truyVan(connection, cauLenhSql, thamSo);
        }
    }
    public List<Map<String, Object>> truyVan(Connection connection, String cauLenhSql, Object ...thamSo) throws SQLException {
        try (PreparedStatement cauLenh = connection.prepareStatement(cauLenhSql)) {
            ganThamSo(cauLenh, thamSo);
            try (ResultSet tapKetQua = cauLenh.executeQuery()) {
                List<Map<String, Object>> cacDong = new ArrayList<>();
                ResultSetMetaData thongTinCot = tapKetQua.getMetaData();
                int soCot = thongTinCot.getColumnCount();
                while (tapKetQua.next()) {
                    Map<String, Object> dong = new LinkedHashMap<>();
                    for (int i = 1; i <= soCot; i++) {
                        dong.put(thongTinCot.getColumnLabel(i), tapKetQua.getObject(i));
                    }
                    cacDong.add(dong);
                }
                return cacDong;
            }
        }
    }
    public Map<String, Object> motDong(String cauLenhSql, Object ...thamSo) throws SQLException {
        List<Map<String, Object>> cacDong = truyVan(cauLenhSql, thamSo);
        return cacDong.isEmpty() ? null : cacDong.get(0);
    }
    public Map<String, Object> motDong(Connection connection, String cauLenhSql, Object ...thamSo) throws SQLException {
        List<Map<String, Object>> cacDong = truyVan(connection, cauLenhSql, thamSo);
        return cacDong.isEmpty() ? null : cacDong.get(0);
    }
    public Object motGiaTri(String cauLenhSql, Object ...thamSo) throws SQLException {
        try (Connection connection = KetNoiCSDL.layKetNoi()) {
            return motGiaTri(connection, cauLenhSql, thamSo);
        }
    }
    public Object motGiaTri(Connection connection, String cauLenhSql, Object ...thamSo) throws SQLException {
        try (PreparedStatement cauLenh = connection.prepareStatement(cauLenhSql)) {
            ganThamSo(cauLenh, thamSo);
            try (ResultSet tapKetQua = cauLenh.executeQuery()) {
                return tapKetQua.next() ? tapKetQua.getObject(1) : null;
            }
        }
    }
    public int capNhat(String cauLenhSql, Object ...thamSo) throws SQLException {
        try (Connection connection = KetNoiCSDL.layKetNoi()) {
            return capNhat(connection, cauLenhSql, thamSo);
        }
    }
    public int capNhat(Connection connection, String cauLenhSql, Object ...thamSo) throws SQLException {
        try (PreparedStatement cauLenh = connection.prepareStatement(cauLenhSql)) {
            ganThamSo(cauLenh, thamSo);
            return cauLenh.executeUpdate();
        }
    }
    protected String goiYMaTiepTheo(String tenBang, String tenCot, String tienTo, int soChuSo) throws SQLException {
        int viTriSo = tienTo.length() + 1;
        String sql = "SELECT ISNULL(MAX(TRY_CONVERT(int,SUBSTRING(" + tenCot + "," + viTriSo + "," + soChuSo + "))),0) " + "FROM dbo." + tenBang + " WHERE " + tenCot + " LIKE ?";
        Object v = motGiaTri(sql, tienTo + "%");
        int lonNhat = v == null ? 0 : ((Number) v).intValue();
        return tienTo + String.format("%0" + soChuSo + "d", lonNhat + 1);
    }
    public long themVaLayKhoa(String cauLenhSql, Object ...thamSo) throws SQLException {
        try (Connection connection = KetNoiCSDL.layKetNoi()) {
            return themVaLayKhoa(connection, cauLenhSql, thamSo);
        }
    }
    public long themVaLayKhoa(Connection connection, String cauLenhSql, Object ...thamSo) throws SQLException {
        try (PreparedStatement cauLenh = connection.prepareStatement(cauLenhSql, Statement.RETURN_GENERATED_KEYS)) {
            ganThamSo(cauLenh, thamSo);
            cauLenh.executeUpdate();
            try (ResultSet tapKetQua = cauLenh.getGeneratedKeys()) {
                if (tapKetQua.next()) {
                    return tapKetQua.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được khóa tự tăng.");
    }
    private void ganThamSo(PreparedStatement cauLenh, Object ...thamSo) throws SQLException {
        for (int i = 0; i < thamSo.length; i++) {
            Object giaTri = thamSo[i];
            if (giaTri instanceof java.time.LocalDate ngay) {
                cauLenh.setDate(i + 1, java.sql.Date.valueOf(ngay));
            } else if (giaTri instanceof java.time.LocalDateTime ngayGio) {
                cauLenh.setTimestamp(i + 1, java.sql.Timestamp.valueOf(ngayGio));
            } else if (giaTri instanceof java.time.LocalTime time) {
                cauLenh.setTime(i + 1, java.sql.Time.valueOf(time));
            } else {
                cauLenh.setObject(i + 1, giaTri);
            }
        }
    }
}

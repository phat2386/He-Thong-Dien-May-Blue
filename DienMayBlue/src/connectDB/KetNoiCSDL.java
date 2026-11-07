package connectDB;
import java.sql.*;
public final class KetNoiCSDL {
    private static final String URL_MAC_DINH = "jdbc:sqlserver://localhost:1433;instanceName=SQLEXPRESS;databaseName=DienMayBlue;encrypt=true;trustServerCertificate=true";
    private static final String NGUOI_DUNG_MAC_DINH = "sa";
    private static final String MAT_KHAU_MAC_DINH = "123";
    private KetNoiCSDL() {
    }
    public static String layUrl() {
        return layCauHinh("DIENMAYBLUE_DB_URL", URL_MAC_DINH);
    }
    public static String layNguoiDung() {
        return layCauHinh("DIENMAYBLUE_DB_USER", NGUOI_DUNG_MAC_DINH);
    }
    public static String layMatKhau() {
        return layCauHinh("DIENMAYBLUE_DB_PASSWORD", MAT_KHAU_MAC_DINH);
    }
    private static String layCauHinh(String tenBienMoiTruong, String giaTriMacDinh) {
        String giaTri = System.getenv(tenBienMoiTruong);
        return giaTri == null || giaTri.trim().isEmpty() ? giaTriMacDinh : giaTri.trim();
    }
    public static Connection layKetNoi() throws SQLException {
        return DriverManager.getConnection(layUrl(), layNguoiDung(), layMatKhau());
    }
    public static void main(String[] args) {
        try (Connection ketNoi = layKetNoi()) {
            System.out.println("Kết nối cơ sở dữ liệu thành công.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}

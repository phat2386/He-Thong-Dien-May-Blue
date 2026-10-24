package database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public final class DatabaseConnection {
    private DatabaseConnection() {}
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Không tìm thấy Microsoft SQL Server JDBC Driver.");
        }
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.URL);
    }
}

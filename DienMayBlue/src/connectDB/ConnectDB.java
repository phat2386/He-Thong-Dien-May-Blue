package connectDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConnectDB {
    private ConnectDB() {}

    public static final String URL =
            "jdbc:sqlserver://localhost:1433;"
          + "databaseName=DienMayBlue;encrypt=true;trustServerCertificate=true";
    public static final String USER = "sa";
    public static final String PASS = "123";
    
    public static Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void main(String[] args)
    {
        try (Connection con = getConnection())
        {
            System.out.println("Kết nối thành công");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}

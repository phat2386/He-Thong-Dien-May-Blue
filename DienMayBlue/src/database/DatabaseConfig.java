package cauhinhConfig;

public final class DatabaseConfig {
    private DatabaseConfig() {}

    public static final String URL =
            "jdbc:sqlserver://localhost;"
          + "instanceName=SQLEXPRESS;"
          + "databaseName=DienMayBlue;"
          + "encrypt=true;"
          + "trustServerCertificate=true;"
          + "integratedSecurity=true;";
}

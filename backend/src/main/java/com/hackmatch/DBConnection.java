package com.hackmatch;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Reads local database settings from environment variables and opens JDBC connections. */
public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mariadb://localhost:3306/hackmatch?sslMode=disable";

    private DBConnection() {
        // Utility class: do not create objects of this class.
    }

    public static Connection getConnection() throws SQLException {
        String url = getEnvironment("HM_DB_URL", DEFAULT_URL);
        String user = System.getenv("HM_DB_USER");
        String password = System.getenv("HM_DB_PASSWORD");

        if (user == null || user.isBlank() || password == null || password.isBlank()) {
            throw new SQLException("Set HM_DB_USER and HM_DB_PASSWORD before starting HackMatch.");
        }

        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("MariaDB JDBC driver is missing. Run Maven package first.", exception);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static String getEnvironment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}

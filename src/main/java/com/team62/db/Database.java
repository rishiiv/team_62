package com.team62.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple helper for obtaining JDBC connections to the shared Postgres database.
 *
 * This uses the credentials provided for the CSCE 315 class database.
 * Make sure the PostgreSQL JDBC driver (org.postgresql.Driver) is on the classpath.
 */
public class Database {

    // Connection details for the team_62_db instance
    private static final String URL =
            "jdbc:postgresql://csce-315-db.engr.tamu.edu:5432/team_62_db";
    private static final String USER = "team_62";
    private static final String PASSWORD = "abgmrr";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found on classpath", e);
        }
    }

    /**
     * Get a new JDBC connection. Caller is responsible for closing it.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}


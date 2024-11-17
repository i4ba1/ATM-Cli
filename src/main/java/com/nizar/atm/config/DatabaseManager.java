// src/main/java/com/yourcompany/atm/config/DatabaseManager.java
package com.nizar.atm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private final Connection connection;
    private final Properties dbProperties;

    private static final String DUCKDB_DRIVER = "org.duckdb.DuckDB";

    private DatabaseManager() {
        try {
            this.dbProperties = loadDatabaseProperties();
            Class.forName(DUCKDB_DRIVER);
            this.connection = createConnection();
            initializeTables();
        } catch (SQLException | IOException | ClassNotFoundException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                throw new SQLException("Database connection is not available");
            }
            return connection;
        } catch (SQLException e) {
            logger.error("Error checking database connection", e);
            throw new RuntimeException("Failed to get database connection", e);
        }
    }

    private Connection createConnection() throws SQLException {
        Properties connectionProps = new Properties();
        connectionProps.setProperty("user", dbProperties.getProperty("db.user", ""));
        connectionProps.setProperty("password", dbProperties.getProperty("db.password", ""));

        String dbPath = dbProperties.getProperty("db.path", "atm.db");
        String jdbcUrl = "jdbc:duckdb:" + dbPath;

        try {
            return DriverManager.getConnection(jdbcUrl, connectionProps);
        } catch (SQLException e) {
            logger.error("Failed to create database connection", e);
            throw new SQLException("Could not create database connection", e);
        }
    }

    private void initializeTables() throws SQLException {
        String[] schemaFiles = {
                "/db/schema/V1__create_tables.sql",
                "/db/schema/V2__create_indices.sql"
        };

        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);

            for (String schemaFile : schemaFiles) {
                executeSchemaFile(conn, schemaFile);
            }

            conn.commit();
            logger.info("Database schema initialized successfully");

        } catch (SQLException | IOException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                logger.error("Failed to rollback schema initialization", ex);
            }
            logger.error("Failed to initialize database schema", e);
            throw new SQLException("Schema initialization failed", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Failed to reset auto-commit", e);
            }
        }
    }

    private void executeSchemaFile(Connection conn, String resourcePath) throws SQLException, IOException {
        try (Statement stmt = conn.createStatement()) {
            String schema = loadResourceFile(resourcePath);
            for (String sql : schema.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
            logger.debug("Executed schema file: {}", resourcePath);
        }
    }

    private String loadResourceFile(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            logger.error("Failed to load resource file: {}", resourcePath, e);
            throw new IOException("Could not load resource file: " + resourcePath, e);
        }
    }

    private Properties loadDatabaseProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/db/database.properties")) {
            if (is == null) {
                logger.warn("database.properties not found, using defaults");
                props.setProperty("db.path", "atm.db");
                props.setProperty("db.user", "");
                props.setProperty("db.password", "");
                props.setProperty("db.env", "prod");
                return props;
            }
            props.load(is);
            return props;
        } catch (IOException e) {
            logger.error("Failed to load database properties", e);
            throw new IOException("Could not load database properties", e);
        }
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }

    // For testing purposes
    protected void clearDatabase() throws SQLException {
        if (!dbProperties.getProperty("db.env", "prod").equals("test")) {
            throw new IllegalStateException("Cannot clear database in non-test environment");
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP SCHEMA public CASCADE IF EXISTS;");
            stmt.execute("CREATE SCHEMA public;");
            initializeTables();
        }
    }

    // For testing and monitoring purposes
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // Get database path - useful for backup/restore operations
    public String getDatabasePath() {
        return dbProperties.getProperty("db.path", "atm.db");
    }

    // Get database environment
    public String getEnvironment() {
        return dbProperties.getProperty("db.env", "prod");
    }
}
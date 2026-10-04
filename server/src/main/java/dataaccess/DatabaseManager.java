package dataaccess;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static final String DATABASE_NAME;
    private static final String USER;
    private static final String PASSWORD;
    private static final String CONNECTION_URL;

    public static void configureDatabase() {
        try {
            createDatabase();
            createTables();
        } catch (DataAccessException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void createTables() throws DataAccessException {
        createUserDataTable();
        createAuthDataTable();
        createGameDataTable();
        addColumnIfMissing("user_data", "elo", "INT NOT NULL DEFAULT 1500");
        addColumnIfMissing("game_data", "stockfish", "TEXT NULL");
        addColumnIfMissing("game_data", "rated", "BOOLEAN NOT NULL DEFAULT FALSE");
    }

    private static void createUserDataTable() throws DataAccessException {
        String createSQLTable = """
                CREATE TABLE IF NOT EXISTS user_data (
                username VARCHAR(255) PRIMARY KEY NOT NULL,
                passwordHash VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL
                );
                """;
        tryUpdateDatabase(createSQLTable);
    }

    private static void createAuthDataTable() throws DataAccessException {
        String createSQLTable = """
                CREATE TABLE IF NOT EXISTS auth_data (
                authToken VARCHAR(255) PRIMARY KEY NOT NULL,
                username VARCHAR(255) NOT NULL
                );
                """;
        tryUpdateDatabase(createSQLTable);
    }

    private static void createGameDataTable() throws DataAccessException {
        String createSQLTable = """
                CREATE TABLE IF NOT EXISTS game_data (
                gameID INT PRIMARY KEY NOT NULL,
                whiteUsername VARCHAR(255),
                blackUsername VARCHAR(255),
                gameName VARCHAR(255) NOT NULL,
                game longtext NOT NULL
                );
                """;
        tryUpdateDatabase(createSQLTable);
    }

    private static synchronized void addColumnIfMissing(String table, String column, String definition) throws DataAccessException {
        try (var conn = getConnection();
             var query = conn.prepareStatement("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ? AND column_name = ?")) {
            query.setString(1, DATABASE_NAME);
            query.setString(2, table);
            query.setString(3, column);
            try (var result = query.executeQuery()) {
                result.next();
                if (result.getInt(1) == 0) {
                    try (var alter = conn.createStatement()) {
                        alter.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    }
                }
            }
        } catch (SQLException e) { throw new DataAccessException(e.getMessage()); }
    }

    private static void tryUpdateDatabase(String statement) throws DataAccessException {
        try (var conn = getConnection()) {
            var preparedStatement = conn.prepareStatement(statement);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /*
     * Load the database information for the db.properties file.
     */
    static {
        try {
            try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
                if (propStream == null) {
                    throw new Exception("Unable to load db.properties");
                }
                Properties props = new Properties();
                props.load(propStream);
                DATABASE_NAME = props.getProperty("db.name");
                USER = props.getProperty("db.user");
                PASSWORD = props.getProperty("db.password");

                var host = props.getProperty("db.host");
                var port = Integer.parseInt(props.getProperty("db.port"));
                CONNECTION_URL = String.format("jdbc:mysql://%s:%d", host, port);
            }
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties. " + ex.getMessage());
        }
    }

    /**
     * Creates the database if it does not already exist.
     */
    static void createDatabase() throws DataAccessException {
        try (var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD)) {
            var statement = "CREATE DATABASE IF NOT EXISTS `" + DATABASE_NAME.replace("`", "``") + "`";
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * Create a connection to the database and sets the catalog based upon the
     * properties specified in db.properties. Connections to the database should
     * be short-lived, and you must close the connection when you are done with it.
     * The easiest way to do that is with a try-with-resource block.
     * <br/>
     * <code>
     * try (var conn = DbInfo.getConnection(databaseName)) {
     * // execute SQL statements.
     * }
     * </code>
     */
    public static Connection getConnection() throws DataAccessException {
        try {
            var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD);
            conn.setCatalog(DATABASE_NAME);
            return conn;
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }
}

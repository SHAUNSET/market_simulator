package model;

import java.io.File;
import java.sql.*;

public class DBHelper {

    // Database directory + file
    private static final String DB_FOLDER = "database";
    private static final String DB_PATH = DB_FOLDER + File.separator + "market_simulator.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    // Load driver + ensure folder exists + create table
    static {
        try {
            // 1) Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // 2) Create database folder if missing
            File folder = new File(DB_FOLDER);
            if (!folder.exists()) folder.mkdir();

            // 3) Create DB and users table if not present
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {

                String sql = "CREATE TABLE IF NOT EXISTS users ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "username TEXT UNIQUE NOT NULL,"
                        + "password TEXT NOT NULL,"
                        + "balance REAL NOT NULL DEFAULT 100000"
                        + ");";

                stmt.execute(sql);
            }

            System.out.println("SQLite DB initialized at: " + DB_PATH);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------- CRUD FUNCTIONS ----------------------

    // Check if user exists
    public static boolean userExists(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    // Insert new user
    public static void insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, balance) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setDouble(3, user.getBalance());
            pstmt.executeUpdate();
        }
    }

    // Authenticate user
    public static User authenticateUser(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getDouble("balance")
                );
            }
            return null;
        }
    }

    // Update user balance
    public static void updateUserBalance(User user) throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, user.getBalance());
            pstmt.setInt(2, user.getId());
            pstmt.executeUpdate();
        }
    }
}

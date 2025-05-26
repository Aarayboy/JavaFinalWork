package Game.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chinese_chess_users.db"; // SQLite数据库文件
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    public DatabaseManager() {
        createNewTable();
    }

    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "无法连接到SQLite数据库: " + e.getMessage(), e);
        }
        return conn;
    }

    public void createNewTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (\n"
                + " id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " username text NOT NULL UNIQUE,\n"
                + " password_hash text NOT NULL\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            if (conn != null) {
                stmt.execute(sql);
                LOGGER.info("用户表已检查/创建。");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "创建用户表失败: " + e.getMessage(), e);
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SHA-256算法不可用", e);
            throw new RuntimeException("SHA-256 Hashing error", e); // 或者返回null并处理
        }
    }

    public synchronized boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            LOGGER.warning("注册尝试：用户名或密码为空。");
            return false;
        }
        // 检查用户名是否已存在
        String checkUserSql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt_check = conn.prepareStatement(checkUserSql)) {
            if (conn == null) return false;
            pstmt_check.setString(1, username);
            ResultSet rs = pstmt_check.executeQuery();
            if (rs.next()) {
                LOGGER.info("用户 '" + username + "' 注册失败：用户名已存在。");
                return false; // 用户名已存在
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "检查用户是否存在时出错: " + e.getMessage(), e);
            return false;
        }

        // 用户名不存在，继续注册
        String sql = "INSERT INTO users(username, password_hash) VALUES(?,?)";
        String hashedPassword = hashPassword(password);

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.executeUpdate();
            LOGGER.info("用户 '" + username + "' 注册成功。");
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "注册用户 '" + username + "' 失败: " + e.getMessage(), e);
            return false;
        }
    }

    public synchronized boolean authenticateUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        String hashedPassword = hashPassword(password);

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (hashedPassword.equals(storedHash)) {
                    LOGGER.info("用户 '" + username + "' 认证成功。");
                    return true;
                } else {
                    LOGGER.info("用户 '" + username + "' 认证失败：密码错误。");
                    return false;
                }
            } else {
                LOGGER.info("用户 '" + username + "' 认证失败：用户不存在。");
                return false; // 用户不存在
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "认证用户 '" + username + "' 时出错: " + e.getMessage(), e);
            return false;
        }
    }
}
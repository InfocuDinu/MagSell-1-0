package com.magsell.services;

import com.magsell.database.DatabaseService;
import com.magsell.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final DatabaseService db = DatabaseService.getInstance();
    private final SecureRandom random = new SecureRandom();

    public void ensureDefaultAdmin() throws SQLException {
        if (getUserByUsername("admin") == null) {
            createUser("admin", "1234", "admin");
            logger.info("Created default admin user 'admin'");
        }
    }

    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setSalt(rs.getString("salt"));
                    u.setRole(rs.getString("role"));
                    return u;
                }
            }
        }
        return null;
    }

    public boolean authenticate(String username, String password) throws SQLException {
        User u = getUserByUsername(username);
        if (u == null) return false;
        String hash = hashPassword(password, u.getSalt());
        return hash.equals(u.getPasswordHash());
    }

    public void createUser(String username, String password, String role) throws SQLException {
        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        String sql = "INSERT INTO users (username, password_hash, salt, role) VALUES (?, ?, ?, ?)";
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, role);
            ps.executeUpdate();
            logger.info("Created user: " + username + " role=" + role);
        }
    }

    private String generateSalt() {
        byte[] s = new byte[16];
        random.nextBytes(s);
        return Base64.getEncoder().encodeToString(s);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(salt));
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

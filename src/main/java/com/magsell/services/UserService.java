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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final DatabaseService db = DatabaseService.getInstance();
    private final SecureRandom random = new SecureRandom();

    public void ensureDefaultAdmin() throws SQLException {
        User admin = getUserByUsername("admin");
        if (admin == null) {
            createUser("admin", "1234", "admin");
            logger.info("Created default admin user 'admin'");
        } else {
            // Verificăm dacă parola e corectă, dacă nu, o resetăm
            if (!authenticate("admin", "1234")) {
                logger.info("Resetting admin password to default");
                updateAdminPassword("admin", "1234");
            }
        }
    }

    /**
     * Actualizează parola utilizatorului admin
     */
    private void updateAdminPassword(String username, String newPassword) throws SQLException {
        String salt = generateSalt();
        String hash = hashPassword(newPassword, salt);
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE username = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setString(3, username);
            ps.executeUpdate();
            logger.info("Updated password for user: " + username);
        }
    }

    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = db.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
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
            // Decodăm salt-ul din Base64 în bytes
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            md.update(saltBytes);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Mapează un rând din ResultSet la un obiect User
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setRole(rs.getString("role"));
        
        // Permisiuni granulare - folosim direct JavaFX Properties
        try {
            Object value = rs.getObject("can_manage_products");
            boolean canManage = value != null && Boolean.TRUE.equals(value);
            u.setCanManageProducts(canManage || u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_manage_products = {}", u.getUsername(), u.canManageProducts());
        } catch (SQLException e) {
            u.setCanManageProducts(u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_manage_products = {}", u.getUsername(), u.canManageProducts());
        }
        
        try {
            Object value = rs.getObject("can_manage_categories");
            boolean canManage = value != null && Boolean.TRUE.equals(value);
            u.setCanManageCategories(canManage || u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_manage_categories = {}", u.getUsername(), u.canManageCategories());
        } catch (SQLException e) {
            u.setCanManageCategories(u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_manage_categories = {}", u.getUsername(), u.canManageCategories());
        }
        
        try {
            Object value = rs.getObject("can_manage_users");
            boolean canManage = value != null && Boolean.TRUE.equals(value);
            u.setCanManageUsers(canManage || u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_manage_users = {}", u.getUsername(), u.canManageUsers());
        } catch (SQLException e) {
            u.setCanManageUsers(u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_manage_users = {}", u.getUsername(), u.canManageUsers());
        }
        
        try {
            Object value = rs.getObject("can_view_reports");
            boolean canView = value != null && Boolean.TRUE.equals(value);
            u.setCanViewReports(canView || u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_view_reports = {}", u.getUsername(), u.canViewReports());
        } catch (SQLException e) {
            u.setCanViewReports(u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_view_reports = {}", u.getUsername(), u.canViewReports());
        }
        
        try {
            Object value = rs.getObject("can_manage_sales");
            boolean canManage = value != null && Boolean.TRUE.equals(value);
            u.setCanManageSales(canManage || u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_manage_sales = {}", u.getUsername(), u.canManageSales());
        } catch (SQLException e) {
            u.setCanManageSales(u.isAdmin()); // Adminii au mereu true
            logger.debug("User {}: can_manage_sales = {}", u.getUsername(), u.canManageSales());
        }
        
        return u;
    }

    /**
     * Obține toți utilizatorii
     */
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    /**
     * Actualizează permisiunile unui utilizator
     */
    public void updateUserPermissions(User user) throws SQLException {
        String sql = "UPDATE users SET role = ?, can_manage_products = ?, can_manage_categories = ?, " +
                    "can_manage_users = ?, can_view_reports = ?, can_manage_sales = ? WHERE id = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, user.getRole());
            ps.setBoolean(2, user.canManageProducts());
            ps.setBoolean(3, user.canManageCategories());
            ps.setBoolean(4, user.canManageUsers());
            ps.setBoolean(5, user.canViewReports());
            ps.setBoolean(6, user.canManageSales());
            ps.setInt(7, user.getId());
            
            ps.executeUpdate();
            logger.info("Updated permissions for user: " + user.getUsername());
        }
    }

    /**
     * Șterge un utilizator
     */
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.executeUpdate();
            logger.info("Deleted user with ID: " + userId);
        }
    }
}

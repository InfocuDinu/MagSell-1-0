package com.magsell.services;

import com.magsell.database.DatabaseService;
import com.magsell.models.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviciu pentru operații cu categorii de produse.
 * Conține logica de business pentru CRUD (Create, Read, Update, Delete) categorii.
 */
public class CategoryService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);
    private final DatabaseService dbService = DatabaseService.getInstance();

    /**
     * Creează o categorie nouă.
     */
    public void createCategory(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());

            pstmt.executeUpdate();
            logger.info("Categorie creată: " + category.getName());
        }
    }

    /**
     * Obține toate categoriile.
     */
    public List<Category> getAllCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY name";

        try (Connection conn = dbService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Category c = mapRowToCategory(rs);
                categories.add(c);
            }
        }
        return categories;
    }

    /**
     * Obține o categorie după ID.
     */
    public Category getCategoryById(int id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ?";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCategory(rs);
                }
            }
        }
        return null;
    }

    /**
     * Obține o categorie după nume.
     */
    public Category getCategoryByName(String name) throws SQLException {
        String sql = "SELECT * FROM categories WHERE name = ?";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCategory(rs);
                }
            }
        }
        return null;
    }

    /**
     * Actualizează o categorie.
     */
    public void updateCategory(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setInt(3, category.getId());

            pstmt.executeUpdate();
            logger.info("Categorie actualizată: " + category.getName());
        }
    }

    /**
     * Șterge o categorie.
     */
    public void deleteCategory(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ?";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            logger.info("Categorie ștearsă cu ID: " + id);
        }
    }

    /**
     * Verifică dacă o categorie poate fi ștearsă (nu are produse asociate).
     */
    public boolean canDeleteCategory(int categoryId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ?";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }
        return true;
    }

    /**
     * Mapează o linie din rezultatul query-ului la un obiect Category.
     */
    private Category mapRowToCategory(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        try {
            c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            c.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        } catch (Exception ignored) {
        }
        return c;
    }
}

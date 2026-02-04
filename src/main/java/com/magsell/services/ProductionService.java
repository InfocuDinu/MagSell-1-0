package com.magsell.services;

import com.magsell.models.*;
import com.magsell.exceptions.InsufficientStockException;
import com.magsell.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviciu pentru managementul producției
 */
public class ProductionService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionService.class);
    private final DatabaseService databaseService;
    
    public ProductionService() {
        this.databaseService = DatabaseService.getInstance();
        createTables();
    }
    
    /**
     * Creează tabelele necesare pentru producție dacă nu există
     */
    private void createTables() {
        try (Connection conn = databaseService.getConnection()) {
            // Tabel pentru rețete
            String createRecipesTable = """
                CREATE TABLE IF NOT EXISTS recipes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    created_by TEXT,
                    active BOOLEAN DEFAULT 1,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                )
                """;
            
            // Tabel pentru ingredientele rețetelor
            String createRecipeIngredientsTable = """
                CREATE TABLE IF NOT EXISTS recipe_ingredients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recipe_id INTEGER NOT NULL,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    unit_of_measure TEXT DEFAULT 'buc',
                    FOREIGN KEY (recipe_id) REFERENCES recipes(id),
                    FOREIGN KEY (product_id) REFERENCES products(id)
                )
                """;
            
            // Tabel pentru ordinele de producție
            String createProductionOrdersTable = """
                CREATE TABLE IF NOT EXISTS production_orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recipe_id INTEGER NOT NULL,
                    recipe_name TEXT NOT NULL,
                    quantity_to_produce REAL NOT NULL,
                    status TEXT DEFAULT 'pending',
                    created_at TEXT NOT NULL,
                    started_at TEXT,
                    completed_at TEXT,
                    created_by TEXT,
                    notes TEXT,
                    FOREIGN KEY (recipe_id) REFERENCES recipes(id)
                )
                """;
            
            // Tabel pentru log-uri inventar
            String createInventoryLogsTable = """
                CREATE TABLE IF NOT EXISTS inventory_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity_change REAL NOT NULL,
                    quantity_before REAL NOT NULL,
                    quantity_after REAL NOT NULL,
                    operation_type TEXT NOT NULL,
                    reference_id INTEGER,
                    reference_type TEXT,
                    created_at TEXT NOT NULL,
                    created_by TEXT,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                )
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createRecipesTable);
                stmt.execute(createRecipeIngredientsTable);
                stmt.execute(createProductionOrdersTable);
                stmt.execute(createInventoryLogsTable);
                logger.info("Production tables created successfully");
            }
            
        } catch (SQLException e) {
            logger.error("Error creating production tables", e);
        }
    }
    
    /**
     * Salvează o rețetă nouă
     */
    public Recipe saveRecipe(Recipe recipe) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = """
                INSERT INTO recipes (product_id, product_name, created_at, updated_at, created_by, active)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, recipe.getProductId());
                pstmt.setString(2, recipe.getProductName());
                pstmt.setString(3, recipe.getCreatedAt().toString());
                pstmt.setString(4, recipe.getUpdatedAt().toString());
                pstmt.setString(5, recipe.getCreatedBy());
                pstmt.setBoolean(6, recipe.isActive());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            recipe.setId(generatedKeys.getInt(1));
                        }
                    }
                }
            }
            
            // Salvăm ingredientele
            if (recipe.getIngredients() != null) {
                for (RecipeIngredient ingredient : recipe.getIngredients()) {
                    ingredient.setRecipeId(recipe.getId());
                    saveRecipeIngredient(ingredient);
                }
            }
            
            logger.info("Saved recipe: {}", recipe.getProductName());
            return recipe;
            
        } catch (SQLException e) {
            logger.error("Error saving recipe", e);
            return null;
        }
    }
    
    /**
     * Salvează un ingredient din rețetă
     */
    private RecipeIngredient saveRecipeIngredient(RecipeIngredient ingredient) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = """
                INSERT INTO recipe_ingredients (recipe_id, product_id, product_name, quantity, unit_of_measure)
                VALUES (?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, ingredient.getRecipeId());
                pstmt.setInt(2, ingredient.getProductId());
                pstmt.setString(3, ingredient.getProductName());
                pstmt.setDouble(4, ingredient.getQuantity());
                pstmt.setString(5, ingredient.getUnitOfMeasure());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            ingredient.setId(generatedKeys.getInt(1));
                        }
                    }
                }
            }
            
            return ingredient;
            
        } catch (SQLException e) {
            logger.error("Error saving recipe ingredient", e);
            return null;
        }
    }
    
    /**
     * Creează o nouă comandă de producție
     */
    public ProductionOrder createProductionOrder(ProductionOrder order) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = """
                INSERT INTO production_orders (recipe_id, recipe_name, quantity_to_produce, status, created_at, created_by, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, order.getRecipeId());
                pstmt.setString(2, order.getRecipeName());
                pstmt.setDouble(3, order.getQuantityToProduce());
                pstmt.setString(4, order.getStatus());
                pstmt.setString(5, order.getCreatedAt().toString());
                pstmt.setString(6, order.getCreatedBy());
                pstmt.setString(7, order.getNotes());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            order.setId(generatedKeys.getInt(1));
                        }
                    }
                }
            }
            
            logger.info("Created production order: {} - Quantity: {}", order.getRecipeName(), order.getQuantityToProduce());
            return order;
            
        } catch (SQLException e) {
            logger.error("Error creating production order", e);
            return null;
        }
    }
    
    /**
     * Procesează o comandă de producție
     */
    public void processProduction(ProductionOrder order) throws InsufficientStockException {
        try (Connection conn = databaseService.getConnection()) {
            // Începem tranzacția
            conn.setAutoCommit(false);
            
            try {
                // Obținem rețeta
                Recipe recipe = getRecipe(order.getRecipeId());
                if (recipe == null) {
                    throw new InsufficientStockException("Rețeta nu a fost găsită");
                }
                
                // Verificăm stocul pentru fiecare ingredient
                for (RecipeIngredient ingredient : recipe.getIngredients()) {
                    double requiredQuantity = ingredient.getQuantity() * order.getQuantityToProduce();
                    double availableStock = getProductStock(ingredient.getProductId());
                    
                    if (availableStock < requiredQuantity) {
                        throw new InsufficientStockException(
                            String.format("Stoc insuficient pentru %s. Necesar: %.2f, Disponibil: %.2f",
                                ingredient.getProductName(), requiredQuantity, availableStock));
                    }
                }
                
                // Actualizăm stocurile
                for (RecipeIngredient ingredient : recipe.getIngredients()) {
                    double requiredQuantity = ingredient.getQuantity() * order.getQuantityToProduce();
                    updateProductStock(conn, ingredient.getProductId(), -requiredQuantity, 
                        "Producție - " + order.getRecipeName(), order.getId(), "production_order");
                }
                
                // Adăugăm stoc pentru produsul finit
                updateProductStock(conn, recipe.getProductId(), order.getQuantityToProduce(),
                    "Producție - " + order.getRecipeName(), order.getId(), "production_order");
                
                // Actualizăm statusul comenzii
                updateProductionOrderStatus(conn, order.getId(), "completed");
                
                // Commit tranzacție
                conn.commit();
                
                logger.info("Production order {} completed successfully", order.getId());
                
            } catch (SQLException | InsufficientStockException e) {
                // Rollback în caz de eroare
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            logger.error("Error processing production order", e);
            throw new InsufficientStockException("Eroare la procesarea comenzii de producție: " + e.getMessage());
        }
    }
    
    /**
     * Obține stocul curent pentru un produs
     */
    private double getProductStock(int productId) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT stock FROM products WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, productId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("stock");
                    }
                }
            }
        }
        return 0.0;
    }
    
    /**
     * Actualizează stocul unui produs și înregistrează log-ul
     */
    private void updateProductStock(Connection conn, int productId, double quantityChange, 
                                   String description, Integer referenceId, String referenceType) throws SQLException {
        
        // Obținem stocul curent
        double currentStock = getProductStock(productId);
        double newStock = currentStock + quantityChange;
        
        // Actualizăm stocul
        String updateSql = "UPDATE products SET stock = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setDouble(1, newStock);
            pstmt.setString(2, LocalDateTime.now().toString());
            pstmt.setInt(3, productId);
            pstmt.executeUpdate();
        }
        
        // Înregistrăm log-ul
        String logSql = """
            INSERT INTO inventory_logs (product_id, product_name, quantity_change, quantity_before, 
                quantity_after, operation_type, reference_id, reference_type, created_at, created_by)
            SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(logSql)) {
            pstmt.setInt(1, productId);
            pstmt.setString(2, getProductName(productId));
            pstmt.setDouble(3, quantityChange);
            pstmt.setDouble(4, currentStock);
            pstmt.setDouble(5, newStock);
            pstmt.setString(6, quantityChange > 0 ? "PRODUCTION_OUTPUT" : "PRODUCTION_INPUT");
            pstmt.setObject(7, referenceId);
            pstmt.setString(8, referenceType);
            pstmt.setString(9, LocalDateTime.now().toString());
            pstmt.setString(10, "system");
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Obține numele unui produs
     */
    private String getProductName(int productId) throws SQLException {
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT name FROM products WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, productId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("name");
                    }
                }
            }
        }
        return "Produs #" + productId;
    }
    
    /**
     * Actualizează statusul unei comenzi de producție
     */
    private void updateProductionOrderStatus(Connection conn, int orderId, String status) throws SQLException {
        String sql = "UPDATE production_orders SET status = ?, completed_at = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, "completed".equals(status) ? LocalDateTime.now().toString() : null);
            pstmt.setInt(3, orderId);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Obține o rețetă după ID
     */
    public Recipe getRecipe(int recipeId) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM recipes WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, recipeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Recipe recipe = new Recipe();
                        recipe.setId(rs.getInt("id"));
                        recipe.setProductId(rs.getInt("product_id"));
                        recipe.setProductName(rs.getString("product_name"));
                        recipe.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
                        recipe.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at")));
                        recipe.setCreatedBy(rs.getString("created_by"));
                        recipe.setActive(rs.getBoolean("active"));
                        
                        // Încărcăm ingredientele
                        recipe.setIngredients(getRecipeIngredients(recipeId));
                        
                        return recipe;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting recipe", e);
        }
        return null;
    }
    
    /**
     * Obține ingredientele unei rețete
     */
    private List<RecipeIngredient> getRecipeIngredients(int recipeId) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM recipe_ingredients WHERE recipe_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, recipeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        RecipeIngredient ingredient = new RecipeIngredient();
                        ingredient.setId(rs.getInt("id"));
                        ingredient.setRecipeId(rs.getInt("recipe_id"));
                        ingredient.setProductId(rs.getInt("product_id"));
                        ingredient.setProductName(rs.getString("product_name"));
                        ingredient.setQuantity(rs.getDouble("quantity"));
                        ingredient.setUnitOfMeasure(rs.getString("unit_of_measure"));
                        ingredients.add(ingredient);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting recipe ingredients", e);
        }
        return ingredients;
    }
    
    /**
     * Obține toate rețetele active
     */
    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM recipes WHERE active = 1 ORDER BY product_name";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Recipe recipe = new Recipe();
                    recipe.setId(rs.getInt("id"));
                    recipe.setProductId(rs.getInt("product_id"));
                    recipe.setProductName(rs.getString("product_name"));
                    recipe.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
                    recipe.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at")));
                    recipe.setCreatedBy(rs.getString("created_by"));
                    recipe.setActive(rs.getBoolean("active"));
                    
                    // Încărcăm ingredientele
                    recipe.setIngredients(getRecipeIngredients(recipe.getId()));
                    
                    recipes.add(recipe);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting all recipes", e);
        }
        return recipes;
    }
    
    /**
     * Obține toate ordinele de producție
     */
    public List<ProductionOrder> getAllProductionOrders() {
        List<ProductionOrder> orders = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM production_orders ORDER BY created_at DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    ProductionOrder order = new ProductionOrder();
                    order.setId(rs.getInt("id"));
                    order.setRecipeId(rs.getInt("recipe_id"));
                    order.setRecipeName(rs.getString("recipe_name"));
                    order.setQuantityToProduce(rs.getDouble("quantity_to_produce"));
                    order.setStatus(rs.getString("status"));
                    order.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
                    
                    String startedAt = rs.getString("started_at");
                    if (startedAt != null) {
                        order.setStartedAt(LocalDateTime.parse(startedAt));
                    }
                    
                    String completedAt = rs.getString("completed_at");
                    if (completedAt != null) {
                        order.setCompletedAt(LocalDateTime.parse(completedAt));
                    }
                    
                    order.setCreatedBy(rs.getString("created_by"));
                    order.setNotes(rs.getString("notes"));
                    
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting all production orders", e);
        }
        return orders;
    }
}

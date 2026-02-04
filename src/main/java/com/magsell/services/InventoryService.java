package com.magsell.services;

import com.magsell.models.*;
import com.magsell.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviciu pentru managementul stocurilor și mișcărilor de inventar
 */
public class InventoryService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    private final DatabaseService databaseService;
    private final ProductService productService;
    
    public InventoryService() {
        this.databaseService = DatabaseService.getInstance();
        this.productService = new ProductService();
    }
    
    /**
     * Scade stocul pentru un produs și înregistrează mișcarea
     */
    public void decreaseStock(Connection conn, Integer productId, BigDecimal quantity, 
                             Integer warehouseId, String documentType, Integer documentId, 
                             String notes) throws SQLException {
        
        logger.info("Decreasing stock for product: {}, quantity: {}, warehouse: {}", 
                   productId, quantity, warehouseId);
        
        // Verifică stocul disponibil
        Product product = productService.getProductById(productId);
        if (new BigDecimal(product.getQuantity()).compareTo(quantity) < 0) {
            throw new SQLException("Stoc insuficient. Disponibil: " + product.getQuantity() + 
                                 ", Necesar: " + quantity);
        }
        
        // Actualizează stocul produsului
        updateProductStock(conn, productId, quantity.negate());
        
        // Înregistrează mișcarea de inventar
        recordInventoryMovement(conn, productId, warehouseId, "OUT", documentType, 
                               documentId, quantity.negate(), null, null, notes);
    }
    
    /**
     * Crește stocul pentru un produs și înregistrează mișcarea
     */
    public void increaseStock(Connection conn, Integer productId, BigDecimal quantity, 
                             Integer warehouseId, String documentType, Integer documentId, 
                             String notes) throws SQLException {
        
        logger.info("Increasing stock for product: {}, quantity: {}, warehouse: {}", 
                   productId, quantity, warehouseId);
        
        // Actualizează stocul produsului
        updateProductStock(conn, productId, quantity);
        
        // Înregistrează mișcarea de inventar
        recordInventoryMovement(conn, productId, warehouseId, "IN", documentType, 
                               documentId, quantity, null, null, notes);
    }
    
    /**
     * Transferă stoc între gestiuni
     */
    public void transferStock(Integer productId, BigDecimal quantity, 
                             Integer fromWarehouseId, Integer toWarehouseId, 
                             Integer userId, String notes) throws Exception {
        
        logger.info("Transferring stock for product: {}, quantity: {} from {} to {}", 
                   productId, quantity, fromWarehouseId, toWarehouseId);
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            // Verifică stocul disponibil în sursă
            Product product = productService.getProductById(productId);
            if (new BigDecimal(product.getQuantity()).compareTo(quantity) < 0) {
                throw new Exception("Stoc insuficient pentru transfer. Disponibil: " + 
                                  product.getQuantity() + ", Necesar: " + quantity);
            }
            
            // Scade stocul din sursă
            decreaseStock(conn, productId, quantity, fromWarehouseId, "TRANSFER", null, 
                         "Transfer către Gestiune " + toWarehouseId + " - " + notes);
            
            // Crește stocul în destinație
            increaseStock(conn, productId, quantity, toWarehouseId, "TRANSFER", null, 
                         "Transfer de la Gestiune " + fromWarehouseId + " - " + notes);
            
            conn.commit();
            logger.info("Stock transfer completed successfully");
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error transferring stock", e);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }
    
    /**
     * Ajustare stoc (inventar)
     */
    public void adjustStock(Integer productId, BigDecimal newQuantity, 
                           Integer warehouseId, Integer userId, String notes) throws Exception {
        
        logger.info("Adjusting stock for product: {}, new quantity: {}", productId, newQuantity);
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            Product product = productService.getProductById(productId);
            BigDecimal currentQuantity = new BigDecimal(product.getQuantity());
            BigDecimal adjustment = newQuantity.subtract(currentQuantity);
            
            if (adjustment.compareTo(BigDecimal.ZERO) == 0) {
                logger.info("No adjustment needed - quantities are equal");
                return;
            }
            
            // Actualizează stocul
            updateProductStock(conn, productId, adjustment);
            
            // Înregistrează mișcarea de ajustare
            String movementType = adjustment.compareTo(BigDecimal.ZERO) > 0 ? "IN" : "OUT";
            recordInventoryMovement(conn, productId, warehouseId, "ADJUSTMENT", "ADJ", 
                                   null, adjustment, null, null, 
                                   "Ajustare stoc: " + notes + " (Vechi: " + currentQuantity + 
                                   ", Nou: " + newQuantity + ")");
            
            conn.commit();
            logger.info("Stock adjustment completed successfully");
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error adjusting stock", e);
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }
    
    /**
     * Obține istoricul mișcărilor pentru un produs
     */
    public List<InventoryMovement> getProductMovements(Integer productId, int limit) throws Exception {
        String sql = "SELECT * FROM inventory_movements WHERE product_id = ? " +
                    "ORDER BY created_at DESC LIMIT ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<InventoryMovement> movements = new ArrayList<>();
                while (rs.next()) {
                    movements.add(mapResultSetToInventoryMovement(rs));
                }
                return movements;
            }
            
        } catch (SQLException e) {
            logger.error("Error getting product movements", e);
            throw new Exception("Eroare la obținerea mișcărilor: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obține toate mișcările dintr-o perioadă
     */
    public List<InventoryMovement> getMovementsByDateRange(LocalDateTime startDate, 
                                                         LocalDateTime endDate) throws Exception {
        String sql = "SELECT * FROM inventory_movements WHERE created_at BETWEEN ? AND ? " +
                    "ORDER BY created_at DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<InventoryMovement> movements = new ArrayList<>();
                while (rs.next()) {
                    movements.add(mapResultSetToInventoryMovement(rs));
                }
                return movements;
            }
            
        } catch (SQLException e) {
            logger.error("Error getting movements by date range", e);
            throw new Exception("Eroare la obținerea mișcărilor: " + e.getMessage(), e);
        }
    }
    
    // Metode private
    
    private void updateProductStock(Connection conn, Integer productId, BigDecimal quantityChange) 
            throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ?, updated_at = ? WHERE id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, quantityChange);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, productId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Product not found: " + productId);
            }
        }
    }
    
    private void recordInventoryMovement(Connection conn, Integer productId, Integer warehouseId,
                                       String movementType, String documentType, Integer documentId,
                                       BigDecimal quantity, BigDecimal unitPrice,
                                       String batchNumber, String notes) throws SQLException {
        
        String sql = "INSERT INTO inventory_movements " +
                    "(product_id, warehouse_id, movement_type, document_type, document_id, " +
                    "quantity, unit_price, batch_number, notes, created_by, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, warehouseId);
            stmt.setString(3, movementType);
            stmt.setString(4, documentType);
            
            if (documentId != null) {
                stmt.setInt(5, documentId);
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            stmt.setBigDecimal(6, quantity);
            
            if (unitPrice != null) {
                stmt.setBigDecimal(7, unitPrice);
            } else {
                stmt.setNull(7, Types.DECIMAL);
            }
            
            stmt.setString(8, batchNumber);
            stmt.setString(9, notes);
            stmt.setNull(10, Types.INTEGER); // created_by - poate fi setat mai târziu
            stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            
            stmt.executeUpdate();
        }
    }
    
    private InventoryMovement mapResultSetToInventoryMovement(ResultSet rs) throws SQLException {
        InventoryMovement movement = new InventoryMovement();
        movement.setId(rs.getInt("id"));
        movement.setProductId(rs.getInt("product_id"));
        movement.setWarehouseId(rs.getInt("warehouse_id"));
        movement.setMovementType(rs.getString("movement_type"));
        movement.setDocumentType(rs.getString("document_type"));
        
        int documentId = rs.getInt("document_id");
        if (!rs.wasNull()) {
            movement.setDocumentId(documentId);
        }
        
        movement.setQuantity(rs.getBigDecimal("quantity"));
        
        BigDecimal unitPrice = rs.getBigDecimal("unit_price");
        if (!rs.wasNull()) {
            movement.setUnitPrice(unitPrice);
        }
        
        movement.setBatchNumber(rs.getString("batch_number"));
        movement.setNotes(rs.getString("notes"));
        
        int createdBy = rs.getInt("created_by");
        if (!rs.wasNull()) {
            movement.setCreatedBy(createdBy);
        }
        
        movement.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        return movement;
    }
}

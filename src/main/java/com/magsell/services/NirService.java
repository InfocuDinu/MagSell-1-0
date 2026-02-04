package com.magsell.services;

import com.magsell.models.*;
import com.magsell.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviciu pentru managementul Notelor de Intrare-Recepție (NIR)
 */
public class NirService {
    private static final Logger logger = LoggerFactory.getLogger(NirService.class);
    
    private final DatabaseService databaseService;
    private final ProductService productService;
    private final PartnerService partnerService;
    private final InventoryService inventoryService;
    
    public NirService() {
        this.databaseService = DatabaseService.getInstance();
        this.productService = new ProductService();
        this.partnerService = new PartnerService();
        this.inventoryService = new InventoryService();
    }
    
    /**
     * Generează următorul număr de NIR
     */
    public String getNextNirNumber() throws Exception {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTR(number, 5) AS INTEGER)), 0) + 1 as next_number " +
                    "FROM nirs WHERE number LIKE 'NIR-%' AND date >= ? AND date <= ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            LocalDate today = LocalDate.now();
            stmt.setDate(1, Date.valueOf(today.withDayOfMonth(1)));
            stmt.setDate(2, Date.valueOf(today.withDayOfMonth(today.lengthOfMonth())));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int nextNumber = rs.getInt("next_number");
                    return "NIR-" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                           String.format("%03d", nextNumber);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting next NIR number", e);
        }
        
        // Default format
        return "NIR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001";
    }
    
    /**
     * Salvează un NIR complet cu item-urile
     */
    public Nir saveNir(Nir nir) throws Exception {
        logger.info("Saving NIR: {}", nir.getNumber());
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            // Generează număr dacă nu există
            if (nir.getNumber() == null || nir.getNumber().trim().isEmpty()) {
                nir.setNumber(getNextNirNumber());
            }
            
            // Setează timestamp-uri
            nir.setCreatedAt(LocalDateTime.now());
            nir.setUpdatedAt(LocalDateTime.now());
            
            // Salvează NIR-ul
            Integer nirId = insertNir(conn, nir);
            nir.setId(nirId);
            
            // Salvează item-urile și actualizează stocurile
            for (NirItem item : nir.getItems()) {
                item.setNirId(nirId);
                insertNirItem(conn, item);
                
                // Creștere stoc
                inventoryService.increaseStock(
                    conn, 
                    item.getProductId(), 
                    item.getQuantity(), 
                    item.getWarehouseId(),
                    "NIR", 
                    nirId,
                    "NIR " + nir.getNumber() + " - " + item.getProductName()
                );
            }
            
            // Recalculează totalul
            nir.recalculateTotal();
            updateNirTotal(conn, nir);
            
            conn.commit();
            logger.info("NIR saved successfully: {}", nir.getNumber());
            
            return nir;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error saving NIR", e);
            throw new Exception("Eroare la salvarea NIR-ului: " + e.getMessage(), e);
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
     * Obține toate NIR-urile
     */
    public List<Nir> getAllNirs() throws Exception {
        String sql = "SELECT * FROM nirs ORDER BY date DESC, number DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            List<Nir> nirs = new ArrayList<>();
            while (rs.next()) {
                Nir nir = mapResultSetToNir(rs);
                // Încarcă item-urile
                nir.setItems(getNirItemsByNirId(nir.getId()));
                nirs.add(nir);
            }
            
            return nirs;
            
        } catch (SQLException e) {
            logger.error("Error getting all NIRs", e);
            throw new Exception("Eroare la obținerea NIR-urilor: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obține NIR după ID
     */
    public Nir getNirById(Integer nirId) throws Exception {
        String sql = "SELECT * FROM nirs WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nirId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Nir nir = mapResultSetToNir(rs);
                    nir.setItems(getNirItemsByNirId(nirId));
                    return nir;
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting NIR by ID: {}", nirId, e);
            throw new Exception("Eroare la obținerea NIR-ului: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Obține item-urile unui NIR
     */
    private List<NirItem> getNirItemsByNirId(Integer nirId) throws Exception {
        String sql = "SELECT * FROM nir_items WHERE nir_id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nirId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<NirItem> items = new ArrayList<>();
                while (rs.next()) {
                    NirItem item = mapResultSetToNirItem(rs);
                    // Încarcă produsul
                    Product product = productService.getProductById(item.getProductId());
                    item.setProduct(product);
                    items.add(item);
                }
                return items;
            }
            
        } catch (SQLException e) {
            logger.error("Error getting NIR items for NIR: {}", nirId, e);
            throw new Exception("Eroare la obținerea item-urilor NIR: " + e.getMessage(), e);
        }
    }
    
    // Metode private
    
    private Integer insertNir(Connection conn, Nir nir) throws SQLException {
        String sql = "INSERT INTO nirs (number, date, supplier_id, invoice_number, invoice_date, " +
                    "warehouse, total_amount, notes, created_by, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nir.getNumber());
            stmt.setDate(2, Date.valueOf(nir.getDate()));
            stmt.setInt(3, nir.getSupplierId());
            stmt.setString(4, nir.getInvoiceNumber());
            stmt.setDate(5, Date.valueOf(nir.getInvoiceDate()));
            stmt.setString(6, nir.getWarehouse());
            stmt.setBigDecimal(7, nir.getTotalAmount());
            stmt.setString(8, nir.getNotes());
            stmt.setInt(9, nir.getCreatedBy());
            stmt.setTimestamp(10, Timestamp.valueOf(nir.getCreatedAt()));
            stmt.setTimestamp(11, Timestamp.valueOf(nir.getUpdatedAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        throw new SQLException("Failed to insert NIR, no ID obtained.");
    }
    
    private void insertNirItem(Connection conn, NirItem item) throws SQLException {
        String sql = "INSERT INTO nir_items (nir_id, product_id, product_code, product_name, " +
                    "quantity, unit, unit_price, total, batch_number, expiry_date, warehouse_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, item.getNirId());
            stmt.setInt(2, item.getProductId());
            stmt.setString(3, item.getProductCode());
            stmt.setString(4, item.getProductName());
            stmt.setBigDecimal(5, item.getQuantity());
            stmt.setString(6, item.getUnit());
            stmt.setBigDecimal(7, item.getUnitPrice());
            stmt.setBigDecimal(8, item.getTotal());
            stmt.setString(9, item.getBatchNumber());
            stmt.setString(10, item.getExpiryDate());
            stmt.setInt(11, item.getWarehouseId());
            
            stmt.executeUpdate();
        }
    }
    
    private void updateNirTotal(Connection conn, Nir nir) throws SQLException {
        String sql = "UPDATE nirs SET total_amount = ?, updated_at = ? WHERE id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, nir.getTotalAmount());
            stmt.setTimestamp(2, Timestamp.valueOf(nir.getUpdatedAt()));
            stmt.setInt(3, nir.getId());
            
            stmt.executeUpdate();
        }
    }
    
    private Nir mapResultSetToNir(ResultSet rs) throws SQLException {
        Nir nir = new Nir();
        nir.setId(rs.getInt("id"));
        nir.setNumber(rs.getString("number"));
        nir.setDate(rs.getDate("date").toLocalDate());
        nir.setSupplierId(rs.getInt("supplier_id"));
        nir.setInvoiceNumber(rs.getString("invoice_number"));
        nir.setInvoiceDate(rs.getDate("invoice_date").toLocalDate());
        nir.setWarehouse(rs.getString("warehouse"));
        nir.setTotalAmount(rs.getBigDecimal("total_amount"));
        nir.setNotes(rs.getString("notes"));
        nir.setCreatedBy(rs.getInt("created_by"));
        nir.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        nir.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        return nir;
    }
    
    private NirItem mapResultSetToNirItem(ResultSet rs) throws SQLException {
        NirItem item = new NirItem();
        item.setId(rs.getInt("id"));
        item.setNirId(rs.getInt("nir_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setProductCode(rs.getString("product_code"));
        item.setProductName(rs.getString("product_name"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setUnit(rs.getString("unit"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setTotal(rs.getBigDecimal("total"));
        item.setBatchNumber(rs.getString("batch_number"));
        item.setExpiryDate(rs.getString("expiry_date"));
        item.setWarehouseId(rs.getInt("warehouse_id"));
        
        return item;
    }
}

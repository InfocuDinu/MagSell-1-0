package com.magsell.services;

import com.magsell.models.*;
import com.magsell.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviciu pentru managementul facturilor fiscale
 * Respectă principiile SOLID și legislația fiscală română
 */
public class InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    
    private final DatabaseService databaseService;
    private final ProductService productService;
    private final PartnerService partnerService;
    private final InventoryService inventoryService;
    
    public InvoiceService() {
        this.databaseService = DatabaseService.getInstance();
        this.productService = new ProductService();
        this.partnerService = new PartnerService();
        this.inventoryService = new InventoryService();
    }
    
    /**
     * Creează o nouă factură cu validări complete
     */
    public Invoice createInvoice(Invoice invoice) throws Exception {
        logger.info("Creating new invoice for partner: {}", invoice.getPartnerId());
        
        // Validări de business
        validateInvoice(invoice);
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // Generează număr factură
            String series = invoice.getSeries();
            if (series == null) {
                series = getNextSeries(conn, 'F');
            }
            Integer number = getNextNumber(conn, series, LocalDate.now().getYear());
            
            invoice.setSeries(series);
            invoice.setNumber(number);
            invoice.setStatus("draft");
            invoice.setCreatedAt(LocalDateTime.now());
            invoice.setUpdatedAt(LocalDateTime.now());
            
            // Salvează factura
            Integer invoiceId = insertInvoice(conn, invoice);
            invoice.setId(invoiceId);
            
            // Salvează item-urile și actualizează stocurile
            for (InvoiceItem item : invoice.getItems()) {
                item.setInvoiceId(invoiceId);
                insertInvoiceItem(conn, item);
                
                // Scădere stoc (doar dacă factura este emisă)
                if ("issued".equals(invoice.getStatus())) {
                    inventoryService.decreaseStock(
                        conn, 
                        item.getProductId(), 
                        item.getQuantity(), 
                        item.getWarehouseId(),
                        "FAC", 
                        invoiceId,
                        "Factură " + invoice.getFullNumber()
                    );
                }
            }
            
            // Recalculează totalele
            invoice.recalculateTotals();
            updateInvoiceTotals(conn, invoice);
            
            conn.commit();
            logger.info("Invoice created successfully: {}", invoice.getFullNumber());
            
            return invoice;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error creating invoice", e);
            throw new Exception("Eroare la crearea facturii: " + e.getMessage(), e);
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
     * Emite o factură (schimbă status din draft în issued)
     */
    public Invoice issueInvoice(Integer invoiceId, Integer userId) throws Exception {
        logger.info("Issuing invoice: {}", invoiceId);
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            Invoice invoice = getInvoiceById(conn, invoiceId);
            if (invoice == null) {
                throw new Exception("Factura nu a fost găsită");
            }
            
            if (!"draft".equals(invoice.getStatus())) {
                throw new Exception("Factura nu este în status draft");
            }
            
            // Verifică stocurile disponibile
            for (InvoiceItem item : invoice.getItems()) {
                Product product = productService.getProductById(item.getProductId());
                if (product.getQuantity().compareTo(item.getQuantity()) < 0) {
                    throw new Exception(String.format(
                        "Stoc insuficient pentru produsul %s. Disponibil: %s, Necesar: %s",
                        product.getName(),
                        product.getQuantity(),
                        item.getQuantity()
                    ));
                }
            }
            
            // Actualizează status
            invoice.setStatus("issued");
            invoice.setUpdatedAt(LocalDateTime.now());
            invoice.setCreatedBy(userId);
            
            updateInvoice(conn, invoice);
            
            // Scădere stoc
            for (InvoiceItem item : invoice.getItems()) {
                inventoryService.decreaseStock(
                    conn, 
                    item.getProductId(), 
                    item.getQuantity(), 
                    item.getWarehouseId(),
                    "FAC", 
                    invoiceId,
                    "Factură " + invoice.getFullNumber()
                );
            }
            
            conn.commit();
            logger.info("Invoice issued successfully: {}", invoice.getFullNumber());
            
            return invoice;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error issuing invoice", e);
            throw new Exception("Eroare la emiterea facturii: " + e.getMessage(), e);
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
     * Anulează o factură și restabilește stocurile
     */
    public Invoice cancelInvoice(Integer invoiceId, String reason, Integer userId) throws Exception {
        logger.info("Cancelling invoice: {} - Reason: {}", invoiceId, reason);
        
        Connection conn = null;
        try {
            conn = databaseService.getConnection();
            conn.setAutoCommit(false);
            
            Invoice invoice = getInvoiceById(conn, invoiceId);
            if (invoice == null) {
                throw new Exception("Factura nu a fost găsită");
            }
            
            if ("cancelled".equals(invoice.getStatus())) {
                throw new Exception("Factura este deja anulată");
            }
            
            if ("paid".equals(invoice.getStatus())) {
                throw new Exception("Factura plătită nu poate fi anulată");
            }
            
            // Restabilește stocurile dacă factura a fost emisă
            if ("issued".equals(invoice.getStatus())) {
                for (InvoiceItem item : invoice.getItems()) {
                    inventoryService.increaseStock(
                        conn, 
                        item.getProductId(), 
                        item.getQuantity(), 
                        item.getWarehouseId(),
                        "FAC", 
                        invoiceId,
                        "Anulare factură " + invoice.getFullNumber() + " - " + reason
                    );
                }
            }
            
            // Actualizează status
            invoice.setStatus("cancelled");
            invoice.setNotes((invoice.getNotes() != null ? invoice.getNotes() + "\n" : "") + 
                           "ANULAT: " + reason);
            invoice.setUpdatedAt(LocalDateTime.now());
            
            updateInvoice(conn, invoice);
            
            conn.commit();
            logger.info("Invoice cancelled successfully: {}", invoice.getFullNumber());
            
            return invoice;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error cancelling invoice", e);
            throw new Exception("Eroare la anularea facturii: " + e.getMessage(), e);
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
     * Obține toate facturile
     */
    public List<Invoice> getAllInvoices() throws Exception {
        String sql = "SELECT * FROM invoices ORDER BY issue_date DESC, series, number DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            List<Invoice> invoices = new ArrayList<>();
            while (rs.next()) {
                Invoice invoice = mapResultSetToInvoice(rs);
                // Încarcă item-urile
                invoice.setItems(getInvoiceItemsByInvoiceId(invoice.getId()));
                invoices.add(invoice);
            }
            
            return invoices;
            
        } catch (SQLException e) {
            logger.error("Error getting all invoices", e);
            throw new Exception("Eroare la obținerea facturilor: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obține factura după ID
     */
    public Invoice getInvoiceById(Integer invoiceId) throws Exception {
        try (Connection conn = databaseService.getConnection()) {
            return getInvoiceById(conn, invoiceId);
        }
    }
    
    private Invoice getInvoiceById(Connection conn, Integer invoiceId) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.setItems(getInvoiceItemsByInvoiceId(invoiceId));
                    return invoice;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Obține item-urile unei facturi
     */
    public List<InvoiceItem> getInvoiceItemsByInvoiceId(Integer invoiceId) throws Exception {
        String sql = "SELECT * FROM invoice_items WHERE invoice_id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, invoiceId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<InvoiceItem> items = new ArrayList<>();
                while (rs.next()) {
                    InvoiceItem item = mapResultSetToInvoiceItem(rs);
                    // Încarcă produsul
                    Product product = productService.getProductById(item.getProductId());
                    item.setProduct(product);
                    items.add(item);
                }
                return items;
            }
            
        } catch (SQLException e) {
            logger.error("Error getting invoice items for invoice: {}", invoiceId, e);
            throw new Exception("Eroare la obținerea item-urilor facturii: " + e.getMessage(), e);
        }
    }
    
    // Metode private pentru operațiuni CRUD
    
    private void validateInvoice(Invoice invoice) throws Exception {
        if (invoice.getPartnerId() == null) {
            throw new Exception("Partenerul este obligatoriu");
        }
        
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            throw new Exception("Factura trebuie să conțină cel puțin un produs");
        }
        
        // Verifică partenerul
        Partner partner = partnerService.getPartnerById(invoice.getPartnerId());
        if (partner == null) {
            throw new Exception("Partenerul specificat nu există");
        }
        
        // Verifică produsele
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getProductId() == null) {
                throw new Exception("Produsul este obligatoriu pentru fiecare item");
            }
            
            if (item.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new Exception("Cantitatea trebuie să fie pozitivă");
            }
            
            if (item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new Exception("Prețul unitar trebuie să fie pozitiv");
            }
            
            Product product = productService.getProductById(item.getProductId());
            if (product == null) {
                throw new Exception("Produsul specificat nu există");
            }
        }
    }
    
    private Integer insertInvoice(Connection conn, Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (series, number, partner_id, issue_date, due_date, " +
                    "payment_method_id, total_amount, total_vat, total_with_vat, status, notes, " +
                    "is_e_factura, e_factura_status, e_factura_xml, created_by, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, invoice.getSeries());
            stmt.setInt(2, invoice.getNumber());
            stmt.setInt(3, invoice.getPartnerId());
            stmt.setDate(4, Date.valueOf(invoice.getIssueDate()));
            stmt.setDate(5, Date.valueOf(invoice.getDueDate()));
            
            if (invoice.getPaymentMethodId() != null) {
                stmt.setInt(6, invoice.getPaymentMethodId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            
            stmt.setBigDecimal(7, invoice.getTotalAmount());
            stmt.setBigDecimal(8, invoice.getTotalVat());
            stmt.setBigDecimal(9, invoice.getTotalWithVat());
            stmt.setString(10, invoice.getStatus());
            stmt.setString(11, invoice.getNotes());
            stmt.setBoolean(12, invoice.getIsEFactura());
            stmt.setString(13, invoice.getEFacturaStatus());
            stmt.setString(14, invoice.getEFacturaXml());
            stmt.setInt(15, invoice.getCreatedBy());
            stmt.setTimestamp(16, Timestamp.valueOf(invoice.getCreatedAt()));
            stmt.setTimestamp(17, Timestamp.valueOf(invoice.getUpdatedAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        throw new SQLException("Failed to insert invoice, no ID obtained.");
    }
    
    private void insertInvoiceItem(Connection conn, InvoiceItem item) throws SQLException {
        String sql = "INSERT INTO invoice_items (invoice_id, product_id, quantity, unit, " +
                    "unit_price, discount_percent, vat_rate, total_amount, total_vat, total_with_vat, " +
                    "warehouse_id, batch_number, expiry_date, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, item.getInvoiceId());
            stmt.setInt(2, item.getProductId());
            stmt.setBigDecimal(3, item.getQuantity());
            stmt.setString(4, item.getUnit());
            stmt.setBigDecimal(5, item.getUnitPrice());
            stmt.setBigDecimal(6, item.getDiscountPercent());
            stmt.setBigDecimal(7, item.getVatRate());
            stmt.setBigDecimal(8, item.getTotalAmount());
            stmt.setBigDecimal(9, item.getTotalVat());
            stmt.setBigDecimal(10, item.getTotalWithVat());
            stmt.setInt(11, item.getWarehouseId());
            stmt.setString(12, item.getBatchNumber());
            
            if (item.getExpiryDate() != null) {
                stmt.setDate(13, Date.valueOf(item.getExpiryDate()));
            } else {
                stmt.setNull(13, Types.DATE);
            }
            
            stmt.setTimestamp(14, Timestamp.valueOf(item.getCreatedAt()));
            
            stmt.executeUpdate();
        }
    }
    
    private void updateInvoice(Connection conn, Invoice invoice) throws SQLException {
        String sql = "UPDATE invoices SET status = ?, notes = ?, updated_at = ? WHERE id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, invoice.getStatus());
            stmt.setString(2, invoice.getNotes());
            stmt.setTimestamp(3, Timestamp.valueOf(invoice.getUpdatedAt()));
            stmt.setInt(4, invoice.getId());
            
            stmt.executeUpdate();
        }
    }
    
    private void updateInvoiceTotals(Connection conn, Invoice invoice) throws SQLException {
        String sql = "UPDATE invoices SET total_amount = ?, total_vat = ?, total_with_vat = ? WHERE id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, invoice.getTotalAmount());
            stmt.setBigDecimal(2, invoice.getTotalVat());
            stmt.setBigDecimal(3, invoice.getTotalWithVat());
            stmt.setInt(4, invoice.getId());
            
            stmt.executeUpdate();
        }
    }
    
    private String getNextSeries(Connection conn, char documentType) throws SQLException {
        String sql = "SELECT prefix FROM document_series WHERE type = ? AND year = ? LIMIT 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(documentType));
            stmt.setInt(2, LocalDate.now().getYear());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("prefix");
                }
            }
        }
        
        // Default series if not found
        return String.valueOf(documentType) + "AC";
    }
    
    private Integer getNextNumber(Connection conn, String series, int year) throws SQLException {
        String sql = "SELECT COALESCE(MAX(number), 0) + 1 as next_number " +
                    "FROM invoices WHERE series = ? AND issue_date >= ? AND issue_date <= ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, series);
            stmt.setDate(2, Date.valueOf(LocalDate.of(year, 1, 1)));
            stmt.setDate(3, Date.valueOf(LocalDate.of(year, 12, 31)));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("next_number");
                }
            }
        }
        
        return 1;
    }
    
    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getInt("id"));
        invoice.setSeries(rs.getString("series"));
        invoice.setNumber(rs.getInt("number"));
        invoice.setPartnerId(rs.getInt("partner_id"));
        invoice.setIssueDate(rs.getDate("issue_date").toLocalDate());
        invoice.setDueDate(rs.getDate("due_date").toLocalDate());
        
        int paymentMethodId = rs.getInt("payment_method_id");
        if (!rs.wasNull()) {
            invoice.setPaymentMethodId(paymentMethodId);
        }
        
        invoice.setTotalAmount(rs.getBigDecimal("total_amount"));
        invoice.setTotalVat(rs.getBigDecimal("total_vat"));
        invoice.setTotalWithVat(rs.getBigDecimal("total_with_vat"));
        invoice.setStatus(rs.getString("status"));
        invoice.setNotes(rs.getString("notes"));
        invoice.setIsEFactura(rs.getBoolean("is_e_factura"));
        invoice.setEFacturaStatus(rs.getString("e_factura_status"));
        invoice.setEFacturaXml(rs.getString("e_factura_xml"));
        invoice.setCreatedBy(rs.getInt("created_by"));
        invoice.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        invoice.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        return invoice;
    }
    
    private InvoiceItem mapResultSetToInvoiceItem(ResultSet rs) throws SQLException {
        InvoiceItem item = new InvoiceItem();
        item.setId(rs.getInt("id"));
        item.setInvoiceId(rs.getInt("invoice_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setUnit(rs.getString("unit"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setDiscountPercent(rs.getBigDecimal("discount_percent"));
        item.setVatRate(rs.getBigDecimal("vat_rate"));
        item.setTotalAmount(rs.getBigDecimal("total_amount"));
        item.setTotalVat(rs.getBigDecimal("total_vat"));
        item.setTotalWithVat(rs.getBigDecimal("total_with_vat"));
        item.setWarehouseId(rs.getInt("warehouse_id"));
        item.setBatchNumber(rs.getString("batch_number"));
        
        Date expiryDate = rs.getDate("expiry_date");
        if (expiryDate != null) {
            item.setExpiryDate(expiryDate.toLocalDate());
        }
        
        item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        return item;
    }
}

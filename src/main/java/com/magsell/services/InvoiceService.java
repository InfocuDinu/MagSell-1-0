package com.magsell.services;

import com.magsell.database.DatabaseService;
import com.magsell.models.Invoice;
import com.magsell.models.InvoiceItem;
import com.magsell.models.ReceptionNote;
import com.magsell.models.ReceptionNoteItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviciu pentru managementul facturilor și notelor de recepție
 */
public class InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    private final DatabaseService databaseService;
    private final SpvIntegrationService spvService;

    public InvoiceService() {
        this.databaseService = DatabaseService.getInstance();
        this.spvService = new SpvIntegrationService();
        initializeDatabase();
    }

    /**
     * Inițializează tabelele pentru facturi și note de recepție
     */
    private void initializeDatabase() {
        try (Connection conn = databaseService.getConnection()) {
            // Tabela pentru facturi
            String createInvoicesTable = """
                CREATE TABLE IF NOT EXISTS invoices (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_number TEXT NOT NULL,
                    series TEXT,
                    issue_date DATE,
                    due_date DATE,
                    supplier_name TEXT,
                    supplier_cif TEXT,
                    supplier_address TEXT,
                    total_amount REAL,
                    vat_amount REAL,
                    currency TEXT DEFAULT 'RON',
                    status TEXT DEFAULT 'imported',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    xml_content TEXT,
                    pdf_path TEXT
                )
                """;

            // Tabela pentru elementele facturilor
            String createInvoiceItemsTable = """
                CREATE TABLE IF NOT EXISTS invoice_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    invoice_id INTEGER,
                    product_name TEXT,
                    product_code TEXT,
                    description TEXT,
                    quantity REAL,
                    unit_of_measure TEXT DEFAULT 'buc',
                    unit_price REAL,
                    total_price REAL,
                    vat_rate REAL DEFAULT 19.0,
                    vat_amount REAL,
                    category TEXT,
                    FOREIGN KEY (invoice_id) REFERENCES invoices (id)
                )
                """;

            // Tabela pentru note de recepție
            String createReceptionNotesTable = """
                CREATE TABLE IF NOT EXISTS reception_notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_number TEXT NOT NULL,
                    series TEXT,
                    reception_date DATE,
                    invoice_date DATE,
                    invoice_number TEXT,
                    supplier_name TEXT,
                    supplier_cif TEXT,
                    supplier_address TEXT,
                    total_amount REAL,
                    vat_amount REAL,
                    currency TEXT DEFAULT 'RON',
                    status TEXT DEFAULT 'draft',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    invoice_id INTEGER,
                    created_by TEXT,
                    notes TEXT,
                    FOREIGN KEY (invoice_id) REFERENCES invoices (id)
                )
                """;

            // Tabela pentru elementele notelor de recepție
            String createReceptionNoteItemsTable = """
                CREATE TABLE IF NOT EXISTS reception_note_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reception_note_id INTEGER,
                    product_name TEXT,
                    product_code TEXT,
                    description TEXT,
                    quantity REAL,
                    unit_of_measure TEXT DEFAULT 'buc',
                    unit_price REAL,
                    total_price REAL,
                    vat_rate REAL DEFAULT 19.0,
                    vat_amount REAL,
                    category TEXT,
                    received_quantity REAL DEFAULT 0,
                    batch_number TEXT,
                    expiry_date DATE,
                    storage_location TEXT,
                    FOREIGN KEY (reception_note_id) REFERENCES reception_notes (id)
                )
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createInvoicesTable);
                stmt.execute(createInvoiceItemsTable);
                stmt.execute(createReceptionNotesTable);
                stmt.execute(createReceptionNoteItemsTable);
                logger.info("Invoice and reception note tables initialized successfully");
            }

        } catch (SQLException e) {
            logger.error("Error initializing invoice database", e);
        }
    }

    /**
     * Importă facturi din SPV
     */
    public List<Invoice> importInvoicesFromSPV(LocalDate startDate, LocalDate endDate, String cif) {
        try {
            // Importăm facturile din SPV
            List<Invoice> invoices = spvService.importInvoicesFromSPV(startDate, endDate, cif);
            
            // Salvăm facturile în baza de date
            for (Invoice invoice : invoices) {
                saveInvoice(invoice);
            }
            
            logger.info("Imported and saved {} invoices from SPV", invoices.size());
            return invoices;
            
        } catch (Exception e) {
            logger.error("Error importing invoices from SPV", e);
            return new ArrayList<>();
        }
    }

    /**
     * Salvează o factură în baza de date
     */
    public Invoice saveInvoice(Invoice invoice) {
        try (Connection conn = databaseService.getConnection()) {
            // Verificăm dacă factura există deja
            String checkSql = "SELECT id FROM invoices WHERE invoice_number = ? AND series = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, invoice.getInvoiceNumber());
                checkStmt.setString(2, invoice.getSeries());
                
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    // Actualizăm factura existentă
                    invoice.setId(rs.getInt("id"));
                    updateInvoice(invoice);
                    return invoice;
                }
            }

            // Inserăm factura nouă
            String insertSql = """
                INSERT INTO invoices (invoice_number, series, issue_date, due_date, 
                    supplier_name, supplier_cif, supplier_address, total_amount, 
                    vat_amount, currency, status, xml_content, pdf_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, invoice.getInvoiceNumber());
                stmt.setString(2, invoice.getSeries());
                stmt.setDate(3, invoice.getIssueDate() != null ? Date.valueOf(invoice.getIssueDate()) : null);
                stmt.setDate(4, invoice.getDueDate() != null ? Date.valueOf(invoice.getDueDate()) : null);
                stmt.setString(5, invoice.getSupplierName());
                stmt.setString(6, invoice.getSupplierCif());
                stmt.setString(7, invoice.getSupplierAddress());
                stmt.setDouble(8, invoice.getTotalAmount());
                stmt.setDouble(9, invoice.getVatAmount());
                stmt.setString(10, invoice.getCurrency());
                stmt.setString(11, invoice.getStatus());
                stmt.setString(12, invoice.getXmlContent());
                stmt.setString(13, invoice.getPdfPath());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            invoice.setId(generatedKeys.getInt(1));
                        }
                    }
                }
            }

            // Salvăm elementele facturii
            if (invoice.getItems() != null) {
                for (InvoiceItem item : invoice.getItems()) {
                    item.setInvoiceId(invoice.getId());
                    saveInvoiceItem(item);
                }
            }

            logger.info("Saved invoice: {}", invoice.getFullInvoiceNumber());
            return invoice;

        } catch (SQLException e) {
            logger.error("Error saving invoice", e);
            return null;
        }
    }

    /**
     * Actualizează o factură existentă
     */
    private void updateInvoice(Invoice invoice) {
        try (Connection conn = databaseService.getConnection()) {
            String updateSql = """
                UPDATE invoices SET issue_date = ?, due_date = ?, supplier_name = ?, 
                    supplier_cif = ?, supplier_address = ?, total_amount = ?, 
                    vat_amount = ?, currency = ?, status = ?, updated_at = CURRENT_TIMESTAMP,
                    xml_content = ?, pdf_path = ?
                WHERE id = ?
                """;

            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setDate(1, invoice.getIssueDate() != null ? Date.valueOf(invoice.getIssueDate()) : null);
                stmt.setDate(2, invoice.getDueDate() != null ? Date.valueOf(invoice.getDueDate()) : null);
                stmt.setString(3, invoice.getSupplierName());
                stmt.setString(4, invoice.getSupplierCif());
                stmt.setString(5, invoice.getSupplierAddress());
                stmt.setDouble(6, invoice.getTotalAmount());
                stmt.setDouble(7, invoice.getVatAmount());
                stmt.setString(8, invoice.getCurrency());
                stmt.setString(9, invoice.getStatus());
                stmt.setString(10, invoice.getXmlContent());
                stmt.setString(11, invoice.getPdfPath());
                stmt.setInt(12, invoice.getId());

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            logger.error("Error updating invoice", e);
        }
    }

    /**
     * Salvează un element de factură
     */
    private void saveInvoiceItem(InvoiceItem item) {
        try (Connection conn = databaseService.getConnection()) {
            String insertSql = """
                INSERT INTO invoice_items (invoice_id, product_name, product_code, description, 
                    quantity, unit_of_measure, unit_price, total_price, vat_rate, vat_amount, category)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, item.getInvoiceId());
                stmt.setString(2, item.getProductName());
                stmt.setString(3, item.getProductCode());
                stmt.setString(4, item.getDescription());
                stmt.setDouble(5, item.getQuantity());
                stmt.setString(6, item.getUnitOfMeasure());
                stmt.setDouble(7, item.getUnitPrice());
                stmt.setDouble(8, item.getTotalPrice());
                stmt.setDouble(9, item.getVatRate());
                stmt.setDouble(10, item.getVatAmount());
                stmt.setString(11, item.getCategory());

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            logger.error("Error saving invoice item", e);
        }
    }

    /**
     * Generează o notă de recepție dintr-o factură
     */
    public ReceptionNote generateReceptionNoteFromInvoice(int invoiceId, String createdBy) {
        try {
            // Obținem factura
            Invoice invoice = getInvoiceById(invoiceId);
            if (invoice == null) {
                logger.error("Invoice not found: {}", invoiceId);
                return null;
            }

            // Creăm nota de recepție
            ReceptionNote receptionNote = new ReceptionNote();
            receptionNote.setInvoiceId(invoiceId);
            receptionNote.setInvoiceNumber(invoice.getFullInvoiceNumber());
            receptionNote.setInvoiceDate(invoice.getIssueDate());
            receptionNote.setSupplierName(invoice.getSupplierName());
            receptionNote.setSupplierCif(invoice.getSupplierCif());
            receptionNote.setSupplierAddress(invoice.getSupplierAddress());
            receptionNote.setTotalAmount(invoice.getTotalAmount());
            receptionNote.setVatAmount(invoice.getVatAmount());
            receptionNote.setCurrency(invoice.getCurrency());
            receptionNote.setCreatedBy(createdBy);

            // Generăm numărul notei de recepție
            receptionNote.setNoteNumber(generateReceptionNoteNumber());
            receptionNote.setSeries("NR");

            // Creăm elementele notei de recepție
            List<ReceptionNoteItem> receptionItems = new ArrayList<>();
            if (invoice.getItems() != null) {
                for (InvoiceItem invoiceItem : invoice.getItems()) {
                    ReceptionNoteItem receptionItem = new ReceptionNoteItem();
                    receptionItem.setProductName(invoiceItem.getProductName());
                    receptionItem.setProductCode(invoiceItem.getProductCode());
                    receptionItem.setDescription(invoiceItem.getDescription());
                    receptionItem.setQuantity(invoiceItem.getQuantity());
                    receptionItem.setUnitOfMeasure(invoiceItem.getUnitOfMeasure());
                    receptionItem.setUnitPrice(invoiceItem.getUnitPrice());
                    receptionItem.setTotalPrice(invoiceItem.getTotalPrice());
                    receptionItem.setVatRate(invoiceItem.getVatRate());
                    receptionItem.setCategory(invoiceItem.getCategory());
                    receptionItem.setReceivedQuantity(invoiceItem.getQuantity()); // Inițial setăm cantitatea facturată

                    receptionItems.add(receptionItem);
                }
            }
            receptionNote.setItems(receptionItems);

            // Salvăm nota de recepție
            ReceptionNote savedNote = saveReceptionNote(receptionNote);

            // Actualizăm statusul facturii
            invoice.setStatus("processed");
            updateInvoice(invoice);

            logger.info("Generated reception note {} from invoice {}", savedNote.getFullNoteNumber(), invoice.getFullInvoiceNumber());
            return savedNote;

        } catch (Exception e) {
            logger.error("Error generating reception note from invoice", e);
            return null;
        }
    }

    /**
     * Salvează o notă de recepție
     */
    public ReceptionNote saveReceptionNote(ReceptionNote receptionNote) {
        try (Connection conn = databaseService.getConnection()) {
            // Generăm numărul notei de recepție dacă nu există
            if (receptionNote.getNoteNumber() == null || receptionNote.getNoteNumber().isEmpty()) {
                receptionNote.setNoteNumber(generateReceptionNoteNumber());
            }
            
            // Setăm seria dacă nu există
            if (receptionNote.getSeries() == null || receptionNote.getSeries().isEmpty()) {
                receptionNote.setSeries("NR");
            }
            
            String insertSql = """
                INSERT INTO reception_notes (note_number, series, reception_date, invoice_date, 
                    invoice_number, supplier_name, supplier_cif, supplier_address, 
                    total_amount, vat_amount, currency, status, invoice_id, created_by, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, receptionNote.getNoteNumber());
                stmt.setString(2, receptionNote.getSeries());
                stmt.setDate(3, Date.valueOf(receptionNote.getReceptionDate()));
                stmt.setDate(4, receptionNote.getInvoiceDate() != null ? Date.valueOf(receptionNote.getInvoiceDate()) : null);
                stmt.setString(5, receptionNote.getInvoiceNumber());
                stmt.setString(6, receptionNote.getSupplierName());
                stmt.setString(7, receptionNote.getSupplierCif());
                stmt.setString(8, receptionNote.getSupplierAddress());
                stmt.setDouble(9, receptionNote.getTotalAmount());
                stmt.setDouble(10, receptionNote.getVatAmount());
                stmt.setString(11, receptionNote.getCurrency());
                stmt.setString(12, receptionNote.getStatus());
                stmt.setInt(13, receptionNote.getInvoiceId());
                stmt.setString(14, receptionNote.getCreatedBy());
                stmt.setString(15, receptionNote.getNotes());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            receptionNote.setId(generatedKeys.getInt(1));
                        }
                    }
                }
            }

            // Salvăm elementele notei de recepție
            if (receptionNote.getItems() != null) {
                for (ReceptionNoteItem item : receptionNote.getItems()) {
                    item.setReceptionNoteId(receptionNote.getId());
                    saveReceptionNoteItem(item);
                }
            }

            logger.info("Saved reception note: {}", receptionNote.getFullNoteNumber());
            return receptionNote;

        } catch (SQLException e) {
            logger.error("Error saving reception note", e);
            return null;
        }
    }

    /**
     * Salvează un element de notă de recepție
     */
    private void saveReceptionNoteItem(ReceptionNoteItem item) {
        try (Connection conn = databaseService.getConnection()) {
            String insertSql = """
                INSERT INTO reception_note_items (reception_note_id, product_name, product_code, 
                    description, quantity, unit_of_measure, unit_price, total_price, vat_rate, 
                    vat_amount, category, received_quantity, batch_number, expiry_date, storage_location)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, item.getReceptionNoteId());
                stmt.setString(2, item.getProductName());
                stmt.setString(3, item.getProductCode());
                stmt.setString(4, item.getDescription());
                stmt.setDouble(5, item.getQuantity());
                stmt.setString(6, item.getUnitOfMeasure());
                stmt.setDouble(7, item.getUnitPrice());
                stmt.setDouble(8, item.getTotalPrice());
                stmt.setDouble(9, item.getVatRate());
                stmt.setDouble(10, item.getVatAmount());
                stmt.setString(11, item.getCategory());
                stmt.setDouble(12, item.getReceivedQuantity());
                stmt.setString(13, item.getBatchNumber());
                stmt.setDate(14, item.getExpiryDate() != null ? Date.valueOf(item.getExpiryDate()) : null);
                stmt.setString(15, item.getStorageLocation());

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            logger.error("Error saving reception note item", e);
        }
    }

    /**
     * Generează un număr unic pentru nota de recepție
     */
    private String generateReceptionNoteNumber() {
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT COUNT(*) as count FROM reception_notes WHERE series = 'NR' AND strftime('%Y', created_at) = strftime('%Y', 'now')";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt("count") + 1;
                    return String.format("%04d", count);
                }
            }
        } catch (SQLException e) {
            logger.error("Error generating reception note number", e);
        }
        
        return "0001";
    }

    /**
     * Obține toate facturile
     */
    public List<Invoice> getAllInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM invoices ORDER BY issue_date DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Invoice invoice = mapRowToInvoice(rs);
                    invoice.setItems(getInvoiceItems(invoice.getId()));
                    invoices.add(invoice);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting all invoices", e);
        }
        
        return invoices;
    }

    /**
     * Obține o factură după ID
     */
    public Invoice getInvoiceById(int id) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM invoices WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Invoice invoice = mapRowToInvoice(rs);
                    invoice.setItems(getInvoiceItems(invoice.getId()));
                    return invoice;
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting invoice by ID", e);
        }
        
        return null;
    }

    /**
     * Obține elementele unei facturi
     */
    private List<InvoiceItem> getInvoiceItems(int invoiceId) {
        List<InvoiceItem> items = new ArrayList<>();
        
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM invoice_items WHERE invoice_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, invoiceId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    items.add(mapRowToInvoiceItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting invoice items", e);
        }
        
        return items;
    }

    /**
     * Obține toate notele de recepție
     */
    public List<ReceptionNote> getAllReceptionNotes() {
        List<ReceptionNote> notes = new ArrayList<>();
        
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM reception_notes ORDER BY reception_date DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    ReceptionNote note = mapRowToReceptionNote(rs);
                    note.setItems(getReceptionNoteItems(note.getId()));
                    notes.add(note);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting all reception notes", e);
        }
        
        return notes;
    }

    /**
     * Obține elementele unei note de recepție
     */
    private List<ReceptionNoteItem> getReceptionNoteItems(int receptionNoteId) {
        List<ReceptionNoteItem> items = new ArrayList<>();
        
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM reception_note_items WHERE reception_note_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, receptionNoteId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    items.add(mapRowToReceptionNoteItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting reception note items", e);
        }
        
        return items;
    }

    // Metode de mapping pentru ResultSet
    private Invoice mapRowToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getInt("id"));
        invoice.setInvoiceNumber(rs.getString("invoice_number"));
        invoice.setSeries(rs.getString("series"));
        invoice.setIssueDate(rs.getDate("issue_date") != null ? rs.getDate("issue_date").toLocalDate() : null);
        invoice.setDueDate(rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null);
        invoice.setSupplierName(rs.getString("supplier_name"));
        invoice.setSupplierCif(rs.getString("supplier_cif"));
        invoice.setSupplierAddress(rs.getString("supplier_address"));
        invoice.setTotalAmount(rs.getDouble("total_amount"));
        invoice.setVatAmount(rs.getDouble("vat_amount"));
        invoice.setCurrency(rs.getString("currency"));
        invoice.setStatus(rs.getString("status"));
        invoice.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        invoice.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        invoice.setXmlContent(rs.getString("xml_content"));
        invoice.setPdfPath(rs.getString("pdf_path"));
        return invoice;
    }

    private InvoiceItem mapRowToInvoiceItem(ResultSet rs) throws SQLException {
        InvoiceItem item = new InvoiceItem();
        item.setId(rs.getInt("id"));
        item.setInvoiceId(rs.getInt("invoice_id"));
        item.setProductName(rs.getString("product_name"));
        item.setProductCode(rs.getString("product_code"));
        item.setDescription(rs.getString("description"));
        item.setQuantity(rs.getDouble("quantity"));
        item.setUnitOfMeasure(rs.getString("unit_of_measure"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        item.setTotalPrice(rs.getDouble("total_price"));
        item.setVatRate(rs.getDouble("vat_rate"));
        item.setVatAmount(rs.getDouble("vat_amount"));
        item.setCategory(rs.getString("category"));
        return item;
    }

    private ReceptionNote mapRowToReceptionNote(ResultSet rs) throws SQLException {
        ReceptionNote note = new ReceptionNote();
        note.setId(rs.getInt("id"));
        note.setNoteNumber(rs.getString("note_number"));
        note.setSeries(rs.getString("series"));
        note.setReceptionDate(rs.getDate("reception_date") != null ? rs.getDate("reception_date").toLocalDate() : null);
        note.setInvoiceDate(rs.getDate("invoice_date") != null ? rs.getDate("invoice_date").toLocalDate() : null);
        note.setInvoiceNumber(rs.getString("invoice_number"));
        note.setSupplierName(rs.getString("supplier_name"));
        note.setSupplierCif(rs.getString("supplier_cif"));
        note.setSupplierAddress(rs.getString("supplier_address"));
        note.setTotalAmount(rs.getDouble("total_amount"));
        note.setVatAmount(rs.getDouble("vat_amount"));
        note.setCurrency(rs.getString("currency"));
        note.setStatus(rs.getString("status"));
        note.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        note.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        note.setInvoiceId(rs.getInt("invoice_id"));
        note.setCreatedBy(rs.getString("created_by"));
        note.setNotes(rs.getString("notes"));
        return note;
    }

    private ReceptionNoteItem mapRowToReceptionNoteItem(ResultSet rs) throws SQLException {
        ReceptionNoteItem item = new ReceptionNoteItem();
        item.setId(rs.getInt("id"));
        item.setReceptionNoteId(rs.getInt("reception_note_id"));
        item.setProductName(rs.getString("product_name"));
        item.setProductCode(rs.getString("product_code"));
        item.setDescription(rs.getString("description"));
        item.setQuantity(rs.getDouble("quantity"));
        item.setUnitOfMeasure(rs.getString("unit_of_measure"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        item.setTotalPrice(rs.getDouble("total_price"));
        item.setVatRate(rs.getDouble("vat_rate"));
        item.setVatAmount(rs.getDouble("vat_amount"));
        item.setCategory(rs.getString("category"));
        item.setReceivedQuantity(rs.getDouble("received_quantity"));
        item.setBatchNumber(rs.getString("batch_number"));
        item.setExpiryDate(rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null);
        item.setStorageLocation(rs.getString("storage_location"));
        return item;
    }
}

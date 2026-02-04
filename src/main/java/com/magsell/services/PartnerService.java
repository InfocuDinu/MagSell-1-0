package com.magsell.services;

import com.magsell.models.Partner;
import com.magsell.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PartnerService {
    private static final Logger logger = LoggerFactory.getLogger(PartnerService.class);
    private final DatabaseService databaseService;
    
    public PartnerService() {
        this.databaseService = DatabaseService.getInstance();
        createTable();
    }
    
    private void createTable() {
        try (Connection conn = databaseService.getConnection()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS partners (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    code TEXT NOT NULL,
                    type TEXT NOT NULL CHECK (type IN ('CLIENT', 'FORNIZOR')),
                    address TEXT,
                    phone TEXT,
                    email TEXT,
                    contact_person TEXT,
                    bank_account TEXT,
                    active BOOLEAN DEFAULT 1,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    created_by TEXT,
                    notes TEXT
                )
                """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                logger.info("Partners table created successfully");
            }
        } catch (SQLException e) {
            logger.error("Error creating partners table", e);
        }
    }
    
    public Partner savePartner(Partner partner) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = """
                INSERT INTO partners (name, code, type, address, phone, email, contact_person, 
                    bank_account, active, created_at, updated_at, created_by, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, partner.getName());
                pstmt.setString(2, partner.getCode());
                pstmt.setString(3, partner.getType());
                pstmt.setString(4, partner.getAddress());
                pstmt.setString(5, partner.getPhone());
                pstmt.setString(6, partner.getEmail());
                pstmt.setString(7, partner.getContactPerson());
                pstmt.setString(8, partner.getBankAccount());
                pstmt.setBoolean(9, partner.isActive());
                pstmt.setString(10, partner.getCreatedAt().toString());
                pstmt.setString(11, partner.getUpdatedAt().toString());
                pstmt.setString(12, partner.getCreatedBy());
                pstmt.setString(13, partner.getNotes());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            partner.setId(generatedKeys.getInt(1));
                        }
                    }
                }
            }
            
            logger.info("Saved partner: {}", partner.getName());
            return partner;
            
        } catch (SQLException e) {
            logger.error("Error saving partner", e);
            return null;
        }
    }
    
    public Partner updatePartner(Partner partner) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = """
                UPDATE partners SET name = ?, code = ?, type = ?, address = ?, phone = ?, 
                    email = ?, contact_person = ?, bank_account = ?, active = ?, 
                    updated_at = ?, notes = ?
                WHERE id = ?
                """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, partner.getName());
                pstmt.setString(2, partner.getCode());
                pstmt.setString(3, partner.getType());
                pstmt.setString(4, partner.getAddress());
                pstmt.setString(5, partner.getPhone());
                pstmt.setString(6, partner.getEmail());
                pstmt.setString(7, partner.getContactPerson());
                pstmt.setString(8, partner.getBankAccount());
                pstmt.setBoolean(9, partner.isActive());
                pstmt.setString(10, LocalDateTime.now().toString());
                pstmt.setString(11, partner.getNotes());
                pstmt.setInt(12, partner.getId());
                
                pstmt.executeUpdate();
            }
            
            logger.info("Updated partner: {}", partner.getName());
            return partner;
            
        } catch (SQLException e) {
            logger.error("Error updating partner", e);
            return null;
        }
    }
    
    public Partner getPartnerById(int id) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM partners WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToPartner(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting partner by ID", e);
        }
        return null;
    }
    
    public List<Partner> getAllPartners() {
        List<Partner> partners = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM partners ORDER BY name";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    partners.add(mapResultSetToPartner(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting all partners", e);
        }
        return partners;
    }
    
    public List<Partner> getPartnersByType(String type) {
        List<Partner> partners = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM partners WHERE type = ? ORDER BY name";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, type);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        partners.add(mapResultSetToPartner(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting partners by type", e);
        }
        return partners;
    }
    
    public List<Partner> getActivePartners() {
        List<Partner> partners = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT * FROM partners WHERE active = 1 ORDER BY name";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    partners.add(mapResultSetToPartner(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting active partners", e);
        }
        return partners;
    }
    
    public boolean deletePartner(int id) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = "UPDATE partners SET active = 0, updated_at = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, LocalDateTime.now().toString());
                pstmt.setInt(2, id);
                int affectedRows = pstmt.executeUpdate();
                
                if (affectedRows > 0) {
                    logger.info("Partner {} marked as inactive", id);
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.error("Error deleting partner", e);
        }
        return false;
    }
    
    public boolean codeExists(String code, int excludeId) {
        try (Connection conn = databaseService.getConnection()) {
            String sql = "SELECT COUNT(*) FROM partners WHERE code = ? AND id != ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, code);
                pstmt.setInt(2, excludeId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking code existence", e);
        }
        return false;
    }
    
    public List<Partner> searchPartners(String searchTerm) {
        List<Partner> partners = new ArrayList<>();
        try (Connection conn = databaseService.getConnection()) {
            String sql = """
                SELECT * FROM partners 
                WHERE (name LIKE ? OR code LIKE ? OR contact_person LIKE ?) 
                AND active = 1 
                ORDER BY name
                """;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                String searchPattern = "%" + searchTerm + "%";
                pstmt.setString(1, searchPattern);
                pstmt.setString(2, searchPattern);
                pstmt.setString(3, searchPattern);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        partners.add(mapResultSetToPartner(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching partners", e);
        }
        return partners;
    }
    
    private Partner mapResultSetToPartner(ResultSet rs) throws SQLException {
        Partner partner = new Partner();
        partner.setId(rs.getInt("id"));
        partner.setName(rs.getString("name"));
        partner.setCode(rs.getString("code"));
        partner.setType(rs.getString("type"));
        partner.setAddress(rs.getString("address"));
        partner.setPhone(rs.getString("phone"));
        partner.setEmail(rs.getString("email"));
        partner.setContactPerson(rs.getString("contact_person"));
        partner.setBankAccount(rs.getString("bank_account"));
        partner.setActive(rs.getBoolean("active"));
        partner.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        partner.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at")));
        partner.setCreatedBy(rs.getString("created_by"));
        partner.setNotes(rs.getString("notes"));
        return partner;
    }
}

package com.magsell.services;

import com.magsell.models.CompanySettings;
import com.magsell.database.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Serviciu pentru managementul setărilor companiei
 */
public class CompanySettingsService {
    private static final Logger logger = LoggerFactory.getLogger(CompanySettingsService.class);
    
    private final DatabaseService databaseService;
    
    public CompanySettingsService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    /**
     * Obține setările companiei active
     */
    public CompanySettings getCompanySettings() throws Exception {
        String sql = "SELECT * FROM company_settings WHERE is_active = 1 LIMIT 1";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return mapResultSetToCompanySettings(rs);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting company settings", e);
        }
        
        return null;
    }
    
    /**
     * Salvează setările companiei
     */
    public void saveCompanySettings(CompanySettings settings) throws Exception {
        String sql = "INSERT OR REPLACE INTO company_settings " +
                    "(id, company_name, cui, reg_com, address, city, county, country, " +
                    "phone, email, bank_account, bank_name, capital_social, vat_payer, " +
                    "logo_path, is_active, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (settings.getId() != null) {
                stmt.setInt(1, settings.getId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            
            stmt.setString(2, settings.getCompanyName());
            stmt.setString(3, settings.getCui());
            stmt.setString(4, settings.getRegCom());
            stmt.setString(5, settings.getAddress());
            stmt.setString(6, settings.getCity());
            stmt.setString(7, settings.getCounty());
            stmt.setString(8, settings.getCountry());
            stmt.setString(9, settings.getPhone());
            stmt.setString(10, settings.getEmail());
            stmt.setString(11, settings.getBankAccount());
            stmt.setString(12, settings.getBankName());
            
            if (settings.getCapitalSocial() != null) {
                stmt.setDouble(13, settings.getCapitalSocial());
            } else {
                stmt.setNull(13, Types.DOUBLE);
            }
            
            stmt.setBoolean(14, settings.getVatPayer());
            stmt.setString(15, settings.getLogoPath());
            stmt.setBoolean(16, settings.getIsActive());
            stmt.setTimestamp(17, Timestamp.valueOf(settings.getCreatedAt()));
            stmt.setTimestamp(18, Timestamp.valueOf(LocalDateTime.now()));
            
            stmt.executeUpdate();
            
            logger.info("Company settings saved successfully");
            
        } catch (SQLException e) {
            logger.error("Error saving company settings", e);
            throw new Exception("Eroare la salvarea setărilor companiei: " + e.getMessage(), e);
        }
    }
    
    private CompanySettings mapResultSetToCompanySettings(ResultSet rs) throws SQLException {
        CompanySettings settings = new CompanySettings();
        settings.setId(rs.getInt("id"));
        settings.setCompanyName(rs.getString("company_name"));
        settings.setCui(rs.getString("cui"));
        settings.setRegCom(rs.getString("reg_com"));
        settings.setAddress(rs.getString("address"));
        settings.setCity(rs.getString("city"));
        settings.setCounty(rs.getString("county"));
        settings.setCountry(rs.getString("country"));
        settings.setPhone(rs.getString("phone"));
        settings.setEmail(rs.getString("email"));
        settings.setBankAccount(rs.getString("bank_account"));
        settings.setBankName(rs.getString("bank_name"));
        
        double capitalSocial = rs.getDouble("capital_social");
        if (!rs.wasNull()) {
            settings.setCapitalSocial(capitalSocial);
        }
        
        settings.setVatPayer(rs.getBoolean("vat_payer"));
        settings.setLogoPath(rs.getString("logo_path"));
        settings.setIsActive(rs.getBoolean("is_active"));
        settings.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        settings.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        return settings;
    }
}

package com.magsell.services;

import com.magsell.database.DatabaseService;
import com.magsell.models.Sale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviciu pentru operații cu vânzări.
 */
public class SalesService {
    private static final Logger logger = LoggerFactory.getLogger(SalesService.class);
    private final DatabaseService dbService = DatabaseService.getInstance();

    /**
     * Creează o vânzare nouă.
     */
    public void createSale(Sale sale) throws SQLException {
        String sql = "INSERT INTO sales (product_id, product_name, quantity, unit_price, total_price, sale_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sale.getProductId());
            pstmt.setString(2, sale.getProductName());
            pstmt.setInt(3, sale.getQuantity());
            pstmt.setBigDecimal(4, sale.getUnitPrice());
            pstmt.setBigDecimal(5, sale.getTotalPrice());
            pstmt.setTimestamp(6, java.sql.Timestamp.valueOf(sale.getSaleDate() != null ? sale.getSaleDate() : LocalDateTime.now()));
            pstmt.setString(7, sale.getNotes());

            pstmt.executeUpdate();
            logger.info("Vânzare creată: " + sale.getProductName() + " x" + sale.getQuantity());
        }
    }

    /**
     * Creează multiple vânzări (pentru un coș complet).
     */
    public void createSales(List<Sale> sales) throws SQLException {
        String sql = "INSERT INTO sales (product_id, product_name, quantity, unit_price, total_price, sale_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Sale sale : sales) {
                pstmt.setInt(1, sale.getProductId());
                pstmt.setString(2, sale.getProductName());
                pstmt.setInt(3, sale.getQuantity());
                pstmt.setBigDecimal(4, sale.getUnitPrice());
                pstmt.setBigDecimal(5, sale.getTotalPrice());
                pstmt.setTimestamp(6, java.sql.Timestamp.valueOf(sale.getSaleDate() != null ? sale.getSaleDate() : LocalDateTime.now()));
                pstmt.setString(7, sale.getNotes());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            logger.info("Creat " + sales.size() + " vânzări");
        }
    }

    /**
     * Obține toate vânzările.
     */
    public List<Sale> getAllSales() throws SQLException {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales ORDER BY sale_date DESC";

        try (Connection conn = dbService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                sales.add(mapRowToSale(rs));
            }
        }
        return sales;
    }

    /**
     * Obține vânzările pentru o zi specifică.
     */
    public List<Sale> getSalesByDate(LocalDate date) throws SQLException {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT * FROM sales WHERE DATE(sale_date) = ? ORDER BY sale_date DESC";

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(date));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapRowToSale(rs));
                }
            }
        }
        return sales;
    }

    /**
     * Obține vânzările pentru astăzi.
     */
    public List<Sale> getTodaySales() throws SQLException {
        return getSalesByDate(LocalDate.now());
    }

    /**
     * Calculează totalul vânzărilor pentru astăzi.
     */
    public BigDecimal getTodayTotal() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_price), 0) as total FROM sales WHERE DATE(sale_date) = DATE('now')";

        try (Connection conn = dbService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Obține cele mai vândute produse (top N).
     */
    public Map<String, Integer> getTopSellingProducts(int limit) throws SQLException {
        Map<String, Integer> topProducts = new HashMap<>();
        String sql = """
            SELECT product_name, SUM(quantity) as total_quantity 
            FROM sales 
            GROUP BY product_name 
            ORDER BY total_quantity DESC 
            LIMIT ?
            """;

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topProducts.put(rs.getString("product_name"), rs.getInt("total_quantity"));
                }
            }
        }
        return topProducts;
    }

    /**
     * Obține statistici de vânzări pentru o perioadă.
     */
    public Map<String, Object> getSalesStatistics(LocalDate startDate, LocalDate endDate) throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        String sql = """
            SELECT 
                COUNT(*) as total_sales,
                SUM(total_price) as total_revenue,
                SUM(quantity) as total_items_sold
            FROM sales 
            WHERE DATE(sale_date) BETWEEN ? AND ?
            """;

        try (Connection conn = dbService.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalSales", rs.getInt("total_sales"));
                    stats.put("totalRevenue", rs.getBigDecimal("total_revenue"));
                    stats.put("totalItemsSold", rs.getInt("total_items_sold"));
                }
            }
        }
        return stats;
    }

    /**
     * Mapează o linie din rezultatul query-ului la un obiect Sale.
     */
    private Sale mapRowToSale(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setId(rs.getInt("id"));
        sale.setProductId(rs.getInt("product_id"));
        sale.setProductName(rs.getString("product_name"));
        sale.setQuantity(rs.getInt("quantity"));
        sale.setUnitPrice(rs.getBigDecimal("unit_price"));
        sale.setTotalPrice(rs.getBigDecimal("total_price"));
        sale.setNotes(rs.getString("notes"));
        try {
            sale.setSaleDate(rs.getTimestamp("sale_date").toLocalDateTime());
        } catch (Exception ignored) {
        }
        return sale;
    }
}

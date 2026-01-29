package com.magsell.ui.controllers;

import com.magsell.models.Product;
import com.magsell.models.Sale;
import com.magsell.services.ProductService;
import com.magsell.services.SalesService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller pentru Dashboard-ul aplicației.
 * Afișează statistici rapide: vânzări azi, produse cu stoc critic, cele mai vândute produse.
 */
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final ProductService productService = new ProductService();
    private final SalesService salesService = new SalesService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ro", "RO"));

    @FXML
    private Label todaySalesLabel;
    @FXML
    private Label todayRevenueLabel;
    @FXML
    private Label lowStockCountLabel;
    @FXML
    private Label totalProductsLabel;
    @FXML
    private ListView<String> topProductsList;
    @FXML
    private ListView<String> lowStockList;

    @FXML
    public void initialize() {
        logger.info("Initializing DashboardController");
        loadDashboardData();
    }

    /**
     * Încarcă toate datele pentru dashboard.
     */
    public void loadDashboardData() {
        new Thread(() -> {
            try {
                // Încarcă statisticile
                BigDecimal todayTotal = salesService.getTodayTotal();
                List<Sale> todaySales = salesService.getTodaySales();
                List<Product> allProducts = productService.getAllProducts();
                List<Product> lowStockProducts = allProducts.stream()
                        .filter(Product::isLowStock)
                        .toList();
                Map<String, Integer> topProducts = salesService.getTopSellingProducts(5);

                // Actualizează UI pe thread-ul JavaFX
                Platform.runLater(() -> {
                    updateTodayStats(todaySales.size(), todayTotal);
                    updateProductStats(allProducts.size(), lowStockProducts.size());
                    updateTopProducts(topProducts);
                    updateLowStockProducts(lowStockProducts);
                });
            } catch (SQLException e) {
                logger.error("Eroare la încărcarea datelor dashboard", e);
                Platform.runLater(() -> {
                    todaySalesLabel.setText("Eroare");
                    todayRevenueLabel.setText("Eroare");
                });
            }
        }).start();
    }

    /**
     * Actualizează statisticile pentru astăzi.
     */
    private void updateTodayStats(int salesCount, BigDecimal revenue) {
        todaySalesLabel.setText(String.valueOf(salesCount));
        todayRevenueLabel.setText(formatCurrency(revenue));
    }

    /**
     * Actualizează statisticile pentru produse.
     */
    private void updateProductStats(int totalProducts, int lowStockCount) {
        totalProductsLabel.setText(String.valueOf(totalProducts));
        lowStockCountLabel.setText(String.valueOf(lowStockCount));
    }

    /**
     * Actualizează lista cu cele mai vândute produse.
     */
    private void updateTopProducts(Map<String, Integer> topProducts) {
        ObservableList<String> items = FXCollections.observableArrayList();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : topProducts.entrySet()) {
            items.add(rank + ". " + entry.getKey() + " - " + entry.getValue() + " buc.");
            rank++;
        }
        if (items.isEmpty()) {
            items.add("Nu există date de vânzări încă");
        }
        topProductsList.setItems(items);
    }

    /**
     * Actualizează lista cu produse cu stoc critic.
     */
    private void updateLowStockProducts(List<Product> lowStockProducts) {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Product product : lowStockProducts) {
            items.add(product.getName() + " - " + product.getQuantity() + " buc. rămase");
        }
        if (items.isEmpty()) {
            items.add("Toate produsele au stoc suficient");
        }
        lowStockList.setItems(items);
    }

    /**
     * Formatează o sumă ca monedă.
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0,00 RON";
        }
        return String.format("%.2f RON", amount.doubleValue());
    }

    /**
     * Reîmprospătează datele dashboard-ului.
     */
    @FXML
    public void handleRefresh() {
        loadDashboardData();
    }
}

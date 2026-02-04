package com.magsell.ui.controllers;

import com.magsell.models.Product;
import com.magsell.models.Sale;
import com.magsell.models.ProductionOrder;
import com.magsell.services.ProductService;
import com.magsell.services.SalesService;
import com.magsell.services.ProductionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller pentru Dashboard în noul design modern
 */
public class DashboardViewController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardViewController.class);
    
    private final ProductService productService;
    private final SalesService salesService;
    private final ProductionService productionService;
    
    private final ObservableList<Sale> recentSalesList;
    private final ObservableList<Product> lowStockList;
    
    // Statistics Labels
    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label todaySalesLabel;
    @FXML
    private Label lowStockLabel;
    @FXML
    private Label activeOrdersLabel;
    @FXML
    private Label lastUpdateLabel;
    
    // Tables
    @FXML
    private TableView<Sale> recentSalesTable;
    @FXML
    private TableColumn<Sale, LocalDateTime> saleDateColumn;
    @FXML
    private TableColumn<Sale, String> saleProductColumn;
    @FXML
    private TableColumn<Sale, Integer> saleQuantityColumn;
    @FXML
    private TableColumn<Sale, Double> saleAmountColumn;
    
    @FXML
    private TableView<Product> lowStockTable;
    @FXML
    private TableColumn<Product, String> stockProductColumn;
    @FXML
    private TableColumn<Product, Integer> stockCurrentColumn;
    @FXML
    private TableColumn<Product, Integer> stockMinColumn;
    @FXML
    private TableColumn<Product, String> stockStatusColumn;
    
    public DashboardViewController() {
        this.productService = new ProductService();
        this.salesService = new SalesService();
        this.productionService = new ProductionService();
        this.recentSalesList = FXCollections.observableArrayList();
        this.lowStockList = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing DashboardViewController");
        
        setupTables();
        loadDashboardData();
        
        logger.info("DashboardViewController initialized successfully");
    }
    
    private void setupTables() {
        // Recent Sales Table
        saleDateColumn.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        saleProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        saleQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        saleAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        
        // Custom cell factory for date formatting
        saleDateColumn.setCellFactory(column -> new TableCell<Sale, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText("");
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd.MM HH:mm")));
                }
            }
        });
        
        recentSalesTable.setItems(recentSalesList);
        
        // Low Stock Table
        stockProductColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        stockCurrentColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        stockMinColumn.setCellValueFactory(cellData -> {
            // Use a default minimum stock of 10
            return javafx.beans.binding.Bindings.createObjectBinding(() -> 10);
        });
        
        // Custom cell factory for stock status
        stockStatusColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty) {
                    setText("");
                    setStyle("");
                } else {
                    Product product = getTableView().getItems().get(getIndex());
                    int currentStock = product.getQuantity();
                    int minStock = 10; // Default minimum stock
                    
                    if (currentStock <= 0) {
                        setText("Epuizat");
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                    } else if (currentStock <= minStock) {
                        setText("Scăzut");
                        setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;");
                    } else {
                        setText("Normal");
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        lowStockTable.setItems(lowStockList);
    }
    
    private void loadDashboardData() {
        try {
            // Load products
            List<Product> allProducts = productService.getAllProducts();
            totalProductsLabel.setText(String.valueOf(allProducts.size()));
            
            // Load today's sales
            List<Sale> todaySales;
            try {
                todaySales = salesService.getTodaySales();
            } catch (Exception e) {
                logger.error("Error loading today's sales", e);
                todaySales = List.of(); // Empty list on error
            }
            todaySalesLabel.setText(String.valueOf(todaySales.size()));
            
            // Calculate today's total
            double todayTotal = todaySales.stream()
                .mapToDouble(sale -> sale.getTotalPrice().doubleValue())
                .sum();
            // You might want to show this instead of count
            // todaySalesLabel.setText(String.format("%.2f RON", todayTotal));
            
            // Load low stock products
            List<Product> lowStockProducts = allProducts.stream()
                .filter(product -> product.getQuantity() <= 10)
                .toList();
            lowStockLabel.setText(String.valueOf(lowStockProducts.size()));
            
            // Load active production orders
            List<ProductionOrder> activeOrders = productionService.getAllProductionOrders().stream()
                .filter(order -> "pending".equals(order.getStatus()) || "in_progress".equals(order.getStatus()))
                .toList();
            activeOrdersLabel.setText(String.valueOf(activeOrders.size()));
            
            // Populate tables
            recentSalesList.clear();
            recentSalesList.addAll(todaySales.stream().limit(10).toList());
            
            lowStockList.clear();
            lowStockList.addAll(lowStockProducts);
            
            // Update last update time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            lastUpdateLabel.setText("📊 Ultima actualizare: " + LocalDateTime.now().format(formatter));
            
            logger.info("Dashboard data loaded successfully");
            
        } catch (Exception e) {
            logger.error("Error loading dashboard data", e);
            // Set default values on error
            totalProductsLabel.setText("0");
            todaySalesLabel.setText("0");
            lowStockLabel.setText("0");
            activeOrdersLabel.setText("0");
        }
    }
    
    @FXML
    private void refreshDashboard() {
        logger.info("Refreshing dashboard data");
        loadDashboardData();
    }
}

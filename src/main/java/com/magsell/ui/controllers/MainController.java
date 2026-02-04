package com.magsell.ui.controllers;

import com.magsell.App;
import com.magsell.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller principal pentru layout-ul modern cu sidebar navigare
 */
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    @FXML
    private StackPane contentArea;
    
    @FXML
    private Label userLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label dateTimeLabel;
    
    // Cache pentru view-urile încărcate
    private final Map<String, Parent> viewCache = new HashMap<>();
    
    // View-uri disponibile
    private static final String DASHBOARD_VIEW = "/com/magsell/ui/fxml/views/DashboardView.fxml";
    private static final String PRODUCTS_VIEW = "/com/magsell/ui/fxml/views/ProductsView.fxml";
    private static final String CATEGORIES_VIEW = "/com/magsell/ui/fxml/views/CategoriesView.fxml";
    private static final String PARTNERS_VIEW = "/com/magsell/ui/fxml/views/PartnersView.fxml";
    private static final String USERS_VIEW = "/com/magsell/ui/fxml/views/UsersView.fxml";
    private static final String NIR_VIEW = "/com/magsell/ui/fxml/views/NIRView.fxml";
    private static final String INVOICES_VIEW = "/com/magsell/ui/fxml/views/InvoicesView.fxml";
    private static final String RECIPES_VIEW = "/com/magsell/ui/fxml/views/RecipesView.fxml";
    private static final String PRODUCTION_VIEW = "/com/magsell/ui/fxml/views/ProductionView.fxml";
    private static final String POS_VIEW = "/com/magsell/ui/fxml/views/POSView.fxml";
    private static final String SALES_VIEW = "/com/magsell/ui/fxml/views/SalesView.fxml";
    private static final String SALES_REPORT_VIEW = "/com/magsell/ui/fxml/views/SalesReportView.fxml";
    private static final String INVENTORY_VIEW = "/com/magsell/ui/fxml/views/InventoryView.fxml";
    private static final String FINANCIAL_VIEW = "/com/magsell/ui/fxml/views/FinancialView.fxml";
    
    @FXML
    public void initialize() {
        logger.info("Initializing MainController");
        
        // Setup user info
        setupUserInfo();
        
        // Setup status bar
        setupStatusBar();
        
        // Load default view (Dashboard)
        loadView(DASHBOARD_VIEW);
        
        // Start clock update
        startClockUpdate();
        
        logger.info("MainController initialized successfully");
    }
    
    private void setupUserInfo() {
        User currentUser = App.getCurrentUser();
        if (currentUser != null) {
            userLabel.setText("Utilizator: " + currentUser.getUsername() + 
                             " (" + currentUser.getRole() + ")");
        } else {
            userLabel.setText("Utilizator: Guest");
        }
    }
    
    private void setupStatusBar() {
        statusLabel.setText("Sistem gata");
        updateDateTime();
    }
    
    private void startClockUpdate() {
        // Update clock every second
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> updateDateTime());
            }
        }, 0, 1000);
    }
    
    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        dateTimeLabel.setText(now.format(formatter));
    }
    
    /**
     * Încarcă un view în zona centrală de content
     */
    private void loadView(String viewPath) {
        try {
            // Verificăm cache-ul
            Parent view = viewCache.get(viewPath);
            if (view == null) {
                logger.info("Loading view: {}", viewPath);
                FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
                view = loader.load();
                viewCache.put(viewPath, view);
                logger.info("View cached: {}", viewPath);
            } else {
                logger.info("Using cached view: {}", viewPath);
            }
            
            // Clear content area and add new view
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
            // Update status
            String viewName = getViewDisplayName(viewPath);
            statusLabel.setText("Modul: " + viewName);
            
        } catch (IOException e) {
            logger.error("Error loading view: " + viewPath, e);
            showErrorView("Nu s-a putut încărca modulul: " + viewPath);
        } catch (Exception e) {
            logger.error("Unexpected error loading view: " + viewPath, e);
            showErrorView("Eroare neașteptată la încărcarea modulului: " + viewPath);
        }
    }
    
    /**
     * Afișează o eroare în zona de content
     */
    private void showErrorView(String errorMessage) {
        Label errorLabel = new Label(errorMessage);
        errorLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 16px; -fx-font-weight: bold;");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(errorLabel);
        statusLabel.setText("Eroare");
    }
    
    /**
     * Returnează numele afișabil al view-ului
     */
    private String getViewDisplayName(String viewPath) {
        if (viewPath.contains("Dashboard")) return "Dashboard";
        if (viewPath.contains("Products")) return "Produse";
        if (viewPath.contains("Categories")) return "Categorii";
        if (viewPath.contains("Partners")) return "Parteneri";
        if (viewPath.contains("Users")) return "Utilizatori";
        if (viewPath.contains("NIR")) return "NIR";
        if (viewPath.contains("Invoices")) return "Facturi";
        if (viewPath.contains("Recipes")) return "Rețetar";
        if (viewPath.contains("Production")) return "Producție";
        if (viewPath.contains("POS")) return "POS Vânzări";
        if (viewPath.contains("Sales")) return "Vânzări";
        if (viewPath.contains("SalesReport")) return "Raport Vânzări";
        if (viewPath.contains("Inventory")) return "Stocuri";
        if (viewPath.contains("Financial")) return "Financiar";
        return "Modul Necunoscut";
    }
    
    // === NAVIGATION HANDLERS ===
    
    @FXML
    private void navigateToDashboard() {
        loadView(DASHBOARD_VIEW);
    }
    
    @FXML
    private void navigateToProducts() {
        loadView(PRODUCTS_VIEW);
    }
    
    @FXML
    private void navigateToCategories() {
        loadView(CATEGORIES_VIEW);
    }
    
    @FXML
    private void navigateToPartners() {
        loadView(PARTNERS_VIEW);
    }
    
    @FXML
    private void navigateToUsers() {
        loadView(USERS_VIEW);
    }
    
    @FXML
    private void navigateToNIR() {
        loadView(NIR_VIEW);
    }
    
    @FXML
    private void navigateToInvoices() {
        loadView(INVOICES_VIEW);
    }
    
    @FXML
    private void navigateToRecipes() {
        loadView(RECIPES_VIEW);
    }
    
    @FXML
    private void navigateToProduction() {
        loadView(PRODUCTION_VIEW);
    }
    
    @FXML
    private void navigateToPOS() {
        loadView(POS_VIEW);
    }
    
    @FXML
    private void navigateToSales() {
        loadView(SALES_VIEW);
    }
    
    @FXML
    private void navigateToSalesReport() {
        loadView(SALES_REPORT_VIEW);
    }
    
    @FXML
    private void navigateToInventory() {
        loadView(INVENTORY_VIEW);
    }
    
    @FXML
    private void navigateToFinancial() {
        loadView(FINANCIAL_VIEW);
    }
    
    @FXML
    private void handleLogout() {
        try {
            logger.info("User logging out");
            
            // Clear current user
            App.setCurrentUser(null);
            
            // Close current window
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.close();
            
            // Open login window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/magsell/ui/fxml/LoginWindow.fxml"));
            Parent root = loader.load();
            
            Stage loginStage = new Stage();
            loginStage.setTitle("MagSell ERP - Autentificare");
            loginStage.setScene(new javafx.scene.Scene(root, 400, 300));
            loginStage.setResizable(false);
            loginStage.show();
            
        } catch (IOException e) {
            logger.error("Error during logout", e);
            showErrorView("Eroare la delogare: " + e.getMessage());
        }
    }
    
    /**
     * Metodă pentru a reîncărca un view (utilă pentru refresh)
     */
    public void refreshCurrentView() {
        // Remove from cache to force reload
        String currentView = getCurrentViewPath();
        if (currentView != null) {
            viewCache.remove(currentView);
            loadView(currentView);
        }
    }
    
    /**
     * Returnează calea view-ului curent (dacă poate fi determinat)
     */
    private String getCurrentViewPath() {
        // This is a simplified implementation
        // In a real scenario, you might want to track the current view
        return DASHBOARD_VIEW; // Default fallback
    }
    
    /**
     * Clear cache for a specific view
     */
    public void clearViewCache(String viewPath) {
        viewCache.remove(viewPath);
    }
    
    /**
     * Clear all view cache
     */
    public void clearAllViewCache() {
        viewCache.clear();
        logger.info("All view cache cleared");
    }
}

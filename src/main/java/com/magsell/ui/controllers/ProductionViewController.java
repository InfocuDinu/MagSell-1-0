package com.magsell.ui.controllers;

import com.magsell.models.Recipe;
import com.magsell.models.ProductionOrder;
import com.magsell.models.Product;
import com.magsell.services.ProductionService;
import com.magsell.services.ProductService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller pentru modul Producție în noul design modern
 */
public class ProductionViewController {
    private static final Logger logger = LoggerFactory.getLogger(ProductionViewController.class);
    
    private final ProductionService productionService;
    private final ProductService productService;
    private final ObservableList<Recipe> recipeList;
    private final ObservableList<ProductionOrder> productionOrderList;
    private final ObservableList<Product> stockList;
    
    // Tab Rețete
    @FXML
    private TableView<Recipe> recipesTableView;
    @FXML
    private TableColumn<Recipe, Integer> recipeIdColumn;
    @FXML
    private TableColumn<Recipe, String> recipeNameColumn;
    @FXML
    private TableColumn<Recipe, List<com.magsell.models.RecipeIngredient>> recipeIngredientsColumn;
    @FXML
    private TableColumn<Recipe, String> recipeCreatedByColumn;
    @FXML
    private TableColumn<Recipe, LocalDateTime> recipeCreatedAtColumn;
    @FXML
    private TableColumn<Recipe, Void> recipeActionsColumn;
    
    // Tab Comenzi Producție
    @FXML
    private TableView<ProductionOrder> productionOrdersTableView;
    @FXML
    private TableColumn<ProductionOrder, Integer> orderIdColumn;
    @FXML
    private TableColumn<ProductionOrder, String> orderRecipeColumn;
    @FXML
    private TableColumn<ProductionOrder, Double> orderQuantityColumn;
    @FXML
    private TableColumn<ProductionOrder, String> orderStatusColumn;
    @FXML
    private TableColumn<ProductionOrder, LocalDateTime> orderCreatedAtColumn;
    @FXML
    private TableColumn<ProductionOrder, String> orderCreatedByColumn;
    @FXML
    private TableColumn<ProductionOrder, Void> orderActionsColumn;
    
    // Tab Stocuri
    @FXML
    private TableView<Product> stockTableView;
    @FXML
    private TableColumn<Product, Integer> stockIdColumn;
    @FXML
    private TableColumn<Product, String> stockNameColumn;
    @FXML
    private TableColumn<Product, String> stockTypeColumn;
    @FXML
    private TableColumn<Product, Double> stockCurrentColumn;
    @FXML
    private TableColumn<Product, String> stockUnitColumn;
    @FXML
    private TableColumn<Product, Double> stockMinColumn;
    @FXML
    private TableColumn<Product, String> stockStatusColumn;
    @FXML
    private TableColumn<Product, Void> stockActionsColumn;
    
    // Status Labels
    @FXML
    private Label totalRecipesLabel;
    @FXML
    private Label activeOrdersLabel;
    @FXML
    private Label lastUpdateLabel;
    
    public ProductionViewController() {
        this.productionService = new ProductionService();
        this.productService = new ProductService();
        this.recipeList = FXCollections.observableArrayList();
        this.productionOrderList = FXCollections.observableArrayList();
        this.stockList = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing ProductionViewController");
        
        setupRecipesTable();
        setupProductionOrdersTable();
        setupStockTable();
        
        loadData();
        
        logger.info("ProductionViewController initialized successfully");
    }
    
    private void setupRecipesTable() {
        recipeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        recipeNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        recipeCreatedByColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        recipeCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Custom cell factory pentru ingrediente
        recipeIngredientsColumn.setCellValueFactory(new PropertyValueFactory<>("ingredients"));
        recipeIngredientsColumn.setCellFactory(column -> new TableCell<Recipe, List<com.magsell.models.RecipeIngredient>>() {
            @Override
            protected void updateItem(List<com.magsell.models.RecipeIngredient> items, boolean empty) {
                super.updateItem(items, empty);
                if (empty || items == null || items.isEmpty()) {
                    setText("Niciun ingredient");
                } else {
                    String ingredientsText = items.stream()
                        .map(item -> item.getProductName() + " (" + item.getQuantity() + " " + item.getUnitOfMeasure() + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                    setText(ingredientsText);
                }
            }
        });
        
        // Custom cell factory pentru acțiuni
        recipeActionsColumn.setCellFactory(column -> new TableCell<Recipe, Void>() {
            private final Button viewButton = new Button("👁️");
            private final Button editButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");
            private final HBox buttons = new HBox(5, viewButton, editButton, deleteButton);
            
            {
                viewButton.setOnAction(e -> viewRecipe(getTableView().getItems().get(getIndex())));
                editButton.setOnAction(e -> editRecipe(getTableView().getItems().get(getIndex())));
                deleteButton.setOnAction(e -> deleteRecipe(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
        
        recipesTableView.setItems(recipeList);
    }
    
    private void setupProductionOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderRecipeColumn.setCellValueFactory(new PropertyValueFactory<>("recipeName"));
        orderQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityToProduce"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        orderCreatedByColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        
        // Custom cell factory pentru status cu culori
        orderStatusColumn.setCellFactory(column -> new TableCell<ProductionOrder, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "pending":
                            setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;");
                            break;
                        case "in_progress":
                            setStyle("-fx-text-fill: #3498DB; -fx-font-weight: bold;");
                            break;
                        case "completed":
                            setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                            break;
                        case "cancelled":
                            setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });
        
        // Custom cell factory pentru acțiuni
        orderActionsColumn.setCellFactory(column -> new TableCell<ProductionOrder, Void>() {
            private final Button viewButton = new Button("👁️");
            private final Button processButton = new Button("⚙️");
            private final Button cancelButton = new Button("❌");
            private final HBox buttons = new HBox(5, viewButton, processButton, cancelButton);
            
            {
                viewButton.setOnAction(e -> viewProductionOrder(getTableView().getItems().get(getIndex())));
                processButton.setOnAction(e -> processProductionOrder(getTableView().getItems().get(getIndex())));
                cancelButton.setOnAction(e -> cancelProductionOrder(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ProductionOrder order = getTableView().getItems().get(getIndex());
                    // Disable process button for completed/cancelled orders
                    processButton.setDisable("completed".equals(order.getStatus()) || "cancelled".equals(order.getStatus()));
                    cancelButton.setDisable("completed".equals(order.getStatus()) || "cancelled".equals(order.getStatus()));
                    setGraphic(buttons);
                }
            }
        });
        
        productionOrdersTableView.setItems(productionOrderList);
    }
    
    private void setupStockTable() {
        stockIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        stockNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        stockTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        stockCurrentColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        stockUnitColumn.setCellValueFactory(new PropertyValueFactory<>("unitOfMeasure"));
        stockMinColumn.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        
        // Custom cell factory pentru status stoc
        stockStatusColumn.setCellFactory(column -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty) {
                    setText("");
                    setStyle("");
                } else {
                    Product product = getTableView().getItems().get(getIndex());
                    double currentStock = product.getQuantity();
                    double minStock = 10; // Default minimum stock
                    
                    if (currentStock <= 0) {
                        setText("Stoc epuizat");
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                    } else if (currentStock <= minStock) {
                        setText("Stoc minim");
                        setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;");
                    } else {
                        setText("Stoc OK");
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        // Custom cell factory pentru acțiuni
        stockActionsColumn.setCellFactory(column -> new TableCell<Product, Void>() {
            private final Button adjustButton = new Button("📊");
            private final Button historyButton = new Button("📜");
            private final HBox buttons = new HBox(5, adjustButton, historyButton);
            
            {
                adjustButton.setOnAction(e -> adjustStock(getTableView().getItems().get(getIndex())));
                historyButton.setOnAction(e -> viewStockHistory(getTableView().getItems().get(getIndex())));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
        
        stockTableView.setItems(stockList);
    }
    
    private void loadData() {
        loadRecipes();
        loadProductionOrders();
        loadStock();
        updateStatusLabels();
    }
    
    private void loadRecipes() {
        try {
            recipeList.clear();
            recipeList.addAll(productionService.getAllRecipes());
            logger.info("Loaded {} recipes", recipeList.size());
        } catch (Exception e) {
            logger.error("Error loading recipes", e);
        }
    }
    
    private void loadProductionOrders() {
        try {
            productionOrderList.clear();
            productionOrderList.addAll(productionService.getAllProductionOrders());
            logger.info("Loaded {} production orders", productionOrderList.size());
        } catch (Exception e) {
            logger.error("Error loading production orders", e);
        }
    }
    
    private void loadStock() {
        try {
            stockList.clear();
            stockList.addAll(productService.getAllProducts());
            logger.info("Loaded {} products for stock", stockList.size());
        } catch (Exception e) {
            logger.error("Error loading stock", e);
        }
    }
    
    private void updateStatusLabels() {
        totalRecipesLabel.setText("📊 Total rețete: " + recipeList.size());
        
        long activeOrders = productionOrderList.stream()
            .filter(order -> "pending".equals(order.getStatus()) || "in_progress".equals(order.getStatus()))
            .count();
        activeOrdersLabel.setText("⚙️ Comenzi active: " + activeOrders);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        lastUpdateLabel.setText("Ultima actualizare: " + LocalDateTime.now().format(formatter));
    }
    
    // === ACTION HANDLERS ===
    
    @FXML
    private void createProductionOrder() {
        // TODO: Implement dialog for creating production order
        logger.info("Create production order clicked");
    }
    
    @FXML
    private void createRecipe() {
        // TODO: Implement dialog for creating recipe
        logger.info("Create recipe clicked");
    }
    
    @FXML
    private void refreshData() {
        loadData();
        logger.info("Data refreshed");
    }
    
    @FXML
    private void generateStockReport() {
        // TODO: Implement stock report generation
        logger.info("Generate stock report clicked");
    }
    
    // === RECIPE ACTIONS ===
    
    private void viewRecipe(Recipe recipe) {
        // TODO: Implement recipe view dialog
        logger.info("View recipe: {}", recipe.getProductName());
    }
    
    private void editRecipe(Recipe recipe) {
        // TODO: Implement recipe edit dialog
        logger.info("Edit recipe: {}", recipe.getProductName());
    }
    
    private void deleteRecipe(Recipe recipe) {
        // TODO: Implement recipe deletion with confirmation
        logger.info("Delete recipe: {}", recipe.getProductName());
    }
    
    // === PRODUCTION ORDER ACTIONS ===
    
    private void viewProductionOrder(ProductionOrder order) {
        // TODO: Implement production order view dialog
        logger.info("View production order: {}", order.getId());
    }
    
    private void processProductionOrder(ProductionOrder order) {
        try {
            productionService.processProduction(order);
            loadProductionOrders();
            loadStock();
            updateStatusLabels();
            logger.info("Production order {} processed successfully", order.getId());
            // TODO: Show success alert
        } catch (Exception e) {
            logger.error("Error processing production order", e);
            // TODO: Show error alert
        }
    }
    
    private void cancelProductionOrder(ProductionOrder order) {
        // TODO: Implement production order cancellation
        logger.info("Cancel production order: {}", order.getId());
    }
    
    // === STOCK ACTIONS ===
    
    private void adjustStock(Product product) {
        // TODO: Implement stock adjustment dialog
        logger.info("Adjust stock for: {}", product.getName());
    }
    
    private void viewStockHistory(Product product) {
        // TODO: Implement stock history view
        logger.info("View stock history for: {}", product.getName());
    }
}

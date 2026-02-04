package com.magsell.ui.controllers;

import com.magsell.models.*;
import com.magsell.services.ProductionService;
import com.magsell.exceptions.InsufficientStockException;
import com.magsell.App;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.util.StringConverter;
import javafx.scene.control.ButtonBar.ButtonData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller pentru managementul producției
 */
public class ProductionController {
    private static final Logger logger = LoggerFactory.getLogger(ProductionController.class);
    
    private final ProductionService productionService;
    private final ObservableList<Recipe> recipeList;
    private final ObservableList<ProductionOrder> orderList;
    
    // Butoane Principale
    @FXML
    private Button addRecipeButton;
    @FXML
    private Button createOrderButton;
    @FXML
    private Button refreshButton;
    
    // Tabel Rețete
    @FXML
    private TableView<Recipe> recipesTableView;
    @FXML
    private TableColumn<Recipe, Integer> recipeIdColumn;
    @FXML
    private TableColumn<Recipe, String> recipeNameColumn;
    @FXML
    private TableColumn<Recipe, List<RecipeIngredient>> recipeIngredientsColumn;
    @FXML
    private TableColumn<Recipe, String> recipeCreatedByColumn;
    @FXML
    private TableColumn<Recipe, LocalDateTime> recipeCreatedAtColumn;
    
    // Butoane Rețete
    @FXML
    private Button viewRecipeButton;
    @FXML
    private Button editRecipeButton;
    @FXML
    private Button deleteRecipeButton;
    
    // Tabel Comenzi
    @FXML
    private TableView<ProductionOrder> ordersTableView;
    @FXML
    private TableColumn<ProductionOrder, Integer> orderIdColumn;
    @FXML
    private TableColumn<ProductionOrder, String> orderRecipeColumn;
    @FXML
    private TableColumn<ProductionOrder, Double> orderQuantityColumn;
    @FXML
    private TableColumn<ProductionOrder, String> orderStatusColumn;
    @FXML
    private TableColumn<ProductionOrder, String> orderCreatedByColumn;
    @FXML
    private TableColumn<ProductionOrder, LocalDateTime> orderCreatedAtColumn;
    
    // Butoane Comenzi
    @FXML
    private Button processOrderButton;
    @FXML
    private Button viewOrderButton;
    @FXML
    private Button cancelOrderButton;
    
    public ProductionController() {
        this.productionService = new ProductionService();
        this.recipeList = FXCollections.observableArrayList();
        this.orderList = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing ProductionController");
        
        try {
            setupRecipesTable();
            setupOrdersTable();
            setupButtonActions();
            
            loadRecipes();
            loadOrders();
            
            // Listener pentru selecția rețetelor
            recipesTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    updateRecipeButtons(newSelection);
                });
            
            // Listener pentru selecția comenzilor
            ordersTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    updateOrderButtons(newSelection);
                });
            
            logger.info("ProductionController initialization completed successfully");
            
        } catch (Exception e) {
            logger.error("Error initializing ProductionController", e);
            throw new RuntimeException("Failed to initialize ProductionController", e);
        }
    }
    
    private void setupRecipesTable() {
        recipeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        recipeNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        recipeCreatedByColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        recipeCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Custom cell factory pentru ingrediente
        recipeIngredientsColumn.setCellValueFactory(new PropertyValueFactory<>("ingredients"));
        recipeIngredientsColumn.setCellFactory(column -> new TableCell<Recipe, List<RecipeIngredient>>() {
            @Override
            protected void updateItem(List<RecipeIngredient> items, boolean empty) {
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
        
        // Formatăm data
        recipeCreatedAtColumn.setCellFactory(column -> new TableCell<Recipe, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                }
            }
        });
        
        recipesTableView.setItems(recipeList);
    }
    
    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderRecipeColumn.setCellValueFactory(new PropertyValueFactory<>("recipeName"));
        orderQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityToProduce"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderCreatedByColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        orderCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Formatăm cantitatea
        orderQuantityColumn.setCellFactory(column -> new TableCell<ProductionOrder, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        
        // Formatăm statusul cu culori
        orderStatusColumn.setCellFactory(column -> new TableCell<ProductionOrder, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(getStatusDisplay(status));
                    setStyle(getStatusStyle(status));
                }
            }
        });
        
        // Formatăm data
        orderCreatedAtColumn.setCellFactory(column -> new TableCell<ProductionOrder, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                }
            }
        });
        
        ordersTableView.setItems(orderList);
    }
    
    private void setupButtonActions() {
        addRecipeButton.setOnAction(e -> addRecipe());
        createOrderButton.setOnAction(e -> createProductionOrder());
        refreshButton.setOnAction(e -> refreshData());
        
        viewRecipeButton.setOnAction(e -> viewSelectedRecipe());
        editRecipeButton.setOnAction(e -> editSelectedRecipe());
        deleteRecipeButton.setOnAction(e -> deleteSelectedRecipe());
        
        processOrderButton.setOnAction(e -> processSelectedOrder());
        viewOrderButton.setOnAction(e -> viewSelectedOrder());
        cancelOrderButton.setOnAction(e -> cancelSelectedOrder());
    }
    
    private void updateRecipeButtons(Recipe selectedRecipe) {
        boolean hasSelection = selectedRecipe != null;
        viewRecipeButton.setDisable(!hasSelection);
        editRecipeButton.setDisable(!hasSelection);
        deleteRecipeButton.setDisable(!hasSelection);
    }
    
    private void updateOrderButtons(ProductionOrder selectedOrder) {
        boolean hasSelection = selectedOrder != null;
        boolean isPending = hasSelection && "pending".equals(selectedOrder.getStatus());
        
        processOrderButton.setDisable(!isPending);
        viewOrderButton.setDisable(!hasSelection);
        cancelOrderButton.setDisable(!isPending);
    }
    
    @FXML
    private void addRecipe() {
        Dialog<Recipe> dialog = createRecipeDialog(null);
        dialog.showAndWait().ifPresent(recipe -> {
            Recipe savedRecipe = productionService.saveRecipe(recipe);
            if (savedRecipe != null) {
                loadRecipes();
                showAlert(AlertType.INFORMATION, "Succes", "Rețeta a fost salvată cu succes!");
            } else {
                showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut salva rețeta.");
            }
        });
    }
    
    @FXML
    private void createProductionOrder() {
        if (recipeList.isEmpty()) {
            showAlert(AlertType.WARNING, "Atenție", "Nu există nicio rețetă definită. Adăugați mai întâi o rețetă.");
            return;
        }
        
        Dialog<ProductionOrder> dialog = createOrderDialog();
        dialog.showAndWait().ifPresent(order -> {
            ProductionOrder savedOrder = productionService.createProductionOrder(order);
            if (savedOrder != null) {
                loadOrders();
                showAlert(AlertType.INFORMATION, "Succes", "Comanda de producție a fost creată!");
            } else {
                showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut crea comanda.");
            }
        });
    }
    
    @FXML
    private void processSelectedOrder() {
        ProductionOrder selectedOrder = ordersTableView.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați o comandă de procesat.");
            return;
        }
        
        try {
            productionService.processProduction(selectedOrder);
            loadOrders();
            showAlert(AlertType.INFORMATION, "Succes", "Producția a fost finalizată cu succes!");
        } catch (InsufficientStockException e) {
            showAlert(AlertType.ERROR, "Stoc Insuficient", e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing production order", e);
            showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut procesa comanda: " + e.getMessage());
        }
    }
    
    @FXML
    private void viewSelectedRecipe() {
        Recipe selectedRecipe = recipesTableView.getSelectionModel().getSelectedItem();
        if (selectedRecipe == null) return;
        
        showRecipeDetails(selectedRecipe);
    }
    
    @FXML
    private void editSelectedRecipe() {
        Recipe selectedRecipe = recipesTableView.getSelectionModel().getSelectedItem();
        if (selectedRecipe == null) return;
        
        Dialog<Recipe> dialog = createRecipeDialog(selectedRecipe);
        dialog.showAndWait().ifPresent(recipe -> {
            Recipe savedRecipe = productionService.saveRecipe(recipe);
            if (savedRecipe != null) {
                loadRecipes();
                showAlert(AlertType.INFORMATION, "Succes", "Rețeta a fost actualizată!");
            }
        });
    }
    
    @FXML
    private void deleteSelectedRecipe() {
        Recipe selectedRecipe = recipesTableView.getSelectionModel().getSelectedItem();
        if (selectedRecipe == null) return;
        
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmare Ștergere");
        alert.setHeaderText("Ștergere Rețetă");
        alert.setContentText("Sunteți sigur că doriți să ștergeți rețeta '" + selectedRecipe.getProductName() + "'?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Implement delete functionality
                showAlert(AlertType.INFORMATION, "Info", "Funcționalitate de ștergere în dezvoltare.");
            }
        });
    }
    
    @FXML
    private void viewSelectedOrder() {
        ProductionOrder selectedOrder = ordersTableView.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) return;
        
        showOrderDetails(selectedOrder);
    }
    
    @FXML
    private void cancelSelectedOrder() {
        ProductionOrder selectedOrder = ordersTableView.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) return;
        
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirmare Anulare");
        alert.setHeaderText("Anulare Comandă");
        alert.setContentText("Sunteți sigur că doriți să anulați comanda '" + selectedOrder.getRecipeName() + "'?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // TODO: Implement cancel functionality
                showAlert(AlertType.INFORMATION, "Info", "Funcționalitate de anulare în dezvoltare.");
            }
        });
    }
    
    @FXML
    private void refreshData() {
        loadRecipes();
        loadOrders();
    }
    
    private void loadRecipes() {
        try {
            List<Recipe> recipes = productionService.getAllRecipes();
            recipeList.clear();
            recipeList.addAll(recipes);
            logger.info("Loaded {} recipes", recipes.size());
        } catch (Exception e) {
            logger.error("Error loading recipes", e);
        }
    }
    
    private void loadOrders() {
        try {
            List<ProductionOrder> orders = productionService.getAllProductionOrders();
            orderList.clear();
            orderList.addAll(orders);
            logger.info("Loaded {} production orders", orders.size());
        } catch (Exception e) {
            logger.error("Error loading production orders", e);
        }
    }
    
    private Dialog<Recipe> createRecipeDialog(Recipe existingRecipe) {
        Dialog<Recipe> dialog = new Dialog<>();
        dialog.setTitle(existingRecipe != null ? "Editează Rețetă" : "Adaugă Rețetă");
        dialog.setHeaderText(null);
        
        ButtonType saveButtonType = new ButtonType("Salvează", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // TODO: Implement product selection from database
        TextField productNameField = new TextField();
        productNameField.setPromptText("Nume produs finisat");
        
        if (existingRecipe != null) {
            productNameField.setText(existingRecipe.getProductName());
        }
        
        grid.add(new Label("Produs Finisat:"), 0, 0);
        grid.add(productNameField, 1, 0);
        
        // TODO: Add ingredients management
        grid.add(new Label("Ingrediente:"), 0, 1);
        grid.add(new Label("Funcționalitate în dezvoltare..."), 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        Platform.runLater(() -> productNameField.requestFocus());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Recipe recipe = new Recipe();
                recipe.setProductName(productNameField.getText());
                recipe.setCreatedBy(App.getCurrentUser() != null ? App.getCurrentUser().getUsername() : "unknown");
                recipe.setIngredients(new ArrayList<>()); // TODO: Implement ingredients
                
                if (existingRecipe != null) {
                    recipe.setId(existingRecipe.getId());
                    recipe.setProductId(existingRecipe.getProductId());
                }
                
                return recipe;
            }
            return null;
        });
        
        return dialog;
    }
    
    private Dialog<ProductionOrder> createOrderDialog() {
        Dialog<ProductionOrder> dialog = new Dialog<>();
        dialog.setTitle("Creează Comandă Producție");
        dialog.setHeaderText(null);
        
        ButtonType createButtonType = new ButtonType("Creează", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        ComboBox<Recipe> recipeComboBox = new ComboBox<>(recipeList);
        recipeComboBox.setConverter(new StringConverter<Recipe>() {
            @Override
            public String toString(Recipe recipe) {
                return recipe != null ? recipe.getProductName() : "";
            }
            
            @Override
            public Recipe fromString(String string) {
                return null;
            }
        });
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Cantitate de produs");
        
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Note (opțional)");
        notesArea.setPrefRowCount(3);
        
        grid.add(new Label("Rețetă:"), 0, 0);
        grid.add(recipeComboBox, 1, 0);
        grid.add(new Label("Cantitate:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("Note:"), 0, 2);
        grid.add(notesArea, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        Platform.runLater(() -> recipeComboBox.requestFocus());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Recipe selectedRecipe = recipeComboBox.getValue();
                if (selectedRecipe == null) {
                    showAlert(AlertType.ERROR, "Eroare", "Selectați o rețetă.");
                    return null;
                }
                
                try {
                    double quantity = Double.parseDouble(quantityField.getText());
                    if (quantity <= 0) {
                        showAlert(AlertType.ERROR, "Eroare", "Cantitatea trebuie să fie pozitivă.");
                        return null;
                    }
                    
                    ProductionOrder order = new ProductionOrder();
                    order.setRecipeId(selectedRecipe.getId());
                    order.setRecipeName(selectedRecipe.getProductName());
                    order.setQuantityToProduce(quantity);
                    order.setNotes(notesArea.getText());
                    order.setCreatedBy(App.getCurrentUser() != null ? App.getCurrentUser().getUsername() : "unknown");
                    
                    return order;
                    
                } catch (NumberFormatException e) {
                    showAlert(AlertType.ERROR, "Eroare", "Introduceți o cantitate validă.");
                    return null;
                }
            }
            return null;
        });
        
        return dialog;
    }
    
    private void showRecipeDetails(Recipe recipe) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Detalii Rețetă");
        alert.setHeaderText(recipe.getProductName());
        
        StringBuilder content = new StringBuilder();
        content.append("ID: ").append(recipe.getId()).append("\n");
        content.append("Creat de: ").append(recipe.getCreatedBy()).append("\n");
        content.append("Creat la: ").append(recipe.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");
        
        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            content.append("Ingrediente:\n");
            for (RecipeIngredient ingredient : recipe.getIngredients()) {
                content.append("- ").append(ingredient.getProductName())
                       .append(" (").append(ingredient.getQuantity()).append(" ")
                       .append(ingredient.getUnitOfMeasure()).append(")\n");
            }
        } else {
            content.append("Nu există ingrediente definite.");
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    private void showOrderDetails(ProductionOrder order) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Detalii Comandă Producție");
        alert.setHeaderText("Comanda #" + order.getId());
        
        StringBuilder content = new StringBuilder();
        content.append("Rețetă: ").append(order.getRecipeName()).append("\n");
        content.append("Cantitate: ").append(String.format("%.2f", order.getQuantityToProduce())).append("\n");
        content.append("Status: ").append(getStatusDisplay(order.getStatus())).append("\n");
        content.append("Creat de: ").append(order.getCreatedBy()).append("\n");
        content.append("Creat la: ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        
        if (order.getStartedAt() != null) {
            content.append("Început la: ").append(order.getStartedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        }
        
        if (order.getCompletedAt() != null) {
            content.append("Finalizat la: ").append(order.getCompletedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        }
        
        if (order.getNotes() != null && !order.getNotes().trim().isEmpty()) {
            content.append("\nNote: ").append(order.getNotes());
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    private String getStatusDisplay(String status) {
        switch (status) {
            case "pending": return "În Așteptare";
            case "in_progress": return "În Procesare";
            case "completed": return "Finalizat";
            case "cancelled": return "Anulat";
            default: return status;
        }
    }
    
    private String getStatusStyle(String status) {
        switch (status) {
            case "pending": return "-fx-text-fill: #FFA500;"; // Orange
            case "in_progress": return "-fx-text-fill: #007BFF;"; // Blue
            case "completed": return "-fx-text-fill: #28A745;"; // Green
            case "cancelled": return "-fx-text-fill: #DC3545;"; // Red
            default: return "";
        }
    }
    
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

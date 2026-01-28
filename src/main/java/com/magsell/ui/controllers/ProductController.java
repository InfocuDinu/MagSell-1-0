package com.magsell.ui.controllers;

import com.magsell.models.Product;
import com.magsell.services.ProductService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controlerul pentru managerul de produse.
 * Gestionate lista de produse, filtru, editare, stergere.
 */
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService = new ProductService();

    @FXML
    private TableView<Product> productTable;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryCombo;

    // Fields from ProductEditor.fxml
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField quantityField;
    @FXML
    private ComboBox<String> categoryField;

    private Product currentEditingProduct;
    private Stage editorStage;
    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredList;

    @FXML
    public void initialize() {
        logger.info("Initializing ProductController");
        setupTableColumns();
        loadProducts();
        loadCategories();

        // Set up table selection
        if (productTable != null) {
            productTable.setItems(filteredList);
        }
    }

    /**
     * Configurează coloanele tabelei
     */
    private void setupTableColumns() {
        if (productTable == null) return;

        TableColumn<Product, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Product, String> nameCol = new TableColumn<>("Nume");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);

        TableColumn<Product, String> descCol = new TableColumn<>("Descriere");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        TableColumn<Product, BigDecimal> priceCol = new TableColumn<>("Pret");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("Cant.");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setPrefWidth(80);

        TableColumn<Product, String> catCol = new TableColumn<>("Categoria");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(100);

        ObservableList<TableColumn<Product, ?>> columns = FXCollections.observableArrayList();
        columns.addAll(idCol, nameCol, descCol, priceCol, qtyCol, catCol);
        productTable.getColumns().setAll(columns);
    }

    /**
     * Incarc lista de produse din baza de date.
     */
    private void loadProducts() {
        new Thread(() -> {
            try {
                List<Product> products = productService.getAllProducts();
                Platform.runLater(() -> {
                    productList = FXCollections.observableArrayList(products);
                    filteredList = new FilteredList<>(productList);
                    if (productTable != null) {
                        productTable.setItems(filteredList);
                    }
                    logger.info("Incarcate " + products.size() + " produse");
                });
            } catch (SQLException e) {
                logger.error("Eroare la incarcarea produselor", e);
                Platform.runLater(() -> showAlert("Eroare", "Eroare la incarcarea produselor: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Incarc lista de categorii disponibile.
     */
    private void loadCategories() {
        new Thread(() -> {
            try {
                List<String> categories = productService.getCategories();
                Platform.runLater(() -> {
                    if (categoryCombo != null) {
                        categoryCombo.setItems(FXCollections.observableArrayList(categories));
                    }
                    if (categoryField != null) {
                        categoryField.setItems(FXCollections.observableArrayList(categories));
                    }
                    logger.info("Incarcate " + categories.size() + " categorii");
                });
            } catch (SQLException e) {
                logger.error("Eroare la incarcarea categoriilor", e);
            }
        }).start();
    }

    @FXML
    public void handleAddProduct() {
        currentEditingProduct = null;
        openEditorDialog();
    }

    @FXML
    public void handleEditProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selectie", "Va rog selectati un produs pentru a-l edita");
            return;
        }
        currentEditingProduct = selected;
        openEditorDialog();
    }

    @FXML
    public void handleDeleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selectie", "Va rog selectati un produs pentru a-l sterge");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmare");
        confirmDialog.setHeaderText("Stergere Produs");
        confirmDialog.setContentText("Sunteti sigur ca doriti sa stergeti produsul: " + selected.getName() + "?");

        if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            new Thread(() -> {
                try {
                    productService.deleteProduct(selected.getId());
                    Platform.runLater(() -> {
                        productList.remove(selected);
                        filteredList.remove(selected);
                        logger.info("Produs sters: " + selected.getName());
                        showAlert("Succes", "Produs sters cu succes");
                    });
                } catch (SQLException e) {
                    logger.error("Eroare la stergerea produsului", e);
                    Platform.runLater(() -> showAlert("Eroare", "Eroare la stergerea produsului: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    public void handleSearchProducts() {
        filterProducts();
    }

    @FXML
    public void handleFilterByCategory() {
        filterProducts();
    }

    /**
     * Filtreaza lista de produse dupa cautare si categorie.
     */
    private void filterProducts() {
        String searchText = searchField != null ? searchField.getText().toLowerCase() : "";
        String selectedCategory = categoryCombo != null ? categoryCombo.getValue() : null;

        filteredList.setPredicate(product -> {
            boolean matchesSearch = searchText.isEmpty() || product.getName().toLowerCase().contains(searchText)
                    || (product.getDescription() != null && product.getDescription().toLowerCase().contains(searchText));

            boolean matchesCategory = selectedCategory == null || selectedCategory.isEmpty()
                    || (product.getCategory() != null && product.getCategory().equals(selectedCategory));

            return matchesSearch && matchesCategory;
        });
    }

    /**
     * Deschide dialogul de editare a produsului.
     */
    private void openEditorDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/magsell/ui/fxml/ProductEditor.fxml"));
            loader.setController(this);
            Parent root = loader.load();

            editorStage = new Stage();
            editorStage.setTitle(currentEditingProduct == null ? "Adauga Produs" : "Editeaza Produs");
            editorStage.setScene(new Scene(root, 500, 400));

            // Pre-populate fields if editing
            if (currentEditingProduct != null) {
                nameField.setText(currentEditingProduct.getName());
                descriptionField.setText(currentEditingProduct.getDescription());
                priceField.setText(currentEditingProduct.getPrice().toString());
                quantityField.setText(String.valueOf(currentEditingProduct.getQuantity()));
                categoryField.setValue(currentEditingProduct.getCategory());
            } else {
                clearEditorFields();
            }

            editorStage.showAndWait();
        } catch (IOException e) {
            logger.error("Eroare la deschiderea dialogului editor", e);
            showAlert("Eroare", "Eroare la deschiderea editorului: " + e.getMessage());
        }
    }

    @FXML
    public void handleSaveProduct() {
        try {
            // Validation
            if (nameField.getText().isEmpty()) {
                showAlert("Validare", "Numele produsului este obligatoriu");
                return;
            }

            Product product = currentEditingProduct != null ? currentEditingProduct : new Product();
            product.setName(nameField.getText());
            product.setDescription(descriptionField.getText());
            product.setPrice(new BigDecimal(priceField.getText().isEmpty() ? "0" : priceField.getText()));
            product.setQuantity(Integer.parseInt(quantityField.getText().isEmpty() ? "0" : quantityField.getText()));
            product.setCategory(categoryField.getValue());

            if (currentEditingProduct == null) {
                // Create new
                productService.createProduct(product);
                Platform.runLater(() -> {
                    loadProducts();
                    logger.info("Produs adaugat: " + product.getName());
                    showAlert("Succes", "Produs adaugat cu succes");
                });
            } else {
                // Update existing
                productService.updateProduct(product);
                Platform.runLater(() -> {
                    loadProducts();
                    logger.info("Produs actualizat: " + product.getName());
                    showAlert("Succes", "Produs actualizat cu succes");
                });
            }

            editorStage.close();
        } catch (NumberFormatException e) {
            showAlert("Eroare", "Pret si Cantitate trebuie sa fie numere");
            logger.error("Eroare format numeric", e);
        } catch (SQLException e) {
            showAlert("Eroare", "Eroare la salvarea produsului: " + e.getMessage());
            logger.error("Eroare la salvarea produsului", e);
        }
    }

    @FXML
    public void handleCancelEdit() {
        editorStage.close();
    }

    @FXML
    public void handleClose() {
        Stage stage = (Stage) productTable.getScene().getWindow();
        stage.close();
    }

    private void clearEditorFields() {
        nameField.clear();
        descriptionField.clear();
        priceField.clear();
        quantityField.clear();
        categoryField.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

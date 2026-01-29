package com.magsell.ui.controllers;

import com.magsell.models.CartItem;
import com.magsell.models.Product;
import com.magsell.models.Sale;
import com.magsell.services.ProductService;
import com.magsell.services.SalesService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controller pentru interfața POS (Point of Sale).
 * Permite selectarea produselor și crearea unei vânzări.
 */
public class POSController {
    private static final Logger logger = LoggerFactory.getLogger(POSController.class);
    private final ProductService productService = new ProductService();
    private final SalesService salesService = new SalesService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ro", "RO"));

    @FXML
    private ScrollPane productsScrollPane;
    @FXML
    private FlowPane productsFlowPane;
    @FXML
    private ListView<String> cartListView;
    @FXML
    private Label totalLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryCombo;
    @FXML
    private Button checkoutButton;
    @FXML
    private Button clearCartButton;

    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private List<Product> allProducts = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        logger.info("Initializing POSController");
        setupCart();
        loadProducts();
        loadCategories();
        setupSearch();
    }

    /**
     * Configurează coșul de cumpărături.
     */
    private void setupCart() {
        cartListView.setItems(FXCollections.observableArrayList());
        updateTotal();
    }

    /**
     * Încarcă produsele disponibile.
     */
    private void loadProducts() {
        new Thread(() -> {
            try {
                allProducts = productService.getAllProducts();
                Platform.runLater(() -> {
                    displayProducts(allProducts);
                    logger.info("Încărcate " + allProducts.size() + " produse pentru POS");
                });
            } catch (SQLException e) {
                logger.error("Eroare la încărcarea produselor", e);
                Platform.runLater(() -> showAlert("Eroare", "Nu s-au putut încărca produsele: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Încarcă categoriile disponibile.
     */
    private void loadCategories() {
        new Thread(() -> {
            try {
                List<String> categories = productService.getCategories();
                Platform.runLater(() -> {
                    ObservableList<String> categoryList = FXCollections.observableArrayList("Toate");
                    categoryList.addAll(categories);
                    categoryCombo.setItems(categoryList);
                    categoryCombo.setValue("Toate");
                });
            } catch (SQLException e) {
                logger.error("Eroare la încărcarea categoriilor", e);
            }
        }).start();
    }

    /**
     * Configurează funcționalitatea de căutare.
     */
    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterProducts());
        categoryCombo.setOnAction(e -> filterProducts());
    }

    /**
     * Filtrează produsele în funcție de căutare și categorie.
     */
    private void filterProducts() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categoryCombo.getValue();

        List<Product> filtered = allProducts.stream()
                .filter(p -> {
                    boolean matchesSearch = searchText.isEmpty() ||
                            p.getName().toLowerCase().contains(searchText) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchText));
                    boolean matchesCategory = selectedCategory == null || "Toate".equals(selectedCategory) ||
                            (p.getCategory() != null && p.getCategory().equals(selectedCategory));
                    return matchesSearch && matchesCategory && p.getQuantity() > 0; // Doar produse cu stoc
                })
                .toList();

        displayProducts(filtered);
    }

    /**
     * Afișează produsele în interfață.
     */
    private void displayProducts(List<Product> products) {
        productsFlowPane.getChildren().clear();
        productsFlowPane.setHgap(15);
        productsFlowPane.setVgap(15);
        productsFlowPane.setPadding(new Insets(15));

        for (Product product : products) {
            VBox productCard = createProductCard(product);
            productsFlowPane.getChildren().add(productCard);
        }
    }

    /**
     * Creează un card pentru un produs.
     */
    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-padding: 15px; " +
                "-fx-effect: dropshadow(gaussian, rgba(139, 111, 71, 0.15), 6, 0, 0, 2); " +
                "-fx-cursor: hand; -fx-min-width: 180px; -fx-max-width: 180px;");
        card.setOnMouseClicked(e -> addToCart(product));

        // Imagine produs (placeholder sau imagine reală dacă există)
        ImageView imageView = new ImageView();
        imageView.setFitWidth(150);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-color: #F5E6D3; -fx-background-radius: 8px;");

        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            try {
                File imageFile = new File(product.getImagePath());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                }
            } catch (Exception e) {
                logger.debug("Nu s-a putut încărca imaginea pentru " + product.getName());
            }
        }

        // Nume produs
        Label nameLabel = new Label(product.getName());
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(150);

        // Preț
        Label priceLabel = new Label(formatCurrency(product.getPrice()));
        priceLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        priceLabel.setStyle("-fx-text-fill: #8B6F47;");

        // Stoc
        Label stockLabel = new Label("Stoc: " + product.getQuantity());
        stockLabel.setFont(Font.font("Segoe UI", 11));
        if (product.isLowStock()) {
            stockLabel.setStyle("-fx-text-fill: #F44336;");
        }

        card.getChildren().addAll(imageView, nameLabel, priceLabel, stockLabel);
        return card;
    }

    /**
     * Adaugă un produs în coș.
     */
    private void addToCart(Product product) {
        if (product.getQuantity() <= 0) {
            showAlert("Stoc epuizat", "Produsul " + product.getName() + " nu mai este disponibil.");
            return;
        }

        // Verifică dacă produsul există deja în coș
        CartItem existingItem = cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Incrementează cantitatea dacă nu depășește stocul
            if (existingItem.getQuantity() < product.getQuantity()) {
                existingItem.incrementQuantity();
            } else {
                showAlert("Stoc insuficient", "Nu mai sunt suficiente unități disponibile pentru " + product.getName());
                return;
            }
        } else {
            // Adaugă produs nou în coș
            cartItems.add(new CartItem(product, 1));
        }

        updateCartDisplay();
        updateTotal();
    }

    /**
     * Actualizează afișarea coșului.
     */
    private void updateCartDisplay() {
        ObservableList<String> cartDisplay = FXCollections.observableArrayList();
        for (CartItem item : cartItems) {
            String display = String.format("%s x%d = %s",
                    item.getProduct().getName(),
                    item.getQuantity(),
                    formatCurrency(item.getSubtotal()));
            cartDisplay.add(display);
        }
        cartListView.setItems(cartDisplay);
    }

    /**
     * Actualizează totalul.
     */
    private void updateTotal() {
        total = cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalLabel.setText("Total: " + formatCurrency(total));
        totalLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #D4AF37;");

        checkoutButton.setDisable(cartItems.isEmpty());
        clearCartButton.setDisable(cartItems.isEmpty());
    }

    /**
     * Finalizează vânzarea.
     */
    @FXML
    public void handleCheckout() {
        if (cartItems.isEmpty()) {
            showAlert("Coș gol", "Adăugați produse în coș înainte de finalizare.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmare vânzare");
        confirmDialog.setHeaderText("Finalizare vânzare");
        confirmDialog.setContentText("Total: " + formatCurrency(total) + "\n\nConfirmați vânzarea?");

        if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            processSale();
        }
    }

    /**
     * Procesează vânzarea și o salvează în baza de date.
     */
    private void processSale() {
        new Thread(() -> {
            try {
                List<Sale> sales = new ArrayList<>();
                LocalDateTime saleDate = LocalDateTime.now();

                for (CartItem item : cartItems) {
                    Sale sale = new Sale(
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getProduct().getPrice(),
                            item.getSubtotal()
                    );
                    sale.setSaleDate(saleDate);
                    sales.add(sale);

                    // Actualizează stocul produsului
                    Product product = item.getProduct();
                    product.setQuantity(product.getQuantity() - item.getQuantity());
                    productService.updateProduct(product);
                }

                // Salvează vânzările
                salesService.createSales(sales);

                Platform.runLater(() -> {
                    showAlert("Succes", "Vânzare finalizată cu succes!\nTotal: " + formatCurrency(total));
                    clearCart();
                    loadProducts(); // Reîncarcă produsele pentru a actualiza stocul
                });

                logger.info("Vânzare procesată: " + sales.size() + " produse, Total: " + total);
            } catch (SQLException e) {
                logger.error("Eroare la procesarea vânzării", e);
                Platform.runLater(() -> showAlert("Eroare", "Eroare la procesarea vânzării: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Golește coșul.
     */
    @FXML
    public void handleClearCart() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Golire coș");
        confirmDialog.setHeaderText("Golire coș");
        confirmDialog.setContentText("Sunteți sigur că doriți să goliți coșul?");

        if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            clearCart();
        }
    }

    /**
     * Golește coșul.
     */
    private void clearCart() {
        cartItems.clear();
        updateCartDisplay();
        updateTotal();
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
     * Afișează un alert.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

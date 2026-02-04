package com.magsell.ui.controllers;

import com.jfoenix.controls.*;
import com.magsell.models.*;
import com.magsell.services.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller pentru Nota de Intrare-Recepție (NIR)
 * Gestionează intrarea mărfurilor în stoc pe baza facturilor de la furnizori
 */
public class NirController {
    private static final Logger logger = LoggerFactory.getLogger(NirController.class);
    
    // Servicii
    private final ProductService productService;
    private final PartnerService partnerService;
    private final InventoryService inventoryService;
    private final NirService nirService;
    
    // Date
    private ObservableList<NirItem> nirItems;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    
    @FXML
    private JFXTextField nirNumberField;
    
    @FXML
    private JFXDatePicker nirDatePicker;
    
    @FXML
    private JFXComboBox<Partner> supplierComboBox;
    
    @FXML
    private JFXTextField invoiceNumberField;
    
    @FXML
    private JFXDatePicker invoiceDatePicker;
    
    @FXML
    private JFXComboBox<String> warehouseComboBox;
    
    @FXML
    private JFXTextField productCodeField;
    
    @FXML
    private JFXTextField productNameField;
    
    @FXML
    private JFXTextField quantityField;
    
    @FXML
    private JFXTextField unitPriceField;
    
    @FXML
    private JFXTextField batchNumberField;
    
    @FXML
    private JFXDatePicker expiryDatePicker;
    
    @FXML
    private JFXButton addProductButton;
    
    @FXML
    private JFXButton removeProductButton;
    
    @FXML
    private JFXButton saveNirButton;
    
    @FXML
    private JFXButton clearButton;
    
    @FXML
    private TableView<NirItem> productsTable;
    
    @FXML
    private TableColumn<NirItem, String> codeColumn;
    
    @FXML
    private TableColumn<NirItem, String> nameColumn;
    
    @FXML
    private TableColumn<NirItem, BigDecimal> quantityColumn;
    
    @FXML
    private TableColumn<NirItem, String> unitColumn;
    
    @FXML
    private TableColumn<NirItem, BigDecimal> unitPriceColumn;
    
    @FXML
    private TableColumn<NirItem, BigDecimal> totalColumn;
    
    @FXML
    private TableColumn<NirItem, String> batchColumn;
    
    @FXML
    private TableColumn<NirItem, String> expiryColumn;
    
    @FXML
    private Label totalAmountLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private VBox mainContainer;
    
    public NirController() {
        this.productService = new ProductService();
        this.partnerService = new PartnerService();
        this.inventoryService = new InventoryService();
        this.nirService = new NirService();
        this.nirItems = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing NirController");
        
        setupComboBoxes();
        setupTable();
        setupEventHandlers();
        setupValidation();
        
        // Setări inițiale
        nirDatePicker.setValue(LocalDate.now());
        invoiceDatePicker.setValue(LocalDate.now());
        warehouseComboBox.getItems().addAll("Gestiune Principală", "Depozit", "Magazin");
        warehouseComboBox.getSelectionModel().selectFirst();
        
        // Generează număr NIR
        generateNirNumber();
        
        updateTotalAmount();
        updateStatus("Gata pentru adăugare produse");
    }
    
    private void setupComboBoxes() {
        // Încarcă furnizorii
        try {
            List<Partner> suppliers = partnerService.getAllPartners();
            supplierComboBox.setItems(FXCollections.observableArrayList(suppliers));
            
            // Filtrează doar furnizorii
            supplierComboBox.getItems().removeIf(partner -> 
                partner.getName() == null || partner.getName().toLowerCase().contains("client"));
            
        } catch (Exception e) {
            logger.error("Error loading suppliers", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-au putut încărca furnizorii: " + e.getMessage());
        }
    }
    
    private void setupTable() {
        // Configurare coloane
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        batchColumn.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        expiryColumn.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        
        // Formatare coloane numerice
        quantityColumn.setCellFactory(column -> new TableCell<NirItem, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        
        unitPriceColumn.setCellFactory(column -> new TableCell<NirItem, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        
        totalColumn.setCellFactory(column -> new TableCell<NirItem, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        
        // Formatare dată expirare
        expiryColumn.setCellFactory(column -> new TableCell<NirItem, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateFormatter));
                }
            }
        });
        
        productsTable.setItems(nirItems);
        
        // Selectie rând pentru editare/ștergere
        productsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    loadSelectedItemToForm(newSelection);
                }
            });
    }
    
    private void setupEventHandlers() {
        // Căutare produs după cod
        productCodeField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && newValue.length() >= 3) {
                searchProductByCode(newValue);
            }
        });
        
        // Adăugare produs
        addProductButton.setOnAction(event -> addProduct());
        
        // Ștergere produs
        removeProductButton.setOnAction(event -> removeSelectedProduct());
        
        // Salvare NIR
        saveNirButton.setOnAction(event -> saveNir());
        
        // Curățare formular
        clearButton.setOnAction(event -> clearForm());
    }
    
    private void setupValidation() {
        // Validare cantitate
        quantityField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                quantityField.setText(oldValue);
            }
        });
        
        // Validare preț
        unitPriceField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                unitPriceField.setText(oldValue);
            }
        });
    }
    
    private void generateNirNumber() {
        try {
            String nextNumber = nirService.getNextNirNumber();
            nirNumberField.setText(nextNumber);
        } catch (Exception e) {
            logger.error("Error generating NIR number", e);
            nirNumberField.setText("NIR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-001");
        }
    }
    
    private void searchProductByCode(String code) {
        try {
            Product product = productService.getProductByCode(code);
            if (product != null) {
                productNameField.setText(product.getName());
                unitPriceField.setText(product.getPurchasePrice().toString());
                updateStatus("Produs găsit: " + product.getName());
            } else {
                productNameField.clear();
                unitPriceField.clear();
                updateStatus("Produs negăsit pentru codul: " + code);
            }
        } catch (Exception e) {
            logger.error("Error searching product by code: " + code, e);
            updateStatus("Eroare la căutarea produsului");
        }
    }
    
    private void addProduct() {
        try {
            // Validări
            if (productCodeField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Atenție", "Introduceți codul produsului");
                return;
            }
            
            if (productNameField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Atenție", "Introduceți numele produsului");
                return;
            }
            
            BigDecimal quantity = parseBigDecimal(quantityField.getText());
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert(Alert.AlertType.WARNING, "Atenție", "Introduceți o cantitate validă");
                return;
            }
            
            BigDecimal unitPrice = parseBigDecimal(unitPriceField.getText());
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert(Alert.AlertType.WARNING, "Atenție", "Introduceți un preț valid");
                return;
            }
            
            // Verifică dacă produsul există deja
            for (NirItem existingItem : nirItems) {
                if (existingItem.getProductCode().equals(productCodeField.getText().trim())) {
                    // Actualizează cantitatea
                    existingItem.setQuantity(existingItem.getQuantity().add(quantity));
                    existingItem.recalculateTotal();
                    productsTable.refresh();
                    updateTotalAmount();
                    clearProductFields();
                    updateStatus("Cantitate actualizată pentru: " + existingItem.getProductName());
                    return;
                }
            }
            
            // Creează item nou
            NirItem item = new NirItem();
            item.setProductCode(productCodeField.getText().trim());
            item.setProductName(productNameField.getText().trim());
            item.setQuantity(quantity);
            item.setUnit("buc"); // Poate fi preluat din produs
            item.setUnitPrice(unitPrice);
            item.setBatchNumber(batchNumberField.getText().trim());
            item.setExpiryDate(expiryDatePicker.getValue());
            item.recalculateTotal();
            
            nirItems.add(item);
            updateTotalAmount();
            clearProductFields();
            updateStatus("Produs adăugat: " + item.getProductName());
            
        } catch (Exception e) {
            logger.error("Error adding product", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut adăuga produsul: " + e.getMessage());
        }
    }
    
    private void removeSelectedProduct() {
        NirItem selectedItem = productsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            nirItems.remove(selectedItem);
            updateTotalAmount();
            updateStatus("Produs șters: " + selectedItem.getProductName());
        } else {
            showAlert(Alert.AlertType.WARNING, "Atenție", "Selectați un produs pentru a-l șterge");
        }
    }
    
    private void saveNir() {
        try {
            // Validări
            if (supplierComboBox.getSelectionModel().getSelectedItem() == null) {
                showAlert(Alert.AlertType.WARNING, "Atenție", "Selectați un furnizor");
                return;
            }
            
            if (invoiceNumberField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Atenție", "Introduceți numărul facturii");
                return;
            }
            
            if (nirItems.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Atenție", "Adăugați cel puțin un produs");
                return;
            }
            
            // Creează NIR
            Nir nir = new Nir();
            nir.setNumber(nirNumberField.getText().trim());
            nir.setDate(nirDatePicker.getValue());
            nir.setSupplier(supplierComboBox.getSelectionModel().getSelectedItem());
            nir.setInvoiceNumber(invoiceNumberField.getText().trim());
            nir.setInvoiceDate(invoiceDatePicker.getValue());
            nir.setWarehouse(warehouseComboBox.getSelectionModel().getSelectedItem());
            nir.setItems(nirItems);
            
            // Salvează NIR
            nirService.saveNir(nir);
            
            showAlert(Alert.AlertType.INFORMATION, "Succes", 
                      "NIR salvat cu succes!\nNumăr: " + nir.getNumber() + 
                      "\nTotal produse: " + nirItems.size());
            
            clearForm();
            
        } catch (Exception e) {
            logger.error("Error saving NIR", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut salva NIR-ul: " + e.getMessage());
        }
    }
    
    private void clearForm() {
        // Reset câmpuri NIR
        generateNirNumber();
        nirDatePicker.setValue(LocalDate.now());
        supplierComboBox.getSelectionModel().clearSelection();
        invoiceNumberField.clear();
        invoiceDatePicker.setValue(LocalDate.now());
        warehouseComboBox.getSelectionModel().selectFirst();
        
        // Reset produse
        nirItems.clear();
        updateTotalAmount();
        
        // Reset câmpuri produs
        clearProductFields();
        
        updateStatus("Formular curățat");
    }
    
    private void clearProductFields() {
        productCodeField.clear();
        productNameField.clear();
        quantityField.clear();
        unitPriceField.clear();
        batchNumberField.clear();
        expiryDatePicker.setValue(null);
    }
    
    private void loadSelectedItemToForm(NirItem item) {
        productCodeField.setText(item.getProductCode());
        productNameField.setText(item.getProductName());
        quantityField.setText(item.getQuantity().toString());
        unitPriceField.setText(item.getUnitPrice().toString());
        batchNumberField.setText(item.getBatchNumber() != null ? item.getBatchNumber() : "");
        expiryDatePicker.setValue(item.getExpiryDate());
    }
    
    private void updateTotalAmount() {
        BigDecimal total = nirItems.stream()
            .map(NirItem::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalAmountLabel.setText(String.format("Total: %.2f RON", total));
    }
    
    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
    
    private BigDecimal parseBigDecimal(String text) {
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * Model pentru item în NIR
     */
    public static class NirItem {
        private String productCode;
        private String productName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal total;
        private String batchNumber;
        private LocalDate expiryDate;
        
        public NirItem() {
            this.quantity = BigDecimal.ZERO;
            this.unit = "buc";
            this.unitPrice = BigDecimal.ZERO;
            this.total = BigDecimal.ZERO;
        }
        
        public void recalculateTotal() {
            this.total = unitPrice.multiply(quantity);
        }
        
        // Getters and Setters
        public String getProductCode() {
            return productCode;
        }
        
        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public void setProductName(String productName) {
            this.productName = productName;
        }
        
        public BigDecimal getQuantity() {
            return quantity;
        }
        
        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
        
        public String getUnit() {
            return unit;
        }
        
        public void setUnit(String unit) {
            this.unit = unit;
        }
        
        public BigDecimal getUnitPrice() {
            return unitPrice;
        }
        
        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
        
        public BigDecimal getTotal() {
            return total;
        }
        
        public void setTotal(BigDecimal total) {
            this.total = total;
        }
        
        public String getBatchNumber() {
            return batchNumber;
        }
        
        public void setBatchNumber(String batchNumber) {
            this.batchNumber = batchNumber;
        }
        
        public LocalDate getExpiryDate() {
            return expiryDate;
        }
        
        public void setExpiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
        }
    }
}

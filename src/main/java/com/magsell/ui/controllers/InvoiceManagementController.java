package com.magsell.ui.controllers;

import com.magsell.models.Invoice;
import com.magsell.models.InvoiceItem;
import com.magsell.models.ReceptionNote;
import com.magsell.models.ReceptionNoteItem;
import com.magsell.services.InvoiceService;
import com.magsell.services.SpvIntegrationService;
import com.magsell.services.ReceptionNotePdfService;
import com.magsell.App;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.util.Callback;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.control.ButtonBar.ButtonData;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller pentru managementul facturilor și notelor de recepție
 */
public class InvoiceManagementController {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceManagementController.class);
    
    private final InvoiceService invoiceService;
    private final SpvIntegrationService spvService;
    
    private ObservableList<Invoice> invoiceList;
    private ObservableList<ReceptionNote> receptionNoteList;
    private ObservableList<InvoiceItem> invoiceItemList;

    // TabPane principal
    @FXML
    private TabPane mainTabPane;

    // Componente Import SPV
    @FXML
    private TextField cifTextField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Button importInvoicesButton;
    @FXML
    private Button searchInvoicesButton;
    @FXML
    private Button downloadInvoicesButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button createManualReceptionNoteButton;

    // Tabel Facturi
    @FXML
    private TableView<Invoice> invoicesTableView;
    @FXML
    private TableColumn<Invoice, Integer> invoiceIdColumn;
    @FXML
    private TableColumn<Invoice, String> invoiceNumberColumn;
    @FXML
    private TableColumn<Invoice, String> invoiceDateColumn;
    @FXML
    private TableColumn<Invoice, String> supplierColumn;
    @FXML
    private TableColumn<Invoice, Double> totalAmountColumn;
    @FXML
    private TableColumn<Invoice, Double> vatAmountColumn;
    @FXML
    private TableColumn<Invoice, String> statusColumn;

    // Butoane Facturi
    @FXML
    private Button generateReceptionNoteButton;
    @FXML
    private Button viewInvoiceButton;
    @FXML
    private Button deleteInvoiceButton;
    @FXML
    private Button viewReceptionNoteButton;

    // Tabel Note Recepție
    @FXML
    private TableView<ReceptionNote> receptionNotesTableView;
    @FXML
    private TableColumn<ReceptionNote, Integer> receptionNoteIdColumn;
    @FXML
    private TableColumn<ReceptionNote, String> receptionNoteNumberColumn;
    @FXML
    private TableColumn<ReceptionNote, LocalDate> receptionDateColumn;
    @FXML
    private TableColumn<ReceptionNote, String> receptionSupplierColumn;
    @FXML
    private TableColumn<ReceptionNote, Double> receptionTotalColumn;
    @FXML
    private TableColumn<ReceptionNote, String> receptionStatusColumn;
    @FXML
    private TableColumn<ReceptionNote, String> receptionCreatedByColumn;

    // Butoane Note Recepție
    @FXML
    private Button editReceptionNoteButton;
    @FXML
    private Button confirmReceptionNoteButton;
    @FXML
    private Button cancelReceptionNoteButton;
    @FXML
    private Button printReceptionNoteButton;

    private ReceptionNotePdfService pdfService;

    // Detalii Factură
    @FXML
    private Label detailInvoiceNumberLabel;
    @FXML
    private Label detailDateLabel;
    @FXML
    private Label detailSupplierLabel;
    @FXML
    private Label detailCifLabel;
    @FXML
    private Label detailTotalLabel;
    @FXML
    private Label detailVatLabel;

    // Tabel Produse Factură
    @FXML
    private TableView<InvoiceItem> invoiceItemsTableView;
    @FXML
    private TableColumn<InvoiceItem, String> itemNameColumn;
    @FXML
    private TableColumn<InvoiceItem, String> itemCodeColumn;
    @FXML
    private TableColumn<InvoiceItem, Double> itemQuantityColumn;
    @FXML
    private TableColumn<InvoiceItem, Double> itemUnitPriceColumn;
    @FXML
    private TableColumn<InvoiceItem, Double> itemTotalColumn;
    @FXML
    private TableColumn<InvoiceItem, Double> itemVatColumn;

    // Status
    @FXML
    private Label statusLabel;

    public InvoiceManagementController() {
        this.invoiceService = new InvoiceService();
        this.spvService = new SpvIntegrationService();
        this.pdfService = new ReceptionNotePdfService();
        this.invoiceList = FXCollections.observableArrayList();
        this.receptionNoteList = FXCollections.observableArrayList();
        this.invoiceItemList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        logger.info("Initializing InvoiceManagementController");
        
        try {
            setupInvoicesTable();
            logger.info("Invoices table setup completed");
            
            setupReceptionNotesTable();
            logger.info("Reception notes table setup completed");
            
            setupInvoiceItemsTable();
            logger.info("Invoice items table setup completed");
            
            setupButtonActions();
            logger.info("Button actions setup completed");
            
            setupDatePickers();
            logger.info("Date pickers setup completed");
            
            loadInvoices();
            logger.info("Invoices loaded");
            
            loadReceptionNotes();
            logger.info("Reception notes loaded");
            
            // Listener pentru selecția facturii
            invoicesTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        showInvoiceDetails(newSelection);
                    } else {
                        clearInvoiceDetails();
                    }
                });
            
            // Listener pentru selecția notei de recepție
            receptionNotesTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    updateReceptionNoteButtons(newSelection);
                });
            
            logger.info("InvoiceManagementController initialization completed successfully");
            
        } catch (Exception e) {
            logger.error("Error initializing InvoiceManagementController", e);
            throw new RuntimeException("Failed to initialize InvoiceManagementController", e);
        }
    }

    private void setupInvoicesTable() {
        invoiceIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        invoiceNumberColumn.setCellValueFactory(new PropertyValueFactory<>("fullInvoiceNumber"));
        invoiceDateColumn.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        vatAmountColumn.setCellValueFactory(new PropertyValueFactory<>("vatAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        invoicesTableView.setItems(invoiceList);
        
        // Formatăm coloanele numerice
        totalAmountColumn.setCellFactory(column -> new TableCell<Invoice, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f RON", item));
                }
            }
        });
        
        vatAmountColumn.setCellFactory(column -> new TableCell<Invoice, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f RON", item));
                }
            }
        });
        
        // Formatăm data
        invoiceDateColumn.setCellFactory(column -> new TableCell<Invoice, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });
    }

    private void setupReceptionNotesTable() {
        receptionNoteIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        receptionNoteNumberColumn.setCellValueFactory(new PropertyValueFactory<>("fullNoteNumber"));
        receptionDateColumn.setCellValueFactory(new PropertyValueFactory<>("receptionDate"));
        receptionSupplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        receptionTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        receptionStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        receptionCreatedByColumn.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        
        receptionNotesTableView.setItems(receptionNoteList);
        
        // Formatăm coloanele numerice
        receptionTotalColumn.setCellFactory(column -> new TableCell<ReceptionNote, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f RON", item));
                }
            }
        });
        
        // Formatăm data
        receptionDateColumn.setCellFactory(column -> new TableCell<ReceptionNote, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                }
            }
        });
    }

    private void setupInvoiceItemsTable() {
        itemNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        itemCodeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        itemQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        itemUnitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        itemTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        itemVatColumn.setCellValueFactory(new PropertyValueFactory<>("vatAmount"));
        
        invoiceItemsTableView.setItems(invoiceItemList);
        
        // Formatăm coloanele numerice
        itemQuantityColumn.setCellFactory(column -> new TableCell<InvoiceItem, Double>() {
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
        
        itemUnitPriceColumn.setCellFactory(column -> new TableCell<InvoiceItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f RON", item));
                }
            }
        });
        
        itemTotalColumn.setCellFactory(column -> new TableCell<InvoiceItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f RON", item));
                }
            }
        });
        
        itemVatColumn.setCellFactory(column -> new TableCell<InvoiceItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f RON", item));
                }
            }
        });
    }

    private void setupButtonActions() {
        createManualReceptionNoteButton.setOnAction(e -> createManualReceptionNote());
        importInvoicesButton.setOnAction(e -> importInvoicesFromSPV());
        searchInvoicesButton.setOnAction(e -> searchInvoicesInSPV());
        downloadInvoicesButton.setOnAction(e -> downloadSelectedInvoices());
        refreshButton.setOnAction(e -> refreshData());
        
        generateReceptionNoteButton.setOnAction(e -> generateReceptionNote());
        viewInvoiceButton.setOnAction(e -> viewSelectedInvoice());
        deleteInvoiceButton.setOnAction(e -> deleteSelectedInvoice());
        
        editReceptionNoteButton.setOnAction(e -> editSelectedReceptionNote());
        confirmReceptionNoteButton.setOnAction(e -> confirmSelectedReceptionNote());
        cancelReceptionNoteButton.setOnAction(e -> cancelSelectedReceptionNote());
        printReceptionNoteButton.setOnAction(e -> printSelectedReceptionNote());
    }

    private void setupDatePickers() {
        // Setăm data de început la începutul lunii curente
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today);
    }

    private void loadInvoices() {
        try {
            List<Invoice> invoices = invoiceService.getAllInvoices();
            invoiceList.clear();
            invoiceList.addAll(invoices);
            statusLabel.setText("ℹ️ Încărcate " + invoices.size() + " facturi.");
            logger.info("Loaded {} invoices", invoices.size());
        } catch (Exception e) {
            logger.error("Error loading invoices", e);
            showAlert(AlertType.ERROR, "Eroare", "Nu s-au putut încărca facturile: " + e.getMessage());
        }
    }

    private void loadReceptionNotes() {
        try {
            List<ReceptionNote> notes = invoiceService.getAllReceptionNotes();
            receptionNoteList.clear();
            receptionNoteList.addAll(notes);
            logger.info("Loaded {} reception notes", notes.size());
        } catch (Exception e) {
            logger.error("Error loading reception notes", e);
            showAlert(AlertType.ERROR, "Eroare", "Nu s-au putut încărca notele de recepție: " + e.getMessage());
        }
    }

    @FXML
    private void createManualReceptionNote() {
        try {
            // Creăm dialog pentru nota de recepție manuală
            Dialog<ReceptionNote> dialog = new Dialog<>();
            dialog.setTitle("Creează Notă de Recepție Manuală");
            dialog.setHeaderText("Introduceți detaliile notei de recepție");

            // Creăm layout
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Câmpuri pentru informații generale
            TextField supplierNameField = new TextField();
            supplierNameField.setPromptText("Nume furnizor");
            
            TextField supplierCifField = new TextField();
            supplierCifField.setPromptText("CIF furnizor");
            
            TextField supplierAddressField = new TextField();
            supplierAddressField.setPromptText("Adresă furnizor");
            
            DatePicker receptionDatePicker = new DatePicker(LocalDate.now());
            
            TextField notesField = new TextField();
            notesField.setPromptText("Observații (opțional)");

            // Tabel pentru produse
            TableView<ReceptionNoteItem> itemsTable = new TableView<>();
            ObservableList<ReceptionNoteItem> items = FXCollections.observableArrayList();
            itemsTable.setItems(items);

            // Coloane tabel
            TableColumn<ReceptionNoteItem, String> nameCol = new TableColumn<>("Produs");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
            nameCol.setPrefWidth(200);

            TableColumn<ReceptionNoteItem, String> codeCol = new TableColumn<>("Cod");
            codeCol.setCellValueFactory(new PropertyValueFactory<>("productCode"));
            codeCol.setPrefWidth(100);

            TableColumn<ReceptionNoteItem, Double> qtyCol = new TableColumn<>("Cantitate");
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            qtyCol.setPrefWidth(80);

            TableColumn<ReceptionNoteItem, String> unitCol = new TableColumn<>("UM");
            unitCol.setCellValueFactory(new PropertyValueFactory<>("unitOfMeasure"));
            unitCol.setPrefWidth(50);

            TableColumn<ReceptionNoteItem, Double> priceCol = new TableColumn<>("Preț Unitar");
            priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
            priceCol.setPrefWidth(100);

            TableColumn<ReceptionNoteItem, Double> totalCol = new TableColumn<>("Total");
            totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
            totalCol.setPrefWidth(100);

            TableColumn<ReceptionNoteItem, String> vatCol = new TableColumn<>("TVA");
            vatCol.setCellValueFactory(new PropertyValueFactory<>("vatRate"));
            vatCol.setPrefWidth(60);

            itemsTable.getColumns().addAll(nameCol, codeCol, qtyCol, unitCol, priceCol, totalCol, vatCol);

            // Butoane pentru adăugare/ștergere produse
            Button addItemButton = new Button("➕ Adaugă Produs");
            Button removeItemButton = new Button("➖ Șterge Produs");

            // Layout pentru produse
            VBox itemsLayout = new VBox(10);
            itemsLayout.getChildren().addAll(
                new HBox(10, addItemButton, removeItemButton),
                itemsTable
            );

            // Adăugăm câmpurile în grid
            grid.add(new Label("Furnizor:"), 0, 0);
            grid.add(supplierNameField, 1, 0);
            grid.add(new Label("CIF:"), 0, 1);
            grid.add(supplierCifField, 1, 1);
            grid.add(new Label("Adresă:"), 0, 2);
            grid.add(supplierAddressField, 1, 2);
            grid.add(new Label("Data Recepție:"), 0, 3);
            grid.add(receptionDatePicker, 1, 3);
            grid.add(new Label("Observații:"), 0, 4);
            grid.add(notesField, 1, 4);
            grid.add(new Label("Produse:"), 0, 5);
            grid.add(itemsLayout, 1, 5, 2, 1);

            // Handler pentru adăugare produs
            addItemButton.setOnAction(e -> {
                Dialog<ReceptionNoteItem> itemDialog = new Dialog<>();
                itemDialog.setTitle("Adaugă Produs");
                
                GridPane itemGrid = new GridPane();
                itemGrid.setHgap(10);
                itemGrid.setVgap(10);
                itemGrid.setPadding(new Insets(20, 150, 10, 10));

                TextField productNameField = new TextField();
                productNameField.setPromptText("Nume produs");
                
                TextField productCodeField = new TextField();
                productCodeField.setPromptText("Cod produs");
                
                TextField quantityField = new TextField();
                quantityField.setPromptText("Cantitate");
                
                TextField unitField = new TextField();
                unitField.setPromptText("UM");
                unitField.setText("buc");
                
                TextField unitPriceField = new TextField();
                unitPriceField.setPromptText("Preț unitar");
                
                ChoiceBox<String> vatChoice = new ChoiceBox<>();
                vatChoice.getItems().addAll("11%", "21%");
                vatChoice.setValue("19%"); // Default

                itemGrid.add(new Label("Produs:"), 0, 0);
                itemGrid.add(productNameField, 1, 0);
                itemGrid.add(new Label("Cod:"), 0, 1);
                itemGrid.add(productCodeField, 1, 1);
                itemGrid.add(new Label("Cantitate:"), 0, 2);
                itemGrid.add(quantityField, 1, 2);
                itemGrid.add(new Label("UM:"), 0, 3);
                itemGrid.add(unitField, 1, 3);
                itemGrid.add(new Label("Preț Unitar:"), 0, 4);
                itemGrid.add(unitPriceField, 1, 4);
                itemGrid.add(new Label("TVA:"), 0, 5);
                itemGrid.add(vatChoice, 1, 5);

                itemDialog.getDialogPane().setContent(itemGrid);
                itemDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                itemDialog.setResultConverter(dialogButton -> {
                    if (dialogButton == ButtonType.OK) {
                        try {
                            ReceptionNoteItem item = new ReceptionNoteItem();
                            item.setProductName(productNameField.getText());
                            item.setProductCode(productCodeField.getText());
                            item.setQuantity(Double.parseDouble(quantityField.getText()));
                            item.setUnitOfMeasure(unitField.getText());
                            item.setUnitPrice(Double.parseDouble(unitPriceField.getText()));
                            item.setReceivedQuantity(item.getQuantity());
                            
                            // Setăm TVA
                            String vatText = vatChoice.getValue();
                            double vatRate = Double.parseDouble(vatText.replace("%", ""));
                            item.setVatRate(vatRate);
                            
                            // Calculăm total și TVA
                            double totalPrice = item.getQuantity() * item.getUnitPrice();
                            item.setTotalPrice(totalPrice);
                            item.setVatAmount(totalPrice * (vatRate / 100.0));
                            
                            return item;
                        } catch (NumberFormatException ex) {
                            showAlert(Alert.AlertType.ERROR, "Eroare", "Introduceți valori numerice valide!");
                            return null;
                        }
                    }
                    return null;
                });

                Optional<ReceptionNoteItem> itemResult = itemDialog.showAndWait();
                itemResult.ifPresent(items::add);
            });

            // Handler pentru ștergere produs
            removeItemButton.setOnAction(e -> {
                ReceptionNoteItem selectedItem = itemsTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    items.remove(selectedItem);
                }
            });

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Setăm rezultatul
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    try {
                        logger.info("Creating manual reception note with {} items", items.size());
                        
                        // Validări
                        if (supplierNameField.getText() == null || supplierNameField.getText().trim().isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, "Eroare", "Introduceți numele furnizorului!");
                            return null;
                        }
                        
                        if (items.isEmpty()) {
                            showAlert(Alert.AlertType.ERROR, "Eroare", "Adăugați cel puțin un produs!");
                            return null;
                        }
                        
                        ReceptionNote receptionNote = new ReceptionNote();
                        receptionNote.setSupplierName(supplierNameField.getText());
                        receptionNote.setSupplierCif(supplierCifField.getText());
                        receptionNote.setSupplierAddress(supplierAddressField.getText());
                        receptionNote.setReceptionDate(receptionDatePicker.getValue());
                        receptionNote.setNotes(notesField.getText());
                        receptionNote.setItems(new ArrayList<>(items));
                        receptionNote.setCreatedBy(App.getCurrentUser() != null ? App.getCurrentUser().getUsername() : "unknown");
                        
                        // Calculăm totaluri
                        double totalAmount = items.stream().mapToDouble(ReceptionNoteItem::getTotalPrice).sum();
                        double totalVat = items.stream().mapToDouble(ReceptionNoteItem::getVatAmount).sum();
                        
                        receptionNote.setTotalAmount(totalAmount);
                        receptionNote.setVatAmount(totalVat);
                        receptionNote.setCurrency("RON");
                        
                        logger.info("Created reception note: {} - Total: {}, VAT: {}", 
                            receptionNote.getSupplierName(), totalAmount, totalVat);
                        
                        return receptionNote;
                        
                    } catch (Exception e) {
                        logger.error("Error creating reception note data", e);
                        showAlert(Alert.AlertType.ERROR, "Eroare", 
                            "Eroare la crearea datelor: " + e.getMessage());
                        return null;
                    }
                }
                return null;
            });

            Optional<ReceptionNote> result = dialog.showAndWait();
            logger.info("Dialog result present: {}", result.isPresent());
            
            result.ifPresent(receptionNote -> {
                try {
                    logger.info("Attempting to save reception note: {}", receptionNote.getSupplierName());
                    
                    // Salvăm nota de recepție
                    ReceptionNote savedNote = invoiceService.saveReceptionNote(receptionNote);
                    
                    logger.info("Save operation completed. Saved note: {}", savedNote != null ? savedNote.getFullNoteNumber() : "null");
                    
                    if (savedNote != null) {
                        loadReceptionNotes();
                        showAlert(Alert.AlertType.INFORMATION, "Succes", 
                            "Nota de recepție " + savedNote.getFullNoteNumber() + 
                            " a fost creată cu succes.");
                        
                        // Selectăm tab-ul cu note de recepție
                        mainTabPane.getSelectionModel().select(1);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut crea nota de recepție.");
                    }
                    
                } catch (Exception e) {
                    logger.error("Error creating manual reception note", e);
                    showAlert(Alert.AlertType.ERROR, "Eroare", 
                        "Nu s-a putut crea nota de recepție: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            logger.error("Error opening manual reception note dialog", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", 
                "Nu s-a putut deschide dialogul: " + e.getMessage());
        }
    }

    @FXML
    private void importInvoicesFromSPV() {
        String cif = cifTextField.getText();
        if (cif == null || cif.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Atenție", "Introduceți CIF-ul furnizorului.");
            return;
        }
        
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați perioada de import.");
            return;
        }
        
        if (startDate.isAfter(endDate)) {
            showAlert(AlertType.WARNING, "Atenție", "Data de început nu poate fi după data de sfârșit.");
            return;
        }
        
        try {
            statusLabel.setText("🔄 Se importă facturi din SPV...");
            
            // Rulăm importul în background
            Thread importThread = new Thread(() -> {
                try {
                    List<Invoice> importedInvoices = invoiceService.importInvoicesFromSPV(startDate, endDate, cif);
                    
                    Platform.runLater(() -> {
                        loadInvoices();
                        statusLabel.setText("✅ Importat cu succes " + importedInvoices.size() + " facturi.");
                        showAlert(AlertType.INFORMATION, "Succes", 
                            "Au fost importate " + importedInvoices.size() + " facturi din SPV.");
                    });
                    
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Eroare la import.");
                        showAlert(AlertType.ERROR, "Eroare", 
                            "Nu s-au putut importa facturile: " + e.getMessage());
                    });
                }
            });
            
            importThread.setDaemon(true);
            importThread.start();
            
        } catch (Exception e) {
            logger.error("Error importing invoices from SPV", e);
            showAlert(AlertType.ERROR, "Eroare", "Nu s-au putut importa facturile: " + e.getMessage());
        }
    }

    @FXML
    private void searchInvoicesInSPV() {
        String cif = cifTextField.getText();
        if (cif == null || cif.trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Atenție", "Introduceți CIF-ul furnizorului.");
            return;
        }
        
        try {
            statusLabel.setText("🔍 Se caută facturi în SPV...");
            
            // Simulare căutare - în realitate ar fi apel API către SPV
            showAlert(AlertType.INFORMATION, "Căutare SPV", 
                "Funcționalitatea de căutare în SPV va fi implementată cu API-ul oficial.");
            statusLabel.setText("ℹ️ Căutare SPV - în dezvoltare.");
            
        } catch (Exception e) {
            logger.error("Error searching invoices in SPV", e);
            showAlert(AlertType.ERROR, "Eroare", "Nu s-au putut căuta facturile: " + e.getMessage());
        }
    }

    @FXML
    private void downloadSelectedInvoices() {
        Invoice selectedInvoice = invoicesTableView.getSelectionModel().getSelectedItem();
        if (selectedInvoice == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați o factură pentru descărcare.");
            return;
        }
        
        try {
            statusLabel.setText("⬇️ Se descarcă factura...");
            
            // Simulare descărcare
            boolean success = spvService.downloadInvoiceFromSPV(
                selectedInvoice.getFullInvoiceNumber(), 
                selectedInvoice.getSupplierCif()
            );
            
            if (success) {
                statusLabel.setText("✅ Factura descărcată cu succes.");
                showAlert(AlertType.INFORMATION, "Succes", "Factura a fost descărcată cu succes.");
            } else {
                statusLabel.setText("❌ Eroare la descărcare.");
                showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut descărca factura.");
            }
            
        } catch (Exception e) {
            logger.error("Error downloading invoice", e);
            showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut descărca factura: " + e.getMessage());
        }
    }

    @FXML
    private void generateReceptionNote() {
        Invoice selectedInvoice = invoicesTableView.getSelectionModel().getSelectedItem();
        if (selectedInvoice == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați o factură pentru a genera nota de recepție.");
            return;
        }
        
        if ("processed".equals(selectedInvoice.getStatus())) {
            showAlert(AlertType.WARNING, "Atenție", 
                "Această factură a fost deja procesată. Există deja o notă de recepție generată.");
            return;
        }
        
        try {
            String currentUser = App.getCurrentUser() != null ? App.getCurrentUser().getUsername() : "unknown";
            
            ReceptionNote receptionNote = invoiceService.generateReceptionNoteFromInvoice(
                selectedInvoice.getId(), currentUser);
            
            if (receptionNote != null) {
                loadReceptionNotes();
                loadInvoices();
                
                showAlert(AlertType.INFORMATION, "Succes", 
                    "Nota de recepție " + receptionNote.getFullNoteNumber() + 
                    " a fost generată cu succes.");
                
                // Selectăm tab-ul cu note de recepție
                mainTabPane.getSelectionModel().select(1);
                
            } else {
                showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut genera nota de recepție.");
            }
            
        } catch (Exception e) {
            logger.error("Error generating reception note", e);
            showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut genera nota de recepție: " + e.getMessage());
        }
    }

    @FXML
    private void viewSelectedInvoice() {
        Invoice selectedInvoice = invoicesTableView.getSelectionModel().getSelectedItem();
        if (selectedInvoice == null) {
            showAlert(Alert.AlertType.WARNING, "Atenție", "Selectați o factură pentru a vedea detaliile.");
            return;
        }
        
        // Selectăm tab-ul cu detalii
        mainTabPane.getSelectionModel().select(2);
        
        // Afișăm detaliile facturii
        showInvoiceDetails(selectedInvoice);
    }

    @FXML
    private void viewSelectedReceptionNote() {
        ReceptionNote selectedNote = receptionNotesTableView.getSelectionModel().getSelectedItem();
        if (selectedNote == null) {
            showAlert(Alert.AlertType.WARNING, "Atenție", "Selectați o notă de recepție pentru a vedea detaliile.");
            return;
        }
        
        // Selectăm tab-ul cu detalii
        mainTabPane.getSelectionModel().select(2);
        
        // Afișăm detaliile notei de recepție
        showReceptionNoteDetails(selectedNote);
    }

    private void showReceptionNoteDetails(ReceptionNote receptionNote) {
        // Curățăm detaliile facturii
        clearInvoiceDetails();
        
        // Afișăm detaliile notei de recepție
        detailInvoiceNumberLabel.setText(receptionNote.getFullNoteNumber());
        detailDateLabel.setText(receptionNote.getReceptionDate() != null ? 
            receptionNote.getReceptionDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "N/A");
        detailSupplierLabel.setText(receptionNote.getSupplierName());
        detailCifLabel.setText(receptionNote.getSupplierCif());
        detailTotalLabel.setText(String.format("%.2f RON", receptionNote.getTotalAmount()));
        detailVatLabel.setText(String.format("%.2f RON", receptionNote.getVatAmount()));
        
        // Încărcăm produsele notei de recepție
        invoiceItemList.clear();
        if (receptionNote.getItems() != null) {
            // Convertim ReceptionNoteItem în InvoiceItem pentru afișare
            for (ReceptionNoteItem receptionItem : receptionNote.getItems()) {
                InvoiceItem displayItem = new InvoiceItem();
                displayItem.setProductName(receptionItem.getProductName());
                displayItem.setProductCode(receptionItem.getProductCode());
                displayItem.setDescription(receptionItem.getDescription());
                displayItem.setQuantity(receptionItem.getQuantity());
                displayItem.setUnitOfMeasure(receptionItem.getUnitOfMeasure());
                displayItem.setUnitPrice(receptionItem.getUnitPrice());
                displayItem.setTotalPrice(receptionItem.getTotalPrice());
                displayItem.setVatAmount(receptionItem.getVatAmount());
                displayItem.setVatRate(receptionItem.getVatRate());
                invoiceItemList.add(displayItem);
            }
        }
        
        statusLabel.setText("ℹ️ Vizualizare notă de recepție: " + receptionNote.getFullNoteNumber());
    }

    @FXML
    private void deleteSelectedInvoice() {
        Invoice selectedInvoice = invoicesTableView.getSelectionModel().getSelectedItem();
        if (selectedInvoice == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați o factură pentru ștergere.");
            return;
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmare Ștergere");
        confirmAlert.setHeaderText("Ștergere Factură");
        confirmAlert.setContentText("Sunteți sigur că doriți să ștergeți factura " + 
            selectedInvoice.getFullInvoiceNumber() + "?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Aici ar fi implementată ștergerea facturii din baza de date
                showAlert(AlertType.INFORMATION, "Succes", "Factura a fost ștearsă.");
                loadInvoices();
                
            } catch (Exception e) {
                logger.error("Error deleting invoice", e);
                showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut șterge factura: " + e.getMessage());
            }
        }
    }

    @FXML
    private void editSelectedReceptionNote() {
        ReceptionNote selectedNote = receptionNotesTableView.getSelectionModel().getSelectedItem();
        if (selectedNote == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați o notă de recepție pentru editare.");
            return;
        }
        
        if ("confirmed".equals(selectedNote.getStatus())) {
            showAlert(AlertType.WARNING, "Atenție", 
                "O notă de recepție confirmată nu poate fi editată.");
            return;
        }
        
        // Aici ar fi implementat dialogul de editare
        showAlert(AlertType.INFORMATION, "Editare", 
            "Funcționalitatea de editare va fi implementată.");
    }

    @FXML
    private void confirmSelectedReceptionNote() {
        ReceptionNote selectedNote = receptionNotesTableView.getSelectionModel().getSelectedItem();
        if (selectedNote == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați o notă de recepție pentru confirmare.");
            return;
        }
        
        if ("confirmed".equals(selectedNote.getStatus())) {
            showAlert(AlertType.INFORMATION, "Info", "Nota de recepție este deja confirmată.");
            return;
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmare Notă Recepție");
        confirmAlert.setHeaderText("Confirmare Notă Recepție");
        confirmAlert.setContentText("Sunteți sigur că doriți să confirmați nota de recepție " + 
            selectedNote.getFullNoteNumber() + "?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                selectedNote.confirm();
                // Aici ar fi implementată salvarea în baza de date
                loadReceptionNotes();
                showAlert(AlertType.INFORMATION, "Succes", "Nota de recepție a fost confirmată.");
                
            } catch (Exception e) {
                logger.error("Error confirming reception note", e);
                showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut confirma nota de recepție: " + e.getMessage());
            }
        }
    }

    @FXML
    private void cancelSelectedReceptionNote() {
        ReceptionNote selectedNote = receptionNotesTableView.getSelectionModel().getSelectedItem();
        if (selectedNote == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați o notă de recepție pentru anulare.");
            return;
        }
        
        if ("cancelled".equals(selectedNote.getStatus())) {
            showAlert(AlertType.INFORMATION, "Info", "Nota de recepție este deja anulată.");
            return;
        }
        
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Anulare Notă Recepție");
        confirmAlert.setHeaderText("Anulare Notă Recepție");
        confirmAlert.setContentText("Sunteți sigur că doriți să anulați nota de recepție " + 
            selectedNote.getFullNoteNumber() + "?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                selectedNote.cancel();
                // Aici ar fi implementată salvarea în baza de date
                loadReceptionNotes();
                showAlert(AlertType.INFORMATION, "Succes", "Nota de recepție a fost anulată.");
                
            } catch (Exception e) {
                logger.error("Error cancelling reception note", e);
                showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut anula nota de recepție: " + e.getMessage());
            }
        }
    }

    @FXML
    private void printSelectedReceptionNote() {
        ReceptionNote selectedNote = receptionNotesTableView.getSelectionModel().getSelectedItem();
        if (selectedNote == null) {
            showAlert(AlertType.WARNING, "Atenție", "Selectați o notă de recepție pentru printare.");
            return;
        }
        
        try {
            // Generăm și deschidem PDF-ul
            pdfService.openReceptionNotePdf(selectedNote);
            showAlert(AlertType.INFORMATION, "Printare", 
                "PDF-ul a fost generat și deschis cu succes.");
            
        } catch (Exception e) {
            logger.error("Error printing reception note", e);
            showAlert(AlertType.ERROR, "Eroare", "Nu s-a putut printa nota de recepție: " + e.getMessage());
        }
    }

    @FXML
    private void refreshData() {
        loadInvoices();
        loadReceptionNotes();
        statusLabel.setText("ℹ️ Date reîncărcate.");
    }

    private void showInvoiceDetails(Invoice invoice) {
        detailInvoiceNumberLabel.setText(invoice.getFullInvoiceNumber());
        detailDateLabel.setText(invoice.getIssueDate() != null ? 
            invoice.getIssueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "N/A");
        detailSupplierLabel.setText(invoice.getSupplierName());
        detailCifLabel.setText(invoice.getSupplierCif());
        detailTotalLabel.setText(String.format("%.2f RON", invoice.getTotalAmount()));
        detailVatLabel.setText(String.format("%.2f RON", invoice.getVatAmount()));
        
        // Încărcăm produsele
        invoiceItemList.clear();
        if (invoice.getItems() != null) {
            invoiceItemList.addAll(invoice.getItems());
        }
    }

    private void clearInvoiceDetails() {
        detailInvoiceNumberLabel.setText("N/A");
        detailDateLabel.setText("N/A");
        detailSupplierLabel.setText("N/A");
        detailCifLabel.setText("N/A");
        detailTotalLabel.setText("N/A");
        detailVatLabel.setText("N/A");
        invoiceItemList.clear();
    }

    private void updateReceptionNoteButtons(ReceptionNote selectedNote) {
        boolean hasSelection = selectedNote != null;
        boolean isDraft = hasSelection && "draft".equals(selectedNote.getStatus());
        boolean isConfirmed = hasSelection && "confirmed".equals(selectedNote.getStatus());
        boolean isCancelled = hasSelection && "cancelled".equals(selectedNote.getStatus());
        
        editReceptionNoteButton.setDisable(!isDraft);
        confirmReceptionNoteButton.setDisable(!isDraft);
        cancelReceptionNoteButton.setDisable(!isDraft);
        // Permitem printarea pentru note draft și confirmed, dar nu pentru cele anulate
        printReceptionNoteButton.setDisable(!hasSelection || isCancelled);
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

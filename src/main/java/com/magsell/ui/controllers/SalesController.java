package com.magsell.ui.controllers;

import com.magsell.models.Sale;
import com.magsell.services.SalesService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller pentru lista de vânzări.
 */
public class SalesController {
    private static final Logger logger = LoggerFactory.getLogger(SalesController.class);
    private final SalesService salesService = new SalesService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ro"));

    @FXML
    private TableView<Sale> salesTable;

    private ObservableList<Sale> salesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        logger.info("Initializing SalesController");
        setupTableColumns();
        loadSales();
    }

    /**
     * Configurează coloanele tabelei.
     */
    private void setupTableColumns() {
        if (salesTable == null) return;

        TableColumn<Sale, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);

        TableColumn<Sale, String> productCol = new TableColumn<>("Produs");
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productCol.setPrefWidth(200);

        TableColumn<Sale, Integer> quantityCol = new TableColumn<>("Cantitate");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(100);

        TableColumn<Sale, BigDecimal> unitPriceCol = new TableColumn<>("Preț unitar");
        unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        unitPriceCol.setPrefWidth(120);
        unitPriceCol.setCellFactory(column -> new javafx.scene.control.TableCell<Sale, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f RON", item.doubleValue()));
                }
            }
        });

        TableColumn<Sale, BigDecimal> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        totalCol.setPrefWidth(120);
        totalCol.setCellFactory(column -> new javafx.scene.control.TableCell<Sale, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f RON", item.doubleValue()));
                }
            }
        });

        TableColumn<Sale, LocalDateTime> dateCol = new TableColumn<>("Data");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        dateCol.setPrefWidth(180);
        dateCol.setCellFactory(column -> new javafx.scene.control.TableCell<Sale, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(dateFormatter));
                }
            }
        });

        salesTable.getColumns().setAll(idCol, productCol, quantityCol, unitPriceCol, totalCol, dateCol);
    }

    /**
     * Încarcă vânzările.
     */
    private void loadSales() {
        new Thread(() -> {
            try {
                List<Sale> sales = salesService.getAllSales();
                Platform.runLater(() -> {
                    salesList = FXCollections.observableArrayList(sales);
                    salesTable.setItems(salesList);
                    logger.info("Încărcate " + sales.size() + " vânzări");
                });
            } catch (SQLException e) {
                logger.error("Eroare la încărcarea vânzărilor", e);
                Platform.runLater(() -> {
                    // Afișează eroare în UI dacă e necesar
                });
            }
        }).start();
    }

    /**
     * Reîmprospătează lista de vânzări.
     */
    @FXML
    public void handleRefresh() {
        loadSales();
    }
}

package com.magsell.ui.controllers;

import com.magsell.models.Partner;
import com.magsell.services.PartnerService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller pentru modul Parteneri în noul design modern
 */
public class PartnersViewController {
    private static final Logger logger = LoggerFactory.getLogger(PartnersViewController.class);
    
    private final PartnerService partnerService;
    private final ObservableList<Partner> partnerList;
    
    @FXML
    private TableView<Partner> partnersTable;
    @FXML
    private TableColumn<Partner, Integer> partnerIdColumn;
    @FXML
    private TableColumn<Partner, String> partnerNameColumn;
    @FXML
    private TableColumn<Partner, String> partnerCodeColumn;
    @FXML
    private TableColumn<Partner, String> partnerTypeColumn;
    @FXML
    private TableColumn<Partner, String> partnerContactColumn;
    @FXML
    private TableColumn<Partner, String> partnerPhoneColumn;
    @FXML
    private TableColumn<Partner, String> partnerEmailColumn;
    @FXML
    private TableColumn<Partner, LocalDateTime> partnerCreatedAtColumn;
    @FXML
    private TableColumn<Partner, Void> partnerActionsColumn;
    
    @FXML
    private Label totalPartnersLabel;
    @FXML
    private Label clientsLabel;
    @FXML
    private Label suppliersLabel;
    @FXML
    private Label lastUpdateLabel;
    
    public PartnersViewController() {
        this.partnerService = new PartnerService();
        this.partnerList = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing PartnersViewController");
        
        setupTable();
        loadPartners();
        
        logger.info("PartnersViewController initialized successfully");
    }
    
    private void setupTable() {
        partnerIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        partnerNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        partnerCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        partnerTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        partnerContactColumn.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        partnerPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        partnerEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        partnerCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Custom cell factory for date formatting
        partnerCreatedAtColumn.setCellFactory(column -> new TableCell<Partner, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText("");
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
                }
            }
        });
        
        // Custom cell factory for actions
        partnerActionsColumn.setCellFactory(column -> new TableCell<Partner, Void>() {
            private final Button editButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");
            private final HBox buttons = new HBox(5, editButton, deleteButton);
            
            {
                editButton.setOnAction(e -> editPartner(getTableView().getItems().get(getIndex())));
                deleteButton.setOnAction(e -> deletePartner(getTableView().getItems().get(getIndex())));
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
        
        partnersTable.setItems(partnerList);
    }
    
    private void loadPartners() {
        try {
            partnerList.clear();
            partnerList.addAll(partnerService.getAllPartners());
            
            totalPartnersLabel.setText("👥 Total parteneri: " + partnerList.size());
            
            // Count by type
            long clientsCount = partnerList.stream()
                .filter(partner -> "CLIENT".equals(partner.getType()))
                .count();
            long suppliersCount = partnerList.stream()
                .filter(partner -> "FORNIZOR".equals(partner.getType()))
                .count();
            
            clientsLabel.setText("👤 Clienți: " + clientsCount);
            suppliersLabel.setText("🏭 Furnizori: " + suppliersCount);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            lastUpdateLabel.setText("Ultima actualizare: " + LocalDateTime.now().format(formatter));
            
            logger.info("Loaded {} partners ({} clients, {} suppliers)", 
                partnerList.size(), clientsCount, suppliersCount);
        } catch (Exception e) {
            logger.error("Error loading partners", e);
            totalPartnersLabel.setText("👥 Total parteneri: 0");
            clientsLabel.setText("👤 Clienți: 0");
            suppliersLabel.setText("🏭 Furnizori: 0");
        }
    }
    
    @FXML
    private void addPartner() {
        // TODO: Implement dialog for adding partner
        logger.info("Add partner clicked");
    }
    
    @FXML
    private void refreshPartners() {
        loadPartners();
        logger.info("Partners refreshed");
    }
    
    private void editPartner(Partner partner) {
        // TODO: Implement dialog for editing partner
        logger.info("Edit partner: {}", partner.getName());
    }
    
    private void deletePartner(Partner partner) {
        // TODO: Implement partner deletion with confirmation
        logger.info("Delete partner: {}", partner.getName());
    }
}

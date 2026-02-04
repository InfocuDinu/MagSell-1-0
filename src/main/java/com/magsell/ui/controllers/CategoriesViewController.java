package com.magsell.ui.controllers;

import com.magsell.models.Category;
import com.magsell.services.CategoryService;
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
 * Controller pentru modul Categorii în noul design modern
 */
public class CategoriesViewController {
    private static final Logger logger = LoggerFactory.getLogger(CategoriesViewController.class);
    
    private final CategoryService categoryService;
    private final ObservableList<Category> categoryList;
    
    @FXML
    private TableView<Category> categoriesTable;
    @FXML
    private TableColumn<Category, Integer> categoryIdColumn;
    @FXML
    private TableColumn<Category, String> categoryNameColumn;
    @FXML
    private TableColumn<Category, String> categoryDescriptionColumn;
    @FXML
    private TableColumn<Category, LocalDateTime> categoryCreatedAtColumn;
    @FXML
    private TableColumn<Category, Void> categoryActionsColumn;
    
    @FXML
    private Label totalCategoriesLabel;
    @FXML
    private Label lastUpdateLabel;
    
    public CategoriesViewController() {
        this.categoryService = new CategoryService();
        this.categoryList = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing CategoriesViewController");
        
        setupTable();
        loadCategories();
        
        logger.info("CategoriesViewController initialized successfully");
    }
    
    private void setupTable() {
        categoryIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Custom cell factory for date formatting
        categoryCreatedAtColumn.setCellFactory(column -> new TableCell<Category, LocalDateTime>() {
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
        categoryActionsColumn.setCellFactory(column -> new TableCell<Category, Void>() {
            private final Button editButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");
            private final HBox buttons = new HBox(5, editButton, deleteButton);
            
            {
                editButton.setOnAction(e -> editCategory(getTableView().getItems().get(getIndex())));
                deleteButton.setOnAction(e -> deleteCategory(getTableView().getItems().get(getIndex())));
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
        
        categoriesTable.setItems(categoryList);
    }
    
    private void loadCategories() {
        try {
            categoryList.clear();
            categoryList.addAll(categoryService.getAllCategories());
            totalCategoriesLabel.setText("📊 Total categorii: " + categoryList.size());
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            lastUpdateLabel.setText("Ultima actualizare: " + LocalDateTime.now().format(formatter));
            
            logger.info("Loaded {} categories", categoryList.size());
        } catch (Exception e) {
            logger.error("Error loading categories", e);
            totalCategoriesLabel.setText("📊 Total categorii: 0");
        }
    }
    
    @FXML
    private void addCategory() {
        // TODO: Implement dialog for adding category
        logger.info("Add category clicked");
    }
    
    @FXML
    private void refreshCategories() {
        loadCategories();
        logger.info("Categories refreshed");
    }
    
    private void editCategory(Category category) {
        // TODO: Implement dialog for editing category
        logger.info("Edit category: {}", category.getName());
    }
    
    private void deleteCategory(Category category) {
        // TODO: Implement category deletion with confirmation
        logger.info("Delete category: {}", category.getName());
    }
}

package com.magsell.ui.controllers;

import com.magsell.models.Category;
import com.magsell.services.CategoryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Controlerul pentru managerul de categorii.
 * Gestionarea listei de categorii, adăugare, editare, ștergere.
 */
public class CategoryController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryService categoryService = new CategoryService();

    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Category, Integer> idColumn;
    @FXML
    private TableColumn<Category, String> nameColumn;
    @FXML
    private TableColumn<Category, String> descriptionColumn;
    
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button newButton;
    @FXML
    private Button editButton;

    private ObservableList<Category> categoryList;
    private Category currentCategory;
    private boolean isEditing = false;

    @FXML
    public void initialize() {
        setupTable();
        loadCategories();
        setupButtonActions();
        setFormEnabled(false);
    }

    private void setupTable() {
        categoryList = FXCollections.observableArrayList();
        categoryTable.setItems(categoryList);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Enable selection
        categoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectCategory(newSelection);
                }
            });
    }

    private void setupButtonActions() {
        newButton.setOnAction(e -> newCategory());
        editButton.setOnAction(e -> editCategory());
        deleteButton.setOnAction(e -> deleteCategory());
        saveButton.setOnAction(e -> saveCategory());
        cancelButton.setOnAction(e -> cancelEdit());
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            categoryList.clear();
            categoryList.addAll(categories);
            logger.info("Încărcate " + categories.size() + " categorii");
        } catch (SQLException e) {
            logger.error("Eroare la încărcarea categoriilor", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-au putut încărca categoriile: " + e.getMessage());
        }
    }

    private void newCategory() {
        currentCategory = new Category();
        isEditing = false;
        clearForm();
        setFormEnabled(true);
        nameField.requestFocus();
    }

    private void editCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Atenție", "Selectați o categorie pentru editare");
            return;
        }

        currentCategory = selected;
        isEditing = true;
        loadCategoryToForm(selected);
        setFormEnabled(true);
        nameField.requestFocus();
    }

    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Atenție", "Selectați o categorie pentru ștergere");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmare ștergere");
        confirm.setHeaderText("Ștergeți categoria '" + selected.getName() + "'?");
        confirm.setContentText("Această acțiune nu poate fi anulată.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                // Verificăm dacă categoria poate fi ștearsă
                if (!categoryService.canDeleteCategory(selected.getId())) {
                    showAlert(Alert.AlertType.ERROR, "Eroare", 
                        "Categoria nu poate fi ștearsă deoarece are produse asociate.");
                    return;
                }

                categoryService.deleteCategory(selected.getId());
                loadCategories();
                clearForm();
                setFormEnabled(false);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Categoria a fost ștearsă cu succes");
            } catch (SQLException e) {
                logger.error("Eroare la ștergerea categoriei", e);
                showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut șterge categoria: " + e.getMessage());
            }
        }
    }

    private void saveCategory() {
        if (!validateForm()) {
            return;
        }

        try {
            // Load form data to category
            currentCategory.setName(nameField.getText().trim());
            currentCategory.setDescription(descriptionField.getText().trim());

            if (isEditing) {
                categoryService.updateCategory(currentCategory);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Categoria a fost actualizată cu succes");
            } else {
                categoryService.createCategory(currentCategory);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Categoria a fost creată cu succes");
            }

            loadCategories();
            clearForm();
            setFormEnabled(false);
        } catch (SQLException e) {
            logger.error("Eroare la salvarea categoriei", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut salva categoria: " + e.getMessage());
        }
    }

    private void cancelEdit() {
        clearForm();
        setFormEnabled(false);
        currentCategory = null;
        isEditing = false;
    }

    private void selectCategory(Category category) {
        if (!isEditing) {
            loadCategoryToForm(category);
        }
    }

    private void loadCategoryToForm(Category category) {
        nameField.setText(category.getName());
        descriptionField.setText(category.getDescription());
    }

    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
    }

    private void setFormEnabled(boolean enabled) {
        nameField.setDisable(!enabled);
        descriptionField.setDisable(!enabled);
        saveButton.setVisible(enabled);
        cancelButton.setVisible(enabled);
        newButton.setDisable(enabled);
        editButton.setDisable(enabled || categoryTable.getSelectionModel().getSelectedItem() == null);
        deleteButton.setDisable(enabled || categoryTable.getSelectionModel().getSelectedItem() == null);
    }

    private boolean validateForm() {
        String name = nameField.getText().trim();
        
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Eroare", "Numele categoriei este obligatoriu");
            nameField.requestFocus();
            return false;
        }

        // Verificăm duplicarea numelui
        try {
            Category existing = categoryService.getCategoryByName(name);
            if (existing != null && (currentCategory.getId() <= 0 || existing.getId() != currentCategory.getId())) {
                showAlert(Alert.AlertType.ERROR, "Eroare", "Există deja o categorie cu acest nume");
                nameField.requestFocus();
                return false;
            }
        } catch (SQLException e) {
            logger.error("Eroare la validarea numelui categoriei", e);
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

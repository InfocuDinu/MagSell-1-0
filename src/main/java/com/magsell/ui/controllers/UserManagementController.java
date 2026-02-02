package com.magsell.ui.controllers;

import com.magsell.models.User;
import com.magsell.services.UserService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller pentru gestionarea utilizatorilor și permisiunilor.
 */
public class UserManagementController {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);
    private final UserService userService = new UserService();

    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, Integer> idColumn;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, Boolean> manageProductsColumn;
    @FXML
    private TableColumn<User, Boolean> manageCategoriesColumn;
    @FXML
    private TableColumn<User, Boolean> manageUsersColumn;
    @FXML
    private TableColumn<User, Boolean> viewReportsColumn;
    @FXML
    private TableColumn<User, Boolean> manageSalesColumn;

    @FXML
    private Button newUserButton;
    @FXML
    private Button editUserButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Label statusLabel;

    private ObservableList<User> userList;

    @FXML
    public void initialize() {
        setupTable();
        loadUsers();
        setupButtonActions();
    }

    private void setupTable() {
        userList = FXCollections.observableArrayList();
        userTable.setItems(userList);

        // Setup columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Setup permission columns with checkboxes
        setupPermissionColumn(manageProductsColumn, "canManageProducts");
        setupPermissionColumn(manageCategoriesColumn, "canManageCategories");
        setupPermissionColumn(manageUsersColumn, "canManageUsers");
        setupPermissionColumn(viewReportsColumn, "canViewReports");
        setupPermissionColumn(manageSalesColumn, "canManageSales");

        // Enable editing for permission columns
        userTable.setEditable(true);
    }

    private void setupPermissionColumn(TableColumn<User, Boolean> column, String propertyName) {
        // Folosim direct JavaFX Properties din modelul User
        column.setCellValueFactory(param -> {
            User user = param.getValue();
            switch (propertyName) {
                case "canManageProducts":
                    return user.canManageProductsProperty();
                case "canManageCategories":
                    return user.canManageCategoriesProperty();
                case "canManageUsers":
                    return user.canManageUsersProperty();
                case "canViewReports":
                    return user.canViewReportsProperty();
                case "canManageSales":
                    return user.canManageSalesProperty();
                default:
                    return new javafx.beans.property.SimpleBooleanProperty(false);
            }
        });
        
        column.setCellFactory(CheckBoxTableCell.forTableColumn(column));
        
        column.setOnEditCommit(event -> {
            User user = event.getRowValue();
            
            // Verificăm dacă utilizatorul este admin - nu permitem modificarea
            if (user.isAdmin()) {
                showAlert(Alert.AlertType.WARNING, "Atenție", 
                    "Permisiunile utilizatorilor admin nu pot fi modificate. Au acces la toate funcționalitățile.");
                // Restaurăm starea corectă
                loadUsers();
                return;
            }
            
            try {
                boolean newValue = event.getNewValue();
                
                if ("canManageProducts".equals(propertyName)) {
                    user.setCanManageProducts(newValue);
                } else if ("canManageCategories".equals(propertyName)) {
                    user.setCanManageCategories(newValue);
                } else if ("canManageUsers".equals(propertyName)) {
                    user.setCanManageUsers(newValue);
                } else if ("canViewReports".equals(propertyName)) {
                    user.setCanViewReports(newValue);
                } else if ("canManageSales".equals(propertyName)) {
                    user.setCanManageSales(newValue);
                }
                
                userService.updateUserPermissions(user);
                logger.info("Updated permission {} for user: {} = {}", propertyName, user.getUsername(), newValue);
                
                // Afișăm un mesaj de confirmare scurt
                showStatusMessage("Permisiune actualizată: " + propertyName + " = " + newValue);
                
            } catch (SQLException e) {
                logger.error("Error updating user permissions", e);
                showAlert(Alert.AlertType.ERROR, "Erore", "Nu s-au putut actualiza permisiunile: " + e.getMessage());
                // Revert the change
                loadUsers();
            }
        });
    }

    private void setupButtonActions() {
        newUserButton.setOnAction(e -> createNewUser());
        editUserButton.setOnAction(e -> editUser());
        deleteButton.setOnAction(e -> deleteUser());
        refreshButton.setOnAction(e -> loadUsers());
    }

    /**
     * Face căsuțele readonly pentru admini după încărcare
     */
    private void makeAdminCellsReadOnly() {
        // Iterăm prin toate rândurile și facem căsuțele readonly pentru admini
        for (User user : userList) {
            if (user.isAdmin()) {
                // Facem toate celulele de permisiuni readonly pentru acest rând
                for (int col = 3; col < userTable.getColumns().size(); col++) {
                    TableColumn<User, ?> column = userTable.getColumns().get(col);
                    // Verificăm dacă este o coloană de permisiuni
                    if (column.getText().toLowerCase().contains("manage") || 
                        column.getText().toLowerCase().contains("view")) {
                        column.setEditable(false);
                    }
                }
            }
        }
        userTable.refresh();
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            userList.clear();
            userList.addAll(users);
            logger.info("Loaded " + users.size() + " users");
            
            // Facem căsuțele readonly pentru admini
            makeAdminCellsReadOnly();
        } catch (SQLException e) {
            logger.error("Error loading users", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-au putut încărca utilizatorii: " + e.getMessage());
        }
    }

    @FXML
    private void editUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Atenție", "Selectați un utilizator pentru a-l edita");
            return;
        }

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Editează Utilizator: " + selected.getUsername());
        dialog.setHeaderText("Modificați permisiunile pentru " + selected.getUsername());

        // Creăm formularul
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("user", "admin");
        roleCombo.setValue(selected.getRole());

        // Checkbox-uri pentru permisiuni
        CheckBox productsCheck = new CheckBox("Gestionare Produse");
        productsCheck.setSelected(selected.canManageProducts());
        
        CheckBox categoriesCheck = new CheckBox("Gestionare Categorii");
        categoriesCheck.setSelected(selected.canManageCategories());
        
        CheckBox usersCheck = new CheckBox("Gestionare Utilizatori");
        usersCheck.setSelected(selected.canManageUsers());
        
        CheckBox reportsCheck = new CheckBox("Vizualizare Rapoarte");
        reportsCheck.setSelected(selected.canViewReports());
        
        CheckBox salesCheck = new CheckBox("Gestionare Vânzări");
        salesCheck.setSelected(selected.canManageSales());

        // Dacă este admin, dezactivăm toate controalele
        boolean isAdmin = selected.isAdmin();
        roleCombo.setDisable(isAdmin);
        productsCheck.setDisable(isAdmin);
        categoriesCheck.setDisable(isAdmin);
        usersCheck.setDisable(isAdmin);
        reportsCheck.setDisable(isAdmin);
        salesCheck.setDisable(isAdmin);

        grid.add(new Label("Rol:"), 0, 0);
        grid.add(roleCombo, 1, 0);
        grid.add(new Label("Permisiuni:"), 0, 1);
        grid.add(productsCheck, 1, 1);
        grid.add(categoriesCheck, 1, 2);
        grid.add(usersCheck, 1, 3);
        grid.add(reportsCheck, 1, 4);
        grid.add(salesCheck, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK && !isAdmin) {
                try {
                    // Actualizăm utilizatorul
                    selected.setRole(roleCombo.getValue());
                    selected.setCanManageProducts(productsCheck.isSelected());
                    selected.setCanManageCategories(categoriesCheck.isSelected());
                    selected.setCanManageUsers(usersCheck.isSelected());
                    selected.setCanViewReports(reportsCheck.isSelected());
                    selected.setCanManageSales(salesCheck.isSelected());

                    userService.updateUserPermissions(selected);
                    showAlert(Alert.AlertType.INFORMATION, "Succes", "Utilizator actualizat cu succes");
                    loadUsers();
                } catch (SQLException e) {
                    logger.error("Error updating user", e);
                    showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut actualiza utilizatorul: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void createNewUser() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Utilizator Nou");
        dialog.setHeaderText("Creează un utilizator nou");

        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nume utilizator");
        
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Parolă");
        
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("user", "admin");
        roleCombo.setValue("user");

        grid.add(new Label("Nume utilizator:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Parolă:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Rol:"), 0, 2);
        grid.add(roleCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String username = usernameField.getText();
                String password = passwordField.getText();
                String role = roleCombo.getValue();
                
                if (username == null || username.trim().isEmpty() || 
                    password == null || password.trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Eroare", "Numele și parola sunt obligatorii");
                    return null;
                }
                
                try {
                    userService.createUser(username, password, role);
                    showAlert(Alert.AlertType.INFORMATION, "Succes", "Utilizator creat cu succes");
                    loadUsers();
                } catch (SQLException e) {
                    logger.error("Error creating user", e);
                    showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut crea utilizatorul: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void deleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Atenție", "Selectați un utilizator pentru a-l șterge");
            return;
        }

        if ("admin".equalsIgnoreCase(selected.getUsername())) {
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu puteți șterge utilizatorul admin");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmare ștergere");
        confirm.setHeaderText("Ștergeți utilizatorul '" + selected.getUsername() + "'?");
        confirm.setContentText("Această acțiune nu poate fi anulată.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                userService.deleteUser(selected.getId());
                loadUsers();
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Utilizator șters cu succes");
            } catch (SQLException e) {
                logger.error("Error deleting user", e);
                showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut șterge utilizatorul: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showStatusMessage(String message) {
        if (statusLabel != null) {
            statusLabel.setText("✅ " + message);
            statusLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
            
            // Resetăm mesajul după 3 secunde
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("ℹ️ Bifați căsuțele pentru a acorda permisiuni. Utilizatorii 'admin' au toate permisiunile.");
                        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
}

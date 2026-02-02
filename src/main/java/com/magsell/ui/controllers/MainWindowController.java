package com.magsell.ui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.magsell.App;
import com.magsell.services.UserService;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.control.TextField;
import javafx.geometry.Insets;
import javafx.scene.control.TabPane;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.stage.Stage;
import com.magsell.services.ExportService;
import javafx.scene.control.DatePicker;
import java.time.LocalDate;

/**
 * Controller pentru fereastra principală a aplicației MagSell
 */
public class MainWindowController {
    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);
    private final ExportService exportService = new ExportService();

    @FXML
    private TabPane mainTabPane;

    @FXML
    public void initialize() {
        // Așteptăm ca fereastra să fie complet încărcată înainte de a ajusta vizibilitatea
        Platform.runLater(this::adjustUIForUserRole);
    }

    /**
     * Ajustează vizibilitatea elementelor UI în funcție de rolul utilizatorului
     */
    private void adjustUIForUserRole() {
        com.magsell.models.User currentUser = App.getCurrentUser();
        if (currentUser == null) return;

        boolean isAdmin = currentUser.isAdmin();
        boolean canManageProducts = currentUser.canManageProducts();
        boolean canManageCategories = currentUser.canManageCategories();
        boolean canManageSales = currentUser.canManageSales();
        boolean canViewReports = currentUser.canViewReports();

        // Ajustăm tab-urile - eliminăm cele care nu sunt permise
        try {
            javafx.collections.ObservableList<Tab> tabs = mainTabPane.getTabs();
            tabs.removeIf(tab -> {
                String tabText = tab.getText();
                // Dashboard și POS-Vânzări sunt mereu vizibile
                if ("Dashboard".equals(tabText) || "POS - Vânzări".equals(tabText)) {
                    return false;
                }
                // Produse - necesită permisiune
                if ("Produse".equals(tabText) && !canManageProducts) {
                    return true; // Eliminăm tab-ul Produse
                }
                // Categorii - necesită permisiune
                if ("Categorii".equals(tabText) && !canManageCategories) {
                    return true; // Eliminăm tab-ul Categorii
                }
                // Vânzări - necesită permisiune
                if ("Vânzări".equals(tabText) && !canManageSales) {
                    return true; // Eliminăm tab-ul Vânzări
                }
                return false;
            });
        } catch (Exception e) {
            logger.error("Error adjusting tab visibility", e);
        }
    }

    @FXML
    private void handleExit() {
        logger.info("Aplicație închisă de utilizator");
        System.exit(0);
    }

    @FXML
    private void handleManageUsers() {
        try {
            com.magsell.models.User cu = App.getCurrentUser();
            if (cu == null || !"admin".equalsIgnoreCase(cu.getRole())) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Access denied");
                a.setHeaderText("Acces interzis");
                a.setContentText("Doar utilizatorii cu rol 'admin' pot gestiona utilizatorii.");
                a.showAndWait();
                return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Creează utilizator nou");
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField username = new TextField();
            username.setPromptText("Username");
            PasswordField password = new PasswordField();
            password.setPromptText("Password");
            ChoiceBox<String> role = new ChoiceBox<>();
            role.getItems().addAll("user", "admin");
            role.setValue("user");

            grid.add(username, 0, 0);
            grid.add(password, 0, 1);
            grid.add(role, 0, 2);

            dialog.getDialogPane().setContent(grid);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    String u = username.getText();
                    String p = password.getText();
                    String r = role.getValue();
                    if (u == null || u.isBlank() || p == null || p.isBlank()) {
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setTitle("Eroare");
                        a.setHeaderText("Date invalide");
                        a.setContentText("Username și parola sunt obligatorii.");
                        a.showAndWait();
                        return;
                    }
                    try {
                        new UserService().createUser(u, p, r);
                        Alert ok = new Alert(Alert.AlertType.INFORMATION);
                        ok.setTitle("Utilizator creat");
                        ok.setHeaderText(null);
                        ok.setContentText("Utilizatorul a fost creat cu succes.");
                        ok.showAndWait();
                    } catch (Exception ex) {
                        logger.error("Eroare la crearea utilizatorului", ex);
                        Alert err = new Alert(Alert.AlertType.ERROR);
                        err.setTitle("Eroare");
                        err.setHeaderText("Nu s-a putut crea utilizatorul");
                        err.setContentText(ex.getMessage());
                        err.showAndWait();
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Eroare gestionează utilizatori", e);
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Eroare");
            a.setHeaderText("Eroare");
            a.setContentText(e.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    private void handleUserPermissions() {
        try {
            com.magsell.models.User cu = App.getCurrentUser();
            if (cu == null || !"admin".equalsIgnoreCase(cu.getRole())) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Access denied");
                a.setHeaderText("Acces interzis");
                a.setContentText("Doar utilizatorii cu rol 'admin' pot gestiona permisiunile.");
                a.showAndWait();
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/magsell/ui/fxml/UserManagement.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Gestionare Permisiuni Utilizatori");
            stage.setScene(new javafx.scene.Scene(root, 900, 600));
            stage.showAndWait();

        } catch (Exception e) {
            logger.error("Eroare la deschiderea managerului de permisiuni", e);
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Eroare");
            alert.setContentText("Eroare la deschiderea managerului de permisiuni: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleAddProduct() {
        com.magsell.models.User currentUser = App.getCurrentUser();
        if (currentUser == null || !currentUser.canManageProducts()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Acces interzis");
            alert.setHeaderText("Permisiuni insuficiente");
            alert.setContentText("Nu aveți permisiunea de a gestiona produse.");
            alert.showAndWait();
            return;
        }
        openProductEditor();
    }

    @FXML
    private void handleViewProducts() {
        com.magsell.models.User currentUser = App.getCurrentUser();
        if (currentUser == null || !currentUser.canManageProducts()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Acces interzis");
            alert.setHeaderText("Permisiuni insuficiente");
            alert.setContentText("Nu aveți permisiunea de a vizualiza produse.");
            alert.showAndWait();
            return;
        }
        openProductList();
    }

    @FXML
    private void handleManageCategories() {
        com.magsell.models.User currentUser = App.getCurrentUser();
        if (currentUser == null || !currentUser.canManageCategories()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Acces interzis");
            alert.setHeaderText("Permisiuni insuficiente");
            alert.setContentText("Nu aveți permisiunea de a gestiona categorii.");
            alert.showAndWait();
            return;
        }
        openCategoryManager();
    }

    @FXML
    private void handleNewSale() {
        com.magsell.models.User currentUser = App.getCurrentUser();
        if (currentUser == null || !currentUser.canManageSales()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Acces interzis");
            alert.setHeaderText("Permisiuni insuficiente");
            alert.setContentText("Nu aveți permisiunea de a gestiona vânzări.");
            alert.showAndWait();
            return;
        }
        showNotImplemented("Nouă vânzare");
    }

    @FXML
    private void handleViewSales() {
        com.magsell.models.User currentUser = App.getCurrentUser();
        if (currentUser == null || !currentUser.canManageSales()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Acces interzis");
            alert.setHeaderText("Permisiuni insuficiente");
            alert.setContentText("Nu aveți permisiunea de a vizualiza vânzări.");
            alert.showAndWait();
            return;
        }
        // Selectează tab-ul de vânzări
        if (mainTabPane != null && mainTabPane.getTabs().size() > 3) {
            mainTabPane.getSelectionModel().select(3);
        }
    }

    @FXML
    private void handleExportPDF() {
        try {
            Dialog<LocalDate[]> dialog = createDateRangeDialog("Export PDF");
            dialog.showAndWait().ifPresent(dates -> {
                if (dates != null && dates.length == 2) {
                    Stage stage = (Stage) mainTabPane.getScene().getWindow();
                    exportService.exportToPDF(dates[0], dates[1], stage);
                    showAlert("Succes", "Raport PDF exportat cu succes!");
                }
            });
        } catch (Exception e) {
            logger.error("Eroare la exportul PDF", e);
            showAlert("Eroare", "Eroare la exportul PDF: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportExcel() {
        try {
            Dialog<LocalDate[]> dialog = createDateRangeDialog("Export Excel");
            dialog.showAndWait().ifPresent(dates -> {
                if (dates != null && dates.length == 2) {
                    Stage stage = (Stage) mainTabPane.getScene().getWindow();
                    exportService.exportToExcel(dates[0], dates[1], stage);
                    showAlert("Succes", "Raport Excel exportat cu succes!");
                }
            });
        } catch (Exception e) {
            logger.error("Eroare la exportul Excel", e);
            showAlert("Eroare", "Eroare la exportul Excel: " + e.getMessage());
        }
    }

    private Dialog<LocalDate[]> createDateRangeDialog(String title) {
        Dialog<LocalDate[]> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Selectează perioada pentru raport");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker startDate = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker endDate = new DatePicker(LocalDate.now());

        grid.add(new javafx.scene.control.Label("Data început:"), 0, 0);
        grid.add(startDate, 1, 0);
        grid.add(new javafx.scene.control.Label("Data sfârșit:"), 0, 1);
        grid.add(endDate, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new LocalDate[]{startDate.getValue(), endDate.getValue()};
            }
            return null;
        });

        return dialog;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Despre MagSell");
        alert.setHeaderText("MagSell v1.0.0");
        alert.setContentText("Aplicație pentru gestionarea patiseriei\nUpgrade la Java 21 LTS");
        alert.showAndWait();
        logger.info("Afișată fereastra 'Despre'");
    }

    private void showNotImplemented(String feature) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("În dezvoltare");
        alert.setHeaderText(feature);
        alert.setContentText("Această funcționalitate este în dezvoltare...");
        alert.showAndWait();
        logger.info("Utilizator a accesat: " + feature);
    }

    /**
     * Deschide dialogul de adăugare de produse
     */
    private void openProductEditor() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/magsell/ui/fxml/ProductEditor.fxml"));
            loader.setController(new ProductController());
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Adaugă produs");
            stage.setScene(new javafx.scene.Scene(root, 500, 400));
            stage.showAndWait();
        } catch (Exception e) {
            logger.error("Eroare la deschiderea editorului de produs", e);
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Eroare");
            alert.setContentText("Eroare la deschiderea editorului: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Deschide fereastra cu lista de produse
     */
    private void openProductList() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/magsell/ui/fxml/ProductList.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Gestiona Produse");
            stage.setScene(new javafx.scene.Scene(root, 1000, 600));
            stage.showAndWait();
        } catch (Exception e) {
            logger.error("Eroare la deschiderea listei de produse", e);
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Eroare");
            alert.setContentText("Eroare la deschiderea listei: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Deschide fereastra de gestionare a categoriilor
     */
    private void openCategoryManager() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/magsell/ui/fxml/CategoryManager.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Gestionare Categorii");
            stage.setScene(new javafx.scene.Scene(root, 800, 500));
            stage.showAndWait();
            
            // După ce se închide fereastra de categorii, reîncărcăm categoriile în ProductController
            refreshProductCategories();
            
        } catch (Exception e) {
            logger.error("Eroare la deschiderea managerului de categorii", e);
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Eroare");
            alert.setContentText("Eroare la deschiderea managerului de categorii: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Reîncarcă categoriile în toate instanțele de ProductController deschise
     */
    private void refreshProductCategories() {
        // Găsim toate tab-urile deschise și reîncărcăm categoriile
        if (mainTabPane != null) {
            mainTabPane.getTabs().forEach(tab -> {
                try {
                    // Căutăm ProductController în tab-uri
                    javafx.scene.Node content = tab.getContent();
                    if (content != null) {
                        // Căutăm recursiv ProductController
                        findAndRefreshProductControllers(content);
                    }
                } catch (Exception e) {
                    logger.debug("Nu s-a putut reîncărca categoriile pentru tab: " + tab.getText(), e);
                }
            });
        }
    }

    /**
     * Caută recursiv ProductController în nodurile UI
     */
    private void findAndRefreshProductControllers(javafx.scene.Node node) {
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            
            // Verificăm dacă acest nod conține ProductController
            Object userData = parent.getUserData();
            if (userData instanceof ProductController) {
                ((ProductController) userData).refreshCategories();
            }
            
            // Căutăm în copiii nodului
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                findAndRefreshProductControllers(child);
            }
        }
    }
}

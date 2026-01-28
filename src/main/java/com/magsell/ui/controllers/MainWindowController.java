package com.magsell.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

/**
 * Controller pentru fereastra principală a aplicației MagSell
 */
public class MainWindowController {
    private static final Logger logger = LoggerFactory.getLogger(MainWindowController.class);

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
    private void handleAddProduct() {
        showNotImplemented("Adăugare produs");
    }

    @FXML
    private void handleViewProducts() {
        showNotImplemented("Vezi produsele");
    }

    @FXML
    private void handleNewSale() {
        showNotImplemented("Nouă vânzare");
    }

    @FXML
    private void handleViewSales() {
        showNotImplemented("Vezi vânzări");
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
}

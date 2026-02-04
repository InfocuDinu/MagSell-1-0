package com.magsell.ui.controllers;

import com.magsell.App;
import com.magsell.models.User;
import com.magsell.services.UserService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller pentru fereastra de login.
 */
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    
    public TextField usernameField;
    public PasswordField passwordField;
    
    private final UserService userService = new UserService();
    
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Eroare", "Completați toate câmpurile!");
            return;
        }
        
        try {
            if (userService.authenticate(username, password)) {
                User user = userService.getUserByUsername(username);
                App.setCurrentUser(user);
                
                logger.info("Utilizatorul {} s-a autentificat cu succes", username);
                
                // Închide fereastra de login și deschide fereastra principală
                Stage loginStage = (Stage) usernameField.getScene().getWindow();
                loginStage.close();
                
                openMainWindow();
            } else {
                showAlert(Alert.AlertType.ERROR, "Eroare", "Username sau parolă incorectă!");
                logger.warn("Încercare de autentificare eșuată pentru utilizatorul: {}", username);
            }
        } catch (SQLException e) {
            logger.error("Eroare la autentificare", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", "Eroare la conectarea cu baza de date!");
        }
    }
    
    private void openMainWindow() {
        try {
            // Închide fereastra de login
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();
            
            // Deschide noul layout principal modern
            App.openMainWindow();
            
        } catch (Exception e) {
            logger.error("Eroare la deschiderea ferestrei principale", e);
            showAlert(Alert.AlertType.ERROR, "Eroare", "Nu s-a putut încărca fereastra principală!");
        }
    }
    
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

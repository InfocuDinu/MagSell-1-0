package com.magsell;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.magsell.database.DatabaseService;
import com.magsell.models.User;
import com.magsell.services.UserService;

import java.io.IOException;

/**
 * Clasa principală a aplicației MagSell.
 * Inițializează baza de date și interfața grafică.
 */
public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final String APP_TITLE = "MagSell - Gestionare Patiserie";
    private static User currentUser;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Inițializare bază de date
        try {
            DatabaseService.getInstance().init();
            logger.info("Baza de date inițializată cu succes");
            
            // Asigură existența utilizatorului admin default
            UserService userService = new UserService();
            userService.ensureDefaultAdmin();
            logger.info("Utilizator admin default verificat");
        } catch (Exception e) {
            logger.error("Eroare la inițializarea bazei de date", e);
            showErrorAndExit("Eroare la conectarea cu baza de date");
        }

        // Încărcare fereastra de login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/magsell/ui/fxml/LoginWindow.fxml"));
            Scene scene = new Scene(loader.load(), 320, 280);

            // Încărcare stiluri CSS
            String css = getClass().getResource("/com/magsell/ui/css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.setTitle(APP_TITLE + " - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.setOnCloseRequest(e -> shutdown());
            primaryStage.show();

            logger.info("Fereastra de login afișată cu succes");
        } catch (IOException e) {
            logger.error("Eroare la încărcarea ferestrei de login", e);
            showErrorAndExit("Eroare la încărcarea interfeței");
        }
    }

    private static void shutdown() {
        try {
            DatabaseService.getInstance().close();
            logger.info("Aplicație închisă normal");
        } catch (Exception e) {
            logger.error("Eroare la închiderea bazei de date", e);
        }
    }
    
    private static void showErrorAndExit(String message) {
        System.err.println(message);
        System.exit(1);
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    /**
     * Deschide fereastra principală cu noul layout modern
     */
    public static void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/magsell/ui/fxml/MainLayout.fxml"));
            Scene scene = new Scene(loader.load(), 1400, 900);

            // Încărcare stiluri CSS
            String css = App.class.getResource("/com/magsell/ui/styles/main-styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            Stage stage = new Stage();
            stage.setTitle(APP_TITLE + " - ERP");
            stage.setScene(scene);
            stage.setMinWidth(1200);
            stage.setMinHeight(700);
            stage.setOnCloseRequest(e -> shutdown());
            stage.show();

            logger.info("Fereastra principală afișată cu succes");
        } catch (IOException e) {
            logger.error("Eroare la încărcarea ferestrei principale", e);
            showErrorAndExit("Eroare la încărcarea interfeței principale");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.magsell;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.magsell.database.DatabaseService;

import java.io.IOException;

/**
 * Clasa principală a aplicației MagSell.
 * Inițializează baza de date și interfața grafică.
 */
public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final String APP_TITLE = "MagSell - Gestionare Patiserie";

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Inițializare bază de date
        try {
            DatabaseService.getInstance().init();
            logger.info("Baza de date inițializată cu succes");
        } catch (Exception e) {
            logger.error("Eroare la inițializarea bazei de date", e);
            showErrorAndExit("Eroare la conectarea cu baza de date");
        }

        // Încărcare FXML
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/magsell/ui/fxml/MainWindow.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            // Încărcare stiluri CSS
            String css = getClass().getResource("/com/magsell/ui/css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(e -> shutdown());
            primaryStage.show();

            logger.info("Aplicație pornită cu succes");
        } catch (IOException e) {
            logger.error("Eroare la încărcarea interfeței", e);
            showErrorAndExit("Eroare la încărcarea interfeței");
        }
    }

    private void showErrorAndExit(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private void shutdown() {
        try {
            DatabaseService.getInstance().close();
            logger.info("Aplicație închisă normal");
        } catch (Exception e) {
            logger.error("Eroare la închiderea bazei de date", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.magsell.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Serviciu singleton pentru gestionarea conexiunilor la baza de date.
 * Implementează Singleton pattern pentru a asigura o singură instanță.
 */
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static DatabaseService instance;
    private String dbUrl;
    private static final String DB_DIR = ".magsell";
    private static final String DB_NAME = "magsell.db";

    private DatabaseService() {
    }

    /**
     * Obține instanța Singleton a DatabaseService.
     */
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /**
     * Inițializează baza de date și creează schema dacă nu există.
     */
    public void init() throws SQLException {
        try {
            // Creează directorul .magsell dacă nu există
            Path dbDir = Paths.get(System.getProperty("user.home"), DB_DIR);
            Files.createDirectories(dbDir);

            // Configurează URL-ul pentru SQLite (deschidem conexiuni per operație)
            String dbPath = dbDir.resolve(DB_NAME).toString();
            this.dbUrl = "jdbc:sqlite:" + dbPath;
            logger.info("Conectare la baza de date: " + dbUrl);

            // Creează tabelele dacă nu există
            createTables();
        } catch (Exception e) {
            logger.error("Eroare la inițializarea bazei de date", e);
            throw new SQLException("Nu s-a putut inițializa baza de date", e);
        }
    }

    /**
     * Creează schema tabelelor.
     */
    private void createTables() throws SQLException {
        String[] tables = {
            createUsersTable(),
            createProductsTable(),
            createSalesTable(),
            createCustomersTable()
        };

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                stmt.execute(table);
            }
            logger.info("Tabelele s-au creat cu succes");
            
            // Adaugă coloana image_path dacă nu există (migrare pentru baze de date existente)
            addImagePathColumnIfNotExists(stmt);
            
            // Creează utilizatorul admin implicit
            ensureDefaultAdmin();
        }
    }

    /**
     * Creează utilizatorul admin implicit dacă nu există.
     */
    private void ensureDefaultAdmin() {
        try {
            com.magsell.services.UserService userService = new com.magsell.services.UserService();
            userService.ensureDefaultAdmin();
        } catch (Exception e) {
            logger.warn("Nu s-a putut crea utilizatorul admin implicit: " + e.getMessage());
        }
    }

    private String createUsersTable() {
        return """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'user',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
    }

    private String createProductsTable() {
        return """
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                price DECIMAL(10,2) NOT NULL,
                quantity INTEGER NOT NULL,
                category TEXT,
                image_path TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
    }

    /**
     * Adaugă coloana image_path la tabelul products dacă nu există (pentru migrare).
     */
    private void addImagePathColumnIfNotExists(Statement stmt) {
        try {
            // Verifică dacă coloana există
            stmt.execute("PRAGMA table_info(products)");
            // Dacă nu există, o adaugă
            stmt.execute("ALTER TABLE products ADD COLUMN image_path TEXT");
            logger.info("Coloana image_path adăugată la tabelul products");
        } catch (SQLException e) {
            // Coloana există deja sau altă eroare - ignorăm
            logger.debug("Coloana image_path există deja sau eroare la adăugare: " + e.getMessage());
        }
    }

    private String createSalesTable() {
        return """
            CREATE TABLE IF NOT EXISTS sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL,
                product_name TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                unit_price DECIMAL(10,2) NOT NULL,
                total_price DECIMAL(10,2) NOT NULL,
                sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                notes TEXT,
                FOREIGN KEY (product_id) REFERENCES products(id)
            )
            """;
    }

    private String createCustomersTable() {
        return """
            CREATE TABLE IF NOT EXISTS customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT,
                phone TEXT,
                address TEXT,
                first_purchase TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_purchase TIMESTAMP,
                notes TEXT
            )
            """;
    }

    /**
     * Obține conexiunea la baza de date.
     */
    public Connection getConnection() throws SQLException {
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new SQLException("Baza de date nu este inițializată");
        }
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Închide conexiunea la baza de date.
     */
    public void close() throws SQLException {
        // Conexiunile sunt deschise per operație (try-with-resources) în servicii.
        // Păstrăm metoda pentru compatibilitate (App.shutdown()).
        logger.info("DatabaseService closed (no persistent connection)");
    }
}

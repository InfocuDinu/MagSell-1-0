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
    private Connection connection;
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

            // Deschide conexiunea la SQLite
            String dbPath = dbDir.resolve(DB_NAME).toString();
            String url = "jdbc:sqlite:" + dbPath;
            this.connection = DriverManager.getConnection(url);
            logger.info("Conectare la baza de date: " + url);

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
            createProductsTable(),
            createSalesTable(),
            createCustomersTable()
        };

        try (Statement stmt = connection.createStatement()) {
            for (String table : tables) {
                stmt.execute(table);
            }
            logger.info("Tabelele s-au creat cu succes");
        }
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
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
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
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Conexiunea la baza de date este închisă");
        }
        return connection;
    }

    /**
     * Închide conexiunea la baza de date.
     */
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            logger.info("Conexiune la baza de date închisă");
        }
    }
}

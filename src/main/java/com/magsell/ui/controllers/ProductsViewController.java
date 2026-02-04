package com.magsell.ui.controllers;

import com.magsell.services.ProductService;
import com.magsell.models.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller pentru modul Produse în noul design
 */
public class ProductsViewController {
    private static final Logger logger = LoggerFactory.getLogger(ProductsViewController.class);
    
    private final ProductService productService;
    private final ObservableList<Product> productList;
    
    @FXML
    private TableView<Product> productsTable;
    
    @FXML
    private TableColumn<Product, Integer> idColumn;
    
    @FXML
    private TableColumn<Product, String> nameColumn;
    
    @FXML
    private TableColumn<Product, String> categoryColumn;
    
    @FXML
    private TableColumn<Product, Double> priceColumn;
    
    @FXML
    private TableColumn<Product, Integer> stockColumn;
    
    @FXML
    private TableColumn<Product, String> typeColumn;
    
    @FXML
    private VBox contentArea;
    
    public ProductsViewController() {
        this.productService = new ProductService();
        this.productList = FXCollections.observableArrayList();
    }
    
    @FXML
    public void initialize() {
        logger.info("Initializing ProductsViewController");
        
        setupTable();
        loadProducts();
        
        logger.info("ProductsViewController initialized successfully");
    }
    
    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        
        productsTable.setItems(productList);
    }
    
    private void loadProducts() {
        try {
            productList.clear();
            productList.addAll(productService.getAllProducts());
            logger.info("Loaded {} products", productList.size());
        } catch (Exception e) {
            logger.error("Error loading products", e);
        }
    }
}

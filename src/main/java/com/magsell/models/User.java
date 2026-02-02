package com.magsell.models;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model pentru utilizatorul aplicației cu permisiuni granulare și JavaFX Properties.
 */
public class User {
    private int id;
    private final StringProperty username = new SimpleStringProperty();
    private String passwordHash;
    private String salt;
    private final StringProperty role = new SimpleStringProperty(); // "admin" sau "user"
    
    // JavaFX Properties pentru permisiuni granulare
    private final BooleanProperty canManageProducts = new SimpleBooleanProperty(false);
    private final BooleanProperty canManageCategories = new SimpleBooleanProperty(false);
    private final BooleanProperty canManageUsers = new SimpleBooleanProperty(false);
    private final BooleanProperty canViewReports = new SimpleBooleanProperty(false);
    private final BooleanProperty canManageSales = new SimpleBooleanProperty(false);

    public User() {
    }

    public User(String username, String passwordHash, String salt, String role) {
        setUsername(username);
        this.passwordHash = passwordHash;
        this.salt = salt;
        setRole(role);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username.get();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getRole() {
        return role.get();
    }

    public StringProperty roleProperty() {
        return role;
    }

    public void setRole(String role) {
        this.role.set(role);
        // Dacă rolul se schimbă în admin, dă toate permisiunile și blochează modificarea
        if ("admin".equalsIgnoreCase(role)) {
            setCanManageProducts(true);
            setCanManageCategories(true);
            setCanManageUsers(true);
            setCanViewReports(true);
            setCanManageSales(true);
        } else {
            // Pentru useri, resetăm permisiunile la false la schimbarea rolului
            setCanManageProducts(false);
            setCanManageCategories(false);
            setCanManageUsers(false);
            setCanViewReports(false);
            setCanManageSales(false);
        }
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getRole());
    }

    // Permisiuni granulare cu JavaFX Properties
    public boolean canManageProducts() {
        return isAdmin() || canManageProducts.get();
    }

    public BooleanProperty canManageProductsProperty() {
        return canManageProducts;
    }

    public void setCanManageProducts(boolean canManageProducts) {
        this.canManageProducts.set(canManageProducts);
    }

    public boolean canManageCategories() {
        return isAdmin() || canManageCategories.get();
    }

    public BooleanProperty canManageCategoriesProperty() {
        return canManageCategories;
    }

    public void setCanManageCategories(boolean canManageCategories) {
        this.canManageCategories.set(canManageCategories);
    }

    public boolean canManageUsers() {
        return isAdmin() || canManageUsers.get();
    }

    public BooleanProperty canManageUsersProperty() {
        return canManageUsers;
    }

    public void setCanManageUsers(boolean canManageUsers) {
        this.canManageUsers.set(canManageUsers);
    }

    public boolean canViewReports() {
        return isAdmin() || canViewReports.get();
    }

    public BooleanProperty canViewReportsProperty() {
        return canViewReports;
    }

    public void setCanViewReports(boolean canViewReports) {
        this.canViewReports.set(canViewReports);
    }

    public boolean canManageSales() {
        return isAdmin() || canManageSales.get();
    }

    public BooleanProperty canManageSalesProperty() {
        return canManageSales;
    }

    public void setCanManageSales(boolean canManageSales) {
        this.canManageSales.set(canManageSales);
    }
}

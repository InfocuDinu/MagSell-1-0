package com.magsell.models;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model pentru rețetele de producție
 */
public class Recipe {
    private int id;
    private int productId; // Produs finit
    private String productName; // Denumire produs finit (pentru afișare)
    private List<RecipeIngredient> ingredients;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private boolean active;
    
    public Recipe() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getProductId() {
        return productId;
    }
    
    public void setProductId(int productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }
    
    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public String toString() {
        return productName != null ? productName : "Rețete #" + id;
    }
}

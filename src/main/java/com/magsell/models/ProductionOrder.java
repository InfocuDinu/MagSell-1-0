package com.magsell.models;

import java.time.LocalDateTime;

/**
 * Model pentru ordinele de producție
 */
public class ProductionOrder {
    private int id;
    private int recipeId;
    private String recipeName; // Denumire rețetă (pentru afișare)
    private double quantityToProduce; // Cantitatea de produs finit de produs
    private String status; // 'pending', 'in_progress', 'completed', 'cancelled'
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String createdBy;
    private String notes;
    
    public ProductionOrder() {
        this.createdAt = LocalDateTime.now();
        this.status = "pending";
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getRecipeId() {
        return recipeId;
    }
    
    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }
    
    public String getRecipeName() {
        return recipeName;
    }
    
    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }
    
    public double getQuantityToProduce() {
        return quantityToProduce;
    }
    
    public void setQuantityToProduce(double quantityToProduce) {
        this.quantityToProduce = quantityToProduce;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Marchează comanda ca fiind în procesare
     */
    public void start() {
        this.status = "in_progress";
        this.startedAt = LocalDateTime.now();
    }
    
    /**
     * Marchează comanda ca fiind finalizată
     */
    public void complete() {
        this.status = "completed";
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Marchează comanda ca fiind anulată
     */
    public void cancel() {
        this.status = "cancelled";
    }
    
    @Override
    public String toString() {
        return recipeName != null ? recipeName + " (" + quantityToProduce + ")" : "Comandă #" + id;
    }
}

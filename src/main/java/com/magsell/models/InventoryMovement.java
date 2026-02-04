package com.magsell.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model pentru mișcările de inventar
 */
public class InventoryMovement {
    private Integer id;
    private Integer productId;
    private Product product;
    private Integer warehouseId;
    private String movementType; // IN, OUT, TRANSFER, ADJUSTMENT
    private String documentType; // NIR, FAC, AVI, ADJ
    private Integer documentId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String batchNumber;
    private String notes;
    private Integer createdBy;
    private User createdByUser;
    private LocalDateTime createdAt;
    
    public InventoryMovement() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public InventoryMovement(Integer productId, BigDecimal quantity, String movementType) {
        this();
        this.productId = productId;
        this.quantity = quantity;
        this.movementType = movementType;
    }
    
    // Business logic methods
    public boolean isInbound() {
        return "IN".equals(movementType);
    }
    
    public boolean isOutbound() {
        return "OUT".equals(movementType);
    }
    
    public boolean isTransfer() {
        return "TRANSFER".equals(movementType);
    }
    
    public boolean isAdjustment() {
        return "ADJUSTMENT".equals(movementType);
    }
    
    public String getMovementDescription() {
        switch (movementType) {
            case "IN": return "Intrare";
            case "OUT": return "Ieșire";
            case "TRANSFER": return "Transfer";
            case "ADJUSTMENT": return "Ajustare";
            default: return movementType;
        }
    }
    
    public String getDocumentDescription() {
        if (documentType == null) return "";
        
        switch (documentType) {
            case "NIR": return "Nota de Intrare-Recepție";
            case "FAC": return "Factură";
            case "AVI": return "Aviz de însoțire";
            case "ADJ": return "Ajustare";
            default: return documentType;
        }
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getProductId() {
        return productId;
    }
    
    public void setProductId(Integer productId) {
        this.productId = productId;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getId();
        }
    }
    
    public Integer getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }
    
    public String getMovementType() {
        return movementType;
    }
    
    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public Integer getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(Integer documentId) {
        this.documentId = documentId;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public String getBatchNumber() {
        return batchNumber;
    }
    
    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Integer getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }
    
    public User getCreatedByUser() {
        return createdByUser;
    }
    
    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
        if (createdByUser != null) {
            this.createdBy = createdByUser.getId();
        }
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "InventoryMovement{" +
                "id=" + id +
                ", productId=" + productId +
                ", movementType='" + movementType + '\'' +
                ", quantity=" + quantity +
                ", documentType='" + documentType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

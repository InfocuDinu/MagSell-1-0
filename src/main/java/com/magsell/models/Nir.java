package com.magsell.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Model pentru Nota de Intrare-Recepție (NIR)
 */
public class Nir {
    private Integer id;
    private String number;
    private LocalDate date;
    private Integer supplierId;
    private Partner supplier;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String warehouse;
    private BigDecimal totalAmount;
    private String notes;
    private Integer createdBy;
    private User createdByUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Relations
    private List<NirItem> items;
    
    public Nir() {
        this.date = LocalDate.now();
        this.invoiceDate = LocalDate.now();
        this.totalAmount = BigDecimal.ZERO;
        this.warehouse = "Gestiune Principală";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business logic methods
    public void recalculateTotal() {
        this.totalAmount = BigDecimal.ZERO;
        if (items != null) {
            for (NirItem item : items) {
                this.totalAmount = this.totalAmount.add(item.getTotal());
            }
        }
    }
    
    public String getFullNumber() {
        return number != null ? number : "NIR-" + date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getNumber() {
        return number;
    }
    
    public void setNumber(String number) {
        this.number = number;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public Integer getSupplierId() {
        return supplierId;
    }
    
    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }
    
    public Partner getSupplier() {
        return supplier;
    }
    
    public void setSupplier(Partner supplier) {
        this.supplier = supplier;
        if (supplier != null) {
            this.supplierId = supplier.getId();
        }
    }
    
    public String getInvoiceNumber() {
        return invoiceNumber;
    }
    
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    
    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }
    
    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }
    
    public String getWarehouse() {
        return warehouse;
    }
    
    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<NirItem> getItems() {
        return items;
    }
    
    public void setItems(List<NirItem> items) {
        this.items = items;
        recalculateTotal();
    }
    
    @Override
    public String toString() {
        return "Nir{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", date=" + date +
                ", supplierId=" + supplierId +
                ", totalAmount=" + totalAmount +
                '}';
    }
}

package com.magsell.models;

import java.time.LocalDate;

/**
 * Model pentru elementele dintr-o notă de recepție
 */
public class ReceptionNoteItem {
    private int id;
    private int receptionNoteId;
    private String productName;
    private String productCode;
    private String description;
    private double quantity;
    private String unitOfMeasure;
    private double unitPrice;
    private double totalPrice;
    private double vatRate; // Procent TVA (ex: 19.0)
    private double vatAmount;
    private String category;
    private double receivedQuantity; // Cantitatea efectiv recepționată
    private String batchNumber; // Număr lot
    private LocalDate expiryDate; // Data expirării
    private String storageLocation; // Locație depozitare

    public ReceptionNoteItem() {
        this.unitOfMeasure = "buc";
        this.vatRate = 19.0;
        this.receivedQuantity = 0.0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReceptionNoteId() {
        return receptionNoteId;
    }

    public void setReceptionNoteId(int receptionNoteId) {
        this.receptionNoteId = receptionNoteId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
        calculateVatAmount();
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        calculateVatAmount();
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
        calculateVatAmount();
    }

    public double getVatRate() {
        return vatRate;
    }

    public void setVatRate(double vatRate) {
        this.vatRate = vatRate;
        calculateVatAmount();
    }

    public double getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(double vatAmount) {
        this.vatAmount = vatAmount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(double receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    /**
     * Calculează automat valoarea TVA
     */
    private void calculateVatAmount() {
        double baseAmount = totalPrice;
        this.vatAmount = baseAmount * (vatRate / 100.0);
    }

    /**
     * Returnează prețul fără TVA
     */
    public double getPriceWithoutVat() {
        return totalPrice - vatAmount;
    }

    /**
     * Verifică dacă cantitatea recepționată corespunde cu cea facturată
     */
    public boolean isQuantityMatch() {
        return Math.abs(receivedQuantity - quantity) < 0.001;
    }

    /**
     * Returnează diferența de cantitate
     */
    public double getQuantityDifference() {
        return receivedQuantity - quantity;
    }

    @Override
    public String toString() {
        return "ReceptionNoteItem{" +
                "productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", receivedQuantity=" + receivedQuantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                ", vatRate=" + vatRate +
                '}';
    }
}

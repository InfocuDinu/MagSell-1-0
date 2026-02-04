package com.magsell.models;

/**
 * Model pentru elementele dintr-o factură
 */
public class InvoiceItem {
    private int id;
    private int invoiceId;
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

    public InvoiceItem() {
        this.unitOfMeasure = "buc";
        this.vatRate = 19.0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
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

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                ", vatRate=" + vatRate +
                '}';
    }
}

package com.magsell.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model pentru Item pe Factură Fiscală
 */
public class InvoiceItem {
    private Integer id;
    private Integer invoiceId;
    private Integer productId;
    private Product product;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal vatRate;
    private BigDecimal totalAmount;
    private BigDecimal totalVat;
    private BigDecimal totalWithVat;
    private Integer warehouseId;
    private String batchNumber;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;
    
    // Legacy fields for backward compatibility
    private String productName;
    private String productCode;
    private String description;
    private String unitOfMeasure;
    private double totalPrice;
    private double vatAmount;
    private String category;
    
    public InvoiceItem() {
        this.quantity = BigDecimal.ZERO;
        this.unit = "buc";
        this.unitPrice = BigDecimal.ZERO;
        this.discountPercent = BigDecimal.ZERO;
        this.vatRate = new BigDecimal("19.0");
        this.totalAmount = BigDecimal.ZERO;
        this.totalVat = BigDecimal.ZERO;
        this.totalWithVat = BigDecimal.ZERO;
        this.warehouseId = 1; // Default warehouse
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public InvoiceItem(Integer productId, BigDecimal quantity, BigDecimal unitPrice) {
        this();
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateTotals();
    }
    
    // Business logic methods
    public void calculateTotals() {
        // Calculate amount after discount
        BigDecimal discountAmount = unitPrice.multiply(discountPercent.divide(new BigDecimal("100")));
        BigDecimal discountedPrice = unitPrice.subtract(discountAmount);
        this.totalAmount = discountedPrice.multiply(quantity);
        
        // Calculate VAT
        this.totalVat = totalAmount.multiply(vatRate.divide(new BigDecimal("100")));
        
        // Calculate total with VAT
        this.totalWithVat = totalAmount.add(totalVat);
        
        // Update legacy fields
        this.totalPrice = totalWithVat.doubleValue();
        this.vatAmount = totalVat.doubleValue();
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getInvoiceId() {
        return invoiceId;
    }
    
    public void setInvoiceId(Integer invoiceId) {
        this.invoiceId = invoiceId;
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
            this.unit = product.getUnit() != null ? product.getUnit() : "buc";
            this.vatRate = new BigDecimal(product.getVatRate());
            this.productName = product.getName();
            this.productCode = product.getCode();
        }
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        calculateTotals();
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
        this.unitOfMeasure = unit; // Update legacy field
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateTotals();
    }
    
    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }
    
    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
        calculateTotals();
    }
    
    public BigDecimal getVatRate() {
        return vatRate;
    }
    
    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
        calculateTotals();
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public BigDecimal getTotalVat() {
        return totalVat;
    }
    
    public void setTotalVat(BigDecimal totalVat) {
        this.totalVat = totalVat;
    }
    
    public BigDecimal getTotalWithVat() {
        return totalWithVat;
    }
    
    public void setTotalWithVat(BigDecimal totalWithVat) {
        this.totalWithVat = totalWithVat;
    }
    
    public Integer getWarehouseId() {
        return warehouseId;
    }
    
    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Legacy getters/setters for backward compatibility
    public String getProductName() {
        return productName != null ? productName : (product != null ? product.getName() : "");
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductCode() {
        return productCode != null ? productCode : (product != null ? product.getCode() : "");
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
    
    public String getUnitOfMeasure() {
        return unitOfMeasure != null ? unitOfMeasure : unit;
    }
    
    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
        this.unit = unitOfMeasure;
    }
    
    public double getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
        this.totalWithVat = new BigDecimal(totalPrice);
    }
    
    public double getVatAmount() {
        return vatAmount;
    }
    
    public void setVatAmount(double vatAmount) {
        this.vatAmount = vatAmount;
        this.totalVat = new BigDecimal(vatAmount);
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    @Override
    public String toString() {
        return "InvoiceItem{" +
                "id=" + id +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalWithVat=" + totalWithVat +
                '}';
    }
}

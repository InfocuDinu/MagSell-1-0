package com.magsell.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Model pentru Factură Fiscală
 * Respectă standardul SmartBill și legislația fiscală română
 */
public class Invoice {
    private Integer id;
    private String series;
    private Integer number;
    private Integer partnerId;
    private Partner partner;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Integer paymentMethodId;
    private String paymentMethodName;
    private BigDecimal totalAmount;
    private BigDecimal totalVat;
    private BigDecimal totalWithVat;
    private String status; // draft, issued, paid, cancelled
    private String notes;
    private Boolean isEFactura;
    private String eFacturaStatus; // pending, submitted, accepted, rejected
    private String eFacturaXml;
    private Integer createdBy;
    private User createdByUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Relations
    private List<InvoiceItem> items = new ArrayList<>();
    
    public Invoice() {
        this.issueDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30);
        this.totalAmount = BigDecimal.ZERO;
        this.totalVat = BigDecimal.ZERO;
        this.totalWithVat = BigDecimal.ZERO;
        this.status = "draft";
        this.isEFactura = false;
        this.eFacturaStatus = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Invoice(String series, Integer number, Integer partnerId) {
        this();
        this.series = series;
        this.number = number;
        this.partnerId = partnerId;
    }
    
    // Business logic methods
    public void addItem(InvoiceItem item) {
        item.setInvoiceId(this.id);
        items.add(item);
        recalculateTotals();
    }
    
    public void removeItem(InvoiceItem item) {
        items.remove(item);
        recalculateTotals();
    }
    
    public void recalculateTotals() {
        this.totalAmount = BigDecimal.ZERO;
        this.totalVat = BigDecimal.ZERO;
        this.totalWithVat = BigDecimal.ZERO;
        
        for (InvoiceItem item : items) {
            this.totalAmount = this.totalAmount.add(item.getTotalAmount());
            this.totalVat = this.totalVat.add(item.getTotalVat());
            this.totalWithVat = this.totalWithVat.add(item.getTotalWithVat());
        }
    }
    
    public boolean isDraft() {
        return "draft".equals(status);
    }
    
    public boolean isIssued() {
        return "issued".equals(status);
    }
    
    public boolean isPaid() {
        return "paid".equals(status);
    }
    
    public boolean isCancelled() {
        return "cancelled".equals(status);
    }
    
    public String getFullNumber() {
        return series + " " + String.format("%06d", number);
    }
    
    // Legacy methods for backward compatibility
    public String getFullInvoiceNumber() {
        return getFullNumber();
    }
    
    public String getInvoiceNumber() {
        return getFullNumber();
    }
    
    public String getSupplierName() {
        return partner != null ? partner.getName() : "";
    }
    
    public String getSupplierCif() {
        return partner != null ? partner.getCui() : "";
    }
    
    public String getSupplierAddress() {
        return partner != null ? partner.getAddress() : "";
    }
    
    public double getVatAmount() {
        return totalVat != null ? totalVat.doubleValue() : 0.0;
    }
    
    public String getCurrency() {
        return "RON";
    }
    
    public String getXmlContent() {
        return eFacturaXml;
    }
    
    public String getPdfPath() {
        return null; // Can be implemented later
    }
    
    // Legacy setters for backward compatibility
    public void setInvoiceNumber(String invoiceNumber) {
        // Parse from format "SERIE 000001"
        if (invoiceNumber != null && invoiceNumber.contains(" ")) {
            String[] parts = invoiceNumber.split(" ", 2);
            if (parts.length == 2) {
                this.series = parts[0];
                try {
                    this.number = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    // Keep existing number
                }
            }
        }
    }
    
    public void setSupplierName(String supplierName) {
        // This would require finding/creating partner - simplified for now
    }
    
    public void setSupplierCif(String supplierCif) {
        // This would require finding/creating partner - simplified for now
    }
    
    public void setSupplierAddress(String supplierAddress) {
        // This would require finding/creating partner - simplified for now
    }
    
    public void setVatAmount(double vatAmount) {
        this.totalVat = new BigDecimal(vatAmount);
    }
    
    public void setCurrency(String currency) {
        // RON is default, ignore other values
    }
    
    public void setXmlContent(String xmlContent) {
        this.eFacturaXml = xmlContent;
    }
    
    public void setPdfPath(String pdfPath) {
        // Can be implemented later
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getSeries() {
        return series;
    }
    
    public void setSeries(String series) {
        this.series = series;
    }
    
    public Integer getNumber() {
        return number;
    }
    
    public void setNumber(Integer number) {
        this.number = number;
    }
    
    public Integer getPartnerId() {
        return partnerId;
    }
    
    public void setPartnerId(Integer partnerId) {
        this.partnerId = partnerId;
    }
    
    public Partner getPartner() {
        return partner;
    }
    
    public void setPartner(Partner partner) {
        this.partner = partner;
        if (partner != null) {
            this.partnerId = partner.getId();
        }
    }
    
    public LocalDate getIssueDate() {
        return issueDate;
    }
    
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    public Integer getPaymentMethodId() {
        return paymentMethodId;
    }
    
    public void setPaymentMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }
    
    public String getPaymentMethodName() {
        return paymentMethodName;
    }
    
    public void setPaymentMethodName(String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Boolean getIsEFactura() {
        return isEFactura;
    }
    
    public void setIsEFactura(Boolean isEFactura) {
        this.isEFactura = isEFactura;
    }
    
    public String getEFacturaStatus() {
        return eFacturaStatus;
    }
    
    public void setEFacturaStatus(String eFacturaStatus) {
        this.eFacturaStatus = eFacturaStatus;
    }
    
    public String getEFacturaXml() {
        return eFacturaXml;
    }
    
    public void setEFacturaXml(String eFacturaXml) {
        this.eFacturaXml = eFacturaXml;
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
    
    public List<InvoiceItem> getItems() {
        return items;
    }
    
    public void setItems(List<InvoiceItem> items) {
        this.items = items;
        recalculateTotals();
    }
    
    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", series='" + series + '\'' +
                ", number=" + number +
                ", partnerId=" + partnerId +
                ", issueDate=" + issueDate +
                ", totalWithVat=" + totalWithVat +
                ", status='" + status + '\'' +
                '}';
    }
}

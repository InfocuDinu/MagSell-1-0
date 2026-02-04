package com.magsell.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Model pentru facturi importate din SPV
 */
public class Invoice {
    private int id;
    private String invoiceNumber;
    private String series;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String supplierName;
    private String supplierCif;
    private String supplierAddress;
    private double totalAmount;
    private double vatAmount;
    private String currency;
    private String status; // 'imported', 'processed', 'rejected'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InvoiceItem> items;
    private String xmlContent; // Conținutul XML original din SPV
    private String pdfPath; // Calea către fișierul PDF descărcat

    public Invoice() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "imported";
        this.currency = "RON";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierCif() {
        return supplierCif;
    }

    public void setSupplierCif(String supplierCif) {
        this.supplierCif = supplierCif;
    }

    public String getSupplierAddress() {
        return supplierAddress;
    }

    public void setSupplierAddress(String supplierAddress) {
        this.supplierAddress = supplierAddress;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(double vatAmount) {
        this.vatAmount = vatAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    /**
     * Returnează numărul complet al facturii (serie + număr)
     */
    public String getFullInvoiceNumber() {
        if (series != null && !series.isEmpty()) {
            return series + " " + invoiceNumber;
        }
        return invoiceNumber;
    }

    /**
     * Verifică dacă factura are TVA
     */
    public boolean hasVat() {
        return vatAmount > 0;
    }

    /**
     * Returnează suma fără TVA
     */
    public double getAmountWithoutVat() {
        return totalAmount - vatAmount;
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", series='" + series + '\'' +
                ", issueDate=" + issueDate +
                ", supplierName='" + supplierName + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                '}';
    }
}

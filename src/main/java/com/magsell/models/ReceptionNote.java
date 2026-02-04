package com.magsell.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Model pentru note de recepție generate din facturi
 */
public class ReceptionNote {
    private int id;
    private String noteNumber;
    private String series;
    private LocalDate receptionDate;
    private LocalDate invoiceDate;
    private String invoiceNumber;
    private String supplierName;
    private String supplierCif;
    private String supplierAddress;
    private double totalAmount;
    private double vatAmount;
    private String currency;
    private String status; // 'draft', 'confirmed', 'cancelled'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReceptionNoteItem> items;
    private int invoiceId; // Referință la factura originală
    private String createdBy; // Utilizatorul care a creat nota
    private String notes; // Observații suplimentare

    public ReceptionNote() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "draft";
        this.currency = "RON";
        this.receptionDate = LocalDate.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNoteNumber() {
        return noteNumber;
    }

    public void setNoteNumber(String noteNumber) {
        this.noteNumber = noteNumber;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public LocalDate getReceptionDate() {
        return receptionDate;
    }

    public void setReceptionDate(LocalDate receptionDate) {
        this.receptionDate = receptionDate;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
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

    public List<ReceptionNoteItem> getItems() {
        return items;
    }

    public void setItems(List<ReceptionNoteItem> items) {
        this.items = items;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
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
     * Returnează numărul complet al notei de recepție (serie + număr)
     */
    public String getFullNoteNumber() {
        if (series != null && !series.isEmpty()) {
            return series + " " + noteNumber;
        }
        return noteNumber;
    }

    /**
     * Verifică dacă nota de recepție are TVA
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

    /**
     * Marchează nota ca fiind confirmată
     */
    public void confirm() {
        this.status = "confirmed";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marchează nota ca fiind anulată
     */
    public void cancel() {
        this.status = "cancelled";
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "ReceptionNote{" +
                "id=" + id +
                ", noteNumber='" + noteNumber + '\'' +
                ", series='" + series + '\'' +
                ", receptionDate=" + receptionDate +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", supplierName='" + supplierName + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                '}';
    }
}

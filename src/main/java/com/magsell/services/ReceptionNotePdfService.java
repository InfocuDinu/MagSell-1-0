package com.magsell.services;

import com.magsell.models.ReceptionNote;
import com.magsell.models.ReceptionNoteItem;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviciu pentru generare PDF pentru note de recepție
 */
public class ReceptionNotePdfService {
    private static final Logger logger = LoggerFactory.getLogger(ReceptionNotePdfService.class);
    
    private static final String PDF_OUTPUT_DIR = System.getProperty("user.home") + "/.magsell/pdf_exports";
    
    public ReceptionNotePdfService() {
        // Creăm directorul pentru export PDF dacă nu există
        try {
            Files.createDirectories(Paths.get(PDF_OUTPUT_DIR));
        } catch (IOException e) {
            logger.error("Error creating PDF export directory", e);
        }
    }
    
    /**
     * Generează un PDF pentru nota de recepție
     */
    public byte[] generateReceptionNotePdf(ReceptionNote receptionNote) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // Fonturi
            PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            
            // Adăugăm titlul
            Paragraph title = new Paragraph("NOTĂ DE RECEPȚIE");
            title.setFont(titleFont);
            title.setTextAlignment(TextAlignment.CENTER);
            title.setMarginBottom(20);
            document.add(title);
            
            Paragraph number = new Paragraph("Număr: " + receptionNote.getFullNoteNumber());
            number.setFont(headerFont);
            number.setTextAlignment(TextAlignment.CENTER);
            number.setMarginBottom(10);
            document.add(number);
            
            Paragraph date = new Paragraph("Data: " + 
                receptionNote.getReceptionDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
            date.setFont(headerFont);
            date.setTextAlignment(TextAlignment.CENTER);
            date.setMarginBottom(30);
            document.add(date);
            
            // Informații furnizor
            Paragraph supplierTitle = new Paragraph("DATE FURNIZOR");
            supplierTitle.setFont(titleFont);
            supplierTitle.setMarginBottom(10);
            document.add(supplierTitle);
            
            Table supplierTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            supplierTable.setMarginBottom(20);
            
            Cell nameLabel = new Cell();
            nameLabel.add(new Paragraph("Nume:").setFont(headerFont));
            supplierTable.addCell(nameLabel);
            
            Cell nameValue = new Cell();
            nameValue.add(new Paragraph(receptionNote.getSupplierName()).setFont(normalFont));
            supplierTable.addCell(nameValue);
            
            Cell cifLabel = new Cell();
            cifLabel.add(new Paragraph("CIF:").setFont(headerFont));
            supplierTable.addCell(cifLabel);
            
            Cell cifValue = new Cell();
            cifValue.add(new Paragraph(receptionNote.getSupplierCif()).setFont(normalFont));
            supplierTable.addCell(cifValue);
            
            Cell addressLabel = new Cell();
            addressLabel.add(new Paragraph("Adresă:").setFont(headerFont));
            supplierTable.addCell(addressLabel);
            
            Cell addressValue = new Cell();
            addressValue.add(new Paragraph(receptionNote.getSupplierAddress() != null ? 
                receptionNote.getSupplierAddress() : "").setFont(normalFont));
            supplierTable.addCell(addressValue);
            
            document.add(supplierTable);
            
            // Informații factură (dacă există)
            if (receptionNote.getInvoiceNumber() != null && !receptionNote.getInvoiceNumber().isEmpty()) {
                Paragraph invoiceTitle = new Paragraph("DATE FACTURĂ");
                invoiceTitle.setFont(titleFont);
                invoiceTitle.setMarginBottom(10);
                document.add(invoiceTitle);
                
                Table invoiceTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
                invoiceTable.setMarginBottom(20);
                
                Cell invoiceNumberLabel = new Cell();
                invoiceNumberLabel.add(new Paragraph("Număr Factură:").setFont(headerFont));
                invoiceTable.addCell(invoiceNumberLabel);
                
                Cell invoiceNumberValue = new Cell();
                invoiceNumberValue.add(new Paragraph(receptionNote.getInvoiceNumber()).setFont(normalFont));
                invoiceTable.addCell(invoiceNumberValue);
                
                if (receptionNote.getInvoiceDate() != null) {
                    Cell invoiceDateLabel = new Cell();
                    invoiceDateLabel.add(new Paragraph("Data Factură:").setFont(headerFont));
                    invoiceTable.addCell(invoiceDateLabel);
                    
                    Cell invoiceDateValue = new Cell();
                    invoiceDateValue.add(new Paragraph(receptionNote.getInvoiceDate()
                        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))).setFont(normalFont));
                    invoiceTable.addCell(invoiceDateValue);
                }
                
                document.add(invoiceTable);
            }
            
            // Tabel produse
            Paragraph productsTitle = new Paragraph("PRODUSE RECEPTIONATE");
            productsTitle.setFont(titleFont);
            productsTitle.setMarginBottom(10);
            document.add(productsTitle);
            
            Table productsTable = new Table(UnitValue.createPercentArray(new float[]{
                8, 25, 12, 10, 8, 12, 12, 13
            }));
            productsTable.setMarginBottom(20);
            
            // Header tabel
            Cell crtHeader = new Cell();
            crtHeader.add(new Paragraph("Nr. Crt.").setFont(headerFont));
            productsTable.addHeaderCell(crtHeader);
            
            Cell productHeader = new Cell();
            productHeader.add(new Paragraph("Produs").setFont(headerFont));
            productsTable.addHeaderCell(productHeader);
            
            Cell codeHeader = new Cell();
            codeHeader.add(new Paragraph("Cod").setFont(headerFont));
            productsTable.addHeaderCell(codeHeader);
            
            Cell qtyHeader = new Cell();
            qtyHeader.add(new Paragraph("Cantitate").setFont(headerFont));
            productsTable.addHeaderCell(qtyHeader);
            
            Cell umHeader = new Cell();
            umHeader.add(new Paragraph("UM").setFont(headerFont));
            productsTable.addHeaderCell(umHeader);
            
            Cell priceHeader = new Cell();
            priceHeader.add(new Paragraph("Preț Unitar").setFont(headerFont));
            productsTable.addHeaderCell(priceHeader);
            
            Cell totalHeader = new Cell();
            totalHeader.add(new Paragraph("Total").setFont(headerFont));
            productsTable.addHeaderCell(totalHeader);
            
            Cell vatHeader = new Cell();
            vatHeader.add(new Paragraph("TVA").setFont(headerFont));
            productsTable.addHeaderCell(vatHeader);
            
            int rowNumber = 1;
            double grandTotal = 0.0;
            double grandVat = 0.0;
            
            if (receptionNote.getItems() != null) {
                for (ReceptionNoteItem item : receptionNote.getItems()) {
                    Cell crtCell = new Cell();
                    crtCell.add(new Paragraph(String.valueOf(rowNumber++)).setFont(normalFont));
                    productsTable.addCell(crtCell);
                    
                    Cell productCell = new Cell();
                    productCell.add(new Paragraph(item.getProductName()).setFont(normalFont));
                    productsTable.addCell(productCell);
                    
                    Cell codeCell = new Cell();
                    codeCell.add(new Paragraph(item.getProductCode() != null ? item.getProductCode() : "").setFont(normalFont));
                    productsTable.addCell(codeCell);
                    
                    Cell qtyCell = new Cell();
                    qtyCell.add(new Paragraph(String.format("%.2f", item.getQuantity())).setFont(normalFont));
                    productsTable.addCell(qtyCell);
                    
                    Cell umCell = new Cell();
                    umCell.add(new Paragraph(item.getUnitOfMeasure()).setFont(normalFont));
                    productsTable.addCell(umCell);
                    
                    Cell priceCell = new Cell();
                    priceCell.add(new Paragraph(String.format("%.2f RON", item.getUnitPrice())).setFont(normalFont));
                    productsTable.addCell(priceCell);
                    
                    Cell totalCell = new Cell();
                    totalCell.add(new Paragraph(String.format("%.2f RON", item.getTotalPrice())).setFont(normalFont));
                    productsTable.addCell(totalCell);
                    
                    Cell vatCell = new Cell();
                    vatCell.add(new Paragraph(String.format("%.2f%%", item.getVatRate())).setFont(normalFont));
                    productsTable.addCell(vatCell);
                    
                    grandTotal += item.getTotalPrice();
                    grandVat += item.getVatAmount();
                }
            }
            
            document.add(productsTable);
            
            // Totaluri
            Paragraph totalsTitle = new Paragraph("TOTALURI");
            totalsTitle.setFont(titleFont);
            totalsTitle.setMarginBottom(10);
            document.add(totalsTitle);
            
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}));
            totalsTable.setMarginBottom(20);
            
            Cell noVatLabel = new Cell();
            noVatLabel.add(new Paragraph("Total Fără TVA:").setFont(headerFont));
            totalsTable.addCell(noVatLabel);
            
            Cell noVatValue = new Cell();
            noVatValue.add(new Paragraph(String.format("%.2f RON", grandTotal - grandVat)).setFont(normalFont));
            totalsTable.addCell(noVatValue);
            
            Cell empty1 = new Cell();
            totalsTable.addCell(empty1);
            
            Cell vatLabel = new Cell();
            vatLabel.add(new Paragraph("TVA:").setFont(headerFont));
            totalsTable.addCell(vatLabel);
            
            Cell vatValue = new Cell();
            vatValue.add(new Paragraph(String.format("%.2f RON", grandVat)).setFont(normalFont));
            totalsTable.addCell(vatValue);
            
            Cell empty2 = new Cell();
            totalsTable.addCell(empty2);
            
            Cell totalLabel = new Cell();
            totalLabel.add(new Paragraph("Total General:").setFont(headerFont));
            totalsTable.addCell(totalLabel);
            
            Cell totalValue = new Cell();
            totalValue.add(new Paragraph(String.format("%.2f RON", grandTotal)).setFont(normalFont));
            totalsTable.addCell(totalValue);
            
            Cell empty3 = new Cell();
            totalsTable.addCell(empty3);
            
            document.add(totalsTable);
            
            // Observații
            if (receptionNote.getNotes() != null && !receptionNote.getNotes().trim().isEmpty()) {
                Paragraph notesTitle = new Paragraph("OBSERVAȚII");
                notesTitle.setFont(titleFont);
                notesTitle.setMarginBottom(10);
                document.add(notesTitle);
                
                Paragraph notes = new Paragraph(receptionNote.getNotes());
                notes.setFont(normalFont);
                notes.setMarginBottom(20);
                document.add(notes);
            }
            
            // Semnături
            Paragraph signaturesTitle = new Paragraph("Semnături");
            signaturesTitle.setFont(titleFont);
            signaturesTitle.setMarginBottom(10);
            document.add(signaturesTitle);
            
            Table signaturesTable = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}));
            signaturesTable.setMarginBottom(20);
            
            Cell receivedLabel = new Cell();
            receivedLabel.add(new Paragraph("Recepționat de:").setFont(headerFont));
            signaturesTable.addCell(receivedLabel);
            
            Cell receivedLine = new Cell();
            receivedLine.add(new Paragraph("_________________________").setFont(normalFont));
            signaturesTable.addCell(receivedLine);
            
            Cell receivedDate = new Cell();
            receivedDate.add(new Paragraph("Data: _________").setFont(normalFont));
            signaturesTable.addCell(receivedDate);
            
            Cell verifiedLabel = new Cell();
            verifiedLabel.add(new Paragraph("Verificat de:").setFont(headerFont));
            signaturesTable.addCell(verifiedLabel);
            
            Cell verifiedLine = new Cell();
            verifiedLine.add(new Paragraph("_________________________").setFont(normalFont));
            signaturesTable.addCell(verifiedLine);
            
            Cell verifiedDate = new Cell();
            verifiedDate.add(new Paragraph("Data: _________").setFont(normalFont));
            signaturesTable.addCell(verifiedDate);
            
            document.add(signaturesTable);
            
            // Footer
            Paragraph footer = new Paragraph("Generat de MagSell - Patisserie Management");
            footer.setFont(normalFont);
            footer.setTextAlignment(TextAlignment.CENTER);
            footer.setMarginTop(50);
            document.add(footer);
            
            document.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generating PDF for reception note", e);
            return null;
        }
    }
    
    /**
     * Salvează PDF-ul pe disc
     */
    public String saveReceptionNotePdf(ReceptionNote receptionNote) {
        try {
            byte[] pdfBytes = generateReceptionNotePdf(receptionNote);
            
            if (pdfBytes != null) {
                String fileName = "Nota_Receptie_" + receptionNote.getFullNoteNumber().replace(" ", "_") + ".pdf";
                String filePath = Paths.get(PDF_OUTPUT_DIR, fileName).toString();
                
                Files.write(Paths.get(filePath), pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
                logger.info("Saved PDF for reception note {} to: {}", receptionNote.getFullNoteNumber(), filePath);
                return filePath;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error saving PDF for reception note", e);
            return null;
        }
    }
    
    /**
     * Deschide PDF-ul în aplicația implicită
     */
    public void openReceptionNotePdf(ReceptionNote receptionNote) {
        try {
            String pdfPath = saveReceptionNotePdf(receptionNote);
            
            if (pdfPath != null) {
                // Deschidem fișierul PDF cu aplicația implicită a sistemului
                File pdfFile = new File(pdfPath);
                java.awt.Desktop.getDesktop().open(pdfFile);
                
                logger.info("Opened PDF for reception note: {}", receptionNote.getFullNoteNumber());
            }
        } catch (Exception e) {
            logger.error("Error opening PDF for reception note", e);
        }
    }
}

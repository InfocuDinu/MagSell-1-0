package com.magsell.services;

import com.magsell.models.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviciu pentru generarea PDF-urilor pentru facturi și alte documente
 * Folosește OpenPDF (open source) pentru generare profesională
 */
public class PdfGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationService.class);
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    
    private final CompanySettingsService companySettingsService;
    
    public PdfGenerationService() {
        this.companySettingsService = new CompanySettingsService();
    }
    
    /**
     * Generează PDF pentru o factură fiscală
     */
    public byte[] generateInvoicePdf(Invoice invoice) throws Exception {
        logger.info("Generating PDF for invoice: {}", invoice.getFullNumber());
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Setări font pentru suport diacritice românești
            Font fontRomanian = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
            Font fontBoldRomanian = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL);
            Font fontTitleRomanian = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.NORMAL);
            
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Antet - Informații companie
            addCompanyHeader(document, fontBoldRomanian, fontRomanian);
            
            // Titlu factură
            addInvoiceTitle(document, invoice, fontTitleRomanian);
            
            // Informații factură (client, date factură)
            addInvoiceInfo(document, invoice, fontBoldRomanian, fontRomanian);
            
            // Tabel produse
            addProductsTable(document, invoice, fontBoldRomanian, fontRomanian);
            
            // Totaluri
            addTotalsSection(document, invoice, fontBoldRomanian, fontRomanian);
            
            // Semnături
            addSignaturesSection(document, fontRomanian);
            
            document.close();
            
            logger.info("PDF generated successfully for invoice: {}", invoice.getFullNumber());
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("Error generating PDF for invoice: {}", invoice.getFullNumber(), e);
            throw new Exception("Eroare la generarea PDF-ului: " + e.getMessage(), e);
        }
    }
    
    /**
     * Salvează PDF pe disc
     */
    public String saveInvoicePdf(Invoice invoice, String outputPath) throws Exception {
        byte[] pdfData = generateInvoicePdf(invoice);
        
        // Creează directorul dacă nu există
        Path directory = Paths.get(outputPath).getParent();
        if (directory != null && !Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        
        // Nume fișier: FACTURA_SERIE_NUMAR.pdf
        String fileName = String.format("FACTURA_%s_%06d.pdf", 
                                       invoice.getSeries(), 
                                       invoice.getNumber());
        String fullPath = Paths.get(outputPath, fileName).toString();
        
        try (FileOutputStream fos = new FileOutputStream(fullPath)) {
            fos.write(pdfData);
        }
        
        logger.info("PDF saved to: {}", fullPath);
        return fullPath;
    }
    
    private void addCompanyHeader(Document document, Font fontBold, Font fontNormal) throws Exception {
        CompanySettings company = companySettingsService.getCompanySettings();
        if (company == null) {
            company = getDefaultCompanySettings();
        }
        
        Paragraph companyInfo = new Paragraph();
        companyInfo.add(new Chunk(company.getCompanyName(), fontBold));
        companyInfo.add(Chunk.NEWLINE);
        companyInfo.add(new Chunk("CUI: " + company.getCui(), fontNormal));
        companyInfo.add(Chunk.NEWLINE);
        companyInfo.add(new Chunk("Reg. Com.: " + company.getRegCom(), fontNormal));
        companyInfo.add(Chunk.NEWLINE);
        companyInfo.add(new Chunk("Adresa: " + company.getAddress(), fontNormal));
        companyInfo.add(Chunk.NEWLINE);
        companyInfo.add(new Chunk("Tel: " + company.getPhone(), fontNormal));
        companyInfo.add(Chunk.NEWLINE);
        companyInfo.add(new Chunk("Email: " + company.getEmail(), fontNormal));
        companyInfo.add(Chunk.NEWLINE);
        companyInfo.add(new Chunk("Banca: " + company.getBankName(), fontNormal));
        companyInfo.add(Chunk.NEWLINE);
        companyInfo.add(new Chunk("IBAN: " + company.getBankAccount(), fontNormal));
        
        document.add(companyInfo);
        document.add(Chunk.NEWLINE);
    }
    
    private void addInvoiceTitle(Document document, Invoice invoice, Font fontTitle) throws DocumentException {
        Paragraph title = new Paragraph();
        title.setAlignment(Element.ALIGN_CENTER);
        title.add(new Chunk("FACTURĂ FISCALĂ", fontTitle));
        document.add(title);
        document.add(Chunk.NEWLINE);
        
        Paragraph number = new Paragraph();
        number.setAlignment(Element.ALIGN_CENTER);
        number.add(new Chunk("Nr. " + invoice.getFullNumber(), fontTitle));
        document.add(number);
        document.add(Chunk.NEWLINE);
    }
    
    private void addInvoiceInfo(Document document, Invoice invoice, Font fontBold, Font fontNormal) 
            throws DocumentException, Exception {
        
        // Tabel cu informații factură
        Table infoTable = new Table(2);
        infoTable.setWidth(100);
        infoTable.setBorderWidth(0);
        infoTable.setPadding(5);
        
        // Client
        if (invoice.getPartner() != null) {
            infoTable.addCell(createCell("CLIENT:", fontBold, Element.ALIGN_LEFT, true));
            infoTable.addCell(createCell(invoice.getPartner().getName(), fontNormal, Element.ALIGN_LEFT, false));
            
            infoTable.addCell(createCell("CUI:", fontBold, Element.ALIGN_LEFT, true));
            infoTable.addCell(createCell(invoice.getPartner().getCui(), fontNormal, Element.ALIGN_LEFT, false));
            
            infoTable.addCell(createCell("Adresa:", fontBold, Element.ALIGN_LEFT, true));
            infoTable.addCell(createCell(invoice.getPartner().getAddress(), fontNormal, Element.ALIGN_LEFT, false));
        }
        
        // Date factură
        infoTable.addCell(createCell("Data emiterii:", fontBold, Element.ALIGN_LEFT, true));
        infoTable.addCell(createCell(invoice.getIssueDate().format(DATE_FORMATTER), fontNormal, Element.ALIGN_LEFT, false));
        
        infoTable.addCell(createCell("Data scadenței:", fontBold, Element.ALIGN_LEFT, true));
        infoTable.addCell(createCell(invoice.getDueDate().format(DATE_FORMATTER), fontNormal, Element.ALIGN_LEFT, false));
        
        document.add(infoTable);
        document.add(Chunk.NEWLINE);
    }
    
    private void addProductsTable(Document document, Invoice invoice, Font fontBold, Font fontNormal) 
            throws DocumentException {
        
        // Tabel produse
        Table productsTable = new Table(7);
        productsTable.setWidth(100);
        productsTable.setBorderWidth(1);
        productsTable.setPadding(3);
        
        // Header
        productsTable.addCell(createCell("Nr. Crt.", fontBold, Element.ALIGN_CENTER, true));
        productsTable.addCell(createCell("Cod produs", fontBold, Element.ALIGN_LEFT, true));
        productsTable.addCell(createCell("Denumire produs", fontBold, Element.ALIGN_LEFT, true));
        productsTable.addCell(createCell("Cant.", fontBold, Element.ALIGN_RIGHT, true));
        productsTable.addCell(createCell("Preț unitar", fontBold, Element.ALIGN_RIGHT, true));
        productsTable.addCell(createCell("Valoare", fontBold, Element.ALIGN_RIGHT, true));
        productsTable.addCell(createCell("TVA", fontBold, Element.ALIGN_RIGHT, true));
        
        // Produse
        int crt = 1;
        for (InvoiceItem item : invoice.getItems()) {
            productsTable.addCell(createCell(String.valueOf(crt++), fontNormal, Element.ALIGN_CENTER, false));
            productsTable.addCell(createCell(item.getProductCode(), fontNormal, Element.ALIGN_LEFT, false));
            productsTable.addCell(createCell(item.getProductName(), fontNormal, Element.ALIGN_LEFT, false));
            productsTable.addCell(createCell(formatDecimal(item.getQuantity()), fontNormal, Element.ALIGN_RIGHT, false));
            productsTable.addCell(createCell(formatDecimal(item.getUnitPrice()), fontNormal, Element.ALIGN_RIGHT, false));
            productsTable.addCell(createCell(formatDecimal(item.getTotalAmount()), fontNormal, Element.ALIGN_RIGHT, false));
            productsTable.addCell(createCell(formatDecimal(item.getTotalVat()), fontNormal, Element.ALIGN_RIGHT, false));
        }
        
        document.add(productsTable);
        document.add(Chunk.NEWLINE);
    }
    
    private void addTotalsSection(Document document, Invoice invoice, Font fontBold, Font fontNormal) 
            throws DocumentException {
        
        // Tabel totaluri
        Table totalsTable = new Table(2);
        totalsTable.setWidth(100);
        totalsTable.setBorderWidth(0);
        totalsTable.setPadding(5);
        
        // Celule goale pentru aliniere la dreapta
        totalsTable.addCell(createCell("", fontNormal, Element.ALIGN_LEFT, false));
        totalsTable.addCell(createCell("", fontNormal, Element.ALIGN_LEFT, false));
        
        totalsTable.addCell(createCell("Subtotal:", fontBold, Element.ALIGN_RIGHT, false));
        totalsTable.addCell(createCell(formatDecimal(invoice.getTotalAmount()) + " RON", fontNormal, Element.ALIGN_RIGHT, false));
        
        totalsTable.addCell(createCell("TVA:", fontBold, Element.ALIGN_RIGHT, false));
        totalsTable.addCell(createCell(formatDecimal(invoice.getTotalVat()) + " RON", fontNormal, Element.ALIGN_RIGHT, false));
        
        totalsTable.addCell(createCell("TOTAL:", fontBold, Element.ALIGN_RIGHT, false));
        totalsTable.addCell(createCell(formatDecimal(invoice.getTotalWithVat()) + " RON", fontBold, Element.ALIGN_RIGHT, false));
        
        document.add(totalsTable);
        document.add(Chunk.NEWLINE);
        
        // Suma în litere
        Paragraph amountInWords = new Paragraph();
        amountInWords.add(new Chunk("Suma în litere: " + convertAmountToWords(invoice.getTotalWithVat()), fontNormal));
        document.add(amountInWords);
        document.add(Chunk.NEWLINE);
    }
    
    private void addSignaturesSection(Document document, Font fontNormal) throws DocumentException {
        Table signaturesTable = new Table(3);
        signaturesTable.setWidth(100);
        signaturesTable.setBorderWidth(0);
        signaturesTable.setPadding(20);
        
        signaturesTable.addCell(createCell("SEMNĂTURA", fontNormal, Element.ALIGN_CENTER, false));
        signaturesTable.addCell(createCell("SEMNĂTURA", fontNormal, Element.ALIGN_CENTER, false));
        signaturesTable.addCell(createCell("SEMNĂTURA", fontNormal, Element.ALIGN_CENTER, false));
        
        signaturesTable.addCell(createCell("_________________", fontNormal, Element.ALIGN_CENTER, false));
        signaturesTable.addCell(createCell("_________________", fontNormal, Element.ALIGN_CENTER, false));
        signaturesTable.addCell(createCell("_________________", fontNormal, Element.ALIGN_CENTER, false));
        
        signaturesTable.addCell(createCell("Delegat client", fontNormal, Element.ALIGN_CENTER, false));
        signaturesTable.addCell(createCell("Casier", fontNormal, Element.ALIGN_CENTER, false));
        signaturesTable.addCell(createCell("Director", fontNormal, Element.ALIGN_CENTER, false));
        
        document.add(signaturesTable);
    }
    
    private Cell createCell(String content, Font font, int alignment, boolean isHeader) {
        Cell cell = new Cell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        if (isHeader) {
            cell.setBackgroundColor(new Color(220, 220, 220));
        }
        
        return cell;
    }
    
    private String formatDecimal(BigDecimal value) {
        if (value == null) return "0,00";
        return DECIMAL_FORMAT.format(value.setScale(2, RoundingMode.HALF_UP));
    }
    
    private String convertAmountToWords(BigDecimal amount) {
        // Implementare simplificată - în producție se folosește o librărie dedicată
        long lei = amount.longValue();
        int bani = amount.subtract(new BigDecimal(lei)).multiply(new BigDecimal(100)).intValue();
        
        String leiInWords = convertNumberToWords(lei);
        String result = leiInWords + " lei";
        
        if (bani > 0) {
            result += " și " + bani + " bani";
        }
        
        return result;
    }
    
    private String convertNumberToWords(long number) {
        // Implementare simplificată pentru conversie numerelor în cuvinte
        if (number == 0) return "zero";
        if (number == 1) return "unu";
        if (number == 2) return "doi";
        if (number == 3) return "trei";
        if (number == 4) return "patru";
        if (number == 5) return "cinci";
        if (number == 6) return "șase";
        if (number == 7) return "șapte";
        if (number == 8) return "opt";
        if (number == 9) return "nouă";
        if (number == 10) return "zece";
        
        // Pentru numere mai mari, se implementează logica completă
        return String.valueOf(number);
    }
    
    private CompanySettings getDefaultCompanySettings() {
        CompanySettings company = new CompanySettings();
        company.setCompanyName("MagSell SRL");
        company.setCui("RO12345678");
        company.setRegCom("J40/1234/2026");
        company.setAddress("Str. Principală Nr. 1, București");
        company.setPhone("0211234567");
        company.setEmail("office@magsell.ro");
        company.setBankName("BANK SA");
        company.setBankAccount("RO12BANK0000123456789012345");
        return company;
    }
}

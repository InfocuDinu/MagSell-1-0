package com.magsell.services;

import com.magsell.models.Sale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Serviciu pentru exportul rapoartelor de vânzări în PDF și Excel.
 */
public class ExportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private final SalesService salesService = new SalesService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("ro"));

    /**
     * Exportă raportul de vânzări în format PDF.
     */
    public void exportToPDF(LocalDate startDate, LocalDate endDate, Stage stage) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvează raport PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            fileChooser.setInitialFileName("Raport_Vanzari_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf");

            File file = fileChooser.showSaveDialog(stage);
            if (file == null) {
                return;
            }

            List<Sale> sales = getSalesInRange(startDate, endDate);
            BigDecimal totalRevenue = sales.stream()
                    .map(Sale::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Titlu
            Paragraph title = new Paragraph("Raport de Vânzări - MagSell")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // Perioada
            Paragraph period = new Paragraph("Perioadă: " + startDate.format(dateFormatter) + " - " + endDate.format(dateFormatter))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(period);

            // Tabel cu vânzări
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 1, 1, 1}))
                    .useAllAvailableWidth();

            // Header
            addTableHeader(table, "ID", "Produs", "Cantitate", "Preț unitar", "Preț total", "Data");

            // Rânduri
            for (Sale sale : sales) {
                addTableRow(table,
                        String.valueOf(sale.getId()),
                        sale.getProductName(),
                        String.valueOf(sale.getQuantity()),
                        formatCurrency(sale.getUnitPrice()),
                        formatCurrency(sale.getTotalPrice()),
                        sale.getSaleDate() != null ? sale.getSaleDate().format(dateFormatter) : ""
                );
            }

            document.add(table);

            // Total
            Paragraph total = new Paragraph("Total venituri: " + formatCurrency(totalRevenue))
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(20);
            document.add(total);

            document.close();
            logger.info("Raport PDF exportat cu succes: " + file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Eroare la exportul PDF", e);
            throw new RuntimeException("Eroare la exportul PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Exportă raportul de vânzări în format Excel.
     */
    public void exportToExcel(LocalDate startDate, LocalDate endDate, Stage stage) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvează raport Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            fileChooser.setInitialFileName("Raport_Vanzari_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx");

            File file = fileChooser.showSaveDialog(stage);
            if (file == null) {
                return;
            }

            List<Sale> sales = getSalesInRange(startDate, endDate);
            BigDecimal totalRevenue = sales.stream()
                    .map(Sale::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Raport Vânzări");

            // Stiluri
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Titlu
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Raport de Vânzări - MagSell");
            titleCell.setCellStyle(headerStyle);

            // Perioada
            Row periodRow = sheet.createRow(1);
            org.apache.poi.ss.usermodel.Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue("Perioadă: " + startDate.format(dateFormatter) + " - " + endDate.format(dateFormatter));

            // Header tabel
            Row headerRow = sheet.createRow(3);
            String[] headers = {"ID", "Produs", "Cantitate", "Preț unitar", "Preț total", "Data"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Date
            int rowNum = 4;
            for (Sale sale : sales) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sale.getId());
                row.createCell(1).setCellValue(sale.getProductName());
                row.createCell(2).setCellValue(sale.getQuantity());
                row.createCell(3).setCellValue(sale.getUnitPrice().doubleValue());
                row.createCell(4).setCellValue(sale.getTotalPrice().doubleValue());
                row.createCell(5).setCellValue(sale.getSaleDate() != null ? sale.getSaleDate().format(dateFormatter) : "");

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // Total
            Row totalRow = sheet.createRow(rowNum + 1);
            org.apache.poi.ss.usermodel.Cell totalLabelCell = totalRow.createCell(4);
            totalLabelCell.setCellValue("Total venituri:");
            totalLabelCell.setCellStyle(headerStyle);
            org.apache.poi.ss.usermodel.Cell totalValueCell = totalRow.createCell(5);
            totalValueCell.setCellValue(totalRevenue.doubleValue());
            totalValueCell.setCellStyle(headerStyle);

            // Auto-size coloane
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

            logger.info("Raport Excel exportat cu succes: " + file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Eroare la exportul Excel", e);
            throw new RuntimeException("Eroare la exportul Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Obține vânzările dintr-o perioadă.
     */
    private List<Sale> getSalesInRange(LocalDate startDate, LocalDate endDate) {
        try {
            List<Sale> allSales = salesService.getAllSales();
            return allSales.stream()
                    .filter(sale -> {
                        if (sale.getSaleDate() == null) return false;
                        LocalDate saleDate = sale.getSaleDate().toLocalDate();
                        return !saleDate.isBefore(startDate) && !saleDate.isAfter(endDate);
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("Eroare la obținerea vânzărilor", e);
            return List.of();
        }
    }

    /**
     * Adaugă header la tabel PDF.
     */
    private void addTableHeader(Table table, String... headers) {
        for (String header : headers) {
            com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell().add(new Paragraph(header))
                    .setBold()
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
    }

    /**
     * Adaugă un rând la tabel PDF.
     */
    private void addTableRow(Table table, String... values) {
        for (String value : values) {
            com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell().add(new Paragraph(value != null ? value : ""));
            table.addCell(cell);
        }
    }

    /**
     * Formatează o sumă ca monedă.
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0,00 RON";
        }
        return String.format("%.2f RON", amount.doubleValue());
    }
}

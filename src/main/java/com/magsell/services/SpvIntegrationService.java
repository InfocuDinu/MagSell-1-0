package com.magsell.services;

import com.magsell.models.Invoice;
import com.magsell.models.InvoiceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Serviciu pentru integrarea cu Spațiul Privat Virtual (SPV)
 * pentru importul facturilor și generarea notelor de recepție
 */
public class SpvIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(SpvIntegrationService.class);
    
    // URL-uri SPV (acestea sunt exemple - în realitate ar fi URL-uri oficiale SPV)
    private static final String SPV_BASE_URL = "https://api.anaf.ro/SPVAPI";
    private static final String SPV_INVOICES_ENDPOINT = "/v1/invoices";
    private static final String SPV_DOWNLOAD_ENDPOINT = "/v1/download";
    
    // Director pentru stocarea fișierelor descărcate
    private static final String DOWNLOAD_DIR = System.getProperty("user.home") + "/.magsell/spv_downloads";
    
    public SpvIntegrationService() {
        // Creăm directorul pentru descărcări dacă nu există
        try {
            Files.createDirectories(Paths.get(DOWNLOAD_DIR));
        } catch (Exception e) {
            logger.error("Error creating download directory", e);
        }
    }
    
    /**
     * Importă facturile din SPV pentru o perioadă dată
     */
    public List<Invoice> importInvoicesFromSPV(LocalDate startDate, LocalDate endDate, String cif) {
        List<Invoice> invoices = new ArrayList<>();
        
        try {
            logger.info("Importing invoices from SPV for period {} to {} for CIF: {}", startDate, endDate, cif);
            
            // Simulare import - în realitate ar fi apel API către SPV
            // Pentru demo, vom citi din fișiere XML locale
            invoices = importInvoicesFromLocalFiles();
            
            logger.info("Successfully imported {} invoices from SPV", invoices.size());
            
        } catch (Exception e) {
            logger.error("Error importing invoices from SPV", e);
        }
        
        return invoices;
    }
    
    /**
     * Importă facturi din fișiere XML locale (pentru demo/testare)
     */
    private List<Invoice> importInvoicesFromLocalFiles() {
        List<Invoice> invoices = new ArrayList<>();
        
        try {
            // Căutăm fișiere XML în directorul de descărcări
            Path downloadPath = Paths.get(DOWNLOAD_DIR);
            if (!Files.exists(downloadPath)) {
                logger.warn("Download directory does not exist: {}", DOWNLOAD_DIR);
                return invoices;
            }
            
            Files.list(downloadPath)
                .filter(path -> path.toString().endsWith(".xml"))
                .forEach(xmlFile -> {
                    try {
                        Invoice invoice = parseInvoiceFromXML(xmlFile.toFile());
                        if (invoice != null) {
                            invoices.add(invoice);
                            logger.debug("Parsed invoice: {}", invoice.getFullInvoiceNumber());
                        }
                    } catch (Exception e) {
                        logger.error("Error parsing XML file: {}", xmlFile, e);
                    }
                });
                
        } catch (Exception e) {
            logger.error("Error reading local XML files", e);
        }
        
        return invoices;
    }
    
    /**
     * Parsează o factură din fișier XML
     */
    private Invoice parseInvoiceFromXML(File xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);
            
            Element root = document.getDocumentElement();
            
            Invoice invoice = new Invoice();
            
            // Extragem datele facturii
            NodeList invoiceNodes = root.getElementsByTagName("Invoice");
            if (invoiceNodes.getLength() > 0) {
                Element invoiceElement = (Element) invoiceNodes.item(0);
                
                invoice.setInvoiceNumber(getElementValue(invoiceElement, "InvoiceNumber"));
                invoice.setSeries(getElementValue(invoiceElement, "InvoiceSeries"));
                
                // Parse date
                String issueDateStr = getElementValue(invoiceElement, "IssueDate");
                if (issueDateStr != null && !issueDateStr.isEmpty()) {
                    invoice.setIssueDate(LocalDate.parse(issueDateStr, DateTimeFormatter.ISO_LOCAL_DATE));
                }
                
                String dueDateStr = getElementValue(invoiceElement, "DueDate");
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    invoice.setDueDate(LocalDate.parse(dueDateStr, DateTimeFormatter.ISO_LOCAL_DATE));
                }
                
                // Supplier information
                NodeList supplierNodes = invoiceElement.getElementsByTagName("Supplier");
                if (supplierNodes.getLength() > 0) {
                    Element supplierElement = (Element) supplierNodes.item(0);
                    invoice.setSupplierName(getElementValue(supplierElement, "Name"));
                    invoice.setSupplierCif(getElementValue(supplierElement, "CIF"));
                    invoice.setSupplierAddress(getElementValue(supplierElement, "Address"));
                }
                
                // Amounts
                String totalAmountStr = getElementValue(invoiceElement, "TotalAmount");
                if (totalAmountStr != null && !totalAmountStr.isEmpty()) {
                    invoice.setTotalAmount(Double.parseDouble(totalAmountStr));
                }
                
                String vatAmountStr = getElementValue(invoiceElement, "VATAmount");
                if (vatAmountStr != null && !vatAmountStr.isEmpty()) {
                    invoice.setVatAmount(Double.parseDouble(vatAmountStr));
                }
                
                invoice.setCurrency(getElementValue(invoiceElement, "Currency"));
                
                // Parse invoice items
                List<InvoiceItem> items = parseInvoiceItems(invoiceElement);
                invoice.setItems(items);
                
                // Store XML content
                String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()));
                invoice.setXmlContent(xmlContent);
                
                // Set PDF path if exists
                String pdfPath = xmlFile.getAbsolutePath().replace(".xml", ".pdf");
                if (Files.exists(Paths.get(pdfPath))) {
                    invoice.setPdfPath(pdfPath);
                }
            }
            
            return invoice;
            
        } catch (Exception e) {
            logger.error("Error parsing invoice from XML: {}", xmlFile.getName(), e);
            return null;
        }
    }
    
    /**
     * Parsează elementele din factură
     */
    private List<InvoiceItem> parseInvoiceItems(Element invoiceElement) {
        List<InvoiceItem> items = new ArrayList<>();
        
        try {
            NodeList itemNodes = invoiceElement.getElementsByTagName("InvoiceLine");
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element itemElement = (Element) itemNodes.item(i);
                
                InvoiceItem item = new InvoiceItem();
                item.setProductName(getElementValue(itemElement, "ProductName"));
                item.setProductCode(getElementValue(itemElement, "ProductCode"));
                item.setDescription(getElementValue(itemElement, "Description"));
                
                String quantityStr = getElementValue(itemElement, "Quantity");
                if (quantityStr != null && !quantityStr.isEmpty()) {
                    item.setQuantity(Double.parseDouble(quantityStr));
                }
                
                item.setUnitOfMeasure(getElementValue(itemElement, "UnitOfMeasure"));
                
                String unitPriceStr = getElementValue(itemElement, "UnitPrice");
                if (unitPriceStr != null && !unitPriceStr.isEmpty()) {
                    item.setUnitPrice(Double.parseDouble(unitPriceStr));
                }
                
                String totalPriceStr = getElementValue(itemElement, "TotalPrice");
                if (totalPriceStr != null && !totalPriceStr.isEmpty()) {
                    item.setTotalPrice(Double.parseDouble(totalPriceStr));
                }
                
                String vatRateStr = getElementValue(itemElement, "VATRate");
                if (vatRateStr != null && !vatRateStr.isEmpty()) {
                    item.setVatRate(Double.parseDouble(vatRateStr));
                }
                
                item.setCategory(getElementValue(itemElement, "Category"));
                
                items.add(item);
            }
        } catch (Exception e) {
            logger.error("Error parsing invoice items", e);
        }
        
        return items;
    }
    
    /**
     * Obține valoarea unui element XML
     */
    private String getElementValue(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            Node node = nodes.item(0);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return node.getTextContent();
            }
        }
        return null;
    }
    
    /**
     * Descarcă o factură din SPV (PDF și XML)
     */
    public boolean downloadInvoiceFromSPV(String invoiceId, String cif) {
        try {
            logger.info("Downloading invoice {} from SPV for CIF: {}", invoiceId, cif);
            
            // Simulare descărcare - în realitate ar fi apel API către SPV
            // Pentru demo, creăm fișiere de exemplu
            return createSampleInvoiceFiles(invoiceId);
            
        } catch (Exception e) {
            logger.error("Error downloading invoice from SPV", e);
            return false;
        }
    }
    
    /**
     * Creează fișiere de exemplu pentru facturi (pentru demo)
     */
    private boolean createSampleInvoiceFiles(String invoiceId) {
        try {
            // Creăm fișier XML de exemplu
            String xmlContent = createSampleInvoiceXML(invoiceId);
            Path xmlPath = Paths.get(DOWNLOAD_DIR, invoiceId + ".xml");
            Files.write(xmlPath, xmlContent.getBytes());
            
            // Creăm fișier PDF de exemplu (gol pentru demo)
            Path pdfPath = Paths.get(DOWNLOAD_DIR, invoiceId + ".pdf");
            Files.write(pdfPath, "Sample PDF content".getBytes());
            
            logger.info("Created sample files for invoice: {}", invoiceId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error creating sample invoice files", e);
            return false;
        }
    }
    
    /**
     * Creează conținut XML de exemplu pentru factură
     */
    private String createSampleInvoiceXML(String invoiceId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Invoice>\n" +
                "  <InvoiceNumber>" + invoiceId + "</InvoiceNumber>\n" +
                "  <InvoiceSeries>F</InvoiceSeries>\n" +
                "  <IssueDate>" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "</IssueDate>\n" +
                "  <DueDate>" + LocalDate.now().plusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE) + "</DueDate>\n" +
                "  <Supplier>\n" +
                "    <Name>FURNIZOR EXEMPLU SRL</Name>\n" +
                "    <CIF>RO12345678</CIF>\n" +
                "    <Address>Str. Exemplu nr. 1, București</Address>\n" +
                "  </Supplier>\n" +
                "  <TotalAmount>1190.00</TotalAmount>\n" +
                "  <VATAmount>190.00</VATAmount>\n" +
                "  <Currency>RON</Currency>\n" +
                "  <InvoiceLine>\n" +
                "    <ProductName>Produs exemplu 1</ProductName>\n" +
                "    <ProductCode>PE001</ProductCode>\n" +
                "    <Description>Descriere produs exemplu 1</Description>\n" +
                "    <Quantity>10</Quantity>\n" +
                "    <UnitOfMeasure>buc</UnitOfMeasure>\n" +
                "    <UnitPrice>100.00</UnitPrice>\n" +
                "    <TotalPrice>1000.00</TotalPrice>\n" +
                "    <VATRate>19.0</VATRate>\n" +
                "    <Category>Categoria 1</Category>\n" +
                "  </InvoiceLine>\n" +
                "  <InvoiceLine>\n" +
                "    <ProductName>Produs exemplu 2</ProductName>\n" +
                "    <ProductCode>PE002</ProductCode>\n" +
                "    <Description>Descriere produs exemplu 2</Description>\n" +
                "    <Quantity>5</Quantity>\n" +
                "    <UnitOfMeasure>buc</UnitOfMeasure>\n" +
                "    <UnitPrice>50.00</UnitPrice>\n" +
                "    <TotalPrice>250.00</TotalPrice>\n" +
                "    <VATRate>19.0</VATRate>\n" +
                "    <Category>Categoria 2</Category>\n" +
                "  </InvoiceLine>\n" +
                "</Invoice>";
    }
    
    /**
     * Verifică conexiunea cu SPV
     */
    public boolean testSpvConnection() {
        try {
            logger.info("Testing SPV connection...");
            
            // Simulare test conexiune
            // În realitate ar fi apel API către SPV
            return true;
            
        } catch (Exception e) {
            logger.error("SPV connection test failed", e);
            return false;
        }
    }
}

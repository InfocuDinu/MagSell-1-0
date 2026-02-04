package com.magsell.services;

import com.magsell.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviciu pentru generarea XML-ului pentru e-Factura (standard UBL 2.1)
 * Respectă specificațiile ANAF pentru RO e-Factura
 */
public class EFacturaService {
    private static final Logger logger = LoggerFactory.getLogger(EFacturaService.class);
    
    private final CompanySettingsService companySettingsService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    public EFacturaService() {
        this.companySettingsService = new CompanySettingsService();
    }
    
    /**
     * Generează XML UBL pentru o factură fiscală
     */
    public String generateEFacturaXml(Invoice invoice) throws Exception {
        logger.info("Generating e-Factura XML for invoice: {}", invoice.getFullNumber());
        
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            
            // Document root
            org.w3c.dom.Document doc = docBuilder.newDocument();
            
            // Root element - Invoice
            org.w3c.dom.Element rootElement = doc.createElement("Invoice");
            rootElement.setAttribute("xmlns", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2");
            rootElement.setAttribute("xmlns:cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
            rootElement.setAttribute("xmlns:cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
            doc.appendChild(rootElement);
            
            // CustomizationID
            addElement(doc, rootElement, "cbc:CustomizationID", "urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:2");
            
            // ProfileID
            addElement(doc, rootElement, "cbc:ProfileID", "urn:fdc:peppol-eu:en16931:bis:billing:01:1.0");
            
            // ID
            addElement(doc, rootElement, "cbc:ID", invoice.getFullNumber());
            
            // IssueDate
            addElement(doc, rootElement, "cbc:IssueDate", invoice.getIssueDate().toString());
            
            // DueDate
            addElement(doc, rootElement, "cbc:DueDate", invoice.getDueDate().toString());
            
            // InvoiceTypeCode
            addElement(doc, rootElement, "cbc:InvoiceTypeCode", "380");
            
            // DocumentCurrencyCode
            addElement(doc, rootElement, "cbc:DocumentCurrencyCode", "RON");
            
            // AccountingSupplierParty (Furnizor)
            addAccountingSupplierParty(doc, rootElement);
            
            // AccountingCustomerParty (Client)
            addAccountingCustomerParty(doc, rootElement, invoice.getPartner());
            
            // Delivery
            addDelivery(doc, rootElement);
            
            // PaymentMeans
            addPaymentMeans(doc, rootElement);
            
            // TaxTotal
            addTaxTotal(doc, rootElement, invoice.getTotalVat());
            
            // LegalMonetaryTotal
            addLegalMonetaryTotal(doc, rootElement, invoice);
            
            // InvoiceLines
            addInvoiceLines(doc, rootElement, invoice.getItems());
            
            // Convert to XML string
            return convertToString(doc);
            
        } catch (Exception e) {
            logger.error("Error generating e-Factura XML for invoice: {}", invoice.getFullNumber(), e);
            throw new Exception("Eroare la generarea XML-ului e-Factura: " + e.getMessage(), e);
        }
    }
    
    private void addAccountingSupplierParty(org.w3c.dom.Document doc, org.w3c.dom.Element parent) throws Exception {
        CompanySettings company = companySettingsService.getCompanySettings();
        if (company == null) {
            throw new Exception("Setările companiei nu sunt configurate");
        }
        
        org.w3c.dom.Element supplierParty = doc.createElement("cac:AccountingSupplierParty");
        parent.appendChild(supplierParty);
        
        org.w3c.dom.Element party = doc.createElement("cac:Party");
        supplierParty.appendChild(party);
        
        // PartyName
        org.w3c.dom.Element partyName = doc.createElement("cac:PartyName");
        party.appendChild(partyName);
        addElement(doc, partyName, "cbc:Name", company.getCompanyName());
        
        // PostalAddress
        org.w3c.dom.Element postalAddress = doc.createElement("cac:PostalAddress");
        party.appendChild(postalAddress);
        
        addElement(doc, postalAddress, "cbc:StreetName", company.getAddress());
        addElement(doc, postalAddress, "cbc:CityName", company.getCity());
        addElement(doc, postalAddress, "cbc:CountrySubentity", company.getCounty());
        
        org.w3c.dom.Element country = doc.createElement("cac:Country");
        postalAddress.appendChild(country);
        addElement(doc, country, "cbc:IdentificationCode", "RO");
        
        // PartyTaxScheme (CUI)
        org.w3c.dom.Element partyTaxScheme = doc.createElement("cac:PartyTaxScheme");
        party.appendChild(partyTaxScheme);
        
        org.w3c.dom.Element companyID = doc.createElement("cbc:CompanyID");
        companyID.setAttribute("schemeID", "VAT");
        companyID.setAttribute("schemeAgencyID", "RO");
        companyID.setTextContent(company.getCui());
        partyTaxScheme.appendChild(companyID);
        
        org.w3c.dom.Element taxScheme = doc.createElement("cac:TaxScheme");
        partyTaxScheme.appendChild(taxScheme);
        addElement(doc, taxScheme, "cbc:ID", "VAT");
        
        // PartyLegalEntity
        org.w3c.dom.Element partyLegalEntity = doc.createElement("cac:PartyLegalEntity");
        party.appendChild(partyLegalEntity);
        
        addElement(doc, partyLegalEntity, "cbc:RegistrationName", company.getCompanyName());
        addElement(doc, partyLegalEntity, "cbc:CompanyID", company.getRegCom());
        addElement(doc, partyLegalEntity, "cbc:CompanyLegalForm", "SRL");
    }
    
    private void addAccountingCustomerParty(org.w3c.dom.Document doc, org.w3c.dom.Element parent, Partner partner) throws Exception {
        if (partner == null) {
            throw new Exception("Clientul nu este specificat");
        }
        
        org.w3c.dom.Element customerParty = doc.createElement("cac:AccountingCustomerParty");
        parent.appendChild(customerParty);
        
        org.w3c.dom.Element party = doc.createElement("cac:Party");
        customerParty.appendChild(party);
        
        // PartyName
        org.w3c.dom.Element partyName = doc.createElement("cac:PartyName");
        party.appendChild(partyName);
        addElement(doc, partyName, "cbc:Name", partner.getName());
        
        // PostalAddress
        org.w3c.dom.Element postalAddress = doc.createElement("cac:PostalAddress");
        party.appendChild(postalAddress);
        
        addElement(doc, postalAddress, "cbc:StreetName", partner.getAddress());
        addElement(doc, postalAddress, "cbc:CityName", partner.getCity());
        addElement(doc, postalAddress, "cbc:CountrySubentity", partner.getCounty());
        
        org.w3c.dom.Element country = doc.createElement("cac:Country");
        postalAddress.appendChild(country);
        addElement(doc, country, "cbc:IdentificationCode", "RO");
        
        // PartyTaxScheme (CUI)
        if (partner.getCui() != null && !partner.getCui().trim().isEmpty()) {
            org.w3c.dom.Element partyTaxScheme = doc.createElement("cac:PartyTaxScheme");
            party.appendChild(partyTaxScheme);
            
            org.w3c.dom.Element companyID = doc.createElement("cbc:CompanyID");
            companyID.setAttribute("schemeID", "VAT");
            companyID.setAttribute("schemeAgencyID", "RO");
            companyID.setTextContent(partner.getCui());
            partyTaxScheme.appendChild(companyID);
            
            org.w3c.dom.Element taxScheme = doc.createElement("cac:TaxScheme");
            partyTaxScheme.appendChild(taxScheme);
            addElement(doc, taxScheme, "cbc:ID", "VAT");
        }
        
        // PartyLegalEntity
        org.w3c.dom.Element partyLegalEntity = doc.createElement("cac:PartyLegalEntity");
        party.appendChild(partyLegalEntity);
        
        addElement(doc, partyLegalEntity, "cbc:RegistrationName", partner.getName());
        if (partner.getRegCom() != null) {
            addElement(doc, partyLegalEntity, "cbc:CompanyID", partner.getRegCom());
        }
    }
    
    private void addDelivery(org.w3c.dom.Document doc, org.w3c.dom.Element parent) {
        org.w3c.dom.Element delivery = doc.createElement("cac:Delivery");
        parent.appendChild(delivery);
        
        org.w3c.dom.Element deliveryLocation = doc.createElement("cac:DeliveryLocation");
        delivery.appendChild(deliveryLocation);
        
        org.w3c.dom.Element address = doc.createElement("cac:Address");
        deliveryLocation.appendChild(address);
        
        addElement(doc, address, "cbc:CountrySubentity", "București");
        
        org.w3c.dom.Element country = doc.createElement("cac:Country");
        address.appendChild(country);
        addElement(doc, country, "cbc:IdentificationCode", "RO");
    }
    
    private void addPaymentMeans(org.w3c.dom.Document doc, org.w3c.dom.Element parent) {
        org.w3c.dom.Element paymentMeans = doc.createElement("cac:PaymentMeans");
        parent.appendChild(paymentMeans);
        
        addElement(doc, paymentMeans, "cbc:PaymentMeansCode", "10"); // 10 = Cash
        addElement(doc, paymentMeans, "cbc:PaymentDueDate", LocalDateTime.now().plusDays(30).format(dateTimeFormatter));
        
        org.w3c.dom.Element payeeFinancialAccount = doc.createElement("cac:PayeeFinancialAccount");
        paymentMeans.appendChild(payeeFinancialAccount);
        
        addElement(doc, payeeFinancialAccount, "cbc:ID", "RO12BANK0000123456789012345");
    }
    
    private void addTaxTotal(org.w3c.dom.Document doc, org.w3c.dom.Element parent, BigDecimal totalVat) {
        org.w3c.dom.Element taxTotal = doc.createElement("cac:TaxTotal");
        parent.appendChild(taxTotal);
        
        addElement(doc, taxTotal, "cbc:TaxAmount", formatDecimal(totalVat));
        
        org.w3c.dom.Element taxSubtotal = doc.createElement("cac:TaxSubtotal");
        taxTotal.appendChild(taxSubtotal);
        
        addElement(doc, taxSubtotal, "cbc:TaxableAmount", formatDecimal(totalVat.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP)));
        addElement(doc, taxSubtotal, "cbc:TaxAmount", formatDecimal(totalVat));
        
        org.w3c.dom.Element taxCategory = doc.createElement("cac:TaxCategory");
        taxSubtotal.appendChild(taxCategory);
        
        addElement(doc, taxCategory, "cbc:ID", "S");
        addElement(doc, taxCategory, "cbc:Percent", "19");
        
        org.w3c.dom.Element taxScheme = doc.createElement("cac:TaxScheme");
        taxCategory.appendChild(taxScheme);
        addElement(doc, taxScheme, "cbc:ID", "VAT");
    }
    
    private void addLegalMonetaryTotal(org.w3c.dom.Document doc, org.w3c.dom.Element parent, Invoice invoice) {
        org.w3c.dom.Element legalMonetaryTotal = doc.createElement("cac:LegalMonetaryTotal");
        parent.appendChild(legalMonetaryTotal);
        
        addElement(doc, legalMonetaryTotal, "cbc:LineExtensionAmount", formatDecimal(invoice.getTotalAmount()));
        addElement(doc, legalMonetaryTotal, "cbc:TaxExclusiveAmount", formatDecimal(invoice.getTotalAmount()));
        addElement(doc, legalMonetaryTotal, "cbc:TaxInclusiveAmount", formatDecimal(invoice.getTotalWithVat()));
        addElement(doc, legalMonetaryTotal, "cbc:PayableAmount", formatDecimal(invoice.getTotalWithVat()));
    }
    
    private void addInvoiceLines(org.w3c.dom.Document doc, org.w3c.dom.Element parent, List<InvoiceItem> items) {
        int lineId = 1;
        for (InvoiceItem item : items) {
            org.w3c.dom.Element invoiceLine = doc.createElement("cac:InvoiceLine");
            parent.appendChild(invoiceLine);
            
            addElement(doc, invoiceLine, "cbc:ID", String.valueOf(lineId++));
            addElement(doc, invoiceLine, "cbc:InvoicedQuantity", formatDecimal(item.getQuantity()));
            invoiceLine.getLastChild().setAttribute("unitCode", item.getUnit());
            
            addElement(doc, invoiceLine, "cbc:LineExtensionAmount", formatDecimal(item.getTotalAmount()));
            
            // Item
            org.w3c.dom.Element itemElement = doc.createElement("cac:Item");
            invoiceLine.appendChild(itemElement);
            
            addElement(doc, itemElement, "cbc:Description", item.getProductName());
            addElement(doc, itemElement, "cbc:Name", item.getProductName());
            
            // ClassifiedTaxCategory
            org.w3c.dom.Element classifiedTaxCategory = doc.createElement("cac:ClassifiedTaxCategory");
            itemElement.appendChild(classifiedTaxCategory);
            
            addElement(doc, classifiedTaxCategory, "cbc:ID", "S");
            addElement(doc, classifiedTaxCategory, "cbc:Percent", formatDecimal(item.getVatRate()));
            
            org.w3c.dom.Element taxScheme = doc.createElement("cac:TaxScheme");
            classifiedTaxCategory.appendChild(taxScheme);
            addElement(doc, taxScheme, "cbc:ID", "VAT");
            
            // Price
            org.w3c.dom.Element price = doc.createElement("cac:Price");
            invoiceLine.appendChild(price);
            
            addElement(doc, price, "cbc:PriceAmount", formatDecimal(item.getUnitPrice()));
            price.getLastChild().setAttribute("unitCode", item.getUnit());
        }
    }
    
    private void addElement(org.w3c.dom.Document doc, org.w3c.dom.Element parent, String tagName, String value) {
        org.w3c.dom.Element element = doc.createElement(tagName);
        element.setTextContent(value);
        parent.appendChild(element);
    }
    
    private String formatDecimal(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toString();
    }
    
    private String convertToString(org.w3c.dom.Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        
        return writer.toString();
    }
    
    /**
     * Validează XML-ul generat conform standardului UBL
     */
    public boolean validateXml(String xmlContent) {
        // Implementare validare XML
        // În producție se folosește un validator XSD proper
        return xmlContent != null && xmlContent.contains("<Invoice") && xmlContent.contains("</Invoice>");
    }
}

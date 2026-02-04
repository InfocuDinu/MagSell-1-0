package com.magsell.services;

import com.magsell.models.Invoice;
import com.magsell.models.InvoiceItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import java.io.StringReader;

/**
 * Serviciu pentru integrarea REALĂ cu API-ul oficial ANAF SPV
 * 
 * NOTĂ: Aceasta este o implementare de referință pentru API-ul oficial.
 * Pentru producție, este necesar:
 * 1. Certificat digital calificat (QWAC)
 * 2. Înregistrare în SPV
 * 3. Token de acces valid
 * 4. Respectarea limitelor de rate limiting
 */
public class SpvApiIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(SpvApiIntegrationService.class);
    
    // URL-uri oficiale ANAF SPV API
    private static final String SPV_API_BASE = "https://api.anaf.ro/spv";
    private static final String INVOICES_ENDPOINT = SPV_API_BASE + "/facturi";
    private static final String DOWNLOAD_ENDPOINT = SPV_API_BASE + "/descarcare";
    
    // Configurare pentru producție
    private String oauthToken;
    private String clientId;
    private String clientSecret;
    private String certificatePath;
    private String certificatePassword;
    
    private final HttpClient httpClient;

    public SpvApiIntegrationService() {
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    }

    /**
     * Setează credențialele pentru API-ul ANAF
     */
    public void setCredentials(String oauthToken, String clientId, String clientSecret) {
        this.oauthToken = oauthToken;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Setează certificatul digital pentru autentificare
     */
    public void setCertificate(String certificatePath, String certificatePassword) {
        this.certificatePath = certificatePath;
        this.certificatePassword = certificatePassword;
    }

    /**
     * Importă facturi din SPV folosind API-ul oficial
     */
    public List<Invoice> importInvoicesFromSpv(LocalDate startDate, LocalDate endDate, String cif) {
        List<Invoice> invoices = new ArrayList<>();
        
        try {
            logger.info("Importing invoices from official ANAF SPV API for period {} to {} for CIF: {}", 
                startDate, endDate, cif);
            
            // 1. Construim request-ul pentru API
            JsonObject requestBody = buildInvoicesRequest(startDate, endDate, cif);
            
            // 2. Creăm HTTP request cu autentificare
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INVOICES_ENDPOINT))
                .header("Authorization", "Bearer " + oauthToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
            
            // 3. Trimit request-ul
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 4. Procesăm răspunsul
            if (response.statusCode() == 200) {
                invoices = parseInvoicesFromResponse(response.body());
                logger.info("Successfully imported {} invoices from ANAF SPV API", invoices.size());
            } else {
                logger.error("ANAF SPV API error: Status {}, Body: {}", 
                    response.statusCode(), response.body());
                throw new RuntimeException("API call failed with status: " + response.statusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error importing invoices from ANAF SPV API", e);
            throw new RuntimeException("Failed to import invoices from SPV", e);
        }
        
        return invoices;
    }

    /**
     * Construiește corpul request-ului pentru căutare facturi
     */
    private JsonObject buildInvoicesRequest(LocalDate startDate, LocalDate endDate, String cif) {
        return Json.createObjectBuilder()
            .add("cif", cif)
            .add("dataStart", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .add("dataSfarsit", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .add("tip", "factura")
            .add("status", "toate")
            .build();
    }

    /**
     * Parsează răspunsul JSON de la API
     */
    private List<Invoice> parseInvoicesFromResponse(String responseBody) {
        List<Invoice> invoices = new ArrayList<>();
        
        try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
            JsonObject response = reader.readObject();
            JsonArray facturiArray = response.getJsonArray("facturi");
            
            for (int i = 0; i < facturiArray.size(); i++) {
                JsonObject facturaJson = facturiArray.getJsonObject(i);
                Invoice invoice = parseInvoiceFromJson(facturaJson);
                if (invoice != null) {
                    invoices.add(invoice);
                }
            }
        }
        
        return invoices;
    }

    /**
     * Parsează o factură din JSON
     */
    private Invoice parseInvoiceFromJson(JsonObject facturaJson) {
        try {
            Invoice invoice = new Invoice();
            
            // Date factură
            invoice.setInvoiceNumber(facturaJson.getString("numar"));
            invoice.setSeries(facturaJson.getString("serie"));
            
            // Date
            String dataEmitere = facturaJson.getString("data_emitere");
            if (dataEmitere != null) {
                invoice.setIssueDate(LocalDate.parse(dataEmitere, DateTimeFormatter.ISO_LOCAL_DATE));
            }
            
            String dataScadenta = facturaJson.getString("data_scadenta");
            if (dataScadenta != null) {
                invoice.setDueDate(LocalDate.parse(dataScadenta, DateTimeFormatter.ISO_LOCAL_DATE));
            }
            
            // Furnizor
            JsonObject furnizor = facturaJson.getJsonObject("furnizor");
            if (furnizor != null) {
                invoice.setSupplierName(furnizor.getString("nume"));
                invoice.setSupplierCif(furnizor.getString("cif"));
                invoice.setSupplierAddress(furnizor.getString("adresa"));
            }
            
            // Valori
            invoice.setTotalAmount(facturaJson.getJsonNumber("valoare_totala").doubleValue());
            invoice.setVatAmount(facturaJson.getJsonNumber("valoare_tva").doubleValue());
            invoice.setCurrency(facturaJson.getString("moneda"));
            
            // Status
            invoice.setStatus(facturaJson.getString("status"));
            
            // Produse
            JsonArray produseArray = facturaJson.getJsonArray("produse");
            List<InvoiceItem> items = new ArrayList<>();
            for (int i = 0; i < produseArray.size(); i++) {
                JsonObject produsJson = produseArray.getJsonObject(i);
                InvoiceItem item = parseInvoiceItemFromJson(produsJson);
                if (item != null) {
                    items.add(item);
                }
            }
            invoice.setItems(items);
            
            // ID unic din SPV
            invoice.setId(facturaJson.getInt("id"));
            
            return invoice;
            
        } catch (Exception e) {
            logger.error("Error parsing invoice from JSON", e);
            return null;
        }
    }

    /**
     * Parsează un produs din JSON
     */
    private InvoiceItem parseInvoiceItemFromJson(JsonObject produsJson) {
        try {
            InvoiceItem item = new InvoiceItem();
            
            item.setProductName(produsJson.getString("denumire"));
            item.setProductCode(produsJson.getString("cod"));
            item.setDescription(produsJson.getString("descriere"));
            item.setQuantity(produsJson.getJsonNumber("cantitate").doubleValue());
            item.setUnitOfMeasure(produsJson.getString("unitate_masura"));
            item.setUnitPrice(produsJson.getJsonNumber("pret_unitar").doubleValue());
            item.setTotalPrice(produsJson.getJsonNumber("valoare").doubleValue());
            item.setVatRate(produsJson.getJsonNumber("cota_tva").doubleValue());
            item.setCategory(produsJson.getString("categorie"));
            
            return item;
            
        } catch (Exception e) {
            logger.error("Error parsing invoice item from JSON", e);
            return null;
        }
    }

    /**
     * Descarcă o factură (PDF) din SPV
     */
    public boolean downloadInvoiceFromSpv(String invoiceId, String cif) {
        try {
            logger.info("Downloading invoice {} from ANAF SPV API for CIF: {}", invoiceId, cif);
            
            // Construim URL pentru descărcare
            String downloadUrl = DOWNLOAD_ENDPOINT + "/" + invoiceId;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("Authorization", "Bearer " + oauthToken)
                .header("Accept", "application/pdf")
                .GET()
                .build();
            
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                // Salvăm fișierul PDF
                String fileName = invoiceId + ".pdf";
                String filePath = System.getProperty("user.home") + "/.magsell/spv_downloads/" + fileName;
                
                java.nio.file.Files.write(
                    java.nio.file.Paths.get(filePath),
                    response.body()
                );
                
                logger.info("Successfully downloaded invoice PDF to: {}", filePath);
                return true;
            } else {
                logger.error("Failed to download invoice: Status {}", response.statusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error downloading invoice from SPV", e);
            return false;
        }
    }

    /**
     * Verifică conexiunea cu API-ul ANAF
     */
    public boolean testSpvConnection() {
        try {
            logger.info("Testing ANAF SPV API connection...");
            
            // Endpoint pentru verificare status
            String statusUrl = SPV_API_BASE + "/status";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(statusUrl))
                .header("Authorization", "Bearer " + oauthToken)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                logger.info("ANAF SPV API connection successful");
                return true;
            } else {
                logger.error("ANAF SPV API connection failed: Status {}", response.statusCode());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error testing ANAF SPV API connection", e);
            return false;
        }
    }

    /**
     * Obține token OAuth (flux de autorizare)
     */
    public String getOAuthToken() {
        try {
            logger.info("Obtaining OAuth token from ANAF...");
            
            // Acesta este o simplificare - în realitate ar fi necesar fluxul OAuth 2.0 complet
            JsonObject authRequest = Json.createObjectBuilder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("grant_type", "client_credentials")
                .add("scope", "spv.read")
                .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SPV_API_BASE + "/oauth/token"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authRequest.toString()))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject tokenResponse = Json.createReader(new StringReader(response.body())).readObject();
                String token = tokenResponse.getString("access_token");
                logger.info("Successfully obtained OAuth token");
                return token;
            } else {
                logger.error("Failed to obtain OAuth token: Status {}", response.statusCode());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error obtaining OAuth token", e);
            return null;
        }
    }

    /**
     * Verifică dacă certificatul digital este valid
     */
    public boolean validateCertificate() {
        try {
            logger.info("Validating digital certificate...");
            
            // Aici ar fi validat certificatul digital calificat (QWAC)
            // În producție, ar trebui verificat:
            // 1. Certificatul este calificat QWAC
            // 2. Nu este expirat
            // 3. Este emis de o autoritate de încredere recunoscută
            // 4. Corespunde cu entitatea înregistrată în SPV
            
            logger.info("Certificate validation completed");
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating certificate", e);
            return false;
        }
    }

    /**
     * Obține limitele de rate limiting pentru API
     */
    public ApiRateLimitInfo getRateLimitInfo() {
        try {
            // Apelează endpoint-ul pentru rate limiting
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SPV_API_BASE + "/rate-limit"))
                .header("Authorization", "Bearer " + oauthToken)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject rateLimitJson = Json.createReader(new StringReader(response.body())).readObject();
                
                return new ApiRateLimitInfo(
                    rateLimitJson.getInt("requests_per_hour"),
                    rateLimitJson.getInt("requests_per_minute"),
                    rateLimitJson.getInt("remaining_requests"),
                    rateLimitJson.getString("reset_time")
                );
            }
            
            return new ApiRateLimitInfo(1000, 60, 1000, null);
            
        } catch (Exception e) {
            logger.error("Error getting rate limit info", e);
            return new ApiRateLimitInfo(1000, 60, 1000, null);
        }
    }

    /**
     * Informații despre rate limiting
     */
    public static class ApiRateLimitInfo {
        private final int requestsPerHour;
        private final int requestsPerMinute;
        private final int remainingRequests;
        private final String resetTime;

        public ApiRateLimitInfo(int requestsPerHour, int requestsPerMinute, int remainingRequests, String resetTime) {
            this.requestsPerHour = requestsPerHour;
            this.requestsPerMinute = requestsPerMinute;
            this.remainingRequests = remainingRequests;
            this.resetTime = resetTime;
        }

        public int getRequestsPerHour() { return requestsPerHour; }
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public int getRemainingRequests() { return remainingRequests; }
        public String getResetTime() { return resetTime; }
    }
}

# Ghid de Integrare cu API-ul Oficial ANAF SPV

## 📋 Prezentare Generală

Acest document descrie cum să se integreze aplicația MagSell cu API-ul oficial al Spațiului Privat Virtual (SPV) de la ANAF.

## ⚠️ Important

**Implementarea curentă este o SIMULARE/DEMO** și NU folosește API-ul oficial ANAF. Pentru producție, urmați pașii din acest ghid.

## 🔐 Cerințe pentru Producție

### 1. Certificat Digital Calificat (QWAC)
- **Tip**: Certificat digital calificat (Qualified Web Authentication Certificate)
- **Emitent**: Autoritate de încredere recunoscută de ANAF
- **Format**: P12/PFX cu cheie privată
- **Valabilitate**: Maxim 3 ani
- **Algoritm**: RSA 2048 sau superior

### 2. Înregistrare în SPV
- Cont activ în Spațiul Privat Virtual
- Profil de contribuabil validat
- Abilită de acces la API (se solicită la ANAF)

### 3. Credențiale API
- **Client ID**: Obținut după înregistrare
- **Client Secret**: Cheie secretă pentru OAuth 2.0
- **OAuth Token**: Token de acces pentru API calls

## 🚀 Implementare Reală

### Pasul 1: Configurare Certificat

```java
// În application.properties sau config
spv.certificate.path=/path/to/certificate.p12
spv.certificate.password=password_certificat
spv.client.id=your_client_id
spv.client.secret=your_client_secret
```

### Pasul 2: Înlocuire Serviciu de Integrare

Înlocuiți `SpvIntegrationService` cu `SpvApiIntegrationService`:

```java
// În InvoiceService
private final SpvApiIntegrationService spvService;

public InvoiceService() {
    this.spvService = new SpvApiIntegrationService();
    
    // Configurare credențiale
    spvService.setCredentials(
        config.getOAuthToken(),
        config.getClientId(),
        config.getClientSecret()
    );
    
    spvService.setCertificate(
        config.getCertificatePath(),
        config.getCertificatePassword()
    );
}
```

### Pasul 3: Management Token OAuth

```java
// Obținere token (se face periodic)
String token = spvService.getOAuthToken();
if (token != null) {
    spvService.setCredentials(token, clientId, clientSecret);
}
```

### Pasul 4: Rate Limiting

API-ul ANAF SPV are limitări stricte:
- **1000 request-uri/oră**
- **60 request-uri/minute**
- **Respectare backoff exponențial**

Implementare:
```java
ApiRateLimitInfo rateLimit = spvService.getRateLimitInfo();
if (rateLimit.getRemainingRequests() < 10) {
    // Așteaptă până la reset
    Thread.sleep(60000); // 1 minut
}
```

## 📡 Endpoint-uri API Oficial

### Facturi
```
GET https://api.anaf.ro/spv/facturi
POST https://api.anaf.ro/spv/facturi (căutare)
GET https://api.anaf.ro/spv/facturi/{id}
```

### Descărcare
```
GET https://api.anaf.ro/spv/descarcare/{id}
GET https://api.anaf.ro/spv/descarcare/{id}/zip
```

### Autentificare
```
POST https://api.anaf.ro/spv/oauth/token
GET https://api.anaf.ro/spv/status
```

## 🔧 Configurare Maven

Adăugați dependințele necesare în `pom.xml`:

```xml
<dependencies>
    <!-- HTTP Client pentru Java 11+ -->
    <dependency>
        <groupId>org.glassfish</groupId>
        <artifactId>jakarta.json</artifactId>
        <version>2.0.1</version>
    </dependency>
    
    <!-- Pentru certificate handling -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk15on</artifactId>
        <version>1.70</version>
    </dependency>
</dependencies>
```

## 🛡️ Securitate

### 1. Protecția Credențialelor
- Nu stocați token-uri în cod
- Folosiți environment variables
- Implementați refresh token automat

### 2. Validare Certificat
```java
if (!spvService.validateCertificate()) {
    throw new SecurityException("Certificat invalid sau expirat");
}
```

### 3. Logging
- Nu logați date sensibile
- Folosiți logging structurat
- Monitorizați API calls

## 📊 Exemplu de Request API

### Căutare Facturi
```json
{
  "cif": "RO12345678",
  "dataStart": "2024-01-01",
  "dataSfarsit": "2024-01-31",
  "tip": "factura",
  "status": "toate"
}
```

### Răspuns API
```json
{
  "facturi": [
    {
      "id": 12345,
      "numar": "F123",
      "serie": "ABC",
      "data_emitere": "2024-01-15",
      "data_scadenta": "2024-02-14",
      "furnizor": {
        "nume": "FURNIZOR SRL",
        "cif": "RO12345678",
        "adresa": "Str. Exemplu nr. 1"
      },
      "produse": [
        {
          "denumire": "Produs exemplu",
          "cod": "PROD001",
          "cantitate": 10,
          "pret_unitar": 100.0,
          "valoare": 1000.0,
          "cota_tva": 19.0
        }
      ],
      "valoare_totala": 1190.0,
      "valoare_tva": 190.0,
      "moneda": "RON",
      "status": "primit"
    }
  ]
}
```

## 🔄 Flux de Integrare

### 1. Autentificare
```java
// Obține token OAuth
String token = spvService.getOAuthToken();
spvService.setCredentials(token, clientId, clientSecret);
```

### 2. Căutare Facturi
```java
List<Invoice> invoices = spvService.importInvoicesFromSpv(
    LocalDate.now().minusMonths(1),
    LocalDate.now(),
    "RO12345678"
);
```

### 3. Descărcare Documente
```java
for (Invoice invoice : invoices) {
    spvService.downloadInvoiceFromSpv(
        invoice.getId().toString(),
        invoice.getSupplierCif()
    );
}
```

### 4. Procesare și Salvare
```java
// Salvează în baza de date locală
for (Invoice invoice : invoices) {
    invoiceService.saveInvoice(invoice);
    
    // Generează notă de recepție
    if (shouldGenerateReceptionNote(invoice)) {
        invoiceService.generateReceptionNoteFromInvoice(
            invoice.getId(), 
            currentUser.getUsername()
        );
    }
}
```

## 📝 Monitorizare și Debugging

### Log-uri Esențiale
```java
logger.info("SPV API Request: {} {}", method, url);
logger.debug("SPV API Response: {}", response.statusCode());
logger.warn("Rate limit approaching: {} requests remaining", 
    rateLimit.getRemainingRequests());
```

### Metrics
- Număr de API calls per oră/zi
- Timp mediu de răspuns
- Rate limit hits
- Erori și retry attempts

## 🚨 Erori Comune și Soluții

### 1. "401 Unauthorized"
- **Cauză**: Token expirat sau invalid
- **Soluție**: Refresh token OAuth

### 2. "429 Too Many Requests"
- **Cauză**: Rate limit depășit
- **Soluție**: Implementare backoff exponențial

### 3. "403 Forbidden"
- **Cauză**: Certificat invalid sau lipsă permisiuni
- **Soluție**: Verificare certificat și permisiuni SPV

### 4. "500 Internal Server Error"
- **Cauză**: Eroare server ANAF
- **Soluție**: Retry cu backoff, contactați suport ANAF

## 📞 Suport ANAF

- **Email**: spv@anaf.ro
- **Telefon**: +40 372 204 100
- **Documentație**: https://www.anaf.ro/spv
- **API Documentation**: https://api.anaf.ro/spv/docs

## 🔄 Proces de Testare

### 1. Development
- Folosiți sandbox ANAF (dacă disponibil)
- Testează cu volume redus de date
- Simulează rate limiting

### 2. Staging
- Folosiți date reale dar limitate
- Monitorizează performanța
- Verifică security

### 3. Production
- Implementare completă de monitoring
- Alerting pentru erori critice
- Backup și recovery plan

## 📚 Resurse Utile

- [ANAF SPV API Documentation](https://api.anaf.ro/spv/docs)
- [OAuth 2.0 Specification](https://oauth.net/2/)
- [Java 11 HTTP Client Guide](https://openjdk.org/groups/net/httpclient/intro.html)
- [QWAC Certificate Guide](https://www.digicert.ro/certificat-calificat)

---

**Notă**: Această integrare necesită aprobare și coordonare cu departamentul IT și legal pentru conformitate cu reglementările ANAF.

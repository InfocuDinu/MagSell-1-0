# MagSell - Instrucțiuni pentru Agenți AI

## Descriere Proiect

MagSell este o aplicație desktop Java pentru gestionarea vânzărilor și inventarului unei patiserii. Folosește JavaFX pentru interfață grafică și SQLite pentru persistența datelor offline.

## Arhitectură Generală

### 1. **Structura în Straturi (Layered Architecture)**

```
UI Layer (JavaFX - Controllers + FXML)
    ↓
Service Layer (ProductService, SaleService - Logică de business)
    ↓
Database Layer (DatabaseService - Operații CRUD)
    ↓
SQLite Database (Persistență locală)
```

... (truncated) ...

# MagSell - Patisserie Management System

Aplicație desktop Java pentru gestionarea vânzărilor și inventarului unei patiserii.

## Caracteristici

- **Gestionare Produse**: Adăugare, editare, ștergere articole de patiserie
- **Înregistrare Vânzări**: Procesare rapida a comenzilor clienților
- **Gestionare Clienți**: Înregistrare și urmărire clienți frecvenți
- **Inventar**: Monitorizare stocuri în timp real
- **Rapoarte**: Analize vânzări, rentabilitate pe produs
- **Bază de date offline**: SQLite - funcționează fără internet

## Tehnologii

- **Java 17+**
- **JavaFX** - Interfață grafică
- **Maven** - Gestionare dependințe și build
- **SQLite** - Bază de date locală

## Build și Rulare

### Build
```
mvn clean compile
```

### Rulare
```
mvn javafx:run
```

### Empaquetare
```
mvn clean package
```

## Structura Proiect

```
src/
├── main/
│   ├── java/com/magsell/
│   │   ├── App.java                 # Punct de intrare
│   │   ├── models/                  # Clase de model (Product, Sale, Customer)
│   │   ├── services/                # Logică de business
│   │   ├── database/                # Gestionare bază de date
│   │   └── ui/controllers/          # Controlere JavaFX
│   └── resources/
│       └── com/magsell/ui/
│           ├── fxml/                # Fișiere FXML pentru UI
│           └── css/                 # Styluri CSS
```

## Bază de Date

Schema SQLite este inițializată automat la prima pornire. Baza de date se creează în directorul home al utilizatorului:
- Windows: `%USERPROFILE%\.magsell\magsell.db`

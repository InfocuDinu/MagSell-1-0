-- MagSell ERP - Schema Bază de Date (SmartBill Compatible)
-- Database: SQLite
-- Version: 2.0
-- Author: Senior Software Architect

-- Drop existing tables if they exist (for development)
DROP TABLE IF EXISTS inventory_movements;
DROP TABLE IF EXISTS invoice_items;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS warehouses;
DROP TABLE IF EXISTS company_settings;
DROP TABLE IF EXISTS payment_methods;
DROP TABLE IF EXISTS document_series;

-- Enhanced Products table
DROP TABLE IF EXISTS products;
CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    barcode VARCHAR(50),
    unit VARCHAR(20) DEFAULT 'buc',
    purchase_price DECIMAL(15,4) DEFAULT 0,
    sale_price DECIMAL(15,4) DEFAULT 0,
    vat_rate DECIMAL(5,2) DEFAULT 19.0,
    quantity DECIMAL(15,4) DEFAULT 0,
    min_quantity DECIMAL(15,4) DEFAULT 0,
    max_quantity DECIMAL(15,4) DEFAULT 0,
    warehouse_id INTEGER DEFAULT 1,
    category_id INTEGER,
    supplier_id INTEGER,
    is_active BOOLEAN DEFAULT 1,
    has_serial_numbers BOOLEAN DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Warehouses (Multi-Gestiune)
CREATE TABLE warehouses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    is_active BOOLEAN DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Insert default warehouse
INSERT INTO warehouses (code, name) VALUES ('G01', 'Gestiune Principală');

-- Suppliers (Furnizori)
CREATE TABLE suppliers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    cui VARCHAR(20) UNIQUE,
    reg_com VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    county VARCHAR(100),
    country VARCHAR(100) DEFAULT 'România',
    phone VARCHAR(50),
    email VARCHAR(100),
    bank_account VARCHAR(50),
    bank_name VARCHAR(100),
    vat_payer BOOLEAN DEFAULT 1,
    is_active BOOLEAN DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Enhanced Partners table (Clienți)
CREATE TABLE partners (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    cui VARCHAR(20) UNIQUE,
    reg_com VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    county VARCHAR(100),
    country VARCHAR(100) DEFAULT 'România',
    phone VARCHAR(50),
    email VARCHAR(100),
    bank_account VARCHAR(50),
    bank_name VARCHAR(100),
    vat_payer BOOLEAN DEFAULT 1,
    is_active BOOLEAN DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Document Series (Serii documente)
CREATE TABLE document_series (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(20) NOT NULL, -- 'FAC', 'NIR', 'AVI', 'CHI'
    prefix VARCHAR(10) NOT NULL,
    current_number INTEGER DEFAULT 1,
    year INTEGER,
    warehouse_id INTEGER,
    UNIQUE(type, prefix, year, warehouse_id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Insert default series
INSERT INTO document_series (type, prefix, year) VALUES 
('FAC', 'FAC', 2026),
('NIR', 'NIR', 2026),
('AVI', 'AVI', 2026),
('CHI', 'CHI', 2026);

-- Payment Methods
CREATE TABLE payment_methods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT 1
);

-- Insert default payment methods
INSERT INTO payment_methods (name, description) VALUES 
('Numerar', 'Plată numerar'),
('Card', 'Plată cu cardul'),
('OP', 'Ordin de plată'),
('Bilet la ordin', 'Bilet la ordin');

-- Company Settings
CREATE TABLE company_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company_name VARCHAR(200) NOT NULL,
    cui VARCHAR(20) UNIQUE NOT NULL,
    reg_com VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    county VARCHAR(100),
    country VARCHAR(100) DEFAULT 'România',
    phone VARCHAR(50),
    email VARCHAR(100),
    bank_account VARCHAR(50),
    bank_name VARCHAR(100),
    capital_social DECIMAL(15,2),
    vat_payer BOOLEAN DEFAULT 1,
    logo_path VARCHAR(500),
    is_active BOOLEAN DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Invoices (Facturi)
CREATE TABLE invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    series VARCHAR(10) NOT NULL,
    number INTEGER NOT NULL,
    partner_id INTEGER NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE,
    payment_method_id INTEGER,
    total_amount DECIMAL(15,4) NOT NULL,
    total_vat DECIMAL(15,4) NOT NULL,
    total_with_vat DECIMAL(15,4) NOT NULL,
    status VARCHAR(20) DEFAULT 'draft', -- draft, issued, paid, cancelled
    notes TEXT,
    is_e_factura BOOLEAN DEFAULT 0,
    e_factura_status VARCHAR(50), -- pending, submitted, accepted, rejected
    e_factura_xml TEXT,
    created_by INTEGER,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (partner_id) REFERENCES partners(id),
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE(series, number)
);

-- Invoice Items (Produse pe factură)
CREATE TABLE invoice_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity DECIMAL(15,4) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    unit_price DECIMAL(15,4) NOT NULL,
    discount_percent DECIMAL(5,2) DEFAULT 0,
    vat_rate DECIMAL(5,2) DEFAULT 19.0,
    total_amount DECIMAL(15,4) NOT NULL,
    total_vat DECIMAL(15,4) NOT NULL,
    total_with_vat DECIMAL(15,4) NOT NULL,
    warehouse_id INTEGER DEFAULT 1,
    batch_number VARCHAR(50),
    expiry_date DATE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

-- Inventory Movements (Mișcări stoc)
CREATE TABLE inventory_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    warehouse_id INTEGER DEFAULT 1,
    movement_type VARCHAR(20) NOT NULL, -- 'IN', 'OUT', 'TRANSFER', 'ADJUSTMENT'
    document_type VARCHAR(20) NOT NULL, -- 'NIR', 'FAC', 'AVI', 'ADJ'
    document_id INTEGER,
    quantity DECIMAL(15,4) NOT NULL,
    unit_price DECIMAL(15,4),
    batch_number VARCHAR(50),
    expiry_date DATE,
    notes TEXT,
    created_by INTEGER,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Create indexes for performance
CREATE INDEX idx_products_code ON products(code);
CREATE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_products_warehouse ON products(warehouse_id);
CREATE INDEX idx_partners_cui ON partners(cui);
CREATE INDEX idx_suppliers_cui ON suppliers(cui);
CREATE INDEX idx_invoices_partner ON invoices(partner_id);
CREATE INDEX idx_invoices_date ON invoices(issue_date);
CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_items_product ON invoice_items(product_id);
CREATE INDEX idx_inventory_movements_product ON inventory_movements(product_id);
CREATE INDEX idx_inventory_movements_date ON inventory_movements(created_at);

-- Create triggers for automatic updates
CREATE TRIGGER update_products_timestamp 
AFTER UPDATE ON products
BEGIN
    UPDATE products SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

CREATE TRIGGER update_partners_timestamp 
AFTER UPDATE ON partners
BEGIN
    UPDATE partners SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

CREATE TRIGGER update_suppliers_timestamp 
AFTER UPDATE ON suppliers
BEGIN
    UPDATE suppliers SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

CREATE TRIGGER update_invoices_timestamp 
AFTER UPDATE ON invoices
BEGIN
    UPDATE invoices SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- Insert default company settings
INSERT INTO company_settings (
    company_name, cui, reg_com, address, city, county, phone, email, 
    bank_account, bank_name, capital_social
) VALUES (
    'MagSell SRL', 'RO12345678', 'J40/1234/2026', 
    'Str. Principală Nr. 1', 'București', 'București', 
    '0211234567', 'office@magsell.ro', 
    'RO12BANK0000123456789012345', 'BANK SA', 100.00
);

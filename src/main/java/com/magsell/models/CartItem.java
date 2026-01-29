package com.magsell.models;

import java.math.BigDecimal;

/**
 * Model pentru un item din coșul de cumpărături (POS).
 */
public class CartItem {
    private Product product;
    private int quantity;
    private BigDecimal subtotal;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        if (product != null) {
            this.subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void incrementQuantity() {
        this.quantity++;
        this.subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public void decrementQuantity() {
        if (quantity > 1) {
            this.quantity--;
            this.subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }
}

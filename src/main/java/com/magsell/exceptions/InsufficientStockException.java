package com.magsell.exceptions;

/**
 * Excepție aruncată atunci când stocul disponibil este insuficient pentru producție
 */
public class InsufficientStockException extends Exception {
    
    public InsufficientStockException(String message) {
        super(message);
    }
    
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}

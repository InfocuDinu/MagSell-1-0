package com.magsell.models;

/**
 * Model pentru ingredientele dintr-o rețetă de producție
 */
public class RecipeIngredient {
    private int id;
    private int recipeId;
    private int productId; // Materie primă
    private String productName; // Denumire materie primă (pentru afișare)
    private double quantity; // Cantitatea necesară pentru producție
    private String unitOfMeasure;
    
    public RecipeIngredient() {
        this.unitOfMeasure = "buc";
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getRecipeId() {
        return recipeId;
    }
    
    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }
    
    public int getProductId() {
        return productId;
    }
    
    public void setProductId(int productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
    
    @Override
    public String toString() {
        return productName != null ? productName + " (" + quantity + " " + unitOfMeasure + ")" : "Ingredient #" + id;
    }
}

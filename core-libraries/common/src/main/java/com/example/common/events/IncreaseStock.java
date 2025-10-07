package com.example.common.events;

/**
 * Event representing a request to increase product stock.
 * Contains seller, product, and quantity information.
 */
public class IncreaseStock {

    private int sellerId;
    private int productId;
    private int quantity;

    public IncreaseStock() {
    }

    /**
     * Constructor with all parameters.
     *
     * @param sellerId  seller ID
     * @param productId product ID
     * @param quantity  quantity to increase
     */
    public IncreaseStock(int sellerId, int productId, int quantity) {
        this.sellerId = sellerId;
        this.productId = productId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}

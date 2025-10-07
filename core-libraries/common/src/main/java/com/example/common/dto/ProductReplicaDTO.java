package com.example.common.dto;

/**
 * Data Transfer Object representing a product replica.
 * Contains seller, product, name, price, and version information.
 */
public class ProductReplicaDTO {

    private int sellerId;
    private int productId;
    private String name;
    private float price;
    private String version;

    public ProductReplicaDTO() {
    }

    /**
     * Constructor with all parameters.
     *
     * @param sellerId  seller ID
     * @param productId product ID
     * @param name      product name
     * @param price     product price
     * @param version   product version
     */
    public ProductReplicaDTO(int sellerId, int productId, String name, float price, String version) {
        this.sellerId = sellerId;
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.version = version;
    }

    // Getter and Setter
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}

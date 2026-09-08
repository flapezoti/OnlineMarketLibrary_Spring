package com.example.common.events;

/**
 * A product has been disabled at the Product microservice. Stock must stop
 * allowing this product to be reserved.
 */
public class ProductDelete {

    private int sellerId;
    private int productId;
    private String version;
    private String instanceId;

    public ProductDelete() {
    }

    public ProductDelete(int sellerId, int productId, String version, String instanceId) {
        this.sellerId = sellerId;
        this.productId = productId;
        this.version = version;
        this.instanceId = instanceId;
    }

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}

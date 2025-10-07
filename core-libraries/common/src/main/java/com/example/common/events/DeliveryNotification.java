package com.example.common.events;

import java.time.LocalDateTime;
import com.example.common.entities.PackageStatus;

/**
 * Event representing a delivery notification.
 * Contains customer, order, package, product, status, and delivery details.
 */
public class DeliveryNotification {

    private int customerId;
    private int orderId;
    private int packageId;
    private int sellerId;
    private int productId;
    private String productName;
    private PackageStatus status;
    private LocalDateTime deliveryDate;
    private String instanceId;

    // Default constructor
    public DeliveryNotification() {
    }

      /**
     * Constructor with all parameters.
     *
     * @param customerId customer ID
     * @param orderId order ID
     * @param packageId package ID
     * @param sellerId seller ID
     * @param productId product ID
     * @param productName product name
     * @param status package status
     * @param deliveryDate delivery date
     * @param instanceId instance ID
     */
    public DeliveryNotification(int customerId, int orderId, int packageId, int sellerId, int productId,
            String productName, PackageStatus status, LocalDateTime deliveryDate, String instanceId) {
        this.customerId = customerId;
        this.orderId = orderId;
        this.packageId = packageId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.productName = productName;
        this.status = status;
        this.deliveryDate = deliveryDate;
        this.instanceId = instanceId;
    }

    // Getters and setters
    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public PackageStatus getStatus() {
        return status;
    }

    public void setStatus(PackageStatus status) {
        this.status = status;
    }

    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDateTime deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}

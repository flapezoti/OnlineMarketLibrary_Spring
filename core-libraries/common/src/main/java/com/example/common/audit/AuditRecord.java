package com.example.common.audit;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * A durable audit-trail entry: a snapshot of the records tied to an order at the
 * moment a key event (a concluded shipment, or a payment attempt) occurred.
 */
public class AuditRecord {

    /** Owning microservice: "order" | "seller" | "shipment" | "payment". */
    private String service;
    /** What produced the entry, e.g. "SHIPMENT_CONCLUDED", "PAYMENT_FAILED", "PAYMENT_PROCESSED". */
    private String trigger;
    private LocalDateTime loggedAt;
    private int customerId;
    private int orderId;
    /** The associated rows, keyed by relation name (e.g. "order", "orderItems", "orderHistory"). */
    private Map<String, Object> payload = new HashMap<>();

    public AuditRecord() {
    }

    public AuditRecord(String service, String trigger, int customerId, int orderId) {
        this.service = service;
        this.trigger = trigger;
        this.customerId = customerId;
        this.orderId = orderId;
        this.loggedAt = LocalDateTime.now();
    }

    /** Adds one associated relation to the payload and returns {@code this} for chaining. */
    public AuditRecord with(String relation, Object rows) {
        this.payload.put(relation, rows);
        return this;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

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

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}

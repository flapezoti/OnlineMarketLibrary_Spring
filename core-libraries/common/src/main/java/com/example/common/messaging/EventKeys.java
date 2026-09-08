package com.example.common.messaging;

/**
 * Builds the partition key attached to an outbound event so that all events about
 * the same entity are routed to one Kafka partition and processed in order.
 */
public final class EventKeys {

    private EventKeys() {
    }

    /** Key for events in one order's lifecycle (invoice, payment, shipment, delivery). */
    public static String order(int customerId, int orderId) {
        return customerId + "-" + orderId;
    }

    /** Key for events about one product (price update, product update, product delete). */
    public static String product(int sellerId, int productId) {
        return sellerId + "-" + productId;
    }

    /** Key for pre-order checkout events, scoped to the customer. */
    public static String customer(int customerId) {
        return Integer.toString(customerId);
    }
}

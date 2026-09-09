package com.example.order.repository;

/**
 * Tracks which checkouts the Order microservice has already processed, so that a
 * redelivered or repeated checkout event does not create a second order.
 */
public interface IProcessedCheckoutRepository {

    /**
     * Atomically records that the checkout identified by {@code checkoutKey} has been processed.
     *
     * @return {@code true} if this call was the first to record it; {@code false} if it was
     *         already present (i.e. this is a duplicate / redelivered checkout).
     */
    boolean markProcessed(String checkoutKey);

    /** Removes all processed-checkout markers (cleanup / reset). */
    void deleteAll();
}

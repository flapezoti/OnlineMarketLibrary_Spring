package com.example.payment.service;

import com.example.common.integration.PaymentIntent;
import com.example.common.integration.PaymentIntentCreateOptions;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Core mock implementation of {@link IExternalProvider}.
 *
 * <p>
 * This class simulates an external payment gateway (e.g., Stripe or PayPal)
 * for testing and local development. It does not make any real API calls.
 * Instead, it randomly marks some payments as failed based on a configurable
 * failure percentage.
 * </p>
 *
 * <p>
 * Each payment request is deduplicated using the provided
 * {@code idempotencyKey}, ensuring deterministic behavior across retries.
 * </p>
 *
 * <p>
 * <b>Usage:</b>
 * This class is primarily used by the core library or test environments
 * to emulate payment provider responses without external dependencies.
 * </p>
 */
public class ExternalProviderProxyCore implements IExternalProvider {

    private final int failPercentage;
    private final Map<String, PaymentIntent> db = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public ExternalProviderProxyCore(int failPercentage) {
        this.failPercentage = failPercentage;
    }

    /**
     * Simulates a payment intent creation request.
     * 
     * <p>
     * Each request generates a unique {@link PaymentIntent} unless the same
     * {@code idempotencyKey} has already been used, in which case the existing
     * intent is returned.
     * </p>
     *
     * @param options the payment request options, including customer, amount, and
     *                key
     * @return a simulated {@link PaymentIntent} object with randomized success or
     *         failure
     */
    @Override
    public PaymentIntent create(PaymentIntentCreateOptions options) {
        if (db.containsKey(options.getIdempotencyKey())) {
            return db.get(options.getIdempotencyKey());
        }

        String status = "succeeded";
        if (random.nextInt(100) < failPercentage) {
            status = "canceled";
        }

        PaymentIntent intent = new PaymentIntent();
        intent.setId(UUID.randomUUID().toString());
        intent.setAmount(options.getAmount());
        intent.setCustomer(options.getCustomer());
        intent.setStatus(status);
        intent.setCurrency(options.getCurrency().toString());
        intent.setCreated((int) System.currentTimeMillis());

        db.putIfAbsent(options.getIdempotencyKey(), intent);
        return intent;
    }
}

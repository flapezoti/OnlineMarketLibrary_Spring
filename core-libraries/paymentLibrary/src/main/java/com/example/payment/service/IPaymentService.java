package com.example.payment.service;

import com.example.common.events.InvoiceIssued;

/**
 * Repository abstraction for persisting and retrieving payment records.
 *
 * <p>
 * This interface defines how payment entities are stored and queried.
 * The {@link com.example.payment.service.PaymentServiceCore} relies on it
 * to persist successful and failed payment transactions.
 * </p>
 *
 * <p>
 * Developers integrating this library should provide an implementation that
 * maps these methods to their data layer (e.g., JPA, Redis, or in-memory
 * storage).
 * See the reference implementation in
 * <code>KafkaSpringbootImplementation/paymentService</code>.
 * </p>
 */
public interface IPaymentService {

    void processPayment(InvoiceIssued paymentRequest);

    void cleanup();

    void processPoisonPayment(InvoiceIssued invoice);
}

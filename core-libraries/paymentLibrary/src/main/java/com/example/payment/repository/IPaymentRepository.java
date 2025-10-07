package com.example.payment.repository;

import com.example.payment.model.OrderPayment;

import java.util.List;

/**
 * Repository interface for persisting and querying {@link OrderPayment}
 * entities.
 *
 * <p>
 * This interface defines the data-access abstraction for payment records
 * within the core library. Platform-specific modules (e.g. Spring Boot, Dapr,
 * Orleans)
 * must provide their own concrete implementations.
 * </p>
 *
 * <p>
 * <b>Implementation note:</b>
 * A sample implementation is provided under
 * <code>KafkaSpringbootImplementation/paymentService</code>.
 * </p>
 */
public interface IPaymentRepository {
    /**
     * Retrieves all payments associated with a given customer and order.
     */
    List<OrderPayment> findAllByCustomerIdAndOrderId(int customerId, int orderId);

    /**
     * Persists a single payment record.
     */
    void save(OrderPayment payment);

    /**
     * Persists multiple payment records in batch.
     */
    void saveAll(List<OrderPayment> payments);

    /**
     * Deletes all payment records from the underlying storage.
     */
    void deleteAll();

}
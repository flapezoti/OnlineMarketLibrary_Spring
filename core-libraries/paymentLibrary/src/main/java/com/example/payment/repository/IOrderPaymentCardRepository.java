package com.example.payment.repository;

import com.example.payment.model.OrderPaymentCard;

/**
 * Repository interface for persisting {@link OrderPaymentCard} entities.
 *
 * <p>
 * This is a platform-agnostic abstraction defined in the core library.
 * Implementations must provide concrete persistence logic (e.g., using JPA,
 * Redis, or any other storage backend)
 * when integrating the library into a specific microservice platform.
 * </p>
 *
 * <p>
 * <b>Implementation note:</b>
 * A reference implementation is provided under
 * <code>KafkaSpringbootImplementation/paymentService</code>.
 * </p>
 */
public interface IOrderPaymentCardRepository {
    void save(OrderPaymentCard card);

    void deleteAll();
}
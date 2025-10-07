package com.example.order.repository;

import com.example.order.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Defines the repository interface for managing {@link Order} entities.
 *
 * <p>
 * This interface must be implemented by the user when integrating the core
 * library
 * into a specific platform or persistence layer.
 * For a concrete implementation example, refer to
 * {@code KafkaSpringbootImplementation → orderService → repository}.
 * </p>
 */
public interface IOrderRepository {
    /** Finds all orders belonging to the specified customer. */
    List<Order> findByCustomerId(int customerId);

    /** Finds an order by customer ID and order ID. */
    Optional<Order> findByCustomerIdAndOrderId(int customerId, int orderId);

    /** Saves or updates an order entity. */
    void save(Order order);

    /** Saves or updates a list of orders. */
    void saveAll(List<Order> orders);

    /** Deletes all orders, typically used for cleanup or testing. */
    void deleteAll();

}

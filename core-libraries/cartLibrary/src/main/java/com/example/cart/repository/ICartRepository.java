package com.example.cart.repository;

import com.example.cart.model.Cart;

/**
 * Repository interface for accessing and managing Cart entities.
 *
 * Defines methods for retrieving, persisting, and deleting carts.
 * Implementations may use different storage backends (e.g., Redis, JPA).
 */
public interface ICartRepository {
    /**
     * Retrieves the cart associated with the given customer ID.
     *
     * @param customerId the customer's ID
     * @return the Cart entity, or null if not found
     */
    Cart findByCustomerId(int customerId);

    /**
     * Persists the specified cart.
     *
     * @param cart the Cart to save
     */
    void saveCart(Cart cart);

    /**
     * Deletes the cart associated with the given customer ID.
     *
     * @param customerId the customer's ID
     */
    void deleteCart(int customerId);

    /**
     * Deletes all carts in the repository.
     */
    void deleteAll();

}
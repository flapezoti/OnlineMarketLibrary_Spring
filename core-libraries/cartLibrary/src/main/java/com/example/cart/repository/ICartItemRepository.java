package com.example.cart.repository;

import com.example.cart.model.CartItem;
import com.example.cart.model.CartItemId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for accessing and managing cart items.
 *
 * Defines methods for storing, retrieving, and deleting CartItem entities.
 * Implementations may use different storage backends (e.g., Redis, JPA).
 */
public interface ICartItemRepository {
    /**
     * Retrieves a cart item by its composite ID.
     *
     * @param id the unique identifier of the cart item
     * @return an Optional containing the CartItem if found
     */
    Optional<CartItem> findById(CartItemId id);

    /**
     * Retrieves all cart items belonging to a specific customer.
     *
     * @param customerId the customer's ID
     * @return a list of CartItems for the customer
     */
    List<CartItem> findByCustomerId(int customerId);

    /**
     * Deletes all cart items belonging to a specific customer.
     *
     * @param customerId the customer's ID
     */
    void deleteByCustomerId(int customerId);

    /**
     * Retrieves all cart items matching a seller and product.
     *
     * @param sellerId  the seller's ID
     * @param productId the product's ID
     * @return a list of CartItems matching the criteria
     */
    List<CartItem> findBySellerIdAndProductId(int sellerId, int productId);

    /**
     * Persists a single cart item.
     *
     * @param cartItem the CartItem to save
     */
    void saveCartItem(CartItem cartItem);

    /**
     * Deletes a single cart item.
     *
     * @param cartItem the CartItem to delete
     */
    void delete(CartItem cartItem);

    /**
     * Deletes all cart items in the repository.
     */
    void deleteAll();

    /**
     * Persists multiple cart items in bulk.
     *
     * @param items the list of CartItems to save
     */
    void saveAll(List<CartItem> items);
}
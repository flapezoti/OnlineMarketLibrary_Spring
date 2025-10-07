package com.example.product.repository;

import com.example.product.model.Product;
import com.example.product.model.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * Defines the abstraction for accessing and managing {@link Product} entities.
 *
 * <p>
 * This interface is part of the core library and serves as the persistence
 * contract
 * for all product-related operations. It is intentionally platform-agnostic,
 * meaning
 * it does not depend on any specific framework or database technology.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Provide basic CRUD access for {@link Product} entities.</li>
 * <li>Support lookup operations by {@link ProductId} or seller ID.</li>
 * <li>Offer utility operations for system reset and cleanup, useful during
 * benchmark tests.</li>
 * </ul>
 *
 * <h3>Implementation Notes</h3>
 * <p>
 * Concrete implementations of this interface must be provided by the platform
 * layer
 * (for example, in the <b>KafkaSpringbootImplementation/productService</b>
 * module),
 * where the actual persistence logic (e.g. JPA, Redis, or in-memory storage)
 * is integrated.
 * </p>
 *
 * <h3>Typical Usage</h3>
 * <p>
 * This interface is injected into the core service class
 * (e.g. {@code ProductServiceCore}) to perform product persistence and
 * retrieval.
 * Application developers should not modify this interface but instead implement
 * or extend it according to their platform requirements.
 * </p>
 */
public interface IProductRepository {

    Optional<Product> findById(ProductId id);

    List<Product> findByIdSellerId(int sellerId);

    /**
     * Resets all products to their default state.
     *
     * <p>
     * Typically, this method sets the product status to <code>ACTIVE</code>
     * and resets the version number to zero. This is mainly used during testing
     * or when initializing the system state.
     * </p>
     */
    void reset();

    /**
     * Removes all product records.
     *
     * <p>
     * Intended for cleanup operations during experiments or benchmarking.
     * </p>
     */
    void cleanup();

    void deleteAll();

    void saveProduct(Product product);

    void saveAll(List<Product> products);
}

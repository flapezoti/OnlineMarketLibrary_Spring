package com.example.cart.repository;

import com.example.cart.model.ProductReplica;
import com.example.cart.model.ProductReplicaId;

/**
 * Repository interface for accessing and managing ProductReplica entities.
 *
 * Provides methods for retrieving, persisting, and clearing product replica
 * data,
 * which serves as local copies of product information synchronized from other
 * services.
 */
public interface IProductReplicaRepository {
    /**
     * Retrieves a ProductReplica by its composite identifier.
     *
     * @param productReplicaId the unique identifier of the product replica
     * @return the ProductReplica entity, or null if not found
     */
    ProductReplica findByProductReplicaId(ProductReplicaId productReplicaId);

    /**
     * Persists the specified ProductReplica.
     *
     * @param productReplica the ProductReplica to save
     */
    void saveProductReplica(ProductReplica productReplica);

    /**
     * Resets the repository state, removing all product replicas or reinitializing
     * them.
     */
    void reset();

    /**
     * Deletes all ProductReplica entries in the repository.
     */
    void deleteAll();
}
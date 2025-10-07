package com.example.seller.repository;

/**
 * Repository interface for managing the materialized view that aggregates
 * seller order data.
 *
 * <p>
 * This interface abstracts the operations required to maintain and refresh
 * a denormalized or precomputed view (often used for dashboards or analytics)
 * that summarizes seller performance metrics.
 * </p>
 *
 * <p>
 * <b>Usage:</b><br>
 * Implementations of this interface should define how the view is cleared and
 * rebuilt
 * in the underlying data store (e.g., via SQL refresh, Redis rebuild, or
 * in-memory recomputation).
 * The actual implementation can be found in integration modules such as
 * <b>KafkaSpringbootImplementation</b>.
 * </p>
 */
public interface IOrderSellerViewRepository {

        void clearMaterializedView();

        void populateMaterializedView();

}

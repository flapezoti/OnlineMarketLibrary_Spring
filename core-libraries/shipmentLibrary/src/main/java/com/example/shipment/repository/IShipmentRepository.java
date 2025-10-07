package com.example.shipment.repository;

import com.example.shipment.model.Shipment;
import com.example.shipment.model.ShipmentId;

import java.util.Optional;

/**
 * Repository interface for managing {@link Shipment} entities.
 *
 * <p>
 * This interface defines the standard persistence operations for shipment data.
 * It should be implemented by the platform-specific persistence layer
 * (e.g., JPA, Redis, or in-memory repository), depending on the chosen
 * architecture.
 * </p>
 *
 * <p>
 * Refer to the concrete implementation in
 * <code>KafkaSpringbootImplementation</code>
 * for platform-specific behavior.
 * </p>
 */
public interface IShipmentRepository {

    /**
     * Retrieves a {@link Shipment} entity by its composite identifier.
     *
     * @param id the {@link ShipmentId} representing the unique shipment key
     * @return an {@link Optional} containing the matching shipment if found,
     *         otherwise empty
     */
    Optional<Shipment> findById(ShipmentId id);

    /**
     * Persists or updates a {@link Shipment} record in the repository.
     *
     * @param shipment the {@link Shipment} entity to save or update
     */

    void save(Shipment shipment);

    /**
     * Deletes the specified {@link Shipment} entity from the repository.
     *
     * @param shipment the {@link Shipment} entity to remove
     */
    void deleteShipment(Shipment shipment);

    /**
     * Deletes all {@link Shipment} records.
     *
     * <p>
     * This method is typically used for cleanup or test data reset operations.
     * </p>
     */
    void deleteAll();

}

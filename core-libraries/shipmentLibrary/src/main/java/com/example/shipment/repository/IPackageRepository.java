package com.example.shipment.repository;

import com.example.shipment.model.Package;
import com.example.common.entities.PackageStatus;
import com.example.shipment.model.Shipment;

import java.util.List;

/**
 * Repository interface for managing {@link Package} entities within the
 * shipment domain.
 * 
 * <p>
 * This interface defines the contract for retrieving, saving, and aggregating
 * package data.
 * It is framework-agnostic and should be implemented by the platform-specific
 * persistence layer
 * (e.g., Spring JPA, Redis, or custom repository) depending on the chosen
 * infrastructure.
 * </p>
 *
 * <p>
 * Refer to the platform-specific implementation (e.g., in
 * <code>KafkaSpringbootImplementation</code>)
 * for concrete persistence behavior.
 * </p>
 */
public interface IPackageRepository {

    /**
     * Retrieves, for each seller, the earliest open shipment with the specified
     * {@link PackageStatus}.
     *
     * <p>
     * The returned list contains Object arrays where:
     * <ul>
     * <li>index 0 – {@code Integer}: sellerId</li>
     * <li>index 1 – {@code String}: concatenated natural key (e.g.,
     * customerId_orderId)</li>
     * </ul>
     * </p>
     *
     * @param status shipment status to filter open packages
     * @return list of sellerId and earliest open shipment identifiers
     */
    List<Object[]> getOldestOpenShipmentPerSeller(PackageStatus status);

    /**
     * Finds all shipped packages for a specific order and seller under a given
     * status.
     *
     * @param customerId the customer identifier
     * @param orderId    the order identifier
     * @param sellerId   the seller identifier
     * @param status     the package status to filter by
     * @return list of {@link Package} objects matching the given parameters
     */
    List<Package> getShippedPackagesByOrderAndSeller(int customerId, int orderId, int sellerId, PackageStatus status);

    /**
     * Counts the total number of delivered packages for a given order and customer.
     *
     * @param customerId the customer identifier
     * @param orderId    the order identifier
     * @param status     the target delivery status (e.g., {@code DELIVERED})
     * @return number of packages matching the specified status
     */
    int getTotalDeliveredPackagesForOrder(int customerId, int orderId, PackageStatus status);

    /**
     * Retrieves all packages belonging to a given order for a specific customer.
     *
     * @param customerId the customer identifier
     * @param orderId    the order identifier
     * @return list of all {@link Package} entities in the specified order
     */
    List<Package> findAllByOrderIdAndCustomerId(int customerId, int orderId);

    /**
     * Deletes all package records.
     *
     * <p>
     * This method is typically used for testing, cleanup, or system resets.
     * </p>
     */
    void deleteAll();

    /**
     * Saves or updates a single {@link Package} record.
     *
     * @param pack the {@link Package} entity to persist
     */
    void savePackage(Package Package);

    void saveAll(List<Package> Packages);

    void save(Package pack);

}

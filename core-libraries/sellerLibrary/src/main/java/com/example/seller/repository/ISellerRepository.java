package com.example.seller.repository;

import com.example.seller.dto.SellerDashboard;
import com.example.seller.model.OrderEntry;
import com.example.seller.model.Seller;

import java.util.List;

/**
 * Repository interface for managing {@link Seller} entities and related order
 * data.
 *
 * <p>
 * This interface defines the data access methods required to retrieve, store,
 * and aggregate seller information and their order activities. It acts as an
 * abstraction
 * over the underlying persistence mechanism (e.g., JPA, Redis, or custom
 * storage).
 * </p>
 *
 * <p>
 * <b>Usage:</b><br>
 * Implementations of this interface should provide concrete logic for accessing
 * and
 * updating seller data. See <b>KafkaSpringbootImplementation</b> for an example
 * implementation.
 * </p>
 */
public interface ISellerRepository {

    List<OrderEntry> findByCustomerIdAndOrderId(int customerId, int orderId);

    OrderEntry findById(int id);

    SellerDashboard queryDashboard(int sellerId);

    void deleteAllSellers();

    void deleteAllOrderEntries();

    void deleteAll();

    void save(Seller seller);

    void deleteById(int sellerId);
}

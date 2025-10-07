package com.example.seller.repository;

import com.example.seller.model.OrderEntry;
import com.example.seller.model.OrderEntryId;
import com.example.common.entities.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface defining persistence operations for {@link OrderEntry}.
 *
 * <p>
 * This interface provides the abstraction for accessing and managing seller
 * order entry data
 * across multiple storage implementations (e.g., JPA, Redis, or any custom
 * database layer).
 * It is designed to be platform-independent — concrete implementations should
 * be provided by
 * specific integration modules such as <b>KafkaSpringbootImplementation</b>.
 * </p>
 *
 * <p>
 * <b>Usage:</b><br>
 * Users of the core library should implement this interface to define how order
 * entries
 * are persisted, queried, and deleted. For a working example, refer to the
 * implementation
 * provided in the Kafka + Spring Boot adapter module.
 * </p>
 */
public interface IOrderEntryRepository {
    Optional<OrderEntry> findById(OrderEntryId id);

    List<OrderEntry> findByCustomerIdAndOrderId(int customerId, int orderId);

    List<OrderEntry> findAllBySellerId(int sellerId);

    List<Object[]> findAllSellerAggregates(List<OrderStatus> statuses);

    void saveOrderEntry(OrderEntry orderEntry);

    void deleteOrderEntry(OrderEntry orderEntry);

    void deleteAll();

    void saveAll(List<OrderEntry> orderEntries);

    void save(OrderEntry orderEntry);
}

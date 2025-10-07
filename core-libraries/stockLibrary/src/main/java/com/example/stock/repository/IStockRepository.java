package com.example.stock.repository;

import com.example.stock.model.StockItem;
import com.example.stock.model.StockItemId;
import java.util.List;
import java.util.Optional;

public interface IStockRepository {

    /**
     * Repository interface for managing stock items.
     *
     * <p>
     * This abstraction defines CRUD and query operations for stock management.
     * Implementations may use JPA, Redis, or other persistence mechanisms.
     * </p>
     *
     * <p>
     * You can refer to the implementation examples under
     * <strong>KafkaSpringbootImplementation</strong> for reference.
     * </p>
     */
    StockItem findForUpdate(int sellerId, int productId);

    List<StockItem> findItemsByIds(List<StockItemId> ids);

    Optional<StockItem> findById(StockItemId stockItemId);

    Optional<StockItem> findById(int sellerId, int productId);

    List<StockItem> findBySellerId(int sellerId);

    void reset(int qty);

    void save(StockItem stockItem);

    void saveAll(List<StockItem> stockItemsReserved);

    void flush();

    void deleteAll();

}

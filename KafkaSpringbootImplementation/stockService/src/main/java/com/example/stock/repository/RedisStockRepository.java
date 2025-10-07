package com.example.stock.repository;

import com.example.stock.model.StockItem;
import com.example.stock.model.StockItemId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class RedisStockRepository implements IStockRepository {

    private static final String STOCK_KEY_PREFIX = "stock:";

    @Autowired
    private RedisTemplate<String, StockItem> redisTemplate;

    /**
     * Generate a Redis key based on sellerId and productId.
     * Format: stock:{sellerId}:{productId}
     */
    private String generateKey(int sellerId, int productId) {
        return STOCK_KEY_PREFIX + sellerId + ":" + productId;
    }

    /**
     * Generate a Redis Key based on StockItem
     */
    private String generateKey(StockItem item) {
        return generateKey(item.getSellerId(), item.getProductId());
    }

    /**
     * Generate Redis key patterns for multiple StockItemIds.
     * Multiple IDs are concatenated with commas.
     */
    private List<String> generateKeys(List<StockItemId> ids) {
        return ids.stream()
                .map(id -> generateKey(id.getSellerId(), id.getProductId()))
                .collect(Collectors.toList());
    }

    @Override
    public StockItem findForUpdate(int sellerId, int productId) {
        // Redis does not support pessimistic locking; simply return the corresponding
        // stock item.
        String key = generateKey(sellerId, productId);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public List<StockItem> findItemsByIds(List<StockItemId> ids) {
        List<String> keys = generateKeys(ids);
        List<StockItem> list = redisTemplate.opsForValue().multiGet(new HashSet<>(keys));
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public Optional<StockItem> findById(StockItemId stockItemId) {
        // Delegate directly to the overloaded version that accepts two parameters.
        return findById(stockItemId.getSellerId(), stockItemId.getProductId());
    }

    @Override
    public Optional<StockItem> findById(int sellerId, int productId) {
        String key = generateKey(sellerId, productId);
        StockItem item = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(item);
    }

    @Override
    public List<StockItem> findBySellerId(int sellerId) {
        // Fuzzy match keys starting with "stock:{sellerId}:"
        String pattern = STOCK_KEY_PREFIX + sellerId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<StockItem> list = redisTemplate.opsForValue().multiGet(keys);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public void reset(int qty) {
        // Retrieve all stock item keys
        Set<String> keys = redisTemplate.keys(STOCK_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            // For each stock item, update: active = true, version = "0", qtyReserved = 0,
            // qtyAvailable = qty
            // Since Redis does not support batch UPDATE, iterate through all stock items
            // and update them individually before saving
            List<StockItem> items = redisTemplate.opsForValue().multiGet(keys);
            if (items != null) {
                for (StockItem item : items) {
                    // Assume that StockItem provides the following setter methods
                    item.setActive(true);
                    item.setVersion("0");
                    item.setQtyReserved(0);
                    item.setQtyAvailable(qty);
                    save(item);
                }
            }
        }
    }

    @Override
    public void save(StockItem stockItem) {
        String key = generateKey(stockItem);
        // Save stock item to Redis，set the expiration time to 1 hour.
        redisTemplate.opsForValue().set(key, stockItem, 1, TimeUnit.HOURS);
    }

    @Override
    public void saveAll(List<StockItem> stockItemsReserved) {
        if (stockItemsReserved == null || stockItemsReserved.isEmpty())
            return;
        Map<String, StockItem> map = new HashMap<>();
        for (StockItem item : stockItemsReserved) {
            map.put(generateKey(item), item);
        }
        redisTemplate.opsForValue().multiSet(map);
    }

    @Override
    public void flush() {
    }

    @Override
    public void deleteAll() {
        Set<String> keys = redisTemplate.keys(STOCK_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}

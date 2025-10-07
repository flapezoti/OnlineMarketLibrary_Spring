package com.example.seller.repository;

import com.example.seller.dto.SellerDashboard;
import com.example.seller.model.OrderEntry;
import com.example.seller.model.Seller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisSellerRepository implements ISellerRepository {

    private static final String SELLER_PREFIX = "seller:";
    private static final String ORDER_ENTRY_PREFIX = "orderEntry:"; // Assume order entries are stored with keys like:
                                                                    // orderEntry:{customerId}:{orderId}:{...}
    private static final String SELLER_DASHBOARD_PREFIX = "sellerDashboard:";

    @Autowired
    private RedisTemplate<String, Seller> sellerRedisTemplate;

    @Autowired
    private RedisTemplate<String, OrderEntry> orderEntryRedisTemplate;

    // @Qualifier("orderSellerViewRedisTemplate")
    @Autowired
    private RedisTemplate<String, SellerDashboard> sellerDashboardRedisTemplate;

    /**
     * Query order entries by customerId and orderId.
     * Assumes that the key format for order entries is:
     * orderEntry:{customerId}:{orderId}:{uniquePart}
     */
    @Override
    public List<OrderEntry> findByCustomerIdAndOrderId(int customerId, int orderId) {
        String pattern = ORDER_ENTRY_PREFIX + customerId + ":" + orderId + ":*";
        Set<String> keys = orderEntryRedisTemplate.keys(pattern);
        return keys == null || keys.isEmpty()
                ? null
                : orderEntryRedisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * Query a single order entry by its unique ID (assumed to be stored as an
     * integer).
     * Redis key format: orderEntry:{id}
     */
    @Override
    public OrderEntry findById(int id) {
        return orderEntryRedisTemplate.opsForValue().get(ORDER_ENTRY_PREFIX + id);
    }

    /**
     * Retrieve the seller dashboard data from Redis.
     * Redis key format: sellerDashboard:{sellerId}
     */
    @Override
    public SellerDashboard queryDashboard(int sellerId) {
        return sellerDashboardRedisTemplate.opsForValue().get(SELLER_DASHBOARD_PREFIX + sellerId);
    }

    /**
     * Delete all seller data by removing every Redis key
     * that starts with the prefix "seller:".
     */
    @Override
    public void deleteAllSellers() {
        Set<String> keys = sellerRedisTemplate.keys(SELLER_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            sellerRedisTemplate.delete(keys);
        }
    }

    /**
     * Delete all order entry data by removing every Redis key
     * that starts with the prefix "orderEntry:".
     */
    @Override
    public void deleteAllOrderEntries() {
        Set<String> keys = orderEntryRedisTemplate.keys(ORDER_ENTRY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            orderEntryRedisTemplate.delete(keys);
        }
    }

    /**
     * Delete all seller-related data, including sellers, order entries,
     * and dashboard data stored in Redis.
     */
    @Override
    public void deleteAll() {
        deleteAllSellers();
        deleteAllOrderEntries();
        Set<String> dashKeys = sellerDashboardRedisTemplate.keys(SELLER_DASHBOARD_PREFIX + "*");
        if (dashKeys != null && !dashKeys.isEmpty()) {
            sellerDashboardRedisTemplate.delete(dashKeys);
        }
    }

    /**
     * Save the seller information into Redis, using the key format:
     * seller:{sellerId}, and set an expiration time (e.g., 30 minutes).
     */
    @Override
    public void save(Seller seller) {
        String key = SELLER_PREFIX + seller.getId();
        sellerRedisTemplate.opsForValue().set(key, seller, 30, TimeUnit.MINUTES);
    }

    @Override
    public void deleteById(int sellerId) {
        String key = SELLER_PREFIX + sellerId;
        sellerRedisTemplate.delete(key);
    }

}

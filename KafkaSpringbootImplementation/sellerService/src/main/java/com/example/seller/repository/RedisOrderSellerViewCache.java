package com.example.seller.repository;

import com.example.seller.dto.SellerDashboard;
import com.example.seller.model.OrderSellerView;
import com.example.seller.service.IMaterializedViewServiceCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisOrderSellerViewCache implements IMaterializedViewServiceCache {

    private static final String DASHBOARD_KEY = "sellerDashboard:";

    @Autowired
    private RedisTemplate<String, SellerDashboard> redisTemplate;

    @Override
    public void clear() {
        Set<String> keys = redisTemplate.keys(DASHBOARD_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // @Override
    // public void updateSellerView(int sellerId, SellerDashboard dashboard) {
    // String key = DASHBOARD_KEY + sellerId;
    // redisTemplate.opsForValue().set(key, dashboard, 1, TimeUnit.HOURS);
    // }

    @Override
    public OrderSellerView getSellerView(int sellerId) {
        String key = DASHBOARD_KEY + sellerId;
        return redisTemplate.opsForValue().get(key).getSellerView();
    }

    @Override
    public void updateSellerView(int sellerId, OrderSellerView view) {
        // Construct Redis key, e.g., "sellerDashboard:123"
        String key = DASHBOARD_KEY + sellerId;
        // Based on business logic, wrap OrderSellerView into a SellerDashboard object
        SellerDashboard dashboard = new SellerDashboard();
        dashboard.setSellerView(view);
        // If detailed order information is available, it can also be included here;
        // for simplicity, this example only wraps the aggregated view.
        redisTemplate.opsForValue().set(key, dashboard, 1, TimeUnit.HOURS);
        // Store the view object in Redis with a 1-hour expiration time
        // redisTemplate.opsForValue().set(key, view, 1, TimeUnit.HOURS);
    }
}

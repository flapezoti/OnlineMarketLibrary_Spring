package com.example.order.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class RedisProcessedCheckoutRepository implements IProcessedCheckoutRepository {

    private static final String PREFIX = "order:processedCheckout:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean markProcessed(String checkoutKey) {
        // SET key "1" NX -> true only for the first caller; atomic, no lock needed.
        Boolean firstTime = stringRedisTemplate.opsForValue().setIfAbsent(PREFIX + checkoutKey, "1");
        return Boolean.TRUE.equals(firstTime);
    }

    @Override
    public void deleteAll() {
        Set<String> keys = stringRedisTemplate.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}

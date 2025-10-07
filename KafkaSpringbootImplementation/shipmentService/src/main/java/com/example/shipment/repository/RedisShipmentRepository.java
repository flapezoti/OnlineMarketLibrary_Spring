//package com.example.shipment.repository;
//
//import com.example.shipment.model.Shipment;
//import com.example.shipment.model.ShipmentId;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//import java.util.Set;
//
//@Repository
//public class RedisShipmentRepository implements IShipmentRepository {
//
//    private static final String SHIPMENT_KEY_PREFIX = "shipment:";
//
//    @Autowired
//    private RedisTemplate<String, Shipment> redisTemplate;
//
//    private String buildKey(ShipmentId id) {
//        return SHIPMENT_KEY_PREFIX + id.getCustomerId() + "_" + id.getOrderId();
//    }
//
//    private String buildKey(Shipment shipment) {
//        return SHIPMENT_KEY_PREFIX + shipment.getCustomerId() + "_" + shipment.getOrderId();
//    }
//
//    @Override
//    public void deleteAll() {
//        Set<String> keys = redisTemplate.keys(SHIPMENT_KEY_PREFIX + "*");
//        if (keys != null && !keys.isEmpty()) {
//            redisTemplate.delete(keys);
//        }
//    }
//
//    @Override
//    public Optional<Shipment> findById(ShipmentId id) {
//        String key = buildKey(id);
//        Shipment shipment = redisTemplate.opsForValue().get(key);
//        return Optional.ofNullable(shipment);
//    }
//
//    @Override
//    public void save(Shipment shipment) {
//        String key = buildKey(shipment);
//        redisTemplate.opsForValue().set(key, shipment);
//    }
//
//    @Override
//    public void deleteShipment(Shipment shipment) {
//        String key = buildKey(shipment);
//        redisTemplate.delete(key);
//    }
//}
package com.example.shipment.repository;

import com.example.shipment.model.Shipment;
import com.example.shipment.model.ShipmentId;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public class RedisShipmentRepository implements IShipmentRepository {

    // Define the key prefix for storing Shipment objects, e.g., "shipment:"
    private static final String SHIPMENT_KEY_PREFIX = "shipment:";

    @Autowired
    private RedisTemplate<String, Shipment> redisTemplate;

    /**
     * Delete all Shipment data from Redis.
     */
    @Override
    public void deleteAll() {
        // Find all keys starting with SHIPMENT_KEY_PREFIX
        Set<String> keys = redisTemplate.keys(SHIPMENT_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Find a Shipment object by its {@link ShipmentId}.
     */
    @Override
    public Optional<Shipment> findById(ShipmentId id) {
        // Construct the Redis key, assuming id.toString() generates a unique identifier
        String key = SHIPMENT_KEY_PREFIX + id.getCustomerId() + "_" + id.getOrderId();
        // String key = SHIPMENT_KEY_PREFIX + id.toString();;
        Shipment shipment = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(shipment);
    }

    /**
     * Save a Shipment to Redis with the key format "shipment:{id}".
     */
    @Override
    public void save(Shipment shipment) {
        // String key = SHIPMENT_KEY_PREFIX + shipment.getId().toString();
        String key = SHIPMENT_KEY_PREFIX + shipment.getCustomerId() + "_" + shipment.getOrderId();

        redisTemplate.opsForValue().set(key, shipment);
    }

    /**
     * Delete a specific Shipment entry from Redis.
     */
    @Override
    public void deleteShipment(Shipment shipment) {
        String key = SHIPMENT_KEY_PREFIX + shipment.getCustomerId() + "_" + shipment.getOrderId();
        // String key = SHIPMENT_KEY_PREFIX + shipment.getId().toString();
        redisTemplate.delete(key);
    }
}

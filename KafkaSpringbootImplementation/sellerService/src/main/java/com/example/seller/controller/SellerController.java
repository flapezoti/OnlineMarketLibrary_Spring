package com.example.seller.controller;

import com.example.seller.dto.SellerDashboard;
import com.example.seller.model.Seller;
import com.example.seller.service.ISellerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seller")
public class SellerController {

    @Autowired
    private RedisTemplate<String, Seller> sellerRedisTemplate;

    @Autowired
    private ISellerService sellerService;

    private static final Logger logger = LoggerFactory.getLogger(SellerController.class);

    /**
     * Add or update a seller record
     * (uses Redis as the sole data storage backend).
     */
    @PostMapping("/")
    public ResponseEntity<?> addSeller(@RequestBody Seller seller) {
        logger.info("Received add seller request: {}", seller);

        // Redis key format: seller:{sellerId}
        String redisKey = "seller:" + seller.getId();

        // Step 1: Try to get Seller from Redis
        Seller cachedSeller = sellerRedisTemplate.opsForValue().get(redisKey);
        if (cachedSeller != null) {
            // If the seller already exists in Redis, treat it as an existing record —
            // either update it or skip to avoid duplication
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body("Seller " + seller.getId() + " already exists.");
        }

        // If the seller does not exist in Redis, treat it as a new seller.
        // Directly use the provided seller object and assign a new sellerId.
        seller.setId(seller.getId());

        // Step 2: Save the seller object into Redis and set an expiration time (e.g.,
        // 30 minutes)
        sellerRedisTemplate.opsForValue().set(redisKey, seller, 30, java.util.concurrent.TimeUnit.MINUTES);
        logger.info("Seller cached in Redis for seller {}", seller.getId());

        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    /**
     * Get seller details (retrieved only from Redis)
     */
    @GetMapping("/{sellerId}")
    public ResponseEntity<?> getSeller(@PathVariable int sellerId) {
        String redisKey = "seller:" + sellerId;
        Seller seller = sellerRedisTemplate.opsForValue().get(redisKey);
        if (seller == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Seller " + sellerId + " not found in Redis");
        }
        return ResponseEntity.ok(seller);
    }

    /**
     * Query seller dashboard data by delegating to {@link SellerService}.
     * <p>
     * This method relies on Redis-based aggregation, allowing real-time computation
     * of summary statistics without involving a relational database.
     * </p>
     */
    @GetMapping("/dashboard/{sellerId}")
    public ResponseEntity<?> getDashboard(@PathVariable int sellerId) {
        logger.info("Received dashboard request for seller: {}", sellerId);
        try {
            SellerDashboard dashboard = sellerService.queryDashboard(sellerId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.error("Error querying dashboard for seller {}: {}", sellerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    /**
     * Delete seller data stored in Redis only.
     * <p>
     * This method removes the seller entry from Redis without affecting
     * any persistent database records.
     * </p>
     */
    @DeleteMapping("/{sellerId}")
    public ResponseEntity<?> deleteSeller(@PathVariable int sellerId) {
        String redisKey = "seller:" + sellerId;
        Seller seller = sellerRedisTemplate.opsForValue().get(redisKey);
        if (seller == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Seller not found");
        }
        sellerRedisTemplate.delete(redisKey);
        return ResponseEntity.accepted().build();
    }

    /**
     * Clear all seller data (e.g., for testing purposes) by removing
     * all Redis keys starting with the prefix "seller:".
     */
    @PatchMapping("/cleanup")
    public ResponseEntity<?> cleanup() {
        logger.warn("Cleanup requested");
        try {

            sellerRedisTemplate.delete(sellerRedisTemplate.keys("seller:*"));
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Seller data cleaned");
        } catch (Exception e) {
            logger.error("Error during seller cleanup: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }

    /**
     * Reset all seller data (e.g., for testing purposes).
     * This may include clearing all existing data and reinitializing
     * default seller records as needed.
     */
    @PatchMapping("/reset")
    public ResponseEntity<?> reset() {
        logger.warn("Reset requested");
        try {
            // Define how to reset seller data here, e.g., delete all existing data and
            // insert initial records
            sellerRedisTemplate.delete(sellerRedisTemplate.keys("seller:*"));
            return ResponseEntity.ok("Seller data reset");
        } catch (Exception e) {
            logger.error("Error during seller reset: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }
    }
}

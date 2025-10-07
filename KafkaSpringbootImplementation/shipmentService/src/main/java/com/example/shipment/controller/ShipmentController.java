package com.example.shipment.controller;

import com.example.shipment.model.Shipment;
import com.example.shipment.model.ShipmentId;
import com.example.shipment.service.IShipmentService;
import com.example.shipment.repository.RedisShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/shipment")
public class ShipmentController {

    @Autowired
    private RedisTemplate<String, Shipment> shipmentRedisTemplate;

    @Autowired
    private IShipmentService shipmentService;

    @Autowired
    private RedisShipmentRepository shipmentRepository;

    private static final Logger logger = LoggerFactory.getLogger(ShipmentController.class);

    private static final String SHIPMENT_KEY_PREFIX = "shipment:";

    /**
     * Add a new Shipment entry (using Redis as the only data store).
     */
    @PostMapping("/")
    public ResponseEntity<?> addShipment(@RequestBody Shipment shipment) {
        try {
            String redisKey = SHIPMENT_KEY_PREFIX + shipment.getId().toString();

            // Check if the Shipment already exists in Redis
            Shipment cachedShipment = shipmentRedisTemplate.opsForValue().get(redisKey);
            if (cachedShipment != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Shipment already exists with id: " + shipment.getId());
            }

            // Save the Shipment to Redis with a 1-hour expiration time (adjustable if
            // needed)
            shipmentRedisTemplate.opsForValue().set(redisKey, shipment, 1, TimeUnit.HOURS);
            // Save using the Redis-based repository implementation
            shipmentRepository.save(shipment);

            logger.info("Shipment added, id: {}", shipment.getId());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            logger.error("Failed to add shipment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{customerId}/{orderId}")
    public ResponseEntity<Shipment> getShipment(@PathVariable int customerId, @PathVariable int orderId) {
        // Construct the Redis key, ensuring consistency with the key used during save
        String redisKey = SHIPMENT_KEY_PREFIX + customerId + "_" + orderId;
        Shipment shipment = shipmentRedisTemplate.opsForValue().get(redisKey);
        if (shipment == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
        return ResponseEntity.ok(shipment);
    }

    @PatchMapping("/{instanceId}")
    public ResponseEntity<Void> updateShipment(@PathVariable("instanceId") String instanceId) {
        try {
            // Call the Service layer to perform update logic
            shipmentService.updateShipment(instanceId);
            return ResponseEntity.accepted().build(); // Expect HTTP status 202 (Accepted) in unit test
        } catch (Exception e) {
            logger.error("Failed to update shipment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a specific Shipment record.
     */
    @DeleteMapping("/{shipmentId}")
    public ResponseEntity<?> deleteShipment(@PathVariable String shipmentId) {
        try {
            String redisKey = SHIPMENT_KEY_PREFIX + shipmentId;
            shipmentRedisTemplate.delete(redisKey);
            // If needed, call the repository layer to delete the corresponding Shipment
            // record
            // shipmentRepository.deleteShipment(targetShipment);
            logger.info("Shipment deleted, id: {}", shipmentId);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            logger.error("Failed to delete shipment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cleans up all Shipment data (for example, used in tests or when resetting the
     * dataset).
     */
    @PatchMapping("/cleanup")
    public ResponseEntity<?> cleanup() {
        try {
            shipmentRepository.deleteAll();
            logger.info("All shipments cleaned up");
            return ResponseEntity.ok("Shipments cleared");
        } catch (Exception e) {
            logger.error("Failed to clean up shipments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resets Shipment data (implement according to specific business needs).
     */
    @PatchMapping("/reset")
    public ResponseEntity<?> reset() {
        try {
            // If a reset logic exists, you can call shipmentRepository.reset() or similar
            // methods.
            logger.info("Shipments reset requested");
            return ResponseEntity.ok("Shipments reset");
        } catch (Exception e) {
            logger.error("Failed to reset shipments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Asynchronously update Shipment data.
     * If synchronization with other storage systems is required,
     * this method can be annotated with {@code @Async} to perform
     * updates in a non-blocking manner.
     */
    @Async
    public void asyncUpdateShipment(Shipment shipment) {
        try {
            shipmentRepository.save(shipment);
            logger.info("Async shipment update saved for id: {}", shipment.getId());
        } catch (Exception e) {
            logger.error("Failed to update shipment asynchronously for id: {}", shipment.getId(), e);
        }
    }
}

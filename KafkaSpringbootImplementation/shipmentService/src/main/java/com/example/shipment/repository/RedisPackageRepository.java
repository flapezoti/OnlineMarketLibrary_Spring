package com.example.shipment.repository;

import com.example.common.entities.PackageStatus;
import com.example.shipment.model.Package;
import com.example.shipment.model.PackageId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class RedisPackageRepository implements IPackageRepository {

    private static final String PACKAGE_PREFIX = "package:";

    @Autowired
    private RedisTemplate<String, Package> redisTemplate;

    /**
     * Generate a unique Redis key based on the Package object.
     * Format: package:{customerId}:{orderId}:{sellerId}:{packageId}
     */
    private String generateKey(Package pkg) {
        PackageId id = pkg.getId();
        return PACKAGE_PREFIX + id.getCustomerId() + ":" + id.getOrderId() + ":" + pkg.getSellerId() + ":"
                + id.getPackageId();
    }

    /**
     * Generate a query pattern based on the provided fields.
     * If sellerId is null, the pattern will match all entries for that part.
     */
    private String generatePattern(Integer customerId, Integer orderId, Integer sellerId) {
        StringBuilder sb = new StringBuilder(PACKAGE_PREFIX);
        sb.append(customerId != null ? customerId : "*").append(":")
                .append(orderId != null ? orderId : "*").append(":")
                .append(sellerId != null ? sellerId : "*").append(":*");
        return sb.toString();
    }

    /**
     * Retrieve all package data from Redis and perform aggregation.
     * Returns the earliest order ID (by lexicographic comparison of
     * "customerId|orderId") for each seller.
     * The result is a List<Object[]>, where each element contains:
     * [sellerId, "customerId|orderId"]
     */
    @Override
    public List<Object[]> getOldestOpenShipmentPerSeller(PackageStatus status) {
        // Retrieve all package keys from Redis
        Set<String> keys = redisTemplate.keys(PACKAGE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Package> allPackages = redisTemplate.opsForValue().multiGet(keys);
        if (allPackages == null) {
            return Collections.emptyList();
        }
        // Filter packages that match the target status
        List<Package> filtered = allPackages.stream()
                .filter(pkg -> pkg.getStatus() == status)
                .collect(Collectors.toList());
        // Group packages by sellerId, and within each group, find the smallest
        // (customerId|orderId) value
        Map<Integer, String> aggregate = new HashMap<>();
        for (Package pkg : filtered) {
            int sellerId = pkg.getSellerId();
            String orderKey = pkg.getId().getCustomerId() + "|" + pkg.getId().getOrderId();
            aggregate.merge(sellerId, orderKey,
                    (existing, current) -> (current.compareTo(existing) < 0) ? current : existing);
        }
        // Convert aggregation result to List<Object[]>: [sellerId, orderKey]
        List<Object[]> result = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : aggregate.entrySet()) {
            result.add(new Object[] { entry.getKey(), entry.getValue() });
        }
        return result;
    }

    /**
     * Query packages by customerId, orderId, sellerId, and the specified status.
     */
    @Override
    public List<Package> getShippedPackagesByOrderAndSeller(int customerId, int orderId, int sellerId,
            PackageStatus status) {
        String pattern = generatePattern(customerId, orderId, sellerId);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Package> packages = redisTemplate.opsForValue().multiGet(keys);
        if (packages == null) {
            return Collections.emptyList();
        }
        return packages.stream()
                .filter(pkg -> pkg.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Query the number of packages with the specified status
     * for the given customerId and orderId.
     */
    @Override
    public int getTotalDeliveredPackagesForOrder(int customerId, int orderId, PackageStatus status) {
        String pattern = generatePattern(customerId, orderId, null);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        List<Package> packages = redisTemplate.opsForValue().multiGet(keys);
        if (packages == null) {
            return 0;
        }
        return (int) packages.stream()
                .filter(pkg -> pkg.getStatus() == status)
                .count();
    }

    /**
     * Retrieve all packages associated with the specified
     * customerId and orderId.
     */
    @Override
    public List<Package> findAllByOrderIdAndCustomerId(int customerId, int orderId) {
        String pattern = generatePattern(customerId, orderId, null);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<Package> packages = redisTemplate.opsForValue().multiGet(keys);
        return packages == null ? Collections.emptyList() : packages;
    }

    /**
     * Delete all package data whose keys start with PACKAGE_PREFIX.
     */
    @Override
    public void deleteAll() {
        Set<String> keys = redisTemplate.keys(PACKAGE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Save a single Package object to Redis with a 1-hour expiration time.
     */
    @Override
    public void savePackage(Package pkg) {
        String key = generateKey(pkg);
        redisTemplate.opsForValue().set(key, pkg, 1, TimeUnit.HOURS);
    }

    /**
     * Save multiple Package objects to Redis in batch mode.
     */
    @Override
    public void saveAll(List<Package> packages) {
        if (packages == null || packages.isEmpty())
            return;
        Map<String, Package> map = new HashMap<>();
        for (Package pkg : packages) {
            map.put(generateKey(pkg), pkg);
        }
        redisTemplate.opsForValue().multiSet(map);
    }

    /**
     * Save a single Package by delegating to {@link #savePackage(Package)}.
     */
    @Override
    public void save(Package pack) {
        savePackage(pack);
    }
}

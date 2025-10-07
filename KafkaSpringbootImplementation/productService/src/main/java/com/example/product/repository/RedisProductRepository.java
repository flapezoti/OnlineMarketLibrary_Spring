package com.example.product.repository;

import com.example.product.model.Product;
import com.example.product.model.ProductId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisProductRepository implements IProductRepository {

    private static final String PRODUCT_PREFIX = "product:";

    @Autowired
    private RedisTemplate<String, Product> productRedisTemplate;

    // key() method
    private String key(ProductId id) {
        return PRODUCT_PREFIX + id.getSellerId() + ":" + id.getProductId();
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        Product product = productRedisTemplate.opsForValue().get(key(id));
        return Optional.ofNullable(product);
    }

    @Override
    public List<Product> findByIdSellerId(int sellerId) {
        Set<String> keys = productRedisTemplate.keys(PRODUCT_PREFIX + sellerId + ":*");
        if (keys != null && !keys.isEmpty()) {
            List<Product> products = productRedisTemplate.opsForValue().multiGet(keys);
            return products != null ? products : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    @Override
    public void reset() {
        // Iterate over all products, set their status to ACTIVE, and reset version to 0
        Set<String> keys = productRedisTemplate.keys(PRODUCT_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                Product product = productRedisTemplate.opsForValue().get(key);
                if (product != null) {
                    product.setStatus("ACTIVE");
                    product.setVersion("0");
                    productRedisTemplate.opsForValue().set(key, product);
                }
            }
        }
    }

    @Override
    public void cleanup() {
        Set<String> keys = productRedisTemplate.keys(PRODUCT_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            productRedisTemplate.delete(keys);
        }
    }

    @Override
    public void deleteAll() {
        cleanup();
    }

    @Override
    public void saveProduct(Product product) {
        // Construct Redis key using the format: "product:{sellerId}:{productId}"
        String redisKey = PRODUCT_PREFIX + product.getSellerId() + ":" + product.getProductId();
        productRedisTemplate.opsForValue().set(
                redisKey,
                product,
                30, TimeUnit.MINUTES);
    }

    @Override
    public void saveAll(List<Product> products) {
        for (Product product : products) {
            saveProduct(product);
        }
    }

}

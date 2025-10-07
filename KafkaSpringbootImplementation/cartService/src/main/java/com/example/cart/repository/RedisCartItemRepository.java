package com.example.cart.repository;

import com.example.cart.model.CartItem;
import com.example.cart.model.CartItemId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis-based implementation of ICartItemRepository.
 * 
 * This repository stores CartItem entities in Redis and maintains two auxiliary
 * indexes:
 * - A per-customer key set (cartItemKeys) to efficiently query items by
 * customer.
 * - A per-seller+product key set (cartItemIndex) to query items by seller and
 * product.
 *
 * Key structure:
 * - Main data: "cartItem:{customerId}:{sellerId}:{productId}"
 * - Customer index: "cartItemKeys:{customerId}" (Set of keys)
 * - Seller-product index: "cartItemIndex:{sellerId}:{productId}" (Set of keys)
 */
@Repository
public class RedisCartItemRepository implements ICartItemRepository {
    // Redis key prefixes
    private static final String CART_ITEM_PREFIX = "cartItem:";
    private static final String CART_ITEM_KEYS_PREFIX = "cartItemKeys:"; // customer index
    private static final String CART_ITEM_INDEX_PREFIX = "cartItemIndex:"; // seller-product index

    @Autowired
    private RedisTemplate<String, CartItem> redisTemplate;
    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;
    // Constructs the Redis key for a CartItem
    private String key(CartItemId id) {
        return CART_ITEM_PREFIX + id.getCustomerId() + ":" + id.getSellerId() + ":" + id.getProductId();
    }

    private String keySet(int customerId) {
        return CART_ITEM_KEYS_PREFIX + customerId;
    }

    private String sellerProductIndexKey(int sellerId, int productId) {
        return CART_ITEM_INDEX_PREFIX + sellerId + ":" + productId;
    }

    @Override
    public Optional<CartItem> findById(CartItemId id) {
        CartItem item = redisTemplate.opsForValue().get(key(id));
        return Optional.ofNullable(item);
    }

    @Override
    public List<CartItem> findByCustomerId(int customerId) {
        Set<String> keys = stringRedisTemplate.opsForSet().members(keySet(customerId));
        if (keys == null || keys.isEmpty())
            return List.of();
        return keys.stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void saveCartItem(CartItem cartItem) {
        String itemKey = key(cartItem.getId());
        redisTemplate.opsForValue().set(itemKey, cartItem);
        stringRedisTemplate.opsForSet().add(keySet(cartItem.getId().getCustomerId()), itemKey);
        stringRedisTemplate.opsForSet().add(
                sellerProductIndexKey(cartItem.getId().getSellerId(), cartItem.getId().getProductId()), itemKey);
    }

    @Override
    public void deleteByCustomerId(int customerId) {
        Set<String> keys = stringRedisTemplate.opsForSet().members(keySet(customerId));
        if (keys != null && !keys.isEmpty()) {
            for (String itemKey : keys) {
                CartItem item = redisTemplate.opsForValue().get(itemKey);
                if (item != null) {
                    stringRedisTemplate.opsForSet().remove(
                            sellerProductIndexKey(item.getId().getSellerId(), item.getId().getProductId()), itemKey);
                }
            }
            redisTemplate.delete(keys);
            stringRedisTemplate.delete(keySet(customerId));
        }
    }

    @Override
    public List<CartItem> findBySellerIdAndProductId(int sellerId, int productId) {
        String indexKey = sellerProductIndexKey(sellerId, productId);
        Set<String> keys = stringRedisTemplate.opsForSet().members(indexKey);
        if (keys == null || keys.isEmpty())
            return List.of();
        return keys.stream()
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(CartItem item) {
        String itemKey = key(item.getId());
        redisTemplate.delete(itemKey);
        stringRedisTemplate.opsForSet().remove(keySet(item.getId().getCustomerId()), itemKey);
        stringRedisTemplate.opsForSet().remove(
                sellerProductIndexKey(item.getId().getSellerId(), item.getId().getProductId()), itemKey);
    }

    @Override
    public void deleteAll() {
        Set<String> allKeySets = stringRedisTemplate.keys(CART_ITEM_KEYS_PREFIX + "*");
        if (allKeySets != null) {
            for (String customerKeySet : allKeySets) {
                Set<String> itemKeys = stringRedisTemplate.opsForSet().members(customerKeySet);
                if (itemKeys != null) {
                    for (String itemKey : itemKeys) {
                        CartItem item = redisTemplate.opsForValue().get(itemKey);
                        if (item != null) {
                            stringRedisTemplate.opsForSet().remove(
                                    sellerProductIndexKey(item.getId().getSellerId(), item.getId().getProductId()),
                                    itemKey);
                        }
                    }
                    redisTemplate.delete(itemKeys);
                }
                stringRedisTemplate.delete(customerKeySet);
            }
        }
        Set<String> allSellerProductIndexes = stringRedisTemplate.keys(CART_ITEM_INDEX_PREFIX + "*");
        if (allSellerProductIndexes != null) {
            stringRedisTemplate.delete(allSellerProductIndexes);
        }
    }

    @Override
    public void saveAll(List<CartItem> items) {
        items.forEach(this::saveCartItem);
    }
}
package com.example.seller.config;

import com.example.seller.dto.SellerDashboard;
import com.example.seller.model.OrderEntry;
import com.example.seller.model.OrderSellerView;
import com.example.seller.model.Seller;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class SellerRedisConfig {
    public final ObjectMapper objectMapper;

    public SellerRedisConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * RedisTemplate is used to store {@link Seller} objects,
     * with key format like "seller:{sellerId}" (e.g., "seller:123").
     */
    @Bean
    public RedisTemplate<String, Seller> sellerRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Seller> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use the specified ObjectMapper and Seller.class, invoking the constructor
        // directly
        Jackson2JsonRedisSerializer<Seller> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Seller.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        return template;
    }

    /**
     * RedisTemplate is used to store OrderEntry objects.
     * For example, the key format can be designed as:
     * "orderEntry:{customerId}:{orderId}:{sellerId}:{productId}"
     */
    @Bean
    public RedisTemplate<String, OrderEntry> orderEntryRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, OrderEntry> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<OrderEntry> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,
                OrderEntry.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        return template;
    }

    /**
     * RedisTemplate is used to store OrderSellerView objects,
     * which represent the aggregated seller dashboard view.
     * The key format can be designed as:
     * "sellerDashboard:{sellerId}"
     */
    @Bean
    public RedisTemplate<String, SellerDashboard> orderSellerViewRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, SellerDashboard> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<SellerDashboard> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,
                SellerDashboard.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        return template;
    }
}

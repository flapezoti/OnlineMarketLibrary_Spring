package com.example.cart.kafka;

import com.example.cart.eventMessaging.AbstractCartConsumer;
import com.example.cart.service.ICartService;
import com.example.common.events.PriceUpdate;
import com.example.common.events.ProductUpdated;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.stereotype.Service;

@Service
public class JsonCartConsumer extends AbstractCartConsumer {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper objectMapper;

    public JsonCartConsumer(ICartService cartService, ObjectMapper objectMapper) {
        super(cartService);
        this.objectMapper = objectMapper;
    }

    @Override
    protected PriceUpdate deserializePriceUpdate(String payload) {
        try {
            return objectMapper.readValue(payload, PriceUpdate.class);
        } catch (Exception e) {
            logger.warn("Failed to deserialize PriceUpdate, payload will be aborted: {}", payload);
            return null;
        }
    }

    @Override
    protected ProductUpdated deserializeProductUpdated(String payload) {
        try {
            return objectMapper.readValue(payload, ProductUpdated.class);
        } catch (Exception e) {
            logger.warn("Failed to deserialize ProductUpdated, payload will be aborted: {}", payload);
            return null;
        }
    }
}
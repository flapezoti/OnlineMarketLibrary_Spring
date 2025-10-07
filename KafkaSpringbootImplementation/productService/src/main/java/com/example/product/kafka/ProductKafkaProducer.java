package com.example.product.kafka;

import com.example.common.driver.MarkStatus;
import com.example.common.driver.TransactionMark;
import com.example.common.driver.TransactionType;
import com.example.common.events.PriceUpdate;
import com.example.common.events.ProductUpdated;
import com.example.common.events.ReserveStock;
import com.example.common.messaging.IEventPublisher;
import com.example.product.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductKafkaProducer implements IEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ProductKafkaProducer.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Topic definition
    private static final String TRANSACTION_PRODUCT_UPDATE_TOPIC = "TransactionMark_UPDATE_PRODUCT";
    private static final String TRANSACTION_PRICE_UPDATE_TOPIC = "TransactionMark_PRICE_UPDATE";
    private static final String PRICE_UPDATE_TOPIC = "price-update-topic";
    private static final String PRODUCT_UPDATE_TOPIC = "product-update-topic";

    @Override
    public void publishEvent(String topic, Object... event) {
        sendAsJson(topic, event);
    }

    // Publish the price update event
    public void publishPriceUpdateEvent(PriceUpdate priceUpdate) {
        sendAsJson(PRICE_UPDATE_TOPIC, priceUpdate);
    }

    // Publish product update event
    public void publishProductUpdateEvent(ProductUpdated productUpdated) {
        sendAsJson(PRODUCT_UPDATE_TOPIC, productUpdated);
    }

    // Handle price update failure (poison message)
    public void publishPoisonPriceUpdateEvent(PriceUpdate priceUpdate) {
        TransactionMark transactionMark = new TransactionMark(
                priceUpdate.getInstanceId(),
                TransactionType.PRICE_UPDATE,
                priceUpdate.getSellerId(),
                MarkStatus.ERROR,
                "product");
        sendAsJson(TRANSACTION_PRICE_UPDATE_TOPIC, transactionMark);
    }

    // Handle product update failure (poison message)
    public void publishPoisonProductUpdateEvent(Product product) {
        TransactionMark transactionMark = new TransactionMark(
                product.getVersion(),
                TransactionType.UPDATE_PRODUCT,
                product.getSellerId(),
                MarkStatus.ERROR,
                "product");
        sendAsJson(TRANSACTION_PRODUCT_UPDATE_TOPIC, transactionMark);
    }

    // Unified method for serializing and publishing events
    private void sendAsJson(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(new ProducerRecord<>(topic, json));
        } catch (Exception e) {
            logger.error("Failed to serialize and send message to topic {}: {}", topic, e.getMessage());
        }
    }
}

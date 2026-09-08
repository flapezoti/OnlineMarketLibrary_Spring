package com.example.order.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderKafkaListener {

    private final JsonOrderConsumer jsonOrderConsumer;

    public OrderKafkaListener(JsonOrderConsumer jsonOrderConsumer) {
        this.jsonOrderConsumer = jsonOrderConsumer;
    }

    @KafkaListener(topics = "stock-confirmed-topic", groupId = "order-group")
    public void listenStockConfirmed(ConsumerRecord<String, String> record) {
        jsonOrderConsumer.handleStockConfirmed(record.value());
    }

    // The events below can arrive before the order exists (they are on a different topic than
    // the creator). A PredecessorNotReadyException from the handler is retried with backoff by
    // the DefaultErrorHandler (see KafkaRetryConfig) rather than being dropped.
    @KafkaListener(topics = "payment-confirmed-topic", groupId = "order-group")
    public void listenPaymentConfirmed(ConsumerRecord<String, String> record) {
        jsonOrderConsumer.handlePaymentConfirmed(record.value());
    }

    @KafkaListener(topics = "payment-failed-topic", groupId = "order-group")
    public void listenPaymentFailed(ConsumerRecord<String, String> record) {
        jsonOrderConsumer.handlePaymentFailed(record.value());
    }

    @KafkaListener(topics = "shipment-notification-topic", groupId = "order-group")
    public void listenShipmentNotification(ConsumerRecord<String, String> record) {
        jsonOrderConsumer.handleShipmentNotification(record.value());
    }
}

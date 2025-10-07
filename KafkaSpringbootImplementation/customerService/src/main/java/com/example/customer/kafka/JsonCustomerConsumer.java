package com.example.customer.kafka;

import com.example.common.events.DeliveryNotification;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;
import com.example.customer.eventMessaging.AbstractCustomerConsumer;
import com.example.customer.service.ICustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JsonCustomerConsumer extends AbstractCustomerConsumer {
    @Autowired
    private final ObjectMapper objectMapper;

    public JsonCustomerConsumer(ICustomerService customerService,ObjectMapper objectMapper) {
        super(customerService);
        this.objectMapper = objectMapper;
    }

    @Override
    protected DeliveryNotification deserializeDeliveryNotification(String payload) {
        try {
            return objectMapper.readValue(payload, DeliveryNotification.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize DeliveryNotification", e);
        }
    }

    @Override
    protected PaymentConfirmed deserializePaymentConfirmed(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentConfirmed.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize PaymentConfirmed", e);
        }
    }

    @Override
    protected PaymentFailed deserializePaymentFailed(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentFailed.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize PaymentFailed", e);
        }
    }
}
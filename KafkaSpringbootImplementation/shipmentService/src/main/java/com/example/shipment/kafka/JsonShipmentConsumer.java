package com.example.shipment.kafka;

import com.example.common.events.PaymentConfirmed;
import com.example.shipment.eventMessaging.AbstractShipmentConsumer;
import com.example.shipment.service.IShipmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class JsonShipmentConsumer extends AbstractShipmentConsumer {

    private final ObjectMapper objectMapper;

    public JsonShipmentConsumer(IShipmentService shipmentService,ObjectMapper objectMapper) {
        super(shipmentService);
        this.objectMapper = objectMapper;
    }

    @Override
    protected PaymentConfirmed deserializeInvoiceIssued(String payload) {
        try {
            // 反序列化 JSON 字符串为 PaymentConfirmed 对象
            return objectMapper.readValue(payload, PaymentConfirmed.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON deserialization failed for PaymentConfirmed", e);
        }
    }
}

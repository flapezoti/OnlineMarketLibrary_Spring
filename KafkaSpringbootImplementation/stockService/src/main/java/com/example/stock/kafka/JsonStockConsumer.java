package com.example.stock.kafka;

import com.example.common.events.ProductDelete;
import com.example.common.events.ProductUpdated;
import com.example.common.events.ReserveStock;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;
import com.example.stock.eventMessaging.AbstractStockConsumer;
import com.example.stock.service.IStockService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JsonStockConsumer extends AbstractStockConsumer {

    private final ObjectMapper mapper;  

    public JsonStockConsumer(IStockService stockService,
                             ObjectMapper mapper) {   // Spring will inject the global mapper 
        super(stockService);
        this.mapper = mapper;
    }

    @Override
    protected ProductUpdated deserializeProductUpdated(String payload) {
        try {
            return mapper.readValue(payload, ProductUpdated.class);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed for ProductUpdated", e);
        }
    }

    @Override
    protected ProductDelete deserializeProductDelete(String payload) {
        try {
            return mapper.readValue(payload, ProductDelete.class);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed for ProductDelete", e);
        }
    }

    @Override
    protected ReserveStock deserializeReserveStock(String payload) {
        try {
            return mapper.readValue(payload, ReserveStock.class);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed for ReserveStock", e);
        }
    }

    @Override
    protected PaymentConfirmed deserializePaymentConfirmed(String payload) {
        try {
            return mapper.readValue(payload, PaymentConfirmed.class);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed for PaymentConfirmed", e);
        }
    }

    @Override
    protected PaymentFailed deserializePaymentFailed(String payload) {
        try {
            return mapper.readValue(payload, PaymentFailed.class);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed for PaymentFailed", e);
        }
    }
}
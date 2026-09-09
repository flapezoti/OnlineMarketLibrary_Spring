package com.example.seller.config;

import com.example.common.messaging.PredecessorNotReadyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * When a listener throws {@link PredecessorNotReadyException} (an event arrived before the
 * event it causally depends on), retry it in place with a fixed backoff instead of dropping
 * it. After the attempts are exhausted the record is logged and skipped.
 */
@Configuration
public class KafkaRetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaRetryConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // 1s between attempts, up to 5 retries (~5s) before giving up on one record.
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, ex) -> logger.error("Event exhausted retries and was skipped: topic={} value={}",
                        record.topic(), record.value()),
                new FixedBackOff(1000L, 5L));
        handler.addRetryableExceptions(PredecessorNotReadyException.class);
        return handler;
    }
}

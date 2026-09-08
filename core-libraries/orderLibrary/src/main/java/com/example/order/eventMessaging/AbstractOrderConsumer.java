package com.example.order.eventMessaging;

import com.example.common.events.*;
import com.example.common.messaging.PredecessorNotReadyException;
import com.example.order.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for consuming order-related events from a message queue.
 *
 * <p>
 * This class defines the unified interface for processing all order-domain
 * events,
 * including {@link StockConfirmed}, {@link PaymentConfirmed},
 * {@link PaymentFailed},
 * and {@link ShipmentNotification}.
 * </p>
 *
 * <p>
 * It does not assume a specific messaging technology (Kafka, RabbitMQ, Dapr,
 * etc.).
 * Instead, it provides abstract deserialization methods that must be
 * implemented by
 * subclasses to convert raw payloads into event objects.
 * </p>
 *
 * <h3>How to use:</h3>
 * <ol>
 * <li>Extend this class to create your own consumer implementation (e.g.
 * {@code JsonOrderConsumer}).</li>
 * <li>Implement the {@code deserializeXxx(String payload)} methods according to
 * your message format.</li>
 * <li>Bind the {@code handleXxx(String payload)} methods to your message queue
 * listener or subscription handler.</li>
 * </ol>
 *
 * <h3>Example:</h3>
 * 
 * <pre>{@code
 * public class JsonOrderConsumer extends AbstractOrderConsumer {
 *     private final ObjectMapper objectMapper = new ObjectMapper();
 *
 *     public JsonOrderConsumer(IOrderService orderService) {
 *         super(orderService);
 *     } @Override
 *     protected StockConfirmed deserializeStockConfirmed(String payload) {
 *         try {
 *             return objectMapper.readValue(payload, StockConfirmed.class);
 *         } catch (Exception e) {
 *             logger.error("Failed to deserialize StockConfirmed: {}", e.getMessage());
 *             return null;
 *         }
 *     }
 *
 *     // Implement other deserializers (PaymentConfirmed, PaymentFailed, etc.)
 * }
 * }</pre>
 */
public abstract class AbstractOrderConsumer {

    private final IOrderService orderService;
    private final Logger logger = LoggerFactory.getLogger(AbstractOrderConsumer.class);

    public AbstractOrderConsumer(IOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Deserializes a raw message payload into a {@link StockConfirmed} event
     * object.
     * To be implemented by subclasses depending on the message format (e.g. JSON,
     * Avro).
     */
    protected abstract StockConfirmed deserializeStockConfirmed(String payload);

    /**
     * Converts the raw message payload into a {@link PaymentConfirmed} event
     * object. Need to be implemented.
     */
    protected abstract PaymentConfirmed deserializePaymentConfirmed(String payload);

    /**
     * Converts the raw message payload into a {@link PaymentFailed} event
     * object.Need to be implemented.
     */
    protected abstract PaymentFailed deserializePaymentFailed(String payload);

    /**
     * Converts the raw message payload into a {@link ShipmentNotification} event
     * object. Need to be implemented.
     */
    protected abstract ShipmentNotification deserializeShipmentNotification(String payload);

    /**
     * Handles {@link StockConfirmed} messages from the queue and delegates
     * processing to {@link IOrderService}.
     */
    public void handleStockConfirmed(String payload) {
        try {
            StockConfirmed stockConfirmed = deserializeStockConfirmed(payload);
            orderService.processStockConfirmed(stockConfirmed).join();
        } catch (Exception e) {
            logger.error("Failed to process StockConfirmed: {}", e.getMessage());
            try {
                StockConfirmed stockConfirmed = deserializeStockConfirmed(payload);
                CompletableFuture.runAsync(() -> orderService.processPoisonStockConfirmed(stockConfirmed));
            } catch (Exception inner) {
                logger.error("Failed to deserialize poison StockConfirmed: {}", inner.getMessage());
            }
        }
    }

    /**
     * Handles {@link PaymentConfirmed} messages from the queue and delegates
     * processing to {@link IOrderService}.
     */

    public void handlePaymentConfirmed(String payload) {
        try {
            PaymentConfirmed paymentConfirmed = deserializePaymentConfirmed(payload);
            orderService.processPaymentConfirmed(paymentConfirmed);
        } catch (PredecessorNotReadyException e) {
            throw e; // let the retry machinery handle out-of-order arrival
        } catch (Exception e) {
            logger.error("Failed to process PaymentConfirmed: {}", e.getMessage());
        }
    }

    /**
     * Handles {@link PaymentFailed} messages from the queue and delegates
     * processing to {@link IOrderService}.
     */

    public void handlePaymentFailed(String payload) {
        try {
            PaymentFailed paymentFailed = deserializePaymentFailed(payload);
            orderService.processPaymentFailed(paymentFailed);
        } catch (PredecessorNotReadyException e) {
            throw e; // let the retry machinery handle out-of-order arrival
        } catch (Exception e) {
            logger.error("Failed to process PaymentFailed: {}", e.getMessage());
        }
    }

    /**
     * Handles {@link ShipmentNotification} messages from the queue and delegates
     * processing to {@link IOrderService}.
     */

    public void handleShipmentNotification(String payload) {
        try {
            ShipmentNotification shipmentNotification = deserializeShipmentNotification(payload);
            orderService.processShipmentNotification(shipmentNotification);
        } catch (PredecessorNotReadyException e) {
            throw e; // let the retry machinery handle out-of-order arrival
        } catch (Exception e) {
            logger.error("Failed to process ShipmentNotification: {}", e.getMessage());
        }
    }
}

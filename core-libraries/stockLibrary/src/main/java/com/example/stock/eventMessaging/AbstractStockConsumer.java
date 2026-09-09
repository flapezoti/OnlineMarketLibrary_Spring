package com.example.stock.eventMessaging;

import com.example.common.events.ProductDelete;
import com.example.common.events.ProductUpdated;
import com.example.common.events.ReserveStock;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;
import com.example.stock.service.IStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract consumer class for handling stock-related events.
 *
 * <p>
 * This class defines the common event-handling logic for the Stock
 * microservice.
 * It is framework-agnostic (no Kafka/Spring dependencies) and delegates event
 * deserialization to its subclasses, which must implement the JSON-to-object
 * mapping.
 * </p>
 *
 * <p>
 * Each handle method corresponds to a specific event type and delegates the
 * processing logic to the {@link IStockService} instance provided at
 * construction.
 * </p>
 *
 * <p>
 * <b>To implement:</b> Extend this class and provide JSON deserialization logic
 * for each event type (e.g., using Jackson, Gson, or another parser).
 * </p>
 */
public abstract class AbstractStockConsumer {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final IStockService stockService;

    protected AbstractStockConsumer(IStockService stockService) {
        this.stockService = stockService;
    }

    /** Need to be implemented */
    protected abstract ProductUpdated deserializeProductUpdated(String payload);

    protected abstract ProductDelete deserializeProductDelete(String payload);

    protected abstract ReserveStock deserializeReserveStock(String payload);

    protected abstract PaymentConfirmed deserializePaymentConfirmed(String payload);

    protected abstract PaymentFailed deserializePaymentFailed(String payload);

    /**
     * Handles a ProductUpdated event.
     * <p>
     * If processing fails, it attempts to call
     * {@link IStockService#processPoisonProductUpdate(ProductUpdated)}.
     * </p>
     */
    public void handleProductUpdate(String payload) {
        try {
            ProductUpdated event = deserializeProductUpdated(payload);
            logger.info("Handling product update event.");
            stockService.processProductUpdate(event);
        } catch (Exception e) {

            ProductUpdated event = deserializeProductUpdated(payload);
            logger.error("Error processing product update: {}", e.getMessage());
            stockService.processPoisonProductUpdate(event);
        }
    }

    /**
     * Handles a ProductDelete event by disabling the corresponding stock item.
     */
    public void handleProductDelete(String payload) {
        try {
            ProductDelete event = deserializeProductDelete(payload);
            logger.info("Handling product delete event.");
            stockService.processProductDelete(event);
        } catch (Exception e) {
            logger.error("Error processing product delete: {}", e.getMessage());
        }
    }

    /**
     * Handles a ReserveStock event.
     * <p>
     * If processing fails, a poison event is passed to
     * {@link IStockService#processPoisonReserveStock(ReserveStock)}.
     * </p>
     */
    public void handleReserveStock(String payload) {
        try {
            ReserveStock event = deserializeReserveStock(payload);
            logger.info("Handling reserve stock event.");
            stockService.reserveStock(event);
            logger.info("Reserve stock processed successfully.");
        } catch (Exception e) {
            ReserveStock event = deserializeReserveStock(payload);
            logger.warn("Failed to process reserve stock event: {}", e.getMessage());
            stockService.processPoisonReserveStock(event);
        }
    }

    /**
     * Handles a PaymentConfirmed event, which indicates that stock reservations
     * should be confirmed and finalized.
     */
    public void handlePaymentConfirmed(String payload) {
        try {
            PaymentConfirmed event = deserializePaymentConfirmed(payload);
            logger.info("Handling payment confirmed event.");
            stockService.confirmReservation(event);
        } catch (Exception e) {
            PaymentConfirmed event = deserializePaymentConfirmed(payload);
            logger.error("Error processing payment confirmed: {}", e.getMessage());

        }
    }

    /**
     * Handles a PaymentFailed event, which indicates that reserved stock should be
     * released.
     */
    public void handlePaymentFailed(String payload) {
        try {
            PaymentFailed event = deserializePaymentFailed(payload);
            logger.info("Handling payment failed event.");
            stockService.cancelReservation(event);
        } catch (Exception e) {
            PaymentFailed event = deserializePaymentFailed(payload);
            logger.error("Error processing payment failed: {}", e.getMessage());
        }
    }
}

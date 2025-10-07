package com.example.shipment.eventMessaging;

import com.example.common.events.InvoiceIssued;
import com.example.common.events.PaymentConfirmed;
import com.example.shipment.service.IShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract consumer class for handling shipment-related events.
 *
 * <p>
 * This class defines the general processing flow for {@link PaymentConfirmed}
 * events without relying on any Kafka or Spring-specific annotations.
 * Subclasses are responsible for implementing JSON deserialization logic
 * from raw payloads into domain event objects.
 * </p>
 *
 * <p>
 * This design allows the same core logic to be reused across different
 * messaging infrastructures (e.g., Kafka, Dapr, or Azure Service Bus)
 * by simply providing different concrete consumers.
 * </p>
 */
public abstract class AbstractShipmentConsumer {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final IShipmentService shipmentService;

    /**
     * Converts a raw JSON payload into a {@link PaymentConfirmed} object.
     *
     * @param payload the raw JSON message body received from the message broker
     * @return deserialized {@link PaymentConfirmed} event
     */
    protected abstract PaymentConfirmed deserializeInvoiceIssued(String payload);

    public AbstractShipmentConsumer(IShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * Handles {@link PaymentConfirmed} events by invoking the shipment processing
     * logic.
     * <p>
     * If deserialization or processing fails, the message will be routed to the
     * "poison" handler (dead-letter equivalent).
     *
     * @param payload JSON message representing the {@link PaymentConfirmed} event
     */
    public void handlePaymentConfirmed(String payload) {
        try {
            PaymentConfirmed paymentConfirmed = deserializeInvoiceIssued(payload);
            logger.info("Received PaymentConfirmed event: {}", paymentConfirmed);
            shipmentService.processShipment(paymentConfirmed);
        } catch (Exception e) {
            PaymentConfirmed paymentConfirmed = deserializeInvoiceIssued(payload);
            logger.error("Error processing PaymentConfirmed event: {}", e.getMessage());
            shipmentService.processPoisonShipment(paymentConfirmed);
        }
    }
}

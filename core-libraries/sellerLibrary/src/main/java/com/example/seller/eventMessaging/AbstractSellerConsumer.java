package com.example.seller.eventMessaging;

import com.example.common.events.DeliveryNotification;
import com.example.common.events.InvoiceIssued;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;
import com.example.common.events.ShipmentNotification;
import com.example.common.messaging.PredecessorNotReadyException;
import com.example.seller.service.ISellerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for consuming seller-related events.
 *
 * <p>
 * This class defines a platform-agnostic structure for handling seller domain
 * events such as
 * {@link InvoiceIssued}, {@link PaymentConfirmed}, {@link PaymentFailed},
 * {@link ShipmentNotification}, and {@link DeliveryNotification}.
 * It provides standardized event-processing pipelines without any dependency on
 * frameworks like Spring or Kafka.
 * </p>
 *
 * <h3>Usage</h3>
 * <p>
 * To use this class, extend it and implement the deserialization methods
 * (e.g. {@code deserializeInvoiceIssued(String payload)}) to convert raw
 * payloads into domain objects.
 * These methods are framework-agnostic and can be implemented using your
 * preferred serialization library
 * (such as Jackson for JSON).
 * </p>
 *
 * <pre>
 * {@code
 * public class JsonSellerConsumer extends AbstractSellerConsumer {
 *     private final ObjectMapper mapper = new ObjectMapper();
 *
 *     public JsonSellerConsumer(ISellerService sellerService) {
 *         super(sellerService);
 *     } @Override
 *     protected InvoiceIssued deserializeInvoiceIssued(String payload) {
 *         return mapper.readValue(payload, InvoiceIssued.class);
 *     }
 *
 *     // Implement other deserialization methods...
 * }
 * }
 * </pre>
 *
 * <p>
 * After extending this class, you can connect the {@code handleXXX} methods
 * (e.g. {@link #handleInvoiceIssued(String)}) to your message queue consumer
 * logic.
 * </p>
 */
public abstract class AbstractSellerConsumer {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final ISellerService sellerService;

    protected AbstractSellerConsumer(ISellerService sellerService) {
        this.sellerService = sellerService;
    }

    protected abstract InvoiceIssued deserializeInvoiceIssued(String payload);

    protected abstract PaymentFailed deserializePaymentFailed(String payload);

    protected abstract ShipmentNotification deserializeShipmentNotification(String payload);

    protected abstract DeliveryNotification deserializeDeliveryNotification(String payload);

    protected abstract PaymentConfirmed deserializePaymentConfirmed(String payload);

    public void handleInvoiceIssued(String payload) {
        try {
            InvoiceIssued invoiceIssued = deserializeInvoiceIssued(payload);
            sellerService.processInvoiceIssued(invoiceIssued);
            logger.info("Processed InvoiceIssued event for orderId: {}", invoiceIssued.getOrderId());
        } catch (Exception e) {
            logger.error("Error processing InvoiceIssued event: {}", e.getMessage());
        }
    }

    public void handlePaymentFailed(String payload) {
        try {
            PaymentFailed paymentFailed = deserializePaymentFailed(payload);
            sellerService.processPaymentFailed(paymentFailed);
            logger.info("Processed PaymentFailed event for orderId: {}", paymentFailed.getOrderId());
        } catch (PredecessorNotReadyException e) {
            throw e; // let the retry machinery handle out-of-order arrival
        } catch (Exception e) {
            logger.error("Error processing PaymentFailed event: {}", e.getMessage());
        }
    }

    public void handleShipmentNotification(String payload) {
        try {
            ShipmentNotification shipmentNotification = deserializeShipmentNotification(payload);
            sellerService.processShipmentNotification(shipmentNotification);
            logger.info("Processed ShipmentNotification event for orderId: {}", shipmentNotification.getOrderId());
        } catch (PredecessorNotReadyException e) {
            throw e; // let the retry machinery handle out-of-order arrival
        } catch (Exception e) {
            logger.error("Error processing ShipmentNotification event: {}", e.getMessage());
        }
    }

    public void handleDeliveryNotification(String payload) {
        try {
            DeliveryNotification deliveryNotification = deserializeDeliveryNotification(payload);
            sellerService.processDeliveryNotification(deliveryNotification);
            logger.info("Processed DeliveryNotification event for orderId: {}", deliveryNotification.getOrderId());
        } catch (PredecessorNotReadyException e) {
            throw e; // let the retry machinery handle out-of-order arrival
        } catch (Exception e) {
            logger.error("Error processing DeliveryNotification event: {}", e.getMessage());
        }
    }

    public void handlePaymentConfirmed(String payload) {
        try {
            PaymentConfirmed paymentConfirmed = deserializePaymentConfirmed(payload);
            sellerService.processPaymentConfirmed(paymentConfirmed);
            logger.info("Processed PaymentConfirmed event for orderId: {}", paymentConfirmed.getOrderId());
        } catch (PredecessorNotReadyException e) {
            throw e; // let the retry machinery handle out-of-order arrival
        } catch (Exception e) {
            logger.error("Error processing PaymentConfirmed event: {}", e.getMessage());
        }
    }

}

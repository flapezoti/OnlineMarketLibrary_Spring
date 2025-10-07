package com.example.payment.eventMessaging;

import com.example.common.events.InvoiceIssued;
import com.example.payment.service.IPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Abstract base class for consuming payment-related events.
 *
 * <p>
 * This class defines the standard event-handling workflow for payment
 * processing,
 * specifically for the {@link InvoiceIssued} event type.
 * </p>
 *
 * <p>
 * To use this class, extend it in your platform-specific implementation (for
 * example,
 * in a Kafka + Spring Boot module) and implement the
 * {@link #deserializeInvoiceIssued(String)} method
 * to convert the raw message payload into an {@link InvoiceIssued} object.
 * </p>
 *
 * <p>
 * <b>Integration Notes:</b>
 * </p>
 * <ul>
 * <li>This class is platform-agnostic and does not depend on any message queue
 * framework.</li>
 * <li>Subclasses must implement message deserialization logic according to the
 * data format used
 * (e.g., JSON, Avro, Protobuf).</li>
 * <li>After extending, connect each handler (e.g.
 * {@link #handleInvoiceIssued(String)}) to your
 * message queue listener to process incoming events.</li>
 * <li>For a working reference implementation, see
 * <code>KafkaSpringbootImplementation/paymentService</code>.</li>
 * </ul>
 *
 * <p>
 * This abstraction ensures that your message consumption logic remains
 * decoupled
 * from business logic, which is encapsulated in the {@link PaymentService}.
 * </p>
 */
public abstract class AbstractPaymentConsumer {

    protected final IPaymentService paymentService;
    private final Logger logger = LoggerFactory.getLogger(AbstractPaymentConsumer.class);

    public AbstractPaymentConsumer(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    protected abstract InvoiceIssued deserializeInvoiceIssued(String payload);

    public void handleInvoiceIssued(String payload) {
        try {
            InvoiceIssued invoiceIssued = deserializeInvoiceIssued(payload);
            paymentService.processPayment(invoiceIssued);
        } catch (Exception e) {
            logger.error("Failed to process InvoiceIssued: {}", e.getMessage());
            try {
                InvoiceIssued invoiceIssued = deserializeInvoiceIssued(payload);
                CompletableFuture.runAsync(() -> paymentService.processPoisonPayment(invoiceIssued));
            } catch (Exception inner) {
                logger.error("Failed to deserialize poison InvoiceIssued: {}", inner.getMessage());
            }
        }
    }
}
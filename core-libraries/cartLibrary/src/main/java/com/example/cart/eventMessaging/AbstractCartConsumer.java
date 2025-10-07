package com.example.cart.eventMessaging;

import com.example.common.events.ProductUpdated;
import com.example.common.events.PriceUpdate;
import com.example.cart.model.ProductReplica;
import com.example.cart.service.ICartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for cart event consumers.
 * 
 * This class defines the common workflow for handling incoming event payloads:
 * - Deserialize the payload into domain events.
 * - Delegate processing to the cart service.
 * - Handle exceptions and send poison messages when needed.
 * 
 * Subclasses must implement deserialization logic for specific message formats
 * (e.g., JSON).
 */
public abstract class AbstractCartConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCartConsumer.class);

    protected final ICartService cartService;

    public AbstractCartConsumer(ICartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Deserializes the payload into a PriceUpdate event.
     * Subclasses must implement this method.
     *
     * @param payload the raw message payload
     * @return a PriceUpdate event, or null if deserialization fails
     */
    protected abstract PriceUpdate deserializePriceUpdate(String payload);

    /**
     * Deserializes the payload into a ProductUpdated event.
     * Subclasses must implement this method.
     *
     * @param payload the raw message payload
     * @return a ProductUpdated event, or null if deserialization fails
     */
    protected abstract ProductUpdated deserializeProductUpdated(String payload);

    /**
     * Handles an incoming price update event.
     * 
     * This method:
     * - Deserializes the payload.
     * - Invokes the cart service to process the update.
     * - Sends a poison message if processing fails.
     *
     * @param payload the raw message payload
     */
    public void handlePriceUpdate(String payload) {
        long start = System.currentTimeMillis();

        PriceUpdate priceUpdate = deserializePriceUpdate(payload);
        // long afterDeserialize = System.currentTimeMillis();
        // logger.info("deserializePriceUpdate cost: {} ms", afterDeserialize - start);

        if (priceUpdate == null) {
            // logger.warn("Received invalid price update message, sending abort
            // TransactionMark.");
            cartService.processPoisonPriceUpdate(null);
            // logger.info("processPoisonPriceUpdate (null) cost: {} ms",
            // System.currentTimeMillis() - afterDeserialize);
            return;
        }

        try {
            cartService.processPriceUpdate(priceUpdate);
            // logger.info("processPriceUpdate cost: {} ms", System.currentTimeMillis() -
            // afterDeserialize);
        } catch (Exception e) {
            // logger.error("Failed to process price update, publishing poison message: {}",
            // e.getMessage());
            cartService.processPoisonPriceUpdate(priceUpdate);
            // logger.info("processPoisonPriceUpdate (exception) cost: {} ms",
            // System.currentTimeMillis() - afterDeserialize);
        }

        logger.info("handlePriceUpdate total cost: {} ms", System.currentTimeMillis() - start);
    }

    /**
     * Handles an incoming product update event.
     *
     * This method:
     * - Deserializes the payload.
     * - Transforms it into a ProductReplica.
     * - Invokes the cart service to process the update.
     * - Sends a poison message if processing fails.
     *
     * @param payload the raw message payload
     */
    public void handleProductUpdate(String payload) {
        try {
            ProductUpdated productUpdate = deserializeProductUpdated(payload);
            logger.info("Product update received at cart, seller id is {}", productUpdate.getSellerId());

            // transform to ProductReplica
            ProductReplica productReplica = new ProductReplica();
            productReplica.setSellerId(productUpdate.getSellerId());
            productReplica.setProductId(productUpdate.getProductId());
            productReplica.setName(productUpdate.getName());
            productReplica.setPrice(productUpdate.getPrice());
            productReplica.setVersion(productUpdate.getVersion());
            productReplica.setActive(true);

            cartService.processProductUpdated(productReplica);
        } catch (Exception e) {
            logger.error("Failed to process product update, publishing poison message: {}", e.getMessage());
            cartService.processPoisonProductUpdated(null);
        }
    }
}
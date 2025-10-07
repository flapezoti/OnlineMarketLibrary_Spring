package com.example.product.service;

import com.example.common.events.PriceUpdate;
import com.example.common.events.ProductUpdated;
import com.example.product.model.Product;
import com.example.product.model.ProductId;
import com.example.product.repository.IProductRepository;
//import com.example.product.kafka.IKafkaProductProducer;
import com.example.common.messaging.IEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core implementation of {@link IProductService} that provides all
 * product-related operations
 * in a platform-independent manner.
 *
 * <p>
 * This class encapsulates the main business logic for managing products,
 * including creation,
 * updates, and price modifications. It also publishes domain events (such as
 * {@code ProductUpdated}
 * and {@code PriceUpdate}) to inform other microservices about changes.
 * </p>
 *
 * <h3>Usage</h3>
 * <p>
 * To use {@code ProductServiceCore}, you only need to provide:
 * </p>
 * <ul>
 * <li>An implementation of
 * {@link com.example.product.repository.IProductRepository} for product
 * persistence, and</li>
 * <li>An implementation of {@link com.example.common.messaging.IEventPublisher}
 * for event publishing.</li>
 * </ul>
 *
 * <p>
 * You do <b>not</b> need to reimplement this logic in platform modules.
 * For example, in a Kafka + Spring Boot implementation, simply instantiate this
 * class
 * and wire it with Spring-managed repository and publisher beans.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 * <li>Handle product creation and update operations</li>
 * <li>Propagate product and price update events across the system</li>
 * <li>Handle poison messages (failed updates) via transaction marks</li>
 * <li>Support repository cleanup and reset for testing or maintenance</li>
 * </ul>
 */
public class ProductServiceCore implements IProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceCore.class);

    private final IProductRepository productRepository;
    private final IEventPublisher eventPublisher;

    public ProductServiceCore(IProductRepository productRepository, IEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void processCreateProduct(Product product) {
        productRepository.saveProduct(product);
    }

    @Override
    public void processProductUpdate(Product product) {
        logger.info("Processing product update for productId: {}", product.getProductId());
        try {
            // Product existingProduct = productRedisTemplate.opsForValue().get(productKey);
            // // Step 2: If Redis doesn't have the product, fallback to MySQL
            // if (existingProduct == null) {
            // existingProduct = productRepository.findById(product.getId())
            // .orElseThrow(() -> new RuntimeException("Product not found: " +
            // product.getId()));
            // logger.info("Product loaded from MySQL: {}", product.getProductId());
            // } else {
            // logger.info("Product loaded from Redis: {}", product.getProductId());
            // }

            Product existingProduct = productRepository.findById(product.getId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + product.getId()));

            productRepository.saveProduct(product);
            logger.info("Product updated successfully for productId: {}", product.getProductId());

            ProductUpdated productUpdated = new ProductUpdated(
                    product.getSellerId(),
                    product.getProductId(),
                    product.getName(),
                    product.getSku(),
                    product.getCategory(),
                    product.getDescription(),
                    product.getPrice(),
                    product.getFreightValue(),
                    product.getStatus(),
                    product.getVersion());

            eventPublisher.publishEvent("product-update-topic", productUpdated);
            logger.info("Product update event sent for productId: {}", product.getProductId());
        } catch (Exception e) {
            logger.error("Error processing product update for productId: {}. Error: {}",
                    product.getProductId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void processPoisonProductUpdate(Product product) {
        eventPublisher.publishEvent("TransactionMark_UPDATE_PRODUCT", product);
    }

    /**
     * Updates product pricing information and publishes a {@code PriceUpdate}
     * event.
     */
    @Override
    public void processPriceUpdate(PriceUpdate priceUpdate) {
        Product existingProduct = productRepository
                .findById(new ProductId(priceUpdate.getSellerId(), priceUpdate.getProductId()))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // update
        existingProduct.setPrice(priceUpdate.getPrice());
        existingProduct.setVersion(priceUpdate.getVersion());
        productRepository.saveProduct(existingProduct);

        // send event
        eventPublisher.publishEvent("price-update-topic", priceUpdate);
    }

    @Override
    public void processPoisonPriceUpdate(PriceUpdate priceUpdate) {
        eventPublisher.publishEvent("TransactionMark_PRICE_UPDATE", priceUpdate);
    }

    @Override
    public void cleanup() {
        productRepository.deleteAll();
    }

    @Override
    public void reset() {
        productRepository.reset();
    }
}

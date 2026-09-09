package com.example.product.service;

import com.example.common.events.PriceUpdate;
import com.example.product.model.Product;

/**
 * Defines the contract for all product-related operations in the core library.
 *
 * <p>
 * This interface specifies the fundamental product handling functions such as
 * product creation, updates, and reacting to {@link PriceUpdate} events.
 * It serves as a platform-agnostic abstraction, independent of any specific
 * message queue, database, or framework.
 * </p>
 *
 * <h3>Usage</h3>
 * <p>
 * Users are <b>not required</b> to reimplement this interface manually.
 * The core library already provides a full implementation via
 * {@link com.example.product.service.ProductServiceCore}.
 * To use it, simply instantiate {@code ProductServiceCore} and inject:
 * </p>
 * <ul>
 * <li>An implementation of {@code IProductRepository}, and</li>
 * <li>An implementation of {@code IEventPublisher}.</li>
 * </ul>
 *
 * <p>
 * For example, in a Kafka + Spring Boot environment, you can directly use
 * the provided implementations from the
 * <b>KafkaSpringbootImplementation/productService</b>
 * module as dependencies.
 * </p>
 *
 * <h3>Extension</h3>
 * <p>
 * If custom platform logic is needed (e.g., additional logging, metrics, or
 * tracing),
 * you may subclass {@code ProductServiceCore} instead of reimplementing this
 * interface.
 * </p>
 */
public interface IProductService {

    void processCreateProduct(Product product);

    void processProductUpdate(Product product);

    void processDeleteProduct(int sellerId, int productId, String instanceId);

    void processPoisonProductUpdate(Product product);

    void processPriceUpdate(PriceUpdate priceUpdate);

    void processPoisonPriceUpdate(PriceUpdate priceUpdate);

    void cleanup();

    void reset();
}

package com.example.cart.service;

import com.example.cart.model.*;
import com.example.cart.repository.*;
import com.example.common.driver.*;
import com.example.common.entities.CartStatus;
import com.example.common.events.*;
import com.example.common.messaging.IEventPublisher;
import com.example.common.requests.CustomerCheckout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Core implementation of ICartService
 * Encapsulates the main business logic for managing shopping carts
 * This class coordinates repositories and event publishing to maintain cart
 * state consistency
 * You can find the definition of all the method from the "Online MarketPlace
 * paper"
 */
public class CartServiceCore implements ICartService {
    private final ICartRepository cartRepository;
    private final ICartItemRepository cartItemRepository;
    private final IProductReplicaRepository productReplicaRepository;
    private final IEventPublisher eventPublisher;

    private static final Logger logger = LoggerFactory.getLogger(CartServiceCore.class);

    /** Tolerance for comparing float prices when reconciling a cart item against its replica. */
    private static final float PRICE_EPSILON = 0.001f;

    public CartServiceCore(ICartRepository cartRepository,
            ICartItemRepository cartItemRepository,
            IProductReplicaRepository productReplicaRepository,
            IEventPublisher eventPublisher) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productReplicaRepository = productReplicaRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Retrieves a cart by customer ID.
     *
     * @param customerId the unique identifier of the customer
     * @return the cart belonging to the customer, or null if not found
     */
    @Override
    public Cart getCart(int customerId) {
        return cartRepository.findByCustomerId(customerId);
    }

    /**
     * Removes a specific item from the customer's cart.
     *
     * @param customerId the customer ID
     * @param productId  the product ID
     * @param sellerId   the seller ID
     */
    @Override
    public void removeItem(int customerId, int productId, int sellerId) {
        Cart cart = cartRepository.findByCustomerId(customerId);
        if (cart != null) {
            CartItemId itemId = new CartItemId(customerId, sellerId, productId);
            Optional<CartItem> item = cartItemRepository.findById(itemId);
            if (item.isPresent()) {
                cartItemRepository.delete(item.get());
            }
        }
    }

    /**
     * Resets the cart to OPEN state and optionally clears items.
     *
     * @param cart       the cart to reset
     * @param cleanItems whether to remove all items from the cart
     */
    @Override
    public void seal(Cart cart, boolean cleanItems) {
        cart.setStatus(CartStatus.OPEN);
        if (cleanItems) {
            cartItemRepository.deleteByCustomerId(cart.getCustomerId());
        }
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.saveCart(cart);
    }

    /**
     * Handles customer checkout:
     * - Validates the cart
     * - Marks cart as submitted
     * - Publishes a ReserveStock event
     *
     * @param customerCheckout customer checkout request
     * @throws RuntimeException if cart is missing, already submitted, or empty
     */
    @Override
    public void notifyCheckout(CustomerCheckout customerCheckout) {
        Cart cart = cartRepository.findByCustomerId(customerCheckout.getCustomerId());
        if (cart == null) {
            throw new RuntimeException("Cart not found for customer: " + customerCheckout.getCustomerId());
        }

        if (cart.getStatus() == CartStatus.CHECKOUT_SENT) {
            throw new RuntimeException("Cart already submitted for checkout: " + customerCheckout.getCustomerId());
        }

        List<CartItem> items = cartItemRepository.findByCustomerId(customerCheckout.getCustomerId());
        if (items.isEmpty()) {
            throw new RuntimeException("Cart has no items: " + customerCheckout.getCustomerId());
        }

        // Reconcile each cart item against the product replica before the cart is sealed.
        // A missing replica means the price cannot be verified, so the checkout is refused and
        // the cart stays OPEN for a retry. When the replica price differs, adopt it; a price
        // drop is added to the item's discount.
        for (CartItem item : items) {
            ProductReplica replica = productReplicaRepository.findByProductReplicaId(
                    new ProductReplicaId(item.getSellerId(), item.getProductId()));
            if (replica == null) {
                throw new RuntimeException("No product replica for " + item.getSellerId() + "-"
                        + item.getProductId() + "; cannot verify checkout price for customer "
                        + customerCheckout.getCustomerId());
            }
            float oldPrice = item.getUnitPrice();
            float newPrice = replica.getPrice();
            if (Math.abs(newPrice - oldPrice) > PRICE_EPSILON) {
                item.setUnitPrice(newPrice);
                if (oldPrice - newPrice > 0f) {
                    item.setVoucher(item.getVoucher() + (oldPrice - newPrice));
                }
            }
        }
        cartItemRepository.saveAll(items);

        cart.setStatus(CartStatus.CHECKOUT_SENT);
        cartRepository.saveCart(cart);

        List<com.example.common.entities.CartItem> cartItems = items.stream()
                .map(i -> {
                    com.example.common.entities.CartItem cartItem = new com.example.common.entities.CartItem();
                    cartItem.setSellerId(i.getSellerId());
                    cartItem.setProductId(i.getProductId());
                    cartItem.setProductName(i.getProductName() == null ? "" : i.getProductName());
                    cartItem.setUnitPrice(i.getUnitPrice());
                    cartItem.setFreightValue(i.getFreightValue());
                    cartItem.setQuantity(i.getQuantity());
                    cartItem.setVersion(i.getVersion());
                    cartItem.setVoucher(i.getVoucher());
                    return cartItem;
                })
                .collect(Collectors.toList());

        LocalDateTime timestamp = LocalDateTime.now();
        ReserveStock checkout = new ReserveStock(timestamp, customerCheckout, cartItems,
                customerCheckout.getInstanceId());
        eventPublisher.publishEvent("reserve-stock-topic", checkout);
    }

    /**
     * Deletes all carts, cart items, and product replicas.
     */
    @Override
    public void cleanCart() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productReplicaRepository.deleteAll();
    }

    /**
     * Processes a product update event.
     * Creates or updates the product replica in the repository.
     *
     * @param productUpdated the updated product replica
     */
    @Override
    public void processProductUpdated(ProductReplica productUpdated) {
        ProductReplica existingProduct = productReplicaRepository.findByProductReplicaId(
                new ProductReplicaId(productUpdated.getSellerId(), productUpdated.getProductId()));

        if (existingProduct == null) {
            existingProduct = new ProductReplica();
            existingProduct.setSellerId(productUpdated.getSellerId());
            existingProduct.setProductId(productUpdated.getProductId());
            existingProduct.setCreatedAt(LocalDateTime.now());
        } else if (!isNewerVersion(productUpdated.getVersion(), existingProduct.getVersion())) {
            logger.warn("Stale/duplicate product update for {}-{} (incoming v{}, replica v{}); skipping",
                    productUpdated.getSellerId(), productUpdated.getProductId(),
                    productUpdated.getVersion(), existingProduct.getVersion());
            return;
        }

        existingProduct.setName(productUpdated.getName());
        existingProduct.setPrice(productUpdated.getPrice());
        existingProduct.setVersion(productUpdated.getVersion());
        existingProduct.setActive(productUpdated.isActive());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        productReplicaRepository.saveProductReplica(existingProduct);
    }

    /**
     * Processes a price update event.
     * Updates product replica and adjusts cart items' unit prices.
     * Marks event as ABORT if product is not found.
     *
     * @param priceUpdate the price update event
     */
    @Override
    public void processPriceUpdate(PriceUpdate priceUpdate) {
        ProductReplica product = productReplicaRepository.findByProductReplicaId(
                new ProductReplicaId(priceUpdate.getSellerId(), priceUpdate.getProductId()));

        // slow, need to create exception
        // if (product == null) {
        // throw new IllegalArgumentException("Product in cart replica not found: " +
        // priceUpdate.getSellerId() + "-"
        // + priceUpdate.getProductId());
        // }

        // faster, change to if
        if (product == null) {
            logger.warn("Product in cart replica not found: {}-{}, aborting PriceUpdate.",
                    priceUpdate.getSellerId(), priceUpdate.getProductId());
            processPoisonPriceUpdate(priceUpdate);
            return;
        }

        if (!isNewerVersion(priceUpdate.getVersion(), product.getVersion())) {
            logger.warn("Stale/duplicate PriceUpdate for {}-{} (incoming v{}, replica v{}); skipping writes",
                    priceUpdate.getSellerId(), priceUpdate.getProductId(),
                    priceUpdate.getVersion(), product.getVersion());
            eventPublisher.publishEvent("TransactionMark_PRICE_UPDATE", new TransactionMark(
                    priceUpdate.getInstanceId(), TransactionType.PRICE_UPDATE,
                    priceUpdate.getSellerId(), MarkStatus.SUCCESS, "cart"));
            return;
        }

        product.setPrice(priceUpdate.getPrice());
        product.setVersion(priceUpdate.getVersion());
        productReplicaRepository.saveProductReplica(product);

        List<CartItem> cartItems = cartItemRepository.findBySellerIdAndProductId(
                priceUpdate.getSellerId(), priceUpdate.getProductId());

        for (CartItem cartItem : cartItems) {
            float oldPrice = cartItem.getUnitPrice();
            cartItem.setUnitPrice(priceUpdate.getPrice());
            cartItem.setVoucher(cartItem.getVoucher() + (oldPrice - priceUpdate.getPrice()));
        }
        cartItemRepository.saveAll(cartItems);

        TransactionMark transactionMark = new TransactionMark(
                priceUpdate.getInstanceId(),
                TransactionType.PRICE_UPDATE,
                priceUpdate.getSellerId(),
                MarkStatus.SUCCESS,
                "cart");

        eventPublisher.publishEvent("TransactionMark_PRICE_UPDATE", transactionMark);
    }

    /**
     * True if {@code incoming} is a strictly newer product version than {@code current}.
     * A null/blank current version means the replica has none yet, so anything is newer.
     * Versions that do not parse as integers are treated as not-newer (update skipped).
     */
    private static boolean isNewerVersion(String incoming, String current) {
        if (current == null || current.isBlank()) {
            return true;
        }
        try {
            return Long.parseLong(incoming.trim()) > Long.parseLong(current.trim());
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("Unparseable product version(s): incoming='{}', current='{}'; skipping update",
                    incoming, current);
            return false;
        }
    }

    /**
     * Clears all cart data and resets product replica repository.
     */
    @Override
    public void reset() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productReplicaRepository.reset();
    }

    /**
     * Handles poison (invalid/unprocessable) product update events.
     *
     * @param productUpdated the invalid product update event (may be null)
     */
    @Override
    public void processPoisonProductUpdated(ProductUpdated productUpdated) {
        TransactionMark transactionMark;

        if (productUpdated != null) {
            transactionMark = new TransactionMark(
                    productUpdated.getVersion(),
                    TransactionType.UPDATE_PRODUCT,
                    productUpdated.getSellerId(),
                    MarkStatus.ABORT,
                    "cart");
        } else {
            transactionMark = new TransactionMark(
                    "unknown", // 用默认字符串
                    TransactionType.UPDATE_PRODUCT,
                    -1, // 默认 sellerId
                    MarkStatus.ABORT,
                    "cart");
        }

        eventPublisher.publishEvent("TransactionMark_UPDATE_PRODUCT", transactionMark);
    }

    /**
     * Handles poison (invalid/unprocessable) price update events.
     * Publishes ABORT transaction mark asynchronously.
     *
     * @param priceUpdated the invalid price update event (may be null)
     */
    @Override
    public void processPoisonPriceUpdate(PriceUpdate priceUpdated) {
        TransactionMark transactionMark;

        if (priceUpdated != null) {
            transactionMark = new TransactionMark(
                    priceUpdated.getInstanceId(),
                    TransactionType.PRICE_UPDATE,
                    priceUpdated.getSellerId(),
                    MarkStatus.ABORT,
                    "cart");
        } else {
            transactionMark = new TransactionMark(
                    "unknown",
                    TransactionType.PRICE_UPDATE,
                    -1,
                    MarkStatus.ABORT,
                    "cart");
        }
        // asyn
        CompletableFuture.runAsync(() -> eventPublisher.publishEvent("TransactionMark_PRICE_UPDATE", transactionMark));
    }

    /**
     * Handles poison checkout attempts.
     *
     * @param customerCheckout the invalid checkout request (may be null)
     * @param status           the mark status (e.g. ABORT)
     */
    @Override
    public void processPoisonCheckout(CustomerCheckout customerCheckout, MarkStatus status) {
        TransactionMark transactionMark;

        if (customerCheckout != null) {
            transactionMark = new TransactionMark(
                    customerCheckout.getInstanceId(),
                    TransactionType.CUSTOMER_SESSION,
                    customerCheckout.getCustomerId(),
                    status,
                    "cart");
        } else {
            transactionMark = new TransactionMark(
                    "unknown",
                    TransactionType.CUSTOMER_SESSION,
                    -1,
                    status,
                    "cart");
        }

        eventPublisher.publishEvent("TransactionMark_CUSTOMER_SESSION", transactionMark);
    }
}
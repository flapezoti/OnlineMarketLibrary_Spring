package com.example.seller.service;

import com.example.common.entities.OrderItem;
import com.example.common.entities.OrderStatus;
import com.example.common.entities.PackageStatus;
import com.example.common.entities.ShipmentStatus;
import com.example.common.events.DeliveryNotification;
import com.example.common.events.InvoiceIssued;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;
import com.example.common.events.ShipmentNotification;
import com.example.seller.dto.SellerDashboard;
//import com.example.seller.infra.SellerConfig;
import com.example.seller.model.OrderEntry;
import com.example.seller.model.OrderEntryId;
import com.example.seller.model.OrderSellerView;
import com.example.common.messaging.PredecessorNotReadyException;
import com.example.seller.repository.IOrderEntryRepository;
import com.example.seller.repository.ISellerRepository;
import com.example.seller.repository.IOrderSellerViewRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Core implementation of {@link ISellerService}.
 *
 * <p>
 * This class encapsulates the business logic for the Seller microservice,
 * handling
 * domain events such as {@code InvoiceIssued}, {@code PaymentConfirmed}, and
 * {@code ShipmentNotification}. It interacts with multiple repositories to
 * persist order state and maintain a consistent seller dashboard view.
 * </p>
 *
 * <p>
 * Unlike platform-specific implementations, this core class contains no
 * framework dependencies (e.g., Spring annotations or Kafka bindings),
 * ensuring full portability across Pub/Sub and RPC platforms.
 * </p>
 */
public class SellerServiceCore implements ISellerService {

    private final ISellerRepository sellerRepository;
    private final IOrderEntryRepository orderEntryRepository;
    private final IOrderSellerViewRepository orderSellerViewRepository;
    private final IMaterializedViewService materializedViewService;
    // private final SellerConfig config;
    private final Logger logger = LoggerFactory.getLogger(SellerServiceCore.class);

    public SellerServiceCore(ISellerRepository sellerRepository,
            IOrderEntryRepository orderEntryRepository,
            IOrderSellerViewRepository orderSellerViewRepository,
            IMaterializedViewService materializedViewService) {
        // SellerConfig config
        this.sellerRepository = sellerRepository;
        this.orderEntryRepository = orderEntryRepository;
        this.orderSellerViewRepository = orderSellerViewRepository;
        this.materializedViewService = materializedViewService;
        // this.config = config;
    }

    /**
     * Handles {@link InvoiceIssued} event.
     * <p>
     * Each {@link OrderItem} in the event payload is transformed into an
     * {@link OrderEntry}
     * and persisted to the repository. The entry is marked as {@code INVOICED}.
     * </p>
     */
    @Override
    public void processInvoiceIssued(InvoiceIssued invoiceIssued) {
        List<OrderItem> items = invoiceIssued.getItems();
        for (OrderItem item : items) {
            OrderEntryId pk = new OrderEntryId(
                    invoiceIssued.getCustomer().getCustomerId(),
                    invoiceIssued.getOrderId(),
                    item.getSellerId(),
                    item.getProductId());

            OrderEntry orderEntry = new OrderEntry();
            orderEntry.setId(pk);
            orderEntry.setProductName(item.getProductName());
            orderEntry.setUnitPrice(item.getUnitPrice());
            orderEntry.setQuantity(item.getQuantity());
            orderEntry.setTotalAmount(item.getTotalAmount());
            orderEntry.setTotalInvoice(item.getTotalAmount() + item.getFreightValue());
            orderEntry.setFreightValue(item.getFreightValue());
            orderEntry.setOrderStatus(OrderStatus.INVOICED);
            orderEntry.setNaturalKey(String.format("%d_%d",
                    invoiceIssued.getCustomer().getCustomerId(),
                    invoiceIssued.getOrderId()));
            orderEntryRepository.save(orderEntry);
        }
    }

    /**
     * Handles {@link ShipmentNotification} event.
     * <p>
     * Updates order entries based on the shipment status, adjusting order
     * and delivery statuses accordingly.
     * </p>
     */
    @Override
    public void processShipmentNotification(ShipmentNotification shipmentNotification) {
        logger.info("Processing ShipmentNotification for Order ID: {}, Customer ID: {}, Status: {}",
                shipmentNotification.getOrderId(),
                shipmentNotification.getCustomerId(),
                shipmentNotification.getStatus());

        List<OrderEntry> entries = orderEntryRepository.findByCustomerIdAndOrderId(
                shipmentNotification.getCustomerId(), shipmentNotification.getOrderId());
        if (entries.isEmpty()) {
            throw new PredecessorNotReadyException("No order entries for "
                    + shipmentNotification.getCustomerId() + "-" + shipmentNotification.getOrderId()
                    + " yet (InvoiceIssued not processed)");
        }

        for (OrderEntry entry : entries) {
            if (shipmentNotification.getStatus() == ShipmentStatus.APPROVED) {
                entry.setOrderStatus(OrderStatus.READY_FOR_SHIPMENT);
                entry.setShipmentDate(shipmentNotification.getEventDate());
                entry.setDeliveryStatus(PackageStatus.READY_TO_SHIP);
            } else if (shipmentNotification.getStatus() == ShipmentStatus.DELIVERY_IN_PROGRESS) {
                entry.setOrderStatus(OrderStatus.IN_TRANSIT);
                entry.setDeliveryStatus(PackageStatus.SHIPPED);
            } else if (shipmentNotification.getStatus() == ShipmentStatus.CONCLUDED) {
                entry.setOrderStatus(OrderStatus.DELIVERED);
            }
        }
        orderEntryRepository.saveAll(entries);
        logger.info("Order entries saved successfully for Order ID: {}", shipmentNotification.getOrderId());
    }

    /**
     * Handles {@link DeliveryNotification} event.
     * <p>
     * Updates delivery information such as package ID, delivery date, and status.
     * </p>
     */
    @Override
    public void processDeliveryNotification(DeliveryNotification deliveryNotification) {
        Optional<OrderEntry> optionalOrderEntry = orderEntryRepository.findById(new OrderEntryId(
                deliveryNotification.getCustomerId(),
                deliveryNotification.getOrderId(),
                deliveryNotification.getSellerId(),
                deliveryNotification.getProductId()));

        OrderEntry orderEntry = optionalOrderEntry.orElseThrow(() -> new PredecessorNotReadyException(
                "[ProcessDeliveryNotification] No order entry for order id "
                        + deliveryNotification.getOrderId() + " product id "
                        + deliveryNotification.getProductId() + " yet (InvoiceIssued not processed)"));

        orderEntry.setPackageId(deliveryNotification.getPackageId());
        orderEntry.setDeliveryDate(deliveryNotification.getDeliveryDate());
        orderEntry.setDeliveryStatus(deliveryNotification.getStatus());

        orderEntryRepository.save(orderEntry);
    }

    /**
     * Handles {@link PaymentConfirmed} event.
     * <p>
     * Marks related order entries as {@code PAYMENT_PROCESSED} once payment is
     * confirmed.
     * </p>
     */
    @Override
    public void processPaymentConfirmed(PaymentConfirmed paymentConfirmed) {
        List<OrderEntry> entries = orderEntryRepository.findByCustomerIdAndOrderId(
                paymentConfirmed.getCustomer().getCustomerId(),
                paymentConfirmed.getOrderId());
        if (entries.isEmpty()) {
            throw new PredecessorNotReadyException("No order entries for "
                    + paymentConfirmed.getCustomer().getCustomerId() + "-" + paymentConfirmed.getOrderId()
                    + " yet (InvoiceIssued not processed)");
        }
        for (OrderEntry entry : entries) {
            entry.setOrderStatus(OrderStatus.PAYMENT_PROCESSED);
        }
        orderEntryRepository.saveAll(entries);
    }

    /**
     * Handles {@link PaymentFailed} event.
     * <p>
     * Marks affected orders as {@code PAYMENT_FAILED}. Logs a warning if no entries
     * are found.
     * </p>
     */
    @Override
    public void processPaymentFailed(PaymentFailed paymentFailed) {
        logger.info("Processing PaymentFailed event: {}", paymentFailed);
        List<OrderEntry> entries = orderEntryRepository.findByCustomerIdAndOrderId(
                paymentFailed.getCustomer().getCustomerId(),
                paymentFailed.getOrderId());
        if (entries.isEmpty()) {
            throw new PredecessorNotReadyException("No order entries for "
                    + paymentFailed.getCustomer().getCustomerId() + "-" + paymentFailed.getOrderId()
                    + " yet (InvoiceIssued not processed)");
        }
        for (OrderEntry entry : entries) {
            entry.setOrderStatus(OrderStatus.PAYMENT_FAILED);
        }
        orderEntryRepository.saveAll(entries);
        logger.info("PaymentFailed processing completed.");
    }

    private static final Set<OrderStatus> ONGOING_STATUSES = Set.of(
            OrderStatus.INVOICED,
            OrderStatus.PAYMENT_PROCESSED,
            OrderStatus.READY_FOR_SHIPMENT,
            OrderStatus.IN_TRANSIT);

    /**
     * Queries the seller dashboard: the discriminated list of order entries plus the
     * aggregate over the seller's ongoing orders.
     * <p>
     * Both halves are computed from a single read of {@code order_entry}, so they always
     * reflect the same snapshot of the application state.
     */
    @Override
    public SellerDashboard queryDashboard(int sellerId) {
        try {
            List<OrderEntry> orderEntries = orderEntryRepository.findAllBySellerId(sellerId);
            OrderSellerView sellerView = aggregateOngoing(sellerId, orderEntries);
            return new SellerDashboard(sellerView, orderEntries);
        } catch (Exception e) {
            logger.error("Error querying dashboard for sellerId {}: {}", sellerId, e.getMessage(), e);
            throw new RuntimeException("Failed to query seller dashboard", e);
        }
    }

    /**
     * Folds the seller's <em>ongoing</em> order entries (not concluded, not payment-failed)
     * into the aggregate the dashboard exposes.
     * <p>
     * Note: {@code totalItems} / {@code totalIncentive} are summed as stored on the entry;
     * {@code processInvoiceIssued} does not currently populate them, so they stay 0 until
     * that is addressed.
     */
    private OrderSellerView aggregateOngoing(int sellerId, List<OrderEntry> entries) {
        OrderSellerView view = new OrderSellerView();
        view.setSellerId(sellerId);
        Set<Integer> orderIds = new HashSet<>();
        for (OrderEntry e : entries) {
            if (!ONGOING_STATUSES.contains(e.getOrderStatus())) {
                continue;
            }
            orderIds.add(e.getOrderId());
            view.setCountItems(view.getCountItems() + e.getQuantity());
            view.setTotalAmount(view.getTotalAmount() + e.getTotalAmount());
            view.setTotalFreight(view.getTotalFreight() + e.getFreightValue());
            view.setTotalInvoice(view.getTotalInvoice() + e.getTotalInvoice());
            view.setTotalItems(view.getTotalItems() + e.getTotalItems());
            view.setTotalIncentive(view.getTotalIncentive() + e.getTotalIncentive());
        }
        view.setCountOrders(orderIds.size());
        return view;
    }

    @Override
    public void cleanup() {
        sellerRepository.deleteAll();
        orderEntryRepository.deleteAll();
    }

    @Override
    public void reset() {
        orderEntryRepository.deleteAll();
    }
}
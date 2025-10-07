package com.example.shipment.service;

import com.example.common.entities.OrderItem;
import com.example.common.entities.PackageStatus;
import com.example.common.entities.ShipmentStatus;
import com.example.common.events.DeliveryNotification;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.ShipmentNotification;
import com.example.common.driver.MarkStatus;
import com.example.common.driver.TransactionMark;
import com.example.common.driver.TransactionType;
//import com.example.shipment.config.IShipmentConfig;
import com.example.common.messaging.IEventPublisher;
import com.example.shipment.model.Package;
import com.example.shipment.model.PackageId;
import com.example.shipment.model.Shipment;
import com.example.shipment.model.ShipmentId;
import com.example.shipment.repository.IPackageRepository;
import com.example.shipment.repository.IShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core implementation of {@link IShipmentService}, handling shipment creation,
 * updates, and event publishing.
 *
 * <p>
 * This class encapsulates all business logic related to shipments and packages.
 * It is platform-agnostic and can be reused across multiple implementations
 * (e.g., Spring Boot, Orleans, or Dapr) as long as the required repository
 * interfaces and {@link IEventPublisher} are provided.
 * </p>
 *
 * <p>
 * <b>To use:</b> Instantiate this class by providing implementations of:
 * <ul>
 * <li>{@link IShipmentRepository}</li>
 * <li>{@link IPackageRepository}</li>
 * <li>{@link IEventPublisher}</li>
 * </ul>
 * These dependencies should be implemented in your own infrastructure layer
 * (for example,
 * in <code>KafkaSpringbootImplementation</code>).
 * </p>
 *
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 * <li>Generate shipment and package records upon payment confirmation</li>
 * <li>Publish shipment and delivery events</li>
 * <li>Handle poison messages for fault recovery</li>
 * <li>Update shipment state transitions and send related events</li>
 * <li>Provide data cleanup operations</li>
 * </ul>
 */
public class ShipmentServiceCore implements IShipmentService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentServiceCore.class);

    private final IShipmentRepository shipmentRepository;
    private final IPackageRepository packageRepository;
    private final IEventPublisher eventPublisher;
    // private final IShipmentConfig config;

    public ShipmentServiceCore(IShipmentRepository shipmentRepository,
            IPackageRepository packageRepository,
            IEventPublisher eventPublisher) {
        // IShipmentConfig config
        this.shipmentRepository = shipmentRepository;
        this.packageRepository = packageRepository;
        this.eventPublisher = eventPublisher;
        // this.config = config;
    }

    /**
     * Processes a {@link PaymentConfirmed} event to create a new {@link Shipment}
     * and its corresponding {@link Package} entries.
     *
     * <p>
     * After persisting shipment and package data, this method emits:
     * <ul>
     * <li>{@link ShipmentNotification} (status = APPROVED)</li>
     * <li>{@link TransactionMark} (status = SUCCESS)</li>
     * </ul>
     * </p>
     *
     * @param paymentConfirmed the event indicating that a payment has been
     *                         successfully processed
     */
    public void processShipment(PaymentConfirmed paymentConfirmed) {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Starting shipment processing for Order ID: {}, Customer ID: {}",
                paymentConfirmed.getOrderId(), paymentConfirmed.getCustomer().getCustomerId());

        Shipment shipment = new Shipment();
        ShipmentId shipmentId = new ShipmentId(paymentConfirmed.getCustomer().getCustomerId(),
                paymentConfirmed.getOrderId());
        shipment.setId(shipmentId);
        shipment.setPackageCount(paymentConfirmed.getItems().size());
        shipment.setTotalFreightValue(
                (float) paymentConfirmed.getItems().stream().mapToDouble(OrderItem::getFreightValue).sum());
        shipment.setRequestDate(now);
        shipment.setStatus(ShipmentStatus.APPROVED);

        shipment.setFirstName(paymentConfirmed.getCustomer().getFirstName());
        shipment.setLastName(paymentConfirmed.getCustomer().getLastName());
        shipment.setStreet(paymentConfirmed.getCustomer().getStreet());
        shipment.setComplement(paymentConfirmed.getCustomer().getComplement());
        shipment.setZipCode(paymentConfirmed.getCustomer().getZipCode());
        shipment.setCity(paymentConfirmed.getCustomer().getCity());
        shipment.setState(paymentConfirmed.getCustomer().getState());

        logger.info("Saving shipment: {}", shipmentId);
        try {
            shipmentRepository.save(shipment);
        } catch (Exception e) {
            logger.error("Error saving shipment for Order ID: {}, Exception: {}",
                    paymentConfirmed.getOrderId(), e.getMessage(), e);
        }

        int packageIdCounter = 1;
        List<Package> packageList = new ArrayList<>();
        for (OrderItem item : paymentConfirmed.getItems()) {
            Package pkg = new Package();
            PackageId pkgId = new PackageId(paymentConfirmed.getCustomer().getCustomerId(),
                    paymentConfirmed.getOrderId(), packageIdCounter);
            pkg.setId(pkgId);
            pkg.setStatus(PackageStatus.SHIPPED);
            pkg.setFreightValue(item.getFreightValue());
            pkg.setShippingDate(now);
            pkg.setSellerId(item.getSellerId());
            pkg.setProductId(item.getProductId());
            pkg.setProductName(item.getProductName());
            pkg.setQuantity(item.getQuantity());
            packageList.add(pkg);
            packageIdCounter++;
        }
        packageRepository.saveAll(packageList);

        ShipmentNotification shipmentNotification = new ShipmentNotification(
                paymentConfirmed.getCustomer().getCustomerId(),
                paymentConfirmed.getOrderId(),
                now,
                paymentConfirmed.getInstanceId(),
                ShipmentStatus.APPROVED);
        // eventPublisher.sendShipmentNotification(shipmentNotification);
        eventPublisher.publishEvent("shipment-notification-topic", shipmentNotification);

        TransactionMark transactionMark = new TransactionMark(
                paymentConfirmed.getInstanceId(),
                TransactionType.CUSTOMER_SESSION,
                paymentConfirmed.getCustomer().getCustomerId(),
                MarkStatus.SUCCESS,
                "shipment");
        eventPublisher.publishEvent("TransactionMark_CUSTOMER_SESSION", transactionMark);
    }

    public void processPoisonShipment(PaymentConfirmed paymentConfirmed) {
        TransactionMark transactionMark = new TransactionMark(
                paymentConfirmed.getInstanceId(),
                TransactionType.CUSTOMER_SESSION,
                paymentConfirmed.getCustomer().getCustomerId(),
                MarkStatus.ABORT,
                "shipment");
        eventPublisher.publishEvent("TransactionMark_CUSTOMER_SESSION", transactionMark);
    }

    /**
     * Updates shipment and package states based on existing open packages.
     * <p>
     * This method:
     * <ul>
     * <li>Transitions shipments from APPROVED → DELIVERY_IN_PROGRESS →
     * CONCLUDED</li>
     * <li>Updates corresponding {@link Package} records</li>
     * <li>Emits {@link DeliveryNotification} and {@link ShipmentNotification}
     * events</li>
     * </ul>
     * </p>
     *
     * @param instanceId the instance identifier for correlation tracking
     * @throws Exception if the shipment or package cannot be found
     */
    public void updateShipment(String instanceId) throws Exception {
        logger.info("Starting updateShipment for instanceId: {}", instanceId);

        List<Object[]> oldestShipments = packageRepository.getOldestOpenShipmentPerSeller(PackageStatus.SHIPPED);
        logger.info("Found {} oldest shipments to process.", oldestShipments.size());

        for (Object[] shipmentData : oldestShipments) {
            Integer sellerId = (Integer) shipmentData[0];
            String orderDetails = (String) shipmentData[1];
            if (orderDetails != null) {
                String[] ids = orderDetails.split("\\|");
                if (ids.length == 2) {
                    int customerId = Integer.parseInt(ids[0]);
                    int orderId = Integer.parseInt(ids[1]);
                    List<Package> shippedPackages = packageRepository.getShippedPackagesByOrderAndSeller(
                            customerId, orderId, sellerId, PackageStatus.SHIPPED);
                    if (shippedPackages.isEmpty()) {
                        logger.warn("No packages for seller ID {} and order ID {}", sellerId, orderId);
                        continue;
                    }
                    updatePackageDelivery(shippedPackages, instanceId);
                } else {
                    logger.warn("Incomplete order details for seller ID {}. Skipping.", sellerId);
                }
            }
        }
        logger.info("Completed updateShipment for instanceId: {}", instanceId);
    }

    /**
     * Updates package delivery states, emits delivery notifications,
     * and updates shipment completion status if all packages are delivered.
     */
    private void updatePackageDelivery(List<Package> sellerPackages, String instanceId) throws Exception {
        int customerId = sellerPackages.get(0).getCustomerId();
        int orderId = sellerPackages.get(0).getOrderId();
        ShipmentId shipmentId = new ShipmentId(customerId, orderId);
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new Exception("Shipment " + customerId + "-" + orderId + " not found"));

        LocalDateTime now = LocalDateTime.now();

        if (shipment.getStatus() == ShipmentStatus.APPROVED) {
            shipment.setStatus(ShipmentStatus.DELIVERY_IN_PROGRESS);
            shipmentRepository.save(shipment);
            ShipmentNotification notification = new ShipmentNotification(
                    shipment.getCustomerId(), shipment.getOrderId(), now, instanceId,
                    ShipmentStatus.DELIVERY_IN_PROGRESS);
            eventPublisher.publishEvent("shipment-notification-topic", notification);
        }

        int countDelivered = packageRepository.getTotalDeliveredPackagesForOrder(customerId, orderId,
                PackageStatus.DELIVERED);
        // update state to DELIVERED，and send DeliveryNotification event
        for (Package pack : sellerPackages) {
            pack.setStatus(PackageStatus.DELIVERED);
            pack.setDeliveryDate(now);
            packageRepository.save(pack);
            DeliveryNotification delivery = new DeliveryNotification(
                    shipment.getCustomerId(), pack.getOrderId(), pack.getPackageId(),
                    pack.getSellerId(), pack.getProductId(), pack.getProductName(),
                    PackageStatus.DELIVERED, now, instanceId);
            // send event
            eventPublisher.publishEvent("delivery-notification-topic", delivery);
        }
        packageRepository.saveAll(sellerPackages);

        // if updated, Shipment state set to CONCLUDED
        if (shipment.getPackageCount() == countDelivered + sellerPackages.size()) {
            shipment.setStatus(ShipmentStatus.CONCLUDED);
            shipmentRepository.save(shipment);
            ShipmentNotification notification = new ShipmentNotification(
                    shipment.getCustomerId(), shipment.getOrderId(), now, instanceId,
                    ShipmentStatus.CONCLUDED);
            eventPublisher.publishEvent("shipment-notification-topic", notification);
        }
    }

    /**
     * Removes all {@link Shipment} and {@link Package} data.
     * <p>
     * Typically used for testing or resetting the system state.
     * </p>
     */
    public void cleanup() {
        shipmentRepository.deleteAll();
    }
}

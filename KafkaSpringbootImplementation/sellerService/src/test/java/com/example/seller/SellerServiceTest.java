package com.example.seller;

import com.example.common.entities.OrderItem;
import com.example.common.entities.OrderStatus;
import com.example.common.entities.PackageStatus;
import com.example.common.entities.ShipmentStatus;
import com.example.common.events.DeliveryNotification;
import com.example.common.events.InvoiceIssued;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;
import com.example.common.events.ShipmentNotification;
import com.example.common.messaging.PredecessorNotReadyException;
import com.example.common.requests.CustomerCheckout;
import com.example.seller.dto.SellerDashboard;
import com.example.seller.model.OrderEntry;
import com.example.seller.model.OrderEntryId;
import com.example.seller.repository.IOrderEntryRepository;
import com.example.seller.service.ISellerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class SellerServiceTest {

    @Autowired
    private ISellerService sellerService;

    @Autowired
    private IOrderEntryRepository orderEntryRepository;

    @BeforeEach
    public void setUp() {
        orderEntryRepository.deleteAll();
    }

    private void seedEntry(int customerId, int orderId, int sellerId, int productId, OrderStatus status) {
        OrderEntry entry = new OrderEntry();
        entry.setId(new OrderEntryId(customerId, orderId, sellerId, productId));
        entry.setOrderStatus(status);
        orderEntryRepository.save(entry);
    }

    private List<OrderItem> oneItem(int orderId, int sellerId, int productId) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setOrderItemId(1);
        item.setProductId(productId);
        item.setProductName("Test Product");
        item.setSellerId(sellerId);
        item.setUnitPrice(100.0f);
        item.setQuantity(2);
        item.setTotalAmount(200.0f);
        item.setFreightValue(10.0f);
        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        return items;
    }

    private CustomerCheckout customer(int id) {
        CustomerCheckout cc = new CustomerCheckout();
        cc.setCustomerId(id);
        return cc;
    }

    @Test
    public void testProcessInvoiceIssued() {
        InvoiceIssued invoiceIssued = new InvoiceIssued();
        invoiceIssued.setOrderId(1001);
        invoiceIssued.setCustomer(customer(1));
        invoiceIssued.setItems(oneItem(1001, 1, 2001));

        sellerService.processInvoiceIssued(invoiceIssued);

        List<OrderEntry> entries = orderEntryRepository.findByCustomerIdAndOrderId(1, 1001);
        assertEquals(1, entries.size());
        assertEquals(OrderStatus.INVOICED, entries.get(0).getOrderStatus());
    }

    @Test
    public void testProcessPaymentFailed() {
        seedEntry(1, 1001, 1, 2001, OrderStatus.CREATED);

        PaymentFailed paymentFailed = new PaymentFailed();
        paymentFailed.setOrderId(1001);
        paymentFailed.setCustomer(customer(1));
        paymentFailed.setStatus("FAILED");
        paymentFailed.setItems(oneItem(1001, 1, 2001));

        sellerService.processPaymentFailed(paymentFailed);

        List<OrderEntry> entries = orderEntryRepository.findByCustomerIdAndOrderId(1, 1001);
        assertEquals(1, entries.size());
        assertEquals(OrderStatus.PAYMENT_FAILED, entries.get(0).getOrderStatus());
    }

    @Test
    public void testProcessShipmentNotification() {
        seedEntry(1, 1001, 1, 2001, OrderStatus.INVOICED);

        ShipmentNotification shipmentNotification = new ShipmentNotification();
        shipmentNotification.setOrderId(1001);
        shipmentNotification.setCustomerId(1);
        shipmentNotification.setStatus(ShipmentStatus.APPROVED);

        sellerService.processShipmentNotification(shipmentNotification);

        List<OrderEntry> entries = orderEntryRepository.findByCustomerIdAndOrderId(1, 1001);
        assertEquals(1, entries.size());
        assertEquals(OrderStatus.READY_FOR_SHIPMENT, entries.get(0).getOrderStatus());
    }

    @Test
    public void testProcessDeliveryNotification() {
        OrderEntry entry = new OrderEntry();
        entry.setId(new OrderEntryId(1, 1001, 1, 2001));
        entry.setOrderStatus(OrderStatus.INVOICED);
        entry.setDeliveryStatus(PackageStatus.READY_TO_SHIP);
        orderEntryRepository.save(entry);

        DeliveryNotification deliveryNotification = new DeliveryNotification();
        deliveryNotification.setOrderId(1001);
        deliveryNotification.setSellerId(1);
        deliveryNotification.setCustomerId(1);
        deliveryNotification.setProductId(2001);
        deliveryNotification.setStatus(PackageStatus.DELIVERED);

        sellerService.processDeliveryNotification(deliveryNotification);

        OrderEntry updated = orderEntryRepository
                .findById(new OrderEntryId(1, 1001, 1, 2001)).orElse(null);
        assertNotNull(updated);
        assertEquals(PackageStatus.DELIVERED, updated.getDeliveryStatus());
    }

    // ---- out-of-order arrival: no order entries yet -> retryable signal --------------

    @Test
    public void testPaymentConfirmedBeforeInvoiceThrows() {
        PaymentConfirmed pc = new PaymentConfirmed();
        pc.setOrderId(4242);
        pc.setCustomer(customer(9));
        assertThrows(PredecessorNotReadyException.class, () -> sellerService.processPaymentConfirmed(pc));
    }

    @Test
    public void testShipmentNotificationBeforeInvoiceThrows() {
        ShipmentNotification sn = new ShipmentNotification();
        sn.setOrderId(4242);
        sn.setCustomerId(9);
        sn.setStatus(ShipmentStatus.APPROVED);
        assertThrows(PredecessorNotReadyException.class, () -> sellerService.processShipmentNotification(sn));
    }

    // ---- dashboard: list and aggregate come from one snapshot -------------------------

    private void seedRichEntry(int customerId, int orderId, int sellerId, int productId,
            OrderStatus status, int qty, float totalAmount, float freight, float totalInvoice) {
        OrderEntry e = new OrderEntry();
        e.setId(new OrderEntryId(customerId, orderId, sellerId, productId));
        e.setOrderStatus(status);
        e.setQuantity(qty);
        e.setTotalAmount(totalAmount);
        e.setFreightValue(freight);
        e.setTotalInvoice(totalInvoice);
        orderEntryRepository.save(e);
    }

    @Test
    public void testProcessInvoiceIssuedIsIdempotent() {
        InvoiceIssued invoiceIssued = new InvoiceIssued();
        invoiceIssued.setOrderId(1001);
        invoiceIssued.setCustomer(customer(1));
        invoiceIssued.setItems(oneItem(1001, 1, 2001));

        sellerService.processInvoiceIssued(invoiceIssued);
        sellerService.processInvoiceIssued(invoiceIssued); // redelivery

        List<OrderEntry> entries = orderEntryRepository.findByCustomerIdAndOrderId(1, 1001);
        assertEquals(1, entries.size());
        assertEquals(OrderStatus.INVOICED, entries.get(0).getOrderStatus());
    }

    @Test
    public void testQueryDashboardAggregatesOngoingOnlyFromOneSnapshot() {
        int sellerId = 5;
        seedRichEntry(1, 100, sellerId, 1, OrderStatus.INVOICED, 2, 100f, 10f, 110f);
        seedRichEntry(1, 101, sellerId, 1, OrderStatus.PAYMENT_PROCESSED, 3, 150f, 15f, 165f);
        seedRichEntry(1, 102, sellerId, 1, OrderStatus.DELIVERED, 9, 900f, 90f, 990f);       // terminal
        seedRichEntry(1, 103, sellerId, 1, OrderStatus.PAYMENT_FAILED, 7, 700f, 70f, 770f);  // terminal

        SellerDashboard dashboard = sellerService.queryDashboard(sellerId);

        // discriminated list = every entry for the seller
        assertEquals(4, dashboard.getOrderEntries().size());

        // aggregate = ongoing entries only
        assertEquals(2, dashboard.getSellerView().getCountOrders());
        assertEquals(5, dashboard.getSellerView().getCountItems());
        assertEquals(250f, dashboard.getSellerView().getTotalAmount(), 0.001f);
        assertEquals(25f, dashboard.getSellerView().getTotalFreight(), 0.001f);
        assertEquals(275f, dashboard.getSellerView().getTotalInvoice(), 0.001f);
    }
}

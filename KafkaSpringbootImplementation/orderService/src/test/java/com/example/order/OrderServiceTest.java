package com.example.order;

import com.example.common.entities.CartItem;
import com.example.common.entities.OrderStatus;
import com.example.common.entities.ShipmentStatus;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;
import com.example.common.events.ShipmentNotification;
import com.example.common.events.StockConfirmed;
import com.example.common.requests.CustomerCheckout;
import com.example.order.model.Order;
import com.example.order.model.OrderId;
import com.example.order.repository.RedisCustomerOrderRepository;
import com.example.order.repository.RedisOrderHistoryRepository;
import com.example.order.repository.RedisOrderRepository;
import com.example.order.repository.RedisProcessedCheckoutRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
public class OrderServiceTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private RedisOrderRepository orderRepository;
    @Autowired
    private RedisOrderHistoryRepository orderHOrderRepository;
    @Autowired
    private RedisCustomerOrderRepository customerOrderRepository;
    @Autowired
    private RedisProcessedCheckoutRepository processedCheckoutRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        orderRepository.deleteAll();
        orderHOrderRepository.deleteAll();
        customerOrderRepository.deleteAll();
        processedCheckoutRepository.deleteAll();
    }

    @Test
    public void testProcessStockConfirmed() throws Exception {
        OrderId orderId = new OrderId();
        orderId.setCustomerId(1001);
        orderId.setOrderId(1);

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CREATED);
        order.setInvoiceNumber("INV-20231025-001");
        order.setPurchaseDate(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setCountItems(2);
        order.setTotalAmount(100.0f);
        order.setTotalFreight(10.0f);
        order.setTotalIncentive(5.0f);
        order.setTotalInvoice(105.0f);
        order.setTotalItems(2);

        orderRepository.save(order);

        CustomerCheckout customerCheckout = new CustomerCheckout();
        customerCheckout.setCustomerId(1001);

        StockConfirmed stockConfirmed = new StockConfirmed();
        stockConfirmed.setCustomerCheckout(customerCheckout);
        stockConfirmed.setInstanceId("test-instance");
        List<CartItem> items = new ArrayList<>();
        CartItem item1 = new CartItem();
        item1.setProductId(2001);
        item1.setSellerId(3001);
        item1.setQuantity(2);
        item1.setUnitPrice(50.0f);
        item1.setFreightValue(5.0f);
        item1.setVoucher(10.0f);
        items.add(item1);

        CartItem item2 = new CartItem();
        item2.setProductId(2002);
        item2.setSellerId(3002);
        item2.setQuantity(1);
        item2.setUnitPrice(100.0f);
        item2.setFreightValue(8.0f);
        item2.setVoucher(0.0f);
        items.add(item2);
        stockConfirmed.setItems(items);

        String payload = objectMapper.writeValueAsString(stockConfirmed);
        kafkaTemplate.send("stock-confirmed-topic", payload);
        TimeUnit.MILLISECONDS.sleep(200);

        Order updatedOrder = orderRepository.findByCustomerIdAndOrderId(1001, 1).orElse(null);

        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.INVOICED, updatedOrder.getStatus());
        assertEquals(1001, updatedOrder.getCustomerId());
    }

    @Test
    public void testProcessPaymentConfirmed() throws Exception {
        OrderId orderId = new OrderId(1001, 1);
        Order order = new Order();
        order.setId(orderId);
        order.setInvoiceNumber("1001-20241030-003");
        order.setStatus(OrderStatus.CREATED);
        order.setPurchaseDate(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setCountItems(3);
        order.setTotalAmount(100.0f);
        order.setTotalFreight(10.0f);
        order.setTotalIncentive(5.0f);
        order.setTotalInvoice(105.0f);
        order.setTotalItems(3);

        orderRepository.save(order);

        CustomerCheckout customerCheckout = new CustomerCheckout();
        customerCheckout.setCustomerId(1001);
        PaymentConfirmed paymentConfirmed = new PaymentConfirmed();
        paymentConfirmed.setCustomer(customerCheckout);
        paymentConfirmed.setOrderId(1);
        paymentConfirmed.setDate(LocalDateTime.now());

        String payload = objectMapper.writeValueAsString(paymentConfirmed);
        kafkaTemplate.send("payment-confirmed-topic", payload);
        TimeUnit.MILLISECONDS.sleep(200);

        Order updatedOrder = orderRepository.findByCustomerIdAndOrderId(1001, 1).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.PAYMENT_PROCESSED, updatedOrder.getStatus());
    }

    @Test
    public void testProcessPaymentFailed() throws Exception {
        OrderId orderId = new OrderId();
        orderId.setCustomerId(1001);
        orderId.setOrderId(1);

        Order order = new Order();
        order.setId(orderId);
        order.setInvoiceNumber("INV-20231025-001");
        order.setStatus(OrderStatus.CREATED);
        order.setPurchaseDate(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setCountItems(3);
        order.setTotalAmount(100.0f);
        order.setTotalFreight(10.0f);
        order.setTotalIncentive(5.0f);
        order.setTotalInvoice(115.0f);
        order.setTotalItems(3);

        orderRepository.save(order);
        CustomerCheckout customerCheckout = new CustomerCheckout();
        customerCheckout.setCustomerId(1001);

        PaymentFailed paymentFailed = new PaymentFailed();
        paymentFailed.setCustomer(customerCheckout);
        paymentFailed.setOrderId(1);

        String payload = objectMapper.writeValueAsString(paymentFailed);
        kafkaTemplate.send("payment-failed-topic", payload);
        TimeUnit.MILLISECONDS.sleep(200);

        Order updateOrder = orderRepository.findByCustomerIdAndOrderId(1001, 1).orElse(null);
        assertNotNull(updateOrder);
        assertEquals(OrderStatus.PAYMENT_FAILED, updateOrder.getStatus());
    }

    @Test
    public void testProcessShipmentNotification() throws Exception {
        OrderId orderId = new OrderId();
        orderId.setCustomerId(1001);
        orderId.setOrderId(1);

        Order order = new Order();
        order.setId(orderId);
        order.setInvoiceNumber("INV-20231025-001");
        order.setStatus(OrderStatus.READY_FOR_SHIPMENT);
        order.setPurchaseDate(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setCountItems(3);
        order.setTotalAmount(100.0f);
        order.setTotalFreight(10.0f);
        order.setTotalIncentive(5.0f);
        order.setTotalInvoice(115.0f);
        order.setTotalItems(3);

        orderRepository.save(order);

        ShipmentNotification shipmentNotification = new ShipmentNotification();
        shipmentNotification.setCustomerId(1001);
        shipmentNotification.setOrderId(1);
        shipmentNotification.setStatus(ShipmentStatus.CONCLUDED);
        shipmentNotification.setEventDate(LocalDateTime.now());

        String payload = objectMapper.writeValueAsString(shipmentNotification);
        kafkaTemplate.send("shipment-notification-topic", payload);

        TimeUnit.MILLISECONDS.sleep(200);

        Order updatedOrder = orderRepository.findByCustomerIdAndOrderId(1001, 1).orElse(null);
        assertNotNull(updatedOrder, "Order should exist in the database");
        assertEquals(OrderStatus.DELIVERED, updatedOrder.getStatus(), "Order status should be updated to DELIVERED");
    }

    // ---- no duplicate checkouts ---------------------------------------------------------

    @Test
    public void testDuplicateStockConfirmedCreatesOneOrder() throws Exception {
        int customerId = 9001;
        String payload = objectMapper.writeValueAsString(buildStockConfirmed(customerId, "dup-instance-1"));

        kafkaTemplate.send("stock-confirmed-topic", payload);
        TimeUnit.MILLISECONDS.sleep(300);
        kafkaTemplate.send("stock-confirmed-topic", payload); // redelivery of the same checkout
        TimeUnit.MILLISECONDS.sleep(300);

        List<Order> orders = orderRepository.findByCustomerId(customerId);
        assertEquals(1, orders.size(), "a redelivered StockConfirmed must not create a second order");
        assertEquals(1, customerOrderRepository.findByCustomerId(customerId).getNextOrderId(),
                "nextOrderId must not be incremented twice for one checkout");
    }

    @Test
    public void testStockConfirmedWithNoInstanceIdIsNotDeduped() throws Exception {
        int customerId = 9002;
        String payload = objectMapper.writeValueAsString(buildStockConfirmed(customerId, null));

        kafkaTemplate.send("stock-confirmed-topic", payload);
        TimeUnit.MILLISECONDS.sleep(300);
        kafkaTemplate.send("stock-confirmed-topic", payload);
        TimeUnit.MILLISECONDS.sleep(300);

        // With no instanceId there is nothing to key the guard on, so both events are processed.
        assertEquals(2, orderRepository.findByCustomerId(customerId).size(),
                "without an instanceId the duplicate guard does not apply");
    }

    private StockConfirmed buildStockConfirmed(int customerId, String instanceId) {
        CustomerCheckout checkout = new CustomerCheckout();
        checkout.setCustomerId(customerId);
        checkout.setInstanceId(instanceId);

        CartItem item = new CartItem();
        item.setProductId(2001);
        item.setSellerId(3001);
        item.setQuantity(2);
        item.setUnitPrice(50.0f);
        item.setFreightValue(5.0f);
        item.setVoucher(0.0f);

        List<CartItem> items = new ArrayList<>();
        items.add(item);

        StockConfirmed sc = new StockConfirmed();
        sc.setTimestamp(LocalDateTime.now());
        sc.setCustomerCheckout(checkout);
        sc.setItems(items);
        sc.setInstanceId(instanceId);
        return sc;
    }
}
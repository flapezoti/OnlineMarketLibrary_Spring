package com.example.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.cart.model.Cart;
import com.example.cart.model.CartItem;
import com.example.cart.model.CartItemId;
import com.example.cart.model.ProductReplica;
import com.example.cart.model.ProductReplicaId;
import com.example.cart.repository.RedisCartItemRepository;
import com.example.cart.repository.RedisCartRepository;
import com.example.cart.repository.RedisProductReplicaRepository;
import com.example.cart.service.CartServiceCore;
import com.example.common.entities.CartStatus;
import com.example.common.events.PriceUpdate;
import com.example.common.requests.CustomerCheckout;

@SpringBootTest
@ActiveProfiles("test")
public class CartServiceTest {

    @Autowired
    private CartServiceCore cartService;

    @Autowired
    private RedisCartRepository cartRepository;

    @Autowired
    private RedisCartItemRepository cartItemRepository;

    @Autowired
    private RedisProductReplicaRepository productReplicaRepository;

    @BeforeEach
    public void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productReplicaRepository.deleteAll();
    }

    @Test
    public void testRemoveItem() {
        Cart cart = new Cart();
        cart.setCustomerId(1);
        cartRepository.saveCart(cart);

        CartItemId cartItemId = new CartItemId(1, 456, 123);
        CartItem item = new CartItem();
        item.setId(cartItemId);
        item.setProductName("Test Product");
        item.setUnitPrice(100.0f);
        item.setFreightValue(10.0f);
        item.setQuantity(2);
        item.setVoucher(5.0f);
        item.setVersion("1.0");
        item.setCart(cart);
        cartItemRepository.saveCartItem(item);

        cartService.removeItem(1, 123, 456);
        CartItem removedItem = cartItemRepository.findById(cartItemId).orElse(null);
        assertNull(removedItem, "购物车中的商品应该已被删除");
    }

    @Test
    public void testSealCartWithCleanItems() {
        Cart cart = new Cart();
        cart.setCustomerId(1);
        cartRepository.saveCart(cart);

        CartItemId cartItemId1 = new CartItemId(1, 100, 200);
        CartItem item1 = new CartItem();
        item1.setId(cartItemId1);
        item1.setQuantity(2);
        item1.setCart(cart);
        cartItemRepository.saveCartItem(item1);

        CartItemId cartItemId2 = new CartItemId(1, 101, 201);
        CartItem item2 = new CartItem();
        item2.setId(cartItemId2);
        item2.setQuantity(1);
        item2.setCart(cart);
        cartItemRepository.saveCartItem(item2);

        cartService.seal(cart, true);

        Cart updatedCart = cartRepository.findByCustomerId(1);
        assertNotNull(updatedCart, "购物车不应该被删除");
        assertEquals(CartStatus.OPEN, updatedCart.getStatus(), "购物车状态应该为OPEN");

        Optional<CartItem> remainingItems = cartItemRepository.findById(cartItemId2);
        assertTrue(remainingItems.isEmpty(), "购物车项应该被清空");
    }

    @Test
    public void testSealCartWithoutCleanItems() {
        Cart cart = new Cart();
        cart.setCustomerId(1);
        cartRepository.saveCart(cart);

        CartItemId cartItemId1 = new CartItemId(1, 100, 200);
        CartItem item1 = new CartItem();
        item1.setId(cartItemId1);
        item1.setQuantity(2);
        item1.setCart(cart);
        cartItemRepository.saveCartItem(item1);

        CartItemId cartItemId2 = new CartItemId(1, 101, 201);
        CartItem item2 = new CartItem();
        item2.setId(cartItemId2);
        item2.setQuantity(1);
        item2.setCart(cart);
        cartItemRepository.saveCartItem(item2);

        cartService.seal(cart, false);

        Optional<CartItem> remainingItem1 = cartItemRepository.findById(cartItemId1);
        Optional<CartItem> remainingItem2 = cartItemRepository.findById(cartItemId2);

        assertTrue(remainingItem1.isPresent(), "购物车项1不应该被清空");
        assertTrue(remainingItem2.isPresent(), "购物车项2不应该被清空");
    }

    // ---- checkout price reconciliation against the product replica ----------------------

    @Test
    public void testCheckoutReconcilesPriceDown() {
        int customerId = 10, sellerId = 20, productId = 30;
        seedCart(customerId);
        seedItem(customerId, sellerId, productId, 100.0f, 0.0f);
        seedReplica(sellerId, productId, 80.0f, "1");

        cartService.notifyCheckout(checkout(customerId, "iid-down"));

        CartItem reconciled = cartItemRepository
                .findById(new CartItemId(customerId, sellerId, productId)).orElseThrow();
        assertEquals(80.0f, reconciled.getUnitPrice(), 0.001f);
        assertEquals(20.0f, reconciled.getVoucher(), 0.001f);
    }

    @Test
    public void testCheckoutReconcilesPriceUp() {
        int customerId = 11, sellerId = 21, productId = 31;
        seedCart(customerId);
        seedItem(customerId, sellerId, productId, 100.0f, 5.0f);
        seedReplica(sellerId, productId, 120.0f, "1");

        cartService.notifyCheckout(checkout(customerId, "iid-up"));

        CartItem reconciled = cartItemRepository
                .findById(new CartItemId(customerId, sellerId, productId)).orElseThrow();
        assertEquals(120.0f, reconciled.getUnitPrice(), 0.001f);
        assertEquals(5.0f, reconciled.getVoucher(), 0.001f);
    }

    @Test
    public void testCheckoutBlockedWhenReplicaMissing() {
        int customerId = 12, sellerId = 22, productId = 32;
        seedCart(customerId);
        seedItem(customerId, sellerId, productId, 100.0f, 0.0f);
        // no replica seeded

        assertThrows(RuntimeException.class, () -> cartService.notifyCheckout(checkout(customerId, "iid-missing")));
        assertEquals(CartStatus.OPEN, cartRepository.findByCustomerId(customerId).getStatus());
    }

    @Test
    public void testStalePriceUpdateIgnored() {
        int sellerId = 23, productId = 33;
        seedReplica(sellerId, productId, 100.0f, "1");

        cartService.processPriceUpdate(new PriceUpdate(sellerId, productId, 50.0f, "5", "iid-v5"));
        assertEquals(50.0f,
                productReplicaRepository.findByProductReplicaId(new ProductReplicaId(sellerId, productId)).getPrice(),
                0.001f);

        cartService.processPriceUpdate(new PriceUpdate(sellerId, productId, 999.0f, "3", "iid-v3"));
        ProductReplica afterStale = productReplicaRepository
                .findByProductReplicaId(new ProductReplicaId(sellerId, productId));
        assertEquals(50.0f, afterStale.getPrice(), 0.001f);
        assertEquals("5", afterStale.getVersion());
    }

    private void seedCart(int customerId) {
        Cart cart = new Cart();
        cart.setCustomerId(customerId);
        cart.setStatus(CartStatus.OPEN);
        cartRepository.saveCart(cart);
    }

    private void seedItem(int customerId, int sellerId, int productId, float unitPrice, float voucher) {
        CartItem item = new CartItem();
        item.setId(new CartItemId(customerId, sellerId, productId));
        item.setProductName("P");
        item.setUnitPrice(unitPrice);
        item.setFreightValue(5.0f);
        item.setQuantity(1);
        item.setVoucher(voucher);
        cartItemRepository.saveCartItem(item);
    }

    private void seedReplica(int sellerId, int productId, float price, String version) {
        ProductReplica replica = new ProductReplica();
        replica.setSellerId(sellerId);
        replica.setProductId(productId);
        replica.setName("P");
        replica.setPrice(price);
        replica.setVersion(version);
        replica.setActive(true);
        productReplicaRepository.saveProductReplica(replica);
    }

    private CustomerCheckout checkout(int customerId, String instanceId) {
        CustomerCheckout cc = new CustomerCheckout();
        cc.setCustomerId(customerId);
        cc.setInstanceId(instanceId);
        return cc;
    }
}

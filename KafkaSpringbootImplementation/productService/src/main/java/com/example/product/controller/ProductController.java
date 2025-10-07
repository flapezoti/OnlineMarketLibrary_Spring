package com.example.product.controller;

import com.example.common.events.PriceUpdate;
import com.example.product.model.Product;
import com.example.product.model.ProductId;
import com.example.product.repository.RedisProductRepository;
import com.example.product.service.IProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/product")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private RedisTemplate<String, Product> productRedisTemplate;

    @Autowired
    private IProductService productService;

    @Autowired
    private RedisProductRepository productRepository;

    @GetMapping("/{sellerId}")
    public ResponseEntity<List<Product>> getBySellerId(@PathVariable int sellerId) {
        logger.info("[GetBySeller] received for seller {}", sellerId);
        if (sellerId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        List<Product> products = productRepository.findByIdSellerId(sellerId);
        if (products == null || products.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(products);
    }

    @GetMapping("/{sellerId}/{productId}")
    public ResponseEntity<Product> getBySellerIdAndProductId(@PathVariable int sellerId, @PathVariable int productId) {
        logger.info("[GetBySellerIdAndProductId] received for product {}", productId);
        if (productId <= 0) {
            return ResponseEntity.badRequest().body(null);
        }

        Optional<Product> productOpt = productRepository.findById(new ProductId(sellerId, productId));
        if (!productOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(productOpt.get());
    }

    @PostMapping("/")
public ResponseEntity<Void> addProduct(@RequestBody com.example.common.entities.Product commonProduct) {
    try {
        // 1. Construct the Redis key in the format: "product:{sellerId}:{productId}"
        String redisKey = "product:" + commonProduct.getSellerId() + ":" + commonProduct.getProductId();

        logger.info("Received product to add: {}", commonProduct);

       // 2. Verify whether the product already exists in Redis
        Product cachedProduct = productRedisTemplate.opsForValue().get(redisKey);
        if (cachedProduct != null) {
            logger.warn("Product already exists in Redis, key={}", redisKey);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // 3. Transform the data into an internal Product entity
        Product product = convertToInternalProduct(commonProduct);

        logger.info("Converted internal product: {}", product);

        // 4. Invoke the service to persist the product entity
        productService.processCreateProduct(product);

        // 5. Save to Redis
        productRedisTemplate.opsForValue().set(redisKey, product);
        logger.info("Product saved to Redis with key={}", redisKey);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    } catch (Exception e) {
        logger.error("Failed to add product: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

    @PutMapping("/")
    public ResponseEntity<Void> updateProduct(@RequestBody com.example.common.entities.Product commonProduct) {
        try {
            Product product = convertToInternalProduct(commonProduct);
            productService.processProductUpdate(product);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to update product: {}", e.toString());
            productService.processPoisonProductUpdate(convertToInternalProduct(commonProduct));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/")
    public ResponseEntity<Void> updateProductPrice(@RequestBody PriceUpdate update) {
        if (update.getVersion() == null) {
            update.setVersion("0");
        }
        logger.info("Received price update request: {}", update);
        try {
            productService.processPriceUpdate(update);
        } catch (Exception e) {
            logger.error("Failed to process price update: {}", e.toString());
            productService.processPoisonPriceUpdate(update);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PatchMapping("/cleanup")
    public ResponseEntity<Void> cleanup() {
        logger.warn("Cleanup requested at {}", System.currentTimeMillis());
        try {
            productRepository.deleteAll();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Cleanup error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/reset")
    public ResponseEntity<Void> reset() {
        try {
            productRepository.reset();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Reset error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Product convertToInternalProduct(com.example.common.entities.Product commonProduct) {
        Product product = new Product();
        ProductId productId = new ProductId(commonProduct.getSellerId(), commonProduct.getProductId());
        product.setId(productId);
        product.setName(commonProduct.getName());
        product.setSku(commonProduct.getSku());
        product.setCategory(commonProduct.getCategory());
        product.setDescription(commonProduct.getDescription());
        product.setPrice(commonProduct.getPrice());
        product.setFreightValue(commonProduct.getFreightValue());
        product.setStatus(commonProduct.getStatus());
        product.setVersion(commonProduct.getVersion());
        return product;
    }
}

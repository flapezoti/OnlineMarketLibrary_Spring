package com.example.stock.controller;

import com.example.common.events.IncreaseStock;
import com.example.stock.model.StockItem;
import com.example.stock.model.StockItemId;
import com.example.stock.repository.IStockRepository;
import com.example.stock.service.IStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private IStockService stockService;

    @Autowired
    private IStockRepository stockRepository;

    private static final Logger logger = LoggerFactory.getLogger(StockController.class);

    /**
     * API endpoint to increase stock quantity.
     * Based on the provided {@link IncreaseStock} request, this method locates
     * the specified stock item. If the item does not exist, it returns HTTP 404;
     * otherwise, it delegates the update operation to the service layer.
     */
    @PatchMapping("/")
    public ResponseEntity<Void> increaseStock(@RequestBody IncreaseStock increaseStock) {
        logger.info("[IncreaseStock] received for product id: {}", increaseStock.getProductId());
        StockItemId stockItemId = new StockItemId(increaseStock.getSellerId(), increaseStock.getProductId());
        Optional<StockItem> stockItemOpt = stockRepository.findById(stockItemId);
        if (!stockItemOpt.isPresent()) {
            logger.info("[IncreaseStock] Item not found for product id: {}", increaseStock.getProductId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // call service method to update stock
        stockService.increaseStock(increaseStock);
        logger.info("[IncreaseStock] completed for product id: {}", increaseStock.getProductId());
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * API endpoint to create a new stock item.
     * Adds a new product record into the stock system.
     */
    @PostMapping("/")
    public ResponseEntity<Void> addStockItem(@RequestBody com.example.common.entities.StockItem commonStockItem) {
        logger.info("[AddStockItem] received for product id: {}", commonStockItem.getProduct_id());
        try {
            // change the uniform style to Common StockItem
            StockItemId stockItemId = new StockItemId(commonStockItem.getSeller_id(), commonStockItem.getProduct_id());
            StockItem stockItem = new StockItem();
            stockItem.setId(stockItemId);
            stockItem.setQtyAvailable(commonStockItem.getQty_available());
            stockItem.setQtyReserved(commonStockItem.getQty_reserved());
            stockItem.setOrderCount(commonStockItem.getOrder_count());
            stockItem.setYtd(commonStockItem.getYtd());
            stockItem.setData(commonStockItem.getData());
            stockItem.setVersion(commonStockItem.getVersion());
            stockItem.setUpdatedAt(LocalDateTime.now());

            // call service layer to create stock item
            stockService.createStockItem(stockItem);

            logger.info("[AddStockItem] completed for product id: {}", stockItem.getId().getProductId());
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("[AddStockItem] failed for product id: {}", commonStockItem.getProduct_id(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * API endpoint to query a stock item by sellerId and productId.
     */
    @GetMapping("/{sellerId}/{productId}")
    public ResponseEntity<StockItem> getBySellerIdAndProductId(@PathVariable int sellerId,
            @PathVariable int productId) {
        StockItemId stockItemId = new StockItemId(sellerId, productId);
        Optional<StockItem> stockOpt = stockRepository.findById(stockItemId);
        if (stockOpt.isPresent()) {
            return new ResponseEntity<>(stockOpt.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * API endpoint to query all stock items belonging to a specific seller.
     */
    @GetMapping("/{sellerId}")
    public ResponseEntity<List<StockItem>> getBySellerId(@PathVariable int sellerId) {
        logger.info("[GetBySeller] received for seller {}", sellerId);
        if (sellerId <= 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        List<StockItem> items = stockRepository.findBySellerId(sellerId);
        if (items != null && !items.isEmpty()) {
            logger.info("[GetBySeller] returning {} items for seller {}", items.size(), sellerId);
            return new ResponseEntity<>(items, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * clean all the request for test, do not use it for real scenario. 
     */
    @PatchMapping("/cleanup")
    public ResponseEntity<Void> cleanup() {
        logger.warn("Cleanup requested at {}", LocalDateTime.now());
        stockService.cleanup();
        return ResponseEntity.ok().build();
    }

    /**
     * reset the stock data
     */
    @PatchMapping("/reset")
    public ResponseEntity<Void> reset() {
        logger.warn("Reset requested at {}", LocalDateTime.now());
        stockService.reset();
        return ResponseEntity.ok().build();
    }

}

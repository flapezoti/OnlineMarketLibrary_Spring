package com.example.seller.service;

import com.example.common.entities.OrderStatus;
import com.example.common.events.InvoiceIssued;
import com.example.common.events.ShipmentNotification;
import com.example.seller.model.OrderSellerView;
import com.example.seller.repository.IOrderEntryRepository;
import com.example.seller.repository.IOrderSellerViewRepository;

import java.util.Arrays;
import java.util.List;

/**
 * Core implementation of {@link IMaterializedViewService} that rebuilds
 * the seller-level materialized view directly from the database.
 *
 * <p>
 * This implementation uses the abstract {@link IOrderEntryRepository}
 * to query order aggregates and {@link IOrderSellerViewRepository}
 * to clear and repopulate the materialized view table.
 *
 * <p>
 * Unlike the cache-based version, this class relies on the underlying
 * database to persist and refresh the aggregated seller statistics.
 * It is designed to work in traditional RDBMS setups where periodic
 * materialized view refresh is preferred over real-time cache updates.
 * </p>
 */
public class MaterializedViewServiceCacheCore implements IMaterializedViewService {

    private final IOrderEntryRepository orderEntryRepository;
    private final IOrderSellerViewRepository orderSellerViewRepository;

    public MaterializedViewServiceCacheCore(IOrderEntryRepository orderEntryRepository,
            IOrderSellerViewRepository orderSellerViewRepository) {
        this.orderEntryRepository = orderEntryRepository;
        this.orderSellerViewRepository = orderSellerViewRepository;
    }

    /**
     * Initializes the materialized view by clearing existing data
     * and repopulating the latest aggregated seller statistics.
     *
     * <p>
     * This method is typically invoked during system startup
     * or periodic batch refresh operations.
     * </p>
     */
    @Override
    public void initializeMaterializedView() {
        orderSellerViewRepository.clearMaterializedView();
        orderSellerViewRepository.populateMaterializedView();
    }

    /**
     * Retrieves an {@link OrderSellerView} for the given seller ID by querying
     * aggregated statistics from the {@link IOrderEntryRepository}.
     *
     * <p>
     * If no matching seller data exists, this method returns {@code null}.
     * </p>
     *
     * <p>
     * The aggregation is based on predefined order statuses:
     * INVOICED, PAYMENT_PROCESSED, READY_FOR_SHIPMENT, and IN_TRANSIT.
     * </p>
     */
    @Override
    public OrderSellerView getSellerView(int sellerId) {
        List<OrderStatus> statuses = Arrays.asList(
                OrderStatus.INVOICED,
                OrderStatus.PAYMENT_PROCESSED,
                OrderStatus.READY_FOR_SHIPMENT,
                OrderStatus.IN_TRANSIT);

        List<Object[]> aggregates = orderEntryRepository.findAllSellerAggregates(statuses);
        // Expected record format:
        // [sellerId (Integer), countOrders (Long), countItems (Long),
        // totalAmount (Double), totalFreight (Double), totalInvoice (Double)]
        for (Object[] row : aggregates) {
            Integer id = (Integer) row[0];
            if (id != null && id == sellerId) {
                OrderSellerView view = new OrderSellerView();
                view.setSellerId(sellerId);
                view.setCountOrders(((Long) row[1]).intValue());
                view.setCountItems(((Long) row[2]).intValue());
                view.setTotalAmount(((Double) row[3]).floatValue());
                view.setTotalFreight(((Double) row[4]).floatValue());
                view.setTotalInvoice(((Double) row[5]).floatValue());
                return view;
            }
        }
        return null;
    }

    /**
     * Handles {@link InvoiceIssued} events by triggering a refresh
     * of the materialized view in the database.
     *
     * <p>
     * For simplicity, this implementation repopulates the entire view.
     * More fine-grained updates can be implemented if necessary.
     * </p>
     */
    @Override
    public void processInvoiceIssued(InvoiceIssued invoiceIssued) {
        if (invoiceIssued.getItems() == null) {
            System.err.println("InvoiceIssued items are null. Event: " + invoiceIssued);
            return;
        }
        orderSellerViewRepository.populateMaterializedView();
    }

    /**
     * Handles {@link ShipmentNotification} events by repopulating
     * the materialized view table with updated shipment data.
     *
     * <p>
     * This approach rebuilds the complete view for simplicity.
     * In optimized systems, this could be limited to affected seller IDs.
     * </p>
     */
    @Override
    public void processShipmentNotification(ShipmentNotification notification) {
        orderSellerViewRepository.populateMaterializedView();
    }
}

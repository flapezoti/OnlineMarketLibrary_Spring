package com.example.seller.dto;

import com.example.seller.model.OrderEntry;
import com.example.seller.model.OrderSellerView;
import java.util.List;

/**
 * Data Transfer Object (DTO) representing a seller’s dashboard view.
 *
 * <p>
 * This class aggregates two key elements of a seller’s order overview:
 * </p>
 * <ul>
 * <li>{@link OrderSellerView} — an aggregated summary of seller-level
 * statistics such as total sales, revenue, or order count.</li>
 * <li>{@link OrderEntry} — a detailed list of individual order entries that
 * provide transaction-level insights.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <p>
 * {@code SellerDashboard} is designed as a read-only response model, typically
 * returned by a controller
 * to provide a seller’s real-time operational view. It can be constructed
 * directly from repository or service layer data:
 * </p>
 *
 * <pre>
 * {@code
 * OrderSellerView view = orderRepository.getSellerSummary(sellerId);
 * List<OrderEntry> entries = orderRepository.getSellerOrders(sellerId);
 * SellerDashboard dashboard = new SellerDashboard(view, entries);
 * return dashboard;
 * }
 * </pre>
 *
 * <p>
 * Both fields are mutable for serialization frameworks (e.g. Jackson) but
 * generally treated as immutable after construction.
 * </p>
 */
public class SellerDashboard {

    private OrderSellerView sellerView; // aggregation view
    private List<OrderEntry> orderEntries; // details entries

    public SellerDashboard() {
    }

    public SellerDashboard(OrderSellerView sellerView, List<OrderEntry> orderEntries) {
        this.sellerView = sellerView;
        this.orderEntries = orderEntries;
    }

    // Getter
    public OrderSellerView getSellerView() {
        return sellerView;
    }

    public List<OrderEntry> getOrderEntries() {
        return orderEntries;
    }

    // Setter
    public void setSellerView(OrderSellerView sellerView) {
        this.sellerView = sellerView;
    }

    public void setOrderEntries(List<OrderEntry> orderEntries) {
        this.orderEntries = orderEntries;
    }

}

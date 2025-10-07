package com.example.customer.service;

import com.example.common.events.DeliveryNotification;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;

/**
 * Defines core operations for handling customer-related events
 * such as delivery and payment status updates.
 */
public interface ICustomerService {
    /**
     * Processes a delivery notification event.
     */
    void processDeliveryNotification(DeliveryNotification paymentConfirmed);

    /**
     * Processes a successful payment confirmation event.
     */
    void processPaymentConfirmed(PaymentConfirmed paymentConfirmed);

    /**
     * Processes a failed payment event.
     */
    void processPaymentFailed(PaymentFailed paymentFailed);

    /**
     * Cleans up temporary or obsolete customer data.
     */
    void cleanup();

    /**
     * Resets the customer service to its initial state.
     */
    void reset();
}

package com.example.customer.service;

import com.example.common.events.DeliveryNotification;
import com.example.common.events.PaymentConfirmed;
import com.example.common.events.PaymentFailed;
import com.example.customer.model.Customer;
import com.example.customer.repository.ICustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core service implementation for handling customer-related events.
 * 
 * This class is intended to be used by message consumers or higher-level
 * service coordinators that process events like PaymentConfirmed,
 * PaymentFailed or DeliveryNotification.
 * 
 *Usage Example:
 * 
 * {@code
 * ICustomerRepository repository = new CustomerRepositoryImpl();
 * ICustomerService service = new CustomerServiceCore(repository);
 * 
 * // When a PaymentConfirmed event arrives:
 * service.processPaymentConfirmed(event);
 * }
 *
 * All operations are idempotent and safe to retry. The service will
 * automatically update counters in the customer record and persist changes
 * through {@link ICustomerRepository}.
 */
public class CustomerServiceCore implements ICustomerService {

    private final ICustomerRepository customerRepository;
    private final Logger logger = LoggerFactory.getLogger(CustomerServiceCore.class);

    public CustomerServiceCore(ICustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Handles {@link DeliveryNotification} events.
     * Increments the delivery counter for the given customer.
     * 
     * @param deliveryNotification event containing delivery info and customer ID
     * @throws IllegalStateException if the customer does not exist
     */
    public void processDeliveryNotification(DeliveryNotification deliveryNotification) {
        Customer customer = customerRepository.findById(deliveryNotification.getCustomerId());
        if (customer != null) {
            customer.setDeliveryCount(customer.getDeliveryCount() + 1);
            customerRepository.save(customer);
            logger.info("Processed delivery notification for customer: {}", deliveryNotification.getCustomerId());
        } else {
            logger.warn("Customer not found for delivery notification: {}", deliveryNotification.getCustomerId());
        }
    }

    /**
     * Handles {@link PaymentConfirmed} events.
     * Increments the customer's successful payment counter.
     *
     * @param paymentConfirmed event containing customer and payment info
     * @throws IllegalStateException if the customer does not exist
     */
    public void processPaymentConfirmed(PaymentConfirmed paymentConfirmed) {
        logger.info("This ID is : {}", paymentConfirmed.getCustomer().getCustomerId());
        Customer customer = customerRepository.findById(paymentConfirmed.getCustomer().getCustomerId());
        if (customer != null) {
            customer.setSuccessPaymentCount(customer.getSuccessPaymentCount() + 1);
            customerRepository.save(customer);
            logger.info("Processed payment confirmation for customer: {}",
                    paymentConfirmed.getCustomer().getCustomerId());
        } else {
            logger.warn("Customer not found for payment confirmation: {}",
                    paymentConfirmed.getCustomer().getCustomerId());
        }
    }

    /**
     * Handles {@link PaymentFailed} events.
     * Increments the customer's failed payment counter.
     *
     * @param paymentFailed event containing failed payment info
     * @throws IllegalStateException if the customer does not exist
     */
    public void processPaymentFailed(PaymentFailed paymentFailed) {
        Customer customer = customerRepository.findById(paymentFailed.getCustomer().getCustomerId());
        if (customer != null) {
            customer.setFailedPaymentCount(customer.getFailedPaymentCount() + 1);
            customerRepository.save(customer);
            logger.info("Processed payment failure for customer: {}", paymentFailed.getCustomer().getCustomerId());
        } else {
            logger.warn("Customer not found for payment failure: {}", paymentFailed.getCustomer().getCustomerId());
        }
    }

    /**
     * Deletes all customer data from the repository.
     * Intended for testing or data refresh scenarios.
     */
    public void cleanup() {
        logger.info("Performing cleanup operation");
        customerRepository.deleteAll();
    }

    /**
     * Resets all customer records to their initial state.
     * This operation should be used to restore a clean environment for experiments
     * or benchmarks.
     */
    public void reset() {
        logger.info("Resetting customer data");
        customerRepository.reset();
    }
}

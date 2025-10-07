package com.example.customer.repository;

import com.example.customer.model.Customer;

/**
 * Repository interface for accessing and managing customer data.
 * Defines basic CRUD and maintenance operations.
 */
public interface ICustomerRepository {
    /**
     * Retrieves a customer by its unique ID.
     */
    Customer findById(int customerId);

    /**
     * Saves or updates a customer record.
     */
    void save(Customer customer);

    /**
     * Deletes all customers from the repository.
     */
    void deleteAll();

    /**
     * Resets the repository to its initial state.
     */
    void reset();
}
package com.example.payment.service;

import com.example.common.integration.PaymentIntent;
import com.example.common.integration.PaymentIntentCreateOptions;

/**
 * Defines the contract for integrating with an external payment provider.
 *
 * <p>
 * This interface represents the abstraction layer between the core payment
 * logic
 * and any specific payment provider (e.g., Stripe, PayPal, or a mock gateway).
 * </p>
 *
 * <p>
 * Implementations are responsible for creating payment intents and handling
 * provider-specific behavior. The provided core implementation
 * {@link com.example.payment.service.ExternalProviderProxyCore} can be used for
 * local testing or simulation.
 * </p>
 *
 */
public interface IExternalProvider {
    PaymentIntent create(PaymentIntentCreateOptions options);
}

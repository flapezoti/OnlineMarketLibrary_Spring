package com.example.common.messaging;

/**
 * Thrown when an event is processed before the event it causally depends on has
 * been handled (e.g. a PaymentConfirmed for an order that has not been created
 * yet). Signals that the event should be retried later rather than discarded.
 */
public class PredecessorNotReadyException extends RuntimeException {

    public PredecessorNotReadyException(String message) {
        super(message);
    }
}

package com.example.common.messaging;

/**
 * Unified event publishing interface, all middleware (Kafka, Orleans) should implement this interface
 */
public interface IEventPublisher {
    /**
     * Publishes an event.
     *
     * @param topic destination topic
     * @param key   partition key: events sharing a key are delivered in order to one
     *              partition. Pass {@code null} when ordering does not matter.
     * @param event the event payload
     */
    void publishEvent(String topic, String key, Object event);
}

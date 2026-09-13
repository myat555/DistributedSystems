package com.dsp.common.kafka;

/**
 * Kafka topic names shared by the checkout saga participants
 * (order-service, inventory-service, payment-service) so the topic
 * contract lives in one place instead of being duplicated per service.
 */
public final class SagaTopics {

    public static final String ORDER_CREATED = "order.created";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_FAILED = "inventory.failed";
    public static final String INVENTORY_COMPENSATE = "inventory.compensate";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";

    private SagaTopics() {
    }
}

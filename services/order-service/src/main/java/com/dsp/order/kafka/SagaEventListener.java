package com.dsp.order.kafka;

import com.dsp.common.kafka.SagaTopics;
import com.dsp.order.event.InventoryFailedEvent;
import com.dsp.order.event.PaymentCompletedEvent;
import com.dsp.order.event.PaymentFailedEvent;
import com.dsp.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes the saga events that drive order status: inventory.failed, payment.failed, payment.completed. */
@Component
public class SagaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public SagaEventListener(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = SagaTopics.INVENTORY_FAILED, groupId = "order-service-inventory-failed")
    public void onInventoryFailed(ConsumerRecord<String, String> record) {
        try {
            InventoryFailedEvent event = objectMapper.readValue(record.value(), InventoryFailedEvent.class);
            orderService.handleInventoryFailed(event.orderId(), event.reason());
        } catch (Exception e) {
            log.error("Failed to process inventory.failed record at offset {}", record.offset(), e);
        }
    }

    @KafkaListener(topics = SagaTopics.PAYMENT_FAILED, groupId = "order-service-payment-failed")
    public void onPaymentFailed(ConsumerRecord<String, String> record) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(record.value(), PaymentFailedEvent.class);
            orderService.handlePaymentFailed(event.orderId(), event.reason());
        } catch (Exception e) {
            log.error("Failed to process payment.failed record at offset {}", record.offset(), e);
        }
    }

    @KafkaListener(topics = SagaTopics.PAYMENT_COMPLETED, groupId = "order-service-payment-completed")
    public void onPaymentCompleted(ConsumerRecord<String, String> record) {
        try {
            PaymentCompletedEvent event = objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
            orderService.handlePaymentCompleted(event.orderId());
        } catch (Exception e) {
            log.error("Failed to process payment.completed record at offset {}", record.offset(), e);
        }
    }
}

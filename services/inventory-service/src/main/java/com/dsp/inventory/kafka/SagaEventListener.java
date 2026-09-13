package com.dsp.inventory.kafka;

import com.dsp.common.kafka.SagaTopics;
import com.dsp.inventory.event.InventoryCompensateEvent;
import com.dsp.inventory.event.OrderCreatedEvent;
import com.dsp.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes order.created (reserve stock) and inventory.compensate (release stock). */
@Component
public class SagaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;

    public SagaEventListener(ObjectMapper objectMapper, InventoryService inventoryService) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = SagaTopics.ORDER_CREATED, groupId = "inventory-service-order-created")
    public void onOrderCreated(ConsumerRecord<String, String> record) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
            inventoryService.reserve(event);
        } catch (Exception e) {
            log.error("Failed to process order.created record at offset {}", record.offset(), e);
        }
    }

    @KafkaListener(topics = SagaTopics.INVENTORY_COMPENSATE, groupId = "inventory-service-compensate")
    public void onInventoryCompensate(ConsumerRecord<String, String> record) {
        try {
            InventoryCompensateEvent event = objectMapper.readValue(record.value(), InventoryCompensateEvent.class);
            inventoryService.compensate(event);
        } catch (Exception e) {
            log.error("Failed to process inventory.compensate record at offset {}", record.offset(), e);
        }
    }
}

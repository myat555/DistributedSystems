package com.dsp.payment.kafka;

import com.dsp.common.kafka.SagaTopics;
import com.dsp.payment.event.InventoryReservedEvent;
import com.dsp.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes inventory.reserved and attempts to charge the order. */
@Component
public class SagaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public SagaEventListener(ObjectMapper objectMapper, PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = SagaTopics.INVENTORY_RESERVED, groupId = "payment-service-inventory-reserved")
    public void onInventoryReserved(ConsumerRecord<String, String> record) {
        try {
            InventoryReservedEvent event = objectMapper.readValue(record.value(), InventoryReservedEvent.class);
            paymentService.charge(event);
        } catch (Exception e) {
            log.error("Failed to process inventory.reserved record at offset {}", record.offset(), e);
        }
    }
}

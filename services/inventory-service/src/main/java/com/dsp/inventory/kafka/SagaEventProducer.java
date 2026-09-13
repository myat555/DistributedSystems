package com.dsp.inventory.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Serializes saga events to JSON and publishes them keyed by orderId, for partition ordering. */
@Component
public class SagaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(SagaEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SagaEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, String orderId, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, orderId, json);
        } catch (Exception e) {
            log.error("Failed to publish event for order {} to topic {}", orderId, topic, e);
            throw new IllegalStateException(e);
        }
    }
}

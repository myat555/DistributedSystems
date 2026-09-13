package com.dsp.chat.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes every chat message to Kafka for durable history/audit/replay, separate
 * from (and slower/more durable than) the low-latency Redis Pub/Sub fanout path used
 * for live delivery. Messages are keyed by roomId so Kafka's per-partition ordering
 * guarantee preserves message order within a room even though different rooms may
 * land on different partitions.
 */
@Component
public class ChatMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageProducer.class);
    public static final String TOPIC = "chat.messages";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ChatMessageProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Fire-and-forget async send; the WebSocket hot path never blocks on Kafka. */
    public void send(String roomId, String payloadJson) {
        kafkaTemplate.send(TOPIC, roomId, payloadJson)
                .exceptionally(ex -> {
                    log.error("Failed to publish chat message for room {} to Kafka topic {}", roomId, TOPIC, ex);
                    return null;
                });
    }
}

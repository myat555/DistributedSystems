package com.dsp.jobqueue.kafka;

import com.dsp.jobqueue.domain.JobMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobProducer {

    private static final Logger log = LoggerFactory.getLogger(JobProducer.class);
    public static final String DLQ_TOPIC = "jobs.dlq";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public JobProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, JobMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, message.jobId(), json);
        } catch (Exception e) {
            log.error("Failed to publish job {} to topic {}", message.jobId(), topic, e);
            throw new IllegalStateException(e);
        }
    }

    public void publishToDlq(JobMessage message) {
        publish(DLQ_TOPIC, message);
    }
}

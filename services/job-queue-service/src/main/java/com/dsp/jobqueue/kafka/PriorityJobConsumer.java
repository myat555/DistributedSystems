package com.dsp.jobqueue.kafka;

import com.dsp.jobqueue.domain.JobMessage;
import com.dsp.jobqueue.domain.JobPriority;
import com.dsp.jobqueue.service.JobProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implements strict-ish priority scheduling across the three per-priority Kafka
 * topics using manual partition pause/resume on a single consumer: each cycle it
 * tries HIGH first, only drains MEDIUM if HIGH had nothing ready, and only drains
 * LOW if both HIGH and MEDIUM were empty. This is the standard "Kafka as a
 * priority queue" pattern, since Kafka itself has no notion of message priority.
 */
@Component
public class PriorityJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriorityJobConsumer.class);

    private final ObjectMapper objectMapper;
    private final JobProcessingService processingService;
    private final String bootstrapServers;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "priority-job-consumer");
        t.setDaemon(true);
        return t;
    });

    private volatile KafkaConsumer<String, String> consumer;
    private volatile boolean running = true;

    public PriorityJobConsumer(ObjectMapper objectMapper,
                                JobProcessingService processingService,
                                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.objectMapper = objectMapper;
        this.processingService = processingService;
        this.bootstrapServers = bootstrapServers;
    }

    @PostConstruct
    public void start() {
        executor.submit(this::runLoop);
    }

    private void runLoop() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "job-queue-worker");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<>(props);
        try {
            consumer.subscribe(List.of(JobPriority.HIGH.topic(), JobPriority.MEDIUM.topic(), JobPriority.LOW.topic()));
            // Force an initial poll so partitions are assigned before we try to pause/resume them.
            consumer.poll(Duration.ofMillis(500));

            while (running) {
                if (!drain(JobPriority.HIGH, Duration.ofMillis(200))) {
                    if (!drain(JobPriority.MEDIUM, Duration.ofMillis(200))) {
                        drain(JobPriority.LOW, Duration.ofMillis(400));
                    }
                }
            }
        } catch (WakeupException e) {
            // expected on shutdown
        } catch (Exception e) {
            log.error("Priority job consumer loop terminated unexpectedly", e);
        } finally {
            consumer.close();
        }
    }

    /** Pauses every partition except {@code priority}'s topic, polls, processes, commits. Returns true if any record was handled. */
    private boolean drain(JobPriority priority, Duration timeout) {
        Set<TopicPartition> all = consumer.assignment();
        Set<TopicPartition> target = all.stream()
                .filter(tp -> tp.topic().equals(priority.topic()))
                .collect(Collectors.toSet());
        Set<TopicPartition> others = all.stream()
                .filter(tp -> !target.contains(tp))
                .collect(Collectors.toSet());

        if (!others.isEmpty()) {
            consumer.pause(others);
        }
        if (!target.isEmpty()) {
            consumer.resume(target);
        }

        ConsumerRecords<String, String> records = consumer.poll(timeout);
        if (records.isEmpty()) {
            return false;
        }
        for (ConsumerRecord<String, String> record : records) {
            handle(record);
        }
        consumer.commitSync();
        return true;
    }

    private void handle(ConsumerRecord<String, String> record) {
        try {
            JobMessage message = objectMapper.readValue(record.value(), JobMessage.class);
            processingService.process(message);
        } catch (Exception e) {
            log.error("Failed to deserialize/process job record at offset {}", record.offset(), e);
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
        }
        executor.shutdown();
    }
}

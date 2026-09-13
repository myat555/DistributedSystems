package com.dsp.payment.service;

import com.dsp.common.kafka.SagaTopics;
import com.dsp.payment.domain.PaymentRecord;
import com.dsp.payment.domain.PaymentStatus;
import com.dsp.payment.event.InventoryReservedEvent;
import com.dsp.payment.event.PaymentCompletedEvent;
import com.dsp.payment.event.PaymentFailedEvent;
import com.dsp.payment.kafka.SagaEventProducer;
import com.dsp.payment.repo.PaymentRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates charging for an order once inventory has been reserved. A random failure rate is
 * injected on top of the (always-succeeding) simulated charge itself, purely so the saga's
 * failure/compensation path is reliably exercised under load rather than only on true errors.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** ~10% simulated charge failure, purely to make the saga's failure/compensation path demonstrable under load. */
    private static final double SIMULATED_FAILURE_RATE = 0.10;

    private final PaymentRecordRepository repository;
    private final SagaEventProducer eventProducer;

    public PaymentService(PaymentRecordRepository repository, SagaEventProducer eventProducer) {
        this.repository = repository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public void charge(InventoryReservedEvent event) {
        boolean simulatedFailure = ThreadLocalRandom.current().nextDouble() < SIMULATED_FAILURE_RATE;
        PaymentStatus status = simulatedFailure ? PaymentStatus.FAILED : PaymentStatus.SUCCEEDED;

        PaymentRecord record = new PaymentRecord(UUID.randomUUID().toString(), event.orderId(),
                event.amount(), status, Instant.now());
        repository.save(record);

        if (status == PaymentStatus.SUCCEEDED) {
            log.info("Charged {} for order {}", event.amount(), event.orderId());
            eventProducer.publish(SagaTopics.PAYMENT_COMPLETED, event.orderId(),
                    new PaymentCompletedEvent(event.orderId()));
        } else {
            log.info("Charge failed for order {}: simulated failure", event.orderId());
            eventProducer.publish(SagaTopics.PAYMENT_FAILED, event.orderId(),
                    new PaymentFailedEvent(event.orderId(), "simulated failure"));
        }
    }
}

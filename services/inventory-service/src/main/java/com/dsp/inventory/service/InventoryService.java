package com.dsp.inventory.service;

import com.dsp.common.kafka.SagaTopics;
import com.dsp.inventory.domain.InventoryItem;
import com.dsp.inventory.event.InventoryCompensateEvent;
import com.dsp.inventory.event.InventoryFailedEvent;
import com.dsp.inventory.event.InventoryReservedEvent;
import com.dsp.inventory.event.OrderCreatedEvent;
import com.dsp.inventory.kafka.SagaEventProducer;
import com.dsp.inventory.repo.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Reserves and releases stock for the checkout saga. Reservation failures include both a real
 * insufficient-stock check and an injected random failure rate, so the saga's compensation path
 * (order cancellation + inventory release) is reliably exercised under load, not just when
 * stock is actually exhausted.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    /** ~10% simulated reservation failure, purely to make the saga's failure/compensation path demonstrable under load. */
    private static final double SIMULATED_FAILURE_RATE = 0.10;

    private final InventoryItemRepository repository;
    private final SagaEventProducer eventProducer;

    public InventoryService(InventoryItemRepository repository, SagaEventProducer eventProducer) {
        this.repository = repository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public void reserve(OrderCreatedEvent event) {
        InventoryItem item = repository.findById(event.itemId()).orElse(null);
        if (item == null) {
            log.warn("Order {} references unknown item {}, failing reservation", event.orderId(), event.itemId());
            publishFailed(event.orderId(), "unknown item: " + event.itemId());
            return;
        }

        if (item.getAvailableQuantity() < event.quantity()) {
            log.info("Order {} reservation failed: insufficient stock for {}", event.orderId(), event.itemId());
            publishFailed(event.orderId(), "insufficient stock");
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() < SIMULATED_FAILURE_RATE) {
            log.info("Order {} reservation failed: simulated failure for {}", event.orderId(), event.itemId());
            publishFailed(event.orderId(), "simulated failure");
            return;
        }

        item.setAvailableQuantity(item.getAvailableQuantity() - event.quantity());
        item.setReservedQuantity(item.getReservedQuantity() + event.quantity());
        repository.save(item);
        log.info("Reserved {} units of {} for order {}", event.quantity(), event.itemId(), event.orderId());

        eventProducer.publish(SagaTopics.INVENTORY_RESERVED, event.orderId(),
                new InventoryReservedEvent(event.orderId(), event.itemId(), event.quantity(), event.amount()));
    }

    @Transactional
    public void compensate(InventoryCompensateEvent event) {
        InventoryItem item = repository.findById(event.itemId()).orElse(null);
        if (item == null) {
            log.warn("Cannot compensate order {}: unknown item {}", event.orderId(), event.itemId());
            return;
        }
        int releasedFromReserved = Math.max(0, item.getReservedQuantity() - event.quantity());
        item.setReservedQuantity(releasedFromReserved);
        item.setAvailableQuantity(item.getAvailableQuantity() + event.quantity());
        repository.save(item);
        log.info("Compensated order {}: released {} units of {} back to available stock",
                event.orderId(), event.quantity(), event.itemId());
    }

    private void publishFailed(String orderId, String reason) {
        eventProducer.publish(SagaTopics.INVENTORY_FAILED, orderId, new InventoryFailedEvent(orderId, reason));
    }
}

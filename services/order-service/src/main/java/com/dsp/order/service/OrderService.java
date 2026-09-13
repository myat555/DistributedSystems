package com.dsp.order.service;

import com.dsp.common.kafka.SagaTopics;
import com.dsp.order.api.CreateOrderRequest;
import com.dsp.order.api.OrderResponse;
import com.dsp.order.domain.Order;
import com.dsp.order.domain.OrderStatus;
import com.dsp.order.event.InventoryCompensateEvent;
import com.dsp.order.event.OrderCreatedEvent;
import com.dsp.order.kafka.SagaEventProducer;
import com.dsp.order.repo.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Owns order state and the order-service side of the checkout saga: creating orders,
 * and reacting to the downstream inventory/payment events that move an order to its
 * terminal state (CONFIRMED or CANCELLED), including firing the compensating
 * inventory.compensate event when payment fails after inventory was already reserved.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final SagaEventProducer eventProducer;

    public OrderService(OrderRepository orderRepository, SagaEventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Instant now = Instant.now();
        Order order = new Order(UUID.randomUUID().toString(), request.customerId(), request.itemId(),
                request.quantity(), request.amount(), OrderStatus.PENDING, now, now);
        orderRepository.save(order);

        eventProducer.publish(SagaTopics.ORDER_CREATED, order.getId(),
                new OrderCreatedEvent(order.getId(), order.getItemId(), order.getQuantity(), order.getAmount()));
        log.info("Order {} created for customer {}, published order.created", order.getId(), order.getCustomerId());
        return OrderResponse.from(order);
    }

    public OrderResponse getOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No order with id " + id));
        return OrderResponse.from(order);
    }

    @Transactional
    public void handleInventoryFailed(String orderId, String reason) {
        Order order = loadForTransition(orderId);
        if (order == null) {
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Order {} cancelled: inventory reservation failed ({})", orderId, reason);
    }

    @Transactional
    public void handlePaymentFailed(String orderId, String reason) {
        Order order = loadForTransition(orderId);
        if (order == null) {
            return;
        }
        String itemId = order.getItemId();
        int quantity = order.getQuantity();

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Order {} cancelled: payment failed ({}); publishing compensating inventory.compensate",
                orderId, reason);

        eventProducer.publish(SagaTopics.INVENTORY_COMPENSATE, orderId,
                new InventoryCompensateEvent(orderId, itemId, quantity));
    }

    @Transactional
    public void handlePaymentCompleted(String orderId) {
        Order order = loadForTransition(orderId);
        if (order == null) {
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);
        log.info("Order {} confirmed: payment completed", orderId);
    }

    /** Loads the order for a saga transition, guarding against unknown orders and re-processing a terminal one. */
    private Order loadForTransition(String orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Received saga event for unknown order {}, ignoring", orderId);
            return null;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} already terminal ({}), ignoring duplicate/late saga event", orderId, order.getStatus());
            return null;
        }
        return order;
    }
}

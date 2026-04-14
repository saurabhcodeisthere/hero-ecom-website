package com.hero.bikestore.saga;

import com.hero.bikestore.client.InventoryServiceClient;
import com.hero.bikestore.dto.event.OrderNotificationEvent;
import com.hero.bikestore.dto.payment.PaymentResultEvent;
import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderItem;
import com.hero.bikestore.entity.OrderStatus;
import com.hero.bikestore.enums.OrderEventType;
import com.hero.bikestore.exception.OrderNotFoundException;
import com.hero.bikestore.publisher.EventPublisher;
import com.hero.bikestore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

/**
 * Coordinates the order side of the payment saga.
 *
 * SUCCESS path:
 *   1. Find order by orderId
 *   2. Transition AWAITING_PAYMENT → CONFIRMED
 *   3. Publish order.confirmed notification
 *
 * FAILURE path (compensation):
 *   1. Find order by orderId
 *   2. Restore stock for every item (compensation step)
 *   3. Transition AWAITING_PAYMENT → CANCELLED
 *   4. Publish order.cancelled notification
 *
 * SRP: Owns only saga coordination. Does not know about RabbitMQ internals,
 *      HTTP details, or checkout pages.
 *
 * DIP: Depends on EventPublisher interface, InventoryServiceClient interface,
 *      OrderRepository — all abstractions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaOrchestrator implements SagaOrchestrator {

    private final OrderRepository        orderRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final EventPublisher         eventPublisher;

    @Override
    @Transactional
    public void handlePaymentSuccess(PaymentResultEvent event) {
        log.info("[OrderSagaOrchestrator] handlePaymentSuccess | ENTER");
        log.info("[OrderSagaOrchestrator] orderId={} orderNumber={} transactionId={}",
                event.getOrderId(), event.getOrderNumber(), event.getTransactionId());

        log.info("[OrderSagaOrchestrator] STEP 1/2 — Looking up Order in DB | orderId={}", event.getOrderId());
        Order order = findOrder(event.getOrderId());
        log.info("[OrderSagaOrchestrator] STEP 1/2 — Order found | orderNumber={} currentStatus={}",
                order.getOrderNumber(), order.getStatus());

        // GUARD — only confirm if still AWAITING_PAYMENT
        // CANCELLED = timeout job already cancelled this order before payment arrived
        //             (race condition: customer paid just as timeout fired)
        // CONFIRMED = duplicate event — already processed, skip safely
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            log.warn("[OrderSagaOrchestrator] handlePaymentSuccess — SKIPPED. Order is not AWAITING_PAYMENT | " +
                     "orderNumber={} currentStatus={}. Order will NOT be confirmed.",
                    order.getOrderNumber(), order.getStatus());
            return;
        }

        log.info("[OrderSagaOrchestrator] STEP 2/2 — Transitioning status {} → CONFIRMED", order.getStatus());
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("[OrderSagaOrchestrator] STEP 2/2 — Order saved | orderNumber={} newStatus=CONFIRMED transactionId={}",
                order.getOrderNumber(), event.getTransactionId());

        notify(order, OrderEventType.ORDER_CONFIRMED);

        log.info("[OrderSagaOrchestrator] handlePaymentSuccess | EXIT — orderNumber={} ✅ CONFIRMED",
                order.getOrderNumber());
    }

    @Override
    @Transactional
    public void handlePaymentFailure(PaymentResultEvent event) {
        log.info("[OrderSagaOrchestrator] handlePaymentFailure | ENTER");
        log.info("[OrderSagaOrchestrator] orderId={} orderNumber={} reason={}",
                event.getOrderId(), event.getOrderNumber(), event.getFailureReason());

        log.info("[OrderSagaOrchestrator] STEP 1/3 — Looking up Order in DB | orderId={}", event.getOrderId());
        Order order = findOrder(event.getOrderId());
        log.info("[OrderSagaOrchestrator] STEP 1/3 — Order found | orderNumber={} currentStatus={}",
                order.getOrderNumber(), order.getStatus());

        // GUARD — only cancel if still AWAITING_PAYMENT
        // CANCELLED = timeout job already cancelled this order — don't restore stock twice
        // CONFIRMED = already confirmed (should not happen, but guard anyway)
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            log.warn("[OrderSagaOrchestrator] handlePaymentFailure — SKIPPED. Order is not AWAITING_PAYMENT | " +
                     "orderNumber={} currentStatus={}. Stock will NOT be double-restored.",
                    order.getOrderNumber(), order.getStatus());
            return;
        }

        // Compensation — restore stock for every item
        log.info("[OrderSagaOrchestrator] STEP 2/3 — Restoring stock for {} item(s) (compensation)",
                order.getItems().size());
        restoreStockForOrder(order);
        log.info("[OrderSagaOrchestrator] STEP 2/3 — Stock restore complete");

        log.info("[OrderSagaOrchestrator] STEP 3/3 — Transitioning status {} → CANCELLED | reason={}",
                order.getStatus(), event.getFailureReason());
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("[OrderSagaOrchestrator] STEP 3/3 — Order saved | orderNumber={} newStatus=CANCELLED",
                order.getOrderNumber());

        notify(order, OrderEventType.ORDER_CANCELLED);

        log.info("[OrderSagaOrchestrator] handlePaymentFailure | EXIT — orderNumber={} ❌ CANCELLED",
                order.getOrderNumber());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Order findOrder(String orderId) {
        Long id = Long.parseLong(orderId);
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Restores inventory stock for every item.
     * Best-effort: logs error and continues if a restore call fails.
     * The order is still cancelled — stock discrepancy is flagged for ops.
     */
    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                inventoryServiceClient.restoreStock(item.getBikeId(), item.getQuantity());
                log.info("Stock restored — bikeId={} quantity={}", item.getBikeId(), item.getQuantity());
            } catch (RestClientException e) {
                log.error("Failed to restore stock for bikeId={} on order {}: {}",
                        item.getBikeId(), order.getOrderNumber(), e.getMessage());
            }
        }
    }

    private void notify(Order order, OrderEventType type) {
        try {
            List<OrderNotificationEvent.OrderItemEvent> itemEvents = order.getItems().stream()
                    .map(item -> OrderNotificationEvent.OrderItemEvent.builder()
                            .bikeName(item.getBikeName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getBikePrice())
                            .subtotal(item.getSubtotal())
                            .build())
                    .toList();

            OrderNotificationEvent event = OrderNotificationEvent.builder()
                    .type(type)
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .userEmail(order.getUserEmail())
                    .occurredAt(Instant.now())
                    .items(itemEvents)
                    .totalAmount(order.getTotalAmount())
                    .shippingAddress(order.getShippingAddress() != null
                            ? order.getShippingAddress().toDisplayString() : null)
                    .metadata(OrderNotificationEvent.EventMetadata.builder().build())
                    .build();

            eventPublisher.publish(event);

        } catch (Exception e) {
            log.warn("Could not send notification for order={} type={}: {}",
                    order.getOrderNumber(), type, e.getMessage());
        }
    }
}

package com.hero.bikestore.service;

import com.hero.bikestore.client.InventoryServiceClient;
import com.hero.bikestore.client.PaymentServiceClient;
import com.hero.bikestore.config.rabbitmq.StockRestoreFailedQueueConfig;
import com.hero.bikestore.dto.event.OrderNotificationEvent;
import com.hero.bikestore.dto.event.StockRestoreFailedEvent;
import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderItem;
import com.hero.bikestore.entity.OrderStatus;
import com.hero.bikestore.enums.OrderEventType;
import com.hero.bikestore.publisher.EventPublisher;
import com.hero.bikestore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements the two-phase payment timeout feature.
 *
 * Phase 1 (sendExpiryWarnings)  — warning email at warningMinutes (default: 10)
 * Phase 2 (cancelExpiredOrders) — hard cancel at cancelMinutes   (default: 15)
 *
 * TRANSACTIONAL DESIGN:
 * ──────────────────────
 * Both public methods are @Transactional. The transaction wraps the full loop.
 * HTTP calls (restoreStock, expirePayment) are non-transactional and wrapped
 * in their own try/catch — a failing HTTP call does NOT roll back DB updates
 * for other orders in the same run.
 *
 * @SCHEDULED + @TRANSACTIONAL SEPARATION:
 * ─────────────────────────────────────────
 * @Scheduled lives in PaymentTimeoutJob. @Transactional lives here.
 * PaymentTimeoutJob injects TimeoutService (interface), so calls go through
 * Spring's proxy — @Transactional is properly intercepted.
 * Mixing both annotations on the same method bypasses the proxy.
 *
 * @RequiredArgsConstructor + @Value:
 * ────────────────────────────────────
 * @RequiredArgsConstructor generates a constructor for FINAL fields only.
 * @Value fields are non-final — Spring injects them via field injection after
 * the constructor runs. Both coexist without conflict.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimeoutServiceImpl implements TimeoutService {

    private final OrderRepository        orderRepository;
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient   paymentServiceClient;
    private final EventPublisher         eventPublisher;
    private final RabbitTemplate         rabbitTemplate;

    @Value("${payment.timeout.warning-minutes:10}")
    private long warningMinutes;

    @Value("${payment.timeout.cancel-minutes:15}")
    private long cancelMinutes;

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 1 — WARNING
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void sendExpiryWarnings() {
        LocalDateTime now          = LocalDateTime.now();
        LocalDateTime warnCutoff   = now.minusMinutes(warningMinutes);
        LocalDateTime cancelCutoff = now.minusMinutes(cancelMinutes);

        log.info("[TimeoutServiceImpl] sendExpiryWarnings | ENTER — warnCutoff={} cancelCutoff={}",
                warnCutoff, cancelCutoff);

        List<Order> orders = orderRepository.findOrdersNeedingWarning(warnCutoff, cancelCutoff);

        if (orders.isEmpty()) {
            log.debug("[TimeoutServiceImpl] sendExpiryWarnings — no orders need warning this run");
            return;
        }

        log.info("[TimeoutServiceImpl] sendExpiryWarnings — {} order(s) need warning email", orders.size());

        for (Order order : orders) {
            try {
                log.info("[TimeoutServiceImpl] Processing warning | orderId={} orderNumber={} createdAt={}",
                        order.getId(), order.getOrderNumber(), order.getCreatedAt());

                // Mark BEFORE publishing — prevents duplicate warnings if publish is slow.
                // Worst case: mark succeeds but publish fails → customer misses one email.
                // Better than: publish succeeds N times → customer gets N warning emails.
                order.setPaymentReminderSentAt(LocalDateTime.now());
                orderRepository.save(order);

                notifyWarning(order);

                log.info("[TimeoutServiceImpl] Warning queued | orderId={} orderNumber={}",
                        order.getId(), order.getOrderNumber());

            } catch (Exception e) {
                log.error("[TimeoutServiceImpl] Failed to process warning for orderId={} orderNumber={}: {}",
                        order.getId(), order.getOrderNumber(), e.getMessage(), e);
                // Do NOT re-throw — continue processing remaining orders
            }
        }

        log.info("[TimeoutServiceImpl] sendExpiryWarnings | EXIT");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHASE 2 — CANCEL
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime cancelCutoff = LocalDateTime.now().minusMinutes(cancelMinutes);

        log.info("[TimeoutServiceImpl] cancelExpiredOrders | ENTER — cancelCutoff={}", cancelCutoff);

        List<Order> orders = orderRepository.findExpiredOrders(cancelCutoff);

        if (orders.isEmpty()) {
            log.debug("[TimeoutServiceImpl] cancelExpiredOrders — no expired orders this run");
            return;
        }

        log.info("[TimeoutServiceImpl] cancelExpiredOrders — {} order(s) to cancel", orders.size());

        for (Order order : orders) {
            try {
                log.info("[TimeoutServiceImpl] Cancelling expired order | orderId={} orderNumber={} createdAt={}",
                        order.getId(), order.getOrderNumber(), order.getCreatedAt());

                // Step 1 — Restore stock (best-effort per item, failures published to DLQ)
                restoreStock(order);

                // Step 2 — Cancel the order
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("[TimeoutServiceImpl] Order status → CANCELLED | orderId={}", order.getId());

                // Step 3 — Expire payment in payment-service (best-effort — order already cancelled)
                expirePayment(order);

                // Step 4 — Notify customer
                notifyCancelled(order);

                log.info("[TimeoutServiceImpl] ❌ Order expired and cancelled | orderId={} orderNumber={}",
                        order.getId(), order.getOrderNumber());

            } catch (Exception e) {
                log.error("[TimeoutServiceImpl] Unexpected error cancelling orderId={} orderNumber={}: {}",
                        order.getId(), order.getOrderNumber(), e.getMessage(), e);
                // Do NOT re-throw — continue processing remaining orders
            }
        }

        log.info("[TimeoutServiceImpl] cancelExpiredOrders | EXIT");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — STOCK RESTORE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Restores stock for every item in the order.
     *
     * Two-level exception handling per item:
     *   RestClientException → expected HTTP failure (service down, timeout, 5xx)
     *   Exception           → unexpected failure (NPE, serialisation error, etc.)
     *
     * Both paths:
     *   1. Log "MANUAL ACTION REQUIRED" — ops team can alert on this keyword in log aggregators
     *   2. Publish StockRestoreFailedEvent to DLQ — durable audit trail in RabbitMQ
     *   3. Continue — a single item failure does NOT block stock restore for other items
     */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                inventoryServiceClient.restoreStock(item.getBikeId(), item.getQuantity());
                log.info("[TimeoutServiceImpl] Stock restored — bikeId={} qty={} orderId={}",
                        item.getBikeId(), item.getQuantity(), order.getId());

            } catch (RestClientException e) {
                log.error("⚠️  MANUAL ACTION REQUIRED — STOCK NOT RESTORED");
                log.error("⚠️  orderId={} orderNumber={} bikeId={} bikeName={} quantity={} reason={}",
                        order.getId(), order.getOrderNumber(),
                        item.getBikeId(), item.getBikeName(), item.getQuantity(), e.getMessage());
                publishStockRestoreFailedEvent(order, item, e.getMessage());

            } catch (Exception e) {
                log.error("⚠️  MANUAL ACTION REQUIRED — STOCK NOT RESTORED (unexpected error)");
                log.error("⚠️  orderId={} orderNumber={} bikeId={} bikeName={} quantity={} reason={}",
                        order.getId(), order.getOrderNumber(),
                        item.getBikeId(), item.getBikeName(), item.getQuantity(), e.getMessage());
                publishStockRestoreFailedEvent(order, item, "Unexpected: " + e.getMessage());
            }
        }
    }

    /**
     * Publishes a StockRestoreFailedEvent to the DLQ.
     * Wrapped in its own try/catch — if RabbitMQ is also down, we log CRITICAL
     * and move on. The "MANUAL ACTION REQUIRED" log above is still searchable.
     */
    private void publishStockRestoreFailedEvent(Order order, OrderItem item, String reason) {
        try {
            StockRestoreFailedEvent failedEvent = StockRestoreFailedEvent.builder()
                    .orderId(order.getId().toString())
                    .orderNumber(order.getOrderNumber())
                    .bikeId(item.getBikeId())
                    .bikeName(item.getBikeName())
                    .quantity(item.getQuantity())
                    .failedAt(LocalDateTime.now())
                    .reason(reason)
                    .retryCount(0)
                    .build();

            rabbitTemplate.convertAndSend(
                    StockRestoreFailedQueueConfig.EXCHANGE_NAME,
                    StockRestoreFailedQueueConfig.ROUTING_KEY,
                    failedEvent
            );

            log.info("[TimeoutServiceImpl] StockRestoreFailedEvent published to DLQ | orderId={} bikeId={}",
                    order.getId(), item.getBikeId());

        } catch (Exception e) {
            log.error("⚠️  CRITICAL — Failed to publish to DLQ. orderId={} bikeId={} bikeName={} quantity={}: {}",
                    order.getId(), item.getBikeId(), item.getBikeName(), item.getQuantity(), e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — EXPIRE PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tells payment-service to mark this payment as EXPIRED.
     * Best-effort — the order is already CANCELLED at this point regardless.
     * If this call fails, the payment stays INITIATED in payment-service DB
     * (slightly inconsistent) but the order is still correctly cancelled.
     */
    private void expirePayment(Order order) {
        try {
            paymentServiceClient.expirePayment(order.getId().toString());
            log.info("[TimeoutServiceImpl] Payment expired | orderId={}", order.getId());
        } catch (RestClientException e) {
            log.warn("[TimeoutServiceImpl] Could not expire payment (payment-service unavailable) | orderId={} reason={}",
                    order.getId(), e.getMessage());
        } catch (Exception e) {
            log.warn("[TimeoutServiceImpl] Unexpected error expiring payment | orderId={} reason={}",
                    order.getId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — NOTIFICATIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Publishes ORDER_PAYMENT_EXPIRY_WARNING event.
     * Includes paymentUrl in metadata so the email template can show a direct link.
     */
    private void notifyWarning(Order order) {
        try {
            OrderNotificationEvent event = OrderNotificationEvent.builder()
                    .type(OrderEventType.ORDER_PAYMENT_EXPIRY_WARNING)
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .userEmail(order.getUserEmail())
                    .occurredAt(Instant.now())
                    .items(buildItemEvents(order))
                    .totalAmount(order.getTotalAmount())
                    .shippingAddress(order.getShippingAddress() != null
                            ? order.getShippingAddress().toDisplayString() : null)
                    .metadata(OrderNotificationEvent.EventMetadata.builder()
                            .paymentUrl(order.getPaymentUrl())
                            .build())
                    .build();

            eventPublisher.publish(event);

        } catch (Exception e) {
            log.warn("[TimeoutServiceImpl] Could not publish warning notification for orderId={}: {}",
                    order.getId(), e.getMessage());
        }
    }

    /**
     * Publishes ORDER_CANCELLED event after timeout cancellation.
     */
    private void notifyCancelled(Order order) {
        try {
            OrderNotificationEvent event = OrderNotificationEvent.builder()
                    .type(OrderEventType.ORDER_CANCELLED)
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .userEmail(order.getUserEmail())
                    .occurredAt(Instant.now())
                    .items(buildItemEvents(order))
                    .totalAmount(order.getTotalAmount())
                    .shippingAddress(order.getShippingAddress() != null
                            ? order.getShippingAddress().toDisplayString() : null)
                    .metadata(OrderNotificationEvent.EventMetadata.builder()
                            .cancellationReason("Payment not completed within the allowed time.")
                            .build())
                    .build();

            eventPublisher.publish(event);

        } catch (Exception e) {
            log.warn("[TimeoutServiceImpl] Could not publish cancelled notification for orderId={}: {}",
                    order.getId(), e.getMessage());
        }
    }

    private List<OrderNotificationEvent.OrderItemEvent> buildItemEvents(Order order) {
        return order.getItems().stream()
                .map(item -> OrderNotificationEvent.OrderItemEvent.builder()
                        .bikeName(item.getBikeName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getBikePrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();
    }
}

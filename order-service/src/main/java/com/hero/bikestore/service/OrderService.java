package com.hero.bikestore.service;

import com.hero.bikestore.client.BikeServiceClient;
import com.hero.bikestore.client.InventoryServiceClient;
import com.hero.bikestore.client.response.BikeClientResponse;
import com.hero.bikestore.client.response.InventoryClientResponse;
import com.hero.bikestore.common.exception.base.ForbiddenException;
import com.hero.bikestore.dto.event.OrderNotificationEvent;
import com.hero.bikestore.dto.request.OrderItemRequest;
import com.hero.bikestore.dto.request.PlaceOrderRequest;
import com.hero.bikestore.dto.response.AdminOrderResponse;
import com.hero.bikestore.dto.response.OrderResponse;
import com.hero.bikestore.dto.response.PagedOrderResponse;
import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderItem;
import com.hero.bikestore.entity.OrderStatus;
import com.hero.bikestore.enums.OrderEventType;
import com.hero.bikestore.exception.BikeNotFoundException;
import com.hero.bikestore.exception.InsufficientStockException;
import com.hero.bikestore.exception.InvalidOrderStateException;
import com.hero.bikestore.exception.OrderNotFoundException;
import com.hero.bikestore.exception.ServiceUnavailableException;
import com.hero.bikestore.mapper.OrderMapper;
import com.hero.bikestore.publisher.EventPublisher;
import com.hero.bikestore.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final BikeServiceClient bikeServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final EventPublisher eventPublisher;

    // ═════════════════════════════════════════════════════════════════════════
    // CUSTOMER OPERATIONS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Places a new order.
     *
     * For each item:
     *   1. Call bike-service  → validate bike exists, snapshot name + price
     *   2. Call inventory-service → validate stock is available
     *   3. Call inventory-service → reduce stock atomically
     *   4. Build OrderItem with snapshotted data
     *
     * Circuit breaker wraps the whole method — if a downstream service is
     * down after retries, circuit opens and returns 503 immediately.
     */
    @Transactional
    @CircuitBreaker(name = "order-placement", fallbackMethod = "placeOrderFallback")
    @Retry(name = "order-placement")
    public OrderResponse placeOrder(PlaceOrderRequest request, Jwt jwt) {
        String userId = jwt.getSubject();
        String userEmail = jwt.getClaimAsString("email");

        log.info("Placing order for userId={}, itemCount={}", userId, request.getItems().size());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .userEmail(userEmail)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .totalAmount(BigDecimal.ZERO)
                .build();

        for (OrderItemRequest itemRequest : request.getItems()) {

            // Step 1: Validate bike exists, get name + price
            BikeClientResponse bike = fetchBike(itemRequest.getBikeId());

            // Step 2: Validate stock is sufficient
            InventoryClientResponse inventory = fetchInventory(itemRequest.getBikeId());

            if (inventory.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        itemRequest.getBikeId(),
                        inventory.getStockQuantity(),
                        itemRequest.getQuantity()
                );
            }

            // Step 3: Reduce stock atomically in inventory-service
            inventoryServiceClient.reduceStock(itemRequest.getBikeId(), itemRequest.getQuantity());

            // Step 4: Build order item — price is a snapshot, not a reference
            BigDecimal price = bike.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem item = OrderItem.builder()
                    .bikeId(bike.getId())
                    .bikeName(bike.getModelName())   // snapshot: bike name at order time
                    .bikePrice(price)                // snapshot: price at order time
                    .quantity(itemRequest.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(item);
        }

        BigDecimal total = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        log.info("Order placed: orderNumber={}, total={}", saved.getOrderNumber(), saved.getTotalAmount());

        notify(saved, OrderEventType.ORDER_PLACED);

        return orderMapper.toResponse(saved);
    }

    /**
     * Circuit breaker fallback — called whenever placeOrder throws.
     * Signature must match placeOrder + Throwable at the end.
     *
     * Re-throws business exceptions (4xx) so they reach the client correctly.
     * Only connectivity failures (RestClientException) become 503.
     */
    public OrderResponse placeOrderFallback(PlaceOrderRequest request, Jwt jwt, Throwable t) {
        if (t instanceof BikeNotFoundException) throw (BikeNotFoundException) t;
        if (t instanceof InsufficientStockException) throw (InsufficientStockException) t;
        log.error("Circuit breaker triggered during placeOrder: {}", t.getMessage());
        throw new ServiceUnavailableException("Order placement service");
    }

    /**
     * Returns all orders placed by the authenticated customer, newest first.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Jwt jwt) {
        String userId = jwt.getSubject();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    /**
     * Returns a single order by ID.
     * Ownership check: customers can only view their own orders.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id, Jwt jwt) {
        String userId = jwt.getSubject();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (!order.getUserId().equals(userId)) {
            log.warn("User {} attempted to access order {} belonging to {}", userId, id, order.getUserId());
            throw new ForbiddenException("You are not authorized to view this order");
        }

        return orderMapper.toResponse(order);
    }

    /**
     * Cancels a customer's own order.
     *
     * Rules:
     *   - Customer can only cancel their own orders
     *   - Only PENDING orders can be cancelled by the customer
     *   - Stock is restored for every item in the order
     */
    @Transactional
    public void cancelMyOrder(Long id, Jwt jwt) {
        String userId = jwt.getSubject();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only PENDING orders can be cancelled by the customer. " +
                    "Current status: " + order.getStatus() +
                    ". Please contact support to cancel orders that are already confirmed or shipped."
            );
        }

        restoreStockForOrder(order);
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        log.info("Order {} cancelled by customer userId={}", order.getOrderNumber(), userId);

        notify(saved, OrderEventType.ORDER_CANCELLED);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN OPERATIONS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns all orders with pagination. Admin use only.
     *
     * If status is provided, filters by that status.
     * If status is null, returns all orders regardless of status.
     */
    @Transactional(readOnly = true)
    public PagedOrderResponse getAllOrders(int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orderPage = (status != null)
                ? orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : orderRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AdminOrderResponse> orders = orderPage.getContent()
                .stream()
                .map(orderMapper::toAdminResponse)
                .toList();

        return PagedOrderResponse.builder()
                .orders(orders)
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    /**
     * Returns any single order by ID. Admin use only — no ownership check.
     */
    @Transactional(readOnly = true)
    public AdminOrderResponse getAdminOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return orderMapper.toAdminResponse(order);
    }

    /**
     * PENDING → CONFIRMED
     * Admin confirms the order after verifying payment and availability.
     */
    @Transactional
    public AdminOrderResponse confirmOrder(Long id) {
        return transitionStatus(id, OrderStatus.PENDING, OrderStatus.CONFIRMED, "confirm");
    }

    /**
     * CONFIRMED → SHIPPED
     * Admin marks the order as dispatched.
     */
    @Transactional
    public AdminOrderResponse shipOrder(Long id) {
        return transitionStatus(id, OrderStatus.CONFIRMED, OrderStatus.SHIPPED, "ship");
    }

    /**
     * SHIPPED → DELIVERED
     * Admin marks the order as delivered to the customer.
     */
    @Transactional
    public AdminOrderResponse deliverOrder(Long id) {
        return transitionStatus(id, OrderStatus.SHIPPED, OrderStatus.DELIVERED, "deliver");
    }

    /**
     * Admin cancels an order. Allowed from PENDING or CONFIRMED state only.
     * Stock is always restored on cancellation.
     *
     * SHIPPED and DELIVERED orders cannot be cancelled — requires a separate
     * return/refund flow (future feature).
     */
    @Transactional
    public void adminCancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() == OrderStatus.SHIPPED ||
            order.getStatus() == OrderStatus.DELIVERED ||
            order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                    "Cannot cancel an order with status: " + order.getStatus() + ". " +
                    "Only PENDING and CONFIRMED orders can be cancelled."
            );
        }

        restoreStockForOrder(order);
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        log.info("Order {} cancelled by admin", order.getOrderNumber());

        notify(saved, OrderEventType.ORDER_CANCELLED);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Generic status transition — validates current status before updating.
     *
     * Prevents invalid transitions like PENDING → DELIVERED in one step,
     * or trying to ship an already CANCELLED order.
     */
    private AdminOrderResponse transitionStatus(Long id, OrderStatus from, OrderStatus to, String action) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != from) {
            throw new InvalidOrderStateException(
                    "Cannot " + action + " an order with status: " + order.getStatus() +
                    ". Required status: " + from
            );
        }

        order.setStatus(to);
        Order saved = orderRepository.save(order);

        log.info("Order {} transitioned: {} → {}", saved.getOrderNumber(), from, to);

        // Map the new status to its notification event type
        OrderEventType eventType = switch (to) {
            case CONFIRMED -> OrderEventType.ORDER_CONFIRMED;
            case SHIPPED   -> OrderEventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEventType.ORDER_DELIVERED;
            default        -> null;
        };
        if (eventType != null) {
            notify(saved, eventType);
        }

        return orderMapper.toAdminResponse(saved);
    }

    /**
     * Restores inventory stock for every item in the order.
     * Called whenever an order is cancelled (by customer or admin).
     *
     * Best-effort: if a stock restore call fails, it logs the error and
     * continues — the cancellation still succeeds.
     */
    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                inventoryServiceClient.restoreStock(item.getBikeId(), item.getQuantity());
                log.info("Stock restored for bikeId={}, quantity={}", item.getBikeId(), item.getQuantity());
            } catch (RestClientException e) {
                log.error("Failed to restore stock for bikeId={} on order {}: {}",
                        item.getBikeId(), order.getOrderNumber(), e.getMessage());
            }
        }
    }

    /**
     * Wraps bike-service HTTP call with proper exception translation.
     */
    private BikeClientResponse fetchBike(Long bikeId) {
        try {
            return bikeServiceClient.getBikeById(bikeId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BikeNotFoundException(bikeId);
        } catch (RestClientException e) {
            log.error("bike-service call failed for bikeId={}: {}", bikeId, e.getMessage());
            throw new ServiceUnavailableException("bike-service");
        }
    }

    /**
     * Wraps inventory-service HTTP call with proper exception translation.
     */
    private InventoryClientResponse fetchInventory(Long bikeId) {
        try {
            return inventoryServiceClient.getInventoryByBikeId(bikeId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BikeNotFoundException(bikeId);
        } catch (RestClientException e) {
            log.error("inventory-service call failed for bikeId={}: {}", bikeId, e.getMessage());
            throw new ServiceUnavailableException("inventory-service");
        }
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().toString().replace("-", "");
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + date + "-" + unique;
    }

    /**
     * Builds an OrderNotificationEvent from the saved order and publishes
     * it to RabbitMQ via EventPublisher.
     *
     * WHY EventPublisher and not RabbitMQEventPublisher directly?
     * ─────────────────────────────────────────────────────────────
     * Dependency Inversion Principle — OrderService depends on the abstraction.
     * Swapping the message broker (RabbitMQ → Kafka) requires zero changes here.
     *
     * Best-effort: any exception here is caught and logged.
     * Notification failure must NEVER roll back or block an order operation.
     */
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
                    .shippingAddress(order.getShippingAddress())
                    .metadata(OrderNotificationEvent.EventMetadata.builder().build())
                    .build();

            eventPublisher.publish(event);

        } catch (Exception e) {
            log.warn("Could not build notification event for order={} type={}: {}",
                    order.getOrderNumber(), type, e.getMessage());
        }
    }
}

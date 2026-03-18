package com.hero.bikestore.service;

import com.hero.bikestore.client.BikeServiceClient;
import com.hero.bikestore.client.InventoryServiceClient;
import com.hero.bikestore.client.response.BikeClientResponse;
import com.hero.bikestore.client.response.InventoryClientResponse;
import com.hero.bikestore.common.exception.base.ForbiddenException;
import com.hero.bikestore.dto.request.OrderItemRequest;
import com.hero.bikestore.dto.request.PlaceOrderRequest;
import com.hero.bikestore.dto.response.OrderResponse;
import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderItem;
import com.hero.bikestore.entity.OrderStatus;
import com.hero.bikestore.exception.BikeNotFoundException;
import com.hero.bikestore.exception.InsufficientStockException;
import com.hero.bikestore.exception.OrderNotFoundException;
import com.hero.bikestore.exception.ServiceUnavailableException;
import com.hero.bikestore.mapper.OrderMapper;
import com.hero.bikestore.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final BikeServiceClient bikeServiceClient;
    private final InventoryServiceClient inventoryServiceClient;

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

            // ── Step 1: Validate bike exists, get name + price ───────────────
            BikeClientResponse bike = fetchBike(itemRequest.getBikeId());

            // ── Step 2: Validate stock is sufficient ─────────────────────────
            InventoryClientResponse inventory = fetchInventory(itemRequest.getBikeId());

            if (inventory.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        itemRequest.getBikeId(),
                        inventory.getStockQuantity(),
                        itemRequest.getQuantity()
                );
            }

            // ── Step 3: Reduce stock atomically in inventory-service ──────────
            inventoryServiceClient.reduceStock(itemRequest.getBikeId(), itemRequest.getQuantity());

            // ── Step 4: Build order item — price is a snapshot, not a reference
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

        return orderMapper.toResponse(saved);
    }

    /**
     * Circuit breaker fallback — called whenever placeOrder throws.
     * Signature must match placeOrder + Throwable at the end.
     *
     * IMPORTANT: @CircuitBreaker's fallbackMethod is invoked for ALL exceptions,
     * including business exceptions like BikeNotFoundException (404) and
     * InsufficientStockException (400). We re-throw those so they reach the
     * client with the correct HTTP status instead of being swallowed into a 503.
     *
     * Only connectivity exceptions (RestClientException, timeout, etc.) should
     * result in 503 SERVICE_UNAVAILABLE.
     */
    public OrderResponse placeOrderFallback(PlaceOrderRequest request, Jwt jwt, Throwable t) {
        // Re-throw business exceptions — these are valid client errors (4xx), not circuit breaker events
        if (t instanceof BikeNotFoundException) {
            throw (BikeNotFoundException) t;
        }
        if (t instanceof InsufficientStockException) {
            throw (InsufficientStockException) t;
        }
        log.error("Circuit breaker triggered during placeOrder: {}", t.getMessage());
        throw new ServiceUnavailableException("Order placement service");
    }

    /**
     * Fetches a single order by ID.
     * Ownership check: customer can only view their own orders.
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

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers — wrap RestClient calls with proper exception translation
    //
    // RestClient exception hierarchy:
    //   RestClientException (base)
    //   ├── HttpClientErrorException   → 4xx from the downstream service
    //   │   ├── NotFound               → 404 specifically
    //   │   ├── BadRequest             → 400
    //   │   └── ...
    //   ├── HttpServerErrorException   → 5xx from the downstream service
    //   └── ResourceAccessException    → I/O failure (connection refused, timeout)
    // ─────────────────────────────────────────────────────────────────────────

    private BikeClientResponse fetchBike(Long bikeId) {
        try {
            return bikeServiceClient.getBikeById(bikeId);
        } catch (HttpClientErrorException.NotFound e) {
            // bike-service returned 404 — the bike does not exist
            throw new BikeNotFoundException(bikeId);
        } catch (RestClientException e) {
            // Connection failure, timeout, or unexpected server error
            log.error("bike-service call failed for bikeId={}: {}", bikeId, e.getMessage());
            throw new ServiceUnavailableException("bike-service");
        }
    }

    private InventoryClientResponse fetchInventory(Long bikeId) {
        try {
            return inventoryServiceClient.getInventoryByBikeId(bikeId);
        } catch (HttpClientErrorException.NotFound e) {
            // No inventory record = bike not available for purchase
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
}

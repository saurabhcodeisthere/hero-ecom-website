package com.hero.bikestore.service;

import com.hero.bikestore.client.BikeServiceClient;
import com.hero.bikestore.client.InventoryServiceClient;
import com.hero.bikestore.client.OrderServiceClient;
import com.hero.bikestore.client.UserServiceClient;
import com.hero.bikestore.client.request.CartOrderItemRequest;
import com.hero.bikestore.client.request.CartPlaceOrderRequest;
import com.hero.bikestore.client.response.BikeClientResponse;
import com.hero.bikestore.client.response.InventoryClientResponse;
import com.hero.bikestore.client.response.OrderClientResponse;
import com.hero.bikestore.client.response.UserAddressClientResponse;
import com.hero.bikestore.common.exception.base.BadRequestException;
import com.hero.bikestore.dto.request.AddToCartRequest;
import com.hero.bikestore.dto.request.CheckoutRequest;
import com.hero.bikestore.dto.request.DeliveryAddressDto;
import com.hero.bikestore.dto.request.UpdateCartItemRequest;
import com.hero.bikestore.dto.response.AdminCartSummaryResponse;
import com.hero.bikestore.dto.response.CartItemResponse;
import com.hero.bikestore.dto.response.CartResponse;
import com.hero.bikestore.dto.response.CheckoutResponse;
import com.hero.bikestore.entity.CartItem;
import com.hero.bikestore.exception.BikeNotAvailableException;
import com.hero.bikestore.exception.CartItemNotFoundException;
import com.hero.bikestore.repository.CartItemRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartItemRepository     cartItemRepository;
    private final BikeServiceClient      bikeServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderServiceClient     orderServiceClient;
    private final UserServiceClient      userServiceClient;

    // Self-reference via Spring proxy — needed so @Retry and @CircuitBreaker on
    // doCheckout() are honoured. Spring AOP only intercepts calls that go through
    // the proxy; calling this.doCheckout() directly would bypass it entirely.
    // @Lazy breaks the circular dependency: Spring creates CartServiceImpl first,
    // then injects the proxy reference once the bean is fully initialised.
    @Autowired
    @Lazy
    private CartServiceImpl self;

    // ─────────────────────────────────────────────────────────────────────────
    // ADD TO CART
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a bike to the cart or increments its quantity if it is already there.
     *
     * Three validations before touching the DB:
     *   1. Bike must exist in bike-service (404 = unknown bike → reject)
     *   2. Bike must be active (inactive = discontinued → reject)
     *   3. Inventory must have stock (stockQuantity == 0 → reject)
     *
     * The unique constraint uk_cart_user_bike ensures one row per (user, bike).
     * We use findByUserIdAndBikeId to upsert rather than always inserting.
     */
    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request, Jwt jwt) {
        String userId = extractUserId(jwt);

        // ── Step 1: Validate bike exists and is active ──────────────────────
        BikeClientResponse bike;
        try {
            bike = bikeServiceClient.getBikeById(request.getBikeId());
        } catch (HttpClientErrorException.NotFound e) {
            throw new BikeNotAvailableException(
                    "Bike not found: " + request.getBikeId());
        }

        if (!bike.isActive()) {
            throw new BikeNotAvailableException(
                    "'" + bike.getModelName() + "' is no longer available for purchase.");
        }

        // ── Step 2: Soft stock check ─────────────────────────────────────────
        try {
            InventoryClientResponse inventory =
                    inventoryServiceClient.getInventoryByBikeId(request.getBikeId());

            if (!inventory.isActive() || inventory.getStockQuantity() == null
                    || inventory.getStockQuantity() < request.getQuantity()) {
                throw new BikeNotAvailableException(
                        "'" + bike.getModelName() + "' does not have enough stock. "
                        + "Available: " + (inventory.getStockQuantity() == null ? 0 : inventory.getStockQuantity()));
            }
        } catch (HttpClientErrorException.NotFound e) {
            // No inventory record means the bike has never been stocked
            throw new BikeNotAvailableException(
                    "'" + bike.getModelName() + "' is currently out of stock.");
        }

        // ── Step 3: Upsert cart item ─────────────────────────────────────────
        CartItem cartItem = cartItemRepository
                .findByUserIdAndBikeId(userId, request.getBikeId())
                .map(existing -> {
                    // Already in cart — increment quantity
                    existing.setQuantity(existing.getQuantity() + request.getQuantity());
                    log.info("Updated cart item quantity: userId={} bikeId={} newQty={}",
                            userId, request.getBikeId(), existing.getQuantity());
                    return existing;
                })
                .orElseGet(() -> {
                    // First time adding this bike — create a new row with snapshotted values
                    log.info("Adding new cart item: userId={} bikeId={} qty={}",
                            userId, request.getBikeId(), request.getQuantity());
                    return CartItem.builder()
                            .userId(userId)
                            .bikeId(request.getBikeId())
                            .bikeName(bike.getModelName())      // snapshot
                            .unitPrice(bike.getPrice())          // snapshot
                            .quantity(request.getQuantity())
                            .build();
                });

        cartItemRepository.save(cartItem);

        return buildCartResponse(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET CART
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public CartResponse getCart(Jwt jwt) {
        return buildCartResponse(extractUserId(jwt));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE CART ITEM
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request, Jwt jwt) {
        String userId = extractUserId(jwt);

        CartItem item = findOwnedCartItem(cartItemId, userId);
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        log.info("Cart item updated: cartItemId={} userId={} newQty={}",
                cartItemId, userId, request.getQuantity());

        return buildCartResponse(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REMOVE SINGLE ITEM
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse removeCartItem(Long cartItemId, Jwt jwt) {
        String userId = extractUserId(jwt);

        CartItem item = findOwnedCartItem(cartItemId, userId);
        cartItemRepository.delete(item);

        log.info("Cart item removed: cartItemId={} userId={}", cartItemId, userId);

        return buildCartResponse(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLEAR ENTIRE CART
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void clearCart(Jwt jwt) {
        String userId = extractUserId(jwt);
        cartItemRepository.deleteAllByUserId(userId);
        log.info("Cart cleared for userId={}", userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHECKOUT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts the cart into an order — public entry point.
     *
     * WHY two methods?
     * ─────────────────────────────────────────────────────────────────────
     * @Retry re-runs the annotated method from the TOP each time it retries.
     * If UUID.randomUUID() lived inside the retried method, every attempt
     * would generate a DIFFERENT idempotency key — and order-service would
     * create a new order on every retry, making duplicates even more likely.
     *
     * FIX: Generate the UUID ONCE here (never retried), then pass it into
     * doCheckout() which IS retried. All retries carry the SAME key.
     * order-service sees the same key on retry → returns existing order → no duplicate.
     */
    @Override
    public CheckoutResponse checkout(CheckoutRequest request, Jwt jwt) {
        String userId = extractUserId(jwt);

        // ── Guard: empty cart — fast fail before any HTTP calls ──────────────
        List<CartItem> items = cartItemRepository.findByUserIdOrderByAddedAtAsc(userId);
        if (items.isEmpty()) {
            throw new BadRequestException(
                    "Your cart is empty. Add at least one bike before checking out.");
        }

        // ── Resolve delivery address ─────────────────────────────────────────
        // Mode 1: customer provided a saved addressId → fetch from user-service
        // Mode 2: customer provided inline address → use directly
        DeliveryAddressDto resolvedAddress;
        if (request.getAddressId() != null) {
            log.info("Resolving saved address: userId={} addressId={}", userId, request.getAddressId());
            try {
                String bearerToken = "Bearer " + jwt.getTokenValue();
                UserAddressClientResponse addressResponse =
                        userServiceClient.getAddressById(request.getAddressId(), userId, bearerToken);
                resolvedAddress = DeliveryAddressDto.from(addressResponse.getData());
            } catch (HttpClientErrorException.NotFound e) {
                throw new BadRequestException(
                        "Address not found or does not belong to your account: " + request.getAddressId());
            }
        } else if (request.getShippingAddress() != null) {
            resolvedAddress = request.getShippingAddress();
        } else {
            throw new BadRequestException(
                    "Please provide either a saved addressId or a shippingAddress.");
        }

        // ── Generate idempotency key ONCE — survives all retries ─────────────
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("Checkout started: userId={} idempotencyKey={} itemCount={}",
                userId, idempotencyKey, items.size());

        // ── Delegate to retried+circuit-broken inner method ──────────────────
        return self.doCheckout(resolvedAddress, jwt, idempotencyKey, items);
    }

    /**
     * Inner checkout execution — wrapped by @Retry and @CircuitBreaker.
     *
     * Receives the idempotency key generated by checkout() so the SAME key
     * is sent to order-service on every retry attempt.
     *
     * CRITICAL ORDER:
     *   1. Build CartPlaceOrderRequest (cart items already loaded by caller)
     *   2. Call order-service with JWT + idempotency key
     *      → order-service: if key seen before → return existing order (no duplicate)
     *      → order-service: if key new → create order, store key, return new order
     *   3. Clear cart ONLY after a successful response
     */
    @Transactional
    @CircuitBreaker(name = "cart-checkout", fallbackMethod = "doCheckoutFallback")
    @Retry(name = "cart-checkout")
    public CheckoutResponse doCheckout(DeliveryAddressDto resolvedAddress, Jwt jwt,
                                       String idempotencyKey, List<CartItem> items) {
        String userId = extractUserId(jwt);

        // ── Build order items from the pre-loaded cart ───────────────────────
        List<CartOrderItemRequest> orderItems = items.stream()
                .map(item -> new CartOrderItemRequest(item.getBikeId(), item.getQuantity()))
                .toList();

        CartPlaceOrderRequest orderRequest = new CartPlaceOrderRequest(
                resolvedAddress,
                orderItems
        );

        // ── Forward to order-service with JWT + idempotency key ──────────────
        String bearerToken = "Bearer " + jwt.getTokenValue();
        log.info("Forwarding to order-service: userId={} key={}", userId, idempotencyKey);

        OrderClientResponse orderResponse = orderServiceClient.placeOrder(
                orderRequest, bearerToken, idempotencyKey);

        // ── Clear cart ONLY after confirmed order creation ───────────────────
        cartItemRepository.deleteAllByUserId(userId);

        log.info("Checkout successful: userId={} orderId={} orderNumber={}",
                userId, orderResponse.getOrderId(), orderResponse.getOrderNumber());

        return CheckoutResponse.builder()
                .orderId(orderResponse.getOrderId())
                .orderNumber(orderResponse.getOrderNumber())
                .status(orderResponse.getStatus())
                .totalAmount(orderResponse.getTotalAmount())
                .paymentUrl(orderResponse.getPaymentUrl())
                .message("Order placed successfully! "
                        + (orderResponse.getPaymentUrl() != null
                        ? "Please complete your payment using the provided link."
                        : "Your order is being processed."))
                .build();
    }

    /**
     * Fallback — called when order-service is unreachable or circuit is open.
     * Cart is NOT cleared — the customer can safely retry later.
     */
    public CheckoutResponse doCheckoutFallback(DeliveryAddressDto resolvedAddress, Jwt jwt,
                                               String idempotencyKey, List<CartItem> items,
                                               Throwable t) {
        log.error("Checkout fallback: userId={} key={} reason={}",
                extractUserId(jwt), idempotencyKey, t.getMessage());
        throw new BadRequestException(
                "Order service is temporarily unavailable. "
                + "Your cart has been saved — please try again in a moment.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN OPERATIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a one-line summary per customer who has items in their cart.
     * Ordered by most recently updated first (freshest carts at the top).
     */
    @Override
    public List<AdminCartSummaryResponse> getAllCartSummaries() {
        return cartItemRepository.findAllCartSummaries().stream()
                .map(row -> AdminCartSummaryResponse.builder()
                        .userId((String) row[0])
                        .itemCount(((Long) row[1]).intValue())
                        .cartTotal((BigDecimal) row[2])
                        .lastUpdatedAt((LocalDateTime) row[3])
                        .build())
                .toList();
    }

    /**
     * Returns the full cart for any customer by their Keycloak userId.
     * Used by support when a customer reports a problem with their cart.
     */
    @Override
    public CartResponse getCartByUserId(String userId) {
        return buildCartResponse(userId);
    }

    /**
     * Clears a specific customer's cart — admin support action.
     * Logged with the userId so the action is auditable.
     */
    @Override
    @Transactional
    public void clearCartByUserId(String userId) {
        cartItemRepository.deleteAllByUserId(userId);
        log.info("[ADMIN] Cart cleared for userId={}", userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts the userId from the Keycloak JWT subject claim.
     * This is the Keycloak-assigned UUID for the user — stable and unique.
     */
    private String extractUserId(Jwt jwt) {
        return jwt.getSubject();
    }

    /**
     * Finds a CartItem by its ID and verifies it belongs to the requesting user.
     *
     * Returns 404 (not 403) even when the item belongs to a different user —
     * this prevents information leakage about other customers' carts.
     */
    private CartItem findOwnedCartItem(Long cartItemId, String userId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId));

        // Ownership check — treat mismatch as 404 to avoid leaking other customers' cart IDs
        if (!item.getUserId().equals(userId)) {
            throw new CartItemNotFoundException(cartItemId);
        }

        return item;
    }

    /**
     * Builds a CartResponse from the current DB state for a given user.
     *
     * Called after every mutation so the controller always returns
     * the full updated cart — no extra GET needed from the frontend.
     */
    private CartResponse buildCartResponse(String userId) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByAddedAtAsc(userId);

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    return CartItemResponse.builder()
                            .cartItemId(item.getId())
                            .bikeId(item.getBikeId())
                            .bikeName(item.getBikeName())
                            .unitPrice(item.getUnitPrice())
                            .quantity(item.getQuantity())
                            .lineTotal(lineTotal)
                            .addedAt(item.getAddedAt())
                            .build();
                })
                .toList();

        BigDecimal cartTotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .itemCount(itemResponses.size())
                .cartTotal(cartTotal)
                .items(itemResponses)
                .build();
    }
}

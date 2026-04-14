package com.hero.bikestore.client;

import com.hero.bikestore.client.request.CartPlaceOrderRequest;
import com.hero.bikestore.client.response.OrderClientResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Spring HTTP Interface client for order-service.
 *
 * Called once at checkout — cart-service assembles all cart items
 * + delivery address into CartPlaceOrderRequest and forwards it here.
 *
 * WHY does this method accept an Authorization header?
 * ──────────────────────────────────────────────────────────────
 * order-service is a JWT-secured resource server. It reads the
 * customer's userId from the JWT subject claim (@AuthenticationPrincipal Jwt).
 * If cart-service did not forward the customer's token, order-service
 * would see no principal and return 401 Unauthorized.
 *
 * cart-service extracts the raw JWT value from Spring Security's
 * SecurityContext and forwards it as "Bearer <token>".
 * This is safe — the JWT is already cryptographically signed by Keycloak;
 * order-service verifies the signature independently via its JWK endpoint.
 * cart-service cannot forge or modify the token.
 *
 * Circuit breaker "cart-checkout" wraps this call in CartServiceImpl.
 * If order-service is unreachable, the cart is NOT cleared — the customer
 * can retry without losing their cart items.
 */
@HttpExchange
public interface OrderServiceClient {

    @PostExchange("/api/v1/orders")
    OrderClientResponse placeOrder(
            @RequestBody CartPlaceOrderRequest request,
            @RequestHeader("Authorization") String bearerToken,
            @RequestHeader("Idempotency-Key") String idempotencyKey);
}

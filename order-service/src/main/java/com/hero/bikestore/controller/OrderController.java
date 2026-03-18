package com.hero.bikestore.controller;

import com.hero.bikestore.dto.request.PlaceOrderRequest;
import com.hero.bikestore.dto.response.OrderResponse;
import com.hero.bikestore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/v1/orders
     *
     * Places a new order for the authenticated user.
     * userId is extracted from the JWT — never passed in the request body.
     *
     * Returns 201 Created with the full order details.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.placeOrder(request, jwt));
    }

    /**
     * GET /api/v1/orders/{id}
     *
     * Retrieves a single order by its database ID.
     * A customer can only fetch their own orders — 403 if the order belongs to someone else.
     *
     * Returns 200 OK with full order details including all items.
     * Returns 404 if the order does not exist.
     * Returns 403 if the order belongs to a different user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(orderService.getOrderById(id, jwt));
    }
}

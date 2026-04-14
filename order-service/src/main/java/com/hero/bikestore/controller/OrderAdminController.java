package com.hero.bikestore.controller;

import com.hero.bikestore.dto.response.AdminOrderResponse;
import com.hero.bikestore.dto.response.PagedOrderResponse;
import com.hero.bikestore.entity.OrderStatus;
import com.hero.bikestore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    /**
     * GET /api/v1/admin/orders
     *
     * Returns all orders with pagination. All filter params are optional —
     * omit any to get all orders regardless of that field.
     *
     * Query params:
     *   page    — zero-based page number (default 0)
     *   size    — page size (default 20)
     *   status  — filter by order status (e.g. CONFIRMED, SHIPPED)
     *   city    — filter by delivery city   (case-insensitive exact match)
     *   state   — filter by delivery state  (case-insensitive exact match)
     *   pincode — filter by delivery pincode
     *
     * Examples:
     *   GET /api/v1/admin/orders
     *   GET /api/v1/admin/orders?status=CONFIRMED
     *   GET /api/v1/admin/orders?city=Mumbai&state=Maharashtra
     *   GET /api/v1/admin/orders?status=SHIPPED&city=Delhi&page=1&size=10
     *
     * Returns 200 OK with paginated list including userId and userEmail.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedOrderResponse> getAllOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String pincode) {

        return ResponseEntity.ok(orderService.getAllOrders(page, size, status, city, state, pincode));
    }

    /**
     * GET /api/v1/admin/orders/{id}
     *
     * Returns any single order by ID — no ownership check.
     * Admin can view any customer's order.
     *
     * Returns 200 OK with full order details including userId, userEmail, updatedAt.
     * Returns 404 if order does not exist.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminOrderResponse> getOrderById(@PathVariable Long id) {

        return ResponseEntity.ok(orderService.getAdminOrderById(id));
    }

    /**
     * PATCH /api/v1/admin/orders/{id}/confirm
     *
     * Transitions order from PENDING → CONFIRMED.
     * Admin confirms order after verifying payment and stock availability.
     *
     * Returns 200 OK with updated order.
     * Returns 400 if order is not in PENDING status.
     * Returns 404 if order does not exist.
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminOrderResponse> confirmOrder(@PathVariable Long id) {

        return ResponseEntity.ok(orderService.confirmOrder(id));
    }

    /**
     * PATCH /api/v1/admin/orders/{id}/ship
     *
     * Transitions order from CONFIRMED → SHIPPED.
     * Admin marks order as dispatched from warehouse.
     *
     * Returns 200 OK with updated order.
     * Returns 400 if order is not in CONFIRMED status.
     * Returns 404 if order does not exist.
     */
    @PatchMapping("/{id}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminOrderResponse> shipOrder(@PathVariable Long id) {

        return ResponseEntity.ok(orderService.shipOrder(id));
    }

    /**
     * PATCH /api/v1/admin/orders/{id}/deliver
     *
     * Transitions order from SHIPPED → DELIVERED.
     * Admin marks order as successfully delivered to the customer.
     *
     * Returns 200 OK with updated order.
     * Returns 400 if order is not in SHIPPED status.
     * Returns 404 if order does not exist.
     */
    @PatchMapping("/{id}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminOrderResponse> deliverOrder(@PathVariable Long id) {

        return ResponseEntity.ok(orderService.deliverOrder(id));
    }

    /**
     * DELETE /api/v1/admin/orders/{id}
     *
     * Cancels an order in PENDING or CONFIRMED status.
     * Stock is automatically restored in inventory-service.
     *
     * SHIPPED and DELIVERED orders cannot be cancelled here —
     * they require a separate return/refund flow.
     *
     * Returns 204 No Content on success.
     * Returns 400 if order is in SHIPPED, DELIVERED, or already CANCELLED status.
     * Returns 404 if order does not exist.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {

        orderService.adminCancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}

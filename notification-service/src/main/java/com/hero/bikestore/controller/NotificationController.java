package com.hero.bikestore.controller;

import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [DISABLED — KEPT FOR REFERENCE]
 *
 * Original HTTP REST endpoint for receiving order notification events.
 * Replaced by RabbitMQ listener (NotificationListener).
 *
 * WHY KEPT:
 *   - Documents the previous HTTP-based approach for learning and comparison
 *   - Can be re-enabled by restoring @RestController if RabbitMQ is removed
 *
 * WHY DISABLED:
 *   - order-service called this directly over HTTP — tight coupling
 *   - If this service was down, the HTTP call failed and email was lost
 *   - Replaced by @RabbitListener which consumes from a durable queue
 *
 * TO RE-ENABLE: restore @RestController below
 */
// @RestController  ← DISABLED: replaced by NotificationListener
@Deprecated
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Internal endpoint for order lifecycle notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Receives an order lifecycle event and triggers the appropriate email.
     *
     * Called asynchronously from order-service via NotificationAsyncSender.
     * Returns 200 immediately — email sending happens synchronously inside
     * notification-service but the calling thread in order-service is already free.
     */
    @PostMapping("/order-event")
    @Operation(summary = "Handle order event", description = "Receives an order lifecycle event and sends the appropriate email notification")
    public ResponseEntity<Void> handleOrderEvent(@Valid @RequestBody OrderNotificationEvent event) {
        notificationService.handleOrderEvent(event);
        return ResponseEntity.ok().build();
    }
}

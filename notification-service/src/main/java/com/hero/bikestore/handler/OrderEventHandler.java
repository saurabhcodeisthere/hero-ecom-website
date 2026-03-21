package com.hero.bikestore.handler;

import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.enums.OrderEventType;

/**
 * Strategy contract — every order event handler must implement this.
 *
 * SOLID principles applied:
 * ──────────────────────────
 * S — Each implementation has one responsibility: handle one event type.
 * O — Adding a new event type = new class only. NotificationService never changes.
 * L — All handlers are substitutable through this interface.
 * I — Thin interface with only two focused methods.
 * D — NotificationService depends on this abstraction, not concrete handlers.
 *
 * getSupportedType() is the "badge" each handler wears.
 * NotificationService reads all badges at startup to build the routing Map.
 */
public interface OrderEventHandler {

    /**
     * Declares which event type this handler is responsible for.
     * Used by NotificationService to build the Map at startup.
     */
    OrderEventType getSupportedType();

    /**
     * Processes the event — builds the email and sends it.
     */
    void handle(OrderNotificationEvent event);
}

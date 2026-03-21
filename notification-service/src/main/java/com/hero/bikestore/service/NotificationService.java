package com.hero.bikestore.service;

import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.exception.UnknownEventTypeException;
import com.hero.bikestore.handler.OrderEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Central router — receives an OrderNotificationEvent and delegates
 * to the correct handler based on event type.
 *
 * HOW THE MAP IS BUILT:
 * ──────────────────────
 * Spring collects ALL @Component beans implementing OrderEventHandler into a List.
 * The constructor streams that list and builds a Map:
 *   key   = handler.getSupportedType()  (e.g. ORDER_PLACED)
 *   value = the handler instance itself
 *
 * This Map is built ONCE at application startup and lives in memory forever.
 * Every incoming request does a single O(1) Map lookup — no loops, no switches.
 *
 * ADDING A NEW EVENT TYPE:
 * ─────────────────────────
 * 1. Add value to OrderEventType enum
 * 2. Create a new @Component implementing OrderEventHandler
 * 3. This class never changes — OCP satisfied.
 */
@Service
@Slf4j
public class NotificationService {

    // Map built at startup: ORDER_PLACED → OrderPlacedHandler, etc.
    private final Map<String, OrderEventHandler> handlers;

    /**
     * Spring automatically injects ALL beans implementing OrderEventHandler.
     * Constructor converts the List into a lookup Map keyed by event type name.
     */
    public NotificationService(List<OrderEventHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        handler -> handler.getSupportedType().name(),  // key: "ORDER_PLACED"
                        Function.identity()                             // value: the handler itself
                ));

        log.info("NotificationService initialized with {} handlers: {}",
                handlers.size(), handlers.keySet());
    }

    /**
     * Routes the event to the correct handler.
     * Throws UnknownEventTypeException (400) if no handler is registered.
     */
    public void handleOrderEvent(OrderNotificationEvent event) {
        log.info("Received event type={} orderId={} orderNumber={}",
                event.getType(), event.getOrderId(), event.getOrderNumber());

        OrderEventHandler handler = handlers.get(event.getType().name());

        if (handler == null) {
            throw new UnknownEventTypeException(event.getType().name());
        }

        handler.handle(event);
    }
}

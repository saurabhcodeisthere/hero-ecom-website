package com.hero.bikestore.handler;

import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.enums.OrderEventType;
import com.hero.bikestore.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles ORDER_PLACED events.
 * Sends a "Your order has been placed" confirmation email to the customer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedHandler implements OrderEventHandler {

    private final EmailService emailService;

    @Override
    public OrderEventType getSupportedType() {
        return OrderEventType.ORDER_PLACED;
    }

    @Override
    public void handle(OrderNotificationEvent event) {
        log.info("Sending ORDER_PLACED email to {} for order {}", event.getUserEmail(), event.getOrderNumber());

        emailService.sendHtmlEmail(
                event.getUserEmail(),
                "Order Confirmed — " + event.getOrderNumber(),
                "email/order-placed",
                event
        );
    }
}

package com.hero.bikestore.handler;

import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.enums.OrderEventType;
import com.hero.bikestore.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles ORDER_CONFIRMED events.
 * Sends a "We are preparing your order" email to the customer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderConfirmedHandler implements OrderEventHandler {

    private final EmailService emailService;

    @Override
    public OrderEventType getSupportedType() {
        return OrderEventType.ORDER_CONFIRMED;
    }

    @Override
    public void handle(OrderNotificationEvent event) {
        log.info("Sending ORDER_CONFIRMED email to {} for order {}", event.getUserEmail(), event.getOrderNumber());

        emailService.sendHtmlEmail(
                event.getUserEmail(),
                "We Are Preparing Your Order — " + event.getOrderNumber(),
                "email/order-confirmed",
                event
        );
    }
}

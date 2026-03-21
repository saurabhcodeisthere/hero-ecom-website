package com.hero.bikestore.handler;

import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.enums.OrderEventType;
import com.hero.bikestore.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles ORDER_SHIPPED events.
 * Sends a "Your bike is on the way" email with tracking information.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderShippedHandler implements OrderEventHandler {

    private final EmailService emailService;

    @Override
    public OrderEventType getSupportedType() {
        return OrderEventType.ORDER_SHIPPED;
    }

    @Override
    public void handle(OrderNotificationEvent event) {
        log.info("Sending ORDER_SHIPPED email to {} for order {}", event.getUserEmail(), event.getOrderNumber());

        emailService.sendHtmlEmail(
                event.getUserEmail(),
                "Your Bike Is On The Way! — " + event.getOrderNumber(),
                "email/order-shipped",
                event
        );
    }
}

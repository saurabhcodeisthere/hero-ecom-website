package com.hero.bikestore.handler;

import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.enums.OrderEventType;
import com.hero.bikestore.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles ORDER_CANCELLED events.
 * Sends an "Order has been cancelled" email to the customer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledHandler implements OrderEventHandler {

    private final EmailService emailService;

    @Override
    public OrderEventType getSupportedType() {
        return OrderEventType.ORDER_CANCELLED;
    }

    @Override
    public void handle(OrderNotificationEvent event) {
        log.info("Sending ORDER_CANCELLED email to {} for order {}", event.getUserEmail(), event.getOrderNumber());

        emailService.sendHtmlEmail(
                event.getUserEmail(),
                "Your Order Has Been Cancelled — " + event.getOrderNumber(),
                "email/order-cancelled",
                event
        );
    }
}

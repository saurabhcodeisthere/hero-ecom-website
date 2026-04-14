package com.hero.bikestore.handler;

import com.hero.bikestore.dto.OrderNotificationEvent;
import com.hero.bikestore.enums.OrderEventType;
import com.hero.bikestore.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles ORDER_PAYMENT_EXPIRY_WARNING events.
 *
 * Sends a "Your payment link is about to expire" email to the customer.
 *
 * The email template (email/payment-expiry-warning) can use:
 *   event.orderNumber    — to display the order reference
 *   event.totalAmount    — to show the order value
 *   event.items          — to list the bikes they ordered
 *   event.metadata.paymentUrl — to render a "Complete your payment" button
 *
 * Template file to create:
 *   notification-service/src/main/resources/templates/email/payment-expiry-warning.html
 *
 * HOW THIS GETS WIRED:
 * ─────────────────────
 * NotificationService collects ALL @Component beans implementing OrderEventHandler at startup.
 * It calls getSupportedType() on each and builds a Map:
 *   ORDER_PAYMENT_EXPIRY_WARNING → this handler
 *
 * When an ORDER_PAYMENT_EXPIRY_WARNING message arrives on notification.queue,
 * NotificationListener → NotificationService → this handler.handle(event).
 * Zero changes to NotificationService or NotificationListener. OCP satisfied.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryWarningHandler implements OrderEventHandler {

    private final EmailService emailService;

    @Override
    public OrderEventType getSupportedType() {
        return OrderEventType.ORDER_PAYMENT_EXPIRY_WARNING;
    }

    @Override
    public void handle(OrderNotificationEvent event) {
        log.info("[PaymentExpiryWarningHandler] Sending expiry warning email | userEmail={} orderNumber={}",
                event.getUserEmail(), event.getOrderNumber());

        emailService.sendHtmlEmail(
                event.getUserEmail(),
                "Complete Your Payment — " + event.getOrderNumber(),
                "email/payment-expiry-warning",
                event
        );

        log.info("[PaymentExpiryWarningHandler] Expiry warning email sent | orderNumber={}",
                event.getOrderNumber());
    }
}

package com.hero.bikestore.processor;

import com.hero.bikestore.dto.PaymentInitiationResult;
import com.hero.bikestore.dto.ProcessPaymentCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock implementation of PaymentProcessor — active only in dev profile.
 *
 * WHY MOCK INSTEAD OF REAL RAZORPAY:
 *   - No Razorpay account needed during development
 *   - No internet required — fully self-contained
 *   - Localhost not reachable by Razorpay servers (webhooks can't be received)
 *   - Lets us test both SUCCESS and FAILED paths on demand
 *
 * HOW IT WORKS:
 *   1. Generates a random UUID as the paymentId
 *   2. Returns a URL pointing to MockCheckoutController on this service
 *   3. Developer opens the URL → sees two buttons: "Pay Now" and "Decline"
 *   4. Clicking a button simulates the webhook — triggers the full saga
 *
 * SWITCHING TO REAL RAZORPAY:
 *   Add RazorpayPaymentProcessor with @Profile("prod")
 *   Change payment.mode=prod in config-server/payment-service-prod.yaml
 *   Zero changes to PaymentService, PaymentCommandListener, or any other class.
 */
@Component
@ConditionalOnProperty(name = "payment.mode", havingValue = "dev", matchIfMissing = true)
@Slf4j
public class MockPaymentProcessor implements PaymentProcessor {

    // Base URL of this service — used to build the mock checkout URL
    private static final String BASE_URL = "http://localhost:8086";

    @Override
    public PaymentInitiationResult initiate(ProcessPaymentCommand command) {
        log.info("[MockPaymentProcessor] initiate | ENTER — orderId={} orderNumber={} amount={}",
                command.getOrderId(), command.getOrderNumber(), command.getAmount());

        String paymentId = UUID.randomUUID().toString();
        String paymentUrl = BASE_URL + "/mock/checkout/" + paymentId;

        log.info("[MockPaymentProcessor] Generated gatewayPaymentId={}", paymentId);
        log.info("[MockPaymentProcessor] ┌─────────────────────────────────────────────────────────┐");
        log.info("[MockPaymentProcessor] │  OPEN THIS URL IN BROWSER TO SIMULATE PAYMENT           │");
        log.info("[MockPaymentProcessor] │  {}  │", paymentUrl);
        log.info("[MockPaymentProcessor] └─────────────────────────────────────────────────────────┘");

        log.info("[MockPaymentProcessor] initiate | EXIT — gatewayPaymentId={} paymentUrl={}",
                paymentId, paymentUrl);

        return PaymentInitiationResult.builder()
                .paymentId(paymentId)
                .paymentUrl(paymentUrl)
                .build();
    }
}

package com.hero.bikestore.client;

import com.hero.bikestore.dto.payment.PaymentInitiationResult;
import com.hero.bikestore.dto.payment.ProcessPaymentCommand;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Spring HTTP Interface client for payment-service.
 *
 * SRP: Only declares the HTTP contract for payment-service.
 *      Spring generates the proxy implementation at startup.
 *
 * DIP: Callers depend on this interface, not any concrete HTTP class.
 *
 * ISP: Each caller only uses what it needs:
 *   - OrderService uses initiatePayment()
 *   - TimeoutServiceImpl uses expirePayment()
 *   Neither is forced to know about the other's method.
 *
 * Why HTTP and not RabbitMQ for initiation?
 * ──────────────────────────────────────────
 * order-service needs the paymentUrl synchronously to return it to the frontend.
 * RabbitMQ is fire-and-forget — no return value possible.
 */
@HttpExchange
public interface PaymentServiceClient {

    /**
     * Initiates a payment — returns checkout URL for the frontend to redirect customer.
     */
    @PostExchange("/payments/initiate")
    PaymentInitiationResult initiatePayment(@RequestBody ProcessPaymentCommand command);

    /**
     * Marks a payment as EXPIRED.
     * Called by TimeoutServiceImpl when an AWAITING_PAYMENT order passes the cancel threshold.
     */
    @PatchExchange("/payments/{orderId}/expire")
    void expirePayment(@PathVariable String orderId);
}

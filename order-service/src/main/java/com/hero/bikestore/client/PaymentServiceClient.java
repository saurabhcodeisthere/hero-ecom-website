package com.hero.bikestore.client;

import com.hero.bikestore.dto.payment.PaymentInitiationResult;
import com.hero.bikestore.dto.payment.ProcessPaymentCommand;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Spring HTTP Interface client for payment-service.
 *
 * SRP: Only declares what HTTP calls are available on payment-service.
 *      No implementation here — Spring generates the proxy at startup.
 *
 * DIP: OrderService depends on this interface, not any concrete HTTP class.
 *      Swapping payment-service with a different provider = new implementation,
 *      zero changes in OrderService.
 *
 * Why HTTP and not RabbitMQ for initiation?
 * ──────────────────────────────────────────
 * order-service needs the paymentUrl synchronously to return it to the frontend.
 * RabbitMQ is fire-and-forget — no return value possible.
 * HTTP returns a response immediately in the same request cycle.
 */
@HttpExchange
public interface PaymentServiceClient {

    // Initiates payment — returns checkout URL for the frontend to redirect customer
    @PostExchange("/payments/initiate")
    PaymentInitiationResult initiatePayment(@RequestBody ProcessPaymentCommand command);
}

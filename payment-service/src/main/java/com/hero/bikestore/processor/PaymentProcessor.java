package com.hero.bikestore.processor;

import com.hero.bikestore.dto.PaymentInitiationResult;
import com.hero.bikestore.dto.ProcessPaymentCommand;

/**
 * DIP — abstraction for payment gateway integration.
 *
 * PaymentService depends on this interface, NOT on Razorpay or any specific gateway.
 *
 * Implementations:
 *   MockPaymentProcessor     — payment.mode=dev  (default) — no real gateway, local mock URL
 *   RazorpayPaymentProcessor — payment.mode=prod (future)  — calls real Razorpay SDK
 *
 * OCP benefit:
 *   Swapping gateways = add new implementation class only.
 *   PaymentService, PaymentCommandListener, and all other classes stay untouched.
 */
public interface PaymentProcessor {

    /**
     * Initiates a payment for the given order command.
     * Returns the checkout URL and internal payment ID.
     * Does NOT charge the customer — only creates the payment session.
     * The actual charge happens when the customer completes checkout and
     * the gateway fires a webhook.
     */
    PaymentInitiationResult initiate(ProcessPaymentCommand command);
}

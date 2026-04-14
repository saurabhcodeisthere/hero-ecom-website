package com.hero.bikestore.controller;

import com.hero.bikestore.entity.Payment;
import com.hero.bikestore.enums.PaymentStatus;
import com.hero.bikestore.repository.PaymentRepository;
import com.hero.bikestore.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Mock payment checkout page — active only when payment.mode=dev.
 *
 * WHY THIS EXISTS:
 * In production, the customer is redirected to Razorpay's hosted checkout page.
 * Razorpay is on the internet and payment-service is not publicly accessible in dev.
 * This controller replaces the Razorpay checkout page locally.
 *
 * HOW IT MIRRORS THE REAL FLOW:
 *   Real: Customer redirected → Razorpay page → pays → Razorpay fires webhook
 *   Mock: Customer opens URL  → THIS page    → clicks button → this controller
 *                                                              calls WebhookService
 *
 * The same WebhookService code runs in both cases.
 * Switching to real Razorpay = set payment.mode=prod → this controller disappears.
 *
 * EXPIRY ENFORCEMENT:
 * ────────────────────
 * The checkout page checks payment status on EVERY GET request.
 * If status = EXPIRED → renders an "expired" page with no buttons.
 * This prevents the customer from seeing Pay/Decline buttons after the order is cancelled.
 *
 * Endpoints:
 *   GET  /mock/checkout/{gatewayPaymentId}         → HTML checkout page (or expired page)
 *   POST /mock/checkout/{gatewayPaymentId}/success → simulate successful payment
 *   POST /mock/checkout/{gatewayPaymentId}/failure → simulate failed payment
 */
@RestController
@RequestMapping("/mock/checkout")
@ConditionalOnProperty(name = "payment.mode", havingValue = "dev", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MockCheckoutController {

    private final WebhookService    webhookService;
    private final PaymentRepository paymentRepository;

    /**
     * Renders the mock checkout page.
     * Checks payment status first — returns an expired page if status is not INITIATED.
     */
    @GetMapping(value = "/{gatewayPaymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkoutPage(@PathVariable String gatewayPaymentId) {
        log.info("[MockCheckoutController] checkoutPage | ENTER — gatewayPaymentId={}", gatewayPaymentId);

        // Look up payment status — if not INITIATED, show expired/completed page
        Payment payment = paymentRepository.findByGatewayPaymentId(gatewayPaymentId).orElse(null);

        if (payment == null) {
            log.warn("[MockCheckoutController] checkoutPage — payment not found | gatewayPaymentId={}", gatewayPaymentId);
            return ResponseEntity.ok(buildExpiredHtml(
                    "Payment Not Found",
                    "❓ Unknown Payment",
                    "No payment record found for this link.",
                    "#6c757d"
            ));
        }

        log.info("[MockCheckoutController] checkoutPage — payment found | orderId={} status={}",
                payment.getOrderId(), payment.getStatus());

        // Guard: only show Pay/Decline buttons if payment is still INITIATED
        if (payment.getStatus() == PaymentStatus.EXPIRED) {
            log.info("[MockCheckoutController] checkoutPage — payment EXPIRED, showing expired page | orderId={}",
                    payment.getOrderId());
            return ResponseEntity.ok(buildExpiredHtml(
                    "Payment Link Expired",
                    "⏰ Payment Link Expired",
                    "This payment link has expired. Your order has been automatically cancelled.",
                    "#dc3545"
            ));
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[MockCheckoutController] checkoutPage — payment already SUCCESS | orderId={}", payment.getOrderId());
            return ResponseEntity.ok(buildExpiredHtml(
                    "Payment Already Completed",
                    "✅ Payment Already Completed",
                    "This payment was already processed successfully.",
                    "#28a745"
            ));
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.info("[MockCheckoutController] checkoutPage — payment already FAILED | orderId={}", payment.getOrderId());
            return ResponseEntity.ok(buildExpiredHtml(
                    "Payment Failed",
                    "❌ Payment Was Declined",
                    "This payment was declined. Your order has been cancelled.",
                    "#dc3545"
            ));
        }

        // Payment is INITIATED — render checkout page with Pay/Decline buttons
        log.info("[MockCheckoutController] checkoutPage — rendering checkout page | orderId={}", payment.getOrderId());

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Mock Payment Checkout</title>
                    <style>
                        body { font-family: Arial, sans-serif; display: flex; justify-content: center;
                               align-items: center; height: 100vh; margin: 0; background: #f5f5f5; }
                        .card { background: white; padding: 40px; border-radius: 12px;
                                box-shadow: 0 4px 20px rgba(0,0,0,0.1); text-align: center; width: 380px; }
                        h2 { color: #333; margin-bottom: 8px; }
                        .amount { font-size: 28px; font-weight: bold; color: #2c7be5; margin: 16px 0; }
                        .id { font-size: 11px; color: #aaa; word-break: break-all; margin-bottom: 24px; }
                        button { width: 100%%; padding: 14px; border: none; border-radius: 8px;
                                 font-size: 16px; font-weight: bold; cursor: pointer; margin: 8px 0; }
                        .pay-btn { background: #28a745; color: white; }
                        .pay-btn:hover { background: #218838; }
                        .fail-btn { background: #dc3545; color: white; }
                        .fail-btn:hover { background: #c82333; }
                        .result { margin-top: 20px; padding: 12px; border-radius: 8px;
                                  display: none; font-weight: bold; }
                        .success-msg { background: #d4edda; color: #155724; }
                        .failure-msg { background: #f8d7da; color: #721c24; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>🏍️ Hero Bikestore</h2>
                        <p style="color:#666; margin:0">Mock Payment Gateway</p>
                        <div class="amount">Checkout</div>
                        <div class="id">Payment ID: %s</div>

                        <button class="pay-btn" onclick="processPayment('success')">
                            ✅ Pay Now (Success)
                        </button>
                        <button class="fail-btn" onclick="processPayment('failure')">
                            ❌ Decline Payment (Failure)
                        </button>

                        <div id="result" class="result"></div>
                    </div>
                    <script>
                        function processPayment(type) {
                            fetch('/mock/checkout/%s/' + type, { method: 'POST' })
                                .then(res => {
                                    const el = document.getElementById('result');
                                    el.style.display = 'block';
                                    if (type === 'success') {
                                        el.className = 'result success-msg';
                                        el.innerText = '✅ Payment successful! Check your email for confirmation.';
                                    } else {
                                        el.className = 'result failure-msg';
                                        el.innerText = '❌ Payment declined. Your order has been cancelled.';
                                    }
                                    document.querySelectorAll('button').forEach(b => b.disabled = true);
                                })
                                .catch(() => alert('Error processing payment'));
                        }
                    </script>
                </body>
                </html>
                """.formatted(gatewayPaymentId, gatewayPaymentId);

        return ResponseEntity.ok(html);
    }

    /**
     * Simulates a successful payment.
     * WebhookService guards against EXPIRED/FAILED status — safe to call.
     */
    @PostMapping("/{gatewayPaymentId}/success")
    public ResponseEntity<Void> simulateSuccess(@PathVariable String gatewayPaymentId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[MockCheckoutController] simulateSuccess | ENTER");
        log.info("[MockCheckoutController] ✅ PAY button clicked — gatewayPaymentId={}", gatewayPaymentId);
        webhookService.processSuccess(gatewayPaymentId);
        log.info("[MockCheckoutController] simulateSuccess | EXIT — gatewayPaymentId={}", gatewayPaymentId);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return ResponseEntity.ok().build();
    }

    /**
     * Simulates a failed payment.
     * WebhookService guards against EXPIRED status — safe to call.
     */
    @PostMapping("/{gatewayPaymentId}/failure")
    public ResponseEntity<Void> simulateFailure(@PathVariable String gatewayPaymentId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[MockCheckoutController] simulateFailure | ENTER");
        log.info("[MockCheckoutController] ❌ DECLINE button clicked — gatewayPaymentId={}", gatewayPaymentId);
        webhookService.processFailure(gatewayPaymentId, "Insufficient funds");
        log.info("[MockCheckoutController] simulateFailure | EXIT — gatewayPaymentId={}", gatewayPaymentId);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return ResponseEntity.ok().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a non-interactive "terminal state" HTML page.
     * Shown when the payment is EXPIRED, SUCCESS, FAILED, or not found.
     * No buttons — the customer cannot take any action.
     */
    private String buildExpiredHtml(String title, String heading, String message, String color) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>%s</title>
                    <style>
                        body { font-family: Arial, sans-serif; display: flex; justify-content: center;
                               align-items: center; height: 100vh; margin: 0; background: #f5f5f5; }
                        .card { background: white; padding: 40px; border-radius: 12px;
                                box-shadow: 0 4px 20px rgba(0,0,0,0.1); text-align: center; width: 380px; }
                        h2 { color: %s; margin-bottom: 16px; }
                        p  { color: #555; line-height: 1.6; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>%s</h2>
                        <p>%s</p>
                        <p style="font-size:12px; color:#aaa; margin-top:24px">Hero Bikestore</p>
                    </div>
                </body>
                </html>
                """.formatted(title, color, heading, message);
    }
}

package com.hero.bikestore.controller;

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
 * Endpoints:
 *   GET  /mock/checkout/{gatewayPaymentId}         → HTML checkout page
 *   POST /mock/checkout/{gatewayPaymentId}/success → simulate successful payment
 *   POST /mock/checkout/{gatewayPaymentId}/failure → simulate failed payment
 */
@RestController
@RequestMapping("/mock/checkout")
@ConditionalOnProperty(name = "payment.mode", havingValue = "dev", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MockCheckoutController {

    private final WebhookService webhookService;

    /**
     * Renders the mock checkout page.
     * Developer opens this URL in the browser after placing an order.
     */
    @GetMapping(value = "/{gatewayPaymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkoutPage(@PathVariable String gatewayPaymentId) {
        log.info("Mock checkout page opened — gatewayPaymentId={}", gatewayPaymentId);

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
     * Equivalent to Razorpay firing a "payment.captured" webhook.
     */
    @PostMapping("/{gatewayPaymentId}/success")
    public ResponseEntity<Void> simulateSuccess(@PathVariable String gatewayPaymentId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[MockCheckoutController] simulateSuccess | ENTER");
        log.info("[MockCheckoutController] ✅ PAY button clicked in browser — gatewayPaymentId={}", gatewayPaymentId);
        log.info("[MockCheckoutController] Delegating to WebhookService.processSuccess()");
        webhookService.processSuccess(gatewayPaymentId);
        log.info("[MockCheckoutController] simulateSuccess | EXIT — saga triggered for gatewayPaymentId={}", gatewayPaymentId);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return ResponseEntity.ok().build();
    }

    /**
     * Simulates a failed payment.
     * Equivalent to Razorpay firing a "payment.failed" webhook.
     */
    @PostMapping("/{gatewayPaymentId}/failure")
    public ResponseEntity<Void> simulateFailure(@PathVariable String gatewayPaymentId) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[MockCheckoutController] simulateFailure | ENTER");
        log.info("[MockCheckoutController] ❌ DECLINE button clicked in browser — gatewayPaymentId={}", gatewayPaymentId);
        log.info("[MockCheckoutController] Delegating to WebhookService.processFailure()");
        webhookService.processFailure(gatewayPaymentId, "Insufficient funds");
        log.info("[MockCheckoutController] simulateFailure | EXIT — saga triggered for gatewayPaymentId={}", gatewayPaymentId);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        return ResponseEntity.ok().build();
    }
}

package com.hero.bikestore.job;

import com.hero.bikestore.service.TimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that drives the payment timeout feature.
 *
 * WHY IS THIS A SEPARATE CLASS FROM TimeoutServiceImpl?
 * ───────────────────────────────────────────────────────
 * @Scheduled and @Transactional CANNOT safely live on the same method.
 *
 * Spring wraps @Transactional beans in a proxy. When you call a @Transactional
 * method from WITHIN the same class (this.method()), you bypass the proxy —
 * the transaction is silently ignored.
 *
 * Spring's @Scheduled executor calls run() on THIS object directly.
 * If run() were in TimeoutServiceImpl and called this.sendExpiryWarnings(),
 * it would bypass the proxy and @Transactional would do nothing.
 *
 * SOLUTION:
 *   - PaymentTimeoutJob  → owns @Scheduled. No @Transactional.
 *   - TimeoutServiceImpl → owns @Transactional. No @Scheduled.
 *   - Job injects TimeoutService (INTERFACE). Spring's proxy intercepts the
 *     call at the interface boundary → @Transactional works correctly. ✅
 *
 * SRP:
 *   This class has one job — trigger the timeout service on schedule.
 *   It knows nothing about orders, payments, queues, or email.
 *
 * SCHEDULE PARAMS (from order-service.yaml):
 *   fixedDelay    — waits N ms AFTER previous run completes before starting next.
 *                   Safer than fixedRate — no overlap even if a run takes longer than the interval.
 *   initialDelay  — waits N ms after startup before first run.
 *                   Gives the app time to fully initialise and connect to all services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutJob {

    private final TimeoutService timeoutService;

    @Scheduled(
        fixedDelayString   = "${payment.timeout.check.interval-ms:60000}",
        initialDelayString = "${payment.timeout.initial-delay-ms:30000}"
    )
    public void run() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[PaymentTimeoutJob] run | START");

        timeoutService.sendExpiryWarnings();
        timeoutService.cancelExpiredOrders();

        log.info("[PaymentTimeoutJob] run | DONE");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}

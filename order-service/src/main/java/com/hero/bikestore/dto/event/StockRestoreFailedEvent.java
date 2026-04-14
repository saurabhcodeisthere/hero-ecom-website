package com.hero.bikestore.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published to stock.restore.failed.queue when inventory-service stock restore
 * fails during the payment timeout cancellation flow.
 *
 * WHY THIS EXISTS:
 * ─────────────────
 * When an AWAITING_PAYMENT order is cancelled due to timeout, we must restore
 * the stock that was reserved at order placement. If that HTTP call to
 * inventory-service fails, the stock is permanently lost — bikes that were
 * "reserved" are never returned to available inventory.
 *
 * This event is the safety net: it records WHAT failed so an operator or
 * a future retry service can manually/automatically correct the stock.
 *
 * WHO CONSUMES THIS?
 * ───────────────────
 * Currently: nobody — it's a holding queue. Messages can be inspected via
 * RabbitMQ Management UI at http://localhost:15672 → stock.restore.failed.queue
 * Future: a stock-recovery-service or admin dashboard can subscribe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRestoreFailedEvent {

    private String  orderId;
    private String  orderNumber;
    private Long    bikeId;
    private String  bikeName;
    private Integer quantity;
    private LocalDateTime failedAt;
    private String  reason;

    // Always 0 on first publish. A future retry consumer can increment this
    // and re-publish until the stock is restored, or escalate after N retries.
    private int retryCount;
}

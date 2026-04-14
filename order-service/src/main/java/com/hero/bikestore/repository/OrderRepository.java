package com.hero.bikestore.repository;

import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {           // enables findAll(Specification, Pageable)

    // Customer — fetch their own orders newest first
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    // Idempotency dedup — check before creating a new order
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    // Lookup by human-readable order number (e.g. from support tickets)
    Optional<Order> findByOrderNumber(String orderNumber);

    // Admin — all orders paginated (no status filter)
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Admin — all orders filtered by status, paginated
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    /**
     * Finds AWAITING_PAYMENT orders that need a payment expiry WARNING email.
     *
     * Conditions:
     *   1. status = AWAITING_PAYMENT       — still waiting for payment
     *   2. createdAt < warnCutoff          — older than warning threshold (e.g. 10 min ago)
     *   3. createdAt >= cancelCutoff       — NOT yet old enough to cancel (e.g. < 15 min ago)
     *                                        Avoids warning orders that should already be cancelled.
     *   4. paymentReminderSentAt IS NULL   — warning not yet sent (prevents duplicate emails)
     *
     * LEFT JOIN FETCH items — loads items eagerly to avoid LazyInitializationException
     * when the service iterates items after the query returns.
     * DISTINCT — JOIN FETCH produces duplicate Order rows (one per item); DISTINCT deduplicates.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.status = 'AWAITING_PAYMENT'
            AND o.createdAt < :warnCutoff
            AND o.createdAt >= :cancelCutoff
            AND o.paymentReminderSentAt IS NULL
            """)
    List<Order> findOrdersNeedingWarning(
            @Param("warnCutoff")   LocalDateTime warnCutoff,
            @Param("cancelCutoff") LocalDateTime cancelCutoff
    );

    /**
     * Finds AWAITING_PAYMENT orders that have EXPIRED and must be cancelled.
     *
     * Conditions:
     *   1. status = AWAITING_PAYMENT  — still waiting (not already cancelled or confirmed)
     *   2. createdAt < cancelCutoff   — older than cancellation threshold (e.g. 15 min ago)
     *
     * No check on paymentReminderSentAt — an order is cancelled regardless of whether
     * a warning was sent. Handles restarts, delayed jobs, and missed warning runs.
     *
     * LEFT JOIN FETCH items — needed so TimeoutServiceImpl can iterate items for stock restore
     * without hitting LazyInitializationException.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.status = 'AWAITING_PAYMENT'
            AND o.createdAt < :cancelCutoff
            """)
    List<Order> findExpiredOrders(
            @Param("cancelCutoff") LocalDateTime cancelCutoff
    );
}

package com.hero.bikestore.repository;

import com.hero.bikestore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for Payment records.
 *
 * findByOrderId        — used to check if payment already exists for an order
 * findByGatewayPaymentId — used by WebhookService to look up payment after mock/Razorpay callback
 *                          The gateway knows its own ID (UUID or Razorpay orderId), not our DB id.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
}

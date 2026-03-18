package com.hero.bikestore.repository;

import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Fetch all orders for a specific user, newest first
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    // Lookup by human-readable order number (e.g. from support tickets)
    Optional<Order> findByOrderNumber(String orderNumber);

    // Admin filter — get all orders in a given status
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}

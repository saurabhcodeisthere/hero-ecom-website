package com.hero.bikestore.repository;

import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Customer — fetch their own orders newest first
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    // Lookup by human-readable order number (e.g. from support tickets)
    Optional<Order> findByOrderNumber(String orderNumber);

    // Admin — all orders paginated (no status filter)
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Admin — all orders filtered by status, paginated
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
}

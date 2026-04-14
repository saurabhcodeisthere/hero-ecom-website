package com.hero.bikestore.repository;

import com.hero.bikestore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByAddedAtAsc(String userId);

    Optional<CartItem> findByUserIdAndBikeId(String userId, Long bikeId);

    void deleteAllByUserId(String userId);

    boolean existsByUserId(String userId);

    @Query("""
            SELECT c.userId,
                   COUNT(c.id),
                   SUM(c.unitPrice * c.quantity),
                   MAX(c.updatedAt)
            FROM CartItem c
            GROUP BY c.userId
            ORDER BY MAX(c.updatedAt) DESC
            """)
    List<Object[]> findAllCartSummaries();
}

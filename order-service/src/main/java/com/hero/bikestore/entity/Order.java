package com.hero.bikestore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-readable order reference e.g. ORD-20260318-A3F9C1B2
    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    // Extracted from JWT — never taken from request body
    @Column(name = "user_id", nullable = false)
    private String userId;

    // Stored for display and support purposes
    @Column(name = "user_email")
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    // Sum of all (bikePrice × quantity) for every item in this order
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Prevents lost-update race conditions when two requests modify the same order
    @Version
    private Integer version;

    // CascadeType.ALL — when an Order is saved/deleted, its items follow
    // orphanRemoval  — if an item is removed from the list, it is deleted from DB
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Always use this method to add items — it keeps the bidirectional
     * relationship consistent so JPA knows item.order == this.
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}

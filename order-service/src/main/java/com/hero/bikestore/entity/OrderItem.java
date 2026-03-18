package com.hero.bikestore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK to orders.id — lazy loaded so we don't fetch the whole order every time
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Reference to bike-service — NOT a foreign key (bike lives in a different database)
    @Column(name = "bike_id", nullable = false)
    private Long bikeId;

    // Snapshot: bike name at the time of order.
    // If the bike name changes later, this order still shows the original name.
    @Column(name = "bike_name", nullable = false, length = 100)
    private String bikeName;

    // Snapshot: price at the time of order.
    // If bike price changes next month, old orders show the original price paid.
    @Column(name = "bike_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal bikePrice;

    @Column(nullable = false)
    private Integer quantity;

    // Precomputed: bikePrice × quantity. Stored to avoid recalculating on every read.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}

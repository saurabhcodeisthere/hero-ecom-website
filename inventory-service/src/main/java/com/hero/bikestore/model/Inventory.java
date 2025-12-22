package com.hero.bikestore.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"bike_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to bike-service (NOT a foreign key).
     * This is the catalog item being sold.
     */
    @Column(name = "bike_id", nullable = false)
    private Long bikeId;

    /**
     * Current selling price.
     * Do NOT store MRP / discount yet.
     */
    @Column(nullable = false)
    private Double price;

    /**
     * Available stock count.
     */
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    /**
     * Whether this inventory entry is active (sellable).
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Optimistic locking to prevent lost updates
     * during concurrent stock updates.
     */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


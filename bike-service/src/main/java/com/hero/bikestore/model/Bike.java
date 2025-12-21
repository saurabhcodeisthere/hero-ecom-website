package com.hero.bikestore.model;

import com.hero.bikestore.model.spec.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bikes")
@Getter
@Setter
@NoArgsConstructor
public class Bike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------- Business Fields --------
    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BikeType type;

    @Embedded
    private EngineSpec engine;

    @Embedded
    private WheelTyreSpec wheelsAndTyres;

    @Embedded
    private SuspensionSpec suspension;

    @Embedded
    private TransmissionSpec transmission;

    @Embedded
    private BrakeSpec brakes;

    @Embedded
    private ElectricalSpec electricals;

    @Embedded
    private DimensionSpec dimensions;

    @Column(length = 1000)
    private String description;

    private String imageUrl;

    // -------- Operational / Internal --------
    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;

    @Column(unique = true, nullable = false)
    private String slug;

    // -------- Audit --------
    @Column(updatable = false)
    private LocalDateTime createdAt;

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


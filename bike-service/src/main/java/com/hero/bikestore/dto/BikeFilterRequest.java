package com.hero.bikestore.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BikeFilterRequest {

    // Category
    private String type;              // SPORTS, COMMUTER

    // Price
    private Double minPrice;
    private Double maxPrice;

    // Engine
    private Integer minCc;
    private Integer maxCc;
    private String engineType;         // Oil-cooled, Air-cooled

    // Wheels
    private String wheelType;          // ALLOY, SPOKE

    // Brakes
    private Boolean abs;               // true / false

    // Transmission
    private String gearbox;            // 4-speed, 5-speed

    // Dimensions
    private Integer minFuelTank;       // liters
    private Integer minGroundClearance;// mm
}


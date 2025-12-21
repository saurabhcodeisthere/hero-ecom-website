package com.hero.bikestore.model.spec;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class DimensionSpec {

    private Integer length;           // mm
    private Integer width;            // mm
    private Integer height;           // mm
    private Integer wheelBase;         // mm
    private Integer groundClearance;   // mm
    private Integer seatHeight;        // mm
    private Integer fuelTankCapacity;  // liters
}


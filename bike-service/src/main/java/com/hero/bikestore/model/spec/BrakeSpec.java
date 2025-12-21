package com.hero.bikestore.model.spec;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class BrakeSpec {

    private String frontBrake; // Disc / Drum
    private String rearBrake;  // Disc / Drum
    private Boolean abs;       // true / false
}


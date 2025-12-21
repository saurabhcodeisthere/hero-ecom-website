package com.hero.bikestore.model.spec;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class WheelTyreSpec {

    private String frontTyre;   // 90/90-18
    private String rearTyre;    // 120/80-18
    private String wheelType;   // Alloy / Spoke
}


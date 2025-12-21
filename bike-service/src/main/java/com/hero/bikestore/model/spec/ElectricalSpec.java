package com.hero.bikestore.model.spec;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ElectricalSpec {

    private Boolean headlamp;      // LED / Halogen (true = LED)
    private Boolean chargingPort;  // USB charger
    private String batteryType;    // MF / Li-ion
}


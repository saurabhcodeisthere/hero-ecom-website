package com.hero.bikestore.model.spec;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class TransmissionSpec {

    private String gearbox;     // 5-speed manual
    private String clutch;      // Wet multi-plate
    private String chassisType; // Diamond / Trellis
}

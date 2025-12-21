package com.hero.bikestore.model.spec;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class SuspensionSpec {

    private String front;  // Telescopic Fork
    private String rear;   // Mono-shock / Twin shock
}


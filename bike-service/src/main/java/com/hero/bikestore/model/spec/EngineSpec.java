package com.hero.bikestore.model.spec;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class EngineSpec {

    @Column(name = "engine_type")
    private String engineType;
    // Air-cooled / Liquid-cooled
    private Integer displacement; // cc
    private String maxPower;       // 20.4 bhp @ 8000 rpm
    private String torque;         // 27 Nm @ 6500 rpm
    private Integer cylinders;
}


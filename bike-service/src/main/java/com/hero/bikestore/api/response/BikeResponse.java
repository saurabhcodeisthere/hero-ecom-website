package com.hero.bikestore.api.response;

import com.hero.bikestore.model.spec.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class BikeResponse {

    private Long id;
    private String modelName;
    private BigDecimal price;
    private BikeType type;
    private boolean active;

    private EngineSpec engine;
    private WheelTyreSpec wheelsAndTyres;
    private SuspensionSpec suspension;
    private TransmissionSpec transmission;
    private BrakeSpec brakes;
    private ElectricalSpec electricals;
    private DimensionSpec dimensions;

    private String description;
    private String imageUrl;
}

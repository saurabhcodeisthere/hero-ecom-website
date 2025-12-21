package com.hero.bikestore.api.request;

import com.hero.bikestore.model.spec.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class BikeRequest {

    @NotBlank
    private String modelName;

    @NotNull
    private BigDecimal price;

    @NotNull
    private BikeType type;

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


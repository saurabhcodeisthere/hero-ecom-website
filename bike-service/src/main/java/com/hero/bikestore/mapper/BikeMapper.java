package com.hero.bikestore.mapper;

import com.hero.bikestore.api.request.BikeRequest;
import com.hero.bikestore.api.response.BikeResponse;
import com.hero.bikestore.model.Bike;
import org.springframework.stereotype.Component;

@Component
public class BikeMapper {

    public Bike toEntity(BikeRequest request) {
        Bike entity = new Bike();

        entity.setModelName(request.getModelName());
        entity.setPrice(request.getPrice());
        entity.setType(request.getType());

        entity.setEngine(request.getEngine());
        entity.setWheelsAndTyres(request.getWheelsAndTyres());
        entity.setSuspension(request.getSuspension());
        entity.setTransmission(request.getTransmission());
        entity.setBrakes(request.getBrakes());
        entity.setElectricals(request.getElectricals());
        entity.setDimensions(request.getDimensions());

        entity.setDescription(request.getDescription());
        entity.setImageUrl(request.getImageUrl());

        return entity;
    }

    public BikeResponse toResponse(Bike entity) {
        BikeResponse response = new BikeResponse();

        response.setId(entity.getId());
        response.setModelName(entity.getModelName());
        response.setPrice(entity.getPrice());
        response.setType(entity.getType());

        response.setEngine(entity.getEngine());
        response.setWheelsAndTyres(entity.getWheelsAndTyres());
        response.setSuspension(entity.getSuspension());
        response.setTransmission(entity.getTransmission());
        response.setBrakes(entity.getBrakes());
        response.setElectricals(entity.getElectricals());
        response.setDimensions(entity.getDimensions());

        response.setDescription(entity.getDescription());
        response.setImageUrl(entity.getImageUrl());

        return response;
    }

    public void updateEntity(Bike bike, BikeRequest request) {

        bike.setModelName(request.getModelName());
        bike.setPrice(request.getPrice());
        bike.setType(request.getType());

        bike.setEngine(request.getEngine());
        bike.setWheelsAndTyres(request.getWheelsAndTyres());
        bike.setSuspension(request.getSuspension());
        bike.setTransmission(request.getTransmission());
        bike.setBrakes(request.getBrakes());
        bike.setElectricals(request.getElectricals());
        bike.setDimensions(request.getDimensions());

        bike.setDescription(request.getDescription());
        bike.setImageUrl(request.getImageUrl());
    }
}

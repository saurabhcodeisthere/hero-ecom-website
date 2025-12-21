package com.hero.bikestore.controller;

import com.hero.bikestore.api.request.BikeRequest;
import com.hero.bikestore.api.response.BikeResponse;
import com.hero.bikestore.service.BikeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class BikeAdminController {

    @Autowired
    private BikeService bikeService;

    /**
     * Admin-only API to add a new bike to the catalog.
     * Authorization is assumed to be handled at API Gateway / Security layer.
     */
    @PostMapping("/bikes")
    public ResponseEntity<BikeResponse> addBike(
            @Valid @RequestBody BikeRequest bikeRequest) {

        BikeResponse response = bikeService.addBike(bikeRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/bikes/{id}")
    public ResponseEntity<BikeResponse> updateBike(
            @PathVariable Long id, @Valid @RequestBody BikeRequest request) {
        return ResponseEntity.ok(
                bikeService.updateBike(id, request)
        );
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BikeResponse> deactivateBike(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                bikeService.deactivateBike(id)
        );
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<BikeResponse> activateBike(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                bikeService.activateBike(id)
        );
    }
}


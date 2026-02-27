package com.hero.bikestore.controller;

import com.hero.bikestore.api.request.BikeRequest;
import com.hero.bikestore.api.response.BikeResponse;
import com.hero.bikestore.dto.BikeFilterRequest;
import com.hero.bikestore.model.Bike;
import com.hero.bikestore.payload.BikeDTO;
import com.hero.bikestore.payload.PagedBikeResponse;
import com.hero.bikestore.service.BikeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/bikes")
@CrossOrigin(origins = "http://localhost:8080")
@RequiredArgsConstructor  // Lombok generates constructor for final fields
public class BikeController {

    private final BikeService bikeService; // injected via constructor (no boilerplate)

    @GetMapping
    public ResponseEntity<PagedBikeResponse> getAllBikes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(
                bikeService.getAllBikes(page, size, sortBy, sortDir)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<PagedBikeResponse> searchBikes(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "modelName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(
                bikeService.searchBikes(query, page, size, sortBy, sortDir)
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<BikeResponse> getBikeById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                bikeService.getBikeById(id)
        );
    }

    @GetMapping("/filter")
    public ResponseEntity<PagedBikeResponse> filterBikes(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer minCc,
            @RequestParam(required = false) Integer maxCc,
            @RequestParam(required = false) String engineType,
            @RequestParam(required = false) String wheelType,
            @RequestParam(required = false) Boolean abs,
            @RequestParam(required = false) String gearbox,
            @RequestParam(required = false) Integer minFuelTank,
            @RequestParam(required = false) Integer minGroundClearance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        BikeFilterRequest filter = new BikeFilterRequest();
        filter.setType(type);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setMinCc(minCc);
        filter.setMaxCc(maxCc);
        filter.setEngineType(engineType);
        filter.setWheelType(wheelType);
        filter.setAbs(abs);
        filter.setGearbox(gearbox);
        filter.setMinFuelTank(minFuelTank);
        filter.setMinGroundClearance(minGroundClearance);

        return ResponseEntity.ok(
                bikeService.filterBikes(filter, page, size)
        );
    }
}

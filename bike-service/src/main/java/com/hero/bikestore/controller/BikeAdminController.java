package com.hero.bikestore.controller;

import com.hero.bikestore.api.request.BikeRequest;
import com.hero.bikestore.api.response.BikeResponse;
import com.hero.bikestore.service.BikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(
        name = "Bike Catalog — Admin",
        description = "Admin-only operations to add, update, activate and deactivate bikes. Requires JWT with ADMIN role."
)
public class BikeAdminController {

    @Autowired
    private BikeService bikeService;

    @Operation(
            summary = "Add a new bike to the catalog",
            description = "Creates a new bike listing with full technical specifications. " +
                          "Authorization is enforced at the API Gateway — only ADMIN role can reach this endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bike created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed — check required fields", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Valid token but insufficient role (need ADMIN)", content = @Content)
    })
    @PostMapping("/bikes")
    public ResponseEntity<BikeResponse> addBike(
            @Valid @RequestBody BikeRequest bikeRequest) {

        BikeResponse response = bikeService.addBike(bikeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Update an existing bike",
            description = "Replaces all fields of the bike with the given ID. Partial updates are not supported — " +
                          "send the complete bike object."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bike updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "404", description = "No bike found with the given ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Valid token but insufficient role (need ADMIN)", content = @Content)
    })
    @PutMapping("/bikes/{id}")
    public ResponseEntity<BikeResponse> updateBike(
            @Parameter(description = "ID of the bike to update", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody BikeRequest request) {
        return ResponseEntity.ok(bikeService.updateBike(id, request));
    }

    @Operation(
            summary = "Deactivate a bike",
            description = "Marks the bike as inactive — it will no longer appear in public catalog listings. " +
                          "The bike record is NOT deleted and can be reactivated."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bike deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "No bike found with the given ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Valid token but insufficient role (need ADMIN)", content = @Content)
    })
    @PatchMapping("/bikes/{id}/deactivate")
    public ResponseEntity<BikeResponse> deactivateBike(
            @Parameter(description = "ID of the bike to deactivate", example = "1", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bikeService.deactivateBike(id));
    }

    @Operation(
            summary = "Activate a bike",
            description = "Marks a previously deactivated bike as active — it will appear in public catalog listings again."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bike activated successfully"),
            @ApiResponse(responseCode = "404", description = "No bike found with the given ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Valid token but insufficient role (need ADMIN)", content = @Content)
    })
    @PatchMapping("/bikes/{id}/activate")
    public ResponseEntity<BikeResponse> activateBike(
            @Parameter(description = "ID of the bike to activate", example = "1", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(bikeService.activateBike(id));
    }
}

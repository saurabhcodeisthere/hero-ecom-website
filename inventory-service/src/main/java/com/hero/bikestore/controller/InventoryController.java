package com.hero.bikestore.controller;

import com.hero.bikestore.api.response.InventoryResponse;
import com.hero.bikestore.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@Tag(
        name = "Inventory — Public",
        description = "Check stock availability and pricing for Hero bikes. Requires a valid JWT."
)
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(
            summary = "Get inventory details for a bike",
            description = "Returns the current stock quantity, price and availability status for the given bike ID. " +
                          "Use this after fetching a bike from the catalog to check if it is in stock before placing an order."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory record found and returned"),
            @ApiResponse(responseCode = "404", description = "No inventory record exists for the given bikeId", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    })
    @GetMapping("/bike/{bikeId}")
    public InventoryResponse getByBikeId(
            @Parameter(description = "The bike ID (from bike-service) to look up inventory for", example = "1", required = true)
            @PathVariable Long bikeId) {
        return inventoryService.getByBikeId(bikeId);
    }
}

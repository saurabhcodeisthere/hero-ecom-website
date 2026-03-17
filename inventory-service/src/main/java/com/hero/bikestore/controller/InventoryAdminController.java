package com.hero.bikestore.controller;

import com.hero.bikestore.api.request.CreateInventoryRequest;
import com.hero.bikestore.api.request.UpdateStockRequest;
import com.hero.bikestore.api.response.InventoryResponse;
import com.hero.bikestore.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inventories")
@RequiredArgsConstructor
@Tag(
        name = "Inventory — Admin",
        description = "Admin-only operations to create inventory records and update stock quantities. Requires JWT with ADMIN role."
)
public class InventoryAdminController {

    private final InventoryService inventoryService;

    @Operation(
            summary = "Create an inventory record for a bike",
            description = "Links a bike (by its `bikeId` from bike-service) to an inventory entry with an initial " +
                          "stock quantity and selling price. Each bike should have exactly one inventory record. " +
                          "Note: `bikeId` is a logical reference — there is no DB foreign key to bike-service (decoupled design)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inventory record created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed — bikeId, price or stockQuantity missing/invalid", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Valid token but insufficient role (need ADMIN)", content = @Content)
    })
    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inventoryService.createInventory(request));
    }

    @Operation(
            summary = "Update stock quantity for an inventory record",
            description = "Sets the stock quantity for the inventory record with the given ID. " +
                          "Use this when bikes are received at the warehouse or after a sale. " +
                          "Setting stock to 0 marks the bike as out-of-stock. " +
                          "This operation uses optimistic locking — concurrent updates are safe."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "stockQuantity is missing or negative", content = @Content),
            @ApiResponse(responseCode = "404", description = "No inventory record found with the given ID", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Valid token but insufficient role (need ADMIN)", content = @Content)
    })
    @PatchMapping("/{id}/stock")
    public ResponseEntity<InventoryResponse> updateStock(
            @Parameter(description = "ID of the inventory record to update", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request) {

        return ResponseEntity.ok(inventoryService.updateStock(id, request));
    }
}

package com.hero.bikestore.controller;


import com.hero.bikestore.api.request.CreateInventoryRequest;
import com.hero.bikestore.api.request.UpdateStockRequest;
import com.hero.bikestore.api.response.InventoryResponse;
import com.hero.bikestore.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/inventories")
@RequiredArgsConstructor
public class InventoryAdminController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(inventoryService.createInventory(request));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<InventoryResponse> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request) {

        return ResponseEntity.ok(
                inventoryService.updateStock(id, request)
        );
    }
}

package com.hero.bikestore.controller;

import com.hero.bikestore.api.response.InventoryResponse;
import com.hero.bikestore.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/bike/{bikeId}")
    public InventoryResponse getByBikeId(@PathVariable Long bikeId) {
        return inventoryService.getByBikeId(bikeId);
    }
}

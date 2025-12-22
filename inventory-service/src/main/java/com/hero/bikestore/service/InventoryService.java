package com.hero.bikestore.service;

import com.hero.bikestore.api.request.CreateInventoryRequest;
import com.hero.bikestore.api.request.UpdateStockRequest;
import com.hero.bikestore.api.response.InventoryResponse;

public interface InventoryService {

    InventoryResponse createInventory(CreateInventoryRequest request);

    InventoryResponse updateStock(Long inventoryId, UpdateStockRequest request);

    InventoryResponse getByBikeId(Long bikeId);
}
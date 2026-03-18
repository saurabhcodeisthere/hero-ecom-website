package com.hero.bikestore.service.impl;

import com.hero.bikestore.api.request.CreateInventoryRequest;
import com.hero.bikestore.api.request.UpdateStockRequest;
import com.hero.bikestore.api.response.InventoryResponse;
import com.hero.bikestore.exception.InsufficientStockException;
import com.hero.bikestore.exception.InventoryAlreadyExistsException;
import com.hero.bikestore.exception.InventoryNotFoundException;
import com.hero.bikestore.model.Inventory;
import com.hero.bikestore.repository.InventoryRepository;
import com.hero.bikestore.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    // -------------------------
    // CREATE INVENTORY (ADMIN)
    // -------------------------
    @Override
    public InventoryResponse createInventory(CreateInventoryRequest request) {

        // Validation: one inventory per bike
        if (inventoryRepository.existsByBikeId(request.getBikeId())) {
            throw new InventoryAlreadyExistsException(
                    "Inventory already exists for bikeId " + request.getBikeId()
            );
        }

        Inventory inventory = Inventory.builder()
                .bikeId(request.getBikeId())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .active(true)
                .build();

        Inventory saved = inventoryRepository.save(inventory);

        return toResponse(saved);
    }

    // -------------------------
    // UPDATE STOCK (ADMIN)
    // -------------------------
    @Override
    public InventoryResponse updateStock(Long inventoryId, UpdateStockRequest request) {

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found with id " + inventoryId
                ));

        // Validation: inventory must be active
        if (!inventory.isActive()) {
            throw new IllegalStateException("Cannot update stock for inactive inventory");
        }

        inventory.setStockQuantity(request.getStockQuantity());

        /*
         * No manual version handling here.
         * Hibernate will:
         * - Check version
         * - Increment version
         * - Throw OptimisticLockException if stale
         */
        Inventory updated = inventoryRepository.save(inventory);

        return toResponse(updated);
    }

    // -------------------------
    // GET INVENTORY BY BIKE ID (PUBLIC)
    // -------------------------
    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByBikeId(Long bikeId) {

        Inventory inventory = inventoryRepository.findByBikeId(bikeId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for bikeId " + bikeId
                ));

        if (!inventory.isActive()) {
            throw new InventoryNotFoundException(
                    "Inventory is inactive for bikeId " + bikeId
            );
        }

        return toResponse(inventory);
    }

    // -----------------------------------------------
    // REDUCE STOCK — called by order-service (Feign)
    // -----------------------------------------------
    @Override
    public InventoryResponse reduceStock(Long bikeId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByBikeId(bikeId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for bikeId " + bikeId));

        if (!inventory.isActive()) {
            throw new InventoryNotFoundException("Inventory is inactive for bikeId " + bikeId);
        }

        if (inventory.getStockQuantity() < quantity) {
            throw new InsufficientStockException(bikeId, inventory.getStockQuantity(), quantity);
        }

        // Hibernate @Version handles optimistic locking automatically —
        // if two orders arrive at the same time, one will get OptimisticLockException
        inventory.setStockQuantity(inventory.getStockQuantity() - quantity);
        return toResponse(inventoryRepository.save(inventory));
    }

    // -----------------------------------------------
    // RESTORE STOCK — called by order-service (Feign) on cancellation
    // -----------------------------------------------
    @Override
    public InventoryResponse restoreStock(Long bikeId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByBikeId(bikeId)
                .orElseThrow(() -> new InventoryNotFoundException(
                        "Inventory not found for bikeId " + bikeId));

        inventory.setStockQuantity(inventory.getStockQuantity() + quantity);
        return toResponse(inventoryRepository.save(inventory));
    }

    // -------------------------
    // MAPPER (ENTITY → RESPONSE)
    // -------------------------
    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .bikeId(inventory.getBikeId())
                .price(inventory.getPrice())
                .stockQuantity(inventory.getStockQuantity())
                .active(inventory.isActive())
                .build();
    }
}

package com.hero.bikestore.repository;

import com.hero.bikestore.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByBikeId(Long bikeId);

    boolean existsByBikeId(Long bikeId);
}

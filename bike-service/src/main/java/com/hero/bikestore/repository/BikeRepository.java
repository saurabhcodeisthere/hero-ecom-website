package com.hero.bikestore.repository;

import com.hero.bikestore.model.Bike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface BikeRepository extends JpaRepository<Bike, Long>, JpaSpecificationExecutor<Bike> {

    Page<Bike> findByActiveTrue(Pageable pageable);
    boolean existsBySlug(String slug);
    Page<Bike> findByModelNameContainingIgnoreCaseAndActiveTrue(String modelName, Pageable pageable);
    Optional<Bike> findByIdAndActiveTrue(Long id);
}

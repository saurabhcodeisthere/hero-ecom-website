package com.hero.bikestore.repository.specifications;

import com.hero.bikestore.dto.BikeFilterRequest;
import com.hero.bikestore.model.Bike;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public class BikeSpecification {

    public static Specification<Bike> build(BikeFilterRequest f) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Always show active bikes
            predicates.add(cb.isTrue(root.get("active")));

            if (f.getType() != null) {
                predicates.add(cb.equal(root.get("type"), f.getType()));
            }

            if (f.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("price"), f.getMinPrice()));
            }

            if (f.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("price"), f.getMaxPrice()));
            }

            if (f.getMinCc() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("engine").get("displacement"), f.getMinCc()));
            }

            if (f.getMaxCc() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("engine").get("displacement"), f.getMaxCc()));
            }

            if (f.getEngineType() != null) {
                predicates.add(cb.equal(
                        root.get("engine").get("engineType"), f.getEngineType()));
            }

            if (f.getWheelType() != null) {
                predicates.add(cb.equal(
                        root.get("wheelsAndTyres").get("wheelType"), f.getWheelType()));
            }

            if (f.getAbs() != null) {
                predicates.add(cb.equal(
                        root.get("brakes").get("abs"), f.getAbs()));
            }

            if (f.getGearbox() != null) {
                predicates.add(cb.equal(
                        root.get("transmission").get("gearbox"), f.getGearbox()));
            }

            if (f.getMinFuelTank() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("dimensions").get("fuelTankCapacity"),
                        f.getMinFuelTank()));
            }

            if (f.getMinGroundClearance() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("dimensions").get("groundClearance"),
                        f.getMinGroundClearance()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

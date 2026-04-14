package com.hero.bikestore.specification;

import com.hero.bikestore.entity.Order;
import com.hero.bikestore.entity.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for building dynamic ORDER query filters using the JPA Criteria API.
 *
 * WHY this exists:
 *   The admin panel needs to filter orders by any combination of:
 *   status, city, state, pincode — and more fields in the future.
 *
 *   Creating a separate repository method for every combination would cause
 *   a combinatorial explosion (2^n methods for n filters). Instead, this class
 *   builds the WHERE clause dynamically at runtime — only adding conditions
 *   for the filters that were actually provided.
 *
 * HOW it works:
 *   withFilters() returns a Specification<Order> — a functional interface whose
 *   single method (toPredicate) is implemented by a lambda.
 *   Spring Data JPA calls that lambda when it executes findAll(spec, pageable).
 *
 *   Each if-block adds a condition ONLY when the corresponding value is non-null/non-blank.
 *   cb.and(...) combines all collected conditions with AND.
 *   An empty list → cb.and(new Predicate[0]) → no WHERE clause → returns all rows.
 *
 * ADDING a new filter field:
 *   Add one more @RequestParam to OrderAdminController,
 *   pass it through to OrderService.getAllOrders(),
 *   and add one more if-block here. Nothing else changes.
 */
public class OrderSpecification {

    // Private constructor — this is a pure static factory class.
    // It should never be instantiated. All methods are static because
    // the class holds no state — it just builds and returns Specification objects.
    private OrderSpecification() {}

    /**
     * Builds a dynamic Specification from the provided filter values.
     * Any parameter that is null or blank is simply ignored.
     *
     * @param status  filter by order status (e.g. CONFIRMED)
     * @param city    filter by delivery city (exact match, case-insensitive)
     * @param state   filter by delivery state (exact match, case-insensitive)
     * @param pincode filter by delivery pincode (exact match)
     * @return a Specification that produces the appropriate WHERE clause
     */
    public static Specification<Order> withFilters(
            OrderStatus status,
            String city,
            String state,
            String pincode
    ) {
        return (root, query, cb) -> {

            // Collect only the conditions that apply — starts empty, grows with each provided filter
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // root.get("shippingAddress") navigates the @Embedded DeliveryAddress object.
            // JPA knows it is embedded and translates this to the city column in orders table.
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("shippingAddress").get("city")),
                        city.trim().toLowerCase()
                ));
            }

            if (state != null && !state.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("shippingAddress").get("state")),
                        state.trim().toLowerCase()
                ));
            }

            if (pincode != null && !pincode.isBlank()) {
                predicates.add(cb.equal(
                        root.get("shippingAddress").get("pincode"),
                        pincode.trim()
                ));
            }

            // AND all collected conditions together.
            // If predicates is empty, cb.and(new Predicate[0]) produces no WHERE clause
            // and the query returns all rows — correct behaviour for "no filters applied".
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package com.hero.bikestore.api.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InventoryResponse {

    private Long id;
    private Long bikeId;
    private Double price;
    private Integer stockQuantity;
    private boolean active;
}
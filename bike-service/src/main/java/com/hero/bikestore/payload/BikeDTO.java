package com.hero.bikestore.payload;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BikeDTO {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private Integer stockQuantity;
    private Integer engineCc;
    private Double mileage;
    private String color;
    private String description;
    private String imageUrl;
    private LocalDate launchedYear;
}

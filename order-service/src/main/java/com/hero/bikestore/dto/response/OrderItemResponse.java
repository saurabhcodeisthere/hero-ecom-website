package com.hero.bikestore.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long bikeId;
    private String bikeName;
    private BigDecimal bikePrice;
    private Integer quantity;
    private BigDecimal subtotal;
}

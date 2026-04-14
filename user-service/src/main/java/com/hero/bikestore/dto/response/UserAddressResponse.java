package com.hero.bikestore.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserAddressResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String streetLine1;
    private String streetLine2;
    private String city;
    private String state;
    private String pincode;
    private boolean isDefault;
    private LocalDateTime createdAt;
}

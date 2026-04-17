package com.hero.bikestore.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties({"default"})
public class UserAddressResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String streetLine1;
    private String streetLine2;
    private String city;
    private String state;
    private String pincode;
    @JsonProperty("isDefault")
    private boolean isDefault;
    private LocalDateTime createdAt;
}

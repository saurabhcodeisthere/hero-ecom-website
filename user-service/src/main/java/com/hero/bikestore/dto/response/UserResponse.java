package com.hero.bikestore.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String role;
    private boolean active;
}

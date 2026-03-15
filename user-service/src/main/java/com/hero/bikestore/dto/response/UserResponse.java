package com.hero.bikestore.dto.response;


import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private boolean active;
}

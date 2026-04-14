package com.hero.bikestore.client.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The fields cart-service reads from the user-service address response.
 *
 * Wraps the ApiResponse envelope from user-service — Jackson maps the
 * nested "data" field automatically via the wrapper below.
 *
 * Field names match UserAddressResponse in user-service exactly.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserAddressClientResponse {

    // ApiResponse<UserAddressResponse> wrapper fields
    private int status;
    private String message;
    private UserAddressData data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UserAddressData {
        private Long id;
        private String fullName;
        private String phone;
        private String streetLine1;
        private String streetLine2;
        private String city;
        private String state;
        private String pincode;
        private boolean isDefault;
    }
}

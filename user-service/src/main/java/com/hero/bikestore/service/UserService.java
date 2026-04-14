package com.hero.bikestore.service;

import com.hero.bikestore.dto.request.SaveAddressRequest;
import com.hero.bikestore.dto.request.UpdateProfileRequest;
import com.hero.bikestore.dto.response.UserAddressResponse;
import com.hero.bikestore.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse getOrCreateUser();
    UserResponse getById(Long userId);
    UserResponse updateProfile(UpdateProfileRequest request);

    // Address operations
    UserAddressResponse saveAddress(SaveAddressRequest request);
    List<UserAddressResponse> getMyAddresses();
    UserAddressResponse setDefaultAddress(Long addressId);
    void deleteAddress(Long addressId);

    // Internal — used by cart-service via HTTP
    UserAddressResponse getAddressById(Long addressId, String keycloakUserId);

    // Admin
    UserResponse blockUser(Long userId);
    UserResponse activateUser(Long userId);
}

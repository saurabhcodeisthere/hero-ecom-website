package com.hero.bikestore.service;

import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.model.UserRole;

public interface UserService {

    /**
     * Fetch currently logged-in user.
     * If user does not exist, create it automatically.
     */
    UserResponse getOrCreateUser(String keycloakUserId,String email,String name);

    /**
     * Get user by internal userId (service-to-service).
     */
    UserResponse getById(Long userId);

    /**
     * Admin-only operations
     */
    UserResponse blockUser(Long userId);

    UserResponse activateUser(Long userId);

    UserResponse changeRole(Long userId, UserRole role);
}

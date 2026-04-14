package com.hero.bikestore.controller;

import com.hero.bikestore.dto.request.SaveAddressRequest;
import com.hero.bikestore.dto.request.UpdateProfileRequest;
import com.hero.bikestore.dto.response.ApiResponse;
import com.hero.bikestore.dto.response.UserAddressResponse;
import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ─────────────────────────────────────────────────────────────────
    // PROFILE
    // ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/users
     * Returns (or auto-creates) the currently authenticated user's profile.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(success("Current user fetched successfully", userService.getOrCreateUser()));
    }

    /**
     * GET /api/v1/users/{userId}
     * Fetch any user by their internal DB id. Accessible to both CUSTOMER and ADMIN.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(success("User fetched successfully", userService.getById(userId)));
    }

    /**
     * PATCH /api/v1/users/me
     * Update the authenticated user's fullName and/or phone.
     * Only fields present in the body are updated — null fields are ignored.
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(success("Profile updated successfully", userService.updateProfile(request)));
    }

    // ─────────────────────────────────────────────────────────────────
    // ADDRESSES
    // ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/users/me/addresses
     * Save a new delivery address. First address is automatically set as default.
     */
    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<UserAddressResponse>> saveAddress(
            @Valid @RequestBody SaveAddressRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(success("Address saved successfully", userService.saveAddress(request)));
    }

    /**
     * GET /api/v1/users/me/addresses
     * List all saved addresses for the authenticated user.
     */
    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>> getMyAddresses() {
        return ResponseEntity.ok(success("Addresses fetched successfully", userService.getMyAddresses()));
    }

    /**
     * PATCH /api/v1/users/me/addresses/{addressId}/default
     * Mark a saved address as the default delivery address.
     */
    @PatchMapping("/me/addresses/{addressId}/default")
    public ResponseEntity<ApiResponse<UserAddressResponse>> setDefaultAddress(
            @PathVariable Long addressId) {
        return ResponseEntity.ok(success("Default address updated", userService.setDefaultAddress(addressId)));
    }

    /**
     * DELETE /api/v1/users/me/addresses/{addressId}
     * Delete a saved address. If it was the default, the next oldest address is promoted.
     */
    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        userService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────
    // INTERNAL — called by cart-service
    // ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/users/addresses/{addressId}?userId={keycloakUserId}
     *
     * Internal endpoint — called by cart-service at checkout time to resolve
     * an addressId into a full address object.
     *
     * The keycloakUserId param ensures cart-service can only fetch addresses
     * that belong to the customer whose JWT it is forwarding. No customer can
     * fetch another customer's address through this endpoint.
     */
    @GetMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<UserAddressResponse>> getAddressById(
            @PathVariable Long addressId,
            @RequestParam String userId) {
        return ResponseEntity.ok(success("Address fetched", userService.getAddressById(addressId, userId)));
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────

    private <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message(message)
                .data(data)
                .build();
    }
}

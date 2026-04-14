package com.hero.bikestore.service.impl;

import com.hero.bikestore.common.exception.base.ForbiddenException;
import com.hero.bikestore.common.exception.base.ResourceNotFoundException;
import com.hero.bikestore.dto.request.SaveAddressRequest;
import com.hero.bikestore.dto.request.UpdateProfileRequest;
import com.hero.bikestore.dto.response.UserAddressResponse;
import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.exception.base.UserBlockedException;
import com.hero.bikestore.model.AuthenticatedUser;
import com.hero.bikestore.model.User;
import com.hero.bikestore.model.UserAddress;
import com.hero.bikestore.model.UserStatus;
import com.hero.bikestore.repository.UserAddressRepository;
import com.hero.bikestore.repository.UserRepository;
import com.hero.bikestore.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository        userRepository;
    private final UserAddressRepository userAddressRepository;

    // ─────────────────────────────────────────────────────────────────
    // PROFILE
    // ─────────────────────────────────────────────────────────────────

    @Override
    public UserResponse getOrCreateUser() {
        AuthenticatedUser authenticatedUser = currentUser();
        User user = userRepository
                .findByKeycloakUserId(authenticatedUser.getKeycloakUserId())
                .orElseGet(this::createNewUser);
        validateUserStatus(user);
        return toResponse(user);
    }

    @Override
    public UserResponse getById(Long userId) {
        User user = getUserOrThrow(userId);
        validateUserStatus(user);
        return toResponse(user);
    }

    @Override
    public UserResponse updateProfile(UpdateProfileRequest request) {
        AuthenticatedUser authenticatedUser = currentUser();
        User user = userRepository
                .findByKeycloakUserId(authenticatedUser.getKeycloakUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found. Call GET /api/v1/users first."));

        // Only update fields that were actually sent (null = not provided = leave unchanged)
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }

        userRepository.save(user);
        log.info("Profile updated for userId={}", authenticatedUser.getKeycloakUserId());
        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────
    // ADDRESSES
    // ─────────────────────────────────────────────────────────────────

    @Override
    public UserAddressResponse saveAddress(SaveAddressRequest request) {
        AuthenticatedUser authenticatedUser = currentUser();
        User user = userRepository
                .findByKeycloakUserId(authenticatedUser.getKeycloakUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found. Call GET /api/v1/users first."));

        // If this address is being set as default, clear any existing default first
        if (request.isDefault()) {
            userAddressRepository.clearDefaultForUser(user.getId());
        }

        // If this is the user's first address, make it default automatically
        boolean isFirstAddress = userAddressRepository.countByUserId(user.getId()) == 0;

        UserAddress address = UserAddress.builder()
                .user(user)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .streetLine1(request.getStreetLine1().trim())
                .streetLine2(request.getStreetLine2() != null ? request.getStreetLine2().trim() : null)
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .pincode(request.getPincode().trim())
                .isDefault(request.isDefault() || isFirstAddress)
                .build();

        UserAddress saved = userAddressRepository.save(address);
        log.info("Address saved for userId={} addressId={} isDefault={}",
                authenticatedUser.getKeycloakUserId(), saved.getId(), saved.isDefault());
        return toAddressResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressResponse> getMyAddresses() {
        AuthenticatedUser authenticatedUser = currentUser();
        User user = userRepository
                .findByKeycloakUserId(authenticatedUser.getKeycloakUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found."));

        return userAddressRepository
                .findByUserIdOrderByCreatedAtAsc(user.getId())
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Override
    public UserAddressResponse setDefaultAddress(Long addressId) {
        AuthenticatedUser authenticatedUser = currentUser();
        User user = userRepository
                .findByKeycloakUserId(authenticatedUser.getKeycloakUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found."));

        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));

        // Ownership check — 404 not 403 to avoid leaking other users' address IDs
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address not found: " + addressId);
        }

        // Clear all existing defaults then set this one
        userAddressRepository.clearDefaultForUser(user.getId());
        address.setDefault(true);
        userAddressRepository.save(address);

        log.info("Default address set: userId={} addressId={}", authenticatedUser.getKeycloakUserId(), addressId);
        return toAddressResponse(address);
    }

    @Override
    public void deleteAddress(Long addressId) {
        AuthenticatedUser authenticatedUser = currentUser();
        User user = userRepository
                .findByKeycloakUserId(authenticatedUser.getKeycloakUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found."));

        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address not found: " + addressId);
        }

        userAddressRepository.delete(address);

        // If the deleted address was the default, promote the oldest remaining address
        if (address.isDefault()) {
            userAddressRepository
                    .findByUserIdOrderByCreatedAtAsc(user.getId())
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        userAddressRepository.save(next);
                        log.info("New default address promoted: addressId={}", next.getId());
                    });
        }

        log.info("Address deleted: userId={} addressId={}", authenticatedUser.getKeycloakUserId(), addressId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAddressResponse getAddressById(Long addressId, String keycloakUserId) {
        // Internal endpoint — called by cart-service at checkout time
        // to resolve an addressId → full address object
        User user = userRepository
                .findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + keycloakUserId));

        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));

        // Security: address must belong to the calling user
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address not found: " + addressId);
        }

        return toAddressResponse(address);
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────────

    @Override
    public UserResponse blockUser(Long userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        return toResponse(user);
    }

    @Override
    public UserResponse activateUser(Long userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return toResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    public AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        return (AuthenticatedUser) auth.getPrincipal();
    }

    private User createNewUser() {
        AuthenticatedUser authenticatedUser = currentUser();
        User user = User.builder()
                .keycloakUserId(authenticatedUser.getKeycloakUserId())
                .email(authenticatedUser.getEmail())
                .fullName(authenticatedUser.getName())
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UserBlockedException("User account is blocked");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .active(user.getStatus() == UserStatus.ACTIVE)
                .build();
    }

    private UserAddressResponse toAddressResponse(UserAddress a) {
        return UserAddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .streetLine1(a.getStreetLine1())
                .streetLine2(a.getStreetLine2())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .isDefault(a.isDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }
}


package com.hero.bikestore.service.impl;


import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.exception.base.ResourceNotFoundException;
import com.hero.bikestore.model.User;
import com.hero.bikestore.model.UserRole;
import com.hero.bikestore.model.UserStatus;
import com.hero.bikestore.repository.UserRepository;
import com.hero.bikestore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getOrCreateUser(String keycloakUserId,String email, String name) {

        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> createNewUser(keycloakUserId,email,name));

        return toResponse(user);
    }

    @Override
    public UserResponse getById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        return toResponse(user);
    }

    @Override
    public UserResponse blockUser(Long userId) {

        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.ACTIVE);

        return toResponse(user);
    }

    @Override
    public UserResponse activateUser(Long userId) {

        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.ACTIVE);

        return toResponse(user);
    }

    @Override
    public UserResponse changeRole(Long userId, UserRole role) {

        User user = getUserOrThrow(userId);
        user.setRole(role);

        return toResponse(user);
    }

    // ---------- private helpers ----------

    private User createNewUser(String keycloakUserId,String email,String name) {

        User user = User.builder()
                .keycloakUserId(keycloakUserId)
                .email(email)
                .fullName(name)
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles( user.getRoles()
                        .stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()))
                .active(user.getStatus() == UserStatus.ACTIVE)
                .build();
    }
}

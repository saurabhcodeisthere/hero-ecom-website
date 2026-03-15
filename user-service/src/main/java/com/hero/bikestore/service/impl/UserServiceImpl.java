
package com.hero.bikestore.service.impl;


import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.exception.base.ResourceNotFoundException;
import com.hero.bikestore.exception.base.UserBlockedException;
import com.hero.bikestore.model.AuthenticatedUser;
import com.hero.bikestore.model.User;
import com.hero.bikestore.model.UserRole;
import com.hero.bikestore.model.UserStatus;
import com.hero.bikestore.repository.UserRepository;
import com.hero.bikestore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getOrCreateUser() {

        AuthenticatedUser authenticatedUser = currentUser();

        User user = userRepository
                .findByKeycloakUserId(authenticatedUser.getKeycloakUserId())
                .orElseGet(this::createNewUser);

        validateUserStatus(user);

        return toResponse(user);
    }

    private void validateUserStatus(User user) {

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UserBlockedException("User account is blocked");
        }
    }

    @Override
    public UserResponse getById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        validateUserStatus(user);

        return toResponse(user);
    }

    @Override
    public UserResponse blockUser(Long userId) {

        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        return toResponse(user);
    }

    public AuthenticatedUser currentUser() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        assert auth != null;
        return (AuthenticatedUser) auth.getPrincipal();
    }

    @Override
    public UserResponse activateUser(Long userId) {

        User user = getUserOrThrow(userId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return toResponse(user);
    }


    // ---------- private helpers ----------

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
                .active(user.getStatus() == UserStatus.ACTIVE)
                .build();
    }
}

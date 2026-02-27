package com.hero.bikestore.service.impl;


import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.model.User;
import com.hero.bikestore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse getOrCreateUser(String keycloakUserId) {

        User user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> createNewUser(keycloakUserId));

        return toResponse(user);
    }

    @Override
    public UserResponse getById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found with id: " + userId));

        return toResponse(user);
    }

    @Override
    public UserResponse blockUser(Long userId) {

        User user = getUserOrThrow(userId);
        user.setActive(false);

        return toResponse(user);
    }

    @Override
    public UserResponse activateUser(Long userId) {

        User user = getUserOrThrow(userId);
        user.setActive(true);

        return toResponse(user);
    }

    @Override
    public UserResponse changeRole(Long userId, UserRole role) {

        User user = getUserOrThrow(userId);
        user.setRole(role);

        return toResponse(user);
    }

    // ---------- private helpers ----------

    private User createNewUser(String keycloakUserId) {

        User user = User.builder()
                .keycloakUserId(keycloakUserId)
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found with id: " + userId));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .active(user.isActive())
                .build();
    }
}

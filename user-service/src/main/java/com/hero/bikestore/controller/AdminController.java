package com.hero.bikestore.controller;

import com.hero.bikestore.dto.response.ApiResponse;
import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.model.UserRole;
import com.hero.bikestore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(
        name = "User Admin",
        description = "Admin-only operations to manage platform user accounts. Requires JWT with ADMIN role."
)
public class AdminController {

    private final UserService userService;

    @Operation(
            summary = "Block a user account",
            description = "Marks the user account as BLOCKED. A blocked user cannot log in or access any protected endpoints. " +
                          "The account is not deleted and can be reactivated with the activate endpoint."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User blocked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No user found with the given ID", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Valid token but insufficient role (need ADMIN)", content = @Content)
    })
    @PatchMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<UserResponse>> blockUser(
            @Parameter(description = "Internal platform user ID to block", example = "5", required = true)
            @PathVariable Long userId) {

        UserResponse user = userService.blockUser(userId);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("User blocked successfully")
                        .data(user)
                        .build()
        );
    }

    @Operation(
            summary = "Activate a blocked user account",
            description = "Restores a BLOCKED user account back to ACTIVE status. " +
                          "The user will be able to log in and use the platform again."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User activated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No user found with the given ID", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Valid token but insufficient role (need ADMIN)", content = @Content)
    })
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @Parameter(description = "Internal platform user ID to activate", example = "5", required = true)
            @PathVariable Long userId) {

        UserResponse user = userService.activateUser(userId);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("User activated successfully")
                        .data(user)
                        .build()
        );
    }
}

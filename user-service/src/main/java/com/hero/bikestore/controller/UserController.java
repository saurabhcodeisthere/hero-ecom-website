package com.hero.bikestore.controller;

import com.hero.bikestore.dto.response.ApiResponse;
import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(
        name = "Users",
        description = "Fetch profile information for authenticated users. Requires JWT with CUSTOMER or ADMIN role."
)
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get the currently authenticated user",
            description = "Reads the JWT claims (email, name) and looks up — or auto-creates — the user record " +
                          "in the local database. This is the entry-point that syncs Keycloak identity with the platform."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Token valid but role insufficient", content = @Content)
    })
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        System.out.println("Controller reached");
        UserResponse user = userService.getOrCreateUser();

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Current User fetched successfully")
                        .data(user)
                        .build()
        );
    }

    @Operation(
            summary = "Get a user by ID",
            description = "Fetches the profile of any platform user by their internal database ID. " +
                          "Accessible to both CUSTOMER and ADMIN roles."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found and returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No user found with the given ID", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT token", content = @Content)
    })
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "Internal platform user ID", example = "1", required = true)
            @PathVariable Long userId) {

        UserResponse user = userService.getById(userId);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("User fetched successfully")
                        .data(user)
                        .build()
        );
    }
}

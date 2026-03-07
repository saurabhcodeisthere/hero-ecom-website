package com.hero.bikestore.controller;

import com.hero.bikestore.dto.response.ApiResponse;
import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal Jwt jwt){
        String keyCloakUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        //String keyCloakUserId = "refjbiwjbfikbsdckjbsdhufvejbceijbwiu";
        System.out.println("Controller reached");

        UserResponse user = userService.getOrCreateUser(keyCloakUserId,email,name);

        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .timestamp(LocalDateTime.now())
                        .status(200)
                        .message("Current User fetched successfully")
                        .data(user)
                        .build()
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId){
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

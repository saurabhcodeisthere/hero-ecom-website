package com.hero.bikestore.controller;

import com.hero.bikestore.dto.response.ApiResponse;
import com.hero.bikestore.dto.response.UserResponse;
import com.hero.bikestore.model.UserRole;
import com.hero.bikestore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PatchMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<UserResponse>> blockUser(@PathVariable Long userId) {

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

        @PatchMapping("/{userId}/activate")
         public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long userId){

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

        @PatchMapping("/{userId}/role")
        public ResponseEntity<ApiResponse<UserResponse>> changeRole(@PathVariable Long userId,
                                                                   @RequestParam UserRole role){
            UserResponse user = userService.changeRole(userId,role);

            return ResponseEntity.ok(
                    ApiResponse.<UserResponse>builder()
                            .timestamp(LocalDateTime.now())
                            .status(200)
                            .message("User Role updated successfully")
                            .data(user)
                            .build()
            );
        }
    }


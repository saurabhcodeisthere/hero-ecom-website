package com.hero.bikestore.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private String keycloakUserId;
    private String email;
    private String name;
    private Set<String> roles;
}

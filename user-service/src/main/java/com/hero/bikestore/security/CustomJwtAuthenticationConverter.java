package com.hero.bikestore.security;

import com.hero.bikestore.model.AuthenticatedUser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KeycloakRoleConverter roleConverter;

    public CustomJwtAuthenticationConverter(KeycloakRoleConverter roleConverter) {
        this.roleConverter = roleConverter;
    }
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = roleConverter.convert(jwt);

        String userId=jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        assert authorities != null;
        Set<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.toSet());

        AuthenticatedUser principal = new AuthenticatedUser(userId, email, name, roles);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }


}

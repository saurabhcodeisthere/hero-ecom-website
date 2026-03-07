package com.hero.bikestore.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private static final Logger log = LoggerFactory.getLogger(KeycloakRoleConverter.class);
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        log.debug("Raw 'realm_access' claim: {}", realmAccess);
        if(realmAccess == null || realmAccess.isEmpty()) {
            log.warn("No 'realm_access' found in JWT! User will have no roles.");
            return Collections.emptyList();
        }
        List<String> roles = (List<String>) realmAccess.get("roles");
        log.debug("Roles found in token: {}", roles);

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(roleName -> "ROLE_" + roleName.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        log.debug("Final Extracted Authorities: {}", authorities);
        log.debug("=== JWT Extraction End ===");

        return authorities;
    }
}

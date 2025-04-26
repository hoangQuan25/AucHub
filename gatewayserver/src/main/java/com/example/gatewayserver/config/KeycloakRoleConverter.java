package com.example.gatewayserver.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt source) {
        Object realmAccessObj = source.getClaims().get("realm_access");

        if (!(realmAccessObj instanceof Map<?, ?> realmAccess)) {
            return Collections.emptyList();
        }

        Object rolesObj = realmAccess.get("roles");

        if (!(rolesObj instanceof List<?> roles)) {
            return Collections.emptyList();
        }

        return roles.stream()
                .filter(role -> role instanceof String)
                .map(role -> "ROLE_" + role)
                .map(role -> new SimpleGrantedAuthority((String) role))
                .collect(Collectors.toList());
    }
}


package com.example.users.config;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class KeycloakAdminConfig {

    private final KeycloakAdminClientProperties keycloakProperties;

    @Bean
    public Keycloak keycloakAdminClient() {

        return KeycloakBuilder.builder()
                .serverUrl(keycloakProperties.getAuthServerUrl())
                .realm(keycloakProperties.getRealm())
                .grantType("client_credentials")
                .clientId(keycloakProperties.getResource())
                .clientSecret(keycloakProperties.getCredentials().getSecret())
                .build();
    }
}
package com.example.users.config;

import com.example.users.config.KeycloakAdminClientProperties;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class KeycloakAdminConfig {

    // Inject the properties class we defined earlier
    private final KeycloakAdminClientProperties keycloakProperties;

    @Bean
    public Keycloak keycloakAdminClient() {
        // Use ResteasyClientBuilder if specific proxy/TLS settings are needed
        // ResteasyClient client = new ResteasyClientBuilderImpl().connectionPoolSize(10).build();

        return KeycloakBuilder.builder()
                .serverUrl(keycloakProperties.getAuthServerUrl())
                .realm(keycloakProperties.getRealm())
                .grantType("client_credentials") // Use client credentials grant
                .clientId(keycloakProperties.getResource()) // Your service account client ID
                .clientSecret(keycloakProperties.getCredentials().getSecret()) // The secret for that client
                // .resteasyClient(client) // Optional: provide custom http client
                .build();
    }
}
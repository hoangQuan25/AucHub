package com.example.users.config; // Or your config package

import lombok.Data; // Or use getters/setters
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Validated
@Data
public class KeycloakAdminClientProperties {


    private String realm;

    private String authServerUrl;

    private String resource; // Corresponds to keycloak.resource (Client ID)

    private Credentials credentials = new Credentials();

    @Data
    public static class Credentials {
        private String secret; // Corresponds to keycloak.credentials.secret
    }

}
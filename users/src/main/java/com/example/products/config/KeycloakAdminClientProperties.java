package com.example.users.config; // Or your config package

import lombok.Data; // Or use getters/setters
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
// Optional: Add validation annotations if needed (e.g., @NotBlank)
// import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "keycloak") // Matches the prefix in application.yml
@Validated // Optional: enables validation
@Data // Lombok annotation for getters/setters/etc.
public class KeycloakAdminClientProperties {

    // Properties must match the keys under 'keycloak:' in YAML
    // Use nested classes for nested structures like 'credentials'

    // @NotBlank // Example validation
    private String realm;

    // @NotBlank
    private String authServerUrl;

    // @NotBlank
    private String resource; // Corresponds to keycloak.resource (Client ID)

    private Credentials credentials = new Credentials();

    @Data // Lombok for nested class
    public static class Credentials {
        // @NotBlank
        private String secret; // Corresponds to keycloak.credentials.secret
    }

    // Add getters and setters if not using Lombok
}
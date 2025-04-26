// src/main/java/com/example/users/service/KeycloakAdminService.java // Or your actual package
package com.example.products.service; // Use your actual package

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation; // <<<--- Add this import
import org.springframework.stereotype.Service;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final Keycloak keycloakAdminClient; // Inject the configured Admin Client Bean
    // Consider making these configurable via KeycloakAdminClientProperties
    private final String realmName = "auction-realm";
    private final String sellerRoleName = "ROLE_SELLER";

    /**
     * Adds the configured seller role to a specific user in Keycloak.
     * Requires the service account to have 'manage-users' permission.
     * @param userId The ID of the user in Keycloak.
     */
    public void addSellerRoleToUser(String userId) {
        log.info("Attempting to add role '{}' to user '{}' in realm '{}'", sellerRoleName, userId, realmName);
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realmName);
            UsersResource usersResource = realmResource.users();
            UserResource userResource = usersResource.get(userId); // Get specific user resource

            // Verify user exists by trying to fetch representation first
            try {
                userResource.toRepresentation();
                log.debug("Verified user '{}' exists in Keycloak.", userId);
            } catch (NotFoundException nfe) {
                log.error("User with ID '{}' not found in Keycloak realm '{}' before attempting role add.", userId, realmName);
                throw new RuntimeException("Cannot add role: Keycloak user not found", nfe);
            }

            // Find the seller role representation
            RoleRepresentation sellerRole = realmResource.roles().get(sellerRoleName).toRepresentation();
            if (sellerRole == null) {
                // This case might also throw NotFoundException depending on Keycloak version/client
                log.error("Role '{}' not found in Keycloak realm '{}'. Cannot assign.", sellerRoleName, realmName);
                throw new RuntimeException("Seller role configuration not found in Keycloak");
            }
            log.debug("Found role representation for '{}'", sellerRoleName);

            // Add the realm role to the user
            userResource.roles().realmLevel().add(List.of(sellerRole));
            log.info("Successfully assigned role '{}' to user '{}' in Keycloak.", sellerRoleName, userId);

        } catch (WebApplicationException e) {
            log.error("Keycloak Admin API error while adding role '{}' to user '{}': Status: {}, Response: {}",
                    sellerRoleName, userId, e.getResponse().getStatus(), e.getResponse().readEntity(String.class), e);
            // Rethrow as a runtime exception (ControllerAdvice can handle it)
            throw new RuntimeException("Failed to add seller role via Keycloak Admin API", e);
        } catch (Exception e) {
            log.error("Unexpected error while adding role '{}' to user '{}'", sellerRoleName, userId, e);
            throw new RuntimeException("Unexpected error during seller role assignment", e);
        }
    }

    // --- NEW METHOD ---
    /**
     * Fetches the user representation (details like username, email, first/last name)
     * from Keycloak using the Admin API.
     * Requires the service account to have 'view-users' permission.
     * @param userId The Keycloak User ID (sub claim)
     * @return UserRepresentation containing user details from Keycloak
     * @throws RuntimeException if user not found or other Keycloak error occurs
     */
    public UserRepresentation getKeycloakUserById(String userId) {
        log.debug("Attempting to fetch Keycloak user representation for ID: {}", userId);
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realmName);
            UsersResource usersResource = realmResource.users();
            // Get the specific user resource by ID
            UserResource userResource = usersResource.get(userId);

            // Fetch the representation (throws NotFoundException if user doesn't exist)
            UserRepresentation userRep = userResource.toRepresentation();

            if (userRep == null) {
                // Should typically be caught by NotFoundException from toRepresentation()
                log.error("UserRepresentation was unexpectedly null for user ID '{}' in realm '{}'", userId, realmName);
                throw new RuntimeException("Keycloak user representation not found but no NotFoundException was thrown.");
            }
            log.info("Successfully fetched Keycloak user representation for ID: {}", userId);
            return userRep;

        } catch (NotFoundException nfe) {
            log.warn("User with ID '{}' not found in Keycloak realm '{}' during fetch.", userId, realmName);
            // Let the caller (UserServiceImpl) handle this specific case if needed,
            // or rethrow a more specific application exception.
            throw new RuntimeException("Keycloak user not found for ID: " + userId, nfe);
        } catch (WebApplicationException e) {
            // Catch other Keycloak/HTTP errors (like permission denied - 403)
            log.error("Keycloak Admin API error while fetching user '{}': Status: {}, Response: {}",
                    userId, e.getResponse().getStatus(), e.getResponse().readEntity(String.class), e);
            throw new RuntimeException("Failed to fetch user details via Keycloak Admin API", e);
        } catch (Exception e) {
            // Catch other potential exceptions
            log.error("Unexpected error while fetching user '{}'", userId, e);
            throw new RuntimeException("Unexpected error during Keycloak user fetch", e);
        }
    }
    // --- END NEW METHOD ---
}
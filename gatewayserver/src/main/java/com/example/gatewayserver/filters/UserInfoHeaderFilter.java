package com.example.gatewayserver.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder; // Corrected import

import java.util.Optional;

@Component
public class UserInfoHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UserInfoHeaderFilter.class);

    // Define header names as constants (matching what UserController expects)
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_USERNAME_HEADER = "X-User-Username";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    // You could add one for roles too if needed downstream
    // private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // Get the security context reactively
        return ReactiveSecurityContextHolder.getContext()
                // Only proceed if security context exists and authentication is a JwtAuthenticationToken
                .filter(context -> {
                    Authentication authentication = context.getAuthentication();
                    return authentication != null && authentication instanceof JwtAuthenticationToken;
                })
                .map(SecurityContext::getAuthentication) // Get the Authentication object
                .cast(JwtAuthenticationToken.class)     // Cast it to JwtAuthenticationToken
                .map(JwtAuthenticationToken::getToken) // Get the Jwt token itself
                .flatMap(jwt -> { // Use flatMap for the async operation of modifying exchange
                    // Extract claims from the validated JWT
                    String userId = jwt.getSubject(); // 'sub' claim is the standard user ID
                    String username = jwt.getClaimAsString("preferred_username"); // Standard Keycloak claim
                    String email = jwt.getClaimAsString("email");              // Standard Keycloak claim
                    // Optional: Extract roles if needed downstream (KeycloakRoleConverter already does this for Security)
                    // List<String> roles = ... extract roles from jwt.getClaim("realm_access")...

                    log.debug("UserInfoHeaderFilter: Adding headers for User ID: {}", userId);

                    // Create a modified request with the new headers
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header(USER_ID_HEADER, Optional.ofNullable(userId).orElse("")) // Add User ID Header
                            .header(USER_USERNAME_HEADER, Optional.ofNullable(username).orElse("")) // Add Username Header
                            .header(USER_EMAIL_HEADER, Optional.ofNullable(email).orElse(""))     // Add Email Header
                            // .header(USER_ROLES_HEADER, roles != null ? String.join(",", roles) : "") // Example for roles
                            .build();

                    // Create a new exchange with the modified request
                    ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
                    // Continue the filter chain with the modified exchange
                    return chain.filter(modifiedExchange);
                })
                // If the security context was empty or authentication wasn't a JwtAuthenticationToken,
                // just continue the filter chain with the original exchange
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        // IMPORTANT: Define the order
        // Run this filter *AFTER* Spring Security's authentication filter has run
        // SecurityWebFiltersOrder.AUTHENTICATION has order -100
        // So, a value slightly higher (less negative or positive) runs after it.
        return SecurityWebFiltersOrder.AUTHENTICATION.getOrder() + 1;
        // Alternatively, just use a value like -90 or 1 etc.
    }
}


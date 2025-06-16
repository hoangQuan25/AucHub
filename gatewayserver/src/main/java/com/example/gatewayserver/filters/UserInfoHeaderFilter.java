package com.example.gatewayserver.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder; // Corrected import

import java.util.Optional;

@Component
public class UserInfoHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UserInfoHeaderFilter.class);

    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_USERNAME_HEADER = "X-User-Username";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
                .filter(context -> {
                    Authentication authentication = context.getAuthentication();
                    return authentication != null && authentication instanceof JwtAuthenticationToken;
                })
                .map(SecurityContext::getAuthentication) // Get the Authentication object
                .cast(JwtAuthenticationToken.class)     // Cast it to JwtAuthenticationToken
                .map(JwtAuthenticationToken::getToken) // Get the Jwt token itself
                .flatMap(jwt -> {
                    String userId = jwt.getSubject(); // 'sub' claim is the standard user ID
                    String username = jwt.getClaimAsString("preferred_username"); // Standard Keycloak claim
                    String email = jwt.getClaimAsString("email");              // Standard Keycloak claim

                    log.debug("UserInfoHeaderFilter: Adding headers for User ID: {}", userId);

                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header(USER_ID_HEADER, Optional.ofNullable(userId).orElse("")) // Add User ID Header
                            .header(USER_USERNAME_HEADER, Optional.ofNullable(username).orElse("")) // Add Username Header
                            .header(USER_EMAIL_HEADER, Optional.ofNullable(email).orElse(""))     // Add Email Header
                            .build();

                    ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
                    return chain.filter(modifiedExchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return SecurityWebFiltersOrder.AUTHENTICATION.getOrder() + 1;
    }
}


package com.example.gatewayserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity security) {
        security.authorizeExchange(exchanges -> exchanges
                        // Define specific public paths if needed (e.g., viewing auctions)
                        // .pathMatchers(HttpMethod.GET, "/api/auctions", "/api/auctions/**").permitAll() // Example: Allow anyone to view auctions
                        // .pathMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll() // Example: Allow anyone to view products

                        // Secure WebSocket connections (handshake needs authentication)
//                        .pathMatchers("/ws/**").authenticated()
//
//                        // Secure user-specific endpoints
//                        .pathMatchers("/api/users/me/**").authenticated() // Requires login to access own profile

                        // Define other specific rules for auctions, products, orders etc. as you build them
                        // Example: Only Sellers can create products
                        // .pathMatchers(HttpMethod.POST, "/api/products").hasRole("SELLER")
                        // Example: Only authenticated users can bid
                        // .pathMatchers(HttpMethod.POST, "/api/auctions/**/bids").authenticated()

                        // --- IMPORTANT: Secure everything else by default ---
                        .anyExchange().permitAll() // Require authentication for any other request
                )
                // Configure JWT validation as before
                .oauth2ResourceServer(oauth2Spec -> oauth2Spec
                        .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
                );

        // Disable CSRF as it's typically not needed for stateless APIs
        security.csrf(ServerHttpSecurity.CsrfSpec::disable);

        return security.build();
    }

    // Keep your grantedAuthoritiesExtractor method as it was
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter
                (new KeycloakRoleConverter()); // Assumes KeycloakRoleConverter is correctly implemented
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }
}
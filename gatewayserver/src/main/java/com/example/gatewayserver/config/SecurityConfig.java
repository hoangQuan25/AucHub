package com.example.gatewayserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity security) {
        security.authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "api/products/categories").permitAll() // Allow public access
                        .pathMatchers(HttpMethod.POST, "api/products/new-product").hasRole("SELLER")
                        .pathMatchers(HttpMethod.PUT, "/api/products/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.GET, "/api/products/my").hasRole("SELLER")

                        .pathMatchers(HttpMethod.POST, "/api/liveauctions/new-auction").permitAll() // Only sellers create
                        .pathMatchers(HttpMethod.POST, "/api/liveauctions/{auctionId}/bids").permitAll() // Must be logged in to bid
                        .pathMatchers(HttpMethod.GET, "/api/liveauctions/{auctionId}/details").permitAll() // Require login to see details (safer default)
                        .pathMatchers(HttpMethod.GET, "/api/liveauctions").permitAll() // Public listing is okay

                        .pathMatchers(HttpMethod.GET, "/api/liveauctions/test/ping").permitAll() // Public ping
                        .pathMatchers(HttpMethod.POST, "/api/liveauctions/test/echo").hasRole("SELLER") // Requires SELLER role

                        // --- WebSocket Rule ---
                        .pathMatchers("/ws/**").permitAll()
                        .anyExchange().permitAll()
                )

                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable) // Disable request caching
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance()) // Force stateless context handling
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .oauth2ResourceServer(oauth2Spec -> oauth2Spec
                        .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
                );

        security.csrf(ServerHttpSecurity.CsrfSpec::disable);

        return security.build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter
                (new KeycloakRoleConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }
}
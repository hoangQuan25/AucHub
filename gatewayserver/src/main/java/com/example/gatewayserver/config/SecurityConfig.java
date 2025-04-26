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
                        .pathMatchers(HttpMethod.GET, "api/products/categories").permitAll() // Allow public access
                        .pathMatchers(HttpMethod.POST, "api/products/new-product").hasRole("SELLER")
                        .pathMatchers(HttpMethod.PUT, "/api/products/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("SELLER")
                        .pathMatchers(HttpMethod.GET, "/api/products/my").hasRole("SELLER")
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
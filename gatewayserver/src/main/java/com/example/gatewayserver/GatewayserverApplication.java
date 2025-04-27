package com.example.gatewayserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class GatewayserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayserverApplication.class, args);
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route(p -> p
						.path("/api/users/**") // Define a path prefix for user-related APIs
						.filters(f -> f
										// Optional: Remove prefix if User Service endpoints don't expect /api/users
										.rewritePath("/api/users/(?<segment>.*)", "/${segment}")
										.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
								// Add other filters like CircuitBreaker if needed for this service
								// Authentication is handled globally by SecurityConfig
						)
						.uri("lb://USERS"))
				.route(p -> p
						.path("/api/products/**") // Define a path prefix for user-related APIs
						.filters(f -> f
										// Optional: Remove prefix if User Service endpoints don't expect /api/users
										.rewritePath("/api/products/(?<segment>.*)", "/${segment}")
										.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
								// Add other filters like CircuitBreaker if needed for this service
								// Authentication is handled globally by SecurityConfig
						)
						.uri("lb://PRODUCTS"))
				.route(p -> p
						.path("/api/liveauctions/**") // Define a path prefix for user-related APIs
						.filters(f -> f
										// Optional: Remove prefix if User Service endpoints don't expect /api/users
										.rewritePath("/api/liveauctions/(?<segment>.*)", "/${segment}")
										.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
								// Add other filters like CircuitBreaker if needed for this service
								// Authentication is handled globally by SecurityConfig
						)
						.uri("lb://LIVEAUCTIONS")).build();

	}
}

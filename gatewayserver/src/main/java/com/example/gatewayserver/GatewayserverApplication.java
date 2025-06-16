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
		LocalDateTime responseTime = LocalDateTime.now(); // Calculate once for potentially better consistency per startup

		return builder.routes()
				.route("users_route", p -> p // Give routes IDs for easier management/debugging
						.path("/api/users/**")
						.filters(f -> f
								.rewritePath("/api/users/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", responseTime.toString())
						)
						.uri("lb://USERS"))
				.route("products_route", p -> p
						.path("/api/products/**")
						.filters(f -> f
								.rewritePath("/api/products/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", responseTime.toString())
						)
						.uri("lb://PRODUCTS"))

				.route("liveauctions_api_route", p -> p // Specific ID for the API route
						.path("/api/liveauctions/**")
						.filters(f -> f
								.rewritePath("/api/liveauctions/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", responseTime.toString())
						)
						.uri("lb://LIVEAUCTIONS"))


				.route("timedauctions_api_route", p -> p // Specific ID for the API route
						.path("/api/timedauctions/**")
						.filters(f -> f
								.rewritePath("/api/timedauctions/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", responseTime.toString())
						)
						.uri("lb://TIMEDAUCTIONS"))

				.route("notifications_api_route", p -> p // Specific ID for the API route
						.path("/api/notifications/**")
						.filters(f -> f
								.rewritePath("/api/notifications/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", responseTime.toString())
						)
						.uri("lb://NOTIFICATIONS"))
				.route("orders_api_route", p -> p // Specific ID for the API route
						.path("/api/orders/**")
						.filters(f -> f
								.rewritePath("/api/orders/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", responseTime.toString())
						)
						.uri("lb://ORDERS"))
				.route("payments_api_route", p -> p // Specific ID for the API route
						.path("/api/payments/**")
						.filters(f -> f
								.rewritePath("/api/payments/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", responseTime.toString())
						)
						.uri("lb://PAYMENTS"))
				.route("deliveries_api_route", p -> p // Specific ID for the API route
						.path("/api/deliveries/**")
						.filters(f -> f
								.rewritePath("/api/deliveries/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", responseTime.toString())
						)
						.uri("lb://DELIVERIES"))

				.route("notifications_ws_route", p -> p
						.path("/ws/notifications/**")
						.uri("lb://NOTIFICATIONS"))

				.route("liveauctions_ws_route", p -> p // ID for the WebSocket route
						.path("/ws/**")
						.uri("lb://LIVEAUCTIONS"))

				.build();
	}
}
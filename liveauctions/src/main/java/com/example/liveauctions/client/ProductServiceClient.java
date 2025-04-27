package com.example.liveauctions.client;

import com.example.liveauctions.client.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// name should match the service discovery name (e.g., Eureka, Consul)
// path is *removed* because Gateway rewrites /api/products/** to /**
@FeignClient(name = "products") // Use registered service name
public interface ProductServiceClient {

    // Path here matches the endpoint on the *target* ProductController
    @GetMapping("/{productId}") // Path after Gateway rewrite
    ProductDto getProductById(@PathVariable("productId") Long productId);

    // Add other methods if needed (e.g., check product availability)
}
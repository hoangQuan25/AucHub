package com.example.liveauctions.client;

import com.example.liveauctions.client.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "products") // Use registered service name
public interface ProductServiceClient {

    @GetMapping("/{productId}") // Path after Gateway rewrite
    ProductDto getProductById(@PathVariable("productId") Long productId);

}
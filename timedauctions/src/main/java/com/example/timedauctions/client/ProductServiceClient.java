package com.example.timedauctions.client;

import com.example.timedauctions.client.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "products")
public interface ProductServiceClient {

    @GetMapping("/{productId}") // Path after Gateway rewrite
    ProductDto getProductById(@PathVariable("productId") Long productId);

}
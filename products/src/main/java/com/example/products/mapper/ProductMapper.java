// src/main/java/com/example/productservice/mapper/ProductMapper.java
package com.example.products.mapper;

import com.example.products.dto.CreateProductDto;
import com.example.products.dto.ProductDto;
import com.example.products.entity.Product;
import java.util.List;

public interface ProductMapper {

    ProductDto toProductDto(Product product);

    List<ProductDto> toProductDtoList(List<Product> products);

    // Map Create DTO to Entity, ignoring fields set by service/DB
    Product createDtoToProduct(CreateProductDto dto);

    // Add updateDtoToProduct later if needed
}
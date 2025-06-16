// src/main/java/com/example/products/service/productsImpl.java
package com.example.products.service.impl;

import com.example.products.dto.CategoryDto;
import com.example.products.dto.CreateProductDto;
import com.example.products.dto.ProductDto;
import com.example.products.dto.UpdateProductDto;
import com.example.products.entity.Category;
import com.example.products.entity.Product;
import com.example.products.exception.ProductNotFoundException;
import com.example.products.mapper.CategoryMapper;
import com.example.products.mapper.ProductMapper;
import com.example.products.repository.CategoryRepository;
import com.example.products.repository.ProductRepository;
import com.example.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;


    @Override
    @Transactional
    public ProductDto createProduct(String sellerId, CreateProductDto dto) {
        log.info("Creating product '{}' for seller ID: {}", dto.getTitle(), sellerId);

        // 1. Fetch Category entities based on IDs provided in DTO
        Set<Category> categories = new HashSet<>();
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            categories = new HashSet<>(categoryRepository.findAllById(dto.getCategoryIds()));
            if (categories.size() != dto.getCategoryIds().size()) {
                log.warn("Some category IDs provided were not found for seller ID: {}", sellerId);
            }
        }
        if (categories.isEmpty()) {
            log.error("No valid categories found or provided for product creation by seller ID: {}", sellerId);
            throw new IllegalArgumentException("At least one valid category must be assigned.");
        }


        // 2. Map basic product info from DTO (now includes condition)
        Product product = productMapper.createDtoToProduct(dto);

        // 3. Set fields not mapped automatically
        product.setSellerId(sellerId);
        product.setCategories(categories); // Set the fetched category entities

        // 4. Save the product
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return productMapper.toProductDto(savedProduct);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(String sellerId, Long productId, UpdateProductDto dto) {
        log.info("Updating product ID: {} for seller ID: {}", productId, sellerId);

        // 1. Fetch existing product, ensuring it belongs to the seller
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found with ID: {}", productId);
                    return new RuntimeException("Product not found"); // Use specific exception
                });

        // 2. Verify ownership
        if (!existingProduct.getSellerId().equals(sellerId)) {
            log.error("User {} attempted to update product {} owned by {}", sellerId, productId, existingProduct.getSellerId());
            // Throw Forbidden or Not Found for security
            throw new RuntimeException("Product not found or access denied");
        }

        // 3. Fetch Category entities based on IDs provided in DTO
        Set<Category> categories = new HashSet<>();
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            categories = new HashSet<>(categoryRepository.findAllById(dto.getCategoryIds()));
        }
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("At least one valid category must be assigned during update.");
        }

        // 4. Apply updates from DTO to the fetched entity using mapper
        productMapper.updateProductFromDto(dto, existingProduct);

        // 5. Manually set the updated categories
        existingProduct.setCategories(categories);

        // 6. Save the updated entity
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product ID: {} updated successfully for seller ID: {}", productId, sellerId);

        // 7. Map back to DTO and return
        return productMapper.toProductDto(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(String sellerId, Long productId) {
        log.warn("Attempting to delete product ID: {} for seller ID: {}", productId, sellerId); // Warn level for deletion

        // Fetch existing product, ensuring it belongs to the seller
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found with ID: {} during delete attempt.", productId);
                    return new RuntimeException("Product not found"); // Use specific exception
                });

        if (!product.getSellerId().equals(sellerId)) {
            log.error("SECURITY: User {} attempted to delete product {} owned by {}", sellerId, productId, product.getSellerId());
            throw new RuntimeException("Access denied - You do not own this product"); // Or Forbidden exception
        }

        productRepository.delete(product);
        log.info("Product ID: {} deleted successfully by seller ID: {}", productId, sellerId);
    }

    @Override
    @Transactional(readOnly = true) // Mark as read-only transaction
    public ProductDto getProductById(Long productId) {
        log.debug("Fetching product by ID: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found with ID: {}", productId);
                    return new ProductNotFoundException("Product not found with ID: " + productId);
                });

        // Map the found entity to DTO
        return productMapper.toProductDto(product);
    }

    @Override
    @Transactional
    public ProductDto markProductAsSold(Long productId) {
        log.info("Marking product ID: {} as SOLD", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        if (product.isSold()) {
            log.warn("Product ID: {} is already marked as SOLD.", productId);
            return productMapper.toProductDto(product); // Or handle as appropriate
        }
        product.setSold(true); // Set the boolean flag
        Product savedProduct = productRepository.save(product);
        log.info("Product ID: {} successfully marked as SOLD.", savedProduct.getId());
        return productMapper.toProductDto(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsBySeller(String sellerId) {
        log.debug("Fetching products for seller ID: {}", sellerId);
        List<Product> products = productRepository.findBySellerId(sellerId);
        return productMapper.toProductDtoList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        log.debug("Fetching all categories");
        List<Category> categories = categoryRepository.findAll(); // Fetch all
        // Let frontend build hierarchy from flat list + parentId for simplicity now
        return categoryMapper.toCategoryDtoList(categories);
    }

    // In ProductServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsBySellerAndStatus(String sellerId, Boolean isSold, Pageable pageable) {
        log.debug("Fetching products for seller ID: {} with isSold: {} and pagination: {}", sellerId, isSold, pageable);

        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("sellerId"), sellerId));

            if (isSold != null) {
                predicates.add(criteriaBuilder.equal(root.get("isSold"), isSold));
            }
            // if isSold is null, it fetches all (sold and not sold)

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Product> productsPage = productRepository.findAll(spec, pageable);
        return productsPage.map(productMapper::toProductDto);
    }
}
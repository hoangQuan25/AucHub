// src/main/java/com/example/productservice/entity/Product.java
package com.example.products.entity; // Use your package

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products", schema = "product_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob // Use Lob for potentially long descriptions
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private String sellerId; // Keycloak User ID of the owner


    @Enumerated(EnumType.STRING) // Store enum name as string in DB
    @Column(name = "product_condition", nullable = false)
    private ProductCondition condition; // Add condition field

    @ElementCollection(fetch = FetchType.EAGER) // Fetch eagerly for simplicity now
    @CollectionTable(name = "product_image_urls", joinColumns = @JoinColumn(name = "product_id"), schema = "product_schema")
    @Column(name = "image_url", nullable = false, length=1024) // Column in the join table
    @OrderColumn // Optional: maintains insertion order
    private List<String> imageUrls = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_categories",          // Join table name
            schema = "product_schema",            // Schema for join table
            joinColumns = @JoinColumn(name = "product_id"), // FK to products table
            inverseJoinColumns = @JoinColumn(name = "category_id") // FK to categories table
    )
    private Set<Category> categories = new HashSet<>();

    @Column(nullable = false)
    private boolean isSold = false; // Flag to indicate if the product is sold

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
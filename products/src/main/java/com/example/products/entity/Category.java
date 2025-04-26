// src/main/java/com/example/productservice/entity/Category.java
package com.example.products.entity; // Use your package

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories", schema = "product_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) // Category names should be unique? Or unique per parent? Consider constraint.
    private String name;

    // Self-referencing foreign key for hierarchy
    @Column(name = "parent_id")
    private Long parentId; // Null for top-level categories

    // Products associated with this category (mappedBy the field in Product entity)
    @ManyToMany(mappedBy = "categories")
    private Set<Product> products = new HashSet<>();

    // Add description, slug, etc. if needed later
}
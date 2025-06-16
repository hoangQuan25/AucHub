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

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "parent_id")
    private Long parentId; // Null for top-level categories

    @ManyToMany(mappedBy = "categories")
    private Set<Product> products = new HashSet<>();

}
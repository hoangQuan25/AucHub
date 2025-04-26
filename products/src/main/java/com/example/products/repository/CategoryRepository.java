package com.example.products.repository;

import com.example.products.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIdIsNull(); // Find top-level categories
    List<Category> findByParentId(Long parentId); // Find children of a category
}

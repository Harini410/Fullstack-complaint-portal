package com.example.complaintbackend.service;

import com.example.complaintbackend.dto.category.CategoryRequest;
import com.example.complaintbackend.dto.category.CategoryResponse;
import com.example.complaintbackend.entity.Category;
import com.example.complaintbackend.exception.DuplicateResourceException;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories from database");
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    @Transactional
    public Category getOrCreateCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "General";
        }
        final String finalName = name.trim();
        return categoryRepository.findByNameIgnoreCase(finalName)
                .orElseGet(() -> {
                    log.info("Creating new category on demand: {}", finalName);
                    return categoryRepository.save(new Category(finalName, finalName + " issues"));
                });
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating new category: {}", request.getName());
        if (categoryRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new DuplicateResourceException("Category '" + request.getName() + "' already exists");
        }

        Category category = new Category(request.getName().trim(), request.getDescription());
        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Deleting category ID: {}", id);
        Category category = getCategoryEntity(id);
        categoryRepository.delete(category);
    }
}

package com.example.complaintbackend.service;

import com.example.complaintbackend.dto.category.CategoryRequest;
import com.example.complaintbackend.dto.category.CategoryResponse;
import com.example.complaintbackend.entity.Category;
import com.example.complaintbackend.exception.DuplicateResourceException;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category electricityCategory;
    private Category waterCategory;

    @BeforeEach
    void setUp() {
        electricityCategory = new Category("Electricity", "Issues related to power and electrical grid");
        electricityCategory.setId(1L);

        waterCategory = new Category("Water", "Water distribution and pipe leakage");
        waterCategory.setId(2L);
    }

    @Test
    @DisplayName("getAllCategories() should return list of CategoryResponses")
    void testGetAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(electricityCategory, waterCategory));

        List<CategoryResponse> categories = categoryService.getAllCategories();

        assertThat(categories).hasSize(2);
        assertThat(categories.get(0).getName()).isEqualTo("Electricity");
        assertThat(categories.get(1).getName()).isEqualTo("Water");
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getCategoryEntity() should return category when ID exists")
    void testGetCategoryEntity_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electricityCategory));

        Category result = categoryService.getCategoryEntity(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Electricity");
    }

    @Test
    @DisplayName("getCategoryEntity() should throw ResourceNotFoundException when ID does not exist")
    void testGetCategoryEntity_NotFound_ThrowsException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryEntity(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found with id : '999'");
    }

    @Test
    @DisplayName("getOrCreateCategory() should return existing category when found by name ignore case")
    void testGetOrCreateCategory_ExistingFound() {
        when(categoryRepository.findByNameIgnoreCase("electricity")).thenReturn(Optional.of(electricityCategory));

        Category result = categoryService.getOrCreateCategory("electricity");

        assertThat(result).isEqualTo(electricityCategory);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateCategory() should create and persist new category when not found")
    void testGetOrCreateCategory_CreateNew() {
        Category roads = new Category("Roads", "Roads issues");
        roads.setId(3L);

        when(categoryRepository.findByNameIgnoreCase("Roads")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(roads);

        Category result = categoryService.getOrCreateCategory("Roads");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Roads");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("getOrCreateCategory() should default to 'General' when name is null or whitespace")
    void testGetOrCreateCategory_NullOrBlank_DefaultsToGeneral() {
        Category general = new Category("General", "General issues");
        when(categoryRepository.findByNameIgnoreCase("General")).thenReturn(Optional.of(general));

        Category resultNull = categoryService.getOrCreateCategory(null);
        Category resultBlank = categoryService.getOrCreateCategory("   ");

        assertThat(resultNull.getName()).isEqualTo("General");
        assertThat(resultBlank.getName()).isEqualTo("General");
    }

    @Test
    @DisplayName("createCategory() should save and return CategoryResponse when unique")
    void testCreateCategory_Success() {
        CategoryRequest request = new CategoryRequest("Sanitation", "Waste collection and garbage cleanup");
        Category sanitation = new Category("Sanitation", "Waste collection and garbage cleanup");
        sanitation.setId(4L);

        when(categoryRepository.existsByNameIgnoreCase("Sanitation")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(sanitation);

        CategoryResponse response = categoryService.createCategory(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Sanitation");
        assertThat(response.getDescription()).contains("Waste collection");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory() should throw DuplicateResourceException when category name exists")
    void testCreateCategory_Duplicate_ThrowsException() {
        CategoryRequest request = new CategoryRequest("Electricity", "Duplicate category");

        when(categoryRepository.existsByNameIgnoreCase("Electricity")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Category 'Electricity' already exists");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteCategory() should delete category entity when found")
    void testDeleteCategory_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(electricityCategory));

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(electricityCategory);
    }
}

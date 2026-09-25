package com.example.complaintbackend.controller;

import com.example.complaintbackend.dto.category.CategoryRequest;
import com.example.complaintbackend.dto.category.CategoryResponse;
import com.example.complaintbackend.entity.Category;
import com.example.complaintbackend.security.JwtAuthenticationEntryPoint;
import com.example.complaintbackend.security.JwtAuthenticationFilter;
import com.example.complaintbackend.security.JwtService;
import com.example.complaintbackend.service.AuthService;
import com.example.complaintbackend.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/categories - Returns list of categories")
    void testGetAllCategories() throws Exception {
        Category c1 = new Category("Electricity", "Power grid issues");
        c1.setId(1L);
        Category c2 = new Category("Water", "Water supply issues");
        c2.setId(2L);

        when(categoryService.getAllCategories()).thenReturn(List.of(new CategoryResponse(c1), new CategoryResponse(c2)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Electricity"))
                .andExpect(jsonPath("$[1].name").value("Water"));
    }

    @Test
    @DisplayName("POST /api/categories - Valid category creates category and returns 201 Created")
    void testCreateCategory_Success() throws Exception {
        CategoryRequest request = new CategoryRequest("Sanitation", "Garbage collection and sewer repairs");
        Category saved = new Category("Sanitation", "Garbage collection and sewer repairs");
        saved.setId(3L);

        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(new CategoryResponse(saved));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Sanitation"));
    }

    @Test
    @DisplayName("POST /api/categories - Blank name returns 400 Bad Request")
    void testCreateCategory_BlankName_ReturnsBadRequest() throws Exception {
        CategoryRequest request = new CategoryRequest("", "Empty name description");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - Returns 204 No Content")
    void testDeleteCategory() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
    }
}

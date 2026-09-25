package com.example.complaintbackend.dto.complaint;

import com.example.complaintbackend.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ComplaintCreateRequest {

    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private String category; // Category name (e.g. "General", "Electricity")
    private Long categoryId; // Optional category ID

    private Priority priority = Priority.MEDIUM; // Defaults to MEDIUM
    private String status; // Optional status for backward compatibility

    public ComplaintCreateRequest() {}

    public ComplaintCreateRequest(String title, String description, String category, Priority priority) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority != null ? priority : Priority.MEDIUM;
    }

    public String getTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        if (description != null) {
            String clean = description.trim();
            return clean.length() > 50 ? clean.substring(0, 47) + "..." : clean;
        }
        return "Complaint";
    }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public Priority getPriority() { return priority != null ? priority : Priority.MEDIUM; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

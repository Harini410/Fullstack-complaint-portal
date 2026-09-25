package com.example.complaintbackend.dto.complaint;

import com.example.complaintbackend.entity.Complaint;
import java.time.LocalDateTime;

public class ComplaintResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private Long categoryId;
    private String status;
    private String priority;
    private Long createdById;
    private String createdByName;
    private Long assignedToId;
    private String assignedToName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int commentCount;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private String deletedByName;
    private String deletedByRole;
    private String deleteReason;

    public ComplaintResponse() {}

    public ComplaintResponse(Complaint complaint) {
        if (complaint != null) {
            this.id = complaint.getId();
            this.title = complaint.getTitle();
            this.description = complaint.getDescription();
            if (complaint.getCategory() != null) {
                this.category = complaint.getCategory().getName();
                this.categoryId = complaint.getCategory().getId();
            } else {
                this.category = "General";
            }
            this.status = complaint.getStatus() != null ? complaint.getStatus().name() : "OPEN";
            this.priority = complaint.getPriority() != null ? complaint.getPriority().name() : "MEDIUM";

            if (complaint.getCreatedBy() != null) {
                this.createdById = complaint.getCreatedBy().getId();
                this.createdByName = complaint.getCreatedBy().getFullName() != null ?
                        complaint.getCreatedBy().getFullName() : complaint.getCreatedBy().getUsername();
            }

            if (complaint.getAssignedTo() != null) {
                this.assignedToId = complaint.getAssignedTo().getId();
                this.assignedToName = complaint.getAssignedTo().getFullName() != null ?
                        complaint.getAssignedTo().getFullName() : complaint.getAssignedTo().getUsername();
            }

            this.createdAt = complaint.getCreatedAt();
            this.updatedAt = complaint.getUpdatedAt();
            this.commentCount = complaint.getComments() != null ? complaint.getComments().size() : 0;

            this.deleted = complaint.isDeleted();
            this.deletedAt = complaint.getDeletedAt();
            this.deleteReason = complaint.getDeleteReason();
            if (complaint.getDeletedBy() != null) {
                this.deletedByName = complaint.getDeletedBy().getFullName() != null ?
                        complaint.getDeletedBy().getFullName() : complaint.getDeletedBy().getUsername();
                this.deletedByRole = complaint.getDeletedBy().getRole() != null ?
                        complaint.getDeletedBy().getRole().name() : null;
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getDeletedByName() { return deletedByName; }
    public void setDeletedByName(String deletedByName) { this.deletedByName = deletedByName; }

    public String getDeletedByRole() { return deletedByRole; }
    public void setDeletedByRole(String deletedByRole) { this.deletedByRole = deletedByRole; }

    public String getDeleteReason() { return deleteReason; }
    public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
}

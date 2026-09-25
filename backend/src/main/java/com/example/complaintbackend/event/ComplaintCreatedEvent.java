package com.example.complaintbackend.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ComplaintCreatedEvent implements Serializable {

    private Long complaintId;
    private String title;
    private Long createdById;
    private String createdByName;
    private String category;
    private String priority;
    private LocalDateTime timestamp;

    public ComplaintCreatedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public ComplaintCreatedEvent(Long complaintId, String title, Long createdById,
                                 String createdByName, String category, String priority) {
        this.complaintId = complaintId;
        this.title = title;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.category = category;
        this.priority = priority;
        this.timestamp = LocalDateTime.now();
    }

    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

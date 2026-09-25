package com.example.complaintbackend.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ComplaintAssignedEvent implements Serializable {

    private Long complaintId;
    private String title;
    private Long assignedToId;
    private String assignedToName;
    private Long assignedById;
    private String assignedByName;
    private LocalDateTime timestamp;

    public ComplaintAssignedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public ComplaintAssignedEvent(Long complaintId, String title, Long assignedToId,
                                  String assignedToName, Long assignedById, String assignedByName) {
        this.complaintId = complaintId;
        this.title = title;
        this.assignedToId = assignedToId;
        this.assignedToName = assignedToName;
        this.assignedById = assignedById;
        this.assignedByName = assignedByName;
        this.timestamp = LocalDateTime.now();
    }

    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public Long getAssignedById() { return assignedById; }
    public void setAssignedById(Long assignedById) { this.assignedById = assignedById; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

package com.example.complaintbackend.event;

import com.example.complaintbackend.entity.ComplaintStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public class ComplaintStatusChangedEvent implements Serializable {

    private Long complaintId;
    private String title;
    private ComplaintStatus previousStatus;
    private ComplaintStatus newStatus;
    private Long changedById;
    private String changedByName;
    private String remarks;
    private LocalDateTime timestamp;

    public ComplaintStatusChangedEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public ComplaintStatusChangedEvent(Long complaintId, String title, ComplaintStatus previousStatus,
                                       ComplaintStatus newStatus, Long changedById, String changedByName, String remarks) {
        this.complaintId = complaintId;
        this.title = title;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedById = changedById;
        this.changedByName = changedByName;
        this.remarks = remarks;
        this.timestamp = LocalDateTime.now();
    }

    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ComplaintStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(ComplaintStatus previousStatus) { this.previousStatus = previousStatus; }

    public ComplaintStatus getNewStatus() { return newStatus; }
    public void setNewStatus(ComplaintStatus newStatus) { this.newStatus = newStatus; }

    public Long getChangedById() { return changedById; }
    public void setChangedById(Long changedById) { this.changedById = changedById; }

    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

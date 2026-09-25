package com.example.complaintbackend.dto.complaint;

import com.example.complaintbackend.entity.ComplaintHistory;
import com.example.complaintbackend.entity.ComplaintStatus;
import java.time.LocalDateTime;

public class ComplaintHistoryResponse {

    private Long id;
    private String action;
    private ComplaintStatus previousStatus;
    private ComplaintStatus newStatus;
    private Long changedById;
    private String changedByName;
    private String remarks;
    private LocalDateTime timestamp;

    public ComplaintHistoryResponse() {}

    public ComplaintHistoryResponse(ComplaintHistory history) {
        if (history != null) {
            this.id = history.getId();
            this.action = history.getAction();
            this.previousStatus = history.getPreviousStatus();
            this.newStatus = history.getNewStatus();
            if (history.getChangedBy() != null) {
                this.changedById = history.getChangedBy().getId();
                this.changedByName = history.getChangedBy().getFullName() != null ?
                        history.getChangedBy().getFullName() : history.getChangedBy().getUsername();
            }
            this.remarks = history.getRemarks();
            this.timestamp = history.getTimestamp();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

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

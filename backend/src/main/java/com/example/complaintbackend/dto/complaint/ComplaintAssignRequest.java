package com.example.complaintbackend.dto.complaint;

import jakarta.validation.constraints.NotNull;

public class ComplaintAssignRequest {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    public ComplaintAssignRequest() {}

    public ComplaintAssignRequest(Long agentId) {
        this.agentId = agentId;
    }

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
}

package com.example.complaintbackend.controller;

import com.example.complaintbackend.dto.complaint.ComplaintCreateRequest;
import com.example.complaintbackend.dto.complaint.ComplaintResponse;
import com.example.complaintbackend.entity.Complaint;
import com.example.complaintbackend.entity.Priority;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.security.JwtAuthenticationEntryPoint;
import com.example.complaintbackend.security.JwtAuthenticationFilter;
import com.example.complaintbackend.security.JwtService;
import com.example.complaintbackend.service.AuthService;
import com.example.complaintbackend.service.ComplaintService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ComplaintController.class)
@AutoConfigureMockMvc(addFilters = false)
class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/complaints - Returns unpaged list for React UI compatibility")
    void testGetComplaints_ReturnsList() throws Exception {
        Complaint c = new Complaint("Electricity", "Street light issue", "Pending");
        c.setId(1L);
        ComplaintResponse response = new ComplaintResponse(c);

        when(complaintService.getAllComplaintsList()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Street light issue"));
    }

    @Test
    @DisplayName("GET /api/complaints/{id} - Existing ID returns 200 OK")
    void testGetComplaintById_Success() throws Exception {
        Complaint c = new Complaint("Water", "Pipe leakage", "In Progress");
        c.setId(2L);
        ComplaintResponse response = new ComplaintResponse(c);

        when(complaintService.getComplaintById(2L)).thenReturn(response);

        mockMvc.perform(get("/api/complaints/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.description").value("Pipe leakage"));
    }

    @Test
    @DisplayName("GET /api/complaints/{id} - Non-existent ID returns 404 Not Found")
    void testGetComplaintById_NotFound() throws Exception {
        when(complaintService.getComplaintById(99L))
                .thenThrow(new ResourceNotFoundException("Complaint", "id", 99L));

        mockMvc.perform(get("/api/complaints/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/complaints - Valid payload creates complaint and returns 201 Created")
    void testCreateComplaint_Valid_ReturnsCreated() throws Exception {
        ComplaintCreateRequest request = new ComplaintCreateRequest(
                "Power Outage", "No power on 5th street", "Electricity", Priority.HIGH
        );
        Complaint c = new Complaint("Electricity", "No power on 5th street", "Pending");
        c.setId(10L);
        ComplaintResponse response = new ComplaintResponse(c);

        when(complaintService.createComplaint(any(ComplaintCreateRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("POST /api/complaints - Blank description returns 400 Bad Request with validation errors")
    void testCreateComplaint_BlankDescription_ReturnsBadRequest() throws Exception {
        ComplaintCreateRequest request = new ComplaintCreateRequest();
        request.setDescription(""); // blank violates @NotBlank

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.description").exists());
    }

    @Test
    @DisplayName("DELETE /api/complaints/{id} - Returns 204 No Content")
    void testDeleteComplaint_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/complaints/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/complaints/{id}/status - Updates status and returns 200 OK")
    void testUpdateStatus_ReturnsUpdated() throws Exception {
        com.example.complaintbackend.dto.complaint.ComplaintStatusUpdateRequest request =
                new com.example.complaintbackend.dto.complaint.ComplaintStatusUpdateRequest(
                        com.example.complaintbackend.entity.ComplaintStatus.RESOLVED, "Issue repaired"
                );
        Complaint c = new Complaint("Water", "Pipe leakage", "Resolved");
        c.setId(10L);
        when(complaintService.updateStatus(eq(10L), any(), any())).thenReturn(new ComplaintResponse(c));

        mockMvc.perform(patch("/api/complaints/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("PATCH /api/complaints/{id}/assign - Assigns agent and returns 200 OK")
    void testAssignComplaint_ReturnsAssigned() throws Exception {
        com.example.complaintbackend.dto.complaint.ComplaintAssignRequest request =
                new com.example.complaintbackend.dto.complaint.ComplaintAssignRequest(5L);
        Complaint c = new Complaint("Water", "Pipe leakage", "Assigned");
        c.setId(10L);
        when(complaintService.assignComplaint(eq(10L), eq(5L), any())).thenReturn(new ComplaintResponse(c));

        mockMvc.perform(patch("/api/complaints/10/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /api/complaints/{id}/history - Returns history timeline")
    void testGetComplaintHistory() throws Exception {
        when(complaintService.getComplaintHistory(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/complaints/10/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

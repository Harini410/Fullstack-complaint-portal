package com.example.complaintbackend.controller;

import com.example.complaintbackend.dto.auth.UserResponse;
import com.example.complaintbackend.dto.complaint.ComplaintResponse;
import com.example.complaintbackend.entity.Complaint;
import com.example.complaintbackend.entity.Role;
import com.example.complaintbackend.entity.User;
import com.example.complaintbackend.security.JwtAuthenticationEntryPoint;
import com.example.complaintbackend.security.JwtAuthenticationFilter;
import com.example.complaintbackend.security.JwtService;
import com.example.complaintbackend.service.AuthService;
import com.example.complaintbackend.service.ComplaintService;
import com.example.complaintbackend.service.UserService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComplaintService complaintService;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/admin/statistics - Returns system aggregate metrics")
    void testGetStatistics() throws Exception {
        Map<String, Object> stats = Map.of(
                "totalComplaints", 12L,
                "openComplaints", 5L,
                "resolvedComplaints", 4L,
                "criticalPriority", 2L
        );
        when(complaintService.getComplaintStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalComplaints").value(12))
                .andExpect(jsonPath("$.openComplaints").value(5))
                .andExpect(jsonPath("$.resolvedComplaints").value(4));
    }

    @Test
    @DisplayName("GET /api/admin/users - Returns list of registered users")
    void testGetAllUsers() throws Exception {
        User user = new User("harini", "h@example.com", "pass", "Harini", Role.ROLE_USER);
        user.setId(1L);
        when(userService.getAllUsers()).thenReturn(List.of(new UserResponse(user)));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("harini"));
    }

    @Test
    @DisplayName("GET /api/admin/support-agents - Returns list of support agents")
    void testGetSupportAgents() throws Exception {
        User agent = new User("sarah", "sarah@example.com", "pass", "Sarah Jenkins", Role.ROLE_SUPPORT_AGENT);
        agent.setId(2L);
        when(userService.getSupportAgents()).thenReturn(List.of(new UserResponse(agent)));

        mockMvc.perform(get("/api/admin/support-agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].username").value("sarah"))
                .andExpect(jsonPath("$[0].role").value("ROLE_SUPPORT_AGENT"));
    }

    @Test
    @DisplayName("GET /api/admin/deleted-complaints - Returns deleted complaints audit list")
    void testGetDeletedComplaints() throws Exception {
        Complaint c = new Complaint("Water", "Leaking pipe", "Resolved");
        c.setId(5L);
        c.setDeleted(true);
        c.setDeleteReason("Fixed independently");
        when(complaintService.getDeletedComplaintsList()).thenReturn(List.of(new ComplaintResponse(c)));

        mockMvc.perform(get("/api/admin/deleted-complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].deleted").value(true));
    }

    @Test
    @DisplayName("PATCH /api/admin/complaints/{id}/restore - Restores deleted complaint")
    void testRestoreComplaint() throws Exception {
        Complaint c = new Complaint("Water", "Leaking pipe", "Open");
        c.setId(5L);
        c.setDeleted(false);
        when(authService.getCurrentAuthenticatedUser()).thenReturn(new User("admin", "a@example.com", "pass", "Admin", Role.ROLE_ADMIN));
        when(complaintService.restoreComplaint(eq(5L), any())).thenReturn(new ComplaintResponse(c));

        mockMvc.perform(patch("/api/admin/complaints/5/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/role - Updates user role")
    void testUpdateUserRole() throws Exception {
        User user = new User("agent_new", "agent@example.com", "pass", "New Agent", Role.ROLE_SUPPORT_AGENT);
        user.setId(7L);
        when(userService.updateUserRole(7L, Role.ROLE_SUPPORT_AGENT)).thenReturn(new UserResponse(user));

        mockMvc.perform(patch("/api/admin/users/7/role").param("role", "ROLE_SUPPORT_AGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_SUPPORT_AGENT"));
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/status - Updates user active status")
    void testUpdateUserStatus() throws Exception {
        User user = new User("locked_user", "locked@example.com", "pass", "Locked User", Role.ROLE_USER);
        user.setId(8L);
        user.setEnabled(false);
        when(userService.updateUserStatus(8L, false)).thenReturn(new UserResponse(user));

        mockMvc.perform(patch("/api/admin/users/8/status").param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}

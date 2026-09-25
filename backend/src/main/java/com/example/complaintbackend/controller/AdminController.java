package com.example.complaintbackend.controller;

import com.example.complaintbackend.dto.auth.UserResponse;
import com.example.complaintbackend.entity.Role;
import com.example.complaintbackend.service.ComplaintService;
import com.example.complaintbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Admin", description = "Administration operations: metrics, user management, and role assignments")
public class AdminController {

    private final ComplaintService complaintService;
    private final UserService userService;
    private final com.example.complaintbackend.service.AuthService authService;

    public AdminController(ComplaintService complaintService, UserService userService, com.example.complaintbackend.service.AuthService authService) {
        this.complaintService = complaintService;
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get complaint metrics", description = "Retrieves aggregated counts of complaints by status and priority for the dashboard")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(complaintService.getComplaintStatistics());
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered users", description = "Retrieves list of all registered portal users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/support-agents")
    @Operation(summary = "List support agents", description = "Retrieves list of users with ROLE_SUPPORT_AGENT available for assignments")
    public ResponseEntity<List<UserResponse>> getSupportAgents() {
        return ResponseEntity.ok(userService.getSupportAgents());
    }

    @GetMapping("/deleted-complaints")
    @Operation(summary = "Get deleted complaints audit trail", description = "Retrieves all deleted complaints with audit details. Admin only.")
    public ResponseEntity<List<com.example.complaintbackend.dto.complaint.ComplaintResponse>> getDeletedComplaints() {
        return ResponseEntity.ok(complaintService.getDeletedComplaintsList());
    }

    @PatchMapping("/complaints/{id}/restore")
    @Operation(summary = "Restore deleted complaint", description = "Restores a soft-deleted complaint. Admin only.")
    public ResponseEntity<com.example.complaintbackend.dto.complaint.ComplaintResponse> restoreComplaint(@PathVariable Long id) {
        com.example.complaintbackend.entity.User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(complaintService.restoreComplaint(id, currentUser));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Change user role", description = "Updates a user's role to ROLE_USER, ROLE_ADMIN, or ROLE_SUPPORT_AGENT")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestParam Role role
    ) {
        return ResponseEntity.ok(userService.updateUserRole(id, role));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Change user active status", description = "Enables or disables a user account")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled
    ) {
        return ResponseEntity.ok(userService.updateUserStatus(id, enabled));
    }
}

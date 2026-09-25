package com.example.complaintbackend.controller;

import com.example.complaintbackend.dto.common.PagedResponse;
import com.example.complaintbackend.dto.complaint.*;
import com.example.complaintbackend.entity.ComplaintStatus;
import com.example.complaintbackend.entity.Priority;
import com.example.complaintbackend.entity.User;
import com.example.complaintbackend.service.AuthService;
import com.example.complaintbackend.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "Complaints", description = "Complaint lifecycle operations: create, read, update, status change, and assign")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final AuthService authService;

    public ComplaintController(ComplaintService complaintService, AuthService authService) {
        this.complaintService = complaintService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Get complaints (array or paged)", description = "Retrieves all complaints. If page/size are provided, returns PagedResponse; otherwise returns a JSON array for backwards compatibility with the UI.")
    public ResponseEntity<?> getComplaints(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search
    ) {
        if (page == null && size == null) {
            // Default unpaged array list for backward compatibility with frontend App.js
            List<ComplaintResponse> list = complaintService.getAllComplaintsList();
            return ResponseEntity.ok(list);
        }

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.trim().isEmpty()) {
            String[] parts = sort.split(",");
            sortObj = Sort.by(
                    parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC,
                    parts[0]
            );
        }

        Pageable pageable = PageRequest.of(pageNum, pageSize, sortObj);
        PagedResponse<ComplaintResponse> result = complaintService.getAllComplaints(status, priority, categoryId, search, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get complaint by ID", description = "Retrieves details of a specific complaint")
    public ResponseEntity<ComplaintResponse> getComplaintById(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    @PostMapping
    @Operation(summary = "Create complaint", description = "Creates a new complaint. Can be authenticated with Bearer token or submitted as a guest.")
    public ResponseEntity<ComplaintResponse> createComplaint(@Valid @RequestBody ComplaintCreateRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        ComplaintResponse created = complaintService.createComplaint(request, currentUser);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update complaint details", description = "Updates title, description, category, priority, or status")
    public ResponseEntity<ComplaintResponse> updateComplaint(
            @PathVariable Long id,
            @RequestBody ComplaintUpdateRequest request
    ) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(complaintService.updateComplaint(id, request, currentUser));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Update complaint status", description = "Updates complaint status (OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED) and records history. Restricted to ADMIN and SUPPORT_AGENT.")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintStatusUpdateRequest request
    ) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(complaintService.updateStatus(id, request, currentUser));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Assign support agent", description = "Assigns a complaint to a support agent. Accessible by ADMIN and SUPPORT_AGENT.")
    public ResponseEntity<ComplaintResponse> assignComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintAssignRequest request
    ) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(complaintService.assignComplaint(id, request.getAgentId(), currentUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete complaint", description = "Deletes a complaint. Citizens can delete their own; Support Agents and Admins can delete assigned/all complaints. Audit trail preserved.")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        complaintService.deleteComplaint(id, currentUser, reason);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get complaint audit history", description = "Retrieves timeline of changes for a specific complaint")
    public ResponseEntity<List<ComplaintHistoryResponse>> getComplaintHistory(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintHistory(id));
    }
}

package com.example.complaintbackend.service;

import com.example.complaintbackend.dto.common.PagedResponse;
import com.example.complaintbackend.dto.complaint.*;
import com.example.complaintbackend.entity.*;
import com.example.complaintbackend.event.ComplaintAssignedEvent;
import com.example.complaintbackend.event.ComplaintCreatedEvent;
import com.example.complaintbackend.event.ComplaintStatusChangedEvent;
import com.example.complaintbackend.exception.BadRequestException;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.exception.UnauthorizedException;
import com.example.complaintbackend.repository.ComplaintHistoryRepository;
import com.example.complaintbackend.repository.ComplaintRepository;
import com.example.complaintbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository historyRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final ComplaintEventPublisher eventPublisher;
    private final NotificationService notificationService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintHistoryRepository historyRepository,
            CategoryService categoryService,
            UserRepository userRepository,
            ComplaintEventPublisher eventPublisher,
            NotificationService notificationService
    ) {
        this.complaintRepository = complaintRepository;
        this.historyRepository = historyRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getAllComplaints(
            ComplaintStatus status,
            Priority priority,
            Long categoryId,
            String search,
            Pageable pageable
    ) {
        log.info("Querying complaints with filters - status: {}, priority: {}, category: {}, search: {}",
                status, priority, categoryId, search);

        Page<Complaint> page = complaintRepository.findWithFilters(status, priority, categoryId, search, pageable);
        List<ComplaintResponse> content = page.getContent().stream()
                .map(ComplaintResponse::new)
                .collect(Collectors.toList());

        return PagedResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaintsList() {
        return complaintRepository.findByIsDeletedFalseOrderByIdDesc().stream()
                .map(ComplaintResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getDeletedComplaintsList() {
        return complaintRepository.findByIsDeletedTrueOrderByIdDesc().stream()
                .map(ComplaintResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ComplaintResponse> getComplaintsForUser(
            Long userId,
            ComplaintStatus status,
            Priority priority,
            Pageable pageable
    ) {
        Page<Complaint> page = complaintRepository.findByUserIdWithFilters(userId, status, priority, pageable);
        List<ComplaintResponse> content = page.getContent().stream()
                .map(ComplaintResponse::new)
                .collect(Collectors.toList());

        return PagedResponse.from(page, content);
    }

    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id) {
        Complaint complaint = getComplaintEntity(id);
        return new ComplaintResponse(complaint);
    }

    @Transactional(readOnly = true)
    public Complaint getComplaintEntity(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ComplaintResponse createComplaint(ComplaintCreateRequest request, User currentUser) {
        log.info("Creating complaint: '{}'", request.getTitle());

        // Resolve Category
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryService.getCategoryEntity(request.getCategoryId());
        } else if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            category = categoryService.getOrCreateCategory(request.getCategory());
        } else {
            category = categoryService.getOrCreateCategory("General");
        }

        // Determine creator (if null, look for demo user or fallback)
        User creator = currentUser;
        if (creator == null) {
            creator = userRepository.findByUsername("demo_user")
                    .or(() -> userRepository.findAll().stream().findFirst())
                    .orElse(null);
        }

        // Parse initial status (default OPEN)
        ComplaintStatus initialStatus = ComplaintStatus.OPEN;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                String normalized = request.getStatus().trim().toUpperCase().replace(" ", "_");
                if ("PENDING".equals(normalized)) {
                    initialStatus = ComplaintStatus.OPEN;
                } else {
                    initialStatus = ComplaintStatus.valueOf(normalized);
                }
            } catch (IllegalArgumentException ignored) {
                initialStatus = ComplaintStatus.OPEN;
            }
        }

        Complaint complaint = new Complaint(
                request.getTitle(),
                request.getDescription(),
                category,
                request.getPriority(),
                initialStatus,
                creator
        );

        Complaint savedComplaint = complaintRepository.save(complaint);

        // Record initial history
        ComplaintHistory history = new ComplaintHistory(
                savedComplaint,
                "CREATED",
                null,
                savedComplaint.getStatus(),
                creator,
                "Complaint logged in system"
        );
        historyRepository.save(history);

        // Dispatch domain event via Kafka producer
        eventPublisher.publishComplaintCreated(new ComplaintCreatedEvent(
                savedComplaint.getId(),
                savedComplaint.getTitle(),
                creator != null ? creator.getId() : null,
                creator != null ? creator.getUsername() : "Guest",
                category != null ? category.getName() : "General",
                savedComplaint.getPriority().name()
        ));

        return new ComplaintResponse(savedComplaint);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ComplaintResponse updateComplaint(Long id, ComplaintUpdateRequest request, User currentUser) {
        Complaint complaint = getComplaintEntity(id);

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            complaint.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            complaint.setDescription(request.getDescription().trim());
        }
        if (request.getCategoryId() != null) {
            complaint.setCategory(categoryService.getCategoryEntity(request.getCategoryId()));
        } else if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            complaint.setCategory(categoryService.getOrCreateCategory(request.getCategory()));
        }

        if (request.getPriority() != null && complaint.getPriority() != request.getPriority()) {
            Priority oldPriority = complaint.getPriority();
            complaint.setPriority(request.getPriority());
            historyRepository.save(new ComplaintHistory(
                    complaint, "PRIORITY_UPDATED", complaint.getStatus(), complaint.getStatus(),
                    currentUser, "Priority updated from " + oldPriority + " to " + request.getPriority()
            ));
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                String norm = request.getStatus().trim().toUpperCase().replace(" ", "_");
                ComplaintStatus newStatus = norm.equals("PENDING") ? ComplaintStatus.OPEN : ComplaintStatus.valueOf(norm);
                if (complaint.getStatus() != newStatus) {
                    ComplaintStatus oldStatus = complaint.getStatus();
                    complaint.setStatus(newStatus);
                    historyRepository.save(new ComplaintHistory(
                            complaint, "STATUS_UPDATED", oldStatus, newStatus,
                            currentUser, "Status updated from " + oldStatus + " to " + newStatus
                    ));
                    eventPublisher.publishComplaintStatusChanged(new ComplaintStatusChangedEvent(
                            complaint.getId(), complaint.getTitle(), oldStatus, newStatus,
                            currentUser != null ? currentUser.getId() : null,
                            currentUser != null ? currentUser.getUsername() : "System",
                            "Status updated"
                    ));
                }
            } catch (IllegalArgumentException ignored) {}
        }

        Complaint saved = complaintRepository.save(complaint);
        return new ComplaintResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ComplaintResponse updateStatus(Long id, ComplaintStatusUpdateRequest request, User currentUser) {
        Complaint complaint = getComplaintEntity(id);
        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return new ComplaintResponse(complaint);
        }

        // Validate business status transition
        if (oldStatus == ComplaintStatus.CLOSED) {
            throw new BadRequestException("Closed complaints cannot change status without reopening");
        }

        complaint.setStatus(newStatus);
        Complaint saved = complaintRepository.save(complaint);

        String remarks = request.getRemarks() != null ? request.getRemarks() : "Status changed to " + newStatus;
        historyRepository.save(new ComplaintHistory(
                saved, "STATUS_UPDATED", oldStatus, newStatus, currentUser, remarks
        ));

        // Notify creator
        if (saved.getCreatedBy() != null) {
            notificationService.createNotification(
                    saved.getCreatedBy(),
                    saved.getId(),
                    "Your complaint #" + saved.getId() + " is now " + newStatus,
                    "STATUS_UPDATE"
            );
        }

        // Publish Kafka event
        eventPublisher.publishComplaintStatusChanged(new ComplaintStatusChangedEvent(
                saved.getId(), saved.getTitle(), oldStatus, newStatus,
                currentUser != null ? currentUser.getId() : null,
                currentUser != null ? currentUser.getUsername() : "System",
                remarks
        ));

        return new ComplaintResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ComplaintResponse assignComplaint(Long id, Long agentId, User currentUser) {
        Complaint complaint = getComplaintEntity(id);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User (Agent)", "id", agentId));

        if (agent.getRole() != Role.ROLE_SUPPORT_AGENT && agent.getRole() != Role.ROLE_ADMIN) {
            throw new BadRequestException("Assigned user must have ROLE_SUPPORT_AGENT or ROLE_ADMIN");
        }

        complaint.setAssignedTo(agent);
        if (complaint.getStatus() == ComplaintStatus.OPEN) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
        }

        Complaint saved = complaintRepository.save(complaint);

        historyRepository.save(new ComplaintHistory(
                saved, "ASSIGNED", complaint.getStatus(), saved.getStatus(), currentUser,
                "Assigned to support agent: " + agent.getFullName()
        ));

        // Notify assigned agent
        notificationService.createNotification(
                agent,
                saved.getId(),
                "Complaint #" + saved.getId() + " has been assigned to you",
                "COMPLAINT_ASSIGNED"
        );

        // Notify complaint creator
        if (saved.getCreatedBy() != null) {
            notificationService.createNotification(
                    saved.getCreatedBy(),
                    saved.getId(),
                    "Complaint #" + saved.getId() + " was assigned to " + agent.getFullName(),
                    "COMPLAINT_ASSIGNED"
            );
        }

        // Publish Kafka event
        eventPublisher.publishComplaintAssigned(new ComplaintAssignedEvent(
                saved.getId(), saved.getTitle(), agent.getId(), agent.getUsername(),
                currentUser != null ? currentUser.getId() : null,
                currentUser != null ? currentUser.getUsername() : "System"
        ));

        return new ComplaintResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void deleteComplaint(Long id, User currentUser, String reason) {
        Complaint complaint = getComplaintEntity(id);

        if (currentUser != null && currentUser.getRole() == Role.ROLE_USER) {
            if (complaint.getCreatedBy() != null && !complaint.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("You are not authorized to delete another user's complaint");
            }
        }

        log.info("Soft-deleting complaint ID: {} by user: {}", id, currentUser != null ? currentUser.getUsername() : "anonymous");
        complaint.setDeleted(true);
        complaint.setDeletedAt(LocalDateTime.now());
        complaint.setDeletedBy(currentUser);
        String finalReason = reason != null && !reason.trim().isEmpty()
                ? reason.trim()
                : "Deleted by " + (currentUser != null ? currentUser.getFullName() + " (" + currentUser.getRole() + ")" : "User");
        complaint.setDeleteReason(finalReason);

        historyRepository.save(new ComplaintHistory(
                complaint, "DELETED", complaint.getStatus(), complaint.getStatus(), currentUser, finalReason
        ));

        complaintRepository.save(complaint);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void deleteComplaint(Long id, User currentUser) {
        deleteComplaint(id, currentUser, null);
    }

    @Transactional
    @CacheEvict(value = "dashboardStats", allEntries = true)
    public ComplaintResponse restoreComplaint(Long id, User currentUser) {
        Complaint complaint = getComplaintEntity(id);
        if (!complaint.isDeleted()) {
            throw new BadRequestException("Complaint #" + id + " is not deleted");
        }

        log.info("Restoring complaint ID: {} by user: {}", id, currentUser != null ? currentUser.getUsername() : "Admin");
        complaint.setDeleted(false);
        complaint.setDeletedAt(null);
        complaint.setDeletedBy(null);
        complaint.setDeleteReason(null);

        historyRepository.save(new ComplaintHistory(
                complaint, "RESTORED", complaint.getStatus(), complaint.getStatus(), currentUser,
                "Restored by " + (currentUser != null ? currentUser.getFullName() : "Admin")
        ));

        Complaint saved = complaintRepository.save(complaint);
        return new ComplaintResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ComplaintHistoryResponse> getComplaintHistory(Long complaintId) {
        if (!complaintRepository.existsById(complaintId)) {
            throw new ResourceNotFoundException("Complaint", "id", complaintId);
        }
        return historyRepository.findByComplaintIdOrderByTimestampDesc(complaintId).stream()
                .map(ComplaintHistoryResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardStats", key = "'summary'")
    public Map<String, Object> getComplaintStatistics() {
        log.info("Calculating aggregate complaint dashboard statistics from database");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalComplaints", complaintRepository.countByIsDeletedFalse());
        stats.put("deletedComplaints", complaintRepository.countByIsDeletedTrue());
        stats.put("openComplaints", complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.OPEN));
        stats.put("assignedComplaints", complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.ASSIGNED));
        stats.put("inProgressComplaints", complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.IN_PROGRESS));
        stats.put("resolvedComplaints", complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.RESOLVED));
        stats.put("closedComplaints", complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.CLOSED));

        stats.put("lowPriority", complaintRepository.countByPriorityAndIsDeletedFalse(Priority.LOW));
        stats.put("mediumPriority", complaintRepository.countByPriorityAndIsDeletedFalse(Priority.MEDIUM));
        stats.put("highPriority", complaintRepository.countByPriorityAndIsDeletedFalse(Priority.HIGH));
        stats.put("criticalPriority", complaintRepository.countByPriorityAndIsDeletedFalse(Priority.CRITICAL));

        return stats;
    }
}

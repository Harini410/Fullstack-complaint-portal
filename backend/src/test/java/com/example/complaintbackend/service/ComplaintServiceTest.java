package com.example.complaintbackend.service;

import com.example.complaintbackend.dto.complaint.ComplaintCreateRequest;
import com.example.complaintbackend.dto.complaint.ComplaintResponse;
import com.example.complaintbackend.dto.complaint.ComplaintStatusUpdateRequest;
import com.example.complaintbackend.entity.*;
import com.example.complaintbackend.exception.BadRequestException;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.exception.UnauthorizedException;
import com.example.complaintbackend.repository.ComplaintHistoryRepository;
import com.example.complaintbackend.repository.ComplaintRepository;
import com.example.complaintbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintHistoryRepository historyRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComplaintEventPublisher eventPublisher;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ComplaintService complaintService;

    private User regularUser;
    private User supportAgent;
    private Category generalCategory;
    private Complaint sampleComplaint;

    @BeforeEach
    void setUp() {
        regularUser = new User("harini", "harini@example.com", "pass", "Harini", Role.ROLE_USER);
        regularUser.setId(10L);

        supportAgent = new User("sarah", "sarah@example.com", "pass", "Sarah Jenkins", Role.ROLE_SUPPORT_AGENT);
        supportAgent.setId(20L);

        generalCategory = new Category("Electricity", "Electricity issues");
        generalCategory.setId(1L);

        sampleComplaint = new Complaint(
                "Power outage",
                "Entire block has no power since morning",
                generalCategory,
                Priority.HIGH,
                ComplaintStatus.OPEN,
                regularUser
        );
        sampleComplaint.setId(100L);
    }

    @Test
    @DisplayName("Should successfully create a complaint, log initial history, and publish event")
    void testCreateComplaint_Success() {
        ComplaintCreateRequest request = new ComplaintCreateRequest(
                "Power outage",
                "Entire block has no power since morning",
                "Electricity",
                Priority.HIGH
        );

        when(categoryService.getOrCreateCategory("Electricity")).thenReturn(generalCategory);
        when(complaintRepository.save(any(Complaint.class))).thenReturn(sampleComplaint);

        ComplaintResponse response = complaintService.createComplaint(request, regularUser);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("Power outage");
        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getPriority()).isEqualTo("HIGH");

        verify(complaintRepository, times(1)).save(any(Complaint.class));
        verify(historyRepository, times(1)).save(any(ComplaintHistory.class));
        verify(eventPublisher, times(1)).publishComplaintCreated(any());
    }

    @Test
    @DisplayName("Should assign complaint to support agent and transition status to ASSIGNED")
    void testAssignComplaint_Success() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(userRepository.findById(20L)).thenReturn(Optional.of(supportAgent));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(sampleComplaint);

        ComplaintResponse response = complaintService.assignComplaint(100L, 20L, supportAgent);

        assertThat(response).isNotNull();
        assertThat(sampleComplaint.getAssignedTo()).isEqualTo(supportAgent);
        assertThat(sampleComplaint.getStatus()).isEqualTo(ComplaintStatus.ASSIGNED);

        verify(historyRepository, times(1)).save(any(ComplaintHistory.class));
        verify(notificationService, atLeastOnce()).createNotification(eq(supportAgent), eq(100L), anyString(), anyString());
        verify(eventPublisher, times(1)).publishComplaintAssigned(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if assigned user does not have SUPPORT_AGENT role")
    void testAssignComplaint_InvalidRole_ThrowsBadRequest() {
        User unauthorizedUser = new User("bob", "bob@example.com", "pass", "Bob", Role.ROLE_USER);
        unauthorizedUser.setId(30L);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(userRepository.findById(30L)).thenReturn(Optional.of(unauthorizedUser));

        assertThatThrownBy(() -> complaintService.assignComplaint(100L, 30L, supportAgent))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assigned user must have ROLE_SUPPORT_AGENT or ROLE_ADMIN");

        verify(complaintRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update status and record history")
    void testUpdateStatus_Success() {
        ComplaintStatusUpdateRequest request = new ComplaintStatusUpdateRequest(
                ComplaintStatus.IN_PROGRESS, "Technician on site"
        );

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(sampleComplaint);

        ComplaintResponse response = complaintService.updateStatus(100L, request, supportAgent);

        assertThat(response).isNotNull();
        assertThat(sampleComplaint.getStatus()).isEqualTo(ComplaintStatus.IN_PROGRESS);

        verify(historyRepository, times(1)).save(any(ComplaintHistory.class));
        verify(eventPublisher, times(1)).publishComplaintStatusChanged(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when trying to change status of closed complaint")
    void testUpdateStatus_ClosedComplaint_ThrowsBadRequest() {
        sampleComplaint.setStatus(ComplaintStatus.CLOSED);

        ComplaintStatusUpdateRequest request = new ComplaintStatusUpdateRequest(
                ComplaintStatus.IN_PROGRESS, "Attempting update"
        );

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThatThrownBy(() -> complaintService.updateStatus(100L, request, supportAgent))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Closed complaints cannot change status");

        verify(complaintRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete complaint successfully by owner")
    void testDeleteComplaint_Success() {
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        complaintService.deleteComplaint(100L, regularUser);

        assertThat(sampleComplaint.isDeleted()).isTrue();
        assertThat(sampleComplaint.getDeletedBy()).isEqualTo(regularUser);
        verify(complaintRepository, times(1)).save(sampleComplaint);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException if non-owner user tries to delete complaint")
    void testDeleteComplaint_Unauthorized_ThrowsException() {
        User anotherUser = new User("intruder", "intruder@example.com", "pass", "Intruder", Role.ROLE_USER);
        anotherUser.setId(999L);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThatThrownBy(() -> complaintService.deleteComplaint(100L, anotherUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not authorized to delete another user's complaint");

        verify(complaintRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should allow admin to soft-delete any complaint with custom reason")
    void testDeleteComplaint_AdminWithCustomReason_Success() {
        User adminUser = new User("admin", "admin@example.com", "pass", "System Admin", Role.ROLE_ADMIN);
        adminUser.setId(1L);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        complaintService.deleteComplaint(100L, adminUser, "Duplicate ticket logged by user");

        assertThat(sampleComplaint.isDeleted()).isTrue();
        assertThat(sampleComplaint.getDeletedBy()).isEqualTo(adminUser);
        assertThat(sampleComplaint.getDeleteReason()).isEqualTo("Duplicate ticket logged by user");
        assertThat(sampleComplaint.getDeletedAt()).isNotNull();

        verify(historyRepository, times(1)).save(any(ComplaintHistory.class));
        verify(complaintRepository, times(1)).save(sampleComplaint);
    }

    @Test
    @DisplayName("Should successfully restore soft-deleted complaint")
    void testRestoreComplaint_Success() {
        sampleComplaint.setDeleted(true);
        sampleComplaint.setDeleteReason("Temporary archive");
        User adminUser = new User("admin", "admin@example.com", "pass", "System Admin", Role.ROLE_ADMIN);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(sampleComplaint);

        ComplaintResponse response = complaintService.restoreComplaint(100L, adminUser);

        assertThat(response).isNotNull();
        assertThat(sampleComplaint.isDeleted()).isFalse();
        assertThat(sampleComplaint.getDeletedAt()).isNull();
        assertThat(sampleComplaint.getDeletedBy()).isNull();
        assertThat(sampleComplaint.getDeleteReason()).isNull();

        verify(historyRepository, times(1)).save(any(ComplaintHistory.class));
        verify(complaintRepository, times(1)).save(sampleComplaint);
    }

    @Test
    @DisplayName("Should throw BadRequestException when restoring an active (non-deleted) complaint")
    void testRestoreComplaint_NotDeleted_ThrowsBadRequest() {
        sampleComplaint.setDeleted(false);
        when(complaintRepository.findById(100L)).thenReturn(Optional.of(sampleComplaint));

        assertThatThrownBy(() -> complaintService.restoreComplaint(100L, regularUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Complaint #100 is not deleted");

        verify(complaintRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when complaint does not exist")
    void testGetComplaint_NotFound_ThrowsException() {
        when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintService.getComplaintById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Complaint not found with id : '999'");
    }

    @Test
    @DisplayName("Should calculate aggregate complaint statistics correctly")
    void testGetComplaintStatistics() {
        when(complaintRepository.countByIsDeletedFalse()).thenReturn(10L);
        when(complaintRepository.countByIsDeletedTrue()).thenReturn(2L);
        when(complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.OPEN)).thenReturn(4L);
        when(complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.ASSIGNED)).thenReturn(2L);
        when(complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.IN_PROGRESS)).thenReturn(2L);
        when(complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.RESOLVED)).thenReturn(1L);
        when(complaintRepository.countByStatusAndIsDeletedFalse(ComplaintStatus.CLOSED)).thenReturn(1L);
        when(complaintRepository.countByPriorityAndIsDeletedFalse(Priority.LOW)).thenReturn(1L);
        when(complaintRepository.countByPriorityAndIsDeletedFalse(Priority.MEDIUM)).thenReturn(3L);
        when(complaintRepository.countByPriorityAndIsDeletedFalse(Priority.HIGH)).thenReturn(4L);
        when(complaintRepository.countByPriorityAndIsDeletedFalse(Priority.CRITICAL)).thenReturn(2L);

        var stats = complaintService.getComplaintStatistics();

        assertThat(stats).isNotNull();
        assertThat(stats.get("totalComplaints")).isEqualTo(10L);
        assertThat(stats.get("deletedComplaints")).isEqualTo(2L);
        assertThat(stats.get("openComplaints")).isEqualTo(4L);
        assertThat(stats.get("criticalPriority")).isEqualTo(2L);
    }
}

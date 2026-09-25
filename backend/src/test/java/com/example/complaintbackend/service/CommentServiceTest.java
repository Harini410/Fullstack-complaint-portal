package com.example.complaintbackend.service;

import com.example.complaintbackend.dto.comment.CommentRequest;
import com.example.complaintbackend.dto.comment.CommentResponse;
import com.example.complaintbackend.entity.*;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.repository.CommentRepository;
import com.example.complaintbackend.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    private User complaintCreator;
    private User assignedAgent;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        complaintCreator = new User("citizen", "citizen@example.com", "pass", "Citizen User", Role.ROLE_USER);
        complaintCreator.setId(10L);

        assignedAgent = new User("agent_alex", "alex@example.com", "pass", "Alex Rivera", Role.ROLE_SUPPORT_AGENT);
        assignedAgent.setId(20L);

        Category cat = new Category("Water", "Water issues");
        cat.setId(1L);

        complaint = new Complaint("Water leakage", "Main pipe leaking", cat, Priority.HIGH, ComplaintStatus.IN_PROGRESS, complaintCreator);
        complaint.setId(100L);
        complaint.setAssignedTo(assignedAgent);
    }

    @Test
    @DisplayName("addComment() by agent should notify complaint creator")
    void testAddComment_ByAgent_NotifiesCreator() {
        CommentRequest request = new CommentRequest("Inspection team will arrive at 2 PM");
        Comment savedComment = new Comment(complaint, assignedAgent, request.getContent());
        savedComment.setId(500L);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        CommentResponse response = commentService.addComment(100L, request, assignedAgent);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Inspection team will arrive at 2 PM");
        assertThat(response.getAuthorName()).isEqualTo("Alex Rivera");

        verify(notificationService, times(1)).createNotification(
                eq(complaintCreator), eq(100L), contains("from agent_alex"), eq("COMMENT_ADDED")
        );
        verify(notificationService, never()).createNotification(
                eq(assignedAgent), anyLong(), anyString(), anyString()
        );
    }

    @Test
    @DisplayName("addComment() by creator should notify assigned agent")
    void testAddComment_ByCreator_NotifiesAssignedAgent() {
        CommentRequest request = new CommentRequest("Thank you, gate is unlocked");
        Comment savedComment = new Comment(complaint, complaintCreator, request.getContent());
        savedComment.setId(501L);

        when(complaintRepository.findById(100L)).thenReturn(Optional.of(complaint));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        CommentResponse response = commentService.addComment(100L, request, complaintCreator);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Thank you, gate is unlocked");

        verify(notificationService, times(1)).createNotification(
                eq(assignedAgent), eq(100L), contains("from citizen"), eq("COMMENT_ADDED")
        );
        verify(notificationService, never()).createNotification(
                eq(complaintCreator), anyLong(), anyString(), anyString()
        );
    }

    @Test
    @DisplayName("addComment() should throw ResourceNotFoundException when complaint does not exist")
    void testAddComment_ComplaintNotFound_ThrowsException() {
        CommentRequest request = new CommentRequest("Any update?");

        when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(999L, request, complaintCreator))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Complaint not found with id : '999'");

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCommentsForComplaint() should return ordered list of comments")
    void testGetCommentsForComplaint_Success() {
        Comment c1 = new Comment(complaint, complaintCreator, "First message");
        c1.setId(1L);
        Comment c2 = new Comment(complaint, assignedAgent, "Second message");
        c2.setId(2L);

        when(complaintRepository.existsById(100L)).thenReturn(true);
        when(commentRepository.findByComplaintIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(c1, c2));

        List<CommentResponse> comments = commentService.getCommentsForComplaint(100L);

        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getContent()).isEqualTo("First message");
        assertThat(comments.get(1).getContent()).isEqualTo("Second message");
    }

    @Test
    @DisplayName("getCommentsForComplaint() should throw ResourceNotFoundException when complaint does not exist")
    void testGetCommentsForComplaint_NotFound_ThrowsException() {
        when(complaintRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getCommentsForComplaint(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Complaint not found with id : '999'");
    }
}

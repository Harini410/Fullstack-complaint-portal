package com.example.complaintbackend.service;

import com.example.complaintbackend.dto.comment.CommentRequest;
import com.example.complaintbackend.dto.comment.CommentResponse;
import com.example.complaintbackend.entity.Comment;
import com.example.complaintbackend.entity.Complaint;
import com.example.complaintbackend.entity.User;
import com.example.complaintbackend.exception.ResourceNotFoundException;
import com.example.complaintbackend.repository.CommentRepository;
import com.example.complaintbackend.repository.ComplaintRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;

    public CommentService(
            CommentRepository commentRepository,
            ComplaintRepository complaintRepository,
            NotificationService notificationService
    ) {
        this.commentRepository = commentRepository;
        this.complaintRepository = complaintRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CommentResponse addComment(Long complaintId, CommentRequest request, User author) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId));

        Comment comment = new Comment(complaint, author, request.getContent());
        Comment saved = commentRepository.save(comment);
        log.info("New comment added on complaint #{} by user '{}'", complaintId, author != null ? author.getUsername() : "anonymous");

        // Notify creator if comment was posted by agent/admin, or notify agent if posted by user
        if (author != null) {
            if (complaint.getCreatedBy() != null && !author.getId().equals(complaint.getCreatedBy().getId())) {
                notificationService.createNotification(
                        complaint.getCreatedBy(),
                        complaint.getId(),
                        "New comment on your complaint #" + complaint.getId() + " from " + author.getUsername(),
                        "COMMENT_ADDED"
                );
            }
            if (complaint.getAssignedTo() != null && !author.getId().equals(complaint.getAssignedTo().getId())) {
                notificationService.createNotification(
                        complaint.getAssignedTo(),
                        complaint.getId(),
                        "New comment on assigned complaint #" + complaint.getId() + " from " + author.getUsername(),
                        "COMMENT_ADDED"
                );
            }
        }

        return new CommentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsForComplaint(Long complaintId) {
        if (!complaintRepository.existsById(complaintId)) {
            throw new ResourceNotFoundException("Complaint", "id", complaintId);
        }
        return commentRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .map(CommentResponse::new)
                .collect(Collectors.toList());
    }
}

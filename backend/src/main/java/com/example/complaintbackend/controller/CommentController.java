package com.example.complaintbackend.controller;

import com.example.complaintbackend.dto.comment.CommentRequest;
import com.example.complaintbackend.dto.comment.CommentResponse;
import com.example.complaintbackend.entity.User;
import com.example.complaintbackend.service.AuthService;
import com.example.complaintbackend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints/{complaintId}/comments")
@Tag(name = "Comments", description = "Add and retrieve comments on complaints")
public class CommentController {

    private final CommentService commentService;
    private final AuthService authService;

    public CommentController(CommentService commentService, AuthService authService) {
        this.commentService = commentService;
        this.authService = authService;
    }

    @PostMapping
    @Operation(summary = "Add comment", description = "Adds a discussion comment to a specific complaint")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long complaintId,
            @Valid @RequestBody CommentRequest request
    ) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        CommentResponse response = commentService.addComment(complaintId, request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get comments", description = "Retrieves all comments for a specific complaint ordered chronologically")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long complaintId) {
        return ResponseEntity.ok(commentService.getCommentsForComplaint(complaintId));
    }
}

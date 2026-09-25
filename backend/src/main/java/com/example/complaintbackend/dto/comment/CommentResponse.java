package com.example.complaintbackend.dto.comment;

import com.example.complaintbackend.entity.Comment;
import java.time.LocalDateTime;

public class CommentResponse {

    private Long id;
    private Long complaintId;
    private Long authorId;
    private String authorName;
    private String authorRole;
    private String content;
    private LocalDateTime createdAt;

    public CommentResponse() {}

    public CommentResponse(Comment comment) {
        if (comment != null) {
            this.id = comment.getId();
            if (comment.getComplaint() != null) {
                this.complaintId = comment.getComplaint().getId();
            }
            if (comment.getAuthor() != null) {
                this.authorId = comment.getAuthor().getId();
                this.authorName = comment.getAuthor().getFullName() != null ?
                        comment.getAuthor().getFullName() : comment.getAuthor().getUsername();
                this.authorRole = comment.getAuthor().getRole() != null ?
                        comment.getAuthor().getRole().name() : null;
            }
            this.content = comment.getContent();
            this.createdAt = comment.getCreatedAt();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

package com.example.complaintbackend.controller;

import com.example.complaintbackend.dto.comment.CommentRequest;
import com.example.complaintbackend.dto.comment.CommentResponse;
import com.example.complaintbackend.entity.*;
import com.example.complaintbackend.security.JwtAuthenticationEntryPoint;
import com.example.complaintbackend.security.JwtAuthenticationFilter;
import com.example.complaintbackend.security.JwtService;
import com.example.complaintbackend.service.AuthService;
import com.example.complaintbackend.service.CommentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("GET /api/complaints/{id}/comments - Returns list of comments")
    void testGetComments() throws Exception {
        User author = new User("sarah", "sarah@example.com", "pass", "Sarah Jenkins", Role.ROLE_SUPPORT_AGENT);
        author.setId(2L);
        Complaint complaint = new Complaint("Water", "Pipe burst", "Open");
        complaint.setId(10L);
        Comment comment = new Comment(complaint, author, "Assigned crew will inspect in 1 hour.");
        comment.setId(100L);

        when(commentService.getCommentsForComplaint(10L)).thenReturn(List.of(new CommentResponse(comment)));

        mockMvc.perform(get("/api/complaints/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].content").value("Assigned crew will inspect in 1 hour."))
                .andExpect(jsonPath("$[0].authorName").value("Sarah Jenkins"));
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/comments - Valid comment returns 201 Created")
    void testAddComment_Success() throws Exception {
        User author = new User("citizen", "citizen@example.com", "pass", "Citizen User", Role.ROLE_USER);
        author.setId(1L);
        Complaint complaint = new Complaint("Water", "Pipe burst", "Open");
        complaint.setId(10L);
        Comment comment = new Comment(complaint, author, "Water pressure has decreased.");
        comment.setId(101L);

        CommentRequest request = new CommentRequest("Water pressure has decreased.");

        when(authService.getCurrentAuthenticatedUser()).thenReturn(author);
        when(commentService.addComment(eq(10L), any(CommentRequest.class), any())).thenReturn(new CommentResponse(comment));

        mockMvc.perform(post("/api/complaints/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.content").value("Water pressure has decreased."));
    }

    @Test
    @DisplayName("POST /api/complaints/{id}/comments - Blank comment returns 400 Bad Request")
    void testAddComment_BlankContent_ReturnsBadRequest() throws Exception {
        CommentRequest request = new CommentRequest("");

        mockMvc.perform(post("/api/complaints/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.content").exists());
    }
}

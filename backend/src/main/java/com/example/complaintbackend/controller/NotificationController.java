package com.example.complaintbackend.controller;

import com.example.complaintbackend.entity.Notification;
import com.example.complaintbackend.entity.User;
import com.example.complaintbackend.service.AuthService;
import com.example.complaintbackend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Notifications", description = "User event notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Get user notifications", description = "Retrieves all notifications for the currently logged-in user")
    public ResponseEntity<List<Notification>> getNotifications() {
        User user = authService.getCurrentAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getId()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Retrieves the number of unread notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User user = authService.getCurrentAuthenticatedUser();
        long count = user != null ? notificationService.getUnreadCount(user.getId()) : 0;
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks an individual notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        if (user != null) {
            notificationService.markAsRead(id, user.getId());
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read")
    public ResponseEntity<Void> markAllAsRead() {
        User user = authService.getCurrentAuthenticatedUser();
        if (user != null) {
            notificationService.markAllAsRead(user.getId());
        }
        return ResponseEntity.ok().build();
    }
}

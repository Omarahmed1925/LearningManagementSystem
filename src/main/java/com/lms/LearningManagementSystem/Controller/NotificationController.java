package com.lms.LearningManagementSystem.Controller;

import com.lms.LearningManagementSystem.Model.Notification;
import com.lms.LearningManagementSystem.Service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotifications(@PathVariable Long userId, @RequestParam(defaultValue = "false") boolean onlyUnread) {
        return notificationService.getNotifications(userId, onlyUnread);
    }

    @PostMapping("/{userId}/{notificationId}/read")
    public ResponseEntity<Object> markNotificationAsRead(
            @PathVariable Long userId,
            @PathVariable String notificationId) {

        int result = notificationService.markNotificationAsRead(userId, notificationId);

        switch (result) {
            case 1:
                return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
            case 0:
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No notifications found for this user."));
            case -1:
            default:
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found."));
        }
    }


}


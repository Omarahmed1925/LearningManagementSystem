package com.lms.LearningManagementSystem.Controller;

import com.lms.LearningManagementSystem.Model.Notification;
import com.lms.LearningManagementSystem.Service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<String> markNotificationAsRead(@PathVariable Long userId, @PathVariable String notificationId) {
        List<Notification> notifications = notificationService.getUnread(userId);

        if (notifications.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No unread notifications found.");
        }

        notificationService.markNotificationAsRead(userId, notificationId);
        return ResponseEntity.ok("Notification marked as read.");
    }

}


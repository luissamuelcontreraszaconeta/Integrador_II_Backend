package com.exportrace.controller;

import com.exportrace.dto.NotificationDTO;
import com.exportrace.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "user@exportrace.pe";
        List<NotificationDTO> notifications = notificationService.getUserNotifications(email);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "user@exportrace.pe";
        long unreadCount = notificationService.getUnreadCount(email);
        Map<String, Object> resp = new HashMap<>();
        resp.put("unreadCount", unreadCount);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "user@exportrace.pe";
        NotificationDTO dto = notificationService.markAsRead(id, email);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "user@exportrace.pe";
        notificationService.markAllAsRead(email);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Todas las notificaciones han sido marcadas como leídas");
        return ResponseEntity.ok(resp);
    }
}

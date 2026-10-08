package com.exportrace.service;

import com.exportrace.dto.NotificationDTO;
import com.exportrace.entity.Notification;
import com.exportrace.entity.User;
import com.exportrace.repository.NotificationRepository;
import com.exportrace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        String roleName = user != null && user.getRole() != null ? user.getRole().getNombre() : null;

        List<Notification> list = notificationRepository.findAuthorizedNotifications(user, roleName);
        return list.stream().map(NotificationDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        String roleName = user != null && user.getRole() != null ? user.getRole().getNombre() : null;

        return notificationRepository.countUnreadAuthorized(user, roleName);
    }

    @Transactional
    public NotificationDTO markAsRead(Long id, String userEmail) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada con ID: " + id));

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);

        User user = userRepository.findByEmail(userEmail).orElse(null);
        auditService.logAction(
                user != null ? user.getId() : null,
                userEmail,
                user != null && user.getRole() != null ? user.getRole().getNombre() : "USER",
                "NOTIFICATION_READ",
                "NOTIFICATIONS",
                "Notification",
                String.valueOf(id),
                "Notificación marcada como leída: " + notification.getTitle(),
                null,
                "isRead: true",
                "EXITOSO",
                null
        );

        return new NotificationDTO(saved);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        String roleName = user != null && user.getRole() != null ? user.getRole().getNombre() : null;

        notificationRepository.markAllAsReadForUser(user, roleName);

        auditService.logAction(
                user != null ? user.getId() : null,
                userEmail,
                roleName != null ? roleName : "USER",
                "NOTIFICATIONS_ALL_READ",
                "NOTIFICATIONS",
                "Notification",
                "ALL",
                "Todas las notificaciones fueron marcadas como leídas",
                null,
                "isRead: true",
                "EXITOSO",
                null
        );
    }

    @Transactional
    public NotificationDTO createNotification(User user, String targetRole, String title, String message,
                                              String type, String priority, String module, String entityType,
                                              String entityId, String route) {
        Notification notification = new Notification(user, targetRole, title, message, type, priority, module, entityType, entityId, route);
        Notification saved = notificationRepository.save(notification);

        auditService.logAction(
                null,
                "SYSTEM",
                "SYSTEM",
                "NOTIFICATION_CREATED",
                module != null ? module : "NOTIFICATIONS",
                "Notification",
                String.valueOf(saved.getId()),
                "Notificación emitida [" + type + "] para rol: " + (targetRole != null ? targetRole : "Usuario " + (user != null ? user.getEmail() : "N/A")),
                null,
                "Type: " + type + ", Priority: " + priority,
                "EXITOSO",
                null
        );

        return new NotificationDTO(saved);
    }

    @Transactional
    public NotificationDTO broadcastRoleNotification(String targetRole, String title, String message,
                                                     String type, String priority, String module,
                                                     String entityType, String entityId, String route) {
        return createNotification(null, targetRole, title, message, type, priority, module, entityType, entityId, route);
    }

    @Transactional
    public NotificationDTO createNotification(String targetRole, String title, String message, String priority) {
        return createNotification(null, targetRole, title, message, "ALERT", priority, "FRIO", "Lot", null, "/quality/coldchain");
    }
}

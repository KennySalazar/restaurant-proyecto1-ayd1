package com.restaurante.application.notification;

import com.restaurante.domain.model.Notification;
import com.restaurante.domain.repository.NotificationRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.notification.NotificationCountResponse;
import com.restaurante.web.dto.notification.NotificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Servicio para la gestión de notificaciones del sistema para el administrador
 */
@Service
public class NotificationService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Lista las notificaciones del administrador con opción de filtrar solo no
     * leídas
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> listAdminNotifications(boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByRestaurantIdAndReadFalseOrderByCreatedAtDesc(DEFAULT_RESTAURANT_ID)
                : notificationRepository.findByRestaurantIdOrderByCreatedAtDesc(DEFAULT_RESTAURANT_ID);

        return notifications.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Obtiene el conteo de notificaciones pendientes/no leídas
     */
    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadNotificationCount() {
        long count = notificationRepository.countByRestaurantIdAndReadFalse(DEFAULT_RESTAURANT_ID);
        return new NotificationCountResponse(count);
    }

    /**
     * Marca una notificación específica como leída
     */
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "notification_not_found",
                        "Notificación no encontrada",
                        "No se encontró la notificación con el identificador " + id));

        if (!notification.isRead()) {
            notification.markAsRead();
            notification = notificationRepository.save(notification);
        }

        return mapToResponse(notification);
    }

    /**
     * Marca todas las notificaciones pendientes del restaurante como leídas
     */
    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead(DEFAULT_RESTAURANT_ID, Instant.now());
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getEntity(),
                n.getEntityId(),
                n.getPriority(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt());
    }
}

package com.restaurante.web.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Conteo de notificaciones pendientes/no leídas")
public record NotificationCountResponse(
                @Schema(description = "Cantidad de notificaciones no leídas", example = "5") long unreadCount) {
}

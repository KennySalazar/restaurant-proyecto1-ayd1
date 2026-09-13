package com.restaurante.web.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Detalle de una notificación del sistema")
public record NotificationResponse(
                @Schema(description = "Identificador único de la notificación", example = "1") Long id,

                @Schema(description = "Tipo de notificación", example = "STOCK_BAJO") String type,

                @Schema(description = "Título de la notificación", example = "Insumo con stock bajo") String title,

                @Schema(description = "Mensaje descriptivo", example = "El insumo Harina de Trigo Integral alcanzó el nivel mínimo de stock") String message,

                @Schema(description = "Entidad asociada a la notificación", example = "INSUMO") String entity,

                @Schema(description = "Identificador de la entidad asociada", example = "1") String entityId,

                @Schema(description = "Nivel de prioridad", example = "ALTA", allowableValues = {
                                "BAJA", "MEDIA", "ALTA", "CRITICA" }) String priority,

                @Schema(description = "Indica si la notificación ha sido leída", example = "false") boolean read,

                @Schema(description = "Fecha y hora en que se marcó como leída", example = "2026-09-13T21:00:00Z") Instant readAt,

                @Schema(description = "Fecha y hora de creación de la notificación", example = "2026-09-13T20:00:00Z") Instant createdAt) {
}

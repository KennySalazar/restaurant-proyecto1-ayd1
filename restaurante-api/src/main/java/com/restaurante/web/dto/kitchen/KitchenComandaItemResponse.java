package com.restaurante.web.dto.kitchen;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Detalle de un platillo o combo en la vista operativa de cocina con tiempos y estado de alerta.
 */
@Schema(description = "Detalle de un platillo u orden en la vista de cocina con tiempos y alertas")
public record KitchenComandaItemResponse(
        @Schema(description = "Identificador del detalle de comanda", example = "10")
        Long id,

        @Schema(description = "Identificador del platillo (si aplica)", example = "1")
        Long dishId,

        @Schema(description = "Identificador del combo (si aplica)", example = "2")
        Long comboId,

        @Schema(description = "Nombre del platillo o combo", example = "Hamburguesa Especial")
        String name,

        @Schema(description = "Cantidad ordenada", example = "2")
        short quantity,

        @Schema(description = "Estado actual del platillo", example = "RECIBIDO")
        String status,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "15")
        short estimatedTimeMinutes,

        @Schema(description = "Notas especiales de preparación", example = "Término medio, sin cebolla")
        String specialNotes,

        @Schema(description = "Modificadores seleccionados", example = "[\"Extra Queso\", \"Tocino\"]")
        List<String> modifiers,

        @Schema(description = "Fecha y hora en que fue recibido en cocina")
        Instant receivedAt,

        @Schema(description = "Fecha y hora en que inició su preparación")
        Instant preparationStartedAt,

        @Schema(description = "Fecha y hora límite estimada de preparación")
        Instant deadline,

        @Schema(description = "Minutos transcurridos desde que fue recibido o inició preparación", example = "18")
        long elapsedMinutes,

        @Schema(description = "Indica si el platillo superó su tiempo estimado de preparación sin estar listo", example = "true")
        boolean timeExceeded,

        @Schema(description = "Minutos de retraso respecto al tiempo estimado de preparación", example = "3")
        long delayMinutes,

        @Schema(description = "Nivel de alerta visual del platillo (NORMAL, TIEMPO_EXCEDIDO)", example = "TIEMPO_EXCEDIDO")
        String alertLevel
) {
    public KitchenComandaItemResponse(
            Long id,
            Long dishId,
            Long comboId,
            String name,
            short quantity,
            String status,
            short estimatedTimeMinutes,
            String specialNotes,
            List<String> modifiers) {
        this(id, dishId, comboId, name, quantity, status, estimatedTimeMinutes, specialNotes, modifiers,
                null, null, null, 0L, false, 0L, "NORMAL");
    }
}

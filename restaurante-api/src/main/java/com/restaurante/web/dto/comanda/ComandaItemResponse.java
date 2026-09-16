package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Detalle comercial y operativo de un platillo o combo dentro de una comanda con tiempos y alertas.
 */
@Schema(description = "Detalle de un platillo o combo en una comanda con tiempos y alertas")
public record ComandaItemResponse(
        @Schema(description = "Identificador del detalle", example = "10")
        Long id,

        @Schema(description = "Identificador del platillo (si aplica)", example = "1")
        Long dishId,

        @Schema(description = "Identificador del combo (si aplica)", example = "2")
        Long comboId,

        @Schema(description = "Nombre registrado del platillo o combo", example = "Hamburguesa Clásica")
        String name,

        @Schema(description = "Cantidad ordenada", example = "2")
        short quantity,

        @Schema(description = "Precio unitario", example = "45.00")
        BigDecimal unitPrice,

        @Schema(description = "Estado actual del ítem", example = "RECIBIDO")
        String status,

        @Schema(description = "Notas especiales de preparación", example = "Sin cebolla")
        String specialNotes,

        @Schema(description = "Modificadores seleccionados")
        List<String> modifiers,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "15")
        short estimatedTimeMinutes,

        @Schema(description = "Minutos transcurridos desde que fue recibido o inició preparación", example = "18")
        long elapsedMinutes,

        @Schema(description = "Fecha y hora límite estimada para estar listo")
        Instant deadline,

        @Schema(description = "Indica si el platillo superó su tiempo estimado de preparación sin estar listo", example = "true")
        boolean timeExceeded,

        @Schema(description = "Minutos de retraso respecto al tiempo estimado de preparación", example = "3")
        long delayMinutes,

        @Schema(description = "Nivel de alerta visual del platillo (NORMAL, TIEMPO_EXCEDIDO)", example = "TIEMPO_EXCEDIDO")
        String alertLevel
) {
    public ComandaItemResponse(
            Long id,
            Long dishId,
            Long comboId,
            String name,
            short quantity,
            BigDecimal unitPrice,
            String status,
            String specialNotes,
            List<String> modifiers) {
        this(id, dishId, comboId, name, quantity, unitPrice, status, specialNotes, modifiers,
                (short) 15, 0L, null, false, 0L, "NORMAL");
    }
}

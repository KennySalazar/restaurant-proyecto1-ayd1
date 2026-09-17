package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Representación del avance y estado de servicio de un platillo u orden en la mesa con alertas de tiempo.
 */
@Schema(description = "Detalle del avance y estado de entrega de un platillo de comanda con tiempos y alertas")
public record ComandaDishProgressResponse(
        @Schema(description = "Identificador del detalle de la comanda", example = "10")
        Long id,

        @Schema(description = "Identificador de la comanda", example = "5")
        Long comandaId,

        @Schema(description = "Identificador de la cuenta", example = "3")
        Long accountId,

        @Schema(description = "Identificador de la mesa", example = "2")
        Long tableId,

        @Schema(description = "Número o código de la mesa", example = "MESA-02")
        String tableNumber,

        @Schema(description = "Número de ronda de la comanda", example = "1")
        short roundNumber,

        @Schema(description = "Identificador del platillo (si aplica)", example = "1")
        Long dishId,

        @Schema(description = "Identificador del combo (si aplica)", example = "2")
        Long comboId,

        @Schema(description = "Nombre registrado del platillo o ítem", example = "Hamburguesa Clásica")
        String name,

        @Schema(description = "Cantidad ordenada", example = "2")
        short quantity,

        @Schema(description = "Precio unitario", example = "45.00")
        BigDecimal unitPrice,

        @Schema(description = "Estado actual del platillo", example = "EN_PREPARACION")
        String status,

        @Schema(description = "Estado previo a la última operación", example = "RECIBIDO")
        String previousStatus,

        @Schema(description = "Notas especiales de preparación", example = "Sin cebolla")
        String specialNotes,

        @Schema(description = "Modificadores seleccionados")
        List<String> modifiers,

        @Schema(description = "Fecha y hora en que fue recibido en cocina")
        Instant receivedAt,

        @Schema(description = "Fecha y hora en que inició su preparación")
        Instant preparationStartedAt,

        @Schema(description = "Fecha y hora en que quedó listo en cocina")
        Instant readyAt,

        @Schema(description = "Fecha y hora en que fue entregado a la mesa")
        Instant deliveredAt,

        @Schema(description = "Identificador del mesero asignado", example = "4")
        Long waiterId,

        @Schema(description = "Nombre del mesero asignado", example = "Juan Pérez")
        String waiterName,

        @Schema(description = "Estado general de la comanda", example = "EN_PREPARACION")
        String comandaStatus,

        @Schema(description = "Mensaje informativo de la operación")
        String message,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "15")
        short estimatedTimeMinutes,

        @Schema(description = "Minutos transcurridos desde que fue recibido o inició preparación", example = "22")
        long elapsedMinutes,

        @Schema(description = "Fecha y hora límite estimada para estar listo")
        Instant deadline,

        @Schema(description = "Indica si el platillo superó su tiempo estimado de preparación sin estar listo", example = "true")
        boolean timeExceeded,

        @Schema(description = "Minutos de retraso respecto al tiempo estimado de preparación", example = "7")
        long delayMinutes,

        @Schema(description = "Nivel de alerta visual del platillo (NORMAL, TIEMPO_EXCEDIDO)", example = "TIEMPO_EXCEDIDO")
        String alertLevel
) {
    public ComandaDishProgressResponse(
            Long id,
            Long comandaId,
            Long accountId,
            Long tableId,
            String tableNumber,
            short roundNumber,
            Long dishId,
            Long comboId,
            String name,
            short quantity,
            BigDecimal unitPrice,
            String status,
            String previousStatus,
            String specialNotes,
            List<String> modifiers,
            Instant receivedAt,
            Instant preparationStartedAt,
            Instant readyAt,
            Instant deliveredAt,
            Long waiterId,
            String waiterName,
            String comandaStatus,
            String message) {
        this(id, comandaId, accountId, tableId, tableNumber, roundNumber, dishId, comboId, name, quantity,
                unitPrice, status, previousStatus, specialNotes, modifiers, receivedAt, preparationStartedAt,
                readyAt, deliveredAt, waiterId, waiterName, comandaStatus, message,
                (short) 15, 0L, null, false, 0L, "NORMAL");
    }
}

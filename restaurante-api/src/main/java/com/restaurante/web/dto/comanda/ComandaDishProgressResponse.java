package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Representación del avance y estado de servicio de un platillo u orden en la mesa.
 */
@Schema(description = "Detalle del avance y estado de entrega de un platillo de comanda")
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

        @Schema(description = "Estado actual del platillo", example = "ENTREGADO")
        String status,

        @Schema(description = "Estado previo a la última operación", example = "LISTO")
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

        @Schema(description = "Estado general de la comanda", example = "ENTREGADA")
        String comandaStatus,

        @Schema(description = "Mensaje informativo de la operación")
        String message
) {
}

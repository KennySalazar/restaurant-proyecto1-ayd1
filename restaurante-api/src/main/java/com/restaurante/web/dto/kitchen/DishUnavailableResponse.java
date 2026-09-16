package com.restaurante.web.dto.kitchen;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Respuesta operativa cuando un platillo de una comanda es marcado como no disponible por cocina.
 */
@Schema(description = "Detalle del platillo marcado como no disponible por cocina por falta de insumos")
public record DishUnavailableResponse(
        @Schema(description = "Identificador del detalle de la comanda", example = "10")
        Long id,

        @Schema(description = "Identificador de la comanda", example = "5")
        Long comandaId,

        @Schema(description = "Identificador de la cuenta", example = "3")
        Long accountId,

        @Schema(description = "Identificador de la mesa", example = "2")
        Long tableId,

        @Schema(description = "Número o código de mesa", example = "MESA-02")
        String tableNumber,

        @Schema(description = "Identificador del platillo en el catálogo (si aplica)", example = "1")
        Long dishId,

        @Schema(description = "Nombre registrado del platillo o ítem", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Cantidad ordenada", example = "2")
        short quantity,

        @Schema(description = "Estado previo del platillo", example = "EN_PREPARACION")
        String previousStatus,

        @Schema(description = "Estado actual del platillo tras la operación", example = "NO_DISPONIBLE")
        String currentStatus,

        @Schema(description = "Motivo de la falta de insumo o discrepancia registrada", example = "Falta de carne molida")
        String reason,

        @Schema(description = "Indica si el platillo fue marcado como no disponible en el menú (App1)", example = "true")
        boolean menuDisabled,

        @Schema(description = "Identificador del mesero responsable notificado", example = "4")
        Long waiterId,

        @Schema(description = "Nombre del mesero notificado", example = "Juan Pérez")
        String waiterName,

        @Schema(description = "Estado general de la comanda tras la operación", example = "EN_PREPARACION")
        String comandaStatus,

        @Schema(description = "Fecha y hora en que se marcó como no disponible")
        Instant markedAt,

        @Schema(description = "Mensaje informativo para el personal de cocina")
        String message
) {
}

package com.restaurante.web.dto.kitchen;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Respuesta a la actualización de estado de preparación de un platillo en cocina.
 */
@Schema(description = "Detalle del cambio de estado de preparación de un platillo")
public record DishPreparationStatusResponse(
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

        @Schema(description = "Nombre registrado del platillo o ítem", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Cantidad ordenada", example = "2")
        short quantity,

        @Schema(description = "Estado previo del platillo", example = "RECIBIDO")
        String previousStatus,

        @Schema(description = "Estado actual del platillo tras la actualización", example = "EN_PREPARACION")
        String currentStatus,

        @Schema(description = "Fecha y hora en que inició la preparación")
        Instant preparationStartedAt,

        @Schema(description = "Fecha y hora en que quedó listo")
        Instant readyAt,

        @Schema(description = "Estado general de la comanda tras la actualización", example = "EN_PREPARACION")
        String comandaStatus,

        @Schema(description = "Mensaje informativo de la operación", example = "Platillo marcado en preparación exitosamente")
        String message
) {
}

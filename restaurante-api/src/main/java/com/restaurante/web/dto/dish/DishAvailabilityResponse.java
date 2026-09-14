package com.restaurante.web.dto.dish;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de confirmación tras modificar manualmente la disponibilidad de un platillo.
 */
@Schema(description = "Respuesta de confirmación de cambio de disponibilidad manual de platillo")
public record DishAvailabilityResponse(
        @Schema(description = "Mensaje descriptivo del resultado de la operación", example = "Platillo marcado como no disponible exitosamente")
        String message,

        @Schema(description = "Datos actualizados y disponibilidad operativa del platillo")
        DishResponse dish
) {
}

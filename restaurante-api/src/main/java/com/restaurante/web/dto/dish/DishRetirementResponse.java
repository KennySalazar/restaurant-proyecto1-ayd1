package com.restaurante.web.dto.dish;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de confirmación tras retirar un platillo del menú.
 */
@Schema(description = "Respuesta de confirmación tras retirar un platillo del menú")
public record DishRetirementResponse(
        @Schema(description = "Mensaje de confirmación", example = "Platillo retirado exitosamente")
        String message,

        @Schema(description = "Datos del platillo retirado")
        DishResponse dish
) {
}

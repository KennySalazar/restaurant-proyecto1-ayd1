package com.restaurante.web.dto.dish;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de confirmación tras actualizar la información de un platillo.
 */
@Schema(description = "Respuesta de confirmación tras actualizar la información de un platillo")
public record DishUpdateResponse(
        @Schema(description = "Mensaje de confirmación", example = "Platillo actualizado exitosamente")
        String message,

        @Schema(description = "Datos actualizados del platillo")
        DishResponse dish
) {
}

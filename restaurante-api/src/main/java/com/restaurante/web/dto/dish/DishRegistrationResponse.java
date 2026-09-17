package com.restaurante.web.dto.dish;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta a la solicitud de registro exitoso de un nuevo platillo.
 */
@Schema(description = "Respuesta de registro de platillo")
public record DishRegistrationResponse(
        @Schema(description = "Mensaje de confirmación", example = "Platillo registrado exitosamente")
        String message,

        @Schema(description = "Datos del platillo guardado")
        DishResponse dish
) {
}

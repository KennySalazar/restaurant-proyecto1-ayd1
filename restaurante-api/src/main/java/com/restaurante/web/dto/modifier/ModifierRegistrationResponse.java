package com.restaurante.web.dto.modifier;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta tras registrar exitosamente un modificador de platillos.
 */
@Schema(description = "Respuesta de registro de modificador")
public record ModifierRegistrationResponse(
        @Schema(description = "Mensaje de confirmación", example = "Modificador registrado exitosamente")
        String message,

        @Schema(description = "Datos del modificador guardado")
        ModifierResponse modifier
) {
}

package com.restaurante.web.dto.combo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta tras registrar exitosamente un nuevo combo o promoción en el menú.
 */
@Schema(description = "Respuesta de registro de combo o promoción")
public record ComboRegistrationResponse(
        @Schema(description = "Mensaje de confirmación", example = "Combo registrado exitosamente")
        String message,

        @Schema(description = "Datos del combo registrado")
        ComboResponse combo
) {
}

package com.restaurante.web.dto.combo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta tras retirar un combo o promoción del menú comercial.
 */
@Schema(description = "Respuesta de confirmación tras retirar un combo o promoción del menú")
public record ComboRetirementResponse(
        @Schema(description = "Mensaje de confirmación", example = "Combo retirado exitosamente")
        String message,

        @Schema(description = "Datos del combo retirado")
        ComboResponse combo
) {
}

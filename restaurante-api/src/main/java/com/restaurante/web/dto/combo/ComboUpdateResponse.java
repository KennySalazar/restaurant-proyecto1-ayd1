package com.restaurante.web.dto.combo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta tras actualizar exitosamente un combo o promoción en el menú.
 */
@Schema(description = "Respuesta de confirmación tras actualizar un combo o promoción")
public record ComboUpdateResponse(
        @Schema(description = "Mensaje de confirmación", example = "Combo actualizado exitosamente")
        String message,

        @Schema(description = "Datos actualizados del combo")
        ComboResponse combo
) {
}

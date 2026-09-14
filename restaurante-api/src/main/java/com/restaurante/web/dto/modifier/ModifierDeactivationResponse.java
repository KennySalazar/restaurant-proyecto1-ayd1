package com.restaurante.web.dto.modifier;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta tras desactivar un modificador de platillos.
 */
@Schema(description = "Respuesta de desactivación de modificador")
public record ModifierDeactivationResponse(
        @Schema(description = "Mensaje de confirmación", example = "Modificador desactivado exitosamente")
        String message,

        @Schema(description = "Datos del modificador desactivado")
        ModifierResponse modifier
) {
}

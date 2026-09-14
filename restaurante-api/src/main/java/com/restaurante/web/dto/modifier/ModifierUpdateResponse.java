package com.restaurante.web.dto.modifier;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta tras actualizar la información de un modificador de platillos.
 */
@Schema(description = "Respuesta de actualización de modificador")
public record ModifierUpdateResponse(
        @Schema(description = "Mensaje de confirmación", example = "Modificador actualizado exitosamente")
        String message,

        @Schema(description = "Datos actualizados del modificador")
        ModifierResponse modifier
) {
}

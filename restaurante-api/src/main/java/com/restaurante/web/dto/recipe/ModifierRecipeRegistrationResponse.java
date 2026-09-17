package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de confirmación tras definir o actualizar la receta de un modificador.
 */
@Schema(description = "Respuesta de confirmación de registro de receta de modificador")
public record ModifierRecipeRegistrationResponse(
        @Schema(description = "Mensaje de confirmación", example = "Receta del modificador guardada exitosamente")
        String message,

        @Schema(description = "Detalle de la receta vigente registrada")
        ModifierRecipeResponse recipe
) {
}

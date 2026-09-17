package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de confirmación tras definir exitosamente la receta de un platillo.
 */
@Schema(description = "Respuesta de confirmación de definición de receta")
public record RecipeRegistrationResponse(
                @Schema(description = "Mensaje de confirmación", example = "Receta del platillo definida exitosamente") String message,

                @Schema(description = "Información detallada de la receta registrada") RecipeResponse recipe) {
}

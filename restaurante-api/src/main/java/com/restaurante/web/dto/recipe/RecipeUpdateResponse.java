package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de confirmación tras actualizar la receta de un platillo.
 */
@Schema(description = "Respuesta de confirmación tras actualizar la receta de un platillo")
public record RecipeUpdateResponse(
                @Schema(
                        description = "Mensaje de confirmación", 
                        example = "Receta del platillo actualizada exitosamente")
                         String message,

                @Schema(
                        description = "Información detallada de la nueva versión de receta vigente") 
                RecipeResponse recipe) {
}

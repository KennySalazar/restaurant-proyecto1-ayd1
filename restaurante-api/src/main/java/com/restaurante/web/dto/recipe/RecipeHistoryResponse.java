package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Respuesta completa del historial de versiones de una receta de platillo.
 */
@Schema(description = "Historial completo de versiones de la receta de un platillo")
public record RecipeHistoryResponse(
        @Schema(description = "Identificador único del platillo", example = "1")
        Long dishId,

        @Schema(description = "Código del platillo", example = "PLA-001")
        String dishCode,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Número de versión vigente actual", example = "2")
        Integer currentVersionNumber,

        @Schema(description = "Cantidad total de versiones registradas", example = "2")
        Integer totalVersions,

        @Schema(description = "Indica si existen cambios posteriores a la creación inicial", example = "true")
        Boolean hasSubsequentChanges,

        @Schema(description = "Mensaje informativo sobre el historial", example = "Historial de modificaciones de la receta obtenido exitosamente")
        String message,

        @Schema(description = "Versiones registradas presentadas en orden cronológico")
        List<RecipeVersionHistoryItemResponse> versions
) {
}

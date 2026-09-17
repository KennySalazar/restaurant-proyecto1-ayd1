package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta completa del historial de versiones de una receta de modificador.
 */
@Schema(description = "Historial completo de versiones de la receta de un modificador")
public record ModifierRecipeHistoryResponse(
        @Schema(description = "Identificador único del modificador", example = "1")
        Long modifierId,

        @Schema(description = "Código del modificador", example = "MOD-001")
        String modifierCode,

        @Schema(description = "Nombre del modificador", example = "Extra Queso")
        String modifierName,

        @Schema(description = "Precio adicional fijado para el modificador", example = "5.00")
        BigDecimal additionalPrice,

        @Schema(description = "Número de versión vigente actual", example = "2")
        Integer currentVersionNumber,

        @Schema(description = "Cantidad total de versiones registradas", example = "2")
        Integer totalVersions,

        @Schema(description = "Indica si existen cambios posteriores a la creación inicial", example = "true")
        Boolean hasSubsequentChanges,

        @Schema(description = "Mensaje informativo sobre el historial", example = "Historial de modificaciones de la receta del modificador obtenido exitosamente")
        String message,

        @Schema(description = "Versiones registradas presentadas en orden cronológico")
        List<ModifierRecipeVersionHistoryItemResponse> versions
) {
}

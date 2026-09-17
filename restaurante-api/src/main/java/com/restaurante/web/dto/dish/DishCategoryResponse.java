package com.restaurante.web.dto.dish;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representación simplificada de una categoría de platillos para selección en el menú.
 */
@Schema(description = "Categoría de platillo disponible en el menú")
public record DishCategoryResponse(
        @Schema(description = "Identificador único de la categoría", example = "1")
        Long id,

        @Schema(description = "Código de la categoría", example = "ENTRADA")
        String code,

        @Schema(description = "Nombre de la categoría", example = "Entrada")
        String name,

        @Schema(description = "Orden de visualización", example = "1")
        Short visualOrder
) {
}

package com.restaurante.web.dto.modifier;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumen de un platillo asociado a un modificador.
 */
@Schema(description = "Resumen de platillo asociado a un modificador")
public record ModifierDishItemResponse(
        @Schema(description = "Identificador único del platillo", example = "1")
        Long id,

        @Schema(description = "Código del platillo", example = "PLA-0001")
        String code,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String name,

        @Schema(description = "Nombre de la categoría del platillo", example = "Plato fuerte")
        String categoryName,

        @Schema(description = "Indica si el modificador es obligatorio para este platillo", example = "false")
        Boolean required,

        @Schema(description = "Máximo de selecciones permitidas para este platillo", example = "1")
        Short maxSelections
) {
}

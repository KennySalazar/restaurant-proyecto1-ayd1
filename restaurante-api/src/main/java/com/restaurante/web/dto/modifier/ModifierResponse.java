package com.restaurante.web.dto.modifier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Información consolidada de un modificador de platillo.
 */
@Schema(description = "Información consolidada de un modificador de platillo")
public record ModifierResponse(
        @Schema(description = "Identificador único del modificador", example = "1")
        Long id,

        @Schema(description = "Código del modificador", example = "MOD-0001")
        String code,

        @Schema(description = "Nombre comercial del modificador", example = "Queso extra")
        String name,

        @Schema(description = "Descripción del modificador", example = "Porción adicional de queso cheddar")
        String description,

        @Schema(description = "Costo o precio adicional al platillo", example = "5.00")
        BigDecimal additionalPrice,

        @Schema(description = "Estado activo del modificador", example = "true")
        Boolean active,

        @Schema(description = "Indica si el modificador cuenta con una receta vigente", example = "false")
        Boolean hasRecipe,

        @Schema(description = "Platillos asociados al modificador")
        List<ModifierDishItemResponse> associatedDishes,

        @Schema(description = "Fecha de creación", example = "2026-09-14T00:00:00Z")
        Instant createdAt,

        @Schema(description = "Fecha de última actualización", example = "2026-09-14T00:00:00Z")
        Instant updatedAt
) {
}

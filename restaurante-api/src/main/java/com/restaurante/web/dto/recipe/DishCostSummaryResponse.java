package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Resumen del costo de producción y margen de ganancia de un platillo.
 */
@Schema(description = "Resumen de costo de producción y margen de ganancia")
public record DishCostSummaryResponse(
        @Schema(description = "Identificador del platillo", example = "1")
        Long dishId,

        @Schema(description = "Código del platillo", example = "PLA-001")
        String dishCode,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Categoría del platillo", example = "Plato fuerte")
        String categoryName,

        @Schema(description = "Precio de venta fijado", example = "45.00")
        BigDecimal salePrice,

        @Schema(description = "Identificador de la versión de receta vigente", example = "1")
        Long recipeVersionId,

        @Schema(description = "Número de versión de receta vigente", example = "1")
        Integer recipeVersionNumber,

        @Schema(description = "Costo de producción actual", example = "18.5000")
        BigDecimal totalProductionCost,

        @Schema(description = "Margen bruto estimado", example = "26.5000")
        BigDecimal grossMargin,

        @Schema(description = "Porcentaje de margen de ganancia", example = "58.8889")
        BigDecimal marginPercentage
) {
}

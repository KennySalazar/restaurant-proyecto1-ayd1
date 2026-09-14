package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Cálculo automático del costo de producción de un platillo con sus márgenes.
 */
@Schema(description = "Costo de producción y márgenes de ganancia de un platillo")
public record DishProductionCostResponse(
        @Schema(description = "Identificador del platillo", example = "1")
        Long dishId,

        @Schema(description = "Código del platillo", example = "PLA-001")
        String dishCode,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Categoría del platillo", example = "Plato fuerte")
        String categoryName,

        @Schema(description = "Precio de venta fijado para el platillo", example = "45.00")
        BigDecimal salePrice,

        @Schema(description = "Identificador de la versión de receta vigente", example = "1")
        Long recipeVersionId,

        @Schema(description = "Número de versión de receta vigente", example = "1")
        Integer recipeVersionNumber,

        @Schema(description = "Costo total de producción calculado", example = "18.5000")
        BigDecimal totalProductionCost,

        @Schema(description = "Margen bruto estimado (precio de venta - costo)", example = "26.5000")
        BigDecimal grossMargin,

        @Schema(description = "Porcentaje de margen de ganancia", example = "58.8889")
        BigDecimal marginPercentage,

        @Schema(description = "Desglose de costos por insumo con conversión de unidades")
        List<ProductionCostIngredientResponse> ingredients,

        @Schema(description = "Fecha y hora del cálculo", example = "2026-09-13T23:59:00Z")
        Instant calculatedAt
) {
}

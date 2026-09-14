package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Cálculo automático del costo de producción de un modificador.
 */
@Schema(description = "Costo de producción de un modificador")
public record ModifierProductionCostResponse(
        @Schema(description = "Identificador del modificador", example = "1")
        Long modifierId,

        @Schema(description = "Código del modificador", example = "MOD-001")
        String modifierCode,

        @Schema(description = "Nombre del modificador", example = "Extra Queso")
        String modifierName,

        @Schema(description = "Precio adicional fijado para el modificador", example = "5.00")
        BigDecimal additionalPrice,

        @Schema(description = "Identificador de la versión de receta vigente", example = "1")
        Long recipeVersionId,

        @Schema(description = "Número de versión de receta vigente", example = "1")
        Integer recipeVersionNumber,

        @Schema(description = "Costo total de producción calculado para el modificador", example = "4.5000")
        BigDecimal totalProductionCost,

        @Schema(description = "Margen bruto estimado (precio adicional - costo)", example = "0.5000")
        BigDecimal grossMargin,

        @Schema(description = "Porcentaje de margen de ganancia", example = "10.0000")
        BigDecimal marginPercentage,

        @Schema(description = "Desglose de costos por insumo con conversión de unidades")
        List<ProductionCostIngredientResponse> ingredients,

        @Schema(description = "Fecha y hora del cálculo", example = "2026-09-13T23:59:00Z")
        Instant calculatedAt
) {
}

package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Detalle del insumo y costo proporcional en el cálculo del costo de producción.
 */
@Schema(description = "Detalle del insumo y costo en el cálculo de producción")
public record ProductionCostIngredientResponse(
        @Schema(description = "Identificador del insumo", example = "1")
        Long supplyId,

        @Schema(description = "Código del insumo", example = "INS-0001")
        String supplyCode,

        @Schema(description = "Nombre del insumo", example = "Carne molida")
        String supplyName,

        @Schema(description = "Cantidad utilizada en la receta", example = "250.000000")
        BigDecimal quantity,

        @Schema(description = "Identificador de la unidad de medida utilizada en la receta", example = "1")
        Short recipeUnitId,

        @Schema(description = "Nombre de la unidad de medida utilizada en la receta", example = "Gramo")
        String recipeUnitName,

        @Schema(description = "Abreviatura de la unidad de medida utilizada en la receta", example = "g")
        String recipeUnitAbbreviation,

        @Schema(description = "Unidad de medida de stock del insumo", example = "Kilogramo")
        String stockUnitName,

        @Schema(description = "Costo unitario actual del insumo en inventario", example = "40.0000")
        BigDecimal unitCost,

        @Schema(description = "Cantidad proporcional convertida a la unidad de stock del insumo", example = "0.250000")
        BigDecimal proportionalQuantity,

        @Schema(description = "Costo proporcional calculado para este insumo", example = "10.0000")
        BigDecimal subtotalCost,

        @Schema(description = "Observaciones", example = "Porción estándar")
        String notes
) {
}

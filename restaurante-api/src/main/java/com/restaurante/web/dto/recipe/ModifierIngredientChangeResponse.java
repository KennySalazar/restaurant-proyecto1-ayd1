package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Representa la identificación de un cambio en un insumo entre dos versiones de receta de un modificador.
 */
@Schema(description = "Identificación de cambio en un insumo de modificador")
public record ModifierIngredientChangeResponse(
        @Schema(description = "Identificador del insumo", example = "1")
        Long supplyId,

        @Schema(description = "Código del insumo", example = "INS-0001")
        String supplyCode,

        @Schema(description = "Nombre del insumo", example = "Queso Cheddar")
        String supplyName,

        @Schema(description = "Tipo de cambio: AGREGADO, RETIRADO, MODIFICADO, SIN_CAMBIOS", example = "MODIFICADO")
        String changeType,

        @Schema(description = "Tipo de ajuste en la versión anterior (AGREGAR o RETIRAR)", example = "AGREGAR")
        String previousAdjustmentType,

        @Schema(description = "Cantidad en la versión anterior (null si fue agregado)", example = "30.000000")
        BigDecimal previousQuantity,

        @Schema(description = "Unidad de medida en la versión anterior", example = "Gramo")
        String previousUnitName,

        @Schema(description = "Tipo de ajuste en la nueva versión (AGREGAR o RETIRAR)", example = "AGREGAR")
        String newAdjustmentType,

        @Schema(description = "Cantidad en la nueva versión (null si fue retirado)", example = "50.000000")
        BigDecimal newQuantity,

        @Schema(description = "Unidad de medida en la nueva versión", example = "Gramo")
        String newUnitName,

        @Schema(description = "Diferencia de cantidad (nueva - anterior)", example = "20.000000")
        BigDecimal quantityDifference,

        @Schema(description = "Diferencia de costo subtotal calculada (nueva - anterior)", example = "3.0000")
        BigDecimal costDifference,

        @Schema(description = "Observaciones asociadas al cambio", example = "Incremento de porción")
        String notes
) {
}

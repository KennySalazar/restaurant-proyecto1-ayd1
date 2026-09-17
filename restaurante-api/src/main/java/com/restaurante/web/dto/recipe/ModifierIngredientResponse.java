package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Detalle de un insumo asociado a una receta de modificador.
 */
@Schema(description = "Detalle de insumo en una receta de modificador")
public record ModifierIngredientResponse(
        @Schema(description = "Identificador único del detalle de receta", example = "1")
        Long id,

        @Schema(description = "Identificador del insumo", example = "1")
        Long supplyId,

        @Schema(description = "Código del insumo", example = "INS-0001")
        String supplyCode,

        @Schema(description = "Nombre del insumo", example = "Queso Cheddar")
        String supplyName,

        @Schema(description = "Cantidad requerida", example = "30.000000")
        BigDecimal quantity,

        @Schema(description = "Identificador de la unidad de medida", example = "1")
        Short measurementUnitId,

        @Schema(description = "Nombre de la unidad de medida", example = "Gramo")
        String measurementUnitName,

        @Schema(description = "Abreviatura de la unidad de medida", example = "g")
        String measurementUnitAbbreviation,

        @Schema(description = "Tipo de ajuste (AGREGAR o RETIRAR)", example = "AGREGAR")
        String adjustmentType,

        @Schema(description = "Costo unitario actual del insumo", example = "0.1500")
        BigDecimal unitCost,

        @Schema(description = "Costo subtotal aportado por este insumo", example = "4.5000")
        BigDecimal subtotalCost,

        @Schema(description = "Observaciones", example = "Porción extra")
        String notes
) {
}

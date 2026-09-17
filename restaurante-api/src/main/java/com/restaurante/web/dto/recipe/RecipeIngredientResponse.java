package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Detalle de un insumo asociado a una versión de receta.
 */
@Schema(description = "Detalle de insumo en una receta")
public record RecipeIngredientResponse(
                @Schema(description = "Identificador único del detalle de receta", example = "1") Long id,

                @Schema(description = "Identificador del insumo", example = "1") Long supplyId,

                @Schema(description = "Código del insumo", example = "INS-0001") String supplyCode,

                @Schema(description = "Nombre del insumo", example = "Carne Molida Especial") String supplyName,

                @Schema(description = "Cantidad requerida del insumo", example = "150.000000") BigDecimal quantity,

                @Schema(description = "Identificador de la unidad de medida usada en la receta", example = "1") Short measurementUnitId,

                @Schema(description = "Nombre de la unidad de medida", example = "Gramo") String measurementUnitName,

                @Schema(description = "Abreviatura de la unidad de medida", example = "g") String measurementUnitAbbreviation,

                @Schema(description = "Costo unitario actual del insumo en inventario", example = "35.0000") BigDecimal unitCost,

                @Schema(description = "Costo proporcional aportado por este insumo a la receta", example = "5.2500") BigDecimal subtotalCost,

                @Schema(description = "Observaciones específicas", example = "Porción estándar") String notes) {
}

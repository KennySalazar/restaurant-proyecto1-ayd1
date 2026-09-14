package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Ingrediente o insumo requerido en la receta de un modificador.
 */
@Schema(description = "Ingrediente o insumo requerido en la receta de un modificador")
public record ModifierIngredientRequest(
        @NotNull(message = "El identificador del insumo es obligatorio")
        @Schema(description = "Identificador único del insumo", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long supplyId,

        @NotNull(message = "La cantidad del insumo es obligatoria")
        @DecimalMin(value = "0.000001", inclusive = true, message = "La cantidad del insumo debe ser mayor que cero")
        @Schema(description = "Cantidad exacta del insumo que se consume con el modificador", example = "30.0000", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal quantity,

        @Schema(description = "Identificador de la unidad de medida (opcional; si se omite se toma la unidad de stock del insumo)", example = "1")
        Short measurementUnitId,

        @Schema(description = "Tipo de ajuste en inventario: AGREGAR o RETIRAR", example = "AGREGAR")
        String adjustmentType,

        @Size(max = 255, message = "Las observaciones no pueden exceder 255 caracteres")
        @Schema(description = "Observaciones o notas para este ingrediente", example = "Queso cheddar extra")
        String notes
) {
}

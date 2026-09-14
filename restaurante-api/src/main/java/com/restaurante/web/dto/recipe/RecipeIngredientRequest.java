package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Solicitud para asociar un insumo con su cantidad a la receta de un platillo.
 */
@Schema(description = "Ingrediente o insumo requerido en la receta de un platillo")
public record RecipeIngredientRequest(
                @NotNull(message = "El identificador del insumo es obligatorio") @Schema(description = "Identificador único del insumo", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long supplyId,

                @NotNull(message = "La cantidad del insumo es obligatoria") @DecimalMin(value = "0.000001", inclusive = true, message = "La cantidad del insumo debe ser mayor que cero") @Schema(description = "Cantidad exacta del insumo requerida para preparar una porción del platillo", example = "150.0000", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal quantity,

                @Schema(description = "Identificador de la unidad de medida (opcional; si se omite se toma la unidad de stock del insumo)", example = "1") Short measurementUnitId,

                @Size(max = 255, message = "Las observaciones no pueden exceder 255 caracteres") @Schema(description = "Observaciones o instrucciones específicas para este ingrediente", example = "Carne molida magra 80/20") String notes) {
}

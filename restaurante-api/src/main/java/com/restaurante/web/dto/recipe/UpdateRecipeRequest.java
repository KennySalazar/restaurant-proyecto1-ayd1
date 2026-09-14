package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud para actualizar la receta vigente de un platillo con sus insumos y
 * cantidades.
 */
@Schema(description = "Datos para actualizar la receta de un platillo")
public record UpdateRecipeRequest(
                @NotEmpty(message = "La receta debe contener al menos un insumo") @Valid @Schema(description = "Nueva lista de insumos y cantidades que componen la receta", requiredMode = Schema.RequiredMode.REQUIRED) List<RecipeIngredientRequest> ingredients,

                @Size(max = 500, message = "El motivo de cambio no puede exceder 500 caracteres") @Schema(description = "Motivo de la actualización o cambio en la receta", example = "Ajuste de porción y sustitución de ingrediente") String changeReason) {
}

package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud para definir la receta de un platillo con sus insumos y cantidades
 * exactas.
 */
@Schema(description = "Datos para definir la receta de un platillo")
public record DefineRecipeRequest(
                @Schema(description = "Identificador único del platillo (opcional si se envía en la URL)", example = "1") Long dishId,

                @NotEmpty(message = "Debe agregarse al menos un insumo a la receta") @Valid @Schema(description = "Lista de insumos y cantidades que componen la receta", requiredMode = Schema.RequiredMode.REQUIRED) List<RecipeIngredientRequest> ingredients,

                @Size(max = 500, message = "El motivo de cambio no puede exceder 500 caracteres") @Schema(description = "Motivo de la creación o definición de la receta", example = "Definición inicial de receta estándar") String changeReason) {
}

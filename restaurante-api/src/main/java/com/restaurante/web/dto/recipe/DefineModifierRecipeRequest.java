package com.restaurante.web.dto.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud para definir o actualizar la receta de un modificador.
 */
@Schema(description = "Datos para definir o actualizar la receta de un modificador")
public record DefineModifierRecipeRequest(
        @NotEmpty(message = "Debe agregarse al menos un insumo")
        @Valid
        @Schema(description = "Lista de insumos y cantidades que componen el modificador", requiredMode = Schema.RequiredMode.REQUIRED)
        List<ModifierIngredientRequest> ingredients,

        @Size(max = 500, message = "El motivo de cambio no puede exceder 500 caracteres")
        @Schema(description = "Motivo de la creación o actualización de la receta", example = "Definición inicial de insumos del modificador")
        String changeReason
) {
}

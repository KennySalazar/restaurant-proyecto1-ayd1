package com.restaurante.web.dto.modifier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Solicitud para asociar platillos a un modificador existente.
 */
@Schema(description = "Solicitud para asociar platillos a un modificador")
public record AssociateDishesRequest(
        @Schema(description = "Listado de identificadores de platillos a asociar", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Debe especificar al menos un platillo para asociar")
        List<Long> dishIds
) {
}

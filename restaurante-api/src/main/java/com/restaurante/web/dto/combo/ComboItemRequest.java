package com.restaurante.web.dto.combo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Platillo y cantidad a incluir dentro de un combo o promoción.
 */
@Schema(description = "Platillo y cantidad a incluir en el combo")
public record ComboItemRequest(
        @Schema(description = "Identificador único del platillo", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El identificador del platillo es obligatorio")
        Long dishId,

        @Schema(description = "Cantidad de porciones de este platillo en el combo (mayor a cero)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La cantidad del platillo es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        Short quantity
) {
}

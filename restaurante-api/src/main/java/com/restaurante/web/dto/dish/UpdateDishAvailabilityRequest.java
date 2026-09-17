package com.restaurante.web.dto.dish;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para actualizar manualmente la disponibilidad de un platillo en el menú.
 */
@Schema(description = "Solicitud para cambiar manualmente la disponibilidad de un platillo")
public record UpdateDishAvailabilityRequest(
        @Schema(description = "Disponibilidad manual en el menú (true para permitir según inventario, false para forzar indisponibilidad)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El estado de disponibilidad manual es obligatorio")
        @JsonProperty("manualAvailable")
        @JsonAlias({"disponibleManual", "available", "disponible", "isAvailable"})
        Boolean manualAvailable
) {
}

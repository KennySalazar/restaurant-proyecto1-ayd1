package com.restaurante.web.dto.kitchen;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.restaurante.domain.model.ComandaDetailStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud para actualizar el estado de preparación de un platillo en cocina.
 */
@Schema(description = "Solicitud para cambiar el estado de preparación de un platillo en cocina")
public record UpdateDishPreparationStatusRequest(
        @Schema(
                description = "Nuevo estado de preparación del platillo (EN_PREPARACION, LISTO, NO_DISPONIBLE)",
                example = "EN_PREPARACION",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"EN_PREPARACION", "LISTO", "NO_DISPONIBLE"}
        )
        @NotNull(message = "El nuevo estado de preparación es obligatorio")
        @JsonProperty("status")
        @JsonAlias({"estado", "newStatus"})
        ComandaDetailStatus status
) {
}

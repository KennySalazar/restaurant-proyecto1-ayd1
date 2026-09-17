package com.restaurante.web.dto.comanda;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Solicitud opcional al marcar un platillo como entregado en la mesa.
 */
@Schema(description = "Solicitud opcional para registrar notas al entregar un platillo en la mesa")
public record DeliverDishRequest(
        @Schema(description = "Observaciones o notas sobre la entrega en mesa", example = "Servido caliente en mesa")
        @JsonProperty("notes")
        @JsonAlias({"notas", "observaciones", "comments"})
        String notes
) {
}

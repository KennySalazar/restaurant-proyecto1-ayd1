package com.restaurante.web.dto.comanda;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Detalle individual de un platillo a registrar en la cuenta con cantidad, modificadores y notas.
 */
@Schema(description = "Detalle de platillo a agregar a la cuenta")
public record AddDishItemRequest(
        @Schema(description = "Identificador del platillo", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El identificador del platillo es obligatorio")
        @JsonProperty("platilloId")
        @JsonAlias({"dishId", "platillo_id"})
        Long dishId,

        @Schema(description = "Cantidad ordenada", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        @JsonProperty("cantidad")
        @JsonAlias({"quantity", "cant"})
        Short quantity,

        @Schema(description = "Notas especiales de preparación", example = "Término medio, sin cebolla")
        @JsonProperty("notas")
        @JsonAlias({"notes", "specialNotes", "notas_especiales"})
        String notes,

        @Schema(description = "Lista de identificadores de modificadores")
        @JsonProperty("modificadoresIds")
        @JsonAlias({"modifierIds", "modificadores", "modificadores_ids"})
        List<Long> modifierIds
) {
}
